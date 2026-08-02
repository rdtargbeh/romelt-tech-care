package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteNavigationItemResponse;
import romelt_techcare.backend.dto.WebsiteNavigationItemCreateRequest;
import romelt_techcare.backend.dto.WebsiteNavigationItemResponse;
import romelt_techcare.backend.dto.WebsiteNavigationItemUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteNavigationItem;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts navigation request DTOs into service-compatible entities
 * and persisted entities into administrator and public responses.
 * ================================================================
 */
@Component
public class WebsiteNavigationItemMapper {

    public WebsiteNavigationItem toEntity(
            WebsiteNavigationItemCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteNavigationItem.builder()
                .websitePage(
                        pageReference(request.websitePageId())
                )
                .navigationLocation(
                        request.navigationLocation()
                )
                .itemKey(request.itemKey())
                .label(request.label())
                .destinationType(
                        request.destinationType() == null
                                ? WebsiteNavigationDestinationType
                                .INTERNAL_ROUTE
                                : request.destinationType()
                )
                .destinationUrl(request.destinationUrl())
                .targetBehavior(
                        request.targetBehavior() == null
                                ? WebsiteNavigationTargetBehavior
                                .SAME_WINDOW
                                : request.targetBehavior()
                )
                .iconKey(request.iconKey())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isVisible(
                        request.isVisible() == null
                                || request.isVisible()
                )
                .build();
    }

    public WebsiteNavigationItem toUpdateEntity(
            WebsiteNavigationItemUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteNavigationItem.builder()
                .websitePage(
                        pageReference(request.websitePageId())
                )
                .navigationLocation(
                        request.navigationLocation()
                )
                .itemKey(request.itemKey())
                .label(request.label())
                .destinationType(
                        request.destinationType()
                )
                .destinationUrl(request.destinationUrl())
                .targetBehavior(
                        request.targetBehavior()
                )
                .iconKey(request.iconKey())
                .displayOrder(request.displayOrder())
                .isVisible(request.isVisible())
                .build();
    }

    public WebsiteNavigationItemResponse toResponse(
            WebsiteNavigationItem item
    ) {
        if (item == null) {
            return null;
        }

        WebsitePage page = item.getWebsitePage();

        AdminUser createdBy =
                item.getCreatedByAdminUser();

        AdminUser updatedBy =
                item.getUpdatedByAdminUser();

        AdminUser deletedBy =
                item.getDeletedByAdminUser();

        return new WebsiteNavigationItemResponse(
                item.getNavigationItemId(),
                page == null
                        ? null
                        : page.getWebsitePageId(),
                page == null
                        ? null
                        : page.getPageKey(),
                page == null
                        ? null
                        : page.getPageName(),
                page == null
                        ? null
                        : page.getRoutePath(),
                item.getNavigationLocation(),
                item.getItemKey(),
                item.getLabel(),
                item.getDestinationType(),
                item.getDestinationUrl(),
                item.resolveDestination(),
                item.getTargetBehavior(),
                item.getIconKey(),
                item.getDisplayOrder(),
                item.getIsVisible(),
                item.isDeleted(),
                item.isPubliclyAvailable(),
                adminId(createdBy),
                adminDisplayName(createdBy),
                adminId(updatedBy),
                adminDisplayName(updatedBy),
                adminId(deletedBy),
                adminDisplayName(deletedBy),
                item.getDeletedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getRowVersion()
        );
    }

    public PublicWebsiteNavigationItemResponse toPublicResponse(
            WebsiteNavigationItem item
    ) {
        if (item == null) {
            return null;
        }

        return new PublicWebsiteNavigationItemResponse(
                item.getNavigationLocation(),
                item.getItemKey(),
                item.getLabel(),
                item.getDestinationType(),
                item.resolveDestination(),
                item.getTargetBehavior(),
                item.getIconKey(),
                item.getDisplayOrder()
        );
    }

    private WebsitePage pageReference(
            UUID websitePageId
    ) {
        if (websitePageId == null) {
            return null;
        }

        return WebsitePage.builder()
                .websitePageId(websitePageId)
                .build();
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