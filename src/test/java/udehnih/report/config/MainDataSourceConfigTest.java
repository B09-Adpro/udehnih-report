package udehnih.report.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainDataSourceConfigTest {

    @InjectMocks
    private MainDataSourceConfig mainDataSourceConfig;

    @Mock
    private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testMainDataSourceProperties() {
        // Test that the method returns a valid DataSourceProperties object
        DataSourceProperties properties = mainDataSourceConfig.mainDataSourceProperties();
        assertNotNull(properties);
    }

    @Test
    void testMainDataSource() {
        // Set test environment indicator
        try {
            System.setProperty("spring.profiles.active", "test");
            
            // Test that the method returns a valid DataSource object
            DataSource dataSource = mainDataSourceConfig.mainDataSource();
            assertNotNull(dataSource);
            
            // Verify it's a HikariDataSource
            assertTrue(dataSource instanceof com.zaxxer.hikari.HikariDataSource);
            
            // Verify the JDBC URL contains H2 for test environment
            com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
            assertEquals("sa", hikariDataSource.getUsername());
            assertEquals("sa", hikariDataSource.getPassword());
            assertEquals("org.h2.Driver", hikariDataSource.getDriverClassName());
        } finally {
            System.clearProperty("spring.profiles.active");
        }
    }

    @Test
    void testMainEntityManagerFactory() {
        // Create a builder mock that properly handles the builder pattern
        EntityManagerFactoryBuilder.Builder builderMock = mock(EntityManagerFactoryBuilder.Builder.class);
        
        // Set up the builder chain
        when(entityManagerFactoryBuilder.dataSource(any(DataSource.class))).thenReturn(builderMock);
        when(builderMock.packages(any(String[].class))).thenReturn(builderMock);
        when(builderMock.persistenceUnit(anyString())).thenReturn(builderMock);
        when(builderMock.properties(anyMap())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(localContainerEntityManagerFactoryBean);

        // Test that the method returns a valid EntityManagerFactoryBean
        LocalContainerEntityManagerFactoryBean result = mainDataSourceConfig.mainEntityManagerFactory(entityManagerFactoryBuilder);
        assertNotNull(result);
        assertEquals(localContainerEntityManagerFactoryBean, result);
        
        // Verify the builder was called with the correct parameters
        verify(entityManagerFactoryBuilder).dataSource(any(DataSource.class));
        verify(builderMock).packages(any(String[].class));
        verify(builderMock).persistenceUnit(anyString());
        verify(builderMock).properties(anyMap());
        verify(builderMock).build();
    }

    @Test
    void testMainTransactionManager() {
        // Test that the method returns a valid JpaTransactionManager
        JpaTransactionManager transactionManager = mainDataSourceConfig.mainTransactionManager(entityManagerFactory);
        assertNotNull(transactionManager);
        assertEquals(entityManagerFactory, transactionManager.getEntityManagerFactory());
    }
}
