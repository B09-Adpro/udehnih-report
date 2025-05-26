package udehnih.report.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import udehnih.report.model.UserInfo;
import udehnih.report.util.JwtUtil;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceClientTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JdbcTemplate authJdbcTemplate;

    @InjectMocks
    private AuthServiceClient authServiceClient;

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ROLE = "ROLE_STUDENT";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_NAME = "Test User";

    @BeforeEach
    void setUp() {
    }

    @Test
    void validateTokenSuccess() {
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(TEST_ROLE);
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(1);
        when(authJdbcTemplate.queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL))).thenReturn(TEST_USER_ID);
        when(authJdbcTemplate.queryForObject(eq("SELECT name FROM users WHERE email = ?"), 
                eq(String.class), eq(TEST_EMAIL))).thenReturn(TEST_NAME);

        UserInfo result = authServiceClient.validateToken(TEST_TOKEN);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_NAME, result.getName());
        assertTrue(result.getRoles().contains(TEST_ROLE));
        
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).extractRole(TEST_TOKEN);
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq("SELECT id FROM users WHERE email = ?"), eq(Long.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq("SELECT name FROM users WHERE email = ?"), eq(String.class), eq(TEST_EMAIL));
    }

    @Test
    void validateTokenNullUsername() {
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(null);

        UserInfo result = authServiceClient.validateToken(TEST_TOKEN);

        assertNull(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil, never()).extractRole(anyString());
        verify(authJdbcTemplate, never()).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), eq(Integer.class), eq(TEST_EMAIL));
    }

    @Test
    void validateTokenUserNotFound() {
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(TEST_ROLE);
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(0);

        UserInfo result = authServiceClient.validateToken(TEST_TOKEN);

        assertNull(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).extractRole(TEST_TOKEN);
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate, never()).queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL));
    }

    @Test
    void validateTokenExceptionDuringUserInfoRetrieval() {
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(TEST_ROLE);
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(1);
        when(authJdbcTemplate.queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL))).thenThrow(new RuntimeException("Database error"));

        UserInfo result = authServiceClient.validateToken(TEST_TOKEN);

        assertNull(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).extractRole(TEST_TOKEN);
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL));
    }

    @Test
    void getUserByEmailSuccess() {
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(1);
        when(authJdbcTemplate.queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL))).thenReturn(TEST_USER_ID);
        when(authJdbcTemplate.queryForObject(eq("SELECT name FROM users WHERE email = ?"), 
                eq(String.class), eq(TEST_EMAIL))).thenReturn(TEST_NAME);
        
        List<String> roles = Arrays.asList("ROLE_STUDENT");
        String rolesSql = "SELECT r.name FROM roles r " +
                         "JOIN user_roles ur ON r.id = ur.role_id " +
                         "JOIN users u ON u.id = ur.user_id " +
                         "WHERE u.email = ?";
        when(authJdbcTemplate.queryForList(eq(rolesSql), eq(String.class), eq(TEST_EMAIL)))
                .thenReturn(roles);

        UserInfo result = authServiceClient.getUserByEmail(TEST_EMAIL);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(1, result.getRoles().size());
        assertEquals("ROLE_STUDENT", result.getRoles().get(0));
    }

    @Test
    void getUserByEmailUserNotFound() {
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(0);

        UserInfo result = authServiceClient.getUserByEmail(TEST_EMAIL);

        assertNull(result);
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate, never()).queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL));
    }

    @Test
    void getUserByEmailExceptionDuringUserInfoRetrieval() {
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenThrow(new RuntimeException("Database error"));

        UserInfo result = authServiceClient.getUserByEmail(TEST_EMAIL);

        assertNull(result);
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL));
    }

    @Test
    void getUserByEmailNoRolesFoundDefaultsToStudent() {
        String countSql = "SELECT COUNT(*) FROM users WHERE email = ?";
        when(authJdbcTemplate.queryForObject(eq(countSql), eq(Integer.class), eq(TEST_EMAIL)))
                .thenReturn(1);
                
        String userIdSql = "SELECT id FROM users WHERE email = ?";
        when(authJdbcTemplate.queryForObject(eq(userIdSql), eq(Long.class), eq(TEST_EMAIL)))
                .thenReturn(TEST_USER_ID);
                
        String userNameSql = "SELECT name FROM users WHERE email = ?";
        when(authJdbcTemplate.queryForObject(eq(userNameSql), eq(String.class), eq(TEST_EMAIL)))
                .thenReturn(TEST_NAME);
        
        String rolesSql = "SELECT r.name FROM roles r " +
                         "JOIN user_roles ur ON r.id = ur.role_id " +
                         "JOIN users u ON u.id = ur.user_id " +
                         "WHERE u.email = ?";
        when(authJdbcTemplate.queryForList(eq(rolesSql), eq(String.class), eq(TEST_EMAIL)))
                .thenReturn(new java.util.ArrayList<>()); // Empty list, not null

        UserInfo result = authServiceClient.getUserByEmail(TEST_EMAIL);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(1, result.getRoles().size(), "Should have one default role");
        assertEquals("STUDENT", result.getRoles().get(0));
        
        verify(authJdbcTemplate).queryForObject(eq(countSql), eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq(userIdSql), eq(Long.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq(userNameSql), eq(String.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForList(eq(rolesSql), eq(String.class), eq(TEST_EMAIL));
    }

    @Test
    void getUserByEmailExceptionDuringRolesRetrievalDefaultsToStudent() {
        when(authJdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), 
                eq(Integer.class), eq(TEST_EMAIL))).thenReturn(1);
        when(authJdbcTemplate.queryForObject(eq("SELECT id FROM users WHERE email = ?"), 
                eq(Long.class), eq(TEST_EMAIL))).thenReturn(TEST_USER_ID);
        when(authJdbcTemplate.queryForObject(eq("SELECT name FROM users WHERE email = ?"), 
                eq(String.class), eq(TEST_EMAIL))).thenReturn(TEST_NAME);
        
        String rolesSql = "SELECT r.name FROM roles r " +
                         "JOIN user_roles ur ON r.id = ur.role_id " +
                         "JOIN users u ON u.id = ur.user_id " +
                         "WHERE u.email = ?";
        when(authJdbcTemplate.queryForList(eq(rolesSql), eq(String.class), eq(TEST_EMAIL)))
                .thenThrow(new RuntimeException("Error retrieving roles"));

        UserInfo result = authServiceClient.getUserByEmail(TEST_EMAIL);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(1, result.getRoles().size(), "Should have one default role");
        assertEquals("STUDENT", result.getRoles().get(0));
        
        verify(authJdbcTemplate).queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), eq(Integer.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq("SELECT id FROM users WHERE email = ?"), eq(Long.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForObject(eq("SELECT name FROM users WHERE email = ?"), eq(String.class), eq(TEST_EMAIL));
        verify(authJdbcTemplate).queryForList(eq(rolesSql), eq(String.class), eq(TEST_EMAIL));
    }
}
