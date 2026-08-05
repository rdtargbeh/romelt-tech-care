package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the reason and optional notes for rejecting a review.
 */
public record CustomerReviewRejectionRequest(

        @NotBlank
        @Size(max = 500)
        String rejectionReason,

        @Size(max = 5000)
        String moderationNotes
) {
}