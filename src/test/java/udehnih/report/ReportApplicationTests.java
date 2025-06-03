package udehnih.report;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;
import udehnih.report.service.CustomUserDetailsService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    
    private Map<String, String> originalProperties = new HashMap<>();
    private static final String[] TEST_KEYS = {
        "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD", "TEST_PROPERTY"
    };
    
    @BeforeEach
    public void saveOriginalProperties() {
        for (String key : TEST_KEYS) {
            originalProperties.put(key, System.getProperty(key));
        }
    }
    
    @AfterEach
    public void restoreOriginalProperties() {
        for (Map.Entry<String, String> entry : originalProperties.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }
    
    @Test
    void contextLoads() {
        assertNotNull(reportService, "ReportService should not be null");
        assertNotNull(jwtUtil, "JwtUtil should not be null");
        assertNotNull(customUserDetailsService, "CustomUserDetailsService should not be null");
        assertNotNull(applicationContext, "ApplicationContext should not be null");
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
    
    @Test
    void mainMethodShouldSetSystemPropertiesFromDotenv() {
        for (String key : TEST_KEYS) {
            System.clearProperty(key);
        }
        
        DotenvBuilder dotenvBuilder = Mockito.mock(DotenvBuilder.class);
        Dotenv dotenv = Mockito.mock(Dotenv.class);

        when(dotenvBuilder.ignoreIfMissing()).thenReturn(dotenvBuilder);
        when(dotenvBuilder.load()).thenReturn(dotenv);

        when(dotenv.get("AUTH_DB_URL")).thenReturn("jdbc:postgresql://localhost:5432/testdb");
        when(dotenv.get("AUTH_DB_USERNAME")).thenReturn("testuser");
        when(dotenv.get("AUTH_DB_PASSWORD")).thenReturn("testpass");

        Set<DotenvEntry> entries = new HashSet<>();
        entries.add(createDotenvEntry("AUTH_DB_URL", "jdbc:postgresql://localhost:5432/testdb"));
        entries.add(createDotenvEntry("AUTH_DB_USERNAME", "testuser"));
        entries.add(createDotenvEntry("AUTH_DB_PASSWORD", "testpass"));
        entries.add(createDotenvEntry("ADDITIONAL_PROPERTY", "additional_value"));
        when(dotenv.entries()).thenReturn(entries);

        try (MockedStatic<Dotenv> mockedDotenv = Mockito.mockStatic(Dotenv.class);
             MockedStatic<SpringApplication> mockedSpringApp = Mockito.mockStatic(SpringApplication.class)) {
            
            mockedDotenv.when(Dotenv::configure).thenReturn(dotenvBuilder);

            mockedSpringApp.when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                .thenReturn(null);

            ReportApplication.main(new String[]{});

            assertEquals("jdbc:postgresql://localhost:5432/testdb", System.getProperty("AUTH_DB_URL"));
            assertEquals("testuser", System.getProperty("AUTH_DB_USERNAME"));
            assertEquals("testpass", System.getProperty("AUTH_DB_PASSWORD"));
            assertEquals("additional_value", System.getProperty("ADDITIONAL_PROPERTY"));
            
            mockedDotenv.verify(Dotenv::configure, times(1));
            mockedSpringApp.verify(() -> SpringApplication.run(eq(ReportApplication.class), any(String[].class)), times(1));
        }
    }
    
    @Test
    void mainMethodShouldHandleExceptionWhenLoadingEnvFile() {
        for (String key : TEST_KEYS) {
            System.clearProperty(key);
        }
        
        DotenvBuilder dotenvBuilder = Mockito.mock(DotenvBuilder.class);
        when(dotenvBuilder.ignoreIfMissing()).thenReturn(dotenvBuilder);
        when(dotenvBuilder.load()).thenThrow(new RuntimeException("Simulated .env file error"));
        
        try (MockedStatic<Dotenv> mockedDotenv = Mockito.mockStatic(Dotenv.class);
             MockedStatic<SpringApplication> mockedSpringApp = Mockito.mockStatic(SpringApplication.class)) {
            
            mockedDotenv.when(Dotenv::configure).thenReturn(dotenvBuilder);
            
            mockedSpringApp.when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                .thenReturn(null);
            
            ReportApplication.main(new String[]{});
            
            mockedSpringApp.verify(() -> SpringApplication.run(eq(ReportApplication.class), any(String[].class)), times(1));
        }
    }
    
    @Test
    void mainMethodShouldNotOverrideExistingSystemProperties() {
        System.setProperty("TEST_PROPERTY", "original-value");
        
        DotenvBuilder dotenvBuilder = Mockito.mock(DotenvBuilder.class);
        Dotenv dotenv = Mockito.mock(Dotenv.class);
        
        when(dotenvBuilder.ignoreIfMissing()).thenReturn(dotenvBuilder);
        when(dotenvBuilder.load()).thenReturn(dotenv);
        
        Set<DotenvEntry> entries = new HashSet<>();
        entries.add(createDotenvEntry("TEST_PROPERTY", "new-value"));
        when(dotenv.entries()).thenReturn(entries);

        try (MockedStatic<Dotenv> mockedDotenv = Mockito.mockStatic(Dotenv.class);
             MockedStatic<SpringApplication> mockedSpringApp = Mockito.mockStatic(SpringApplication.class)) {
            
            mockedDotenv.when(Dotenv::configure).thenReturn(dotenvBuilder);
            
            mockedSpringApp.when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                .thenReturn(null);
            
            ReportApplication.main(new String[]{});
            
            assertEquals("original-value", System.getProperty("TEST_PROPERTY"), 
                    "Existing system properties should not be overridden");
        }
    }
    
    private DotenvEntry createDotenvEntry(String key, String value) {
        DotenvEntry entry = Mockito.mock(DotenvEntry.class);
        when(entry.getKey()).thenReturn(key);
        when(entry.getValue()).thenReturn(value);
        return entry;
    }
}
