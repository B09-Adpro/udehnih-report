package udehnih.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import udehnih.report.util.AppConstants;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to load user by email: {}", email);
        
        try {
            // Trim the email to handle any potential whitespace issues
            String trimmedEmail = email.trim();
            
            // Query to get basic user information
            String userSql = "SELECT id, email, password, name FROM users WHERE email = ?";
            log.debug("Executing user query: {}", userSql);
            
            // Find the user by email
            Map<String, Object> userData;
            try {
                userData = authJdbcTemplate.queryForMap(userSql, trimmedEmail);
                log.info("User found in database: {}", userData.get("email"));
            } catch (EmptyResultDataAccessException e) {
                log.error("No user found with email: {}", trimmedEmail);
                // List some users to help with debugging
                try {
                    List<Map<String, Object>> allUsers = authJdbcTemplate.queryForList("SELECT email FROM users LIMIT 5");
                    log.info("Available users in database: {}", allUsers);
                } catch (Exception listEx) {
                    log.error("Could not list users: {}", listEx.getMessage());
                }
                throw new UsernameNotFoundException("User not found with email: " + trimmedEmail);
            } catch (DataAccessException e) {
                log.error("Database error while looking up user: {}", e.getMessage(), e);
                throw new UsernameNotFoundException("Database error while looking up user: " + e.getMessage());
            }
            
            // Get the user's ID to look up their role
            Long userId = ((Number) userData.get("id")).longValue();
            
            // Query to get the user's role from the roles and user_roles tables
            String roleSql = 
                "SELECT r.name FROM roles r " +
                "JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";
            
            // Default role if none is found
            String userRole = AppConstants.STUDENT_ROLE; // Default role
            
            try {
                // Try to get the role from the database
                String role = authJdbcTemplate.queryForObject(roleSql, String.class, userId);
                if (role != null) {
                    userRole = role;
                    log.info("Found role for user {}: {}", email, userRole);
                }
            } catch (EmptyResultDataAccessException e) {
                // No role found, use the default
                log.warn("No role found for user {}, using default: {}", email, userRole);
            } catch (DataAccessException e) {
                // Error querying roles, log and use default
                log.error("Error querying roles for user {}: {}", email, e.getMessage());
                log.warn("Using default role: {}", userRole);
            }
            
            // Format the role name for Spring Security (add ROLE_ prefix)
            String roleName = AppConstants.ROLE_PREFIX + userRole.toUpperCase();
            
            // Build and return the user details
            return User.builder()
                .username((String) userData.get("email"))
                .password((String) userData.get("password"))
                .authorities(new SimpleGrantedAuthority(roleName))
                .build();
                
        } catch (Exception e) {
            log.error("Unexpected error in loadUserByUsername: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage());
        }
    }
    
    /**
     * Get user ID by email
     * @param email User's email
     * @return Optional containing user ID if found, empty otherwise
     */
    public Optional<String> getUserIdByEmail(String email) {
        try {
            String sql = "SELECT id FROM users WHERE email = ?";
            String userId = authJdbcTemplate.queryForObject(sql, String.class, email);
            return Optional.ofNullable(userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Get full user information by email
     * @param email User's email
     * @return Optional containing user information if found, empty otherwise
     */
    public Optional<Map<String, Object>> getUserInfoByEmail(String email) {
        try {
            // First get basic user info
            String sql = "SELECT id, email, name FROM users WHERE email = ?";
            Map<String, Object> userInfo = authJdbcTemplate.queryForMap(sql, email);
            
            // Get user role if available
            try {
                Long userId = ((Number) userInfo.get("id")).longValue();
                String roleSql = 
                    "SELECT r.name FROM roles r " +
                    "JOIN user_roles ur ON r.id = ur.role_id " +
                    "WHERE ur.user_id = ?";
                
                String role = authJdbcTemplate.queryForObject(roleSql, String.class, userId);
                if (role != null) {
                    userInfo.put("role", role);
                } else {
                    userInfo.put("role", AppConstants.STUDENT_ROLE); // Default role
                }
            } catch (Exception roleEx) {
                // If we can't get the role, use default
                userInfo.put("role", "STUDENT");
            }
            
            return Optional.of(userInfo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}