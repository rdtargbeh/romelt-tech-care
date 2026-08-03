package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsitePricingPlanFeature;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for features belonging to versioned
 * website pricing plans.
 *
 * Responsibilities:
 * - Creates features for draft pricing-plan versions.
 * - Updates draft-owned features.
 * - Retrieves and searches pricing-plan features.
 * - Activates and deactivates draft-owned features.
 * - Updates display order.
 * - Permanently deletes draft-owned features.
 * - Retrieves active public features by plan code and slug.
 *
 * Immutability:
 * Features belonging to PUBLISHED or ARCHIVED pricing-plan versions
 * cannot be changed or deleted.
 * ================================================================
 */
public interface WebsitePricingPlanFeatureService {

    WebsitePricingPlanFeature createPricingPlanFeature(
            UUID pricingPlanVersionId,
            WebsitePricingPlanFeature requestedFeature,
            UUID administratorId
    );

    WebsitePricingPlanFeature updatePricingPlanFeature(
            UUID pricingPlanFeatureId,
            WebsitePricingPlanFeature requestedUpdate,
            UUID administratorId
    );

    WebsitePricingPlanFeature getPricingPlanFeature(
            UUID pricingPlanFeatureId
    );

    List<WebsitePricingPlanFeature> getPricingPlanFeatures(
            UUID pricingPlanVersionId
    );

    Page<WebsitePricingPlanFeature> searchPricingPlanFeatures(
            UUID pricingPlanVersionId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    );

    WebsitePricingPlanFeature updatePricingPlanFeatureStatus(
            UUID pricingPlanFeatureId,
            boolean isActive,
            UUID administratorId
    );

    WebsitePricingPlanFeature updatePricingPlanFeatureOrder(
            UUID pricingPlanFeatureId,
            Integer displayOrder,
            UUID administratorId
    );

    void deletePricingPlanFeature(
            UUID pricingPlanFeatureId,
            UUID administratorId
    );

    List<WebsitePricingPlanFeature> getPublicFeaturesByPlanCode(
            String planCode
    );

    List<WebsitePricingPlanFeature> getPublicFeaturesByPlanSlug(
            String planSlug
    );

    long countPricingPlanFeatures(
            UUID pricingPlanVersionId
    );

    long countActivePricingPlanFeatures(
            UUID pricingPlanVersionId
    );
}