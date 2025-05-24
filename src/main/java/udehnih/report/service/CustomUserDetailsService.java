package udehnih.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            String sql = "SELECT id, email, password FROM users WHERE email = ?";
            Map<String, Object> user = authJdbcTemplate.queryForMap(sql, email);

            return User.builder()
                .username((String) user.get("email"))
                .password((String) user.get("password"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .build();
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email);
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
} 