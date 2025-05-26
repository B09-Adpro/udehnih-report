package udehnih.report.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import udehnih.report.util.AppConstants;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("password", testPassword);
        userData.put("name", testName);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim()))).thenReturn(userData);
        List<String> roles = Collections.singletonList("STUDENT");
        when(authJdbcTemplate.queryForList(anyString(), eq(String.class), eq(testId)))
                .thenReturn(roles);
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        verify(authJdbcTemplate).queryForMap(anyString(), eq(testEmail.trim()));
        verify(authJdbcTemplate).queryForList(anyString(), eq(String.class), eq(testId));
    }
    @Test
    void loadUserByUsername_ShouldHandleMultipleRoles() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("password", testPassword);
        userData.put("name", testName);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim()))).thenReturn(userData);
        List<String> roles = Arrays.asList("STUDENT", "STAFF", "TUTOR");
        when(authJdbcTemplate.queryForList(anyString(), eq(String.class), eq(testId)))
                .thenReturn(roles);
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(3, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STAFF")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_TUTOR")));
        verify(authJdbcTemplate).queryForMap(anyString(), eq(testEmail.trim()));
        verify(authJdbcTemplate).queryForList(anyString(), eq(String.class), eq(testId));
    }
    @Test
    void loadUserByUsername_ShouldUseDefaultRole_WhenRoleNotFound() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("password", testPassword);
        userData.put("name", testName);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim()))).thenReturn(userData);
        when(authJdbcTemplate.queryForList(anyString(), eq(String.class), eq(testId)))
                .thenReturn(Collections.emptyList());
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        assertEquals(1, userDetails.getAuthorities().size());
    }
    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail.trim())))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(testEmail));
    }
    @Test
    void getUserIdByEmail_ShouldReturnUserId_WhenUserExists() {
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenReturn(testId.toString());
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);
        assertTrue(result.isPresent());
        assertEquals(testId.toString(), result.get());
    }
    @Test
    void getUserIdByEmail_ShouldReturnEmptyOptional_WhenUserNotFound() {
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenThrow(new EmptyResultDataAccessException(1));
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);
        assertFalse(result.isPresent());
    }
    @Test
    void getUserInfoByEmail_ShouldReturnUserInfo_WhenUserExists() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("name", testName);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId)))
                .thenReturn("STUDENT");
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);
        assertTrue(result.isPresent());
        Map<String, Object> userInfo = result.get();
        assertEquals(testId, userInfo.get("id"));
        assertEquals(testEmail, userInfo.get("email"));
        assertEquals(testName, userInfo.get("name"));
        assertEquals("STUDENT", userInfo.get("role"));
    }
    @Test
    void getUserInfoByEmail_ShouldUseDefaultRole_WhenRoleNotFound() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", testId);
        userData.put("email", testEmail);
        userData.put("name", testName);
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail))).thenReturn(userData);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testId)))
                .thenThrow(new EmptyResultDataAccessException(1));
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);
        assertTrue(result.isPresent());
        Map<String, Object> userInfo = result.get();
        assertEquals(testId, userInfo.get("id"));
        assertEquals(testEmail, userInfo.get("email"));
        assertEquals(testName, userInfo.get("name"));
        assertEquals(AppConstants.STUDENT_ROLE, userInfo.get("role"));
    }
    @Test
    void getUserInfoByEmail_ShouldReturnEmptyOptional_WhenUserNotFound() {
        when(authJdbcTemplate.queryForMap(anyString(), eq(testEmail)))
                .thenThrow(new EmptyResultDataAccessException(1));
        Optional<Map<String, Object>> result = userDetailsService.getUserInfoByEmail(testEmail);
        assertFalse(result.isPresent());
    }
}
