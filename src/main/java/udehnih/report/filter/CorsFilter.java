package udehnih.report.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {
    
    @Autowired
    private Environment env;
    
    private String getAllowedHeaders() {
        String headers = env.getProperty("ALLOWED_HEADERS");
        if (headers == null || headers.isEmpty()) {
            return "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control, Access-Control-Allow-Origin, Access-Control-Allow-Headers, X-Auth-Token";
        }
        return headers;
    }
    
    private String getExposedHeaders() {
        String headers = env.getProperty("EXPOSED_HEADERS");
        if (headers == null || headers.isEmpty()) {
            return "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-User-Email, X-User-Role, X-Auth-Token, X-User-Id, Access-Control-Allow-Origin, Access-Control-Allow-Credentials";
        }
        return headers;
    }
    
    private String getAllowedMethods() {
        String methods = env.getProperty("ALLOWED_METHODS");
        if (methods == null || methods.isEmpty()) {
            return "GET, POST, PUT, DELETE, OPTIONS, PATCH";
        }
        return methods;
    }
    
    private String getAllowCredentials() {
        String credentials = env.getProperty("ALLOWED_CREDENTIALS");
        if (credentials == null || credentials.isEmpty()) {
            return "true";
        }
        return credentials;
    }
    
    private String getAllowedOrigins() {
        String origins = env.getProperty("ALLOWED_ORIGINS");
        if (origins == null || origins.isEmpty()) {
            return "*";
        }
        return origins;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", getAllowCredentials());
        } else {
            response.setHeader("Access-Control-Allow-Origin", getAllowedOrigins());
        }
        
        response.setHeader("Access-Control-Allow-Methods", getAllowedMethods());
        response.setHeader("Access-Control-Allow-Headers", getAllowedHeaders());
        response.setHeader("Access-Control-Expose-Headers", getExposedHeaders());
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Vary", "Origin");
        
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
