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
import java.util.function.Function;
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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
} 