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
import romelt_techcare.backend.dto.PublicWebsiteServiceFeatureResponse;
import romelt_techcare.backend.mapper.WebsiteServiceFeatureMapper;
import romelt_techcare.backend.service.WebsiteServiceFeatureService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC SERVICE FEATURE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active features belonging to currently published website
 * service versions.
 *
 * Public filtering:
 * Returned features must belong to:
 * - an active and non-deleted stable service;
 * - the stable service's current published version;
 * - a PUBLISHED service version;
 * - a public service version;
 * - a currently effective service version.
 *
 * Base endpoint:
 * /api/v1/public/website-service-features
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-service-features")
@RequiredArgsConstructor
public class PublicWebsiteServiceFeatureController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteServiceFeatureService
            websiteServiceFeatureService;

    private final WebsiteServiceFeatureMapper
            websiteServiceFeatureMapper;

    /**
     * Retrieves active public features by stable service code.
     *
     * GET /api/v1/public/website-service-features/by-code/REMOTE_SUPPORT
     */
    @GetMapping("/by-code/{serviceCode}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceFeatureResponse>
                    >
            > getPublicFeaturesByServiceCode(
            @PathVariable
            String serviceCode,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceFeatureResponse> response =
                websiteServiceFeatureService
                        .getPublicFeaturesByServiceCode(
                                serviceCode
                        )
                        .stream()
                        .map(
                                websiteServiceFeatureMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website service features retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves active public features by service slug.
     *
     * GET /api/v1/public/website-service-features/by-slug/remote-support
     */
    @GetMapping("/by-slug/{serviceSlug}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceFeatureResponse>
                    >
            > getPublicFeaturesByServiceSlug(
            @PathVariable
            String serviceSlug,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceFeatureResponse> response =
                websiteServiceFeatureService
                        .getPublicFeaturesByServiceSlug(
                                serviceSlug
                        )
                        .stream()
                        .map(
                                websiteServiceFeatureMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website service features retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteServiceFeatureResponse>
                    >
            > publicResponse(
            String message,
            List<PublicWebsiteServiceFeatureResponse> response,
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