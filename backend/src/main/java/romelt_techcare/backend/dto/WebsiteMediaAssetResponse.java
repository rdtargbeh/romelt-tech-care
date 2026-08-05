package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides a safe API representation of website media metadata.
 *
 * Responsibilities:
 * - Exposes public and administrative media information.
 * - Exposes lifecycle and accessibility state.
 * - Exposes administrator identity snapshots without serializing the
 *   full AdminUser entity.
 * - Prevents lazy-loading and recursive-serialization problems.
 *
 * Security:
 * Passwords, authentication data, session information, and unrelated
 * administrator fields are never included.
 *
 * Storage warning:
 * storageKey is administrative infrastructure information. It should
 * only be returned by protected administrator endpoints and should be
 * omitted from public media responses when a dedicated public response
 * is introduced.
 * ================================================================
 */
public record WebsiteMediaAssetResponse(

        UUID mediaAssetId,

        UUID fileAttachmentId,

        String assetKey,

        String originalFileName,

        String publicUrl,

        String storageKey,

        String mimeType,

        String fileExtension,

        Long fileSizeBytes,

        Integer widthPixels,

        Integer heightPixels,

        String title,

        String altText,

        String caption,

        String description,

        Boolean isDecorative,

        BigDecimal focalPointX,

        BigDecimal focalPointY,

        WebsiteMediaAssetStatus assetStatus,

        Boolean isPublic,

        Boolean publiclyAvailable,

        Boolean archived,

        Boolean deleted,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID archivedByAdminUserId,

        String archivedByAdminUserDisplayName,

        UUID deletedByAdminUserId,

        String deletedByAdminUserDisplayName,

        Instant archivedAt,

        Instant deletedAt,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}