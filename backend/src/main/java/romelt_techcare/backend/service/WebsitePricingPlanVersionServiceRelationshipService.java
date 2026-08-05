package romelt_techcare.backend.service;

import romelt_techcare.backend.entity.WebsitePricingPlanVersionService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE RELATIONSHIP SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for assigning stable website services
 * to versioned pricing plans.
 *
 * Responsibilities:
 * - Adds one service to a draft pricing-plan version.
 * - Adds multiple services to a draft.
 * - Replaces the complete service membership of a draft.
 * - Removes one service from a draft.
 * - Clears all services from a draft.
 * - Retrieves administrator-facing relationships.
 * - Retrieves active public services included in published plans.
 *
 * Mutation rules:
 * Relationships may be changed only when they belong to the stable
 * pricing plan's current DRAFT version.
 * ================================================================
 */
public interface WebsitePricingPlanVersionServiceRelationshipService {

    WebsitePricingPlanVersionService addService(
            UUID pricingPlanVersionId,
            UUID serviceId,
            UUID administratorId
    );

    List<WebsitePricingPlanVersionService> addServices(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds,
            UUID administratorId
    );

    List<WebsitePricingPlanVersionService> replaceServices(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds,
            UUID administratorId
    );

    void removeService(
            UUID pricingPlanVersionId,
            UUID serviceId,
            UUID administratorId
    );

    void clearServices(
            UUID pricingPlanVersionId,
            UUID administratorId
    );

    WebsitePricingPlanVersionService getRelationship(
            UUID pricingPlanVersionId,
            UUID serviceId
    );

    List<WebsitePricingPlanVersionService>
    getServicesForPricingPlanVersion(
            UUID pricingPlanVersionId
    );

    List<WebsitePricingPlanVersionService>
    getPricingPlanVersionsForService(
            UUID serviceId
    );

    List<WebsitePricingPlanVersionService>
    getPublicServicesByPlanCode(
            String planCode
    );

    List<WebsitePricingPlanVersionService>
    getPublicServicesByPlanSlug(
            String planSlug
    );

    long countServicesForPricingPlanVersion(
            UUID pricingPlanVersionId
    );
}