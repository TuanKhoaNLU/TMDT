package com.tmdt.marketplace.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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
            request.setAttribute("userId", claims.get("userId", Long.class));
            request.setAttribute("role", claims.get("role", String.class));
            if (claims.containsKey("shopId")) {
                request.setAttribute("shopId", claims.get("shopId", Long.class));
            }
        }
        return true;
    }
}
