package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines draft, publication, archive, history, and public retrieval
 * operations for versioned pricing-plan content.
 *
 * Responsibilities:
 * - Creates new pricing-plan drafts.
 * - Creates drafts from published versions.
 * - Updates draft content.
 * - Publishes drafts transactionally.
 * - Archives prior published versions.
 * - Synchronizes stable plan version pointers.
 * - Retrieves administrator version history.
 * - Retrieves public, featured, and recommended pricing plans.
 * ================================================================
 */
public interface WebsitePricingPlanVersionService {

    WebsitePricingPlanVersion createDraft(
            UUID pricingPlanId,
            WebsitePricingPlanVersion requestedDraft,
            UUID administratorId
    );

    WebsitePricingPlanVersion createDraftFromPublishedVersion(
            UUID pricingPlanId,
            String changeSummary,
            UUID administratorId
    );

    WebsitePricingPlanVersion updateDraft(
            UUID pricingPlanVersionId,
            WebsitePricingPlanVersion requestedUpdate,
            UUID administratorId
    );

    WebsitePricingPlanVersion getPricingPlanVersion(
            UUID pricingPlanVersionId
    );

    WebsitePricingPlanVersion getCurrentDraft(
            UUID pricingPlanId
    );

    WebsitePricingPlanVersion getCurrentPublishedVersion(
            UUID pricingPlanId
    );

    Page<WebsitePricingPlanVersion> getVersionHistory(
            UUID pricingPlanId,
            WebsitePricingPlanVersionStatus versionStatus,
            Pageable pageable
    );

    WebsitePricingPlanVersion publishDraft(
            UUID pricingPlanVersionId,
            UUID administratorId
    );

    WebsitePricingPlanVersion archiveVersion(
            UUID pricingPlanVersionId,
            UUID administratorId
    );

    void deleteDraft(
            UUID pricingPlanVersionId,
            UUID administratorId
    );

    WebsitePricingPlanVersion getPublicPricingPlanByCode(
            String planCode
    );

    WebsitePricingPlanVersion getPublicPricingPlanBySlug(
            String planSlug
    );

    List<WebsitePricingPlanVersion> getPublicPricingPlans();

    List<WebsitePricingPlanVersion>
    getFeaturedPublicPricingPlans();

    List<WebsitePricingPlanVersion>
    getRecommendedPublicPricingPlans();
}