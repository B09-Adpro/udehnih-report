package udehnih.report.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {
    
    @Value("${cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${cors.exposed-headers}")
    private String exposedHeaders;
    
    @Value("${cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${cors.allow-credentials}")
    private String allowCredentials;
    
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = "*";
        }
        
        // Check if the origin is allowed (if allowedOrigins is not '*')
        if (!"*".equals(allowedOrigins)) {
            boolean allowed = false;
            for (String allowedOrigin : allowedOrigins.split(",")) {
                if (allowedOrigin.trim().equals(origin)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                origin = "*";
            }
        }
        
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", allowedMethods);
        response.setHeader("Access-Control-Allow-Headers", allowedHeaders);
        response.setHeader("Access-Control-Expose-Headers", exposedHeaders);
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", allowCredentials);
        response.setHeader("Vary", "Origin");
        
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
