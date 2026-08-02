package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteNavigationItem;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for website header, mobile, and footer
 * navigation items.
 *
 * Responsibilities:
 * - Creates and updates navigation items.
 * - Resolves and validates managed website-page destinations.
 * - Validates internal routes, external URLs, anchors, and actions.
 * - Searches administrator-facing records.
 * - Retrieves ordered public navigation.
 * - Updates visibility.
 * - Soft-deletes and restores items.
 * - Enforces location-and-key uniqueness.
 * ================================================================
 */
public interface WebsiteNavigationItemService {

    WebsiteNavigationItem createNavigationItem(
            WebsiteNavigationItem navigationItem,
            UUID administratorId
    );

    WebsiteNavigationItem updateNavigationItem(
            UUID navigationItemId,
            WebsiteNavigationItem requestedUpdate,
            UUID administratorId
    );

    WebsiteNavigationItem getNavigationItem(
            UUID navigationItemId
    );

    WebsiteNavigationItem getNavigationItemByLocationAndKey(
            WebsiteNavigationLocation navigationLocation,
            String itemKey
    );

    Page<WebsiteNavigationItem> searchNavigationItems(
            String keyword,
            WebsiteNavigationLocation navigationLocation,
            WebsiteNavigationDestinationType destinationType,
            Boolean isVisible,
            Pageable pageable
    );

    Page<WebsiteNavigationItem> getDeletedNavigationItems(
            Pageable pageable
    );

    List<WebsiteNavigationItem> getPublicNavigationItems(
            WebsiteNavigationLocation navigationLocation
    );

    List<WebsiteNavigationItem> getAllPublicNavigationItems();

    WebsiteNavigationItem updateNavigationItemVisibility(
            UUID navigationItemId,
            boolean isVisible,
            UUID administratorId
    );

    void deleteNavigationItem(
            UUID navigationItemId,
            UUID administratorId
    );

    WebsiteNavigationItem restoreNavigationItem(
            UUID navigationItemId,
            UUID administratorId
    );

    long countVisibleNavigationItems(
            WebsiteNavigationLocation navigationLocation
    );

    long countDeletedNavigationItems();
}