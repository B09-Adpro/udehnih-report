package udehnih.report.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
@Configuration

@EnableJpaRepositories(
    basePackages = "udehnih.report.repository",
    entityManagerFactoryRef = "mainEntityManagerFactory",
    transactionManagerRef = "mainTransactionManager"
)
@EntityScan(basePackages = "udehnih.report.model")
public class MainDataSourceConfig {

    public MainDataSourceConfig() {
    }
    @Primary

    @Bean
    @ConfigurationProperties("spring.datasource")

    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }
    @Primary

    @Bean

    public DataSource mainDataSource() {

 

       return mainDataSourceProperties().initializeDataSourceBuilder().build();
    }
    @Primary

    @Bean

    public LocalContainerEntityManagerFactoryBean mainEntityManagerFactory(
            final EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(mainDataSource())
                .packages("udehnih.report.model")
                .persistenceUnit("main")
                .build();
    }
    @Primary

    @Bean

    public JpaTransactionManager mainTransactionManager(
            @Qualifier("mainEntityManagerFactory") final EntityManagerFactory emFactory) {
        return new JpaTransactionManager(emFactory);
    }
}
