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
 * ROMELT TECHCARE — SERVICE VERSION CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating a new service draft.
 *
 * Version numbering:
 * The backend calculates the next version number. Clients must not
 * submit or calculate service-version numbers.
 *
 * Draft rule:
 * A stable website service may have only one current draft.
 * ================================================================
 */
public record WebsiteServiceVersionCreateRequest(

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

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        Boolean isFeatured,

        Boolean isBookable,

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