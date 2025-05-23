package udehnih.report.dto;

import udehnih.report.model.Report;

public class ReportMapper {
    public static Report toEntity(ReportRequestDto dto) {
        if (dto == null) return null;
        return Report.builder()
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .detail(dto.getDetail())
                .build();
    }

    public static ReportResponseDto toDto(Report report) {
        if (report == null) return null;
        ReportResponseDto dto = new ReportResponseDto();
        dto.setReportId(report.getReportId());
        dto.setStudentId(report.getStudentId());
        dto.setTitle(report.getTitle());
        dto.setDetail(report.getDetail());
        dto.setStatus(report.getStatus());
        dto.setRejectionMessage(report.getRejectionMessage());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        return dto;
    }
} 