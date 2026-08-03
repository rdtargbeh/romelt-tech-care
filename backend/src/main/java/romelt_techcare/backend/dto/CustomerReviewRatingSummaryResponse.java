package romelt_techcare.backend.dto;

/**
 * Public aggregate customer-rating information.
 */
public record CustomerReviewRatingSummaryResponse(

        long totalReviews,

        double averageRating,

        long fiveStarReviews,

        long fourStarReviews,

        long threeStarReviews,

        long twoStarReviews,

        long oneStarReviews
) {
}