package udehnih.report.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        final String authorizationHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authorizationHeader);

        String username = null;
        String role = null;
        String jwt = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                log.debug("JWT token extracted: {}", jwt);
                
                username = jwtUtil.extractUsername(jwt);
                role = jwtUtil.extractRole(jwt);
                log.debug("Extracted username: {}, role: {}", username, role);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
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
                        log.debug("Authentication successful. Role set to: {}", role);

                        // Set user information in request attributes
                        request.setAttribute("X-User-Email", username);
                        request.setAttribute("X-User-Role", role.replace("ROLE_", ""));
                    } else {
                        log.warn("Token validation failed");
                    }
                }
            } else {
                log.debug("No JWT token found in request");
            }
        } catch (Exception e) {
            log.error("Authentication error", e);
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
} 