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
import romelt_techcare.backend.dto.WebsitePageVersionCreateRequest;
import romelt_techcare.backend.dto.WebsitePageVersionResponse;
import romelt_techcare.backend.dto.WebsitePageVersionUpdateRequest;
import romelt_techcare.backend.entity.WebsitePageVersion;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;
import romelt_techcare.backend.mapper.WebsitePageVersionMapper;
import romelt_techcare.backend.service.WebsitePageVersionService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PAGE VERSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for creating drafts,
 * editing content, publishing versions, archiving history, and reading
 * version history.
 *
 * Base endpoint:
 * /api/v1/admin/website-page-versions
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-page-versions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePageVersionController {

    private final WebsitePageVersionService
            websitePageVersionService;

    private final WebsitePageVersionMapper
            websitePageVersionMapper;

    /**
     * Creates a new page draft.
     *
     * POST /api/v1/admin/website-page-versions/page/{websitePageId}/draft
     */
    @PostMapping("/page/{websitePageId}/draft")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > createDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @Valid
            @RequestBody
            WebsitePageVersionCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePageVersion version =
                websitePageVersionService.createDraft(
                        websitePageId,
                        websitePageVersionMapper.toEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website page draft created successfully.",
                                websitePageVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Creates a draft by copying the current published version.
     *
     * POST /api/v1/admin/website-page-versions/page/{websitePageId}/draft-from-published
     */
    @PostMapping(
            "/page/{websitePageId}/draft-from-published"
    )
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > createDraftFromPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @RequestParam(required = false)
            String changeSummary,

            HttpServletRequest httpRequest
    ) {
        WebsitePageVersion version =
                websitePageVersionService
                        .createDraftFromPublishedVersion(
                                websitePageId,
                                changeSummary,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Draft created from the published page version.",
                                websitePageVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates an existing draft.
     *
     * PUT /api/v1/admin/website-page-versions/{pageVersionId}
     */
    @PutMapping("/{pageVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > updateDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pageVersionId,

            @Valid
            @RequestBody
            WebsitePageVersionUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePageVersion version =
                websitePageVersionService.updateDraft(
                        pageVersionId,
                        websitePageVersionMapper
                                .toUpdateEntity(request),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page draft updated successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Retrieves one page version.
     */
    @GetMapping("/{pageVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > getPageVersion(
            @PathVariable
            UUID pageVersionId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website page version retrieved successfully.",
                websitePageVersionService
                        .getPageVersion(pageVersionId),
                httpRequest
        );
    }

    /**
     * Retrieves the page's current draft.
     */
    @GetMapping("/page/{websitePageId}/draft")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > getCurrentDraft(
            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current website page draft retrieved successfully.",
                websitePageVersionService
                        .getCurrentDraft(websitePageId),
                httpRequest
        );
    }

    /**
     * Retrieves the page's current published version.
     */
    @GetMapping("/page/{websitePageId}/published")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > getCurrentPublishedVersion(
            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current published page version retrieved successfully.",
                websitePageVersionService
                        .getCurrentPublishedVersion(
                                websitePageId
                        ),
                httpRequest
        );
    }

    /**
     * Retrieves ordered version history.
     */
    @GetMapping("/page/{websitePageId}/history")
    public ResponseEntity<
            ApiResponse<Page<WebsitePageVersionResponse>>
            > getVersionHistory(
            @PathVariable
            UUID websitePageId,

            @RequestParam(required = false)
            WebsitePageVersionStatus versionStatus,

            @PageableDefault(
                    size = 10,
                    sort = "versionNumber",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePageVersionResponse> response =
                websitePageVersionService
                        .getVersionHistory(
                                websitePageId,
                                versionStatus,
                                pageable
                        )
                        .map(
                                websitePageVersionMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website page version history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Publishes a draft.
     */
    @PatchMapping("/{pageVersionId}/publish")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > publishDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pageVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsitePageVersion version =
                websitePageVersionService.publishDraft(
                        pageVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page published successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Archives a draft or published version.
     */
    @PatchMapping("/{pageVersionId}/archive")
    public ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > archiveVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pageVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsitePageVersion version =
                websitePageVersionService.archiveVersion(
                        pageVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page version archived successfully.",
                version,
                httpRequest
        );
    }

    /**
     * Permanently deletes a draft.
     *
     * Published and archived history cannot be deleted through this
     * endpoint.
     */
    @DeleteMapping("/{pageVersionId}/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pageVersionId
    ) {
        websitePageVersionService.deleteDraft(
                pageVersionId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<
            ApiResponse<WebsitePageVersionResponse>
            > successfulResponse(
            String message,
            WebsitePageVersion version,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websitePageVersionMapper.toResponse(
                                version
                        ),
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