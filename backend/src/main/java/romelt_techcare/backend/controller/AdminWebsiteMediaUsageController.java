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
import romelt_techcare.backend.dto.WebsiteMediaUsageResponse;
import romelt_techcare.backend.dto.WebsiteMediaUsageSummaryResponse;
import romelt_techcare.backend.entity.WebsiteMediaUsage;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;
import romelt_techcare.backend.mapper.WebsiteMediaUsageMapper;
import romelt_techcare.backend.service.WebsiteMediaUsageService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE MEDIA USAGE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Provides protected, read-only administrator endpoints for viewing
 * where website media assets are used.
 *
 * Responsibilities:
 * - Retrieves one media usage record.
 * - Lists references for a media asset.
 * - Lists media references belonging to a CMS resource.
 * - Reports whether a media asset can be deleted.
 *
 * Write policy:
 * Generic create, update, replace, and delete endpoints are
 * intentionally not exposed. Usage records are managed internally by
 * the CMS service that owns the referenced resource.
 *
 * Security:
 * All endpoints require an authenticated administrator. The current
 * role model allows SUPER_ADMIN, ADMIN, and STAFF to inspect usage
 * information. Write operations remain internal service operations.
 *
 * Base endpoint:
 * /api/v1/admin/website-media-usages
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-media-usages")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')"
)
public class AdminWebsiteMediaUsageController {

    private final WebsiteMediaUsageService websiteMediaUsageService;
    private final WebsiteMediaUsageMapper websiteMediaUsageMapper;

    /**
     * Retrieves one media usage.
     *
     * GET /api/v1/admin/website-media-usages/{mediaUsageId}
     */
    @GetMapping("/{mediaUsageId}")
    public ResponseEntity<
            ApiResponse<WebsiteMediaUsageResponse>
            > getUsage(
            @PathVariable
            UUID mediaUsageId,

            HttpServletRequest request
    ) {
        WebsiteMediaUsage usage =
                websiteMediaUsageService.getUsage(
                        mediaUsageId
                );

        WebsiteMediaUsageResponse response =
                websiteMediaUsageMapper.toResponse(
                        usage
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media usage retrieved successfully.",
                        response,
                        request.getRequestURI()
                )
        );
    }

    /**
     * Lists references for one media asset.
     *
     * GET /api/v1/admin/website-media-usages/by-media/{mediaAssetId}
     */
    @GetMapping("/by-media/{mediaAssetId}")
    public ResponseEntity<
            ApiResponse<Page<WebsiteMediaUsageResponse>>
            > getUsagesByMediaAsset(
            @PathVariable
            UUID mediaAssetId,

            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest request
    ) {
        Page<WebsiteMediaUsageResponse> response =
                websiteMediaUsageService
                        .getUsagesByMediaAsset(
                                mediaAssetId,
                                pageable
                        )
                        .map(
                                websiteMediaUsageMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media usages retrieved successfully.",
                        response,
                        request.getRequestURI()
                )
        );
    }

    /**
     * Lists media references belonging to one CMS resource.
     *
     * GET /api/v1/admin/website-media-usages/by-resource
     *     ?resourceType=WEBSITE_PAGE_VERSION
     *     &resourceId={uuid}
     */
    @GetMapping("/by-resource")
    public ResponseEntity<
            ApiResponse<List<WebsiteMediaUsageResponse>>
            > getUsagesByResource(
            @RequestParam
            WebsiteMediaUsageResourceType resourceType,

            @RequestParam
            UUID resourceId,

            HttpServletRequest request
    ) {
        List<WebsiteMediaUsageResponse> response =
                websiteMediaUsageService
                        .getUsagesByResource(
                                resourceType,
                                resourceId
                        )
                        .stream()
                        .map(
                                websiteMediaUsageMapper::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Resource media usages retrieved successfully.",
                        response,
                        request.getRequestURI()
                )
        );
    }

    /**
     * Returns the current deletion-protection state for a media asset.
     *
     * GET /api/v1/admin/website-media-usages/by-media/{mediaAssetId}/summary
     */
    @GetMapping("/by-media/{mediaAssetId}/summary")
    public ResponseEntity<
            ApiResponse<WebsiteMediaUsageSummaryResponse>
            > getMediaUsageSummary(
            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest request
    ) {
        long usageCount =
                websiteMediaUsageService.countUsages(
                        mediaAssetId
                );

        WebsiteMediaUsageSummaryResponse response =
                websiteMediaUsageMapper.toSummaryResponse(
                        mediaAssetId,
                        usageCount
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media usage summary retrieved successfully.",
                        response,
                        request.getRequestURI()
                )
        );
    }
}