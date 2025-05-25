package udehnih.report.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    private static final String ALLOWED_HEADERS = "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control, Access-Control-Allow-Origin, Access-Control-Allow-Headers, X-Auth-Token";
    private static final String EXPOSED_HEADERS = "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-User-Email, X-User-Role, X-Auth-Token, X-User-Id, Access-Control-Allow-Origin, Access-Control-Allow-Credentials";
    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, PATCH";
    // Support multiple origins - we'll handle this dynamically in the filter

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Get the origin from the request
        String origin = request.getHeader("Origin");
        
        // If origin is null (e.g., same-origin request), or we want to allow any origin
        // we'll set a default value or handle it specially
        if (origin == null) {
            // For same-origin requests, this is fine
            // For API testing tools like Postman, this helps too
            origin = "*";
        }
        
        // Set the allowed origin to the requesting origin
        // This is important for CORS with credentials
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        response.setHeader("Access-Control-Expose-Headers", EXPOSED_HEADERS);
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        // Add Vary header to indicate that the response varies based on Origin
        // This is important for proper caching behavior
        response.setHeader("Vary", "Origin");
        
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
