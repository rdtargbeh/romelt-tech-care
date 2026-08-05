package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating a stable website
 * pricing-plan identity.
 *
 * Version lifecycle:
 * Draft and published version identifiers are intentionally excluded.
 * They are managed only by the pricing-plan version lifecycle.
 * ================================================================
 */
public record WebsitePricingPlanCreateRequest(

        @NotBlank(message = "Plan code is required.")
        @Size(
                max = 100,
                message = "Plan code must not exceed 100 characters."
        )
        String planCode,

        @NotBlank(message = "Plan slug is required.")
        @Size(
                max = 180,
                message = "Plan slug must not exceed 180 characters."
        )
        String planSlug,

        WebsitePricingPlanStatus planStatus
) {
}