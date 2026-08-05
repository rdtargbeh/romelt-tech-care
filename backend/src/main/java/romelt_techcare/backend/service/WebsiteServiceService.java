package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for stable website-service identities.
 *
 * Responsibilities:
 * - Creates and updates services.
 * - Retrieves services by ID, code, and slug.
 * - Searches services using administrator filters.
 * - Manages lifecycle status.
 * - Temporarily manages draft and published version pointers.
 * - Soft-deletes and restores services.
 * - Retrieves active public service identities.
 * ================================================================
 */
public interface WebsiteServiceService {

    WebsiteService createWebsiteService(WebsiteService websiteService, UUID administratorId);

    WebsiteService updateWebsiteService(UUID serviceId, WebsiteService requestedUpdate, UUID administratorId);

    WebsiteService getWebsiteService(UUID serviceId);

    WebsiteService getWebsiteServiceByCode(String serviceCode);

    WebsiteService getWebsiteServiceBySlug(String serviceSlug);

    Page<WebsiteService> searchWebsiteServices(String keyword, WebsiteServiceStatus serviceStatus, Pageable pageable);

    Page<WebsiteService> getDeletedWebsiteServices(Pageable pageable);

    WebsiteService updateWebsiteServiceStatus(UUID serviceId, WebsiteServiceStatus serviceStatus, UUID administratorId);

    WebsiteService activateWebsiteService(UUID serviceId, UUID administratorId);

    WebsiteService deactivateWebsiteService(UUID serviceId, UUID administratorId);

    WebsiteService archiveWebsiteService(UUID serviceId, UUID administratorId);

    void deleteWebsiteService(UUID serviceId, UUID administratorId);

    WebsiteService restoreWebsiteService(UUID serviceId, UUID administratorId);

    WebsiteService getPublicWebsiteServiceByCode(String serviceCode);

    WebsiteService getPublicWebsiteServiceBySlug(String serviceSlug);

    List<WebsiteService> getPublicWebsiteServices();

    long countWebsiteServicesByStatus(WebsiteServiceStatus serviceStatus);

    long countDeletedWebsiteServices();
}