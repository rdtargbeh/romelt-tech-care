package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionService;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionServiceId;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for relationships between
 * pricing-plan versions and stable website services.
 *
 * Responsibilities:
 * - Retrieves relationships by composite identifier.
 * - Retrieves services linked to one pricing-plan version.
 * - Retrieves pricing-plan versions linked to one service.
 * - Supports locked relationship deletion.
 * - Supports bulk membership replacement.
 * - Retrieves public services included in published pricing plans.
 * ================================================================
 */
@Repository
public interface WebsitePricingPlanVersionServiceRepository
        extends JpaRepository<
        WebsitePricingPlanVersionService,
        WebsitePricingPlanVersionServiceId
        > {

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    Optional<WebsitePricingPlanVersionService>
    findByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceId(
            UUID pricingPlanVersionId,
            UUID serviceId
    );

    boolean existsByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceId(
            UUID pricingPlanVersionId,
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    List<WebsitePricingPlanVersionService>
    findAllByPricingPlanVersion_PricingPlanVersionIdOrderByWebsiteService_ServiceCodeAsc(
            UUID pricingPlanVersionId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    List<WebsitePricingPlanVersionService>
    findAllByWebsiteService_ServiceIdOrderByCreatedAtDesc(
            UUID serviceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select relationship
            from WebsitePricingPlanVersionService relationship
            join fetch relationship.pricingPlanVersion version
            join fetch version.pricingPlan pricingPlan
            join fetch relationship.websiteService service
            where version.pricingPlanVersionId =
                  :pricingPlanVersionId
              and service.serviceId = :serviceId
            """)
    Optional<WebsitePricingPlanVersionService>
    findByPricingPlanVersionAndServiceForUpdate(
            @Param("pricingPlanVersionId")
            UUID pricingPlanVersionId,

            @Param("serviceId")
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    List<WebsitePricingPlanVersionService>
    findAllByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceIdIn(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds
    );

    void deleteAllByPricingPlanVersion_PricingPlanVersionId(
            UUID pricingPlanVersionId
    );

    void deleteAllByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceIdNotIn(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds
    );

    long countByPricingPlanVersion_PricingPlanVersionId(
            UUID pricingPlanVersionId
    );

    long countByWebsiteService_ServiceId(
            UUID serviceId
    );

    /**
     * Retrieves public services included in a pricing plan by plan
     * code.
     *
     * Both the pricing plan and service must have active published
     * content.
     */
    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    @Query("""
            select relationship
            from WebsitePricingPlanVersionService relationship
            join relationship.pricingPlanVersion version
            join version.pricingPlan pricingPlan
            join relationship.websiteService service
            where pricingPlan.planCode = :planCode
              and pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId is not null
            order by service.serviceCode asc
            """)
    List<WebsitePricingPlanVersionService>
    findPublicServicesByPlanCode(
            @Param("planCode")
            String planCode,

            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves public services included in a pricing plan by plan
     * slug.
     */
    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "websiteService"
    })
    @Query("""
            select relationship
            from WebsitePricingPlanVersionService relationship
            join relationship.pricingPlanVersion version
            join version.pricingPlan pricingPlan
            join relationship.websiteService service
            where pricingPlan.planSlug = :planSlug
              and pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId is not null
            order by service.serviceCode asc
            """)
    List<WebsitePricingPlanVersionService>
    findPublicServicesByPlanSlug(
            @Param("planSlug")
            String planSlug,

            @Param("currentTime")
            Instant currentTime
    );
}