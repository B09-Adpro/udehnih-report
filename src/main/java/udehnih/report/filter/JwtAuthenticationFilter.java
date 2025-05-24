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
import udehnih.report.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Clear any existing authentication
        SecurityContextHolder.clearContext();

        final String authorizationHeader = request.getHeader("Authorization");
        log.info("Request to: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Authorization header: {}", authorizationHeader);

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
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
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