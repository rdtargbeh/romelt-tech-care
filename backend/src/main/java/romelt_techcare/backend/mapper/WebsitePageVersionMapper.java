package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteMediaAssetResponse;
import romelt_techcare.backend.dto.PublicWebsitePageVersionResponse;
import romelt_techcare.backend.dto.WebsitePageVersionCreateRequest;
import romelt_techcare.backend.dto.WebsitePageVersionResponse;
import romelt_techcare.backend.dto.WebsitePageVersionUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.entity.WebsitePageVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts page-version request DTOs into service-compatible entities
 * and persisted versions into administrator and public responses.
 *
 * Responsibilities:
 * - Excludes lifecycle and audit fields from request mapping.
 * - Creates lightweight media references from media identifiers.
 * - Prevents direct JPA relationship serialization.
 * - Produces safe public published-page responses.
 * ================================================================
 */
@Component
public class WebsitePageVersionMapper {

    public WebsitePageVersion toEntity(
            WebsitePageVersionCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePageVersion.builder()
                .contentSchemaVersion(
                        request.contentSchemaVersion()
                )
                .contentJson(request.contentJson())
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .socialTitle(request.socialTitle())
                .socialDescription(
                        request.socialDescription()
                )
                .socialImageMedia(
                        mediaReference(
                                request.socialImageMediaId()
                        )
                )
                .canonicalUrl(request.canonicalUrl())
                .robotsIndex(
                        request.robotsIndex() == null
                                || request.robotsIndex()
                )
                .robotsFollow(
                        request.robotsFollow() == null
                                || request.robotsFollow()
                )
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsitePageVersion toUpdateEntity(
            WebsitePageVersionUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePageVersion.builder()
                .contentSchemaVersion(
                        request.contentSchemaVersion()
                )
                .contentJson(request.contentJson())
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .socialTitle(request.socialTitle())
                .socialDescription(
                        request.socialDescription()
                )
                .socialImageMedia(
                        mediaReference(
                                request.socialImageMediaId()
                        )
                )
                .canonicalUrl(request.canonicalUrl())
                .robotsIndex(request.robotsIndex())
                .robotsFollow(request.robotsFollow())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsitePageVersionResponse toResponse(
            WebsitePageVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsitePage page = version.getWebsitePage();

        AdminUser createdBy =
                version.getCreatedByAdminUser();

        AdminUser updatedBy =
                version.getUpdatedByAdminUser();

        AdminUser publishedBy =
                version.getPublishedByAdminUser();

        AdminUser archivedBy =
                version.getArchivedByAdminUser();

        return new WebsitePageVersionResponse(
                version.getPageVersionId(),
                page == null
                        ? null
                        : page.getWebsitePageId(),
                page == null ? null : page.getPageKey(),
                page == null ? null : page.getPageName(),
                page == null ? null : page.getRoutePath(),
                version.getVersionNumber(),
                version.getVersionStatus(),
                version.getContentSchemaVersion(),
                version.getContentJson(),
                version.getSeoTitle(),
                version.getSeoDescription(),
                version.getSocialTitle(),
                version.getSocialDescription(),
                publicMedia(version.getSocialImageMedia()),
                version.getCanonicalUrl(),
                version.getRobotsIndex(),
                version.getRobotsFollow(),
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

    public PublicWebsitePageVersionResponse toPublicResponse(
            WebsitePageVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsitePage page = version.getWebsitePage();

        return new PublicWebsitePageVersionResponse(
                page == null
                        ? null
                        : page.getWebsitePageId(),
                page == null ? null : page.getPageKey(),
                page == null ? null : page.getPageName(),
                page == null ? null : page.getRoutePath(),
                page == null ? null : page.getPageType(),
                version.getPageVersionId(),
                version.getVersionNumber(),
                version.getContentSchemaVersion(),
                version.getContentJson(),
                version.getSeoTitle(),
                version.getSeoDescription(),
                version.getSocialTitle(),
                version.getSocialDescription(),
                publicMedia(version.getSocialImageMedia()),
                version.getCanonicalUrl(),
                version.getRobotsIndex(),
                version.getRobotsFollow(),
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