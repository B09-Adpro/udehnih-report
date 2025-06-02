package udehnih.report;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ReportApplication {
    public static void main(final String[] args) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            
            System.setProperty("AUTH_DB_URL", dotenv.get("AUTH_DB_URL"));
            System.setProperty("AUTH_DB_USERNAME", dotenv.get("AUTH_DB_USERNAME"));
            System.setProperty("AUTH_DB_PASSWORD", dotenv.get("AUTH_DB_PASSWORD"));

            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
        
        SpringApplication.run(ReportApplication.class, args);
    }
}
