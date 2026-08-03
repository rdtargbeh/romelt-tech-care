package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.CustomerReviewPublicSubmissionRequest;
import romelt_techcare.backend.dto.CustomerReviewRatingSummaryResponse;
import romelt_techcare.backend.dto.PublicCustomerReviewResponse;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.mapper.CustomerReviewMapper;
import romelt_techcare.backend.service.CustomerReviewService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC CUSTOMER REVIEW CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Supports secure verified-review submission and exposes approved
 * public customer reviews to the React website.
 *
 * Security:
 * - Review submission responses are not cached.
 * - Public review responses exclude customer contact information,
 *   consent evidence, IP addresses, and moderation details.
 *
 * Base endpoint:
 * /api/v1/public/customer-reviews
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/customer-reviews")
@RequiredArgsConstructor
public class PublicCustomerReviewController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final CustomerReviewService customerReviewService;

    private final CustomerReviewMapper customerReviewMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<PublicCustomerReviewResponse>>
    submitReview(
            @Valid
            @RequestBody
            CustomerReviewPublicSubmissionRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview requestedReview =
                CustomerReview.builder()
                        .reviewerDisplayName(
                                request.reviewerDisplayName()
                        )
                        .reviewerDisplayPreference(
                                request.reviewerDisplayPreference()
                        )
                        .reviewTitle(request.reviewTitle())
                        .reviewText(request.reviewText())
                        .rating(request.rating())
                        .customerConsentConfirmed(
                                request.customerConsentConfirmed()
                        )
                        .build();

        CustomerReview review =
                customerReviewService.submitVerifiedReview(
                        request.token(),
                        requestedReview,
                        request.serviceId(),
                        request.customerPhotoMediaId(),
                        request.customerConsentVersion(),
                        resolveClientIp(httpRequest),
                        httpRequest.getHeader("User-Agent")
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Thank you. Your review was submitted and is awaiting moderation.",
                                customerReviewMapper
                                        .toPublicResponse(review),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<PublicCustomerReviewResponse>>
            > getPublicReviews(
            @PageableDefault(
                    size = 10,
                    sort = "publishedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<PublicCustomerReviewResponse> response =
                customerReviewService
                        .getPublicReviews(pageable)
                        .map(
                                customerReviewMapper
                                        ::toPublicResponse
                        );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public customer reviews retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping("/featured")
    public ResponseEntity<
            ApiResponse<List<PublicCustomerReviewResponse>>
            > getFeaturedReviews(
            HttpServletRequest httpRequest
    ) {
        List<PublicCustomerReviewResponse> response =
                customerReviewService
                        .getFeaturedPublicReviews()
                        .stream()
                        .map(
                                customerReviewMapper
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
                                "Featured customer reviews retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping("/service/{serviceSlug}")
    public ResponseEntity<
            ApiResponse<Page<PublicCustomerReviewResponse>>
            > getPublicReviewsByService(
            @PathVariable
            String serviceSlug,

            @PageableDefault(
                    size = 10,
                    sort = "publishedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<PublicCustomerReviewResponse> response =
                customerReviewService
                        .getPublicReviewsByServiceSlug(
                                serviceSlug,
                                pageable
                        )
                        .map(
                                customerReviewMapper
                                        ::toPublicResponse
                        );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Customer reviews for the service retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping("/rating-summary")
    public ResponseEntity<
            ApiResponse<CustomerReviewRatingSummaryResponse>
            > getRatingSummary(
            HttpServletRequest httpRequest
    ) {
        CustomerReviewRatingSummaryResponse response =
                customerReviewService
                        .getPublicRatingSummary();

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Customer rating summary retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    private String resolveClientIp(
            HttpServletRequest request
    ) {
        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (
                forwardedFor != null
                        && !forwardedFor.isBlank()
        ) {
            String firstAddress =
                    forwardedFor.split(",")[0].trim();

            return firstAddress.length() > 64
                    ? firstAddress.substring(0, 64)
                    : firstAddress;
        }

        String remoteAddress =
                request.getRemoteAddr();

        if (remoteAddress == null) {
            return null;
        }

        return remoteAddress.length() > 64
                ? remoteAddress.substring(0, 64)
                : remoteAddress;
    }
}