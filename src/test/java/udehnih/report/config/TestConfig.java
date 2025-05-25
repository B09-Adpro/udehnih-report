package udehnih.report.config;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import udehnih.report.service.CustomUserDetailsService;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;

/**
 * Test configuration for controller tests.
 * Provides mock beans for all required dependencies.
 */
@TestConfiguration
public class TestConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(5000);
    }
    
    @Bean
    public DataSource mainDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build();
    }
    
    @Bean
    public DataSource authDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("authdb")
            .build();
    }
    
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate mainJdbcTemplate() {
        return new JdbcTemplate(mainDataSource());
    }
    
    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate() {
        return new JdbcTemplate(authDataSource());
    }
    
    @Bean
    public JwtConfig jwtConfig() {
        JwtConfig config = new JwtConfig();
        // Use reflection to set values since we can't use @Value in tests
        try {
            java.lang.reflect.Field secretKeyField = JwtConfig.class.getDeclaredField("secretKey");
            secretKeyField.setAccessible(true);
            secretKeyField.set(config, "testSecretKeyWithAtLeast32Characters12345");
            
            java.lang.reflect.Field expirationField = JwtConfig.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(config, 3600000L); // 1 hour
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }
    
    @Bean
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }
    
    @Bean
    public CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }
    
    @Bean
    public ReportService reportService() {
        return Mockito.mock(ReportService.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // Add AsyncConfig-related beans to fix the AsyncConfigTest
    @Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ReportThread-");
        executor.initialize();
        return executor;
    }
} 