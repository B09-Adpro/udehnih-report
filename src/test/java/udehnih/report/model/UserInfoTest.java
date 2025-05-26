package udehnih.report.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class UserInfoTest {

    @Test
    void testHasRoleWithNullRoles() {
        UserInfo userInfo = UserInfo.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .roles(null)
                .build();
        
        assertFalse(userInfo.hasRole("STUDENT"));
    }
    
    @Test
    void testHasRoleWithExactMatch() {
        UserInfo userInfo = UserInfo.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .roles(Arrays.asList("STUDENT", "STAFF"))
                .build();
        
        assertTrue(userInfo.hasRole("STUDENT"));
        assertTrue(userInfo.hasRole("student"));
    }
    
    @Test
    void testHasRoleWithRolePrefix() {
        UserInfo userInfo = UserInfo.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .roles(Arrays.asList("ROLE_STUDENT", "ROLE_STAFF"))
                .build();
        
        assertTrue(userInfo.hasRole("STUDENT"));
        assertTrue(userInfo.hasRole("STAFF"));
    }
    
    @Test
    void testHasRoleWithNoMatch() {
        UserInfo userInfo = UserInfo.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .roles(Arrays.asList("STUDENT", "STAFF"))
                .build();
        
        assertFalse(userInfo.hasRole("TUTOR"));
    }
    
    @Test
    void testIsStaff() {
        UserInfo staffUser = UserInfo.builder()
                .id(1L)
                .email("staff@example.com")
                .name("Staff User")
                .roles(Arrays.asList("STAFF"))
                .build();
        
        UserInfo nonStaffUser = UserInfo.builder()
                .id(2L)
                .email("student@example.com")
                .name("Student User")
                .roles(Arrays.asList("STUDENT"))
                .build();
        
        assertTrue(staffUser.isStaff());
        assertFalse(nonStaffUser.isStaff());
    }
    
    @Test
    void testIsStudent() {
        UserInfo studentUser = UserInfo.builder()
                .id(1L)
                .email("student@example.com")
                .name("Student User")
                .roles(Arrays.asList("STUDENT"))
                .build();
        
        UserInfo nonStudentUser = UserInfo.builder()
                .id(2L)
                .email("staff@example.com")
                .name("Staff User")
                .roles(Arrays.asList("STAFF"))
                .build();
        
        assertTrue(studentUser.isStudent());
        assertFalse(nonStudentUser.isStudent());
    }
    
    @Test
    void testIsTutor() {
        UserInfo tutorUser = UserInfo.builder()
                .id(1L)
                .email("tutor@example.com")
                .name("Tutor User")
                .roles(Arrays.asList("TUTOR"))
                .build();
        
        UserInfo nonTutorUser = UserInfo.builder()
                .id(2L)
                .email("student@example.com")
                .name("Student User")
                .roles(Arrays.asList("STUDENT"))
                .build();
        
        assertTrue(tutorUser.isTutor());
        assertFalse(nonTutorUser.isTutor());
    }
    
    @Test
    void testRoleWithPrefixAndWithoutPrefix() {
        UserInfo userWithPrefix = UserInfo.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .roles(Arrays.asList("ROLE_STUDENT"))
                .build();
        
        UserInfo userWithoutPrefix = UserInfo.builder()
                .id(2L)
                .email("user2@example.com")
                .name("User 2")
                .roles(Arrays.asList("STUDENT"))
                .build();
        
        assertTrue(userWithPrefix.isStudent());
        assertTrue(userWithoutPrefix.isStudent());
    }
    
    @Test
    void testEmptyRolesList() {
        UserInfo userInfo = UserInfo.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .roles(Collections.emptyList())
                .build();
        
        assertFalse(userInfo.isStudent());
        assertFalse(userInfo.isStaff());
        assertFalse(userInfo.isTutor());
    }
}
