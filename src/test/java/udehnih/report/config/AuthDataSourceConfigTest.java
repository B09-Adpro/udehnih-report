package udehnih.report.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthDataSourceConfigTest {

    @InjectMocks
    private AuthDataSourceConfig authDataSourceConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test environment
        System.setProperty("spring.profiles.active", "test");
    }

    @AfterEach
    void tearDown() {
        // Clean up environment variables and system properties
        System.clearProperty("spring.profiles.active");
        System.clearProperty("AUTH_DB_URL");
        System.clearProperty("AUTH_DB_USERNAME");
        System.clearProperty("AUTH_DB_PASSWORD");
    }

    @Test
    void testAuthDataSourceProperties() {
        // Test that the method returns a valid DataSourceProperties object
        DataSourceProperties properties = authDataSourceConfig.authDataSourceProperties();
        assertNotNull(properties);
        assertEquals("org.postgresql.Driver", properties.getDriverClassName());
    }

    @Test
    void testAuthDataSourceInTestEnvironment() {
        // Test that the method returns a valid DataSource for test environment
        DataSource dataSource = authDataSourceConfig.authDataSource();
        assertNotNull(dataSource);
        
        // Verify it's a HikariDataSource
        assertTrue(dataSource instanceof com.zaxxer.hikari.HikariDataSource);
        
        // Verify the JDBC URL contains H2 for test environment
        com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
        assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        assertEquals("sa", hikariDataSource.getUsername());
        assertEquals("sa", hikariDataSource.getPassword());
        assertEquals("org.h2.Driver", hikariDataSource.getDriverClassName());
    }

    @Test
    void testAuthJdbcTemplate() {
        // Create a mock DataSource
        DataSource mockDataSource = new com.zaxxer.hikari.HikariDataSource();
        
        // Test that the method returns a valid JdbcTemplate
        JdbcTemplate jdbcTemplate = authDataSourceConfig.authJdbcTemplate(mockDataSource);
        assertNotNull(jdbcTemplate);
        assertEquals(mockDataSource, jdbcTemplate.getDataSource());
    }
}
