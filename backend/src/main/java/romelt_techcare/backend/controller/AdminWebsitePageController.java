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
import romelt_techcare.backend.dto.WebsitePageCreateRequest;
import romelt_techcare.backend.dto.WebsitePageResponse;
import romelt_techcare.backend.dto.WebsitePageStatusRequest;
import romelt_techcare.backend.dto.WebsitePageUpdateRequest;
import romelt_techcare.backend.dto.WebsitePageVersionReferenceRequest;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsitePageType;
import romelt_techcare.backend.mapper.WebsitePageMapper;
import romelt_techcare.backend.service.WebsitePageService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE PAGE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing stable React
 * page identities and their draft/published version pointers.
 *
 * Base endpoint:
 * /api/v1/admin/website-pages
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-pages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePageController {

    private final WebsitePageService websitePageService;
    private final WebsitePageMapper websitePageMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    createWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsitePageCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.createWebsitePage(
                        websitePageMapper.toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website page created successfully.",
                                websitePageMapper.toResponse(page),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{websitePageId}")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    updateWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @Valid
            @RequestBody
            WebsitePageUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.updateWebsitePage(
                        websitePageId,
                        websitePageMapper.toUpdateEntity(request),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page updated successfully.",
                page,
                httpRequest
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WebsitePageResponse>>>
    searchWebsitePages(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsitePageType pageType,

            @RequestParam(required = false)
            Boolean isSystemPage,

            @RequestParam(required = false)
            Boolean isActive,

            @PageableDefault(
                    size = 10,
                    sort = "pageName",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePageResponse> response =
                websitePageService
                        .searchWebsitePages(
                                keyword,
                                pageType,
                                isSystemPage,
                                isActive,
                                pageable
                        )
                        .map(websitePageMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pages retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<Page<WebsitePageResponse>>>
    getDeletedWebsitePages(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePageResponse> response =
                websitePageService
                        .getDeletedWebsitePages(pageable)
                        .map(websitePageMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website pages retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{websitePageId}")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    getWebsitePage(
            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website page retrieved successfully.",
                websitePageService.getWebsitePage(
                        websitePageId
                ),
                httpRequest
        );
    }

    @GetMapping("/by-key/{pageKey}")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    getWebsitePageByKey(
            @PathVariable
            String pageKey,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website page retrieved successfully.",
                websitePageService.getWebsitePageByKey(
                        pageKey
                ),
                httpRequest
        );
    }

    @GetMapping("/by-route")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    getWebsitePageByRoute(
            @RequestParam
            String routePath,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website page retrieved successfully.",
                websitePageService.getWebsitePageByRoute(
                        routePath
                ),
                httpRequest
        );
    }

    @PatchMapping("/{websitePageId}/draft-version")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    assignDraftVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @Valid
            @RequestBody
            WebsitePageVersionReferenceRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.assignDraftVersion(
                        websitePageId,
                        request.pageVersionId(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Draft page version assigned successfully.",
                page,
                httpRequest
        );
    }

    @DeleteMapping("/{websitePageId}/draft-version")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    clearDraftVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.clearDraftVersion(
                        websitePageId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Draft page version cleared successfully.",
                page,
                httpRequest
        );
    }

    @PatchMapping("/{websitePageId}/published-version")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    assignPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @Valid
            @RequestBody
            WebsitePageVersionReferenceRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.assignPublishedVersion(
                        websitePageId,
                        request.pageVersionId(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Published page version assigned successfully.",
                page,
                httpRequest
        );
    }

    @DeleteMapping("/{websitePageId}/published-version")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    clearPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.clearPublishedVersion(
                        websitePageId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Published page version cleared successfully.",
                page,
                httpRequest
        );
    }

    @PatchMapping("/{websitePageId}/status")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    updateWebsitePageStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            @Valid
            @RequestBody
            WebsitePageStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.updateWebsitePageStatus(
                        websitePageId,
                        request.isActive(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website-page status updated successfully.",
                page,
                httpRequest
        );
    }

    @PatchMapping("/{websitePageId}/activate")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    activateWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.activateWebsitePage(
                        websitePageId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page activated successfully.",
                page,
                httpRequest
        );
    }

    @PatchMapping("/{websitePageId}/deactivate")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    deactivateWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.deactivateWebsitePage(
                        websitePageId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page deactivated successfully.",
                page,
                httpRequest
        );
    }

    @DeleteMapping("/{websitePageId}")
    public ResponseEntity<Void> deleteWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId
    ) {
        websitePageService.deleteWebsitePage(
                websitePageId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{websitePageId}/restore")
    public ResponseEntity<ApiResponse<WebsitePageResponse>>
    restoreWebsitePage(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websitePageId,

            HttpServletRequest httpRequest
    ) {
        WebsitePage page =
                websitePageService.restoreWebsitePage(
                        websitePageId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website page restored successfully.",
                page,
                httpRequest
        );
    }

    @GetMapping("/counts/active")
    public ResponseEntity<ApiResponse<Long>>
    countActiveWebsitePages(
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active website-page count retrieved successfully.",
                        websitePageService
                                .countActiveWebsitePages(),
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedWebsitePages(
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website-page count retrieved successfully.",
                        websitePageService
                                .countDeletedWebsitePages(),
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<ApiResponse<WebsitePageResponse>>
    successfulResponse(
            String message,
            WebsitePage page,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websitePageMapper.toResponse(page),
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