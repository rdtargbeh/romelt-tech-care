package romelt_techcare.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries validated administrator input for registering media metadata
 * in the Romelt TechCare website content-management system.
 *
 * Responsibilities:
 * - Accepts the shared file-attachment identifier when available.
 * - Accepts storage and public-delivery metadata.
 * - Accepts image dimensions and focal-point information.
 * - Accepts accessibility metadata.
 * - Supports initial upload and processing lifecycle states.
 *
 * Security:
 * Administrator ownership, timestamps, archive information, deletion
 * information, and optimistic-lock fields are assigned by the backend
 * and must never be accepted from the client.
 *
 * File handling:
 * This request registers metadata only. The physical upload is handled
 * separately by the shared file-storage module.
 * ================================================================
 */
public record WebsiteMediaAssetCreateRequest(

        UUID fileAttachmentId,

        @Size(
                max = 180,
                message = "Asset key must not exceed 180 characters."
        )
        String assetKey,

        @NotBlank(
                message = "Original file name is required."
        )
        @Size(
                max = 255,
                message = "Original file name must not exceed 255 characters."
        )
        String originalFileName,

        @Size(
                max = 1500,
                message = "Public URL must not exceed 1500 characters."
        )
        String publicUrl,

        @Size(
                max = 1000,
                message = "Storage key must not exceed 1000 characters."
        )
        String storageKey,

        @NotBlank(
                message = "MIME type is required."
        )
        @Size(
                max = 150,
                message = "MIME type must not exceed 150 characters."
        )
        String mimeType,

        @Size(
                max = 30,
                message = "File extension must not exceed 30 characters."
        )
        String fileExtension,

        @PositiveOrZero(
                message = "File size must not be negative."
        )
        Long fileSizeBytes,

        @Positive(
                message = "Image width must be greater than zero."
        )
        Integer widthPixels,

        @Positive(
                message = "Image height must be greater than zero."
        )
        Integer heightPixels,

        @Size(
                max = 255,
                message = "Title must not exceed 255 characters."
        )
        String title,

        @Size(
                max = 500,
                message = "Alternative text must not exceed 500 characters."
        )
        String altText,

        String caption,

        String description,

        Boolean isDecorative,

        @DecimalMin(
                value = "0.00",
                message = "Horizontal focal point must not be less than zero."
        )
        @DecimalMax(
                value = "100.00",
                message = "Horizontal focal point must not exceed 100."
        )
        BigDecimal focalPointX,

        @DecimalMin(
                value = "0.00",
                message = "Vertical focal point must not be less than zero."
        )
        @DecimalMax(
                value = "100.00",
                message = "Vertical focal point must not exceed 100."
        )
        BigDecimal focalPointY,

        WebsiteMediaAssetStatus assetStatus,

        Boolean isPublic
) {
}