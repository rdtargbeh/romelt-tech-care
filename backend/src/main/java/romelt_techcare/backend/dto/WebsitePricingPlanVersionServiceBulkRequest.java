package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN SERVICE BULK REQUEST
 * ================================================================
 *
 * Purpose:
 * Replaces the complete service membership of one draft pricing-plan
 * version.
 *
 * Behavior:
 * - Existing relationships not included in serviceIds are removed.
 * - Missing relationships included in serviceIds are created.
 * - Duplicate identifiers are ignored after normalization.
 * ================================================================
 */
public record WebsitePricingPlanVersionServiceBulkRequest(

        @NotEmpty(
                message = "At least one website service ID is required."
        )
        List<
                @NotNull(
                        message = "Website service IDs must not contain null."
                )
                        UUID
                > serviceIds
) {
}