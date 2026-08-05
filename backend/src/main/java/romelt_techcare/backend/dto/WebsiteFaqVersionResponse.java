package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteFaqStatus;
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing FAQ version information.
 * ================================================================
 */
public record WebsiteFaqVersionResponse(

        UUID faqVersionId,

        UUID faqId,

        String faqKey,

        WebsiteFaqStatus faqStatus,

        Integer versionNumber,

        WebsiteFaqVersionStatus versionStatus,

        String faqCategory,

        String question,

        String answer,

        Integer displayOrder,

        Boolean isFeatured,

        Boolean isPublic,

        Boolean publiclyAvailable,

        String changeSummary,

        Instant publishedAt,

        Instant archivedAt,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID publishedByAdminUserId,

        String publishedByAdminUserDisplayName,

        UUID archivedByAdminUserId,

        String archivedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}