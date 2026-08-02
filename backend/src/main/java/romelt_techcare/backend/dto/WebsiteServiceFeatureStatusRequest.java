package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the active state of one service feature.
 * ================================================================
 */
public record WebsiteServiceFeatureStatusRequest(

        @NotNull(message = "Active status is required.")
        Boolean isActive
) {
}