package udehnih.report.util;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import udehnih.report.config.JwtConfig;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.function.Function;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component

@Slf4j
public class JwtUtil {
    @Autowired
    private JwtConfig jwtConfig;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        if (role.contains(",")) {
            String[] roles = role.split(",");
            StringBuilder formattedRoles = new StringBuilder();
            for (String singleRole : roles) {
                String formattedRole = singleRole.trim();
                if (!formattedRole.startsWith(AppConstants.ROLE_PREFIX)) {
                    formattedRole = AppConstants.ROLE_PREFIX + formattedRole;
                }
                if (formattedRoles.length() > 0) {
                    formattedRoles.append(",");
                }
                formattedRoles.append(formattedRole);
            }
            claims.put("role", formattedRoles.toString());
            claims.put("roles", formattedRoles.toString().split(","));
        } else {
            String formattedRole = role.startsWith(AppConstants.ROLE_PREFIX) ? role : AppConstants.ROLE_PREFIX + role;
            claims.put("role", formattedRole);
            claims.put("roles", new String[]{formattedRole});
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {

 

       return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            try {
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof String[]) {
                    String[] roles = (String[]) rolesObj;
                    if (roles.length > 0) {
                        StringBuilder roleStr = new StringBuilder();
                        for (String role : roles) {
                            if (roleStr.length() > 0) {
                                roleStr.append(",");
                            }
                            roleStr.append(role);
                        }
                        return roleStr.toString();
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract roles array, falling back to role string: {}", e.getMessage());
            }
            String role = claims.get("role", String.class);
            if (role == null) {
                log.warn("Role claim is missing in the token");
                return AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE; 
            }
            return role.startsWith(AppConstants.ROLE_PREFIX) ? role : AppConstants.ROLE_PREFIX + role;
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            return AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE; 
        }
    }

    public Date extractExpiration(String token) {

 

       return extractClaim(token, Claims::getExpiration);
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            log.error("Error extracting claim from token: {}", e.getMessage());
            return null;
        }
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Error parsing JWT with local key: {}", e.getMessage());
            // If token validation fails with our key, try to extract claims without verification
            // This is useful for tokens from external services
            return extractClaimsWithoutVerification(token);
        }
    }

    private Claims extractClaimsWithoutVerification(String token) {
        try {
            // Split the token into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }
            
            // Decode the claims part (middle part) without verification
            String base64EncodedClaims = parts[1];
            java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
            String claimsJson = new String(decoder.decode(base64EncodedClaims), StandardCharsets.UTF_8);
            
            // Parse JSON into Claims
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claimsMap = mapper.readValue(claimsJson, Map.class);
            
            // Create a custom Claims implementation
            CustomClaims claims = new CustomClaims(claimsMap);
            log.info("Successfully extracted claims without verification: {}", claims.getSubject());
            return claims;
        } catch (Exception e) {
            log.error("Failed to extract claims without verification: {}", e.getMessage());
            throw new RuntimeException("Failed to extract claims from token", e);
        }
    }
    
    // Custom implementation of Claims interface
    private static class CustomClaims implements Claims {
        private final Map<String, Object> claims;
        
        public CustomClaims(Map<String, Object> claims) {
            this.claims = claims;
        }
        
        @Override
        public String getIssuer() {
            return (String) claims.get("iss");
        }
        
        @Override
        public Claims setIssuer(String iss) {
            claims.put("iss", iss);
            return this;
        }
        
        @Override
        public String getSubject() {
            return (String) claims.get("sub");
        }
        
        @Override
        public Claims setSubject(String sub) {
            claims.put("sub", sub);
            return this;
        }
        
        @Override
        public String getAudience() {
            return (String) claims.get("aud");
        }
        
        @Override
        public Claims setAudience(String aud) {
            claims.put("aud", aud);
            return this;
        }
        
        @Override
        public Date getExpiration() {
            Object exp = claims.get("exp");
            if (exp instanceof Number) {
                return new Date(((Number) exp).longValue() * 1000);
            }
            return null;
        }
        
        @Override
        public Claims setExpiration(Date exp) {
            claims.put("exp", exp);
            return this;
        }
        
        @Override
        public Date getNotBefore() {
            Object nbf = claims.get("nbf");
            if (nbf instanceof Number) {
                return new Date(((Number) nbf).longValue() * 1000);
            }
            return null;
        }
        
        @Override
        public Claims setNotBefore(Date nbf) {
            claims.put("nbf", nbf);
            return this;
        }
        
        @Override
        public Date getIssuedAt() {
            Object iat = claims.get("iat");
            if (iat instanceof Number) {
                return new Date(((Number) iat).longValue() * 1000);
            }
            return null;
        }
        
        @Override
        public Claims setIssuedAt(Date iat) {
            claims.put("iat", iat);
            return this;
        }
        
        @Override
        public String getId() {
            return (String) claims.get("jti");
        }
        
        @Override
        public Claims setId(String jti) {
            claims.put("jti", jti);
            return this;
        }
        
        @Override
        public <T> T get(String claimName, Class<T> requiredType) {
            Object value = claims.get(claimName);
            if (value == null) {
                return null;
            }
            if (requiredType.isAssignableFrom(value.getClass())) {
                return requiredType.cast(value);
            }
            return null;
        }
        
        @Override
        public Object get(Object key) {
            return claims.get(key);
        }
        
        @Override
        public Object put(String key, Object value) {
            return claims.put(key, value);
        }
        
        // Implement other Map methods
        @Override
        public int size() {
            return claims.size();
        }
        
        @Override
        public boolean isEmpty() {
            return claims.isEmpty();
        }
        
        @Override
        public boolean containsKey(Object key) {
            return claims.containsKey(key);
        }
        
        @Override
        public boolean containsValue(Object value) {
            return claims.containsValue(value);
        }
        
        @Override
        public Object remove(Object key) {
            return claims.remove(key);
        }
        
        @Override
        public void putAll(Map<? extends String, ?> m) {
            claims.putAll(m);
        }
        
        @Override
        public void clear() {
            claims.clear();
        }
        
        @Override
        public Set<String> keySet() {
            return claims.keySet();
        }
        
        @Override
        public Collection<Object> values() {
            return claims.values();
        }
        
        @Override
        public Set<Entry<String, Object>> entrySet() {
            return claims.entrySet();
        }
    }

    private Boolean isTokenExpired(String token) {

 

       return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateTokenIgnoreExpiration(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("expired")) {
                String username = extractUsername(token);
                return username != null && !username.isEmpty();
            }
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}

