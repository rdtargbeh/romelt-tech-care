package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsitePageResponse;
import romelt_techcare.backend.dto.WebsitePageCreateRequest;
import romelt_techcare.backend.dto.WebsitePageResponse;
import romelt_techcare.backend.dto.WebsitePageUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsitePageType;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts website-page requests into service-compatible entities and
 * persisted pages into administrator and public responses.
 * ================================================================
 */
@Component
public class WebsitePageMapper {

    public WebsitePage toEntity(
            WebsitePageCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePage.builder()
                .pageKey(request.pageKey())
                .pageName(request.pageName())
                .routePath(request.routePath())
                .pageType(
                        request.pageType() == null
                                ? WebsitePageType.STANDARD
                                : request.pageType()
                )
                .draftVersionId(request.draftVersionId())
                .publishedVersionId(request.publishedVersionId())
                .contentSchemaVersion(
                        request.contentSchemaVersion() == null
                                ? 1
                                : request.contentSchemaVersion()
                )
                .isSystemPage(
                        request.isSystemPage() == null
                                || request.isSystemPage()
                )
                .isActive(
                        request.isActive() == null
                                || request.isActive()
                )
                .build();
    }

    public WebsitePage toUpdateEntity(
            WebsitePageUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePage.builder()
                .pageKey(request.pageKey())
                .pageName(request.pageName())
                .routePath(request.routePath())
                .pageType(request.pageType())
                .contentSchemaVersion(
                        request.contentSchemaVersion()
                )
                .isSystemPage(request.isSystemPage())
                .isActive(request.isActive())
                .build();
    }

    public WebsitePageResponse toResponse(
            WebsitePage page
    ) {
        if (page == null) {
            return null;
        }

        AdminUser createdBy = page.getCreatedByAdminUser();
        AdminUser updatedBy = page.getUpdatedByAdminUser();
        AdminUser deletedBy = page.getDeletedByAdminUser();

        return new WebsitePageResponse(
                page.getWebsitePageId(),
                page.getPageKey(),
                page.getPageName(),
                page.getRoutePath(),
                page.getPageType(),
                page.getDraftVersionId(),
                page.getPublishedVersionId(),
                page.getContentSchemaVersion(),
                page.getIsSystemPage(),
                page.getIsActive(),
                page.isDeleted(),
                page.isPubliclyAvailable(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(deletedBy),
                getAdminUserDisplayName(deletedBy),
                page.getDeletedAt(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getRowVersion()
        );
    }

    public PublicWebsitePageResponse toPublicResponse(
            WebsitePage page
    ) {
        if (page == null) {
            return null;
        }

        return new PublicWebsitePageResponse(
                page.getWebsitePageId(),
                page.getPageKey(),
                page.getPageName(),
                page.getRoutePath(),
                page.getPageType(),
                page.getPublishedVersionId(),
                page.getContentSchemaVersion()
        );
    }

    private UUID getAdminUserId(
            AdminUser adminUser
    ) {
        return adminUser == null
                ? null
                : adminUser.getAdminUserId();
    }

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