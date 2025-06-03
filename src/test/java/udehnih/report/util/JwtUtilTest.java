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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    void generateTokenShouldCreateValidToken() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(testEmail, jwtUtil.extractUsername(token));
        String extractedRole = jwtUtil.extractRole(token);
        assertTrue(extractedRole.contains(testRole));
        assertTrue(extractedRole.startsWith(AppConstants.ROLE_PREFIX));
    }
    @Test

    void extractUsernameShouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertEquals(testEmail, jwtUtil.extractUsername(token));
    }
    @Test

    void extractRoleShouldReturnRoleWithPrefix() {
        String token1 = jwtUtil.generateToken(testEmail, "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token1));
        String token2 = jwtUtil.generateToken(testEmail, "STUDENT");
        assertEquals("ROLE_STUDENT", jwtUtil.extractRole(token2));
    }
    @Test

    void extractRoleShouldHandleNullRole() {
        JwtUtil spyUtil = spy(jwtUtil);
        doReturn(null).when(spyUtil).extractClaim(anyString(), any());
        String result = spyUtil.extractRole("dummy.token.value");
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, result);
    }
    
    @Test
    void extractRoleShouldHandleRolesArray() {
        String token = jwtUtil.generateToken(testEmail, "STUDENT,ADMIN");
        
        String extractedRoles = jwtUtil.extractRole(token);
        
        assertTrue(extractedRoles.contains("ROLE_STUDENT"));
        assertTrue(extractedRoles.contains("ROLE_ADMIN"));
    }
    
    @Test
    void extractRoleShouldHandleExceptionWhenExtractingRoles() {
        String invalidToken = "invalid.token";
        
        String result = jwtUtil.extractRole(invalidToken);
        
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, result);
    }
    
    @Test
    void extractRoleShouldHandleMultipleRoles() {
        String multipleRoles = "ADMIN,STAFF,TUTOR";
        String token = jwtUtil.generateToken(testEmail, multipleRoles);
        
        String extractedRoles = jwtUtil.extractRole(token);
        
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "ADMIN"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "STAFF"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "TUTOR"));
    }
    
    @Test
    void extractRoleShouldHandleSingleRoleWithoutPrefix() {
        String token = jwtUtil.generateToken(testEmail, "ADMIN");
        
        String extractedRole = jwtUtil.extractRole(token);
        
        assertEquals(AppConstants.ROLE_PREFIX + "ADMIN", extractedRole);
    }
    
    @Test
    void extractRoleShouldHandleSingleRoleWithPrefix() {
        String token = jwtUtil.generateToken(testEmail, "ROLE_ADMIN");
        
        String extractedRole = jwtUtil.extractRole(token);
        
        assertEquals("ROLE_ADMIN", extractedRole);
    }
    
    @Test
    void extractRoleShouldReturnDefaultRoleForMalformedToken() {
        String malformedToken = "malformed.token.string";
        

        String extractedRole = jwtUtil.extractRole(malformedToken);
        
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, extractedRole);
    }
    
    @Test
    void extractRoleShouldReturnDefaultRoleForEmptyToken() {
        String emptyToken = "";
        
        String extractedRole = jwtUtil.extractRole(emptyToken);
        
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, extractedRole);
    }
    
    @Test
    void extractRoleShouldReturnDefaultRoleForNullToken() {
        String extractedRole = jwtUtil.extractRole(null);
        
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, extractedRole);
    }
    @Test

    void extractExpirationShouldReturnExpirationDate() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        Date expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    @Test

    void validateTokenShouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }
    @Test

    void validateTokenShouldReturnFalseForInvalidUsername() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        UserDetails wrongUser = User.withUsername("wrong@example.com")
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        assertFalse(jwtUtil.validateToken(token, wrongUser));
    }
    @Test

    void validateTokenShouldReturnFalseForExpiredToken() {
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

    void extractClaimShouldExtractCustomClaim() {
        String token = jwtUtil.generateToken(testEmail, testRole);
        String username = jwtUtil.extractClaim(token, Claims::getSubject);
        assertEquals(testEmail, username);
    }
    @Test

    void generateAndExtractTokenShouldHandleMultipleRoles() {
        String multipleRoles = "STUDENT,STAFF,TUTOR";
        String token = jwtUtil.generateToken(testEmail, multipleRoles);
        String extractedRoles = jwtUtil.extractRole(token);
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "STUDENT"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "STAFF"));
        assertTrue(extractedRoles.contains(AppConstants.ROLE_PREFIX + "TUTOR"));
        assertEquals(testEmail, jwtUtil.extractUsername(token));
    }    @Test
    void extractClaimShouldHandleException() {
        String invalidToken = "invalid.token.string";
        assertNull(jwtUtil.extractClaim(invalidToken, Claims::getSubject));
    }
    
    @Test
    void validateTokenIgnoreExpirationShouldReturnTrueForExpiredTokenWithValidUsername() {
        // Create a token with a very short expiration time
        when(jwtConfig.getExpiration()).thenReturn(1L); // 1 millisecond
        String token = jwtUtil.generateToken(testEmail, testRole);
        
        // Wait for the token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Should return true because we ignore expiration and username is valid
        assertTrue(jwtUtil.validateTokenIgnoreExpiration(token));
    }
    
    @Test
    void validateTokenIgnoreExpirationShouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.string";
        assertFalse(jwtUtil.validateTokenIgnoreExpiration(invalidToken));
    }
    
    @Test
    void extractClaimsWithoutVerificationShouldWorkForExternalToken() throws Exception {
        // Create a token manually to simulate an external token
        String header = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";
        String payload = "{\"sub\":\"external@example.com\",\"role\":\"ROLE_ADMIN\",\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + "}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".invalid_signature";
        
        // This should work because it will fallback to extractClaimsWithoutVerification
        String extractedUsername = jwtUtil.extractUsername(externalToken);
        assertEquals("external@example.com", extractedUsername);
        
        String extractedRole = jwtUtil.extractRole(externalToken);
        assertEquals("ROLE_ADMIN", extractedRole);
    }
    
    @Test
    void extractClaimsWithoutVerificationShouldReturnNullForInvalidTokenFormat() {
        String invalidToken = "invalid.format";
        
        assertNull(jwtUtil.extractUsername(invalidToken));
        assertEquals(AppConstants.ROLE_PREFIX + AppConstants.STUDENT_ROLE, jwtUtil.extractRole(invalidToken));
    }
    
    @Test
    void customClaimsShouldImplementAllMethods() throws Exception {
        // Create a token that will use CustomClaims via extractClaimsWithoutVerification
        String header = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";
        String payload = "{\"sub\":\"test@example.com\",\"iss\":\"test-issuer\",\"aud\":\"test-audience\",\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + ",\"nbf\":" + (System.currentTimeMillis() / 1000) + ",\"iat\":" + (System.currentTimeMillis() / 1000) + ",\"jti\":\"test-id\",\"role\":\"ROLE_TEST\"}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".invalid_signature";
        
        // Extract claims to trigger CustomClaims creation
        Claims claims = jwtUtil.extractClaim(externalToken, claims1 -> claims1);
        
        assertNotNull(claims);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("test-audience", claims.getAudience());
        assertEquals("test-id", claims.getId());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getNotBefore());
        assertNotNull(claims.getIssuedAt());
        assertEquals("ROLE_TEST", claims.get("role", String.class));
        
        // Test map operations
        assertTrue(claims.containsKey("sub"));
        assertTrue(claims.containsValue("test@example.com"));
        assertFalse(claims.isEmpty());
        assertTrue(claims.size() > 0);
        assertNotNull(claims.keySet());
        assertNotNull(claims.values());
        assertNotNull(claims.entrySet());
        
        // Test setter methods
        claims.setSubject("new-subject");
        assertEquals("new-subject", claims.getSubject());
        
        claims.setIssuer("new-issuer");
        assertEquals("new-issuer", claims.getIssuer());
        
        claims.setAudience("new-audience");
        assertEquals("new-audience", claims.getAudience());
        
        claims.setId("new-id");
        assertEquals("new-id", claims.getId());
        
        Date newExpiration = new Date();
        claims.setExpiration(newExpiration);
        assertEquals(newExpiration, claims.get("exp"));
        
        Date newNotBefore = new Date();
        claims.setNotBefore(newNotBefore);
        assertEquals(newNotBefore, claims.get("nbf"));
        
        Date newIssuedAt = new Date();
        claims.setIssuedAt(newIssuedAt);
        assertEquals(newIssuedAt, claims.get("iat"));
        
        // Test put and get
        claims.put("custom", "value");
        assertEquals("value", claims.get("custom"));
        
        // Test putAll
        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("key1", "value1");
        newClaims.put("key2", "value2");
        claims.putAll(newClaims);
        assertEquals("value1", claims.get("key1"));
        assertEquals("value2", claims.get("key2"));
        
        // Test remove
        claims.remove("key1");
        assertNull(claims.get("key1"));
        
        // Test clear
        int sizeBefore = claims.size();
        assertTrue(sizeBefore > 0);
        claims.clear();
        assertTrue(claims.isEmpty());
        assertEquals(0, claims.size());
    }
    
    @Test
    void customClaimsShouldHandleNumericDates() throws Exception {
        // Create a token with numeric timestamps
        long currentTime = System.currentTimeMillis() / 1000;
        String payload = "{\"sub\":\"test@example.com\",\"exp\":" + (currentTime + 3600) + ",\"nbf\":" + currentTime + ",\"iat\":" + currentTime + "}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".invalid_signature";
        
        Claims claims = jwtUtil.extractClaim(externalToken, claims1 -> claims1);
        
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getNotBefore());
        assertNotNull(claims.getIssuedAt());
        
        // Verify the dates are converted correctly from timestamps
        assertTrue(claims.getExpiration().after(new Date()));
        assertTrue(claims.getNotBefore().before(new Date(System.currentTimeMillis() + 1000)));
        assertTrue(claims.getIssuedAt().before(new Date(System.currentTimeMillis() + 1000)));
    }
    
    @Test
    void customClaimsShouldReturnNullForMissingDates() throws Exception {
        // Create a token without date fields
        String payload = "{\"sub\":\"test@example.com\",\"role\":\"ROLE_TEST\"}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".invalid_signature";
        
        Claims claims = jwtUtil.extractClaim(externalToken, claims1 -> claims1);
        
        assertNotNull(claims);
        assertNull(claims.getExpiration());
        assertNull(claims.getNotBefore());
        assertNull(claims.getIssuedAt());
        assertNull(claims.getIssuer());
        assertNull(claims.getAudience());
        assertNull(claims.getId());
    }
    
    @Test
    void customClaimsShouldHandleWrongTypeForGet() throws Exception {
        String payload = "{\"sub\":\"test@example.com\",\"role\":\"ROLE_TEST\",\"number\":123}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".invalid_signature";
        
        Claims claims = jwtUtil.extractClaim(externalToken, claims1 -> claims1);
        
        // Should return null when type doesn't match
        assertNull(claims.get("role", Integer.class));
        assertNull(claims.get("number", String.class));
        
        // Should return correct value when type matches
        assertEquals("ROLE_TEST", claims.get("role", String.class));
        assertEquals(123, claims.get("number", Integer.class));
        
        // Should return null for non-existent key
        assertNull(claims.get("nonexistent", String.class));
    }
    
    @Test
    void isTokenExpiredShouldReturnTrueForExpiredToken() throws Exception {
        // Use reflection to test the private isTokenExpired method
        java.lang.reflect.Method method = JwtUtil.class.getDeclaredMethod("isTokenExpired", String.class);
        method.setAccessible(true);
        
        // Create an expired token
        when(jwtConfig.getExpiration()).thenReturn(-1000L); // Negative expiration = already expired
        String expiredToken = jwtUtil.generateToken(testEmail, testRole);
        
        Boolean result = (Boolean) method.invoke(jwtUtil, expiredToken);
        assertTrue(result);
    }
    
    @Test
    void isTokenExpiredShouldReturnFalseForValidToken() throws Exception {
        // Use reflection to test the private isTokenExpired method
        java.lang.reflect.Method method = JwtUtil.class.getDeclaredMethod("isTokenExpired", String.class);
        method.setAccessible(true);
        
        String validToken = jwtUtil.generateToken(testEmail, testRole);
        
        Boolean result = (Boolean) method.invoke(jwtUtil, validToken);
        assertFalse(result);
    }
    
    @Test
    void extractAllClaimsShouldFallbackToWithoutVerification() {
        // Create a token with a different signature that will fail verification
        String header = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";
        String payload = "{\"sub\":\"external@example.com\",\"role\":\"ROLE_EXTERNAL\"}";
        
        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String externalToken = encodedHeader + "." + encodedPayload + ".different_signature";
        
        // This should work because it falls back to extractClaimsWithoutVerification
        String username = jwtUtil.extractUsername(externalToken);
        assertEquals("external@example.com", username);
        
        String role = jwtUtil.extractRole(externalToken);
        assertEquals("ROLE_EXTERNAL", role);
    }
}
