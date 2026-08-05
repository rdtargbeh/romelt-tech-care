package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteBusinessHourExceptionResponse;
import romelt_techcare.backend.mapper.WebsiteBusinessHourExceptionMapper;
import romelt_techcare.backend.service.WebsiteBusinessHourExceptionService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BUSINESS-HOUR EXCEPTION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active-profile holidays, closures, and special operating
 * hours to the public React website.
 *
 * Base endpoint:
 * /api/v1/public/website-business-hour-exceptions
 *
 * Usage:
 * The frontend should request a relevant date range rather than all
 * historical exceptions.
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/public/website-business-hour-exceptions"
)
@RequiredArgsConstructor
public class PublicWebsiteBusinessHourExceptionController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteBusinessHourExceptionService
            websiteBusinessHourExceptionService;

    private final WebsiteBusinessHourExceptionMapper
            websiteBusinessHourExceptionMapper;

    /**
     * Retrieves public exceptions within an inclusive date range.
     *
     * GET /api/v1/public/website-business-hour-exceptions
     *     ?startDate=2026-08-01
     *     &endDate=2026-09-01
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsiteBusinessHourExceptionResponse>
                    >
            > getPublicBusinessHourExceptions(
            @RequestParam
            LocalDate startDate,

            @RequestParam
            LocalDate endDate,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteBusinessHourExceptionResponse> response =
                websiteBusinessHourExceptionService
                        .getPublicBusinessHourExceptions(
                                startDate,
                                endDate
                        )
                        .stream()
                        .map(
                                websiteBusinessHourExceptionMapper
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
                                "Public website business-hour exceptions retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Retrieves an exception for one date.
     *
     * GET /api/v1/public/website-business-hour-exceptions/by-date
     *     ?date=2026-12-25
     */
    @GetMapping("/by-date")
    public ResponseEntity<
            ApiResponse<PublicWebsiteBusinessHourExceptionResponse>
            > getPublicBusinessHourExceptionByDate(
            @RequestParam("date")
            LocalDate exceptionDate,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteBusinessHourExceptionResponse response =
                websiteBusinessHourExceptionMapper.toPublicResponse(
                        websiteBusinessHourExceptionService
                                .getPublicBusinessHourExceptionByDate(
                                        exceptionDate
                                )
                );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website business-hour exception retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}