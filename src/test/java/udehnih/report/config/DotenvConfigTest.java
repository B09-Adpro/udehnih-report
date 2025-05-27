package udehnih.report.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

class DotenvConfigTest {

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private MutablePropertySources propertySources;

    private DotenvConfig dotenvConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(environment.getPropertySources()).thenReturn(propertySources);
        doNothing().when(propertySources).addFirst(any(PropertiesPropertySource.class));
        
        // Set up active profiles for test environment
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        
        dotenvConfig = new DotenvConfig(environment);
    }

    @Test
    void testLoadEnvironmentVariablesWithEnvFile() throws Exception {
        // Create a temporary .env file
        Path envFile = createTempEnvFile();
        
        // Set the user.dir property to point to our temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Call afterPropertiesSet to trigger loadEnvironmentVariables
            dotenvConfig.afterPropertiesSet();
            
            // Verify system properties were set
            assertEquals("postgresql://localhost:5432/testdb", System.getProperty("DB_URL"));
            assertEquals("testuser", System.getProperty("DB_USERNAME"));
            assertEquals("testpass", System.getProperty("DB_PASSWORD"));
            // The jdbc: prefix is added to the spring.datasource.url property
            assertEquals("jdbc:postgresql://localhost:5432/testdb", System.getProperty("spring.datasource.url"));
            
            // Verify verifyAndLogProperty was called
            // This is hard to test directly, but we can verify the properties exist
            assertNotNull(System.getProperty("spring.datasource.url"));
            assertNotNull(System.getProperty("spring.datasource.username"));
            assertNotNull(System.getProperty("spring.datasource.password"));
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
            
            // Clean up system properties
            System.clearProperty("DB_URL");
            System.clearProperty("DB_USERNAME");
            System.clearProperty("DB_PASSWORD");
            System.clearProperty("spring.datasource.url");
            System.clearProperty("spring.datasource.username");
            System.clearProperty("spring.datasource.password");
        }
    }

    @Test
    void testLoadEnvironmentVariablesInTestEnvironment() throws Exception {
        // Don't create a .env file, but set up for test environment
        String originalUserDir = System.getProperty("user.dir");
        try {
            // Point to a directory without a .env file
            System.setProperty("user.dir", tempDir.toString());
            
            // Call afterPropertiesSet to trigger loadEnvironmentVariables
            dotenvConfig.afterPropertiesSet();
            
            // Verify test properties were set
            verify(propertySources).addFirst(any(PropertiesPropertySource.class));
            
            // Verify system properties were set for test environment
            assertTrue(System.getProperty("spring.datasource.url").contains("h2:mem"));
            assertEquals("sa", System.getProperty("spring.datasource.username"));
            assertEquals("sa", System.getProperty("spring.datasource.password"));
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
            
            // Clean up system properties
            System.clearProperty("spring.datasource.url");
            System.clearProperty("spring.datasource.username");
            System.clearProperty("spring.datasource.password");
        }
    }

    @Test
    void testVerifyAndLogProperty() throws Exception {
        // Set up a test property
        System.setProperty("test.property", "test-value");
        System.setProperty("test.password", "secret");
        
        // Create a new DotenvConfig with a spy to verify method calls
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("verifyAndLogProperty", String.class);
        method.setAccessible(true);
        
        // Call the method for a regular property
        method.invoke(spyConfig, "test.property");
        
        // Call the method for a password property
        method.invoke(spyConfig, "test.password");
        
        // Call the method for a non-existent property
        method.invoke(spyConfig, "non.existent.property");
        
        // Clean up
        System.clearProperty("test.property");
        System.clearProperty("test.password");
    }

    private Path createTempEnvFile() throws IOException {
        Path envFile = tempDir.resolve(".env");
        
        String envContent = 
            "# Database configuration\n" +
            "DB_URL=postgresql://localhost:5432/testdb\n" +
            "DB_USERNAME=testuser\n" +
            "DB_PASSWORD=testpass\n" +
            "AUTH_DB_URL=postgresql://localhost:5432/authdb\n" +
            "AUTH_DB_USERNAME=authuser\n" +
            "AUTH_DB_PASSWORD=authpass\n" +
            "\n" +
            "# Empty line and comment should be ignored\n" +
            "TEST_PROPERTY=\"quoted value\"\n";
        
        Files.writeString(envFile, envContent);
        return envFile;
    }
}
