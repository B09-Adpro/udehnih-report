package udehnih.report.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    
    @Autowired
    private Environment env;
    
    public String getSecretKey() {
        return env.getProperty("JWT_SECRET_KEY");
    }

    public Long getExpiration() {
        String expirationStr = env.getProperty("JWT_EXPIRATION");
        return Long.parseLong(expirationStr);
    }
} 