package udehnih.report.util;

/**
 * Application-wide constants to avoid duplicated string literals
 */
public final class AppConstants {
    // Authentication related constants
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String EMAIL_FIELD = "email";
    public static final String ERROR_KEY = "error";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String STUDENT_ROLE = "STUDENT";
    
    // Report related constants
    public static final String REPORT_NOT_FOUND_MSG = "Report not found with id: ";
    
    // Test constants
    public static final String TEST_STUDENT_ID = "12345";
    
    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
