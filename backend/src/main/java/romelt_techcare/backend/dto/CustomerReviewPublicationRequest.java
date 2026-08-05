package romelt_techcare.backend.dto;

/**
 * Controls whether an approved review is featured when published.
 */
public record CustomerReviewPublicationRequest(

        Boolean isFeatured
) {
}