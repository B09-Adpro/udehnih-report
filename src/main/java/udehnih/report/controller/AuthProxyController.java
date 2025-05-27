package udehnih.report.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import udehnih.report.util.JwtUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthProxyController {

    private final RestTemplate restTemplate;
    private final Environment env;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate authJdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthProxyController(Environment env, JwtUtil jwtUtil, 
                              @Qualifier("authJdbcTemplate") JdbcTemplate authJdbcTemplate) {
        this.restTemplate = new RestTemplate();
        this.env = env;
        this.jwtUtil = jwtUtil;
        this.authJdbcTemplate = authJdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    private String getAuthServiceUrl() {
        String url = env.getProperty("AUTH_SERVICE_URL");
        if (url == null || url.isEmpty()) {
            url = System.getProperty("AUTH_SERVICE_URL");
        }
        if (url == null || url.isEmpty()) {
            url = System.getenv("AUTH_SERVICE_URL");
        }
        if (url == null || url.isEmpty()) {
            url = "http://localhost:8080";
            log.warn("No AUTH_SERVICE_URL found, defaulting to self: {}", url);
        }
        return url;
    }


    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, Object> loginRequest, @RequestHeader HttpHeaders headers) {
        log.info("=== LOGIN REQUEST RECEIVED ====");
        log.debug("Request body: {}", loginRequest);
        log.debug("Request headers: {}", headers);
        
        String authServiceUrl = getAuthServiceUrl();
        boolean useExternalAuth = shouldUseExternalAuth(authServiceUrl);
        
        if (useExternalAuth) {
            try {
                log.info("Using external auth service at: {}", authServiceUrl);
                ResponseEntity<Object> response = forwardRequest("/auth/login", HttpMethod.POST, loginRequest, headers);
                log.info("External auth service response: {}", response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.warn("External auth service unavailable: {}", e.getMessage());
                log.info("Falling back to local authentication...");
            }
        } else {
            log.info("Using local authentication directly");
        }
        
        ResponseEntity<Object> response = handleLocalLogin(loginRequest);
        log.info("Local authentication response: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Object> refreshToken(@RequestBody(required = false) Map<String, Object> refreshRequest, 
                                             @RequestHeader HttpHeaders headers) {
        log.info("=== TOKEN REFRESH REQUEST RECEIVED ====");
        
        String authServiceUrl = getAuthServiceUrl();
        boolean useExternalAuth = shouldUseExternalAuth(authServiceUrl);
        
        if (useExternalAuth) {
            try {
                log.info("Using external auth service at: {}", authServiceUrl);
                ResponseEntity<Object> response = forwardRequest("/auth/refresh-token", HttpMethod.POST, refreshRequest, headers);
                log.info("External auth service response: {}", response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.warn("External auth service unavailable: {}", e.getMessage());
                log.info("Falling back to local token refresh...");
            }
        } else {
            log.info("Using local token refresh directly");
        }
        
        ResponseEntity<Object> response = handleLocalTokenRefresh(refreshRequest, headers);
        log.info("Local token refresh response: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Map<String, Object> registerRequest, 
                                         @RequestHeader HttpHeaders headers) {
        log.info("=== REGISTRATION REQUEST RECEIVED ====");
        
        String authServiceUrl = getAuthServiceUrl();
        boolean useExternalAuth = shouldUseExternalAuth(authServiceUrl);
        
        if (useExternalAuth) {
            try {
                log.info("Using external auth service at: {}", authServiceUrl);
                ResponseEntity<Object> response = forwardRequest("/auth/register", HttpMethod.POST, registerRequest, headers);
                log.info("External auth service response: {}", response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.warn("External auth service unavailable: {}", e.getMessage());
                log.info("Falling back to local registration...");
            }
        } else {
            log.info("Using local registration directly");
        }
        
        ResponseEntity<Object> response = handleLocalRegistration(registerRequest);
        log.info("Local registration response: {}", response.getStatusCode());
        return response;
    }


    private boolean shouldUseExternalAuth(String authServiceUrl) {
        if (authServiceUrl == null || authServiceUrl.isEmpty()) {
            return false;
        }

        String serverPort = env.getProperty("server.port", "8080");
        String localUrl = "http://localhost:" + serverPort;
        
        if (authServiceUrl.equals(localUrl) || 
            authServiceUrl.equals("http://localhost:8080") ||
            authServiceUrl.startsWith("http://localhost:" + serverPort)) {
            log.info("Auth service URL points to this server: {}, using local auth", authServiceUrl);
            return false;
        }

        try {
            RestTemplate pingTemplate = new RestTemplate();
            pingTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
            ((org.springframework.http.client.SimpleClientHttpRequestFactory) pingTemplate.getRequestFactory()).setConnectTimeout(1000);
            pingTemplate.headForHeaders(authServiceUrl);
            return true;
        } catch (Exception e) {
            log.warn("External auth service at {} is not available: {}", authServiceUrl, e.getMessage());
            return false;
        }
    }
    
    private ResponseEntity<Object> forwardRequest(String path, HttpMethod method, 
                                                Object body, HttpHeaders headers) {
        try {
            String authServiceUrl = getAuthServiceUrl();
            String fullUrl = authServiceUrl + path;
            
            log.info("Forwarding request to external auth service: {}", fullUrl);
            log.debug("Request method: {}", method);
            log.debug("Request body: {}", body);
            
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(fullUrl, method, requestEntity, Object.class);
            
            log.info("Response status: {}", response.getStatusCode());
            log.debug("Response body: {}", response.getBody());
            
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, value) -> responseHeaders.put(key, value));
            
            if (response.getHeaders().containsKey("Authorization")) {
                log.info("Authorization header found in response");
            }
            
            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("Error forwarding request to auth service: {}", e.getMessage(), e);
            throw new RestClientException("Error connecting to authentication service: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> handleLocalLogin(Map<String, Object> loginRequest) {
        String email = (String) loginRequest.get("email");
        String password = (String) loginRequest.get("password");
        
        if (email == null || password == null) {
            log.warn("Login attempt with missing email or password");
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        
        try {
            log.info("Attempting local login for user: {}", email);
            
            // Proceed with local authentication using the configured database connection
            
            String userSql = "SELECT id, email, password, name FROM users WHERE email = ?";
            List<Map<String, Object>> users = authJdbcTemplate.queryForList(userSql, email);
            
            if (users.isEmpty()) {
                log.warn("User {} not found in database", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password"));
            }
            
            Map<String, Object> user = users.get(0);
            String storedPassword = (String) user.get("password");
            Long userId = ((Number) user.get("id")).longValue();
            String name = (String) user.get("name");
            
            if (!passwordEncoder.matches(password, storedPassword)) {
                log.warn("Invalid password for user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
            }
            
            List<String> roles = getUserRoles(email);
            if (roles.isEmpty()) {
                log.info("No roles found for user {}, assigning default STUDENT role", email);
                roles.add("STUDENT");
            }
            
            String token = jwtUtil.generateToken(email, String.join(",", roles));
            log.info("Generated JWT token for user: {} with ID: {}", email, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", email);
            response.put("name", name);
            response.put("id", userId);
            response.put("roles", roles);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("X-Auth-Token", token);
            headers.add("X-Auth-Username", email);
            headers.add("X-User-Id", userId.toString());
            headers.add("X-User-Email", email);
            headers.add("X-User-Role", String.join(",", roles));
            headers.add("X-Auth-Role", String.join(",", roles));
            headers.add("X-Auth-Name", name);
            headers.add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            headers.add("Pragma", "no-cache");
            
            log.info("Local login successful for user: {} with ID: {}", email, userId);
            return ResponseEntity.ok().headers(headers).body(response);
        } catch (Exception e) {
            log.error("Error in local login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    private ResponseEntity<Object> handleLocalTokenRefresh(Map<String, Object> refreshRequest, HttpHeaders headers) {
        String authHeader = headers.getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Token refresh attempt with no Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No authentication token provided"));
        }
        
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        
        if (username == null) {
            log.warn("Token refresh attempt with invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
        
        log.info("Refreshing token for user: {}", username);
        
        String newToken = jwtUtil.generateToken(username, role);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("email", username);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Authorization", "Bearer " + newToken);
        responseHeaders.add("X-Auth-Token", newToken);
        
        log.info("Token refresh successful for user: {}", username);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    // Method removed as environment variable checks are now handled in AuthDataSourceConfig
    
    private ResponseEntity<Object> handleLocalRegistration(Map<String, Object> registerRequest) {
        String email = (String) registerRequest.get("email");
        String password = (String) registerRequest.get("password");
        String name = (String) registerRequest.get("name");
        
        if (email == null || password == null || name == null) {
            log.warn("Registration attempt with missing required fields");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email, password, and name are required"));
        }
        
        try {
            log.info("Attempting local registration for user: {}", email);
            
            String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            Integer count = authJdbcTemplate.queryForObject(checkSql, Integer.class, email);
            
            if (count != null && count > 0) {
                log.warn("Registration attempt for existing user: {}", email);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already exists"));
            }
            
            createUserInH2(email, password, name);
            
            String token = jwtUtil.generateToken(email, "STUDENT");
            log.info("Generated JWT token for new user: {}", email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", email);
            response.put("name", name);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("X-Auth-Token", token);
            
            log.info("Local registration successful for user: {}", email);
            return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(response);
        } catch (Exception e) {
            log.error("Error in local registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    private void createUserInH2(String email, String password, String name) {
        try {
            String encodedPassword = passwordEncoder.encode(password);
            authJdbcTemplate.update("INSERT INTO users (email, password, name) VALUES (?, ?, ?)",
                    email, encodedPassword, name);
            
            Long userId = authJdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?", Long.class, email);
            Long roleId = authJdbcTemplate.queryForObject(
                    "SELECT id FROM roles WHERE name = 'STUDENT'", Long.class);
            
            authJdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                    userId, roleId);
            
            log.info("Created user {} with role STUDENT in H2 database", email);
        } catch (Exception e) {
            log.error("Error creating user in H2: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private List<String> getUserRoles(String email) {
        try {
            List<String> roles = authJdbcTemplate.queryForList(
                    "SELECT r.name FROM roles r " +
                    "JOIN user_roles ur ON r.id = ur.role_id " +
                    "JOIN users u ON u.id = ur.user_id " +
                    "WHERE u.email = ?", String.class, email);
            log.debug("Retrieved roles for user {}: {}", email, roles);
            return roles;
        } catch (Exception e) {
            log.error("Error getting user roles: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
