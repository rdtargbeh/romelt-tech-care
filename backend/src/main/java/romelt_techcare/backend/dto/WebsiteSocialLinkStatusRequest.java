package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries an explicit active or inactive status change for one social
 * link.
 * ================================================================
 */
public record WebsiteSocialLinkStatusRequest(

        @NotNull(
                message = "Active status is required."
        )
        Boolean isActive
) {
}