package romelt_techcare.backend.mapper;


import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.WebsiteMediaAssetCreateRequest;
import romelt_techcare.backend.dto.WebsiteMediaAssetResponse;
import romelt_techcare.backend.dto.WebsiteMediaAssetUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts between website media request DTOs, response DTOs, and the
 * WebsiteMediaAsset persistence entity.
 *
 * Responsibilities:
 * - Converts create requests into new entity instances.
 * - Converts update requests into service-compatible entity instances.
 * - Converts persisted media entities into safe API responses.
 * - Extracts limited administrator identity information.
 * - Avoids exposing full entity graphs through controllers.
 *
 * Important:
 * Lifecycle transitions, ownership assignment, validation, uniqueness,
 * timestamps, and persistence remain responsibilities of the service
 * layer and entity lifecycle callbacks.
 * ================================================================
 */
@Component
public class WebsiteMediaAssetMapper {

    /**
     * Converts a create request into a new media entity.
     *
     * @param request validated create request
     * @return unsaved media entity
     */
    public WebsiteMediaAsset toEntity(
            WebsiteMediaAssetCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteMediaAsset.builder()
                .fileAttachmentId(request.fileAttachmentId())
                .assetKey(request.assetKey())
                .originalFileName(request.originalFileName())
                .publicUrl(request.publicUrl())
                .storageKey(request.storageKey())
                .mimeType(request.mimeType())
                .fileExtension(request.fileExtension())
                .fileSizeBytes(request.fileSizeBytes())
                .widthPixels(request.widthPixels())
                .heightPixels(request.heightPixels())
                .title(request.title())
                .altText(request.altText())
                .caption(request.caption())
                .description(request.description())
                .isDecorative(
                        request.isDecorative() != null
                                ? request.isDecorative()
                                : false
                )
                .focalPointX(request.focalPointX())
                .focalPointY(request.focalPointY())
                .assetStatus(
                        request.assetStatus() != null
                                ? request.assetStatus()
                                : WebsiteMediaAssetStatus.ACTIVE
                )
                .isPublic(
                        request.isPublic() != null
                                ? request.isPublic()
                                : true
                )
                .build();
    }

    /**
     * Converts an update request into a detached entity carrying only
     * administrator-editable media metadata.
     *
     * The service implementation copies these fields onto the locked
     * persistent entity and preserves protected lifecycle information.
     *
     * @param request validated update request
     * @return detached update entity
     */
    public WebsiteMediaAsset toUpdateEntity(
            WebsiteMediaAssetUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteMediaAsset.builder()
                .fileAttachmentId(request.fileAttachmentId())
                .assetKey(request.assetKey())
                .originalFileName(request.originalFileName())
                .publicUrl(request.publicUrl())
                .storageKey(request.storageKey())
                .mimeType(request.mimeType())
                .fileExtension(request.fileExtension())
                .fileSizeBytes(request.fileSizeBytes())
                .widthPixels(request.widthPixels())
                .heightPixels(request.heightPixels())
                .title(request.title())
                .altText(request.altText())
                .caption(request.caption())
                .description(request.description())
                .isDecorative(
                        request.isDecorative() != null
                                ? request.isDecorative()
                                : false
                )
                .focalPointX(request.focalPointX())
                .focalPointY(request.focalPointY())
                .isPublic(request.isPublic())
                .build();
    }

    /**
     * Converts a persisted media entity into an API response.
     *
     * @param mediaAsset persisted media asset
     * @return safe media response
     */
    public WebsiteMediaAssetResponse toResponse(
            WebsiteMediaAsset mediaAsset
    ) {
        if (mediaAsset == null) {
            return null;
        }

        AdminUser createdBy =
                mediaAsset.getCreatedByAdminUser();

        AdminUser updatedBy =
                mediaAsset.getUpdatedByAdminUser();

        AdminUser archivedBy =
                mediaAsset.getArchivedByAdminUser();

        AdminUser deletedBy =
                mediaAsset.getDeletedByAdminUser();

        return new WebsiteMediaAssetResponse(
                mediaAsset.getMediaAssetId(),
                mediaAsset.getFileAttachmentId(),
                mediaAsset.getAssetKey(),
                mediaAsset.getOriginalFileName(),
                mediaAsset.getPublicUrl(),
                mediaAsset.getStorageKey(),
                mediaAsset.getMimeType(),
                mediaAsset.getFileExtension(),
                mediaAsset.getFileSizeBytes(),
                mediaAsset.getWidthPixels(),
                mediaAsset.getHeightPixels(),
                mediaAsset.getTitle(),
                mediaAsset.getAltText(),
                mediaAsset.getCaption(),
                mediaAsset.getDescription(),
                mediaAsset.getIsDecorative(),
                mediaAsset.getFocalPointX(),
                mediaAsset.getFocalPointY(),
                mediaAsset.getAssetStatus(),
                mediaAsset.getIsPublic(),
                mediaAsset.isPubliclyAvailable(),
                mediaAsset.isArchived(),
                mediaAsset.isDeleted(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(archivedBy),
                getAdminUserDisplayName(archivedBy),
                getAdminUserId(deletedBy),
                getAdminUserDisplayName(deletedBy),
                mediaAsset.getArchivedAt(),
                mediaAsset.getDeletedAt(),
                mediaAsset.getCreatedAt(),
                mediaAsset.getUpdatedAt(),
                mediaAsset.getRowVersion()
        );
    }

    /**
     * Extracts the administrator identifier without exposing the entire
     * administrator entity.
     */
    private UUID getAdminUserId(
            AdminUser adminUser
    ) {
        return adminUser == null
                ? null
                : adminUser.getAdminUserId();
    }

    /**
     * Produces a display-safe administrator name.
     *
     * The method uses first and last name when available and falls back
     * to the administrator email address.
     */
    private String getAdminUserDisplayName(
            AdminUser adminUser
    ) {
        if (adminUser == null) {
            return null;
        }

        String firstName =
                normalizeOptional(adminUser.getFirstName());

        String lastName =
                normalizeOptional(adminUser.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(adminUser.getEmail());
    }

    /**
     * Trims optional values and converts blanks to null.
     */
    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}