package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC NAVIGATION ITEM RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the React website with one visible navigation item.
 *
 * Security:
 * Internal identifiers, administrator attribution, deletion metadata,
 * timestamps, and optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsiteNavigationItemResponse(

        WebsiteNavigationLocation navigationLocation,

        String itemKey,

        String label,

        WebsiteNavigationDestinationType destinationType,

        String destination,

        WebsiteNavigationTargetBehavior targetBehavior,

        String iconKey,

        Integer displayOrder
) {
}