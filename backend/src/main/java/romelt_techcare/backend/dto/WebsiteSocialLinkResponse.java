package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing website social-link data.
 *
 * Responsibilities:
 * - Returns link identity and public display fields.
 * - Returns active status and ordering.
 * - Returns administrator attribution.
 * - Returns timestamps and optimistic-lock information.
 * ================================================================
 */
public record WebsiteSocialLinkResponse(

        UUID socialLinkId,

        String platform,

        String label,

        String profileUrl,

        String iconKey,

        Integer displayOrder,

        Boolean isActive,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}