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
import udehnih.report.service.CustomUserDetailsService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    
    @Autowired
    private CustomUserDetailsService userDetailsService;

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

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user info with all roles
            Optional<Map<String, Object>> userInfoOpt = userDetailsService.getUserInfoByEmail(email);
            if (!userInfoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "User not found"));
            }
            
            Map<String, Object> userInfo = userInfoOpt.get();
            
            // Get primary role and all roles
            String primaryRole = (String) userInfo.getOrDefault("role", AppConstants.STUDENT_ROLE);
            String allRolesStr = (String) userInfo.getOrDefault("allRoles", primaryRole);
            
            // Ensure roles have ROLE_ prefix
            if (!primaryRole.startsWith(AppConstants.ROLE_PREFIX)) {
                primaryRole = AppConstants.ROLE_PREFIX + primaryRole;
            }
            
            // Generate JWT token with all roles
            String token = jwtUtil.generateToken(email, allRolesStr);
            
            // Set token in response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(AppConstants.AUTHORIZATION_HEADER, AppConstants.BEARER_PREFIX + token);
            
            // Set role headers for CORS
            headers.add("X-Auth-Role", primaryRole);
            headers.add("X-Auth-Roles", allRolesStr);
            
            // Set other auth headers
            headers.add("X-Auth-Status", "authenticated");
            headers.add("X-Auth-Username", email);
            headers.add("X-Auth-Name", (String) userInfo.get("name"));
            headers.add("X-Auth-Token", token);
            headers.add("X-User-Id", userInfo.get("id").toString());
            
            // Add Access-Control-Expose-Headers to ensure frontend can read these headers
            headers.add("Access-Control-Expose-Headers", 
                "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Roles, X-Auth-Name, X-Auth-Token, X-User-Id");
            
            // Create a response that exactly matches what the frontend expects
            Map<String, Object> responseBody = new HashMap<>();
            
            // Core authentication data
            responseBody.put("authenticated", true);
            responseBody.put("token", token);
            responseBody.put("email", email);
            responseBody.put("name", userInfo.get("name"));
            
            // IMPORTANT: Use userId instead of id to match frontend expectations
            responseBody.put("userId", userInfo.get("id"));
            responseBody.put("id", userInfo.get("id")); // For backward compatibility
            
            // Process roles for response
            String primaryRoleWithoutPrefix = primaryRole.replace(AppConstants.ROLE_PREFIX, "");
            responseBody.put("role", primaryRoleWithoutPrefix);
            
            // Process all roles for the roles array
            String[] allRoles = allRolesStr.split(",");
            String[] processedRoles = new String[allRoles.length];
            
            for (int i = 0; i < allRoles.length; i++) {
                processedRoles[i] = allRoles[i].replace(AppConstants.ROLE_PREFIX, "");
            }
            
            // Include all roles as a list
            responseBody.put("roles", processedRoles);
            
            // Include refreshToken for frontend compatibility
            responseBody.put("refreshToken", token);
            
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