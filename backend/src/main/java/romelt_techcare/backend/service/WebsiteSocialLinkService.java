package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteSocialLink;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for managing social and external-profile
 * links displayed on the Romelt TechCare website.
 *
 * Responsibilities:
 * - Creates and updates social links.
 * - Retrieves individual and paginated administrator records.
 * - Retrieves active public links in display order.
 * - Enforces one record per normalized platform.
 * - Activates and deactivates links.
 * - Deletes obsolete links.
 * - Attributes write operations to an administrator.
 * ================================================================
 */
public interface WebsiteSocialLinkService {

    /**
     * Creates a social link.
     */
    WebsiteSocialLink createSocialLink(
            WebsiteSocialLink socialLink,
            UUID administratorId
    );

    /**
     * Updates a social link.
     */
    WebsiteSocialLink updateSocialLink(
            UUID socialLinkId,
            WebsiteSocialLink requestedUpdate,
            UUID administratorId
    );

    /**
     * Retrieves one social link by identifier.
     */
    WebsiteSocialLink getSocialLink(
            UUID socialLinkId
    );

    /**
     * Retrieves one social link by platform.
     */
    WebsiteSocialLink getSocialLinkByPlatform(
            String platform
    );

    /**
     * Searches administrator-facing social links.
     */
    Page<WebsiteSocialLink> searchSocialLinks(
            String keyword,
            Boolean isActive,
            Pageable pageable
    );

    /**
     * Retrieves active public links in display order.
     */
    List<WebsiteSocialLink> getActivePublicSocialLinks();

    /**
     * Updates active status.
     */
    WebsiteSocialLink updateSocialLinkStatus(
            UUID socialLinkId,
            boolean isActive,
            UUID administratorId
    );

    /**
     * Activates a link.
     */
    WebsiteSocialLink activateSocialLink(
            UUID socialLinkId,
            UUID administratorId
    );

    /**
     * Deactivates a link.
     */
    WebsiteSocialLink deactivateSocialLink(
            UUID socialLinkId,
            UUID administratorId
    );

    /**
     * Permanently removes a social-link record.
     */
    void deleteSocialLink(
            UUID socialLinkId,
            UUID administratorId
    );

    /**
     * Counts active social links.
     */
    long countActiveSocialLinks();
}