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
        // Generate token
        String token = jwtUtil.generateToken(testEmail, testRole);
        
        // Verify token is not null or empty
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify username can be extracted
        assertEquals(testEmail, jwtUtil.extractUsername(token));
        
        // Verify role is correctly set
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
        // Test with role that already has prefix
        String token1 = jwtUtil.generateToken(testEmail, "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token1));
        
        // Test with role that doesn't have prefix
        String token2 = jwtUtil.generateToken(testEmail, "STUDENT");
        assertEquals("ROLE_STUDENT", jwtUtil.extractRole(token2));
    }
    
    @Test
    void extractRole_ShouldHandleNullRole() {
        // Instead of trying to access private methods, we'll test the behavior
        // by mocking the JwtUtil and verifying it returns the default role
        // when a null role is encountered
        
        // Create a JwtUtil spy that will return null for the role claim
        JwtUtil spyUtil = spy(jwtUtil);
        
        // When extractClaim is called to get the role, return null
        doReturn(null).when(spyUtil).extractClaim(anyString(), any());
        
        // Call extractRole which should handle the null case
        String result = spyUtil.extractRole("dummy.token.value");
        
        // Verify it returns the default role with prefix
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
        // Create a token with a past expiration date
        Date pastDate = new Date(System.currentTimeMillis() - 1000); // 1 second in the past
        
        // Create a UserDetails instance for this test
        UserDetails expiredTokenUser = User.withUsername(testEmail)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        
        // Create a spy of the JwtUtil to override the extractExpiration method
        JwtUtil spyJwtUtil = spy(jwtUtil);
        doReturn(pastDate).when(spyJwtUtil).extractExpiration(anyString());
        
        // Generate a token
        String token = jwtUtil.generateToken(testEmail, testRole);
        
        // Verify the token is considered expired
        assertFalse(spyJwtUtil.validateToken(token, expiredTokenUser));
    }
    
    @Test
    void extractClaim_ShouldExtractCustomClaim() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        String username = jwtUtil.extractClaim(token, Claims::getSubject);
        assertEquals(testEmail, username);
    }
    
    @Test
    void extractClaim_ShouldHandleException() {
        // Test with an invalid token
        String invalidToken = "invalid.token.string";
        assertNull(jwtUtil.extractClaim(invalidToken, Claims::getSubject));
    }
}
