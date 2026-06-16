package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.config.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtUtil jwtUtil;

    public AuthController(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtUtil = jwtUtil;
    }

    record LoginRequest(String username, String password) {}
    record LoginResponse(String token, String refreshToken, long userId, String role, Long shopId, String fullName, String status) {}
    record OtpRequest(String email, String otp) {}
    record ForgotPasswordRequest(String email) {}
    record ResetPasswordRequest(String email, String otp, String newPassword) {}
    record RefreshRequest(String refreshToken) {}
    record LogoutRequest(String refreshToken) {}
    record GoogleLoginRequest(String email, String fullName) {}
    record MessageResponse(String message, String otp) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        try {
            // Find account
            Map<String, Object> account = jdbcTemplate.queryForMap(
                    "SELECT a.id, a.user_id, a.password_hash, a.role, a.status AS account_status, COALESCE(u.status, 'ACTIVE') AS user_status, u.full_name " +
                    "FROM Accounts a JOIN Users u ON a.user_id = u.id " +
                    "WHERE a.username = ?", request.username());
            String userStatus = (String) account.get("user_status");
            Number accountStatus = (Number) account.get("account_status");
            if (accountStatus == null || accountStatus.intValue() != 1 || !"ACTIVE".equalsIgnoreCase(userStatus)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan chua active hoac dang bi khoa");
            }
            
            String storedHash = (String) account.get("password_hash");
            if (storedHash.startsWith("{noop}")) {
                if (!storedHash.substring(6).equals(request.password())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mat khau");
                }
            } else {
                if (!BCrypt.checkpw(request.password(), storedHash)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mat khau");
                }
            }

            long userId = ((Number) account.get("user_id")).longValue();
            String role = (String) account.get("role");
            String fullName = (String) account.get("full_name");
            
            // Check if user has any shop(s); pick the first shop id if multiple exist
            Long shopId = null;
            try {
                var shopIds = jdbcTemplate.queryForList("SELECT id FROM Shops WHERE owner_id = ?", Long.class, userId);
                if (!shopIds.isEmpty()) {
                    shopId = shopIds.get(0);
                }
            } catch (EmptyResultDataAccessException ignored) {}

            String token = jwtUtil.generateToken(userId, role, shopId);
            String refreshToken = issueRefreshToken(((Number) account.get("id")).longValue());
            return new LoginResponse(token, refreshToken, userId, role, shopId, fullName, userStatus);

        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Khong tim thay tai khoan");
        }
    }

    record RegisterRequest(String username, String password, String email, String fullName) {}

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request) {
        // Validation basic
        if (request.username() == null || request.username().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username phai it nhat 3 ky tu");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password phai it nhat 6 ky tu");
        }
        
        // Generate IDs
        Long nextUserId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM Users", Long.class);
        Long nextAccountId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM Accounts", Long.class);

        String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt());

        try {
            jdbcTemplate.update("INSERT INTO Users (id, full_name, status) VALUES (?, ?, 'PENDING')", nextUserId, request.fullName());
            jdbcTemplate.update("INSERT INTO Accounts (id, user_id, username, password_hash, email, role, status) VALUES (?, ?, ?, ?, ?, 'BUYER', 1)",
                    nextAccountId, nextUserId, request.username(), hash, request.email());
            createOtp(nextAccountId, request.email(), "VERIFY_EMAIL");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username hoac Email da ton tai");
        }

        return new LoginResponse(null, null, nextUserId, "BUYER", null, request.fullName(), "PENDING");
    }

    @PostMapping("/verify-otp")
    public MessageResponse verifyOtp(@RequestBody OtpRequest request) {
        long accountId = consumeOtp(request.email(), request.otp(), "VERIFY_EMAIL");
        jdbcTemplate.update("UPDATE Users u JOIN Accounts a ON a.user_id = u.id SET u.status = 'ACTIVE' WHERE a.id = ?", accountId);
        return new MessageResponse("Email da duoc xac thuc", null);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Long accountId = jdbcTemplate.queryForObject("SELECT id FROM Accounts WHERE email = ?", Long.class, request.email());
        String otp = createOtp(accountId, request.email(), "RESET_PASSWORD");
        return new MessageResponse("OTP reset password da duoc tao cho demo local", otp);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        long accountId = consumeOtp(request.email(), request.otp(), "RESET_PASSWORD");
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password phai it nhat 6 ky tu");
        }
        jdbcTemplate.update("UPDATE Accounts SET password_hash = ? WHERE id = ?",
                BCrypt.hashpw(request.newPassword(), BCrypt.gensalt()), accountId);
        return new MessageResponse("Da doi mat khau", null);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshRequest request) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT rt.id AS refresh_id, a.id, a.user_id, a.role, COALESCE(u.status, 'ACTIVE') AS user_status, u.full_name
                FROM refresh_tokens rt
                JOIN Accounts a ON a.id = rt.account_id
                JOIN Users u ON u.id = a.user_id
                WHERE rt.token = ? AND rt.revoked = false AND (rt.expires_at IS NULL OR rt.expires_at > CURRENT_TIMESTAMP)
                """, request.refreshToken());
        String userStatus = (String) row.get("user_status");
        if (!"ACTIVE".equalsIgnoreCase(userStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan chua active hoac dang bi khoa");
        }
        jdbcTemplate.update("UPDATE refresh_tokens SET revoked = true WHERE id = ?", row.get("refresh_id"));
        long userId = ((Number) row.get("user_id")).longValue();
        String role = (String) row.get("role");
        Long shopId = firstShopId(userId);
        String token = jwtUtil.generateToken(userId, role, shopId);
        String refreshToken = issueRefreshToken(((Number) row.get("id")).longValue());
        return new LoginResponse(token, refreshToken, userId, role, shopId, (String) row.get("full_name"), userStatus);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestBody LogoutRequest request) {
        jdbcTemplate.update("UPDATE refresh_tokens SET revoked = true WHERE token = ?", request.refreshToken());
        return new MessageResponse("Da logout", null);
    }

    @PostMapping("/google-login")
    public LoginResponse googleLogin(@RequestBody GoogleLoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu email Google");
        }
        Long accountId;
        Long userId;
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("SELECT id, user_id FROM Accounts WHERE email = ?", request.email());
            accountId = ((Number) row.get("id")).longValue();
            userId = ((Number) row.get("user_id")).longValue();
        } catch (EmptyResultDataAccessException ex) {
            userId = nextId("Users");
            accountId = nextId("Accounts");
            jdbcTemplate.update("INSERT INTO Users (id, full_name, status) VALUES (?, ?, 'ACTIVE')", userId, request.fullName());
            jdbcTemplate.update("INSERT INTO Accounts (id, user_id, username, password_hash, email, role, status) VALUES (?, ?, ?, '{noop}GOOGLE', ?, 'BUYER', 1)",
                    accountId, userId, request.email(), request.email());
        }
        String token = jwtUtil.generateToken(userId, "BUYER", firstShopId(userId));
        return new LoginResponse(token, issueRefreshToken(accountId), userId, "BUYER", firstShopId(userId), request.fullName(), "ACTIVE");
    }

    private String issueRefreshToken(long accountId) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (id, account_id, token, revoked, expires_at)
                VALUES (?, ?, ?, false, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 14 DAY))
                """, nextId("refresh_tokens"), accountId, token);
        return token;
    }

    private String createOtp(Long accountId, String email, String purpose) {
        String otp = "123456";
        jdbcTemplate.update("""
                INSERT INTO auth_otps (id, account_id, email, purpose, otp_code, consumed, expires_at)
                VALUES (?, ?, ?, ?, ?, false, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 MINUTE))
                """, nextId("auth_otps"), accountId, email, purpose, otp);
        return otp;
    }

    private long consumeOtp(String email, String otp, String purpose) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT id, account_id FROM auth_otps
                    WHERE email = ? AND otp_code = ? AND purpose = ? AND consumed = false AND expires_at > CURRENT_TIMESTAMP
                    ORDER BY id DESC LIMIT 1
                    """, email, otp, purpose);
            jdbcTemplate.update("UPDATE auth_otps SET consumed = true WHERE id = ?", row.get("id"));
            return ((Number) row.get("account_id")).longValue();
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP khong hop le hoac da het han");
        }
    }

    private Long firstShopId(long userId) {
        var shopIds = jdbcTemplate.queryForList("SELECT id FROM Shops WHERE owner_id = ? ORDER BY id LIMIT 1", Long.class, userId);
        return shopIds.isEmpty() ? null : shopIds.get(0);
    }

    private long nextId(String tableName) {
        Long id = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM " + tableName, Long.class);
        return id == null ? 1L : id;
    }
}
