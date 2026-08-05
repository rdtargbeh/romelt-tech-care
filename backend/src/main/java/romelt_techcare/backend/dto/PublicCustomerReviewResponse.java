package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.CustomerReviewSource;

import java.time.Instant;
import java.util.UUID;

/**
 * Privacy-safe public review response.
 */
public record PublicCustomerReviewResponse(

        UUID customerReviewId,

        String reviewerDisplayName,

        String reviewTitle,

        String reviewText,

        Short rating,

        CustomerReviewSource reviewSource,

        String externalSourceUrl,

        PublicWebsiteMediaAssetResponse customerPhoto,

        Boolean isVerifiedCustomer,

        Boolean isFeatured,

        String serviceName,

        String serviceSlug,

        String adminResponse,

        Instant respondedAt,

        Instant publishedAt
) {
}