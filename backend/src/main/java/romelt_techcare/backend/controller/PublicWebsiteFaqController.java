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
import romelt_techcare.backend.dto.PublicWebsiteFaqResponse;
import romelt_techcare.backend.mapper.WebsiteFaqMapper;
import romelt_techcare.backend.service.WebsiteFaqService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE FAQ CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active, non-deleted FAQ identities that have a current
 * published-version pointer.
 *
 * Actual FAQ questions, answers, categories, display order, and page
 * placement will be returned by the WebsiteFaqVersion public API.
 *
 * Base endpoint:
 * /api/v1/public/website-faqs
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-faqs")
@RequiredArgsConstructor
public class PublicWebsiteFaqController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteFaqService websiteFaqService;

    private final WebsiteFaqMapper websiteFaqMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqResponse>>
            > getPublicFaqs(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteFaqResponse> response =
                websiteFaqService
                        .getPublicFaqs()
                        .stream()
                        .map(websiteFaqMapper::toPublicResponse)
                        .toList();

        return listResponse(
                "Public website FAQs retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-key/{faqKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteFaqResponse>
            > getPublicFaqByKey(
            @PathVariable
            String faqKey,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteFaqResponse response =
                websiteFaqMapper.toPublicResponse(
                        websiteFaqService
                                .getPublicFaqByKey(faqKey)
                );

        return singleResponse(
                "Public website FAQ retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<List<PublicWebsiteFaqResponse>>
            > listResponse(
            String message,
            List<PublicWebsiteFaqResponse> response,
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
            ApiResponse<PublicWebsiteFaqResponse>
            > singleResponse(
            String message,
            PublicWebsiteFaqResponse response,
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