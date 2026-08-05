package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries internal audit information from CMS business services to
 * the audit-log service.
 *
 * Usage:
 * This request is intended primarily for internal service-to-service
 * calls. Normal administrators should not manually fabricate audit
 * events through a public request.
 * ================================================================
 */
public record WebsiteContentAuditLogCreateRequest(

        @NotNull(message = "Audit action is required.")
        WebsiteContentAuditAction action,

        @NotNull(message = "Audit resource type is required.")
        WebsiteContentAuditResourceType resourceType,

        UUID resourceId,

        @Size(
                max = 255,
                message = "Resource name must not exceed 255 characters."
        )
        String resourceName,

        JsonNode beforeDataJson,

        JsonNode afterDataJson,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary,

        @Size(
                max = 64,
                message = "IP address must not exceed 64 characters."
        )
        String ipAddress,

        @Size(
                max = 500,
                message = "User agent must not exceed 500 characters."
        )
        String userAgent
) {
}