package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteMediaAssetResponse;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.service.WebsiteMediaAssetService;

import java.time.Duration;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE MEDIA ASSET CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes safe metadata for active public website media assets.
 *
 * Responsibilities:
 * - Retrieves public media by stable asset key.
 * - Returns only approved public metadata.
 * - Excludes storage keys and administrator information.
 * - Adds short-lived public cache headers.
 *
 * Endpoint:
 * GET /api/v1/public/website-media-assets/{assetKey}
 *
 * Usage examples:
 * - PRIMARY_LOGO
 * - HOME_HERO_IMAGE
 * - SERVICES_HERO_IMAGE
 * - CONTACT_SUPPORT_IMAGE
 *
 * Important:
 * This endpoint returns media metadata, including the public asset URL.
 * Binary delivery may continue through the shared file-storage endpoint
 * or object-storage URL.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-media-assets")
@RequiredArgsConstructor
public class PublicWebsiteMediaAssetController {

    /**
     * Public media metadata may be cached briefly.
     *
     * Publishing or replacing an asset should clear application-level
     * caches or use versioned media URLs.
     */
    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteMediaAssetService
            websiteMediaAssetService;

    /**
     * Returns an active public media asset by its stable key.
     *
     * Endpoint:
     * GET /api/v1/public/website-media-assets/{assetKey}
     */
    @GetMapping("/{assetKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteMediaAssetResponse>
            > getPublicMediaAsset(
            @PathVariable
            String assetKey,

            HttpServletRequest httpRequest
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetService
                        .getPublicMediaAssetByAssetKey(
                                assetKey
                        );

        if (!mediaAsset.isPubliclyAvailable()) {
            throw new IllegalStateException(
                    "The requested media asset is not publicly available."
            );
        }

        PublicWebsiteMediaAssetResponse response =
                PublicWebsiteMediaAssetResponse.from(
                        mediaAsset
                );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website media asset retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}