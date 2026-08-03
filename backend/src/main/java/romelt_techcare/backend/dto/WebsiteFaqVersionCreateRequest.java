package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating a new draft FAQ version.
 *
 * Backend-managed fields:
 * - FAQ ownership
 * - version number
 * - version status
 * - publication information
 * - archival information
 * - administrator attribution
 * ================================================================
 */
public record WebsiteFaqVersionCreateRequest(

        @Size(
                max = 120,
                message = "FAQ category must not exceed 120 characters."
        )
        String faqCategory,

        @NotBlank(message = "FAQ question is required.")
        @Size(
                max = 500,
                message = "FAQ question must not exceed 500 characters."
        )
        String question,

        @NotBlank(message = "FAQ answer is required.")
        String answer,

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        Boolean isFeatured,

        Boolean isPublic,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}