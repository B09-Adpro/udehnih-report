package udehnih.report.enums;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RejectionMessageTest {
    @Test

    void testIncompleteDetailMessage() {
        assertEquals("Detail laporan kurang lengkap", RejectionMessage.INCOMPLETE_DETAIL.getMessage());
    }
    @Test

    void testSimilarReportMessage() {
        assertEquals("Laporan serupa sudah ada", RejectionMessage.SIMILAR_REPORT.getMessage());
    }
    @Test

    void testOtherMessage() {
        assertEquals("Alasan lain", RejectionMessage.OTHER.getMessage());
    }
    @Test

    void testEnumValues() {
        RejectionMessage[] values = RejectionMessage.values();
        assertEquals(3, values.length);
        assertEquals(RejectionMessage.INCOMPLETE_DETAIL, values[0]);
        assertEquals(RejectionMessage.SIMILAR_REPORT, values[1]);
        assertEquals(RejectionMessage.OTHER, values[2]);
    }
    @Test

    void testEnumValueOf() {
        assertEquals(RejectionMessage.INCOMPLETE_DETAIL, RejectionMessage.valueOf("INCOMPLETE_DETAIL"));
        assertEquals(RejectionMessage.SIMILAR_REPORT, RejectionMessage.valueOf("SIMILAR_REPORT"));
        assertEquals(RejectionMessage.OTHER, RejectionMessage.valueOf("OTHER"));
    }
    @Test

    void testEnumValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> RejectionMessage.valueOf("INVALID_VALUE"));
    }
    @Test

    void testMessageNotNull() {
        for (RejectionMessage message : RejectionMessage.values()) {
            assertNotNull(message.getMessage(), "Message should not be null for " + message.name());
        }
    }
    @Test

    void testMessageUniqueness() {
        String[] messages = new String[RejectionMessage.values().length];
        int i = 0;
        for (RejectionMessage message : RejectionMessage.values()) {
            String messageText = message.getMessage();
            for (int j = 0; j < i; j++) {
                assertNotEquals(messages[j], messageText, "Messages should be unique");
            }
            messages[i++] = messageText;
        }
    }
} 