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
        Properties props = new Properties();
        boolean envLoaded = false;

        if (checkSystemEnvironmentVariables(props)) {
            log.info("Environment variables found in system environment, using those");
            envLoaded = true;
        } else {
            String[] possibleEnvPaths = {
                System.getProperty("user.dir") + "/.env",
                "/opt/udehnih/.env",
                "/app/.env",
                "/etc/secrets/.env"
            };
            

            for (String envPath : possibleEnvPaths) {
                log.info("Looking for .env file at: {}" , envPath);
                if (Files.exists(Paths.get(envPath))) {
                    log.info(".env file found at {}, loading variables...", envPath);
                    envLoaded = loadEnvFile(envPath, props);
                    if (envLoaded) break;
                }
            }
            
            if (!envLoaded) {
                envLoaded = loadFromRenderSecrets(props);
            }
        }
        
        if (envLoaded) {
            PropertiesPropertySource propertySource = new PropertiesPropertySource("dotenv", props);
            environment.getPropertySources().addFirst(propertySource);
            logConfiguredProperties();
        } else {
            handleMissingEnvironmentConfiguration();
        }
    }
    
    private boolean checkSystemEnvironmentVariables(Properties props) {
        String[] requiredVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
        boolean allFound = true;
        
        for (String key : requiredVars) {
            String value = System.getenv(key);
            if (value != null && !value.isEmpty()) {
                setPropertyWithAppropriateLogging(props, key, value);
                mapToSpringProperties(props, key, value);
            } else {
                allFound = false;
            }
        }
        
        return allFound;
    }
    
    private boolean loadEnvFile(String envPath, Properties props) {
        try {
            log.info(".env file found, loading variables...");
                
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
                return true;
            } catch (IOException e) {
                log.error("Error reading .env file {}: {}", envPath, e.getMessage());
                return false;
            }
        }
        
        private boolean loadFromRenderSecrets(Properties props) {
            String secretsDir = "/etc/secrets";
            log.info("Checking for Render secrets in directory: {}", secretsDir);
            
            if (!Files.exists(Paths.get(secretsDir))) {
                log.info("Secrets directory {} does not exist", secretsDir);
                return false;
            }
            
            String[] requiredVars = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "AUTH_DB_URL", "AUTH_DB_USERNAME", "AUTH_DB_PASSWORD"};
            boolean anyLoaded = false;
            
            for (String varName : requiredVars) {
                String secretPath = secretsDir + "/" + varName;
                if (Files.exists(Paths.get(secretPath))) {
                    try {
                        String value = Files.readString(Paths.get(secretPath)).trim();
                        if (!value.isEmpty()) {
                            setPropertyWithAppropriateLogging(props, varName, value);
                            mapToSpringProperties(props, varName, value);
                            anyLoaded = true;
                            log.info("Loaded secret from file: {}", secretPath);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read secret file {}: {}", secretPath, e.getMessage());
                    }
                } else {
                    log.debug("Secret file not found: {}", secretPath);
                }
            }
            
            if (anyLoaded) {
                log.info("Successfully loaded environment variables from Render secrets");
            } else {
                log.info("No environment variables found in Render secrets directory");
            }
            
            return anyLoaded;
        }
        
        private void setPropertyWithAppropriateLogging(Properties props, String key, String value) {
            props.setProperty(key, value);
            System.setProperty(key, value);
            
            if (key.contains("PASSWORD")) {
                log.info("Setting environment variable: {} = [MASKED]", key);
            } else {
                log.info("Setting environment variable: {} = {}", key, value);
            }
        }
        
        private void mapToSpringProperties(Properties props, String key, String value) {
            switch (key) {
                case "DB_URL":
                    String dbUrl = value;
                    if (!dbUrl.startsWith("jdbc:")) {
                        dbUrl = "jdbc:" + dbUrl;
                        log.info("Added jdbc: prefix to {}: {}", key, dbUrl);
                    }
                    props.setProperty("spring.datasource.url", dbUrl);
                    System.setProperty("spring.datasource.url", dbUrl);
                    log.info("Setting spring.datasource.url = {}", dbUrl);
                    
                    props.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                    System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
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
                    String authDbUrl = value;
                    if (!authDbUrl.startsWith("jdbc:")) {
                        authDbUrl = "jdbc:" + authDbUrl;
                        log.info("Added jdbc: prefix to {}: {}", key, authDbUrl);
                    }
                    props.setProperty("auth.datasource.url", authDbUrl);
                    System.setProperty("auth.datasource.url", authDbUrl);
                    log.info("Setting auth.datasource.url = {}", authDbUrl);
                    
                    props.setProperty("auth.datasource.driver-class-name", "org.postgresql.Driver");
                    System.setProperty("auth.datasource.driver-class-name", "org.postgresql.Driver");
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
            }
        }
        
        private void logConfiguredProperties() {
            log.info("Environment variables loaded and set as system properties:");
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
        }
        
        private void handleMissingEnvironmentConfiguration() {
            boolean isTestEnvironment = isTestEnvironment();
            
            if (isTestEnvironment) {
                setupTestEnvironment();
            } else {
                log.error("ERROR: No environment configuration found. Database configuration will fail.");
                throw new IllegalStateException("Required environment configuration not found");
            }
        }
        
        private boolean isTestEnvironment() {
            for (String profile : environment.getActiveProfiles()) {
                if (profile.equals("test")) {
                    return true;
                }
            }

            try {
                Class.forName("org.junit.jupiter.api.Test");
                return true;
            } catch (ClassNotFoundException e) {
                // Not in a test environment
            }
            
            if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
                log.info("Running in CI environment, skipping .env file requirement");
                return true;
            }
            
            return false;
        }
        
        private void setupTestEnvironment() {
            log.info("Setting up test environment with H2 database");
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
}
