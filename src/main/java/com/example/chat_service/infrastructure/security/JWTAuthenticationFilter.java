// src/main/java/com/example/chat_service/infrastructure/security/JWTAuthenticationFilter.java
package com.example.chat_service.infrastructure.security;

import com.example.chat_service.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final JWTVerifier verifier;
    private final AuthConfig config;
    
    private static final String COOKIE_NAME = "access_token";
    
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/health/",
            "/actuator/health",
            "/api/v1/auth/ping"
    );

    public JWTAuthenticationFilter(JWTVerifier verifier, AuthConfig config) {
        this.verifier = verifier;
        this.config = config;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);

            if (token != null && !token.isEmpty()) {
                logger.debug("Verifying token for request: {}", requestURI);
                
                Claims claims = verifier.verify(token, config.getAuthPublicKeyUrl());
                String userId = claims.get("user_id", String.class);
                
                if (userId == null || userId.isEmpty()) {
                    throw new SecurityException("Token missing required claim: user_id");
                }
                
                UserContext.setUserId(userId);
                logger.debug("Authenticated user_id={}", userId);
            }
            
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, "Token expired");
            
        } catch (SignatureException | MalformedJwtException e) {
            logger.warn("Invalid JWT for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, "Invalid token");
            
        } catch (SecurityException e) {
            logger.warn("Security error for request {}: {}", requestURI, e.getMessage());
            sendUnauthorized(response, e.getMessage());
            
        } catch (Exception e) {
            // ✅ ENHANCED LOGGING: Print exception class + message + stack trace
            logger.error("Unexpected error during JWT verification for request {}: {} - {}", 
                        requestURI, e.getClass().getName(), e.getMessage(), e);
            sendUnauthorized(response, "Authentication failed");
            
        } finally {
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                logger.debug("Extracted token from Authorization header");
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.isEmpty()) {
                        logger.debug("Extracted token from cookie '{}'", COOKIE_NAME);
                        return token;
                    }
                }
            }
        }

        logger.debug("No authentication token found in request");
        return null;
    }

    private boolean isExcludedPath(String requestURI) {
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message.replace("\"", "\\\"")));
        response.getWriter().flush();
    }
}