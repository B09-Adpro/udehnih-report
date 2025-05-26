package udehnih.report.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import udehnih.report.model.UserInfo;
import udehnih.report.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for interacting with the Auth microservice.
 * This implementation uses direct database access since we don't have the auth service URL.
 */
@Service
@Slf4j
public class AuthServiceClient {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;
    
    /**
     * Validates a JWT token and returns user information if valid
     * 
     * @param token JWT token to validate
     * @return UserInfo object or null if token is invalid
     */
    public UserInfo validateToken(String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                log.warn("Username could not be extracted from token");
                return null;
            }
            
            // Extract role from token
            String role = jwtUtil.extractRole(token);
            
            // Verify user exists in auth database
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer count = authJdbcTemplate.queryForObject(sql, Integer.class, username);
            
            if (count == null || count == 0) {
                log.warn("User {} not found in auth database", username);
                return null;
            }
            
            // Get user details from auth database
            try {
                // Get user ID
                String userIdSql = "SELECT id FROM users WHERE email = ?";
                Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, username);
                
                // Get user name
                String userNameSql = "SELECT name FROM users WHERE email = ?";
                String name = authJdbcTemplate.queryForObject(userNameSql, String.class, username);
                
                // Parse roles from the token
                List<String> roles = new ArrayList<>();
                for (String r : role.split(",")) {
                    roles.add(r.trim());
                }
                
                return UserInfo.builder()
                    .id(userId)
                    .email(username)
                    .name(name)
                    .roles(roles)
                    .build();
            } catch (Exception e) {
                log.warn("Error retrieving additional user information: {}", e.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets user information by email
     * 
     * @param email User email
     * @return UserInfo object or null if user not found
     */
    public UserInfo getUserByEmail(String email) {
        try {
            // Check if user exists
            String countSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer count = authJdbcTemplate.queryForObject(countSql, Integer.class, email);
            
            if (count == null || count == 0) {
                return null;
            }
            
            // Get user ID
            String userIdSql = "SELECT id FROM users WHERE email = ?";
            Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, email);
            
            // Get user name
            String userNameSql = "SELECT name FROM users WHERE email = ?";
            String name = authJdbcTemplate.queryForObject(userNameSql, String.class, email);
            
            // Get user roles
            String rolesSql = "SELECT r.name FROM roles r " +
                             "JOIN user_roles ur ON r.id = ur.role_id " +
                             "JOIN users u ON u.id = ur.user_id " +
                             "WHERE u.email = ?";
            
            List<String> roles = new ArrayList<>();
            try {
                roles = authJdbcTemplate.queryForList(rolesSql, String.class, email);
                if (roles.isEmpty()) {
                    roles.add("STUDENT"); // Default role
                }
            } catch (Exception e) {
                log.warn("Error retrieving user roles: {}", e.getMessage());
                roles.add("STUDENT"); // Default role
            }
            
            return UserInfo.builder()
                .id(userId)
                .email(email)
                .name(name)
                .roles(roles)
                .build();
        } catch (Exception e) {
            log.error("Error retrieving user information: {}", e.getMessage());
            return null;
        }
    }
}
