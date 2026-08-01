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
import romelt_techcare.backend.dto.WebsiteMediaAssetAccessibilityRequest;
import romelt_techcare.backend.dto.WebsiteMediaAssetCreateRequest;
import romelt_techcare.backend.dto.WebsiteMediaAssetFocalPointRequest;
import romelt_techcare.backend.dto.WebsiteMediaAssetResponse;
import romelt_techcare.backend.dto.WebsiteMediaAssetUpdateRequest;
import romelt_techcare.backend.dto.WebsiteMediaAssetVisibilityRequest;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.mapper.WebsiteMediaAssetMapper;
import romelt_techcare.backend.service.WebsiteMediaAssetService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE MEDIA ASSET CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing media used
 * by the Romelt TechCare public website and CMS.
 *
 * Responsibilities:
 * - Registers uploaded file metadata as website media.
 * - Updates media metadata and accessibility information.
 * - Searches and retrieves media records.
 * - Manages public visibility.
 * - Manages upload and processing lifecycle states.
 * - Archives, restores, and soft-deletes media assets.
 * - Uses the authenticated JWT administrator as the actor for every
 *   write operation.
 *
 * Base endpoint:
 * /api/v1/admin/website-media-assets
 *
 * Authorization:
 * These endpoints are protected by the administrator JWT security
 * configuration. The authenticated administrator ID is taken from
 * AdminJwtPrincipal and is never accepted from request payloads.
 *
 * File-storage integration:
 * This controller manages media metadata only. Binary file upload,
 * download, virus scanning, optimization, and physical deletion are
 * handled by the shared file-storage module.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-media-assets")
@RequiredArgsConstructor
public class AdminWebsiteMediaAssetController {

    private final WebsiteMediaAssetService websiteMediaAssetService;
    private final WebsiteMediaAssetMapper websiteMediaAssetMapper;

    /**
     * Creates a website media asset metadata record.
     *
     * Endpoint:
     * POST /api/v1/admin/website-media-assets
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    createMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteMediaAssetCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        UUID administratorId =
                requireAdministratorId(principal);

        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetMapper.toEntity(request);

        WebsiteMediaAsset createdMediaAsset =
                websiteMediaAssetService.createMediaAsset(
                        mediaAsset,
                        administratorId
                );

        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        createdMediaAsset
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website media asset created successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates editable media metadata.
     *
     * Endpoint:
     * PUT /api/v1/admin/website-media-assets/{mediaAssetId}
     */
    @PutMapping("/{mediaAssetId}")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    updateMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            @Valid
            @RequestBody
            WebsiteMediaAssetUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        UUID administratorId =
                requireAdministratorId(principal);

        WebsiteMediaAsset requestedUpdate =
                websiteMediaAssetMapper.toUpdateEntity(request);

