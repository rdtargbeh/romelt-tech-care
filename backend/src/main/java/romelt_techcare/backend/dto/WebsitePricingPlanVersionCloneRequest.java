package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION CLONE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries an optional change summary when creating a new draft by
 * copying the current published pricing-plan version.
 * ================================================================
 */
public record WebsitePricingPlanVersionCloneRequest(

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}