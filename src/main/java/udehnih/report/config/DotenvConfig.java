package udehnih.report.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
@Configuration
@Order(-2147483648)
public class DotenvConfig implements InitializingBean {

    private final ConfigurableEnvironment environment;

    public DotenvConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadEnvironmentVariables();
    }
    
    private void loadEnvironmentVariables() {
        try {
            String envPath = System.getProperty("user.dir") + "/.env";
            log.info("Looking for .env file at: {}" , envPath);
            
            if (Files.exists(Paths.get(envPath))) {
                log.info(".env file found, loading variables...");
                Properties props = new Properties();
                
                try (BufferedReader reader = new BufferedReader(new FileReader(envPath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                            continue;
                        }
                        
                        int equalIndex = line.indexOf('=');
                        if (equalIndex > 0) {
                            String key = line.substring(0, equalIndex).trim();
                            String value = line.substring(equalIndex + 1).trim();
                            
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            props.setProperty(key, value);
                            System.setProperty(key, value);
                            
                            if (key.equals("AUTH_DB_URL") || key.equals("AUTH_DB_USERNAME") || key.equals("DB_URL") || key.equals("DB_USERNAME")) {
                                log.info("Setting environment variable: {} = {}", key, value);
                            } else if (key.equals("AUTH_DB_PASSWORD") || key.equals("DB_PASSWORD")) {
                                log.info("Setting environment variable: {} = [MASKED]", key);
                            }
                            

                            if ((key.equals("DB_URL") || key.equals("AUTH_DB_URL")) && !value.startsWith("jdbc:")) {
                                value = "jdbc:" + value;
                                log.info("Added jdbc: prefix to {}: {}", key, value);
                            }                           
                            switch (key) {
                                case "DB_URL":
                                    props.setProperty("spring.datasource.url", value);
                                    System.setProperty("spring.datasource.url", value);
                                    log.info("Setting spring.datasource.url = {}", value);
                                    
                                    props.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                                    System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                                    log.info("Setting spring.datasource.driver-class-name = org.postgresql.Driver");
                                    
                                    props.setProperty("spring.h2.console.enabled", "false");
                                    System.setProperty("spring.h2.console.enabled", "false");
                                    break;
                                    
                                case "DB_USERNAME":
                                    props.setProperty("spring.datasource.username", value);
                                    System.setProperty("spring.datasource.username", value);
                                    log.info("Setting spring.datasource.username = {}", value);
                                    break;
                                    
                                case "DB_PASSWORD":
                                    props.setProperty("spring.datasource.password", value);
                                    System.setProperty("spring.datasource.password", value);
                                    log.info("Setting spring.datasource.password = [MASKED]");
                                    break;
                                    
                                case "AUTH_DB_URL":
                                    props.setProperty("auth.datasource.url", value);
                                    System.setProperty("auth.datasource.url", value);
                                    log.info("Setting auth.datasource.url = {}", value);

                                    props.setProperty("auth.datasource.driver-class-name", "org.postgresql.Driver");
                                    System.setProperty("auth.datasource.driver-class-name", "org.postgresql.Driver");
                                    log.info("Setting auth.datasource.driver-class-name = org.postgresql.Driver");
                                    break;
                                    
                                case "AUTH_DB_USERNAME":
                                    props.setProperty("auth.datasource.username", value);
                                    System.setProperty("auth.datasource.username", value);
                                    log.info("Setting auth.datasource.username = {}", value);
                                    break;
                                    
                                case "AUTH_DB_PASSWORD":
                                    props.setProperty("auth.datasource.password", value);
                                    System.setProperty("auth.datasource.password", value);
                                    log.info("Setting auth.datasource.password = [MASKED]");
                                    break;
                                    
                                default:
                                    if (key.contains("PASSWORD")) {
                                        log.info("Setting environment variable: {} = [MASKED]", key);
                                    } else {
                                        log.info("Setting environment variable: {} = {}", key, value);
                                    }
                            }
                        }
                    }
                }

                PropertiesPropertySource propertySource = new PropertiesPropertySource("dotenv", props);
                environment.getPropertySources().addFirst(propertySource);

                log.info("Environment variables loaded from .env file and set as system properties:");
                verifyAndLogProperty("DB_URL");
                verifyAndLogProperty("DB_USERNAME");
                verifyAndLogProperty("DB_PASSWORD");
                verifyAndLogProperty("AUTH_DB_URL");
                verifyAndLogProperty("AUTH_DB_USERNAME");
                verifyAndLogProperty("AUTH_DB_PASSWORD");
                verifyAndLogProperty("spring.datasource.url");
                verifyAndLogProperty("spring.datasource.username");
                verifyAndLogProperty("spring.datasource.password");
                verifyAndLogProperty("auth.datasource.url");
                verifyAndLogProperty("auth.datasource.username");
                verifyAndLogProperty("auth.datasource.password");
                
                log.info("Loaded environment variables from .env file: {}", props.keySet());
                
                log.info("DB_URL from environment: {}", environment.getProperty("DB_URL"));
                log.info("spring.datasource.url from environment: {}", environment.getProperty("spring.datasource.url"));
            } else {
                boolean isTestEnvironment = false;
                
                for (String profile : environment.getActiveProfiles()) {
                    if (profile.equals("test")) {
                        isTestEnvironment = true;
                        break;
                    }
                }
                
                // Check for JUnit classes
                try {
                    Class.forName("org.junit.jupiter.api.Test");
                    isTestEnvironment = true;
                } catch (ClassNotFoundException e) {
                    // Not in a test environment
                }
                
                // Check for CI environment variables (like in GitHub Actions)
                if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
                    isTestEnvironment = true;
                    log.info("Running in CI environment, skipping .env file requirement");
                }
                
                if (isTestEnvironment) {
                    log.info(".env file not found at {}, but we're in a test environment so continuing anyway", envPath);
                    Properties testProps = new Properties();
                    testProps.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                    testProps.setProperty("spring.datasource.username", "sa");
                    testProps.setProperty("spring.datasource.password", "sa");
                    testProps.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
                    testProps.setProperty("auth.datasource.url", "jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                    testProps.setProperty("auth.datasource.username", "sa");
                    testProps.setProperty("auth.datasource.password", "sa");
                    testProps.setProperty("auth.datasource.driver-class-name", "org.h2.Driver");
                    
                    environment.getPropertySources().addFirst(new PropertiesPropertySource("testProperties", testProps));
                    
                    for (String key : testProps.stringPropertyNames()) {
                        System.setProperty(key, testProps.getProperty(key));
                    }
                } else {
                    log.error("ERROR: .env file not found at {}. Database configuration will fail.", envPath);
                    throw new IllegalStateException("Required .env file not found at " + envPath);
                }
            }
        } catch (IOException e) {
            log.error("Error reading .env file: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to read .env file", e);
        }
    }
    
    private void verifyAndLogProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            log.warn("WARNING: Property '{}' is not set or is empty", key);
        } else {
            if (key.contains("PASSWORD")) {
                log.info("Property '{}' is set", key);
            } else {
                log.info("Property '{}' is set to: {}", key, value);
            }
        }
    }
    
    // Method removed as it's been replaced by verifyAndLogProperty
}
