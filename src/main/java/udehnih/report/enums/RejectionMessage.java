package udehnih.report.enums;
public enum RejectionMessage {
    INCOMPLETE_DETAIL("Detail laporan kurang lengkap"),
    SIMILAR_REPORT("Laporan serupa sudah ada"),
    OTHER("Alasan lain");
    private final String message;
    RejectionMessage(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
} 