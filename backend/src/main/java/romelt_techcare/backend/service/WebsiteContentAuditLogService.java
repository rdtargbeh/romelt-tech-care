package romelt_techcare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.WebsiteContentAuditContext;
import romelt_techcare.backend.entity.WebsiteContentAuditLog;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT LOG SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines append-only audit recording and authorized audit retrieval
 * operations for the website CMS.
 *
 * Important transaction rule:
 * CMS services should call recordAudit within the same transaction as
 * the content change. This ensures both the content change and audit
 * entry succeed or roll back together.
 * ================================================================
 */
public interface WebsiteContentAuditLogService {

    WebsiteContentAuditLog recordAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            String resourceName,
            JsonNode beforeDataJson,
            JsonNode afterDataJson,
            String changeSummary,
            WebsiteContentAuditContext context
    );

    WebsiteContentAuditLog getAuditLog(
            UUID contentAuditLogId
    );

    Page<WebsiteContentAuditLog> searchAuditLogs(
            String keyword,
            UUID adminUserId,
            WebsiteContentAuditAction action,
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            Instant createdFrom,
            Instant createdUntil,
            Pageable pageable
    );

    Page<WebsiteContentAuditLog> getResourceHistory(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            Pageable pageable
    );

    Page<WebsiteContentAuditLog> getAdministratorHistory(
            UUID administratorId,
            Pageable pageable
    );

    long countResourceAuditLogs(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId
    );

    long countAuditLogsByAction(
            WebsiteContentAuditAction action
    );
}