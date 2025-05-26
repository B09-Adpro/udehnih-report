package udehnih.report;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;
import udehnih.report.service.CustomUserDetailsService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class ReportApplicationTests {
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void contextLoads() {
        assert reportService != null : "ReportService should not be null";
        assert jwtUtil != null : "JwtUtil should not be null";
        assert customUserDetailsService != null : "CustomUserDetailsService should not be null";
        assert applicationContext != null : "ApplicationContext should not be null";
    }
    
    @Test
    void applicationClassShouldHaveMainMethod() throws Exception {
        Class<?> appClass = ReportApplication.class;
        assertNotNull(appClass, "Application class should exist");
        
        java.lang.reflect.Method mainMethod = appClass.getDeclaredMethod("main", String[].class);
        assertNotNull(mainMethod, "Main method should exist");

        int modifiers = mainMethod.getModifiers();
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "Main method should be public");
        assertTrue(java.lang.reflect.Modifier.isStatic(modifiers), "Main method should be static");
    }
}
