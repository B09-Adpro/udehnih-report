package udehnih.report.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.model.UserInfo;
import udehnih.report.util.AppConstants;
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private AuthServiceClient authServiceClient;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to load user by email: {}", email);
        try {
            String trimmedEmail = email.trim();
            UserInfo userInfo = authServiceClient.getUserByEmail(trimmedEmail);
            if (userInfo == null) {
                log.error("No user found with email: {}", trimmedEmail);
                throw new UsernameNotFoundException("User not found with email: " + trimmedEmail);
            }
            log.info("User found in database: {}", userInfo.getEmail());
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
                for (String role : userInfo.getRoles()) {
                    String roleName = role.startsWith(AppConstants.ROLE_PREFIX) ? 
                                      role : AppConstants.ROLE_PREFIX + role.toUpperCase();
                    authorities.add(new SimpleGrantedAuthority(roleName));
                    log.info("Added role for user {}: {}", email, roleName);
                }
            } else {
                String defaultRole = AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE;
                authorities.add(new SimpleGrantedAuthority(defaultRole));
                log.warn("No roles found for user {}, using default: {}", email, defaultRole);
            }
            return User.builder()
                .username(userInfo.getEmail())
                .password("") 
                .authorities(authorities)
                .build();
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in loadUserByUsername: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage());
        }
    }
    public Optional<String> getUserIdByEmail(String email) {
        try {
            UserInfo userInfo = authServiceClient.getUserByEmail(email);
            if (userInfo != null && userInfo.getId() != null) {
                return Optional.of(userInfo.getId().toString());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    public Optional<Map<String, Object>> getUserInfoByEmail(String email) {
        try {
            UserInfo userInfo = authServiceClient.getUserByEmail(email);
            if (userInfo == null) {
                return Optional.empty();
            }
            Map<String, Object> userInfoMap = new HashMap<>();
            userInfoMap.put("id", userInfo.getId());
            userInfoMap.put("email", userInfo.getEmail());
            userInfoMap.put("name", userInfo.getName());
            if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
                userInfoMap.put("role", userInfo.getRoles().get(0));
                StringBuilder allRoles = new StringBuilder();
                for (String role : userInfo.getRoles()) {
                    if (allRoles.length() > 0) {
                        allRoles.append(",");
                    }
                    allRoles.append(role);
                }
                userInfoMap.put("allRoles", allRoles.toString());
                log.info("Set all roles for user {}: {}", email, allRoles.toString());
            } else {
                userInfoMap.put("role", AppConstants.STUDENT_ROLE);
                userInfoMap.put("allRoles", AppConstants.STUDENT_ROLE);
                log.warn("No roles found for user {}, using default: {}", email, AppConstants.STUDENT_ROLE);
            }
            return Optional.of(userInfoMap);
        } catch (Exception e) {
            log.error("Error getting user info for {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }
}