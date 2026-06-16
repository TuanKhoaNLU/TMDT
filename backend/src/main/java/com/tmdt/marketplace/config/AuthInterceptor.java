package com.tmdt.marketplace.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;

    public AuthInterceptor(JwtUtil jwtUtil, JdbcTemplate jdbcTemplate) {
        this.jwtUtil = jwtUtil;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (request.getRequestURI().startsWith("/api/v1/auth/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token khong hop le hoac da het han");
                return false;
            }
            Claims claims = jwtUtil.extractClaims(token);
            Number userIdClaim = claims.get("userId", Number.class);
            if (userIdClaim == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token khong hop le hoac da het han");
                return false;
            }

            Long userId = userIdClaim.longValue();
            String tokenRole = claims.get("role", String.class);
            Map<String, Object> account = jdbcTemplate.queryForMap("""
                    SELECT a.role AS account_role, a.status AS account_status, COALESCE(u.status, 'ACTIVE') AS user_status
                    FROM Accounts a
                    JOIN Users u ON u.id = a.user_id
                    WHERE a.user_id = ?
                    """, userId);
            Number accountStatus = (Number) account.get("account_status");
            String userStatus = (String) account.get("user_status");
            String accountRole = (String) account.get("account_role");
            if (accountStatus == null || accountStatus.intValue() != 1 || !"ACTIVE".equalsIgnoreCase(userStatus)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tai khoan chua active hoac dang bi khoa");
                return false;
            }
            if (tokenRole != null && accountRole != null && !accountRole.equalsIgnoreCase(tokenRole)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tai khoan khong duoc phep thuc hien thao tac nay");
                return false;
            }

            request.setAttribute("userId", userId);
            request.setAttribute("role", accountRole == null ? tokenRole : accountRole);
            if (claims.containsKey("shopId")) {
                Number shopIdClaim = claims.get("shopId", Number.class);
                if (shopIdClaim != null) {
                    request.setAttribute("shopId", shopIdClaim.longValue());
                }
            }
        }
        return true;
    }
}
