package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteBusinessProfileResponse;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.mapper.WebsiteBusinessProfileMapper;
import romelt_techcare.backend.service.WebsiteBusinessProfileService;

import java.time.Duration;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE BUSINESS PROFILE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the active public business identity and branding information
 * used by the Romelt TechCare React website.
 *
 * Endpoint:
 * GET /api/v1/public/website-business-profile
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-business-profile")
@RequiredArgsConstructor
public class PublicWebsiteBusinessProfileController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteBusinessProfileService
            websiteBusinessProfileService;

    private final WebsiteBusinessProfileMapper
            websiteBusinessProfileMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<PublicWebsiteBusinessProfileResponse>
            > getPublicBusinessProfile(
            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .getActiveBusinessProfile();

        PublicWebsiteBusinessProfileResponse response =
                websiteBusinessProfileMapper
                        .toPublicResponse(profile);

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website business profile retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}