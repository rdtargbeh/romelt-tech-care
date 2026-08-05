package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing navigation-item information.
 * ================================================================
 */
public record WebsiteNavigationItemResponse(

        UUID navigationItemId,

        UUID websitePageId,

        String websitePageKey,

        String websitePageName,

        String websitePageRoutePath,

        WebsiteNavigationLocation navigationLocation,

        String itemKey,

        String label,

        WebsiteNavigationDestinationType destinationType,

        String destinationUrl,

        String resolvedDestination,

        WebsiteNavigationTargetBehavior targetBehavior,

        String iconKey,

        Integer displayOrder,

        Boolean isVisible,

        Boolean deleted,

        Boolean publiclyAvailable,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID deletedByAdminUserId,

        String deletedByAdminUserDisplayName,

        Instant deletedAt,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}