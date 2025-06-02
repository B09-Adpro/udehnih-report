package udehnih.report.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import udehnih.report.util.JwtUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

class AuthProxyControllerTest {

    @Mock
    private Environment env;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JdbcTemplate authJdbcTemplate;

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthProxyController authProxyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Ensure we're using the mocked RestTemplate
        ReflectionTestUtils.setField(authProxyController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(authProxyController, "passwordEncoder", passwordEncoder);
        
        // Set up environment for tests
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://test-auth-service:8080");
    }

    @Test
    void testGetAuthServiceUrl() {
        // Test case 1: Environment property is set
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://auth-service:8080");
        String result = ReflectionTestUtils.invokeMethod(authProxyController, "getAuthServiceUrl");
        assertEquals("http://auth-service:8080", result, "Should use AUTH_SERVICE_URL from environment");

        // Test case 2: Environment property is null, system property is set
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn(null);
        try {
            System.setProperty("AUTH_SERVICE_URL", "http://auth-service-sys:8080");
            result = ReflectionTestUtils.invokeMethod(authProxyController, "getAuthServiceUrl");
            assertEquals("http://auth-service-sys:8080", result, "Should use AUTH_SERVICE_URL from system properties");
        } finally {
            System.clearProperty("AUTH_SERVICE_URL");
        }

        // Test case 3: Environment and system properties are null, but system env is set
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn(null);
        System.clearProperty("AUTH_SERVICE_URL");
        try {
            Map<String, String> env = new HashMap<>();
            env.put("AUTH_SERVICE_URL", "http://auth-service-env:8080");
            setEnv(env);
            result = ReflectionTestUtils.invokeMethod(authProxyController, "getAuthServiceUrl");
            assertEquals("http://auth-service-env:8080", result, "Should use AUTH_SERVICE_URL from system environment");
        } catch (Exception e) {
            // If we can't set the environment variable, just log and continue
            System.out.println("Could not set environment variable: " + e.getMessage());
        }

        // Test case 4: All sources are null, should default to localhost with server.port
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn(null);
        System.clearProperty("AUTH_SERVICE_URL");
        when(env.getProperty(eq("server.port"), eq("8000"))).thenReturn("8000");
        
        result = ReflectionTestUtils.invokeMethod(authProxyController, "getAuthServiceUrl");
        assertEquals("http://localhost:8000", result, "Should default to localhost with configured port");
    }
    
