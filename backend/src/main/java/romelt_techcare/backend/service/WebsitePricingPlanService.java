package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for stable website pricing-plan
 * identities.
 *
 * Responsibilities:
 * - Creates pricing-plan identities.
 * - Updates plan codes, slugs, and lifecycle status.
 * - Retrieves plans by identifier, code, and slug.
 * - Searches administrator-facing plans.
 * - Activates, deactivates, and archives plans.
 * - Soft-deletes and restores plans.
 * - Retrieves active public plan identities.
 * - Returns status and deletion counts.
 *
 * Version lifecycle:
 * Draft creation, content editing, publishing, version history,
 * archival, and version-pointer synchronization belong exclusively to
 * WebsitePricingPlanVersionService after that module is implemented.
 *
 * This service does not expose arbitrary version-pointer operations.
 * ================================================================
 */
public interface WebsitePricingPlanService {

    WebsitePricingPlan createPricingPlan(
            WebsitePricingPlan pricingPlan,
            UUID administratorId
    );

    WebsitePricingPlan updatePricingPlan(
            UUID pricingPlanId,
            WebsitePricingPlan requestedUpdate,
            UUID administratorId
    );

    WebsitePricingPlan getPricingPlan(
            UUID pricingPlanId
    );

    WebsitePricingPlan getPricingPlanByCode(
            String planCode
    );

    WebsitePricingPlan getPricingPlanBySlug(
            String planSlug
    );

    Page<WebsitePricingPlan> searchPricingPlans(
            String keyword,
            WebsitePricingPlanStatus planStatus,
            Pageable pageable
    );

    Page<WebsitePricingPlan> getDeletedPricingPlans(
            Pageable pageable
    );

    WebsitePricingPlan updatePricingPlanStatus(
            UUID pricingPlanId,
            WebsitePricingPlanStatus planStatus,
            UUID administratorId
    );

    WebsitePricingPlan activatePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    );

    WebsitePricingPlan deactivatePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    );

    WebsitePricingPlan archivePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    );

    void deletePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    );

    WebsitePricingPlan restorePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    );

    WebsitePricingPlan getPublicPricingPlanByCode(
            String planCode
    );

    WebsitePricingPlan getPublicPricingPlanBySlug(
            String planSlug
    );

    List<WebsitePricingPlan> getPublicPricingPlans();

    long countPricingPlansByStatus(
            WebsitePricingPlanStatus planStatus
    );

    long countDeletedPricingPlans();
}