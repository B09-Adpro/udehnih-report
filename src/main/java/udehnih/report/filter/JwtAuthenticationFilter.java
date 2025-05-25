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
        logRequestInfo(request);
        
        try {
            processJwtAuthentication(request, response);
        } catch (Exception e) {
            handleAuthenticationError(response, e);
        }
        
        logAuthenticationState();
        chain.doFilter(request, response);
    }
    
    /**
     * Log request information including headers
     */
    private void logRequestInfo(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        log.info("Request to: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Authorization header: {}", authorizationHeader);
        
        // Debug all headers to see what's being sent
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("Header: {} = {}", headerName, request.getHeader(headerName));
        }
    }
    
    /**
     * Process JWT authentication from the request
     */
    private void processJwtAuthentication(HttpServletRequest request, HttpServletResponse response) {
        final String authorizationHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        
        if (authorizationHeader == null || !authorizationHeader.startsWith(AppConstants.BEARER_PREFIX)) {
            log.info("No JWT token found in request for: {} {}", request.getMethod(), request.getRequestURI());
            return;
        }
        
        String jwt = authorizationHeader.substring(AppConstants.BEARER_PREFIX.length());
        log.info("JWT token extracted: {}", jwt);
        
        String username = jwtUtil.extractUsername(jwt);
        String role = jwtUtil.extractRole(jwt);
        log.info("Extracted username: {}, role: {}", username, role);
        
        if (username == null) {
            log.warn("Username extracted from token is null");
            return;
        }
        
        authenticateUser(request, response, username, role, jwt);
    }
    
    /**
     * Authenticate the user and set up security context
     */
    private void authenticateUser(HttpServletRequest request, HttpServletResponse response, 
                                 String username, String role, String jwt) {
        // Ensure role has ROLE_ prefix
        if (!role.startsWith(AppConstants.ROLE_PREFIX)) {
            role = AppConstants.ROLE_PREFIX + role;
        }
        
        UserDetails userDetails = new User(username, "", 
            Collections.singleton(new SimpleGrantedAuthority(role)));

        if (!jwtUtil.validateToken(jwt, userDetails)) {
            log.warn("Token validation failed - token may be expired or invalid");
            return;
        }
        
        // Set up authentication token
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.info("Authentication successful. Role set to: {}", role);
        log.info("Authentication details: {}", authToken.getAuthorities());

        // Set request attributes and response headers
        setRequestAttributes(request, username, role);
        setAuthenticationHeaders(response, username, role, jwt);
        setAuthenticationCookies(response, jwt);
        setCacheControlHeaders(response);
        setUserInfoHeaders(response, username);
    }
    
    /**
     * Set request attributes for the authenticated user
     */
    private void setRequestAttributes(HttpServletRequest request, String username, String role) {
        request.setAttribute("X-User-Email", username);
        request.setAttribute("X-User-Role", role.replace(AppConstants.ROLE_PREFIX, ""));
    }
    
    /**
     * Set authentication headers in the response
     */
    private void setAuthenticationHeaders(HttpServletResponse response, String username, String role, String jwt) {
        response.setHeader("X-Auth-Status", "authenticated");
        response.setHeader("X-Auth-Username", username);
        response.setHeader("X-Auth-Role", role.replace(AppConstants.ROLE_PREFIX, ""));
        response.setHeader(AppConstants.AUTHORIZATION_HEADER, AppConstants.BEARER_PREFIX + jwt);
        response.setHeader("X-Auth-Token", jwt);
        response.setHeader("Access-Control-Expose-Headers", 
            "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-Auth-Token, X-User-Id, X-User-Roles");
        
        // Add user ID to headers
        addUserIdToHeader(response, username);
    }
    
    /**
     * Add user ID to response headers
     */
    private void addUserIdToHeader(HttpServletResponse response, String username) {
        try {
            String userIdSql = "SELECT id FROM users WHERE email = ?";
            Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, username);
            if (userId != null) {
                response.setHeader("X-User-Id", userId.toString());
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user ID: {}", e.getMessage());
        }
    }
    
    /**
     * Set authentication cookies in the response
     */
    private void setAuthenticationCookies(HttpServletResponse response, String jwt) {
        // Auth token cookie (HttpOnly for security)
        jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("auth-token", jwt);
        authCookie.setPath("/");
        authCookie.setHttpOnly(true);
        authCookie.setMaxAge(24 * 60 * 60); // 24 hours
        response.addCookie(authCookie);
        
        // User authenticated cookie (accessible by JavaScript)
        jakarta.servlet.http.Cookie userAuthCookie = new jakarta.servlet.http.Cookie("user-authenticated", "true");
        userAuthCookie.setPath("/");
        userAuthCookie.setHttpOnly(false);
        userAuthCookie.setMaxAge(24 * 60 * 60); // 24 hours
        response.addCookie(userAuthCookie);
    }
    
    /**
     * Set cache control headers in the response
     */
    private void setCacheControlHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
    
    /**
     * Set user info headers in the response
     */
    private void setUserInfoHeaders(HttpServletResponse response, String username) {
        try {
            String userNameSql = "SELECT name FROM users WHERE email = ?";
            String userName = authJdbcTemplate.queryForObject(userNameSql, String.class, username);
            if (userName != null) {
                response.setHeader("X-Auth-Name", userName);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user name: {}", e.getMessage());
        }
    }
    
    /**
     * Handle authentication errors
     */
    private void handleAuthenticationError(HttpServletResponse response, Exception e) {
        log.error("Authentication error", e);
        SecurityContextHolder.clearContext();
        // Add headers to indicate authentication failure
        response.setHeader("X-Auth-Status", "unauthenticated");
        response.setHeader("X-Auth-Error", e.getMessage());
    }
    
    /**
     * Log the current authentication state
     */
    private void logAuthenticationState() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Proceeding with filter chain with authentication: {}", auth.getAuthorities());
        } else {
            log.warn("Proceeding with filter chain with no authentication");
        }
    }
} 