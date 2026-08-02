package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsiteServiceVersionCloneRequest;
import romelt_techcare.backend.dto.WebsiteServiceVersionCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceVersionResponse;
import romelt_techcare.backend.dto.WebsiteServiceVersionUpdateRequest;
import romelt_techcare.backend.entity.WebsiteServiceVersion;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;
import romelt_techcare.backend.mapper.WebsiteServiceVersionMapper;
import romelt_techcare.backend.service.WebsiteServiceVersionService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN SERVICE VERSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for creating drafts,
 * editing service content, publishing versions, archiving versions,
 * deleting drafts, and reviewing service-version history.
 *
 * Base endpoint:
 * /api/v1/admin/website-service-versions
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-service-versions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteServiceVersionController {

    private final WebsiteServiceVersionService
            websiteServiceVersionService;

    private final WebsiteServiceVersionMapper
            websiteServiceVersionMapper;

    /**
     * Creates a new draft for one stable website service.
     *
     * POST /api/v1/admin/website-service-versions/service/{serviceId}/draft
     */
    @PostMapping("/service/{serviceId}/draft")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > createDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            @Valid
            @RequestBody
            WebsiteServiceVersionCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceVersion version =
                websiteServiceVersionService.createDraft(
                        serviceId,
                        websiteServiceVersionMapper
                                .toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website service draft created successfully.",
                                websiteServiceVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Creates a draft by copying the current published version.
     *
     * POST /api/v1/admin/website-service-versions/service/{serviceId}/draft-from-published
     */
    @PostMapping(
            "/service/{serviceId}/draft-from-published"
    )
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > createDraftFromPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            @Valid
            @RequestBody(required = false)
            WebsiteServiceVersionCloneRequest request,

            HttpServletRequest httpRequest
    ) {
        String changeSummary =
                request == null
                        ? null
                        : request.changeSummary();

        WebsiteServiceVersion version =
                websiteServiceVersionService
                        .createDraftFromPublishedVersion(
                                serviceId,
                                changeSummary,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Draft created from the published service version.",
                                websiteServiceVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates an existing draft.
     *
     * PUT /api/v1/admin/website-service-versions/{serviceVersionId}
     */
    @PutMapping("/{serviceVersionId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > updateDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceVersionId,

            @Valid
            @RequestBody
            WebsiteServiceVersionUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceVersion version =
                websiteServiceVersionService.updateDraft(
                        serviceVersionId,
                        websiteServiceVersionMapper
                                .toUpdateEntity(request),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website service draft updated successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Retrieves one service version.
     *
     * GET /api/v1/admin/website-service-versions/{serviceVersionId}
     */
    @GetMapping("/{serviceVersionId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > getServiceVersion(
            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website service version retrieved successfully.",
                websiteServiceVersionService
                        .getServiceVersion(serviceVersionId),
                httpRequest
        );
    }

    /**
     * Retrieves the current draft for one stable service.
     */
    @GetMapping("/service/{serviceId}/draft")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > getCurrentDraft(
            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current website service draft retrieved successfully.",
                websiteServiceVersionService
                        .getCurrentDraft(serviceId),
                httpRequest
        );
    }

    /**
     * Retrieves the current published version.
     */
    @GetMapping("/service/{serviceId}/published")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > getCurrentPublishedVersion(
            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current published service version retrieved successfully.",
                websiteServiceVersionService
                        .getCurrentPublishedVersion(
                                serviceId
                        ),
                httpRequest
        );
    }

    /**
     * Retrieves version history for one stable service.
     */
    @GetMapping("/service/{serviceId}/history")
    public ResponseEntity<
            ApiResponse<Page<WebsiteServiceVersionResponse>>
            > getVersionHistory(
            @PathVariable
            UUID serviceId,

            @RequestParam(required = false)
            WebsiteServiceVersionStatus versionStatus,

            @PageableDefault(
                    size = 10,
                    sort = "versionNumber",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteServiceVersionResponse> response =
                websiteServiceVersionService
                        .getVersionHistory(
                                serviceId,
                                versionStatus,
                                pageable
                        )
                        .map(
                                websiteServiceVersionMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website service version history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Publishes a draft and archives the previous published version.
     */
    @PatchMapping("/{serviceVersionId}/publish")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > publishDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceVersion version =
                websiteServiceVersionService.publishDraft(
                        serviceVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website service published successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Archives a draft or published version.
     */
    @PatchMapping("/{serviceVersionId}/archive")
    public ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > archiveVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceVersion version =
                websiteServiceVersionService.archiveVersion(
                        serviceVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website service version archived successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Permanently deletes a draft.
     *
     * Published and archived versions cannot be deleted here.
     */
    @DeleteMapping("/{serviceVersionId}/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceVersionId
    ) {
        websiteServiceVersionService.deleteDraft(
                serviceVersionId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<
            ApiResponse<WebsiteServiceVersionResponse>
            > successfulResponse(
            String message,
            WebsiteServiceVersion version,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteServiceVersionMapper
                                .toResponse(version),
                        request.getRequestURI()
                )
        );
    }

    private UUID requireAdministratorId(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw new IllegalArgumentException(
                    "Authenticated administrator information is required."
            );
        }

        return principal.adminUserId();
    }
}