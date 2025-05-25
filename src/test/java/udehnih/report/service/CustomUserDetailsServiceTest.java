package udehnih.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import udehnih.report.util.AppConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock
    private JdbcTemplate authJdbcTemplate;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private final String testEmail = "test@example.com";
    private final String testPassword = "hashedPassword";
    private final String testName = "Test User";
    private final Long testId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Arrange
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("password", testPassword);
        userData.put("name", testName);

        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim()))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId))).thenReturn("STUDENT");

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        // Assert
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        
        // Verify interactions
        verify(authJdbcTemplate).queryForMap(anyString(), eq(testEmail.trim()));
        verify(authJdbcTemplate).queryForObject(anyString(), eq(String.class), eq(testId));
    }

    @Test
    void loadUserByUsername_ShouldUseDefaultRole_WhenRoleNotFound() {
        // Arrange
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("password", testPassword);
        userData.put("name", testName);

        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim()))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        // Assert
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim())))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(testEmail));
    }

    @Test
    void getUserIdByEmail_ShouldReturnUserId_WhenUserExists() {
        // Arrange
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenReturn(testId.toString());

        // Act
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testId.toString(), result.get());
    }

    @Test
    void getUserIdByEmail_ShouldReturnEmptyOptional_WhenUserNotFound() {
        // Arrange
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getUserInfoByEmail_ShouldReturnUserInfo_WhenUserExists() {
        // Arrange
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("name", testName);

        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId)))
                .thenReturn("STUDENT");

        // Act
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);

        // Assert
        assertTrue(result.isPresent());
        Map<String, Object> userInfo = result.get();
        assertEquals(testId, userInfo.get("id"));
        assertEquals(testEmail, userInfo.get("email"));
        assertEquals(testName, userInfo.get("name"));
        assertEquals("STUDENT", userInfo.get("role"));
    }

    @Test
    void getUserInfoByEmail_ShouldUseDefaultRole_WhenRoleNotFound() {
        // Arrange
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("name", testName);

        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);

        // Assert
        assertTrue(result.isPresent());
        Map<String, Object> userInfo = result.get();
        assertEquals(testId, userInfo.get("id"));
        assertEquals(testEmail, userInfo.get("email"));
        assertEquals(testName, userInfo.get("name"));
        assertEquals(AppConstants.STUDENT_ROLE, userInfo.get("role"));
    }

    @Test
    void getUserInfoByEmail_ShouldReturnEmptyOptional_WhenUserNotFound() {
        // Arrange
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);

        // Assert
        assertFalse(result.isPresent());
    }
}
