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
import romelt_techcare.backend.dto.PublicWebsiteNavigationItemResponse;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.mapper.WebsiteNavigationItemMapper;
import romelt_techcare.backend.service.WebsiteNavigationItemService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC NAVIGATION ITEM CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes visible, non-deleted website navigation items to the React
 * public website.
 *
 * Base endpoint:
 * /api/v1/public/website-navigation
 *
 * Security:
 * Only items passing the service's public-availability rules are
 * returned.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-navigation")
@RequiredArgsConstructor
public class PublicWebsiteNavigationItemController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteNavigationItemService
            websiteNavigationItemService;

    private final WebsiteNavigationItemMapper
            websiteNavigationItemMapper;

    /**
     * Retrieves all visible navigation items grouped by their location
     * field in each response item.
     *
     * GET /api/v1/public/website-navigation
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteNavigationItemResponse>
                    >
            > getAllPublicNavigationItems(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteNavigationItemResponse> response =
                websiteNavigationItemService
                        .getAllPublicNavigationItems()
                        .stream()
                        .map(
                                websiteNavigationItemMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website navigation retrieved successfully.",
                response,
                httpRequest
        );
    }

    /**
     * Retrieves visible items for one navigation location.
     *
     * GET /api/v1/public/website-navigation/HEADER
     */
    @GetMapping("/{navigationLocation}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteNavigationItemResponse>
                    >
            > getPublicNavigationItems(
            @PathVariable
            WebsiteNavigationLocation navigationLocation,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteNavigationItemResponse> response =
                websiteNavigationItemService
                        .getPublicNavigationItems(
                                navigationLocation
                        )
                        .stream()
                        .map(
                                websiteNavigationItemMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website navigation retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteNavigationItemResponse>
                    >
            > publicResponse(
            String message,
            List<PublicWebsiteNavigationItemResponse> response,
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