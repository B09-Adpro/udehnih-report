package udehnih.report.dto;
import udehnih.report.model.Report;
public final class ReportMapper {
    private ReportMapper() {
    }
    public static Report toEntity(final ReportRequestDto dto) {
        Report result = null;
        if (dto == null) {
            return result;
        }
        result = Report.builder()
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .detail(dto.getDetail())
                .build();
        return result;
    }
    public static ReportResponseDto toDto(final Report report) {
        ReportResponseDto result = null;
        if (report == null) {
            return result;
        }
        final ReportResponseDto dto = new ReportResponseDto();
        dto.setReportId(report.getReportId());
        dto.setStudentId(report.getStudentId());
        dto.setTitle(report.getTitle());
        dto.setDetail(report.getDetail());
        dto.setStatus(report.getStatus());
        dto.setRejectionMessage(report.getRejectionMessage());
        if (report.getRejectionMessage() != null) {
            dto.setRejectionMessageText(report.getRejectionMessage().getMessage());
        }
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        result = dto;
        return result;
    }
} 