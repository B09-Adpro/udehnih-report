package udehnih.report.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;

import java.util.Enumeration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JdbcTemplate authJdbcTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Enumeration<String> headerNames;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String testToken = "test.jwt.token";
    private final String testEmail = "test@example.com";
    private final String testRole = "STUDENT";
    private final String testAuthHeader = AppConstants.BEARER_PREFIX + testToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        
        // Setup common request mocking
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(headerNames.hasMoreElements()).thenReturn(false);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenValidTokenIsProvided() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        
        // Mock database queries
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(testEmail)))
                .thenReturn(1L);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenReturn("Test User");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, atLeastOnce()).validateToken(eq(testToken), any());
        
        // Verify authentication headers are set
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Status"), eq("authenticated"));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Username"), eq(testEmail));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Role"), eq(testRole));
        verify(response, atLeastOnce()).setHeader(eq(AppConstants.AUTHORIZATION_HEADER), eq(testAuthHeader));
        
        // Verify request attributes are set
        verify(request, atLeastOnce()).setAttribute(eq("X-User-Email"), eq(testEmail));
        verify(request, atLeastOnce()).setAttribute(eq("X-User-Role"), eq(testRole));
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenNoTokenIsProvided() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).extractRole(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any());
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenInvalidTokenFormatIsProvided() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn("InvalidToken");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).extractRole(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any());
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleNullUsername_WhenExtractedFromToken() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(null);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, never()).validateToken(anyString(), any());
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleInvalidToken_WhenValidationFails() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, atLeastOnce()).validateToken(eq(testToken), any());
        
        // Verify no authentication headers are set
        verify(response, never()).setHeader(eq("X-Auth-Status"), eq("authenticated"));
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleExceptionDuringAuthentication() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenThrow(new RuntimeException("Test exception"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        
        // Verify error headers are set
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Status"), eq("unauthenticated"));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Error"), anyString());
        
        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void addUserIdToHeader_ShouldHandleDatabaseException() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        
        // Mock database exception
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(testEmail)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authJdbcTemplate, atLeastOnce()).queryForObject(anyString(), eq(Long.class), eq(testEmail));
        
        // Verify filter chain continues despite database error
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void setUserInfoHeaders_ShouldHandleDatabaseException() throws Exception {
        // Arrange
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        
        // Mock userId success but userName failure
        when(authJdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(testEmail)))
                .thenReturn(1L);
        when(authJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(testEmail)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authJdbcTemplate, atLeastOnce()).queryForObject(anyString(), eq(String.class), eq(testEmail));
        
        // Verify user ID header is set but not user name
        verify(response, atLeastOnce()).setHeader(eq("X-User-Id"), eq("1"));
        verify(response, never()).setHeader(eq("X-Auth-Name"), anyString());
        
        // Verify filter chain continues despite database error
        verify(filterChain).doFilter(request, response);
    }
}
