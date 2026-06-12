package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.config.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

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
    record LoginResponse(String token, long userId, String role, Long shopId, String fullName) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        try {
            // Find account
            Map<String, Object> account = jdbcTemplate.queryForMap(
                    "SELECT a.id, a.user_id, a.password_hash, a.role, u.full_name " +
                    "FROM Accounts a JOIN Users u ON a.user_id = u.id " +
                    "WHERE a.username = ? AND a.status = 1", request.username());
            
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
            return new LoginResponse(token, userId, role, shopId, fullName);

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
            jdbcTemplate.update("INSERT INTO Users (id, full_name) VALUES (?, ?)", nextUserId, request.fullName());
            jdbcTemplate.update("INSERT INTO Accounts (id, user_id, username, password_hash, email, role, status) VALUES (?, ?, ?, ?, ?, 'BUYER', 1)",
                    nextAccountId, nextUserId, request.username(), hash, request.email());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username hoac Email da ton tai");
        }

        String token = jwtUtil.generateToken(nextUserId, "BUYER", null);
        return new LoginResponse(token, nextUserId, "BUYER", null, request.fullName());
    }
}
