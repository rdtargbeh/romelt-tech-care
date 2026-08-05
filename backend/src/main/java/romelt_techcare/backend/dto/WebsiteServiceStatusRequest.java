package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the lifecycle status of one website service.
 * ================================================================
 */
public record WebsiteServiceStatusRequest(

        @NotNull(message = "Service status is required.")
        WebsiteServiceStatus serviceStatus
) {
}