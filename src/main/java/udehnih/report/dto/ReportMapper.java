package udehnih.report.dto;

import udehnih.report.model.Report;

/**
 * Utility class for mapping between Report entities and DTOs.
 */
public final class ReportMapper {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ReportMapper() {
        // Utility class should not be instantiated
    }
    /**
     * Converts a ReportRequestDto to a Report entity.
     * 
     * @param dto the DTO to convert
     * @return the Report entity, or null if dto is null
     */
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

    /**
     * Converts a Report entity to a ReportResponseDto.
     * 
     * @param report the Report entity to convert
     * @return the DTO, or null if report is null
     */
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