    // Helper method to set environment variables for testing
    private void setEnv(Map<String, String> newEnv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newEnv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newEnv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newEnv);
                }
            }
        }
    }

    @Test
    void testShouldUseExternalAuth_NullOrEmptyUrl() {
        // Test with null URL
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", (String)null);
        assertFalse(result, "Should return false for null URL");
        
        // Test with empty URL
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", "");
        assertFalse(result, "Should return false for empty URL");
    }

    @Test
    void testShouldUseExternalAuth_LocalUrls() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Test with localhost:8000
        String authServiceUrl = "http://localhost:8000";
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for localhost URL with matching port");
        
        // Test with localhost:8080
        authServiceUrl = "http://localhost:8080";
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for localhost:8080");
        
        // Test with 127.0.0.1
        authServiceUrl = "http://127.0.0.1:8000";
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for 127.0.0.1 URL");
    }
    
    @Test
    void testShouldUseExternalAuth_ExternalUrl_Available() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Test with external URL that is available
        String authServiceUrl = "http://external-auth:8080";
        
        // Mock RestTemplate to simulate successful connection
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(headers, HttpStatus.OK);
        when(restTemplate.headForHeaders(eq(authServiceUrl))).thenReturn(headers);
        
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertTrue(result, "Should return true for available external URL");
    }
    
    @Test
    void testShouldUseExternalAuth_ExternalUrl_Unavailable() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Test with external URL that is not available
        String authServiceUrl = "http://external-auth:8080";
        
        // Mock RestTemplate to simulate connection failure
        when(restTemplate.headForHeaders(eq(authServiceUrl)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));
        
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for unavailable external URL");
    }

    @Test
    void testForwardRequest() {
        // Setup test data
        String path = "/auth/login";
        HttpMethod method = HttpMethod.POST;
        Map<String, Object> body = Map.of("email", "test@example.com", "password", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        Map<String, String> responseMap = Map.of("token", "jwt-token");
        ResponseEntity<Object> expectedResponse = new ResponseEntity<>(responseMap, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(expectedResponse);

        try {
            // Call the method directly
            ResponseEntity<Object> result = authProxyController.forwardRequest(
                    path, 
                    method, 
                    body, 
                    headers
            );

            // Verify the results
            assertNotNull(result, "Result should not be null");
            assertEquals(HttpStatus.OK, result.getStatusCode(), "Status code should be OK");
            assertEquals(responseMap, result.getBody(), "Response body should match expected");
            
            // Verify that the RestTemplate was called with the expected parameters
            verify(restTemplate).exchange(
                    anyString(), // Use anyString() for URL to be more flexible
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Object.class)
            );
        } catch (Exception e) {
            // If there's still an exception, fail with a better error message
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    void testHandleLocalLogin_Success() {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("password", "$2a$10$encoded_password");
        userMap.put("name", "Test User");
        
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, password, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);

        List<String> roles = List.of("STUDENT");
        when(authJdbcTemplate.queryForList(anyString(), eq(String.class), eq("test@example.com")))
                .thenReturn(roles);

        when(jwtUtil.generateToken(eq("test@example.com"), eq("STUDENT")))
                .thenReturn("jwt-token");
                
        when(passwordEncoder.matches(eq("password"), eq("$2a$10$encoded_password"))).thenReturn(true);

        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalLogin",
                loginRequest
        );

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("jwt-token", responseBody.get("token"));
        assertEquals("test@example.com", responseBody.get("email"));
        assertEquals("Test User", responseBody.get("name"));
    }

    @Test
    void testHandleLocalLogin_InvalidCredentials() {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "wrong_password");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("password", "$2a$10$encoded_password");
        userMap.put("name", "Test User");
        
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, password, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);
        
        when(passwordEncoder.matches(eq("wrong_password"), eq("$2a$10$encoded_password"))).thenReturn(false);

        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalLogin",
                loginRequest
        );

        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid email or password", responseBody.get("error"));
    }

    @Test
    void testHandleLocalLogin_UserNotFound() {

        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "nonexistent@example.com");
        loginRequest.put("password", "password");

        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, password, name FROM users WHERE email = ?"),
                eq("nonexistent@example.com")
        )).thenReturn(new ArrayList<>());


        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalLogin",
                loginRequest
        );


        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
    }

    @Test
    void testHandleLocalTokenRefresh_Success() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "old-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer old-token");
        
        // Mock token validation and extraction
        when(jwtUtil.validateTokenIgnoreExpiration(eq("old-token"))).thenReturn(true);
        when(jwtUtil.extractUsername(eq("old-token"))).thenReturn("test@example.com");
        when(jwtUtil.extractRole(eq("old-token"))).thenReturn("ROLE_STUDENT");
        
        // Mock user info retrieval
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("name", "Test User");
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);
        
        // Mock token generation
        when(jwtUtil.generateToken(eq("test@example.com"), eq("ROLE_STUDENT"))).thenReturn("new-token");
        
        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalTokenRefresh",
                refreshRequest,
                headers
        );
        
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("new-token", responseBody.get("token"));
    }
    
    @Test
    void testHandleLocalTokenRefresh_InvalidToken() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "invalid-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer invalid-token");
        
        // Mock token validation to fail
        when(jwtUtil.validateTokenIgnoreExpiration(eq("invalid-token"))).thenReturn(false);
        
        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalTokenRefresh",
                refreshRequest,
                headers
        );
        
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid token", responseBody.get("error"));
    }

    @Test
    void testRefreshToken_LocalAuth() {
        // Setup request and mocks
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "expired-jwt-token");
        HttpHeaders headers = new HttpHeaders();
        
        // Mock local auth service
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock token validation
        when(jwtUtil.validateTokenIgnoreExpiration(eq("expired-jwt-token"))).thenReturn(true);
        when(jwtUtil.extractUsername(eq("expired-jwt-token"))).thenReturn("test@example.com");
        when(jwtUtil.extractRole(eq("expired-jwt-token"))).thenReturn("ROLE_STUDENT");

        // Mock user info retrieval
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("name", "Test User");
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);

        // Mock new token generation
        when(jwtUtil.generateToken(eq("test@example.com"), eq("ROLE_STUDENT")))
                .thenReturn("new-jwt-token");
        
        // Execute the method
        ResponseEntity<Object> result = authProxyController.refreshToken(refreshRequest, headers);
        
        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("new-jwt-token", responseBody.get("token"));
    }
    
    @Test
    void testRefreshToken_ExternalAuth() {
        // Setup request and mocks
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "expired-jwt-token");
        HttpHeaders headers = new HttpHeaders();
        
        // Mock external auth service URL
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock successful external auth service response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "new-external-jwt-token");
        responseBody.put("user", Map.of("email", "test@example.com", "role", "STUDENT"));
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        // Mock RestTemplate for both connection check and actual request
        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute the method
        ResponseEntity<Object> result = authProxyController.refreshToken(refreshRequest, headers);
        
        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("new-external-jwt-token", resultBody.get("token"));
        
        // Verify RestTemplate was called with correct URL
        verify(restTemplate).exchange(
            eq("http://external-auth:8080/auth/refresh"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        );
    }
    
    @Test
    void testHandleLocalRegistration_Success() {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");

        // Mock user existence check
        when(authJdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users WHERE email = ?"),
                eq(Integer.class),
                eq("newuser@example.com")
        )).thenReturn(0);

        // Mock user ID query after creation
        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM users WHERE email = ?"),
                eq(Long.class),
                eq("newuser@example.com")
        )).thenReturn(1L);

        // Mock role ID query
        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM roles WHERE name = 'STUDENT'"),
                eq(Long.class)
        )).thenReturn(2L);

        // Mock password encoding
        String encodedPassword = "$2a$10$encoded_password";
        when(passwordEncoder.encode(eq("password"))).thenReturn(encodedPassword);

        when(jwtUtil.generateToken(eq("newuser@example.com"), eq("STUDENT")))
                .thenReturn("jwt-token");


        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalRegistration",
                registerRequest
        );


        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("jwt-token", responseBody.get("token"));
        assertEquals("newuser@example.com", responseBody.get("email"));
        assertEquals("New User", responseBody.get("name"));
    }

    @Test
    void testHandleLocalRegistration_UserAlreadyExists() {

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "existing@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "Existing User");

        when(authJdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users WHERE email = ?"),
                eq(Integer.class),
                eq("existing@example.com")
        )).thenReturn(1);


        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalRegistration",
                registerRequest
        );


        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("User already exists", responseBody.get("error"));
    }

    @Test
    void testCreateUserInH2() {

        String email = "newuser@example.com";
        String password = "password";
        String name = "New User";
        
        String encodedPassword = "$2a$10$encoded_password";
        when(passwordEncoder.encode(eq(password))).thenReturn(encodedPassword);

        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM users WHERE email = ?"),
                eq(Long.class),
                eq(email)
        )).thenReturn(1L);

        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM roles WHERE name = 'STUDENT'"),
                eq(Long.class)
        )).thenReturn(2L);


        ReflectionTestUtils.invokeMethod(
                authProxyController,
                "createUserInH2",
                email,
                password,
                name
        );


        verify(authJdbcTemplate, times(1)).update(
                eq("INSERT INTO users (email, password, name) VALUES (?, ?, ?)"),
                eq(email),
                eq(encodedPassword),
                eq(name)
        );

        verify(authJdbcTemplate, times(1)).update(
                eq("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)"),
                eq(1L),
                eq(2L)
        );
    }

    @Test
    void testGetUserRoles() {

        String email = "test@example.com";
        List<String> expectedRoles = List.of("STUDENT", "ADMIN");

        when(authJdbcTemplate.queryForList(
                anyString(),
                eq(String.class),
                eq(email)
        )).thenReturn(expectedRoles);


        List<String> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "getUserRoles",
                email
        );


        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("STUDENT"));
        assertTrue(result.contains("ADMIN"));
    }

    @Test
    void testGetUserRoles_Exception() {

        String email = "test@example.com";

        when(authJdbcTemplate.queryForList(
                anyString(),
                eq(String.class),
                eq(email)
        )).thenThrow(new RuntimeException("Database error"));


        List<String> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "getUserRoles",
                email
        );


        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLogin_ExternalAuth() {
        // Setup request and mocks
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");
        HttpHeaders headers = new HttpHeaders();
        
        // Mock external auth service URL
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock successful external auth service response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "external-jwt-token");
        responseBody.put("user", Map.of("email", "test@example.com", "role", "STUDENT"));
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        // Mock RestTemplate for both connection check and actual request
        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute the method
        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);
        
        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("external-jwt-token", resultBody.get("token"));
        
        // Verify RestTemplate was called with correct URL
        verify(restTemplate).exchange(
            eq("http://external-auth:8080/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        );
    }

    @Test
    void testLogin_LocalAuth_Success() {
        // Setup request and mocks
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");
        HttpHeaders headers = new HttpHeaders();

        // Mock local auth service
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock user exists in database
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("password", "$2a$10$encoded_password");
        userMap.put("name", "Test User");
        
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, password, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);

        // Mock user roles
        List<String> roles = List.of("STUDENT");
        when(authJdbcTemplate.queryForList(anyString(), eq(String.class), eq("test@example.com")))
                .thenReturn(roles);

        // Mock JWT token generation
        when(jwtUtil.generateToken(eq("test@example.com"), eq("STUDENT")))
                .thenReturn("jwt-token");
        
        // Mock password matching        
        when(passwordEncoder.matches(eq("password"), eq("$2a$10$encoded_password"))).thenReturn(true);

        // Execute the method
        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);

        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("jwt-token", responseBody.get("token"));
        assertEquals("test@example.com", ((Map<String, Object>)responseBody.get("user")).get("email"));
        assertEquals("Test User", ((Map<String, Object>)responseBody.get("user")).get("name"));
        assertEquals("STUDENT", ((Map<String, Object>)responseBody.get("user")).get("role"));
    }
    
    @Test
    void testLogin_LocalAuth_InvalidCredentials() {
        // Setup request and mocks
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "wrong_password");
        HttpHeaders headers = new HttpHeaders();

        // Mock local auth service
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock user exists in database
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("email", "test@example.com");
        userMap.put("password", "$2a$10$encoded_password");
        userMap.put("name", "Test User");
        
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(userMap);
        
        when(authJdbcTemplate.queryForList(
                contains("SELECT id, email, password, name FROM users WHERE email = ?"),
                eq("test@example.com")
        )).thenReturn(userList);

        // Mock password NOT matching        
        when(passwordEncoder.matches(eq("wrong_password"), eq("$2a$10$encoded_password"))).thenReturn(false);

        // Execute the method
        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);

        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid credentials", responseBody.get("error"));
    }

    @Test
    void testRegister_LocalAuth() {
        // Setup request and mocks
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");
        HttpHeaders headers = new HttpHeaders();

        // Mock local auth service
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock user doesn't exist yet
        when(authJdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users WHERE email = ?"),
                eq(Integer.class),
                eq("newuser@example.com")
        )).thenReturn(0);

        // Mock password encoding
        when(passwordEncoder.encode(eq("password"))).thenReturn("encoded_password");
        
        // Mock JWT token generation
        when(jwtUtil.generateToken(eq("newuser@example.com"), eq("STUDENT")))
                .thenReturn("jwt-token");

        // Execute the method
        ResponseEntity<Object> result = authProxyController.register(registerRequest, headers);

        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("jwt-token", responseBody.get("token"));
        
        // Verify user was created in database
        verify(authJdbcTemplate).update(
            contains("INSERT INTO users (email, password, name) VALUES (?, ?, ?)"),
            eq("newuser@example.com"),
            eq("encoded_password"),
            eq("New User")
        );
        
        // Verify role was assigned
        verify(authJdbcTemplate).update(
            contains("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)"),
            anyLong(),
            anyLong()
        );
    }
    
    @Test
    void testRegister_ExternalAuth() {
        // Setup request and mocks
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");
        HttpHeaders headers = new HttpHeaders();

        // Mock external auth service
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        // Mock RestTemplate for both connection check and actual request
        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);
        
        // Mock successful external auth service response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "external-jwt-token");
        responseBody.put("user", Map.of("email", "newuser@example.com", "role", "STUDENT"));
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.CREATED);
        
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute the method
        ResponseEntity<Object> result = authProxyController.register(registerRequest, headers);
        
        // Verify results
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("external-jwt-token", resultBody.get("token"));
        
        // Verify RestTemplate was called with correct URL
        verify(restTemplate).exchange(
            eq("http://external-auth:8080/auth/register"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        );
    }
}