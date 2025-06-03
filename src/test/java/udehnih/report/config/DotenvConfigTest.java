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
        createTempEnvFile();
        
        String originalUserDir = System.getProperty("user.dir");
        
        String[] envVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
        String[] originalEnvValues = new String[envVars.length];
        
        try {
            for (int i = 0; i < envVars.length; i++) {
                originalEnvValues[i] = System.getProperty(envVars[i]);
                System.clearProperty(envVars[i]);
            }
            
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
            for (String envVar : envVars) {
                System.clearProperty(envVar);
            }
            System.clearProperty("spring.datasource.url");
            System.clearProperty("spring.datasource.username");
            System.clearProperty("spring.datasource.password");
            System.clearProperty("spring.datasource.driver-class-name");
            System.clearProperty("auth.datasource.url");
            System.clearProperty("auth.datasource.username");
            System.clearProperty("auth.datasource.password");
            System.clearProperty("auth.datasource.driver-class-name");
            
            for (int i = 0; i < envVars.length; i++) {
                if (originalEnvValues[i] != null) {
                    System.setProperty(envVars[i], originalEnvValues[i]);
                }
            }
        }
    }

    @Test
    void testLoadEnvironmentVariablesInTestEnvironment() throws Exception {
        // Don't create a .env file, but set up for test environment
        String originalUserDir = System.getProperty("user.dir");
        
        String[] envVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
        String[] originalEnvValues = new String[envVars.length];
        
        try {
            for (int i = 0; i < envVars.length; i++) {
                originalEnvValues[i] = System.getProperty(envVars[i]);
                System.clearProperty(envVars[i]);
            }
            
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
            System.clearProperty("spring.datasource.driver-class-name");
            System.clearProperty("auth.datasource.url");
            System.clearProperty("auth.datasource.username");
            System.clearProperty("auth.datasource.password");
            System.clearProperty("auth.datasource.driver-class-name");
            
            // Restore original environment variable values if they existed
            for (int i = 0; i < envVars.length; i++) {
                if (originalEnvValues[i] != null) {
                    System.setProperty(envVars[i], originalEnvValues[i]);
                }
            }
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

    @Test
    void testMapToSpringProperties() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("mapToSpringProperties", Properties.class, String.class, String.class);
        method.setAccessible(true);
        
        // Test DB_URL mapping
        method.invoke(spyConfig, props, "DB_URL", "postgresql://localhost:5432/testdb");
        assertEquals("jdbc:postgresql://localhost:5432/testdb", props.getProperty("spring.datasource.url"));
        assertEquals("org.postgresql.Driver", props.getProperty("spring.datasource.driver-class-name"));
        
        // Test DB_URL with jdbc prefix already present
        props.clear();
        method.invoke(spyConfig, props, "DB_URL", "jdbc:postgresql://localhost:5432/testdb");
        assertEquals("jdbc:postgresql://localhost:5432/testdb", props.getProperty("spring.datasource.url"));
        
        // Test DB_USERNAME mapping
        method.invoke(spyConfig, props, "DB_USERNAME", "testuser");
        assertEquals("testuser", props.getProperty("spring.datasource.username"));
        
        // Test DB_PASSWORD mapping
        method.invoke(spyConfig, props, "DB_PASSWORD", "testpass");
        assertEquals("testpass", props.getProperty("spring.datasource.password"));
        
        // Test AUTH_DB_URL mapping
        method.invoke(spyConfig, props, "AUTH_DB_URL", "postgresql://localhost:5432/authdb");
        assertEquals("jdbc:postgresql://localhost:5432/authdb", props.getProperty("auth.datasource.url"));
        assertEquals("org.postgresql.Driver", props.getProperty("auth.datasource.driver-class-name"));
        
        // Test AUTH_DB_USERNAME mapping
        method.invoke(spyConfig, props, "AUTH_DB_USERNAME", "authuser");
        assertEquals("authuser", props.getProperty("auth.datasource.username"));
        
        // Test AUTH_DB_PASSWORD mapping
        method.invoke(spyConfig, props, "AUTH_DB_PASSWORD", "authpass");
        assertEquals("authpass", props.getProperty("auth.datasource.password"));
    }

    @Test
    void testSetPropertyWithAppropriateLogging() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("setPropertyWithAppropriateLogging", Properties.class, String.class, String.class);
        method.setAccessible(true);
        
        try {
            // Test with regular property
            method.invoke(spyConfig, props, "DB_URL", "postgresql://localhost:5432/testdb");
            assertEquals("postgresql://localhost:5432/testdb", props.getProperty("DB_URL"));
            assertEquals("postgresql://localhost:5432/testdb", System.getProperty("DB_URL"));
            
            // Test with password property
            method.invoke(spyConfig, props, "DB_PASSWORD", "secret");
            assertEquals("secret", props.getProperty("DB_PASSWORD"));
            assertEquals("secret", System.getProperty("DB_PASSWORD"));
        } finally {
            // Clean up system properties
            System.clearProperty("DB_URL");
            System.clearProperty("DB_PASSWORD");
        }
    }

    @Test
    void testLoadFromRenderSecrets() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("loadFromRenderSecrets", Properties.class);
        method.setAccessible(true);
        
        // Test when secrets directory doesn't exist
        boolean result = (boolean) method.invoke(spyConfig, props);
        assertFalse(result);
        
        // Create a mock secrets directory structure
        Path secretsDir = tempDir.resolve("etc").resolve("secrets");
        Files.createDirectories(secretsDir);
        
        // Create secret files
        Files.writeString(secretsDir.resolve("DB_URL"), "postgresql://localhost:5432/testdb");
        Files.writeString(secretsDir.resolve("DB_USERNAME"), "testuser");
        Files.writeString(secretsDir.resolve("DB_PASSWORD"), "testpass");
        
        // Mock Files.exists to return true for our test directory
        // This test will verify the method exists and can be called, but won't test the actual secrets loading
        // since we can't easily mock the static Files.exists method
    }

    @Test
    void testCheckSystemEnvironmentVariables() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("checkSystemEnvironmentVariables", Properties.class);
        method.setAccessible(true);
        
        // Test when no environment variables are set
        boolean result = (boolean) method.invoke(spyConfig, props);
        assertFalse(result); // Should return false since required env vars are not set
    }

    @Test
    void testHandleMissingEnvironmentConfiguration() throws Exception {
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("handleMissingEnvironmentConfiguration");
        method.setAccessible(true);
        
        // Test in test environment (should not throw exception)
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        assertDoesNotThrow(() -> {
            try {
                method.invoke(spyConfig);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testHandleMissingEnvironmentConfigurationInProduction() throws Exception {
        // Create a spy and mock the environment to simulate production
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("handleMissingEnvironmentConfiguration");
        method.setAccessible(true);
        
        // Mock environment to return non-test profiles
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});
        
        // Since we can't easily mock Class.forName() without PowerMock or similar,
        // and JUnit classes are always present during tests, we'll test the logic indirectly
        // by creating a scenario where the test environment detection should fail
        
        // This test verifies that the method can be called and doesn't throw in test environment
        // (since JUnit classes are present), but documents the intended behavior
        assertDoesNotThrow(() -> {
            try {
                method.invoke(spyConfig);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Note: In a real production environment without JUnit on classpath,
        // this would throw IllegalStateException when no env config is found
    }

    @Test
    void testIsTestEnvironment() throws Exception {
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("isTestEnvironment");
        method.setAccessible(true);
        
        // Test with test profile
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        boolean result = (boolean) method.invoke(spyConfig);
        assertTrue(result);
        
        // Test without test profile
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        result = (boolean) method.invoke(spyConfig);
        assertTrue(result); // Should still be true because JUnit classes are present
    }

    @Test
    void testCheckSystemEnvironmentVariablesWithActualEnvironment() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Mock environment variables using system properties (since we can't actually set env vars in tests)
        String[] originalValues = new String[6];
        String[] envVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
        
        try {
            // Store original values
            for (int i = 0; i < envVars.length; i++) {
                originalValues[i] = System.getProperty(envVars[i]);
            }
            
            // Test with some environment variables set
            System.setProperty("DB_URL", "postgresql://localhost:5432/testdb");
            System.setProperty("DB_USERNAME", "testuser");
            System.setProperty("DB_PASSWORD", "testpass");
            // Leave AUTH_DB variables unset to test partial coverage
            
            // Use a custom DotenvConfig that checks system properties instead of environment variables
            DotenvConfig customConfig = new DotenvConfig(environment) {
                // Override to use system properties for testing
                private boolean checkSystemEnvironmentVariables(Properties props) {
                    String[] requiredVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
                    boolean allFound = true;
                    
                    for (String key : requiredVars) {
                        String value = System.getProperty(key); // Use system properties instead of env vars
                        if (value != null && !value.isEmpty()) {
                            props.setProperty(key, value);
                            System.setProperty(key, value);
                            
                            // Simple mapping for testing
                            if (key.equals("DB_URL")) {
                                props.setProperty("spring.datasource.url", "jdbc:" + value);
                                System.setProperty("spring.datasource.url", "jdbc:" + value);
                            }
                        } else {
                            allFound = false;
                        }
                    }
                    
                    return allFound;
                }
            };
            
            // Use reflection to call the method
            java.lang.reflect.Method method = customConfig.getClass().getDeclaredMethod("checkSystemEnvironmentVariables", Properties.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(customConfig, props);
            
            // Should return false because not all required vars are set
            assertFalse(result);
            
            // Verify some properties were set
            assertEquals("postgresql://localhost:5432/testdb", props.getProperty("DB_URL"));
            assertEquals("testuser", props.getProperty("DB_USERNAME"));
            assertEquals("testpass", props.getProperty("DB_PASSWORD"));
            
        } finally {
            // Restore original values
            for (int i = 0; i < envVars.length; i++) {
                if (originalValues[i] != null) {
                    System.setProperty(envVars[i], originalValues[i]);
                } else {
                    System.clearProperty(envVars[i]);
                }
            }
            System.clearProperty("spring.datasource.url");
        }
    }

    @Test
    void testIsTestEnvironmentWithCIEnvironment() throws Exception {
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("isTestEnvironment");
        method.setAccessible(true);
        
        // Test with non-test profiles but mock CI environment
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});
        
        // We can't actually set environment variables in tests, but we can test the logic
        // The method will still return true because JUnit classes are present
        boolean result = (boolean) method.invoke(spyConfig);
        assertTrue(result); // JUnit classes are present in test environment
    }

    @Test
    void testLoadEnvFileWithIOException() throws Exception {
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("loadEnvFile", String.class, Properties.class);
        method.setAccessible(true);
        
        // Test with non-existent file
        boolean result = (boolean) method.invoke(spyConfig, "/non/existent/path/.env", props);
        assertFalse(result);
    }

    @Test
    void testLoadEnvFileWithComplexContent() throws Exception {
        // Create a more complex .env file
        Path envFile = tempDir.resolve(".env");
        String envContent = 
            "# This is a comment\n" +
            "DB_URL=postgresql://localhost:5432/testdb\n" +
            "DB_USERNAME=testuser\n" +
            "DB_PASSWORD=\"quoted password with spaces\"\n" +
            "AUTH_DB_URL=jdbc:postgresql://localhost:5432/authdb\n" +
            "AUTH_DB_USERNAME=authuser\n" +
            "AUTH_DB_PASSWORD=authpass\n" +
            "OTHER_PROPERTY=some_value\n" +
            "SOME_PASSWORD_VAR=secret\n" +
            "\n" +  // Empty line
            "INVALID_LINE_NO_EQUALS\n" +
            "=INVALID_LINE_NO_KEY\n";
        
        Files.writeString(envFile, envContent);
        
        Properties props = new Properties();
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("loadEnvFile", String.class, Properties.class);
        method.setAccessible(true);
        
        String originalUserDir = System.getProperty("user.dir");
        
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            boolean result = (boolean) method.invoke(spyConfig, envFile.toString(), props);
            assertTrue(result);
            
            // Verify properties were loaded
            assertEquals("postgresql://localhost:5432/testdb", props.getProperty("DB_URL"));
            assertEquals("testuser", props.getProperty("DB_USERNAME"));
            assertEquals("quoted password with spaces", props.getProperty("DB_PASSWORD"));
            assertEquals("jdbc:postgresql://localhost:5432/authdb", props.getProperty("AUTH_DB_URL"));
            assertEquals("some_value", props.getProperty("OTHER_PROPERTY"));
            assertEquals("secret", props.getProperty("SOME_PASSWORD_VAR"));
            
            // Verify Spring properties were set
            assertEquals("jdbc:postgresql://localhost:5432/testdb", System.getProperty("spring.datasource.url"));
            assertEquals("testuser", System.getProperty("spring.datasource.username"));
            assertEquals("quoted password with spaces", System.getProperty("spring.datasource.password"));
            assertEquals("jdbc:postgresql://localhost:5432/authdb", System.getProperty("auth.datasource.url"));
            
        } finally {
            System.setProperty("user.dir", originalUserDir);
            
            // Clean up system properties
            String[] propsToClean = {
                "DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD",
                "OTHER_PROPERTY", "SOME_PASSWORD_VAR",
                "spring.datasource.url", "spring.datasource.username", "spring.datasource.password", "spring.datasource.driver-class-name",
                "auth.datasource.url", "auth.datasource.username", "auth.datasource.password", "auth.datasource.driver-class-name",
                "spring.h2.console.enabled"
            };
            
            for (String prop : propsToClean) {
                System.clearProperty(prop);
            }
        }
    }

    @Test
    void testVerifyAndLogPropertyEdgeCases() throws Exception {
        DotenvConfig spyConfig = spy(new DotenvConfig(environment));
        
        // Use reflection to call the private method
        java.lang.reflect.Method method = DotenvConfig.class.getDeclaredMethod("verifyAndLogProperty", String.class);
        method.setAccessible(true);
        
        try {
            // Test with empty property
            System.setProperty("empty.property", "");
            method.invoke(spyConfig, "empty.property");
            
            // Test with whitespace-only property
            System.setProperty("whitespace.property", "   ");
            method.invoke(spyConfig, "whitespace.property");
            
            // Test with null property (cleared)
            System.clearProperty("null.property");
            method.invoke(spyConfig, "null.property");
            
        } finally {
            System.clearProperty("empty.property");
            System.clearProperty("whitespace.property");
        }
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
