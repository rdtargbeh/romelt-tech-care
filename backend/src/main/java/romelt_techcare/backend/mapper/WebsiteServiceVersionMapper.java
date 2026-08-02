package romelt_techcare.backend.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteMediaAssetResponse;
import romelt_techcare.backend.dto.PublicWebsiteServiceVersionResponse;
import romelt_techcare.backend.dto.WebsiteServiceVersionCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceVersionResponse;
import romelt_techcare.backend.dto.WebsiteServiceVersionUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.entity.WebsiteServiceVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE VERSION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts service-version requests into service-compatible entities
 * and persisted versions into administrator and public responses.
 *
 * Responsibilities:
 * - Excludes lifecycle and audit fields from request mappings.
 * - Creates lightweight media references from media identifiers.
 * - Prevents direct JPA relationship serialization.
 * - Produces administrator-facing version responses.
 * - Produces safe published public-service responses.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class WebsiteServiceVersionMapper {

    private final WebsiteMediaAssetMapper websiteMediaAssetMapper;

    public WebsiteServiceVersion toEntity(
            WebsiteServiceVersionCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteServiceVersion.builder()
                .serviceName(request.serviceName())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .iconKey(request.iconKey())
                .cardImageMedia(
                        mediaReference(
                                request.cardImageMediaId()
                        )
                )
                .heroImageMedia(
                        mediaReference(
                                request.heroImageMediaId()
                        )
                )
                .startingPrice(request.startingPrice())
                .currencyCode(
                        request.currencyCode() == null
                                || request.currencyCode().isBlank()
                                ? "USD"
                                : request.currencyCode()
                )
                .priceUnitLabel(request.priceUnitLabel())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isFeatured(
                        Boolean.TRUE.equals(
                                request.isFeatured()
                        )
                )
                .isBookable(
                        request.isBookable() == null
                                || request.isBookable()
                )
                .isPublic(
                        request.isPublic() == null
                                || request.isPublic()
                )
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsiteServiceVersion toUpdateEntity(
            WebsiteServiceVersionUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteServiceVersion.builder()
                .serviceName(request.serviceName())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .iconKey(request.iconKey())
                .cardImageMedia(
                        mediaReference(
                                request.cardImageMediaId()
                        )
                )
                .heroImageMedia(
                        mediaReference(
                                request.heroImageMediaId()
                        )
                )
                .startingPrice(request.startingPrice())
                .currencyCode(request.currencyCode())
                .priceUnitLabel(request.priceUnitLabel())
                .displayOrder(request.displayOrder())
                .isFeatured(request.isFeatured())
                .isBookable(request.isBookable())
                .isPublic(request.isPublic())
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsiteServiceVersionResponse toResponse(
            WebsiteServiceVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsiteService service =
                version.getWebsiteService();

        AdminUser createdBy =
                version.getCreatedByAdminUser();

        AdminUser updatedBy =
                version.getUpdatedByAdminUser();

        AdminUser publishedBy =
                version.getPublishedByAdminUser();

        AdminUser archivedBy =
                version.getArchivedByAdminUser();

        return new WebsiteServiceVersionResponse(
                version.getServiceVersionId(),
                service == null
                        ? null
                        : service.getServiceId(),
                service == null
                        ? null
                        : service.getServiceCode(),
                service == null
                        ? null
                        : service.getServiceSlug(),
                service == null
                        ? null
                        : service.getServiceStatus(),
                version.getVersionNumber(),
                version.getVersionStatus(),
                version.getServiceName(),
                version.getShortDescription(),
                version.getFullDescription(),
                version.getIconKey(),
                publicMedia(version.getCardImageMedia()),
                publicMedia(version.getHeroImageMedia()),
                version.getStartingPrice(),
                version.getCurrencyCode(),
                version.getPriceUnitLabel(),
                version.getDisplayOrder(),
                version.getIsFeatured(),
                version.getIsBookable(),
                version.getIsPublic(),
                version.getSeoTitle(),
                version.getSeoDescription(),
                version.getEffectiveFrom(),
                version.getEffectiveUntil(),
                version.isCurrentlyEffective(),
                version.isPubliclyAvailable(),
                version.isPubliclyBookable(),
                version.getChangeSummary(),
                version.getPublishedAt(),
                version.getArchivedAt(),
                adminId(createdBy),
                adminDisplayName(createdBy),
                adminId(updatedBy),
                adminDisplayName(updatedBy),
                adminId(publishedBy),
                adminDisplayName(publishedBy),
                adminId(archivedBy),
                adminDisplayName(archivedBy),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getRowVersion()
        );
    }

    public PublicWebsiteServiceVersionResponse toPublicResponse(
            WebsiteServiceVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsiteService service =
                version.getWebsiteService();

        return new PublicWebsiteServiceVersionResponse(
                service == null
                        ? null
                        : service.getServiceId(),
                service == null
                        ? null
                        : service.getServiceCode(),
                service == null
                        ? null
                        : service.getServiceSlug(),
                version.getServiceVersionId(),
                version.getVersionNumber(),
                version.getServiceName(),
                version.getShortDescription(),
                version.getFullDescription(),
                version.getIconKey(),
                publicMedia(version.getCardImageMedia()),
                publicMedia(version.getHeroImageMedia()),
                version.getStartingPrice(),
                version.getCurrencyCode(),
                version.getPriceUnitLabel(),
                version.getDisplayOrder(),
                version.getIsFeatured(),
                version.getIsBookable(),
                version.getSeoTitle(),
                version.getSeoDescription(),
                version.getEffectiveFrom(),
                version.getEffectiveUntil(),
                version.getPublishedAt()
        );
    }

    private WebsiteMediaAsset mediaReference(
            UUID mediaAssetId
    ) {
        if (mediaAssetId == null) {
            return null;
        }

        return WebsiteMediaAsset.builder()
                .mediaAssetId(mediaAssetId)
                .build();
    }

    private PublicWebsiteMediaAssetResponse publicMedia(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : PublicWebsiteMediaAssetResponse.from(
                mediaAsset
        );
    }

    private UUID adminId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String adminDisplayName(
            AdminUser administrator
    ) {
        if (administrator == null) {
            return null;
        }

        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(
                administrator.getEmail()
        );
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}