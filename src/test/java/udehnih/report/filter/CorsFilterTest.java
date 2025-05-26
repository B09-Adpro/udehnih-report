package udehnih.report.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
class CorsFilterTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private CorsFilter corsFilter;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void doFilterInternal_ShouldSetCorsHeaders() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getHeader("Origin")).thenReturn("http:
        corsFilter.doFilterInternal(request, response, filterChain);
        verify(response).setHeader("Access-Control-Allow-Origin", "http:
        verify(response).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        verify(response).setHeader("Access-Control-Max-Age", "3600");
        verify(response).setHeader("Access-Control-Allow-Headers", 
                "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control, Access-Control-Allow-Origin, Access-Control-Allow-Headers, X-Auth-Token");
        verify(response).setHeader("Access-Control-Expose-Headers", 
                "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-User-Email, X-User-Role, X-Auth-Token, X-User-Id, Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response).setHeader("Vary", "Origin");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(filterChain, never()).doFilter(request, response);
    }
    @Test
    void doFilterInternal_ShouldContinueFilterChain_ForNonOptionsRequests() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Origin")).thenReturn("http:
        corsFilter.doFilterInternal(request, response, filterChain);
        verify(response).setHeader("Access-Control-Allow-Origin", "http:
        verify(response).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        verify(response).setHeader("Access-Control-Max-Age", "3600");
        verify(response).setHeader("Access-Control-Allow-Headers", 
                "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control, Access-Control-Allow-Origin, Access-Control-Allow-Headers, X-Auth-Token");
        verify(response).setHeader("Access-Control-Expose-Headers", 
                "Authorization, X-Auth-Status, X-Auth-Username, X-Auth-Role, X-Auth-Name, X-User-Email, X-User-Role, X-Auth-Token, X-User-Id, Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response).setHeader("Vary", "Origin");
        verify(filterChain).doFilter(request, response);
    }
}
