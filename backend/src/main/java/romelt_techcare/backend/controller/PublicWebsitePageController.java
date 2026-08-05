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
import romelt_techcare.backend.dto.PublicWebsitePageResponse;
import romelt_techcare.backend.mapper.WebsitePageMapper;
import romelt_techcare.backend.service.WebsitePageService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE PAGE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active website-page identities that have a published-version
 * pointer.
 *
 * Actual page content will be exposed by the page-version module after
 * its schema is implemented.
 *
 * Base endpoint:
 * /api/v1/public/website-pages
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-pages")
@RequiredArgsConstructor
public class PublicWebsitePageController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePageService websitePageService;
    private final WebsitePageMapper websitePageMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsitePageResponse>>
            > getPublicWebsitePages(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePageResponse> response =
                websitePageService
                        .getPublicWebsitePages()
                        .stream()
                        .map(
                                websitePageMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website pages retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping("/by-key/{pageKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsitePageResponse>
            > getPublicWebsitePageByKey(
            @PathVariable
            String pageKey,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePageResponse response =
                websitePageMapper.toPublicResponse(
                        websitePageService
                                .getPublicWebsitePageByKey(
                                        pageKey
                                )
                );

        return publicResponse(
                "Public website page retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-route")
    public ResponseEntity<
            ApiResponse<PublicWebsitePageResponse>
            > getPublicWebsitePageByRoute(
            @RequestParam
            String routePath,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePageResponse response =
                websitePageMapper.toPublicResponse(
                        websitePageService
                                .getPublicWebsitePageByRoute(
                                        routePath
                                )
                );

        return publicResponse(
                "Public website page retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<PublicWebsitePageResponse>
            > publicResponse(
            String message,
            PublicWebsitePageResponse response,
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