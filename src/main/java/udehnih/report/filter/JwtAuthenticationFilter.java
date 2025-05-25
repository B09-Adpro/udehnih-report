package udehnih.report.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Don't clear the security context at the beginning, as it might already be set
        // Only clear it if authentication fails

        final String authorizationHeader = request.getHeader("Authorization");
        log.info("Request to: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Authorization header: {}", authorizationHeader);
        
        // Debug all headers to see what's being sent
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("Header: {} = {}", headerName, request.getHeader(headerName));
        }

        String username = null;
        String role = null;
        String jwt = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                log.info("JWT token extracted: {}", jwt);
                
                username = jwtUtil.extractUsername(jwt);
                role = jwtUtil.extractRole(jwt);
                log.info("Extracted username: {}, role: {}", username, role);
                
                if (username != null) {
                    // Ensure role has ROLE_ prefix
                    if (!role.startsWith(AppConstants.ROLE_PREFIX)) {
                        role = AppConstants.ROLE_PREFIX + role;
                    }
                    
                    UserDetails userDetails = new User(username, "", 
                        Collections.singleton(new SimpleGrantedAuthority(role)));

                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("Authentication successful. Role set to: {}", role);
                        log.info("Authentication details: {}", authToken.getAuthorities());

                        // Set user information in request attributes
                        request.setAttribute("X-User-Email", username);
                        request.setAttribute("X-User-Role", role.replace("ROLE_", ""));
                        
                        // Add authentication headers to the response
                        response.setHeader("X-Auth-Status", "authenticated");
                        response.setHeader("X-Auth-Username", username);
                        response.setHeader("X-Auth-Role", role.replace(AppConstants.ROLE_PREFIX, ""));
                        
                        // Set comprehensive authentication headers for the frontend
                        response.setHeader("Authorization", "Bearer " + jwt);
                        
                        // Add additional authentication headers to help frontend
                        response.setHeader("X-Auth-Token", jwt);
                        response.setHeader("Access-Control-Expose-Headers", 
                            "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-Auth-Token, X-User-Id, X-User-Roles");
                        
                        // Add user ID and roles in the format expected by the frontend
                        try {
                            String userIdSql = "SELECT id FROM users WHERE email = ?";
                            Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, username);
                            if (userId != null) {
                                response.setHeader("X-User-Id", userId.toString());
                            }
                        } catch (Exception e) {
                            log.warn("Could not retrieve user ID: {}", e.getMessage());
                        }
                        
                        // Set authentication cookies for frontend frameworks
                        jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("auth-token", jwt);
                        authCookie.setPath("/");
                        authCookie.setHttpOnly(true); // For security
                        authCookie.setMaxAge(24 * 60 * 60); // 24 hours
                        response.addCookie(authCookie);
                        
                        // Set a non-HttpOnly cookie for frontend JavaScript access
                        jakarta.servlet.http.Cookie userAuthCookie = new jakarta.servlet.http.Cookie("user-authenticated", "true");
                        userAuthCookie.setPath("/");
                        userAuthCookie.setHttpOnly(false); // Allow JavaScript access
                        userAuthCookie.setMaxAge(24 * 60 * 60); // 24 hours
                        response.addCookie(userAuthCookie);
                        
                        // Add Cache-Control header to prevent caching of authenticated responses
                        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        response.setHeader("Pragma", "no-cache");
                        response.setHeader("Expires", "0");
                        
                        // Add additional headers with user information
                        try {
                            String userNameSql = "SELECT name FROM users WHERE email = ?";
                            String userName = authJdbcTemplate.queryForObject(userNameSql, String.class, username);
                            if (userName != null) {
                                response.setHeader("X-Auth-Name", userName);
                            }
                        } catch (Exception e) {
                            log.warn("Could not retrieve user name: {}", e.getMessage());
                        }
                    } else {
                        log.warn("Token validation failed - token may be expired or invalid");
                    }
                } else {
                    log.warn("Username extracted from token is null");
                }
            } else {
                log.info("No JWT token found in request for: {} {}", request.getMethod(), request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("Authentication error", e);
            SecurityContextHolder.clearContext();
            // Add a header to indicate authentication failure
            response.setHeader("X-Auth-Status", "unauthenticated");
            response.setHeader("X-Auth-Error", e.getMessage());
        }
        
        // Log the current authentication state before proceeding
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Proceeding with filter chain with authentication: {}", auth.getAuthorities());
        } else {
            log.warn("Proceeding with filter chain with no authentication");
        }
        
        chain.doFilter(request, response);
    }
} 