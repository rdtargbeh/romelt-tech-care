package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating a stable FAQ identity.
 *
 * Version lifecycle:
 * Draft and published version identifiers are intentionally excluded.
 * They are managed by WebsiteFaqVersionService.
 * ================================================================
 */
public record WebsiteFaqCreateRequest(

        @NotBlank(message = "FAQ key is required.")
        @Size(
                max = 120,
                message = "FAQ key must not exceed 120 characters."
        )
        String faqKey,

        WebsiteFaqStatus faqStatus
) {
}