package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteFaqStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing information for a stable
 * website FAQ identity.
 * ================================================================
 */
public record WebsiteFaqResponse(

        UUID faqId,

        String faqKey,

        UUID draftVersionId,

        UUID publishedVersionId,

        WebsiteFaqStatus faqStatus,

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