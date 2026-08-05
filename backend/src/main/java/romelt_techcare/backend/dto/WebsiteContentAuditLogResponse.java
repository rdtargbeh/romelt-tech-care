package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one CMS audit-log entry to authorized administrators.
 * ================================================================
 */
public record WebsiteContentAuditLogResponse(

        UUID contentAuditLogId,

        UUID adminUserId,

        String adminUserDisplayName,

        WebsiteContentAuditAction action,

        WebsiteContentAuditResourceType resourceType,

        UUID resourceId,

        String resourceName,

        JsonNode beforeDataJson,

        JsonNode afterDataJson,

        String changeSummary,

        String ipAddress,

        String userAgent,

        Instant createdAt
) {
}