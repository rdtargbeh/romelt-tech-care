package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of an existing draft FAQ
 * version.
 *
 * Rules:
 * Only DRAFT FAQ versions may be updated.
 * ================================================================
 */
public record WebsiteFaqVersionUpdateRequest(

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

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        @NotNull(message = "Featured status is required.")
        Boolean isFeatured,

        @NotNull(message = "Public status is required.")
        Boolean isPublic,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}