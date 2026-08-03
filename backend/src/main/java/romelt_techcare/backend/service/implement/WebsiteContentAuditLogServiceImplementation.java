package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.dto.WebsiteContentAuditContext;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteContentAuditLog;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteContentAuditLogRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT LOG SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements immutable website-content audit recording and history
 * retrieval.
 *
 * Transaction behavior:
 * recordAudit uses MANDATORY propagation. The calling CMS operation
 * must already be transactional. This prevents a content change from
 * committing without its required audit record.
 *
 * Append-only policy:
 * No audit update or deletion operation is implemented.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteContentAuditLogServiceImplementation
        implements WebsiteContentAuditLogService {

    private final WebsiteContentAuditLogRepository
            websiteContentAuditLogRepository;

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public WebsiteContentAuditLog recordAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            String resourceName,
            JsonNode beforeDataJson,
            JsonNode afterDataJson,
            String changeSummary,
            WebsiteContentAuditContext context
    ) {
        if (action == null) {
            throw badRequest(
                    "Website content audit action is required."
            );
        }

        if (resourceType == null) {
            throw badRequest(
                    "Website content audit resource type is required."
            );
        }

        validateJsonObject(
                beforeDataJson,
                "Before-data JSON"
        );

        validateJsonObject(
                afterDataJson,
                "After-data JSON"
        );

        AdminUser administrator =
                administratorId == null
                        ? null
                        : getRequiredAdministrator(
                        administratorId
                );

        WebsiteContentAuditLog auditLog =
                WebsiteContentAuditLog.builder()
                        .adminUser(administrator)
                        .action(action)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .resourceName(
                                truncate(
                                        resourceName,
                                        255
                                )
                        )
                        .beforeDataJson(beforeDataJson)
                        .afterDataJson(afterDataJson)
                        .changeSummary(
                                truncate(
                                        changeSummary,
                                        1000
                                )
                        )
                        .ipAddress(
                                truncate(
                                        context == null
                                                ? null
                                                : context.ipAddress(),
                                        64
                                )
                        )
                        .userAgent(
                                truncate(
                                        context == null
                                                ? null
                                                : context.userAgent(),
                                        500
                                )
                        )
                        .build();

        return websiteContentAuditLogRepository
                .saveAndFlush(auditLog);
    }

    @Override
    public WebsiteContentAuditLog getAuditLog(
            UUID contentAuditLogId
    ) {
        requireIdentifier(
                contentAuditLogId,
                "Content audit log ID"
        );

        return websiteContentAuditLogRepository
                .findByContentAuditLogId(
                        contentAuditLogId
                )
                .orElseThrow(() -> notFound(
                        "Website content audit log was not found."
                ));
    }

    @Override
    public Page<WebsiteContentAuditLog> searchAuditLogs(
            String keyword,
            UUID adminUserId,
            WebsiteContentAuditAction action,
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            Instant createdFrom,
            Instant createdUntil,
            Pageable pageable
    ) {
        requirePageable(pageable);
        validateTimeRange(createdFrom, createdUntil);

        return websiteContentAuditLogRepository.searchAuditLogs(
                normalizeOptional(keyword),
                adminUserId,
                action,
                resourceType,
                resourceId,
                createdFrom,
                createdUntil,
                pageable
        );
    }

    @Override
    public Page<WebsiteContentAuditLog> getResourceHistory(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            Pageable pageable
    ) {
        if (resourceType == null) {
            throw badRequest(
                    "Audit resource type is required."
            );
        }

        requireIdentifier(
                resourceId,
                "Resource ID"
        );

        requirePageable(pageable);

        return websiteContentAuditLogRepository
                .findAllByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        resourceType,
                        resourceId,
                        pageable
                );
    }

    @Override
    public Page<WebsiteContentAuditLog> getAdministratorHistory(
            UUID administratorId,
            Pageable pageable
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        requirePageable(pageable);

        if (!adminUserRepository.existsById(administratorId)) {
            throw notFound(
                    "Administrator account was not found."
            );
        }

        return websiteContentAuditLogRepository
                .findAllByAdminUser_AdminUserIdOrderByCreatedAtDesc(
                        administratorId,
                        pageable
                );
    }

    @Override
    public long countResourceAuditLogs(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId
    ) {
        if (resourceType == null) {
            throw badRequest(
                    "Audit resource type is required."
            );
        }

        requireIdentifier(
                resourceId,
                "Resource ID"
        );

        return websiteContentAuditLogRepository
                .countByResourceTypeAndResourceId(
                        resourceType,
                        resourceId
                );
    }

    @Override
    public long countAuditLogsByAction(
            WebsiteContentAuditAction action
    ) {
        if (action == null) {
            throw badRequest(
                    "Audit action is required."
            );
        }

        return websiteContentAuditLogRepository
                .countByAction(action);
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private void validateJsonObject(
            JsonNode value,
            String fieldName
    ) {
        if (value != null && !value.isObject()) {
            throw badRequest(
                    fieldName + " must be a JSON object."
            );
        }
    }

    private void validateTimeRange(
            Instant createdFrom,
            Instant createdUntil
    ) {
        if (
                createdFrom != null
                        && createdUntil != null
                        && !createdUntil.isAfter(createdFrom)
        ) {
            throw badRequest(
                    "createdUntil must be later than createdFrom."
            );
        }
    }

    private String truncate(
            String value,
            int maximumLength
    ) {
        String normalized =
                normalizeOptional(value);

        if (
                normalized == null
                        || normalized.length() <= maximumLength
        ) {
            return normalized;
        }

        return normalized.substring(
                0,
                maximumLength
        );
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
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

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}