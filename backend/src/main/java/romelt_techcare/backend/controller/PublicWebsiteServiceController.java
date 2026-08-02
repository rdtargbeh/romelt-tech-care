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
import romelt_techcare.backend.dto.PublicWebsiteServiceResponse;
import romelt_techcare.backend.mapper.WebsiteServiceMapper;
import romelt_techcare.backend.service.WebsiteServiceService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SERVICE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active, non-deleted website-service identities that have a
 * current published-version pointer.
 *
 * Actual public service content will be returned by the future
 * WebsiteServiceVersion public endpoint.
 *
 * Base endpoint:
 * /api/v1/public/website-services
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-services")
@RequiredArgsConstructor
public class PublicWebsiteServiceController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteServiceService
            websiteServiceService;

    private final WebsiteServiceMapper
            websiteServiceMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteServiceResponse>>
            > getPublicWebsiteServices(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteServiceResponse> response =
                websiteServiceService
                        .getPublicWebsiteServices()
                        .stream()
                        .map(
                                websiteServiceMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website services retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-code/{serviceCode}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteServiceResponse>
            > getPublicWebsiteServiceByCode(
            @PathVariable
            String serviceCode,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteServiceResponse response =
                websiteServiceMapper.toPublicResponse(
                        websiteServiceService
                                .getPublicWebsiteServiceByCode(
                                        serviceCode
                                )
                );

        return singlePublicResponse(
                "Public website service retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-slug/{serviceSlug}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteServiceResponse>
            > getPublicWebsiteServiceBySlug(
            @PathVariable
            String serviceSlug,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteServiceResponse response =
                websiteServiceMapper.toPublicResponse(
                        websiteServiceService
                                .getPublicWebsiteServiceBySlug(
                                        serviceSlug
                                )
                );

        return singlePublicResponse(
                "Public website service retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<List<PublicWebsiteServiceResponse>>
            > publicResponse(
            String message,
            List<PublicWebsiteServiceResponse> response,
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
            ApiResponse<PublicWebsiteServiceResponse>
            > singlePublicResponse(
            String message,
            PublicWebsiteServiceResponse response,
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