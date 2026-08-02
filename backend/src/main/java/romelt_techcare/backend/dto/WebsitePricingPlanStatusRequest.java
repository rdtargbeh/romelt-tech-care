package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the lifecycle status of one stable website pricing plan.
 * ================================================================
 */
public record WebsitePricingPlanStatusRequest(

        @NotNull(message = "Plan status is required.")
        WebsitePricingPlanStatus planStatus
) {
}