        WebsiteMediaAsset updatedMediaAsset =
                websiteMediaAssetService.updateMediaAsset(
                        mediaAssetId,
                        requestedUpdate,
                        administratorId
                );

        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        updatedMediaAsset
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media asset updated successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Searches non-deleted media assets.
     *
     * Example:
     * GET /api/v1/admin/website-media-assets
     *     ?keyword=logo
     *     &assetStatus=ACTIVE
     *     &isPublic=true
     *     &mimeType=image/png
     *     &page=0
     *     &size=10
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteMediaAssetResponse>>
            > searchMediaAssets(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsiteMediaAssetStatus assetStatus,

            @RequestParam(required = false)
            Boolean isPublic,

            @RequestParam(required = false)
            String mimeType,

            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteMediaAssetResponse> response =
                websiteMediaAssetService
                        .searchMediaAssets(
                                keyword,
                                assetStatus,
                                isPublic,
                                mimeType,
                                pageable
                        )
                        .map(
                                websiteMediaAssetMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media assets retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one non-deleted media asset.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/{mediaAssetId}
     */
    @GetMapping("/{mediaAssetId}")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    getMediaAsset(
            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.getMediaAsset(
                        mediaAssetId
                );

        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        mediaAsset
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media asset retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves a media asset by stable asset key.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/by-key/{assetKey}
     */
    @GetMapping("/by-key/{assetKey}")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    getMediaAssetByAssetKey(
            @PathVariable
            String assetKey,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService
                        .getMediaAssetByAssetKey(assetKey);

        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        mediaAsset
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media asset retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves the media asset connected to a file attachment.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/by-file-attachment/{id}
     */
    @GetMapping(
            "/by-file-attachment/{fileAttachmentId}"
    )
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    getMediaAssetByFileAttachmentId(
            @PathVariable
            UUID fileAttachmentId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService
                        .getMediaAssetByFileAttachmentId(
                                fileAttachmentId
                        );

        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        mediaAsset
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website media asset retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves soft-deleted media assets.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/deleted
     */
    @GetMapping("/deleted")
    public ResponseEntity<
            ApiResponse<Page<WebsiteMediaAssetResponse>>
            > getDeletedMediaAssets(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteMediaAssetResponse> response =
                websiteMediaAssetService
                        .getDeletedMediaAssets(pageable)
                        .map(
                                websiteMediaAssetMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website media assets retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Marks an asset as uploading.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/uploading
     */
    @PatchMapping("/{mediaAssetId}/uploading")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    markUploading(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.markUploading(
                        mediaAssetId,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset marked as uploading.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Marks an asset as processing.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/processing
     */
    @PatchMapping("/{mediaAssetId}/processing")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    markProcessing(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.markProcessing(
                        mediaAssetId,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset marked as processing.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Marks upload or processing as failed.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/failed
     */
    @PatchMapping("/{mediaAssetId}/failed")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    markFailed(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.markFailed(
                        mediaAssetId,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset marked as failed.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Activates a media asset.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/activate
     *
     * makePublic defaults to false so activation does not
     * unintentionally expose media.
     */
    @PatchMapping("/{mediaAssetId}/activate")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    activateMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            @RequestParam(defaultValue = "false")
            boolean makePublic,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.activateMediaAsset(
                        mediaAssetId,
                        makePublic,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset activated successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Archives a media asset.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/archive
     */
    @PatchMapping("/{mediaAssetId}/archive")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    archiveMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.archiveMediaAsset(
                        mediaAssetId,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset archived successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Restores an archived media asset.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/restore
     */
    @PatchMapping("/{mediaAssetId}/restore")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    restoreMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.restoreMediaAsset(
                        mediaAssetId,
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media asset restored successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Updates public visibility.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/visibility
     */
    @PatchMapping("/{mediaAssetId}/visibility")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    updatePublicVisibility(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            @Valid
            @RequestBody
            WebsiteMediaAssetVisibilityRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.updatePublicVisibility(
                        mediaAssetId,
                        request.isPublic(),
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media visibility updated successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Updates alternative text and decorative status.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/accessibility
     */
    @PatchMapping("/{mediaAssetId}/accessibility")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    updateAccessibility(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            @Valid
            @RequestBody
            WebsiteMediaAssetAccessibilityRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.updateAccessibility(
                        mediaAssetId,
                        request.altText(),
                        request.isDecorative(),
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media accessibility updated successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Updates image focal-point percentages.
     *
     * Endpoint:
     * PATCH /api/v1/admin/website-media-assets/{id}/focal-point
     */
    @PatchMapping("/{mediaAssetId}/focal-point")
    public ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    updateFocalPoint(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId,

            @Valid
            @RequestBody
            WebsiteMediaAssetFocalPointRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService.updateFocalPoint(
                        mediaAssetId,
                        request.focalPointX(),
                        request.focalPointY(),
                        requireAdministratorId(principal)
                );

        return successfulMediaResponse(
                "Website media focal point updated successfully.",
                mediaAsset,
                httpRequest
        );
    }

    /**
     * Soft-deletes a media asset.
     *
     * Endpoint:
     * DELETE /api/v1/admin/website-media-assets/{id}
     */
    @DeleteMapping("/{mediaAssetId}")
    public ResponseEntity<Void> deleteMediaAsset(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID mediaAssetId
    ) {
        websiteMediaAssetService.deleteMediaAsset(
                mediaAssetId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the active public media count.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/counts/active-public
     */
    @GetMapping("/counts/active-public")
    public ResponseEntity<ApiResponse<Long>>
    countActivePublicMediaAssets(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteMediaAssetService
                        .countActivePublicMediaAssets();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active public media asset count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Returns the soft-deleted media count.
     *
     * Endpoint:
     * GET /api/v1/admin/website-media-assets/counts/deleted
     */
    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedMediaAssets(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteMediaAssetService
                        .countDeletedMediaAssets();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted media asset count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Builds a standard successful media response.
     */
    private ResponseEntity<ApiResponse<WebsiteMediaAssetResponse>>
    successfulMediaResponse(
            String message,
            WebsiteMediaAsset mediaAsset,
            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAssetResponse response =
                websiteMediaAssetMapper.toResponse(
                        mediaAsset
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Returns the authenticated administrator ID.
     */
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