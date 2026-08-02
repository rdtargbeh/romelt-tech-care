package romelt_techcare.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of an existing service draft.
 *
 * Rules:
 * - Only DRAFT service versions may be updated.
 * - Service ownership, version number, lifecycle status, publication
 *   data, and archive data are backend-managed.
 * ================================================================
 */
public record WebsiteServiceVersionUpdateRequest(

        @NotBlank(message = "Service name is required.")
        @Size(
                max = 180,
                message = "Service name must not exceed 180 characters."
        )
        String serviceName,

        @Size(
                max = 500,
                message = "Short description must not exceed 500 characters."
        )
        String shortDescription,

        String fullDescription,

        @Size(
                max = 100,
                message = "Icon key must not exceed 100 characters."
        )
        String iconKey,

        UUID cardImageMediaId,

        UUID heroImageMediaId,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Starting price must not be negative."
        )
        BigDecimal startingPrice,

        @NotBlank(message = "Currency code is required.")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency code must contain exactly three letters."
        )
        String currencyCode,

        @Size(
                max = 100,
                message = "Price unit label must not exceed 100 characters."
        )
        String priceUnitLabel,

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        @NotNull(message = "Featured status is required.")
        Boolean isFeatured,

        @NotNull(message = "Bookable status is required.")
        Boolean isBookable,

        @NotNull(message = "Public status is required.")
        Boolean isPublic,

        @Size(
                max = 255,
                message = "SEO title must not exceed 255 characters."
        )
        String seoTitle,

        @Size(
                max = 500,
                message = "SEO description must not exceed 500 characters."
        )
        String seoDescription,

        Instant effectiveFrom,

        Instant effectiveUntil,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}