package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION VISIBILITY REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates whether one navigation item is shown publicly.
 * ================================================================
 */
public record WebsiteNavigationItemVisibilityRequest(

        @NotNull(message = "Visibility status is required.")
        Boolean isVisible
) {
}