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
import romelt_techcare.backend.dto.PublicWebsiteServiceVersionResponse;
import romelt_techcare.backend.mapper.WebsiteServiceVersionMapper;
import romelt_techcare.backend.service.WebsiteServiceVersionService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC SERVICE CONTENT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the current published content for active website services.
 *
 * Public filtering:
 * Only versions that are public, published, currently effective, and
 * referenced by an active service's publishedVersionId are returned.
 *
 * Base endpoint:
 * /api/v1/public/website-service-content
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-service-content")
@RequiredArgsConstructor
public class PublicWebsiteServiceVersionController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteServiceVersionService
            websiteServiceVersionService;

    private final WebsiteServiceVersionMapper
            websiteServiceVersionMapper;

    /**
     * Retrieves all currently public services.
     *
     * GET /api/v1/public/website-service-content
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceVersionResponse>
                    >
            > getPublicServices(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceVersionResponse> response =
                websiteServiceVersionService
                        .getPublicServices()
                        .stream()
                        .map(
                                websiteServiceVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Public website services retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves featured public services.
     *
     * GET /api/v1/public/website-service-content/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceVersionResponse>
                    >
            > getFeaturedPublicServices(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceVersionResponse> response =
                websiteServiceVersionService
                        .getFeaturedPublicServices()
                        .stream()
                        .map(
                                websiteServiceVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Featured public website services retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves public services available for booking.
     *
     * GET /api/v1/public/website-service-content/bookable
     */
    @GetMapping("/bookable")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceVersionResponse>
                    >
            > getBookablePublicServices(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceVersionResponse> response =
                websiteServiceVersionService
                        .getBookablePublicServices()
                        .stream()
                        .map(
                                websiteServiceVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Bookable public website services retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves one public service by stable code.
     *
     * GET /api/v1/public/website-service-content/by-code/REMOTE_SUPPORT
     */
    @GetMapping("/by-code/{serviceCode}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteServiceVersionResponse>
            > getPublicServiceByCode(
            @PathVariable
            String serviceCode,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteServiceVersionResponse response =
                websiteServiceVersionMapper.toPublicResponse(
                        websiteServiceVersionService
                                .getPublicServiceByCode(
                                        serviceCode
                                )
                );

        return singleResponse(
                "Public website service retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves one public service by slug.
     *
     * GET /api/v1/public/website-service-content/by-slug/remote-support
     */
    @GetMapping("/by-slug/{serviceSlug}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteServiceVersionResponse>
            > getPublicServiceBySlug(
            @PathVariable
            String serviceSlug,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteServiceVersionResponse response =
                websiteServiceVersionMapper.toPublicResponse(
                        websiteServiceVersionService
                                .getPublicServiceBySlug(
                                        serviceSlug
                                )
                );

        return singleResponse(
                "Public website service retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceVersionResponse>
                    >
            > listResponse(
            String message,
            List<PublicWebsiteServiceVersionResponse> response,
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

    private ResponseEntity<
            ApiResponse<PublicWebsiteServiceVersionResponse>
            > singleResponse(
            String message,
            PublicWebsiteServiceVersionResponse response,
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