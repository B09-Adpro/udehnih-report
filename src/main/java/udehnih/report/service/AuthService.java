package udehnih.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    public boolean isValidUser(String email, String password) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?";
        Integer count = authJdbcTemplate.queryForObject(sql, Integer.class, email, password);
        return count != null && count > 0;
    }

    public Long getUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        return authJdbcTemplate.queryForObject(sql, Long.class, email);
    }
} 