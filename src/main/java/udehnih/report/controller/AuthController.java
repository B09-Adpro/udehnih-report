package udehnih.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
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
            String email = loginRequest.get(AppConstants.EMAIL_FIELD);
            String password = loginRequest.get("password");

            // Basic validation
            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    AppConstants.ERROR_KEY, "Email and password are required"
                ));
            }


            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);


            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String token = jwtUtil.generateToken(email, role);
            
            String sql = "SELECT id, name FROM users WHERE email = ?";
            Map<String, Object> userInfo = authJdbcTemplate.queryForMap(sql, email);
            
            // Create the response with headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("X-Auth-Status", "authenticated");
            headers.add("X-Auth-Username", email);
            headers.add("X-Auth-Role", role.replace(AppConstants.ROLE_PREFIX, ""));
            headers.add("X-Auth-Name", (String) userInfo.get("name"));
            headers.add("X-Auth-Token", token);
            headers.add("X-User-Id", userInfo.get("id").toString());
            
            // Add Access-Control-Expose-Headers to ensure frontend can read these headers
            headers.add("Access-Control-Expose-Headers", 
                "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-Auth-Token, X-User-Id");
            
            // Create a response that exactly matches what the frontend expects
            Map<String, Object> responseBody = new HashMap<String, Object>();
            
            // Core authentication data
            responseBody.put("authenticated", true);
            responseBody.put("token", token);
            responseBody.put("email", email);
            responseBody.put("name", userInfo.get("name"));
            
            // IMPORTANT: Use userId instead of id to match frontend expectations
            responseBody.put("userId", userInfo.get("id"));
            
            // Include refreshToken for frontend compatibility
            responseBody.put("refreshToken", token);
            
            // Add roles as an array (for frontend compatibility)
            // This is critical - frontend expects a roles array, not a single role string
            java.util.List<String> roles = new java.util.ArrayList<String>();
            roles.add(role.replace(AppConstants.ROLE_PREFIX, ""));
            responseBody.put("roles", roles);
            
            // For backward compatibility, also include these fields
            responseBody.put("id", userInfo.get("id"));
            responseBody.put("role", role.replace(AppConstants.ROLE_PREFIX, ""));
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);

        } catch (AuthenticationException e) {
            log.error("Authentication failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                AppConstants.ERROR_KEY, "Invalid credentials"
            ));
        } catch (Exception e) {
            log.error("Error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                AppConstants.ERROR_KEY, "Login failed: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody Map<String, String> registrationRequest) {
        try {
            String name = registrationRequest.get("name");
            String email = registrationRequest.get(AppConstants.EMAIL_FIELD);
            String password = registrationRequest.get("password");

            // Basic validation
            if (name == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    AppConstants.ERROR_KEY, "Name, email, and password are required"
                ));
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(Map.of(
                    AppConstants.ERROR_KEY, "Invalid email format"
                ));
            }

            // Validate password length
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                    AppConstants.ERROR_KEY, "Password must be at least 8 characters long"
                ));
            }

            // Check if email already exists
            String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            int count = authJdbcTemplate.queryForObject(checkEmailSql, Integer.class, email);
            if (count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    AppConstants.ERROR_KEY, "Email address is already registered"
                ));
            }

            // Hash the password
            String hashedPassword = passwordEncoder.encode(password);

            // Insert new user
            String insertUserSql = "INSERT INTO users (name, email, password, registration_date) VALUES (?, ?, ?, ?) RETURNING id";
            Long userId = authJdbcTemplate.queryForObject(insertUserSql, Long.class, 
                name, email, hashedPassword, LocalDateTime.now());
            
            // Assign STUDENT role to the new user
            try {
                // Get the STUDENT role ID
                String getRoleIdSql = "SELECT id FROM roles WHERE name = '" + AppConstants.STUDENT_ROLE + "'";
                Long roleId = authJdbcTemplate.queryForObject(getRoleIdSql, Long.class);
                
                // Assign role to user
                String assignRoleSql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
                authJdbcTemplate.update(assignRoleSql, userId, roleId);
                
                log.info("Assigned {} role to new user: {}", AppConstants.STUDENT_ROLE, email);
            } catch (Exception roleEx) {
                log.error("Failed to assign {} role to user: {}", AppConstants.STUDENT_ROLE, email, roleEx);
                // Continue with registration even if role assignment fails
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Registration successful",
                "email", email
            ));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.internalServerError().body(Map.of(
                AppConstants.ERROR_KEY, "Registration failed: " + e.getMessage()
            ));
        }
    }
} 