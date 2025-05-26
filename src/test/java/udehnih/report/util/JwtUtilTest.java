package udehnih.report.util;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import udehnih.report.config.JwtConfig;
import java.util.ArrayList;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
class JwtUtilTest {
    @Mock
    private JwtConfig jwtConfig;
    @InjectMocks
    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private final String testEmail = "test@example.com";
    private final String testRole = "STUDENT";
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jwtConfig.getSecretKey()).thenReturn("testSecretKeyWithAtLeast32Characters12345");
        when(jwtConfig.getExpiration()).thenReturn(3600000L);
        userDetails = User.withUsername(testEmail)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
    }
    @Test
    void generateToken_ShouldCreateValidToken() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(testEmail, jwtUtil.extractUsername(token));
        String extractedRole = jwtUtil.extractRole(token);
        assertTrue(extractedRole.contains(testRole));
        assertTrue(extractedRole.startsWith(AppConstants.ROLE_PREFIX));
    }
    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertEquals(testEmail, jwtUtil.extractUsername(token));
    }
    @Test
    void extractRole_ShouldReturnRoleWithPrefix() {
        String token1 = jwtUtil.generateToken(testEmail, "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token1));
        String token2 = jwtUtil.generateToken(testEmail, "STUDENT");
        assertEquals("ROLE_STUDENT", jwtUtil.extractRole(token2));
    }
    @Test
    void extractRole_ShouldHandleNullRole() {
        JwtUtil spyUtil = spy(jwtUtil);
        doReturn(null).when(spyUtil).extractClaim(anyString(), any());
        String result = spyUtil.extractRole("dummy.token.value");
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, result);
    }
    @Test
    void extractExpiration_ShouldReturnExpirationDate() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        Date expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }
    @Test
    void validateToken_ShouldReturnFalseForInvalidUsername() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        UserDetails wrongUser = User.withUsername("wrong@example.com")
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        assertFalse(jwtUtil.validateToken(token, wrongUser));
    }
    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() {
        Date pastDate = new Date(System.currentTimeMillis() - 1000); 
        UserDetails expiredTokenUser = User.withUsername(testEmail)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        JwtUtil spyJwtUtil = spy(jwtUtil);
        doReturn(pastDate).when(spyJwtUtil).extractExpiration(anyString());
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertFalse(spyJwtUtil.validateToken(token, expiredTokenUser));
    }
    @Test
    void extractClaim_ShouldExtractCustomClaim() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        String username = jwtUtil.extractClaim(token, Claims::getSubject);
        assertEquals(testEmail, username);
    }
    @Test
    void generateAndExtractToken_ShouldHandleMultipleRoles() {
        String multipleRoles = "STUDENT,STAFF,TUTOR";
        String token = jwtUtil.generateToken(testEmail, multipleRoles);
        String extractedRoles = jwtUtil.extractRole(token);
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "STUDENT"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "STAFF"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "TUTOR"));
        assertEquals(testEmail, jwtUtil.extractUsername(token));
    }
    @Test
    void extractClaim_ShouldHandleException() {
        String invalidToken = "invalid.token.string";
        assertNull(jwtUtil.extractClaim(invalidToken, Claims::getSubject));
    }
}
