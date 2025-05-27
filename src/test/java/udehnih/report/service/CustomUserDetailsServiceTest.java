package udehnih.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import udehnih.report.client.AuthServiceClient;
import udehnih.report.model.UserInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {
    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private final String testEmail = "test@example.com";

    private final String testName = "Test User";
    private final Long testId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
        UserInfo userInfo = UserInfo.builder()
            .id(testId)
            .email(testEmail)
            .name(testName)
            .roles(Collections.singletonList("STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        verify(authServiceClient).getUserByEmail(testEmail);
    }

    @Test
    void loadUserByUsernameShouldHandleMultipleRoles() {
        UserInfo userInfo = UserInfo.builder()
            .id(testId)
            .email(testEmail)
            .name(testName)
            .roles(Arrays.asList("STUDENT", "STAFF", "TUTOR"))
            .build();
        
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(3, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STAFF")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_TUTOR")));
    }

    @Test
    void loadUserByUsernameShouldThrowExceptionWhenUserNotFound() {
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(null);
        
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(testEmail));
    }

    @Test
    void getUserIdByEmailShouldReturnUserIdWhenUserExists() {
        UserInfo userInfo = UserInfo.builder()
            .id(testId)
            .email(testEmail)
            .name(testName)
            .roles(Collections.singletonList("STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);
        
        assertTrue(result.isPresent());
        assertEquals(testId.toString(), result.get());
    }

    @Test
    void getUserIdByEmailShouldReturnEmptyOptionalWhenUserNotFound() {
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(null);

        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);

        assertFalse(result.isPresent());
    }

    @Test
    void getUserInfoByEmailShouldReturnUserInfoWhenUserExists() {
        UserInfo userInfo = UserInfo.builder()
            .id(testId)
            .email(testEmail)
            .name(testName)
            .roles(Collections.singletonList("STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        UserInfo result = userDetailsService.getUserInfoByEmail(testEmail);
        
        assertNotNull(result);
        assertEquals(testId, result.getId());
        assertEquals(testEmail, result.getEmail());
        assertEquals(testName, result.getName());
        assertTrue(result.getRoles().contains("STUDENT"));
    }

    @Test
    void getUserInfoByEmailShouldReturnNullWhenUserNotFound() {
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(null);
        
        UserInfo result = userDetailsService.getUserInfoByEmail(testEmail);
        
        assertNull(result);
    }
    
    @Test
    void loadUserByUsernameShouldUseDefaultRoleWhenUserHasNoRoles() {
        UserInfo userInfo = UserInfo.builder()
            .id(testId)
            .email(testEmail)
            .name(testName)
            .roles(Collections.emptyList())
            .build();
        
        when(authServiceClient.getUserByEmail(testEmail)).thenReturn(userInfo);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }
    
    @Test
    void loadUserByUsernameShouldHandleExceptionDuringUserRetrieval() {
        when(authServiceClient.getUserByEmail(testEmail)).thenThrow(new RuntimeException("Database error"));
        
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(testEmail));
    }
    
    @Test
    void getUserIdByEmailShouldHandleExceptionDuringUserRetrieval() {
        when(authServiceClient.getUserByEmail(testEmail)).thenThrow(new RuntimeException("Database error"));
        
        Optional<String> result = userDetailsService.getUserIdByEmail(testEmail);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void getUserInfoByEmailShouldHandleExceptionDuringUserRetrieval() {
        when(authServiceClient.getUserByEmail(testEmail)).thenThrow(new RuntimeException("Database error"));
        
        UserInfo result = userDetailsService.getUserInfoByEmail(testEmail);
        
        assertNull(result);
    }
}
