package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable stable identity of one website FAQ.
 *
 * Version pointers are excluded because they are managed by
 * WebsiteFaqVersionService.
 * ================================================================
 */
public record WebsiteFaqUpdateRequest(

        @NotBlank(message = "FAQ key is required.")
        @Size(
                max = 120,
                message = "FAQ key must not exceed 120 characters."
        )
        String faqKey,

        @NotNull(message = "FAQ status is required.")
        WebsiteFaqStatus faqStatus
) {
}