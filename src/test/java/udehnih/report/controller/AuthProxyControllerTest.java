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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        ReflectionTestUtils.setField(authProxyController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(authProxyController, "passwordEncoder", passwordEncoder);
    }

    @Test
    void testShouldUseExternalAuth_NullOrEmptyUrl() {
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", (String)null);
        assertFalse(result, "Should return false for null URL");
        
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", "");
        assertFalse(result, "Should return false for empty URL");
    }

    @Test
    void testShouldUseExternalAuth_LocalUrls() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        String authServiceUrl = "http://localhost:8000";
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for localhost URL with matching port");
        
        authServiceUrl = "http://localhost:8080";
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for localhost:8080");
        
        authServiceUrl = "http://127.0.0.1:8000";
        result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for 127.0.0.1 URL");
    }
    
    @Test
    void testShouldUseExternalAuth_ExternalUrl_Available() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        String authServiceUrl = "http://external-auth:8080";
        
        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.headForHeaders(eq(authServiceUrl))).thenReturn(headers);

        boolean result = ReflectionTestUtils.invokeMethod(
            authProxyController,
            "shouldUseExternalAuth",
            authServiceUrl
        );

        assertTrue(result, "Should return true for available external URL");
    }
    
    @Test
    void testShouldUseExternalAuth_ExternalUrl_Unavailable() {
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        String authServiceUrl = "http://external-auth:8080";
        
        when(restTemplate.headForHeaders(eq(authServiceUrl)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));
        
        boolean result = ReflectionTestUtils.invokeMethod(authProxyController, "shouldUseExternalAuth", authServiceUrl);
        assertFalse(result, "Should return false for unavailable external URL");
    }

    @Test
    void testForwardRequest() {
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
            ResponseEntity<Object> result = authProxyController.forwardRequest(
                    path, 
                    method, 
                    body, 
                    headers
            );

            assertNotNull(result, "Result should not be null");
            assertEquals(HttpStatus.OK, result.getStatusCode(), "Status code should be OK");
            assertEquals(responseMap, result.getBody(), "Response body should match expected");
            
            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Object.class)
            );
        } catch (Exception e) {
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
        
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid email or password", responseBody.get("error"));
    }

    
    @Test
    void testHandleLocalTokenRefresh_Success() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "old-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer old-token");
        
        when(jwtUtil.validateTokenIgnoreExpiration(eq("old-token"))).thenReturn(true);
        when(jwtUtil.extractUsername(eq("old-token"))).thenReturn("test@example.com");
        when(jwtUtil.extractRole(eq("old-token"))).thenReturn("ROLE_STUDENT");
        
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
        
        when(jwtUtil.generateToken(eq("test@example.com"), eq("ROLE_STUDENT"))).thenReturn("new-token");
        
        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalTokenRefresh",
                refreshRequest,
                headers
        );
        
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("new-token", responseBody.get("token"));
    }

    @Test
    void testHandleLocalTokenRefresh_InvalidToken() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "invalid-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer invalid-token");
        
        when(jwtUtil.validateTokenIgnoreExpiration(eq("invalid-token"))).thenReturn(false);
        
        ResponseEntity<Object> result = ReflectionTestUtils.invokeMethod(
                authProxyController,
                "handleLocalTokenRefresh",
                refreshRequest,
                headers
        );
        
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid token", responseBody.get("error"));
    }

    @Test
    void testRefreshToken_LocalAuth() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "expired-jwt-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer expired-jwt-token");

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        when(jwtUtil.validateTokenIgnoreExpiration(eq("expired-jwt-token"))).thenReturn(true);
        when(jwtUtil.extractUsername(eq("expired-jwt-token"))).thenReturn("test@example.com");
        when(jwtUtil.extractRole(eq("expired-jwt-token"))).thenReturn("ROLE_STUDENT");

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

        when(jwtUtil.generateToken(eq("test@example.com"), eq("ROLE_STUDENT")))
                .thenReturn("new-jwt-token");
        
        ResponseEntity<Object> result = authProxyController.refreshToken(refreshRequest, headers);
        
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("new-jwt-token", responseBody.get("token"));
    }

    @Test
    void testRefreshToken_ExternalAuth() {
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("token", "expired-jwt-token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer expired-jwt-token");

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "new-external-jwt-token");
        responseBody.put("user", Map.of("email", "test@example.com", "role", "STUDENT"));
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);

        ResponseEntity<Object> result = authProxyController.refreshToken(refreshRequest, headers);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("new-external-jwt-token", resultBody.get("token"));
    }
        
    @Test
    void testHandleLocalRegistration_Success() {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");

        when(authJdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users WHERE email = ?"),
                eq(Integer.class),
                eq("newuser@example.com")
        )).thenReturn(0);

        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM users WHERE email = ?"),
                eq(Long.class),
                eq("newuser@example.com")
        )).thenReturn(1L);

        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM roles WHERE name = 'STUDENT'"),
                eq(Long.class)
        )).thenReturn(2L);

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
        
        @SuppressWarnings("unchecked")
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
        
        @SuppressWarnings("unchecked")
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
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");
        HttpHeaders headers = new HttpHeaders();
        
        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "external-jwt-token");
        responseBody.put("user", Map.of("email", "test@example.com", "role", "STUDENT"));
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);
        
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("external-jwt-token", resultBody.get("token"));
    }

    @Test
    void testLogin_LocalAuth_Success() {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");
        HttpHeaders headers = new HttpHeaders();

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
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

        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertEquals("jwt-token", responseBody.get("token"));

        Map<String, Object> userObj;
        if (responseBody.containsKey("user")) {
            Object userObjRaw = responseBody.get("user");
            if (userObjRaw instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castedUserObj = (Map<String, Object>) userObjRaw;
                userObj = castedUserObj;
            } else {
                userObj = new HashMap<>();
            }
        } else {
            userObj = new HashMap<>();
            userObj.put("email", responseBody.get("email"));
            userObj.put("name", responseBody.get("name"));
            userObj.put("role", responseBody.get("roles") != null ? responseBody.get("roles") : "STUDENT");
        }

        assertNotNull(userObj.get("email"), "User email should not be null");
        assertNotNull(userObj.get("name"), "User name should not be null");
    }

    @Test
    void testLogin_LocalAuth_InvalidCredentials() {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "wrong_password");
        HttpHeaders headers = new HttpHeaders();

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
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

        ResponseEntity<Object> result = authProxyController.login(loginRequest, headers);

        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("Invalid email or password", responseBody.get("error"));
    }

    @Test
    void testRegister_LocalAuth() {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");
        HttpHeaders headers = new HttpHeaders();

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://localhost:8000");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        when(authJdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users WHERE email = ?"),
                eq(Integer.class),
                eq("newuser@example.com")
        )).thenReturn(0);

        when(passwordEncoder.encode(eq("password"))).thenReturn("encoded_password");
        
        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM users WHERE email = ?"),
                eq(Long.class),
                eq("newuser@example.com")
        )).thenReturn(1L);

        when(authJdbcTemplate.queryForObject(
                eq("SELECT id FROM roles WHERE name = 'STUDENT'"),
                eq(Long.class)
        )).thenReturn(2L);
        
        when(jwtUtil.generateToken(eq("newuser@example.com"), eq("STUDENT")))
                .thenReturn("jwt-token");

        ResponseEntity<Object> result = authProxyController.register(registerRequest, headers);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("jwt-token", responseBody.get("token"));
        
        verify(authJdbcTemplate).update(
            contains("INSERT INTO users (email, password, name) VALUES (?, ?, ?)"),
            eq("newuser@example.com"),
            eq("encoded_password"),
            eq("New User")
        );
        
        verify(authJdbcTemplate).update(
            contains("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)"),
            eq(1L),
            eq(2L)
        );
    }
    
    @Test
    void testRegister_ExternalAuth() {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password");
        registerRequest.put("name", "New User");
        HttpHeaders headers = new HttpHeaders();

        when(env.getProperty("AUTH_SERVICE_URL")).thenReturn("http://external-auth:8080");
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8000");
        
        HttpHeaders mockHeaders = new HttpHeaders();
        when(restTemplate.headForHeaders(anyString())).thenReturn(mockHeaders);

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
        
        ResponseEntity<Object> result = authProxyController.register(registerRequest, headers);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertEquals("external-jwt-token", resultBody.get("token"));
    }

}
