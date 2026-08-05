package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Carries optional moderation notes when approving a review.
 */
public record CustomerReviewApprovalRequest(

        @Size(max = 5000)
        String moderationNotes
) {
}