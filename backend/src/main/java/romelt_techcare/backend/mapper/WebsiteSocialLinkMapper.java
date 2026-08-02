package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteSocialLinkResponse;
import romelt_techcare.backend.dto.WebsiteSocialLinkCreateRequest;
import romelt_techcare.backend.dto.WebsiteSocialLinkResponse;
import romelt_techcare.backend.dto.WebsiteSocialLinkUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteSocialLink;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts social-link request DTOs into service-compatible entities
 * and persisted entities into administrator and public responses.
 *
 * Responsibilities:
 * - Excludes protected persistence fields from request mapping.
 * - Prevents direct JPA entity serialization.
 * - Produces safe administrator attribution snapshots.
 * - Produces minimal public responses.
 * ================================================================
 */
@Component
public class WebsiteSocialLinkMapper {

    /**
     * Converts a create request into an unsaved entity.
     */
    public WebsiteSocialLink toEntity(
            WebsiteSocialLinkCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteSocialLink.builder()
                .platform(request.platform())
                .label(request.label())
                .profileUrl(request.profileUrl())
                .iconKey(request.iconKey())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isActive(
                        request.isActive() == null
                                || request.isActive()
                )
                .build();
    }

    /**
     * Converts an update request into a detached update entity.
     */
    public WebsiteSocialLink toUpdateEntity(
            WebsiteSocialLinkUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteSocialLink.builder()
                .platform(request.platform())
                .label(request.label())
                .profileUrl(request.profileUrl())
                .iconKey(request.iconKey())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();
    }

    /**
     * Converts an entity into a protected administrator response.
     */
    public WebsiteSocialLinkResponse toResponse(
            WebsiteSocialLink socialLink
    ) {
        if (socialLink == null) {
            return null;
        }

        AdminUser createdBy =
                socialLink.getCreatedByAdminUser();

        AdminUser updatedBy =
                socialLink.getUpdatedByAdminUser();

        return new WebsiteSocialLinkResponse(
                socialLink.getSocialLinkId(),
                socialLink.getPlatform(),
                socialLink.getLabel(),
                socialLink.getProfileUrl(),
                socialLink.getIconKey(),
                socialLink.getDisplayOrder(),
                socialLink.getIsActive(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                socialLink.getCreatedAt(),
                socialLink.getUpdatedAt(),
                socialLink.getRowVersion()
        );
    }

    /**
     * Converts an active entity into a public response.
     */
    public PublicWebsiteSocialLinkResponse toPublicResponse(
            WebsiteSocialLink socialLink
    ) {
        if (socialLink == null) {
            return null;
        }

        return new PublicWebsiteSocialLinkResponse(
                socialLink.getPlatform(),
                socialLink.getLabel(),
                socialLink.getProfileUrl(),
                socialLink.getIconKey(),
                socialLink.getDisplayOrder()
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