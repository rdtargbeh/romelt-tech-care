package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.WebsiteContentAuditLogResponse;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.mapper.WebsiteContentAuditLogMapper;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE CONTENT AUDIT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes read-only administrator endpoints for reviewing CMS audit
 * history.
 *
 * Security:
 * - Audit records cannot be created manually through this controller.
 * - Audit records cannot be changed or deleted.
 * - Only privileged administrators should access before/after data.
 *
 * Base endpoint:
 * /api/v1/admin/website-content-audit-logs
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/admin/website-content-audit-logs"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteContentAuditLogController {

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditLogMapper
            websiteContentAuditLogMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteContentAuditLogResponse>>
            > searchAuditLogs(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            UUID adminUserId,

            @RequestParam(required = false)
            WebsiteContentAuditAction action,

            @RequestParam(required = false)
            WebsiteContentAuditResourceType resourceType,

            @RequestParam(required = false)
            UUID resourceId,

            @RequestParam(required = false)
            Instant createdFrom,

            @RequestParam(required = false)
            Instant createdUntil,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteContentAuditLogResponse> response =
                websiteContentAuditLogService
                        .searchAuditLogs(
                                keyword,
                                adminUserId,
                                action,
                                resourceType,
                                resourceId,
                                createdFrom,
                                createdUntil,
                                pageable
                        )
                        .map(
                                websiteContentAuditLogMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website content audit logs retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{contentAuditLogId}")
    public ResponseEntity<
            ApiResponse<WebsiteContentAuditLogResponse>
            > getAuditLog(
            @PathVariable
            UUID contentAuditLogId,

            HttpServletRequest httpRequest
    ) {
        WebsiteContentAuditLogResponse response =
                websiteContentAuditLogMapper.toResponse(
                        websiteContentAuditLogService
                                .getAuditLog(contentAuditLogId)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website content audit log retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping(
            "/resource/{resourceType}/{resourceId}"
    )
    public ResponseEntity<
            ApiResponse<Page<WebsiteContentAuditLogResponse>>
            > getResourceHistory(
            @PathVariable
            WebsiteContentAuditResourceType resourceType,

            @PathVariable
            UUID resourceId,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteContentAuditLogResponse> response =
                websiteContentAuditLogService
                        .getResourceHistory(
                                resourceType,
                                resourceId,
                                pageable
                        )
                        .map(
                                websiteContentAuditLogMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website content resource history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/administrator/{administratorId}")
    public ResponseEntity<
            ApiResponse<Page<WebsiteContentAuditLogResponse>>
            > getAdministratorHistory(
            @PathVariable
            UUID administratorId,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteContentAuditLogResponse> response =
                websiteContentAuditLogService
                        .getAdministratorHistory(
                                administratorId,
                                pageable
                        )
                        .map(
                                websiteContentAuditLogMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Administrator website-content audit history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping(
            "/resource/{resourceType}/{resourceId}/count"
    )
    public ResponseEntity<ApiResponse<Long>>
    countResourceAuditLogs(
            @PathVariable
            WebsiteContentAuditResourceType resourceType,

            @PathVariable
            UUID resourceId,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteContentAuditLogService
                        .countResourceAuditLogs(
                                resourceType,
                                resourceId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website content resource audit count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/action/{action}/count")
    public ResponseEntity<ApiResponse<Long>>
    countAuditLogsByAction(
            @PathVariable
            WebsiteContentAuditAction action,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteContentAuditLogService
                        .countAuditLogsByAction(action);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website content audit action count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }
}