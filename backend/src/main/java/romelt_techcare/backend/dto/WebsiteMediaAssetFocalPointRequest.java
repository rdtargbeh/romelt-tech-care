package romelt_techcare.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * ================================================================
 * ROMELT TECHCARE — MEDIA ASSET FOCAL POINT REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries horizontal and vertical image focal-point percentages.
 *
 * Usage:
 * Focal points help React position important image content when images
 * are cropped by responsive containers or object-fit behavior.
 *
 * Range:
 * - 0 represents the left or top edge.
 * - 50 represents the center.
 * - 100 represents the right or bottom edge.
 * ================================================================
 */
public record WebsiteMediaAssetFocalPointRequest(

        @DecimalMin(
                value = "0.00",
                message = "Horizontal focal point must not be less than zero."
        )
        @DecimalMax(
                value = "100.00",
                message = "Horizontal focal point must not exceed 100."
        )
        BigDecimal focalPointX,

        @DecimalMin(
                value = "0.00",
                message = "Vertical focal point must not be less than zero."
        )
        @DecimalMax(
                value = "100.00",
                message = "Vertical focal point must not exceed 100."
        )
        BigDecimal focalPointY
) {
}