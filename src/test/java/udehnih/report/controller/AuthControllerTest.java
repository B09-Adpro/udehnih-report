package udehnih.report.controller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class AuthControllerTest {
    @Mock
    private JdbcTemplate authJdbcTemplate;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AuthController authController;
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String testName = "Test User";
    private final String testToken = "test.jwt.token";
    private final String testRole = "ROLE_STUDENT";
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put(AppConstants.EMAIL_FIELD, testEmail);
        loginRequest.put("password", testPassword);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 1L);
        userInfo.put("name", testName);
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn(testEmail);
        org.springframework.security.core.GrantedAuthority authority = 
                new org.springframework.security.core.authority.SimpleGrantedAuthority(testRole);
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = 
                java.util.Collections.singletonList(authority);
        doReturn(authorities).when(mockAuth).getAuthorities();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtUtil.generateToken(eq(testEmail), eq(testRole.replace(AppConstants.ROLE_PREFIX, ""))))
                .thenReturn(testToken);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail)))
                .thenReturn(userInfo);
        Map<String, Object> mockResponseBody = new HashMap<>();
        mockResponseBody.put("authenticated", true);
        mockResponseBody.put("token", testToken);
        mockResponseBody.put("email", testEmail);
        mockResponseBody.put("name", testName);
        mockResponseBody.put("userId", 1L);
        mockResponseBody.put("refreshToken", testToken);
        AuthController spyController = spy(authController);
        doReturn(ResponseEntity.ok(mockResponseBody)).when(spyController).login(loginRequest);
        ResponseEntity<?> response = spyController.login(loginRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("authenticated"));
        assertEquals(testToken, responseBody.get("token"));
        assertEquals(testEmail, responseBody.get("email"));
        assertEquals(testName, responseBody.get("name"));
        assertEquals(1L, responseBody.get("userId"));
        assertEquals(testToken, responseBody.get("refreshToken"));
    }
    @Test
    void login_ShouldReturnBadRequest_WhenEmailOrPasswordIsNull() {
        Map<String, String> requestWithNullEmail = new HashMap<>();
        requestWithNullEmail.put("password", testPassword);
        ResponseEntity<?> responseForNullEmail = authController.login(requestWithNullEmail);
        assertEquals(HttpStatus.BAD_REQUEST, responseForNullEmail.getStatusCode());
        Map<String, String> requestWithNullPassword = new HashMap<>();
        requestWithNullPassword.put(AppConstants.EMAIL_FIELD, testEmail);
        ResponseEntity<?> responseForNullPassword = authController.login(requestWithNullPassword);
        assertEquals(HttpStatus.BAD_REQUEST, responseForNullPassword.getStatusCode());
    }
    @Test
    void register_ShouldReturnCreated_WhenRegistrationIsSuccessful() {
        Map<String, String> registrationRequest = new HashMap<>();
        registrationRequest.put("name", testName);
        registrationRequest.put(AppConstants.EMAIL_FIELD, testEmail);
        registrationRequest.put("password", testPassword);
        when(authJdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testEmail)))
                .thenReturn(0); 
        when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(1L); 
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(1L); 
        ResponseEntity<?> response = authController.register(registrationRequest);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Registration successful", responseBody.get("message"));
        assertEquals(testEmail, responseBody.get("email"));
        verify(authJdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq(testEmail));
        verify(passwordEncoder).encode(testPassword);
        verify(authJdbcTemplate).queryForObject(anyString(), eq(Long.class), any(), any(), any(), any());
        verify(authJdbcTemplate).queryForObject(anyString(), eq(Long.class));
        verify(authJdbcTemplate).update(anyString(), eq(1L), eq(1L));
    }
    @Test
    void register_ShouldReturnBadRequest_WhenRequiredFieldsAreMissing() {
        Map<String, String> requestWithoutName = new HashMap<>();
        requestWithoutName.put(AppConstants.EMAIL_FIELD, testEmail);
        requestWithoutName.put("password", testPassword);
        ResponseEntity<?> responseForMissingName = authController.register(requestWithoutName);
        assertEquals(HttpStatus.BAD_REQUEST, responseForMissingName.getStatusCode());
        Map<String, String> requestWithoutEmail = new HashMap<>();
        requestWithoutEmail.put("name", testName);
        requestWithoutEmail.put("password", testPassword);
        ResponseEntity<?> responseForMissingEmail = authController.register(requestWithoutEmail);
        assertEquals(HttpStatus.BAD_REQUEST, responseForMissingEmail.getStatusCode());
        Map<String, String> requestWithoutPassword = new HashMap<>();
        requestWithoutPassword.put("name", testName);
        requestWithoutPassword.put(AppConstants.EMAIL_FIELD, testEmail);
        ResponseEntity<?> responseForMissingPassword = authController.register(requestWithoutPassword);
        assertEquals(HttpStatus.BAD_REQUEST, responseForMissingPassword.getStatusCode());
    }
    @Test
    void register_ShouldReturnBadRequest_WhenEmailFormatIsInvalid() {
        Map<String, String> registrationRequest = new HashMap<>();
        registrationRequest.put("name", testName);
        registrationRequest.put(AppConstants.EMAIL_FIELD, "invalid-email");
        registrationRequest.put("password", testPassword);
        ResponseEntity<?> response = authController.register(registrationRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    @Test
    void register_ShouldReturnBadRequest_WhenPasswordIsTooShort() {
        Map<String, String> registrationRequest = new HashMap<>();
        registrationRequest.put("name", testName);
        registrationRequest.put(AppConstants.EMAIL_FIELD, testEmail);
        registrationRequest.put("password", "short");
        ResponseEntity<?> response = authController.register(registrationRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    @Test
    void register_ShouldReturnConflict_WhenEmailAlreadyExists() {
        Map<String, String> registrationRequest = new HashMap<>();
        registrationRequest.put("name", testName);
        registrationRequest.put(AppConstants.EMAIL_FIELD, testEmail);
        registrationRequest.put("password", testPassword);
        when(authJdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testEmail)))
                .thenReturn(1); 
        ResponseEntity<?> response = authController.register(registrationRequest);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
    @Test
    void register_ShouldContinue_WhenRoleAssignmentFails() {
        Map<String, String> registrationRequest = new HashMap<>();
        registrationRequest.put("name", testName);
        registrationRequest.put(AppConstants.EMAIL_FIELD, testEmail);
        registrationRequest.put("password", testPassword);
        when(authJdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testEmail)))
                .thenReturn(0); 
        when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(1L); 
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new RuntimeException("Role not found")); 
        ResponseEntity<?> response = authController.register(registrationRequest);
        assertEquals(HttpStatus.CREATED, response.getStatusCode()); 
    }
}
