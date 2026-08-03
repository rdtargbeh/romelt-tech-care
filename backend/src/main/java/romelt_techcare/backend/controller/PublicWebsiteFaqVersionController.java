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
import romelt_techcare.backend.dto.PublicWebsiteFaqVersionResponse;
import romelt_techcare.backend.mapper.WebsiteFaqVersionMapper;
import romelt_techcare.backend.service.WebsiteFaqVersionService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC FAQ CONTENT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes current published FAQ content to the public React website.
 *
 * Public filtering:
 * Returned FAQs must:
 * - belong to active, non-deleted FAQ identities;
 * - be the current published FAQ version;
 * - have PUBLISHED status;
 * - be marked public.
 *
 * Base endpoint:
 * /api/v1/public/website-faq-content
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-faq-content")
@RequiredArgsConstructor
public class PublicWebsiteFaqVersionController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteFaqVersionService
            websiteFaqVersionService;

    private final WebsiteFaqVersionMapper
            websiteFaqVersionMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqVersionResponse>>
            > getPublicFaqs(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteFaqVersionResponse> response =
                websiteFaqVersionService
                        .getPublicFaqs()
                        .stream()
                        .map(
                                websiteFaqVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Public website FAQs retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqVersionResponse>>
            > getFeaturedPublicFaqs(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteFaqVersionResponse> response =
                websiteFaqVersionService
                        .getFeaturedPublicFaqs()
                        .stream()
                        .map(
                                websiteFaqVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Featured public website FAQs retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>>
    getPublicFaqCategories(
            HttpServletRequest httpRequest
    ) {
        List<String> response =
                websiteFaqVersionService
                        .getPublicFaqCategories();

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public FAQ categories retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping("/category/{faqCategory}")
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqVersionResponse>>
            > getPublicFaqsByCategory(
            @PathVariable
            String faqCategory,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteFaqVersionResponse> response =
                websiteFaqVersionService
                        .getPublicFaqsByCategory(faqCategory)
                        .stream()
                        .map(
                                websiteFaqVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Public website FAQs for the category retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-key/{faqKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteFaqVersionResponse>
            > getPublicFaqByKey(
            @PathVariable
            String faqKey,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteFaqVersionResponse response =
                websiteFaqVersionMapper.toPublicResponse(
                        websiteFaqVersionService
                                .getPublicFaqByKey(faqKey)
                );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website FAQ retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    private ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqVersionResponse>>
            > listResponse(
            String message,
            List<PublicWebsiteFaqVersionResponse> response,
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