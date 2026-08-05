package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Carries optional moderation notes when hiding a review.
 */
public record CustomerReviewHideRequest(

        @Size(max = 5000)
        String moderationNotes
) {
}