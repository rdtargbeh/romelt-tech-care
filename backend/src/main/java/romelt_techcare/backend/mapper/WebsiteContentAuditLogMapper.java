package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.WebsiteContentAuditLogCreateRequest;
import romelt_techcare.backend.dto.WebsiteContentAuditLogResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteContentAuditLog;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT LOG MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts audit requests into append-only entities and persisted
 * audit records into administrator-facing responses.
 * ================================================================
 */
@Component
public class WebsiteContentAuditLogMapper {

    public WebsiteContentAuditLog toEntity(
            WebsiteContentAuditLogCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteContentAuditLog.builder()
                .action(request.action())
                .resourceType(request.resourceType())
                .resourceId(request.resourceId())
                .resourceName(request.resourceName())
                .beforeDataJson(request.beforeDataJson())
                .afterDataJson(request.afterDataJson())
                .changeSummary(request.changeSummary())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .build();
    }

    public WebsiteContentAuditLogResponse toResponse(
            WebsiteContentAuditLog auditLog
    ) {
        if (auditLog == null) {
            return null;
        }

        AdminUser administrator =
                auditLog.getAdminUser();

        return new WebsiteContentAuditLogResponse(
                auditLog.getContentAuditLogId(),
                getAdminUserId(administrator),
                getAdminUserDisplayName(administrator),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResourceName(),
                auditLog.getBeforeDataJson(),
                auditLog.getAfterDataJson(),
                auditLog.getChangeSummary(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }

    private UUID getAdminUserId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String getAdminUserDisplayName(
            AdminUser administrator
    ) {
        if (administrator == null) {
            return null;
        }

        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(
                administrator.getEmail()
        );
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}