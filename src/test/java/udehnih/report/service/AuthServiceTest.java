package udehnih.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Mock
    private JdbcTemplate authJdbcTemplate;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void isValidUser_WithValidCredentials_ReturnsTrue() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?";
        when(authJdbcTemplate.queryForObject(
            eq(sql),
            eq(Integer.class),
            eq(TEST_EMAIL),
            eq(TEST_PASSWORD)
        )).thenReturn(1);

        // Act
        boolean result = authService.isValidUser(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertTrue(result);
    }

    @Test
    void isValidUser_WithInvalidCredentials_ReturnsFalse() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?";
        when(authJdbcTemplate.queryForObject(
            eq(sql),
            eq(Integer.class),
            eq(TEST_EMAIL),
            eq(TEST_PASSWORD)
        )).thenReturn(0);

        // Act
        boolean result = authService.isValidUser(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidUser_WhenQueryReturnsNull_ReturnsFalse() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?";
        when(authJdbcTemplate.queryForObject(
            eq(sql),
            eq(Integer.class),
            eq(TEST_EMAIL),
            eq(TEST_PASSWORD)
        )).thenReturn(null);

        // Act
        boolean result = authService.isValidUser(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertFalse(result);
    }

    @Test
    void getUserIdByEmail_WithExistingEmail_ReturnsUserId() {
        // Arrange
        String sql = "SELECT id FROM users WHERE email = ?";
        Long expectedId = 123L;
        when(authJdbcTemplate.queryForObject(
            eq(sql),
            eq(Long.class),
            eq(TEST_EMAIL)
        )).thenReturn(expectedId);

        // Act
        Long result = authService.getUserIdByEmail(TEST_EMAIL);

        // Assert
        assertEquals(expectedId, result);
    }

    @Test
    void getUserIdByEmail_WithNonExistingEmail_ReturnsNull() {
        // Arrange
        String sql = "SELECT id FROM users WHERE email = ?";
        when(authJdbcTemplate.queryForObject(
            eq(sql),
            eq(Long.class),
            eq(TEST_EMAIL)
        )).thenReturn(null);

        // Act
        Long result = authService.getUserIdByEmail(TEST_EMAIL);

        // Assert
        assertNull(result);
    }
} 