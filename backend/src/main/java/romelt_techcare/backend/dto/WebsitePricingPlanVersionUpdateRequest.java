package romelt_techcare.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteBillingInterval;
import romelt_techcare.backend.enums.WebsitePricingModel;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of an existing draft
 * pricing-plan version.
 *
 * Rules:
 * - Only DRAFT pricing-plan versions may be updated.
 * - Version ownership, version number, lifecycle status, publishing,
 *   and archival information are backend-managed.
 * ================================================================
 */
public record WebsitePricingPlanVersionUpdateRequest(

        @NotBlank(message = "Plan name is required.")
        @Size(
                max = 180,
                message = "Plan name must not exceed 180 characters."
        )
        String planName,

        @Size(
                max = 500,
                message = "Short description must not exceed 500 characters."
        )
        String shortDescription,

        String fullDescription,

        @NotNull(message = "Pricing model is required.")
        WebsitePricingModel pricingModel,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Pricing amount must not be negative."
        )
        BigDecimal amount,

        @NotBlank(message = "Currency code is required.")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency code must contain exactly three letters."
        )
        String currencyCode,

        @Size(
                max = 100,
                message = "Price prefix must not exceed 100 characters."
        )
        String pricePrefix,

        @Size(
                max = 100,
                message = "Price suffix must not exceed 100 characters."
        )
        String priceSuffix,

        WebsiteBillingInterval billingInterval,

        @Size(
                max = 180,
                message = "Call-to-action label must not exceed 180 characters."
        )
        String callToActionLabel,

        @Size(
                max = 1000,
                message = "Call-to-action URL must not exceed 1000 characters."
        )
        String callToActionUrl,

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        @NotNull(message = "Recommended status is required.")
        Boolean isRecommended,

        @NotNull(message = "Featured status is required.")
        Boolean isFeatured,

        @NotNull(message = "Public status is required.")
        Boolean isPublic,

        Instant effectiveFrom,

        Instant effectiveUntil,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}