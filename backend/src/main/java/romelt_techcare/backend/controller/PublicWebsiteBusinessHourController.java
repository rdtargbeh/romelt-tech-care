package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteBusinessHourResponse;
import romelt_techcare.backend.mapper.WebsiteBusinessHourMapper;
import romelt_techcare.backend.service.WebsiteBusinessHourService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE BUSINESS HOUR CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the active Romelt TechCare normal weekly schedule to the
 * public React website.
 *
 * Endpoint:
 * GET /api/v1/public/website-business-hours
 *
 * Important:
 * This endpoint returns the normal schedule only. Date-specific
 * exceptions will be returned by the business-hour exception module
 * or a future resolved-schedule endpoint.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-business-hours")
@RequiredArgsConstructor
public class PublicWebsiteBusinessHourController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteBusinessHourService
            websiteBusinessHourService;

    private final WebsiteBusinessHourMapper
            websiteBusinessHourMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteBusinessHourResponse>>
            > getPublicBusinessHours(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteBusinessHourResponse> response =
                websiteBusinessHourService
                        .getActivePublicBusinessHours()
                        .stream()
                        .map(
                                websiteBusinessHourMapper
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
                                "Public website business hours retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}