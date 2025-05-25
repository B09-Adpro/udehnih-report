package udehnih.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportApplication {
    // Spring Boot application class - not a utility class
    // Constructor is required for Spring to properly initialize the application

    public static void main(final String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }

}
