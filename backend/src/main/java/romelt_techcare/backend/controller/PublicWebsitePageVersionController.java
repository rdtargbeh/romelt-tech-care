package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsitePageVersionResponse;
import romelt_techcare.backend.mapper.WebsitePageVersionMapper;
import romelt_techcare.backend.service.WebsitePageVersionService;

import java.time.Duration;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PAGE VERSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the current published content for active website pages.
 *
 * Security:
 * Only the version referenced by website_pages.published_version_id
 * and carrying PUBLISHED status is returned.
 *
 * Base endpoint:
 * /api/v1/public/website-page-content
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-page-content")
@RequiredArgsConstructor
public class PublicWebsitePageVersionController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePageVersionService
            websitePageVersionService;

    private final WebsitePageVersionMapper
            websitePageVersionMapper;

    /**
     * Retrieves published content by stable page key.
     *
     * GET /api/v1/public/website-page-content/by-key/HOME
     */
    @GetMapping("/by-key/{pageKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsitePageVersionResponse>
            > getPublicPageByKey(
            @PathVariable
            String pageKey,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePageVersionResponse response =
                websitePageVersionMapper.toPublicResponse(
                        websitePageVersionService
                                .getPublicPageByKey(pageKey)
                );

        return publicResponse(
                "Public website page content retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves published content by React route.
     *
     * GET /api/v1/public/website-page-content/by-route?routePath=/services
     */
    @GetMapping("/by-route")
    public ResponseEntity<
            ApiResponse<PublicWebsitePageVersionResponse>
            > getPublicPageByRoute(
            @RequestParam
            String routePath,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePageVersionResponse response =
                websitePageVersionMapper.toPublicResponse(
                        websitePageVersionService
                                .getPublicPageByRoute(
                                        routePath
                                )
                );

        return publicResponse(
                "Public website page content retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<PublicWebsitePageVersionResponse>
            > publicResponse(
            String message,
            PublicWebsitePageVersionResponse response,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                message,
                                response,
                                request.getRequestURI()
                        )
                );
    }
}