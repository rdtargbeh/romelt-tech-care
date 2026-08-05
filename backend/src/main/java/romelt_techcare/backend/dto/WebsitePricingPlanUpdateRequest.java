package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable stable identity of one website
 * pricing plan.
 *
 * Version pointers are excluded because they are controlled by the
 * pricing-plan version lifecycle.
 * ================================================================
 */
public record WebsitePricingPlanUpdateRequest(

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

        @NotNull(message = "Plan status is required.")
        WebsitePricingPlanStatus planStatus
) {
}