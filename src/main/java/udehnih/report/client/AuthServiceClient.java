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
@Service

@Slf4j
public class AuthServiceClient {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired

    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    public UserInfo validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                log.warn("Username could not be extracted from token");
                return null;
            }
            String role = jwtUtil.extractRole(token);
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer count = authJdbcTemplate.queryForObject(sql, Integer.class, username);
            if (count == null || count == 0) {
                log.warn("User {} not found in auth database", username);
                return null;
            }
            try {
                return fetchUserDetails(username, role);
            } catch (Exception e) {
                log.warn("Error retrieving additional user information: {}", e.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return null;
        }
    }
    private UserInfo fetchUserDetails(String username, String role) {
        String userIdSql = "SELECT id FROM users WHERE email = ?";
        Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, username);
        String userNameSql = "SELECT name FROM users WHERE email = ?";
        String name = authJdbcTemplate.queryForObject(userNameSql, String.class, username);
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
    }

    public UserInfo getUserByEmail(String email) {
        try {
            // Check if the email is actually a numeric user ID
            if (email != null && email.matches("\\d+")) {
                log.info("Email appears to be a numeric ID: {}, trying to look up by ID", email);
                return getUserById(Long.parseLong(email));
            }
            
            String countSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer count = authJdbcTemplate.queryForObject(countSql, Integer.class, email);
            if (count == null || count == 0) {
                return null;
            }
            String userIdSql = "SELECT id FROM users WHERE email = ?";
            Long userId = authJdbcTemplate.queryForObject(userIdSql, Long.class, email);
            String userNameSql = "SELECT name FROM users WHERE email = ?";
            String name = authJdbcTemplate.queryForObject(userNameSql, String.class, email);
            String rolesSql = "SELECT r.name FROM roles r " +
                             "JOIN user_roles ur ON r.id = ur.role_id " +
                             "JOIN users u ON u.id = ur.user_id " +
                             "WHERE u.email = ?";
            List<String> roles = new ArrayList<>();
            try {
                roles = authJdbcTemplate.queryForList(rolesSql, String.class, email);
                if (roles.isEmpty()) {
                    roles.add("STUDENT"); 
                }
            } catch (Exception e) {
                log.warn("Error retrieving user roles: {}", e.getMessage());
                roles.add("STUDENT"); 
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
    
    public UserInfo getUserById(Long userId) {
        try {
            String countSql = "SELECT COUNT(*) FROM users WHERE id = ?";
            Integer count = authJdbcTemplate.queryForObject(countSql, Integer.class, userId);
            if (count == null || count == 0) {
                log.warn("User with ID {} not found", userId);
                return null;
            }
            
            String userEmailSql = "SELECT email FROM users WHERE id = ?";
            String email = authJdbcTemplate.queryForObject(userEmailSql, String.class, userId);
            
            String userNameSql = "SELECT name FROM users WHERE id = ?";
            String name = authJdbcTemplate.queryForObject(userNameSql, String.class, userId);
            
            String rolesSql = "SELECT r.name FROM roles r " +
                             "JOIN user_roles ur ON r.id = ur.role_id " +
                             "JOIN users u ON u.id = ur.user_id " +
                             "WHERE u.id = ?";
            List<String> roles = new ArrayList<>();
            try {
                roles = authJdbcTemplate.queryForList(rolesSql, String.class, userId);
                if (roles.isEmpty()) {
                    roles.add("STUDENT"); 
                }
            } catch (Exception e) {
                log.warn("Error retrieving user roles for ID {}: {}", userId, e.getMessage());
                roles.add("STUDENT"); 
            }
            
            log.info("Successfully retrieved user by ID {}: email={}, roles={}", userId, email, roles);
            return UserInfo.builder()
                .id(userId)
                .email(email)
                .name(name)
                .roles(roles)
                .build();
        } catch (Exception e) {
            log.error("Error retrieving user information for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
