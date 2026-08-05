package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteServiceFeature;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for features belonging to versioned
 * website services.
 *
 * Responsibilities:
 * - Creates features for draft service versions.
 * - Updates draft-owned features.
 * - Retrieves features by identifier and service version.
 * - Searches and orders administrator-facing features.
 * - Activates and deactivates draft-owned features.
 * - Updates feature display order.
 * - Permanently deletes draft-owned features.
 * - Retrieves active features for public published services.
 *
 * Immutability:
 * Features belonging to PUBLISHED or ARCHIVED versions cannot be
 * changed or deleted.
 * ================================================================
 */
public interface WebsiteServiceFeatureService {

    WebsiteServiceFeature createServiceFeature(
            UUID serviceVersionId,
            WebsiteServiceFeature requestedFeature,
            UUID administratorId
    );

    WebsiteServiceFeature updateServiceFeature(
            UUID serviceFeatureId,
            WebsiteServiceFeature requestedUpdate,
            UUID administratorId
    );

    WebsiteServiceFeature getServiceFeature(
            UUID serviceFeatureId
    );

    List<WebsiteServiceFeature> getServiceFeatures(
            UUID serviceVersionId
    );

    Page<WebsiteServiceFeature> searchServiceFeatures(
            UUID serviceVersionId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    );

    WebsiteServiceFeature updateServiceFeatureStatus(
            UUID serviceFeatureId,
            boolean isActive,
            UUID administratorId
    );

    WebsiteServiceFeature updateServiceFeatureOrder(
            UUID serviceFeatureId,
            Integer displayOrder,
            UUID administratorId
    );

    void deleteServiceFeature(
            UUID serviceFeatureId,
            UUID administratorId
    );

    List<WebsiteServiceFeature> getPublicFeaturesByServiceCode(
            String serviceCode
    );

    List<WebsiteServiceFeature> getPublicFeaturesByServiceSlug(
            String serviceSlug
    );

    long countServiceFeatures(
            UUID serviceVersionId
    );

    long countActiveServiceFeatures(
            UUID serviceVersionId
    );
}