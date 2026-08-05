package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.WebsiteMediaAsset;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE MEDIA ASSET RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides a safe public representation of an active website media
 * asset.
 *
 * Responsibilities:
 * - Exposes only information required by the public website.
 * - Provides the public media URL and display metadata.
 * - Provides image dimensions and focal-point positioning.
 * - Provides accessible alternative text.
 * - Excludes storage keys, file attachment identifiers, administrator
 *   information, deletion data, and optimistic-lock information.
 *
 * Security:
 * This response must only be created from an ACTIVE, public,
 * non-deleted WebsiteMediaAsset.
 * ================================================================
 */
public record PublicWebsiteMediaAssetResponse(

        UUID mediaAssetId,

        String assetKey,

        String publicUrl,

        String mimeType,

        String fileExtension,

        Integer widthPixels,

        Integer heightPixels,

        String title,

        String altText,

        String caption,

        Boolean isDecorative,

        BigDecimal focalPointX,

        BigDecimal focalPointY
) {

    /**
     * Converts a website media entity into a safe public response.
     *
     * Decorative images return an empty alternative-text value so the
     * frontend can render alt="" correctly.
     *
     * @param mediaAsset active public media entity
     * @return safe public media response
     */
    public static PublicWebsiteMediaAssetResponse from(
            WebsiteMediaAsset mediaAsset
    ) {
        if (mediaAsset == null) {
            return null;
        }

        String publicAltText =
                Boolean.TRUE.equals(mediaAsset.getIsDecorative())
                        ? ""
                        : mediaAsset.getAltText();

        return new PublicWebsiteMediaAssetResponse(
                mediaAsset.getMediaAssetId(),
                mediaAsset.getAssetKey(),
                mediaAsset.getPublicUrl(),
                mediaAsset.getMimeType(),
                mediaAsset.getFileExtension(),
                mediaAsset.getWidthPixels(),
                mediaAsset.getHeightPixels(),
                mediaAsset.getTitle(),
                publicAltText,
                mediaAsset.getCaption(),
                mediaAsset.getIsDecorative(),
                mediaAsset.getFocalPointX(),
                mediaAsset.getFocalPointY()
        );
    }
}