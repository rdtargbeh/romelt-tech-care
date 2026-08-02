package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing information for a stable
 * website service.
 * ================================================================
 */
public record WebsiteServiceResponse(

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        UUID draftVersionId,

        UUID publishedVersionId,

        WebsiteServiceStatus serviceStatus,

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