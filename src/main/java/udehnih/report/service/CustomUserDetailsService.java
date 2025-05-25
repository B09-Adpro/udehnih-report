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
            log.info("User ID for {}: {}", email, userId);
            
            // Query to get all user's roles from the roles and user_roles tables
            String roleSql = 
                "SELECT r.name FROM roles r " +
                "JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";
            
            log.info("Executing role query for loadUserByUsername: {}", roleSql);
            
            // List to store all user roles
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            
            try {
                // Try to get all roles from the database
                log.info("About to execute queryForList for roles with userId: {}", userId);
                List<String> roles;
                try {
                    roles = authJdbcTemplate.queryForList(roleSql, String.class, userId);
                    log.info("Successfully retrieved {} roles for user {}: {}", roles.size(), email, roles);
                } catch (org.springframework.dao.IncorrectResultSizeDataAccessException ex) {
                    // This exception occurs when queryForObject is used but multiple results are found
                    // Let's handle it by getting all roles instead
                    log.warn("Multiple roles found for user {}, switching to queryForList", email);
                    roles = authJdbcTemplate.queryForList(roleSql, String.class, userId);
                    log.info("Successfully retrieved {} roles using queryForList: {}", roles.size(), roles);
                } catch (Exception ex) {
                    log.error("Exception in queryForList for roles: {}", ex.getMessage(), ex);
                    throw ex;
                }
                
                if (roles != null && !roles.isEmpty()) {
                    // Add all roles found in the database
                    for (String role : roles) {
                        // Format the role name for Spring Security (add ROLE_ prefix if not already present)
                        String roleName = role.startsWith(AppConstants.ROLE_PREFIX) ? 
                                          role : AppConstants.ROLE_PREFIX + role.toUpperCase();
                        authorities.add(new SimpleGrantedAuthority(roleName));
                        log.info("Added role for user {}: {}", email, roleName);
                    }
                } else {
                    // No roles found, use the default STUDENT role
                    String defaultRole = AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE;
                    authorities.add(new SimpleGrantedAuthority(defaultRole));
                    log.warn("No roles found for user {}, using default: {}", email, defaultRole);
                }
            } catch (DataAccessException e) {
                // Error querying roles, log and use default
                log.error("Error querying roles for user {}: {}", email, e.getMessage(), e);
                String defaultRole = AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE;
                authorities.add(new SimpleGrantedAuthority(defaultRole));
                log.warn("Using default role due to error: {}", defaultRole);
            }
            
            // Build and return the user details with all authorities
            return User.builder()
                .username((String) userData.get("email"))
                .password((String) userData.get("password"))
                .authorities(authorities)
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
            
            // Get user roles if available
            try {
                Long userId = ((Number) userInfo.get("id")).longValue();
                String roleSql = 
                    "SELECT r.name FROM roles r " +
                    "JOIN user_roles ur ON r.id = ur.role_id " +
                    "WHERE ur.user_id = ?";
                
                log.info("Executing role query for user ID {}: {}", userId, roleSql);
                
                // Get all roles for the user
                List<String> roles;
                try {
                    roles = authJdbcTemplate.queryForList(roleSql, String.class, userId);
                    log.info("Found {} roles for user {}: {}", roles.size(), email, roles);
                } catch (org.springframework.dao.IncorrectResultSizeDataAccessException ex) {
                    // This exception occurs when queryForObject is used but multiple results are found
                    // Let's handle it by getting all roles instead
                    log.warn("Multiple roles found for user {}, switching to queryForList", email);
                    roles = authJdbcTemplate.queryForList(roleSql, String.class, userId);
                    log.info("Successfully retrieved {} roles using queryForList: {}", roles.size(), roles);
                } catch (Exception e) {
                    log.error("Error in queryForList for roles: {}", e.getMessage(), e);
                    throw e;
                }
                
                if (roles != null && !roles.isEmpty()) {
                    // Store the primary role (first one) for backward compatibility
                    userInfo.put("role", roles.get(0));
                    log.info("Set primary role for user {}: {}", email, roles.get(0));
                    
                    // Store all roles as a comma-separated string
                    StringBuilder allRoles = new StringBuilder();
                    for (String role : roles) {
                        if (allRoles.length() > 0) {
                            allRoles.append(",");
                        }
                        allRoles.append(role);
                    }
                    userInfo.put("allRoles", allRoles.toString());
                    log.info("Set all roles for user {}: {}", email, allRoles.toString());
                } else {
                    // No roles found, use default
                    userInfo.put("role", AppConstants.STUDENT_ROLE);
                    userInfo.put("allRoles", AppConstants.STUDENT_ROLE);
                    log.warn("No roles found for user {}, using default: {}", email, AppConstants.STUDENT_ROLE);
                }
            } catch (Exception roleEx) {
                // If we can't get the roles, use default
                log.error("Error getting roles for user {}: {}", email, roleEx.getMessage(), roleEx);
                userInfo.put("role", AppConstants.STUDENT_ROLE);
                userInfo.put("allRoles", AppConstants.STUDENT_ROLE);
                log.warn("Using default role for user {} due to error: {}", email, AppConstants.STUDENT_ROLE);
            }
            
            return Optional.of(userInfo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}