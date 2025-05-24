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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            String sql = "SELECT email, password FROM users WHERE email = ?";
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
} 