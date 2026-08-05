package romelt_techcare.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Marks a review as spam.
 */
public record CustomerReviewSpamRequest(

        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal spamScore,

        @Size(max = 5000)
        String moderationNotes
) {
}