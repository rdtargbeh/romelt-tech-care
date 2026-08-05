package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for adding a feature to a draft service
 * version.
 *
 * Version ownership:
 * serviceVersionId is supplied through the endpoint path and is not
 * accepted from the request body.
 * ================================================================
 */
public record WebsiteServiceFeatureCreateRequest(

        @NotBlank(message = "Feature text is required.")
        @Size(
                max = 500,
                message = "Feature text must not exceed 500 characters."
        )
        String featureText,

        @Size(
                max = 100,
                message = "Icon key must not exceed 100 characters."
        )
        String iconKey,

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        Boolean isActive
) {
}