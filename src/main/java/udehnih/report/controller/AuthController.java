package udehnih.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import udehnih.report.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("authJdbcTemplate")
    private JdbcTemplate authJdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");

            // Basic validation
            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email and password are required"
                ));
            }

            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String token = jwtUtil.generateToken(email, authentication.getAuthorities().iterator().next().getAuthority());

            // Return success response with token
            return ResponseEntity.ok(Map.of(
                "token", token,
                "email", email,
                "role", authentication.getAuthorities().iterator().next().getAuthority()
            ));

        } catch (AuthenticationException e) {
            log.error("Authentication failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Invalid credentials"
            ));
        } catch (Exception e) {
            log.error("Error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Login failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody Map<String, String> registrationRequest) {
        try {
            String name = registrationRequest.get("name");
            String email = registrationRequest.get("email");
            String password = registrationRequest.get("password");

            // Basic validation
            if (name == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Name, email, and password are required"
                ));
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid email format"
                ));
            }

            // Validate password length
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Password must be at least 8 characters long"
                ));
            }

            // Check if email already exists
            String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            int count = authJdbcTemplate.queryForObject(checkEmailSql, Integer.class, email);
            if (count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Email address is already registered"
                ));
            }

            // Hash the password
            String hashedPassword = passwordEncoder.encode(password);

            // Insert new user - role will be assigned by auth service
            String insertUserSql = "INSERT INTO users (name, email, password, registration_date) VALUES (?, ?, ?, ?) RETURNING id";
            authJdbcTemplate.queryForObject(insertUserSql, Long.class, 
                name, email, hashedPassword, LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Registration successful",
                "email", email
            ));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Registration failed: " + e.getMessage()
            ));
        }
    }
} 