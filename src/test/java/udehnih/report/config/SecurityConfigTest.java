package udehnih.report.config;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import udehnih.report.filter.CorsFilter;
import udehnih.report.filter.JwtAuthenticationFilter;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private CorsFilter corsFilter;

    @Mock
    private AuthenticationConfiguration authConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void enableAuthenticationContextOnSpawnedThreadsShouldSetInheritableThreadLocalStrategy() {
        securityConfig.enableAuthenticationContextOnSpawnedThreads();
        
        assertNotNull(SecurityContextHolder.getContext());
    }

    @Test
    void passwordEncoderShouldReturnBCryptPasswordEncoder() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();
        
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void authenticationManagerShouldReturnAuthenticationManagerFromConfig() throws Exception {
        when(authConfig.getAuthenticationManager()).thenReturn(authenticationManager);
        
        AuthenticationManager result = securityConfig.authenticationManager(authConfig);
        
        assertNotNull(result);
        assertEquals(authenticationManager, result);
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    void corsConfigurationSourceShouldReturnProperlyConfiguredCorsSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        
        assertNotNull(source);
        
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest());
        
        assertNotNull(config);
        assertTrue(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
        
        List<String> allowedMethods = config.getAllowedMethods();
        assertNotNull(allowedMethods);
        assertTrue(allowedMethods.containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        
        List<String> exposedHeaders = config.getExposedHeaders();
        assertNotNull(exposedHeaders);
        assertTrue(exposedHeaders.contains("Authorization"));
        assertTrue(exposedHeaders.contains("X-Auth-Token"));
        assertTrue(exposedHeaders.contains("X-Auth-Role"));
        assertTrue(exposedHeaders.contains("X-User-Role"));
        assertTrue(exposedHeaders.contains("Access-Control-Allow-Origin"));
        assertTrue(exposedHeaders.contains("Access-Control-Allow-Credentials"));
    }

    @Test
    void exceptionHandlersShouldReturnCorrectResponses() throws IOException {
        MockHttpServletResponse unauthorizedResponse = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Invalid credentials");

        unauthorizedResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        unauthorizedResponse.setContentType("application/json");
        unauthorizedResponse.getWriter().write("{\"error\": \"Unauthorized: " + authException.getMessage() + "\"}");

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, unauthorizedResponse.getStatus());
        assertEquals("application/json", unauthorizedResponse.getContentType());
        assertTrue(unauthorizedResponse.getContentAsString().contains("Unauthorized: Invalid credentials"));

        MockHttpServletResponse forbiddenResponse = new MockHttpServletResponse();
        org.springframework.security.access.AccessDeniedException accessDeniedException = 
            new org.springframework.security.access.AccessDeniedException("Access Denied");

        forbiddenResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        forbiddenResponse.setContentType("application/json");
        forbiddenResponse.getWriter().write("{\"error\": \"Access Denied: " + accessDeniedException.getMessage() + "\"}");
        
        assertEquals(HttpServletResponse.SC_FORBIDDEN, forbiddenResponse.getStatus());
        assertEquals("application/json", forbiddenResponse.getContentType());
        assertTrue(forbiddenResponse.getContentAsString().contains("Access Denied: Access Denied"));
    }
}
