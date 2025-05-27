package udehnih.report.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Enumeration;
import udehnih.report.model.UserInfo;
@Slf4j

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthServiceClient authServiceClient;
    @Override

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        logRequestInfo(request);
        try {
            processJwtAuthentication(request, response);
        } catch (Exception e) {
            handleAuthenticationError(response, e);
        }
        logAuthenticationState();
        chain.doFilter(request, response);
    }

    private void logRequestInfo(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        log.info("Request to: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Authorization header: {}", authorizationHeader);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("Header: {} = {}", headerName, request.getHeader(headerName));
        }
    }

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
            SecurityContextHolder.clearContext(); 
            return;
        }
        authenticateUser(request, response, username, role, jwt);
    }

    private void authenticateUser(HttpServletRequest request, HttpServletResponse response, 
                                 String username, String role, String jwt) {
    // First, try to get user roles from the database
    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
    try {
        UserInfo userInfo = authServiceClient.getUserByEmail(username);
        if (userInfo != null && userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            log.info("Retrieved roles from database for user {}: {}", username, userInfo.getRoles());
            for (String dbRole : userInfo.getRoles()) {
                String formattedRole = dbRole.trim();
                if (!formattedRole.startsWith(AppConstants.ROLE_PREFIX)) {
                    formattedRole = AppConstants.ROLE_PREFIX + formattedRole;
                }
                authorities.add(new SimpleGrantedAuthority(formattedRole));
            }
        }
    } catch (Exception e) {
        log.warn("Error retrieving user roles from database: {}", e.getMessage());
        // Fall back to JWT token roles if database lookup fails
    }
    
    // If no roles were found in the database, fall back to the JWT token roles
    if (authorities.isEmpty()) {
        log.info("No roles found in database, using roles from JWT token: {}", role);
        String[] roleArray = role.split(",");
        for (String singleRole : roleArray) {
            if (!singleRole.trim().startsWith(AppConstants.ROLE_PREFIX)) {
                singleRole = AppConstants.ROLE_PREFIX + singleRole.trim();
            }
            authorities.add(new SimpleGrantedAuthority(singleRole));
        }
        
        // If still no roles, use STUDENT as default
        if (authorities.isEmpty()) {
            log.info("No roles found in JWT token, using default STUDENT role");
            authorities.add(new SimpleGrantedAuthority(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE));
        }
    }
    
    UserDetails userDetails = new User(username, "", authorities);
    if (!jwtUtil.validateToken(jwt, userDetails)) {
        log.warn("Token validation failed - token may be expired or invalid");
        SecurityContextHolder.clearContext(); 
        return;
    }
    
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
    
    // Build a role string for logging and headers
    StringBuilder roleBuilder = new StringBuilder();
    for (SimpleGrantedAuthority authority : authorities) {
        if (roleBuilder.length() > 0) {
            roleBuilder.append(",");
        }
        roleBuilder.append(authority.getAuthority());
    }
    String updatedRole = roleBuilder.toString();
    
    log.info("Authentication successful. Roles set to: {}", updatedRole);
    log.info("Authentication details: {}", authToken.getAuthorities());
    
    setRequestAttributes(request, username, updatedRole);
    setAuthenticationHeaders(response, username, updatedRole, jwt);
    setAuthenticationCookies(response, jwt);
    setCacheControlHeaders(response);
    setUserInfoHeaders(response, username);
}

    private void setRequestAttributes(HttpServletRequest request, String username, String role) {
        request.setAttribute("X-User-Email", username);
        request.setAttribute("X-User-Role", role.replace(AppConstants.ROLE_PREFIX, ""));
    }

    private void setAuthenticationHeaders(HttpServletResponse response, String username, String role, String jwt) {
        response.setHeader("X-Auth-Status", "authenticated");
        response.setHeader("X-Auth-Username", sanitizeHeaderValue(username));
        response.setHeader("X-Auth-Role", sanitizeHeaderValue(role.replace(AppConstants.ROLE_PREFIX, "")));
        response.setHeader(AppConstants.AUTHORIZATION_HEADER, AppConstants.BEARER_PREFIX + jwt);
        response.setHeader("X-Auth-Token", jwt);
        response.setHeader("Access-Control-Expose-Headers", 
            "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-Auth-Token, X-User-Id, X-User-Roles");
        addUserIdToHeader(response, username);
    }

    private void addUserIdToHeader(HttpServletResponse response, String username) {
        try {
            UserInfo userInfo = authServiceClient.getUserByEmail(username);
            if (userInfo != null && userInfo.getId() != null) {
                response.setHeader("X-User-Id", sanitizeHeaderValue(userInfo.getId().toString()));
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user ID: {}", e.getMessage());
        }
    }

    private void setAuthenticationCookies(HttpServletResponse response, String jwt) {
        jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("auth-token", jwt);
        authCookie.setPath("/");
        authCookie.setHttpOnly(true);
        authCookie.setMaxAge(24 * 60 * 60); 
        response.addCookie(authCookie);
        jakarta.servlet.http.Cookie userAuthCookie = new jakarta.servlet.http.Cookie("user-authenticated", "true");
        userAuthCookie.setPath("/");
        userAuthCookie.setHttpOnly(false);
        userAuthCookie.setMaxAge(24 * 60 * 60); 
        response.addCookie(userAuthCookie);
    }

    private void setCacheControlHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    private void setUserInfoHeaders(HttpServletResponse response, String username) {
        try {
            UserInfo userInfo = authServiceClient.getUserByEmail(username);
            if (userInfo != null && userInfo.getName() != null) {
                response.setHeader("X-Auth-Name", sanitizeHeaderValue(userInfo.getName()));
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user name: {}", e.getMessage());
        }
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) {
        log.error("Authentication error", e);
        SecurityContextHolder.clearContext();
        response.setHeader("X-Auth-Status", "unauthenticated");
        response.setHeader("X-Auth-Error", sanitizeHeaderValue(e.getMessage()));
    }

    /**
     * Sanitizes a value to be used in HTTP headers to prevent HTTP Response Splitting attacks
     * @param value the value to sanitize
     * @return the sanitized value
     */
    private String sanitizeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        // Remove CR and LF characters to prevent HTTP Response Splitting
        return value.replaceAll("[\r\n]", "");
    }
    
    private void logAuthenticationState() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Proceeding with filter chain with authentication: {}", auth.getAuthorities());
        } else {
            log.warn("Proceeding with filter chain with no authentication");
        }
    }
} 