package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN SERVICE LINK CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries one stable website-service identifier to be linked to a
 * draft pricing-plan version.
 *
 * Pricing-plan version ownership:
 * pricingPlanVersionId is supplied through the endpoint path.
 * ================================================================
 */
public record WebsitePricingPlanVersionServiceCreateRequest(

        @NotNull(message = "Website service ID is required.")
        UUID serviceId
) {
}