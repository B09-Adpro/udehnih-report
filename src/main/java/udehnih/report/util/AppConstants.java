package udehnih.report.util;
public final class AppConstants {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String EMAIL_FIELD = "email";
    public static final String ERROR_KEY = "error";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String STUDENT_ROLE = "STUDENT";
    public static final String REPORT_NOT_FOUND_MSG = "Report not found with id: ";
    public static final String TEST_STUDENT_ID = "12345";
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
