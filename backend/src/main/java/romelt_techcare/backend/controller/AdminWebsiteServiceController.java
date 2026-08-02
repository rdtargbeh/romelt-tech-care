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
import romelt_techcare.backend.dto.WebsiteServiceCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceResponse;
import romelt_techcare.backend.dto.WebsiteServiceStatusRequest;
import romelt_techcare.backend.dto.WebsiteServiceUpdateRequest;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteServiceStatus;
import romelt_techcare.backend.mapper.WebsiteServiceMapper;
import romelt_techcare.backend.service.WebsiteServiceService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE SERVICE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing stable
 * website-service identities.
 *
 * Responsibilities:
 * - Creates stable services.
 * - Updates service codes, slugs, and lifecycle status.
 * - Retrieves and searches service identities.
 * - Activates, deactivates, and archives services.
 * - Soft-deletes and restores services.
 * - Returns service counts.
 *
 * Version lifecycle:
 * Draft creation, draft editing, publishing, version history, and
 * version archival are exposed by AdminWebsiteServiceVersionController.
 *
 * This controller does not allow arbitrary draftVersionId or
 * publishedVersionId assignment.
 *
 * Base endpoint:
 * /api/v1/admin/website-services
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-services")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteServiceController {

    private final WebsiteServiceService
            websiteServiceService;

    private final WebsiteServiceMapper
            websiteServiceMapper;

    /**
     * Creates a stable website-service identity.
     *
     * POST /api/v1/admin/website-services
     */
    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > createWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteServiceCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .createWebsiteService(
                                websiteServiceMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website service created successfully.",
                                websiteServiceMapper
                                        .toResponse(service),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates the stable identity of a website service.
     *
     * PUT /api/v1/admin/website-services/{serviceId}
     */
    @PutMapping("/{serviceId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > updateWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            @Valid
            @RequestBody
            WebsiteServiceUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .updateWebsiteService(
                                serviceId,
                                websiteServiceMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service updated successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Searches active, non-deleted website services.
     *
     * GET /api/v1/admin/website-services
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteServiceResponse>>
            > searchWebsiteServices(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsiteServiceStatus serviceStatus,

            @PageableDefault(
                    size = 10,
                    sort = "serviceCode",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteServiceResponse> response =
                websiteServiceService
                        .searchWebsiteServices(
                                keyword,
                                serviceStatus,
                                pageable
                        )
                        .map(
                                websiteServiceMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website services retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves soft-deleted website services.
     *
     * GET /api/v1/admin/website-services/deleted
     */
    @GetMapping("/deleted")
    public ResponseEntity<
            ApiResponse<Page<WebsiteServiceResponse>>
            > getDeletedWebsiteServices(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteServiceResponse> response =
                websiteServiceService
                        .getDeletedWebsiteServices(pageable)
                        .map(
                                websiteServiceMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website services retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one website service by identifier.
     *
     * GET /api/v1/admin/website-services/{serviceId}
     */
    @GetMapping("/{serviceId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > getWebsiteService(
            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website service retrieved successfully.",
                websiteServiceService
                        .getWebsiteService(serviceId),
                httpRequest
        );
    }

    /**
     * Retrieves one website service by stable code.
     *
     * GET /api/v1/admin/website-services/by-code/{serviceCode}
     */
    @GetMapping("/by-code/{serviceCode}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > getWebsiteServiceByCode(
            @PathVariable
            String serviceCode,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website service retrieved successfully.",
                websiteServiceService
                        .getWebsiteServiceByCode(serviceCode),
                httpRequest
        );
    }

    /**
     * Retrieves one website service by public slug.
     *
     * GET /api/v1/admin/website-services/by-slug/{serviceSlug}
     */
    @GetMapping("/by-slug/{serviceSlug}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > getWebsiteServiceBySlug(
            @PathVariable
            String serviceSlug,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website service retrieved successfully.",
                websiteServiceService
                        .getWebsiteServiceBySlug(serviceSlug),
                httpRequest
        );
    }

    /**
     * Updates the lifecycle status of a website service.
     *
     * PATCH /api/v1/admin/website-services/{serviceId}/status
     */
    @PatchMapping("/{serviceId}/status")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > updateWebsiteServiceStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            @Valid
            @RequestBody
            WebsiteServiceStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .updateWebsiteServiceStatus(
                                serviceId,
                                request.serviceStatus(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website-service status updated successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Activates a website service.
     *
     * PATCH /api/v1/admin/website-services/{serviceId}/activate
     */
    @PatchMapping("/{serviceId}/activate")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > activateWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .activateWebsiteService(
                                serviceId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service activated successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Deactivates a website service.
     *
     * PATCH /api/v1/admin/website-services/{serviceId}/deactivate
     */
    @PatchMapping("/{serviceId}/deactivate")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > deactivateWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .deactivateWebsiteService(
                                serviceId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service deactivated successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Archives the stable service identity.
     *
     * PATCH /api/v1/admin/website-services/{serviceId}/archive
     */
    @PatchMapping("/{serviceId}/archive")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > archiveWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .archiveWebsiteService(
                                serviceId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service archived successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Soft-deletes a website service.
     *
     * DELETE /api/v1/admin/website-services/{serviceId}
     */
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> deleteWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId
    ) {
        websiteServiceService.deleteWebsiteService(
                serviceId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted website service as inactive.
     *
     * PATCH /api/v1/admin/website-services/{serviceId}/restore
     */
    @PatchMapping("/{serviceId}/restore")
    public ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > restoreWebsiteService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        WebsiteService service =
                websiteServiceService
                        .restoreWebsiteService(
                                serviceId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service restored successfully.",
                service,
                httpRequest
        );
    }

    /**
     * Counts non-deleted services by lifecycle status.
     *
     * GET /api/v1/admin/website-services/counts/status/{serviceStatus}
     */
    @GetMapping("/counts/status/{serviceStatus}")
    public ResponseEntity<ApiResponse<Long>>
    countWebsiteServicesByStatus(
            @PathVariable
            WebsiteServiceStatus serviceStatus,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteServiceService
                        .countWebsiteServicesByStatus(
                                serviceStatus
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website-service count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Counts soft-deleted website services.
     *
     * GET /api/v1/admin/website-services/counts/deleted
     */
    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedWebsiteServices(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteServiceService
                        .countDeletedWebsiteServices();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website-service count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<WebsiteServiceResponse>
            > successfulResponse(
            String message,
            WebsiteService service,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteServiceMapper.toResponse(service),
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