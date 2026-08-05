package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteSocialLinkResponse;
import romelt_techcare.backend.mapper.WebsiteSocialLinkMapper;
import romelt_techcare.backend.service.WebsiteSocialLinkService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SOCIAL LINK CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active Romelt TechCare social and external-profile links to
 * the public React website.
 *
 * Responsibilities:
 * - Returns active links only.
 * - Returns links in administrator-controlled display order.
 * - Excludes identifiers, administrator data, and internal metadata.
 * - Adds short-lived public cache headers.
 *
 * Endpoint:
 * GET /api/v1/public/website-social-links
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-social-links")
@RequiredArgsConstructor
public class PublicWebsiteSocialLinkController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteSocialLinkService
            websiteSocialLinkService;

    private final WebsiteSocialLinkMapper
            websiteSocialLinkMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteSocialLinkResponse>>
            > getPublicSocialLinks(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteSocialLinkResponse> response =
                websiteSocialLinkService
                        .getActivePublicSocialLinks()
                        .stream()
                        .map(
                                websiteSocialLinkMapper
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
                                "Public website social links retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}