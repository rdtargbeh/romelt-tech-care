package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries an explicit active or inactive state for a website page.
 * ================================================================
 */
public record WebsitePageStatusRequest(

        @NotNull(message = "Active status is required.")
        Boolean isActive
) {
}