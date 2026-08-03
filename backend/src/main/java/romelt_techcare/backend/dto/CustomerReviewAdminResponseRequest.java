package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Adds or updates a public administrator response to a review.
 */
public record CustomerReviewAdminResponseRequest(

        @NotBlank
        @Size(max = 10000)
        String adminResponse
) {
}