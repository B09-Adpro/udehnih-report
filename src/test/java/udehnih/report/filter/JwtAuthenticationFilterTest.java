package udehnih.report.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.model.UserInfo;
import udehnih.report.util.AppConstants;
import udehnih.report.util.JwtUtil;
import java.util.Collections;
import java.util.Enumeration;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
class JwtAuthenticationFilterTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthServiceClient authServiceClient;
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
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(headerNames.hasMoreElements()).thenReturn(false);
    }
    
    @Test
    void doFilterInternalShouldAuthenticateUserWhenValidTokenIsProvided() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        
        UserInfo userInfo = UserInfo.builder()
            .id(1L)
            .email(testEmail)
            .name("Test User")
            .roles(Collections.singletonList(testRole))
            .build();
            
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, atLeastOnce()).validateToken(eq(testToken), any());
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Status"), eq("authenticated"));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Username"), eq(testEmail));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Role"), eq(testRole));
        verify(response, atLeastOnce()).setHeader(eq(AppConstants.AUTHORIZATION_HEADER), eq(testAuthHeader));
        verify(request, atLeastOnce()).setAttribute(eq("X-User-Email"), eq(testEmail));
        verify(request, atLeastOnce()).setAttribute(eq("X-User-Role"), eq(testRole));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalShouldContinueFilterChainWhenNoTokenIsProvided() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).extractRole(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalShouldContinueFilterChainWhenInvalidTokenFormatIsProvided() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn("InvalidToken");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).extractRole(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalShouldHandleNullUsernameWhenExtractedFromToken() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(null);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, never()).validateToken(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalShouldHandleInvalidTokenWhenValidationFails() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(false);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(jwtUtil, atLeastOnce()).extractRole(testToken);
        verify(jwtUtil, atLeastOnce()).validateToken(eq(testToken), any());
        verify(response, never()).setHeader(eq("X-Auth-Status"), eq("authenticated"));
        verify(filterChain).doFilter(request, response);
    }
    @Test

    void doFilterInternalShouldHandleExceptionDuringAuthentication() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenThrow(new RuntimeException("Test exception"));
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(request, atLeastOnce()).getHeader(AppConstants.AUTHORIZATION_HEADER);
        verify(jwtUtil, atLeastOnce()).extractUsername(testToken);
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Status"), eq("unauthenticated"));
        verify(response, atLeastOnce()).setHeader(eq("X-Auth-Error"), anyString());
        verify(filterChain).doFilter(request, response);
    }
    @Test

    void addUserIdToHeaderShouldHandleDatabaseException() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        when(authServiceClient.getUserByEmail(testEmail))
                .thenThrow(new RuntimeException("Database error"));
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(authServiceClient, atLeastOnce()).getUserByEmail(testEmail);
        verify(filterChain).doFilter(request, response);
    }
    @Test

    void setUserInfoHeadersShouldHandleDatabaseException() throws Exception {
        when(request.getHeader(AppConstants.AUTHORIZATION_HEADER)).thenReturn(testAuthHeader);
        when(jwtUtil.extractUsername(testToken)).thenReturn(testEmail);
        when(jwtUtil.extractRole(testToken)).thenReturn(AppConstants.ROLE_PREFIX + testRole);
        when(jwtUtil.validateToken(eq(testToken), any())).thenReturn(true);
        
        UserInfo userInfo = UserInfo.builder()
            .id(1L)
            .email(testEmail)
            .name(null) // Simulate missing name
            .roles(Collections.singletonList(testRole))
            .build();
            
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        verify(authServiceClient, atLeastOnce()).getUserByEmail(testEmail);
        verify(response, atLeastOnce()).setHeader(eq("X-User-Id"), eq("1"));
        verify(response, never()).setHeader(eq("X-Auth-Name"), anyString());
        verify(filterChain).doFilter(request, response);
    }
}
