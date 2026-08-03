package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsitePricingPlanFeature;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for pricing-plan version features.
 *
 * Responsibilities:
 * - Retrieves features by ID and pricing-plan version.
 * - Retrieves ordered administrator-facing features.
 * - Searches features by text, icon, and active status.
 * - Supports pessimistic write locking.
 * - Retrieves active features for current public plan versions.
 * - Counts features by version.
 * ================================================================
 */
@Repository
public interface WebsitePricingPlanFeatureRepository
        extends JpaRepository<WebsitePricingPlanFeature, UUID> {

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsitePricingPlanFeature>
    findByPricingPlanFeatureId(
            UUID pricingPlanFeatureId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsitePricingPlanFeature>
    findByPricingPlanFeatureIdAndPricingPlanVersion_PricingPlanVersionId(
            UUID pricingPlanFeatureId,
            UUID pricingPlanVersionId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsitePricingPlanFeature>
    findAllByPricingPlanVersion_PricingPlanVersionIdOrderByDisplayOrderAscCreatedAtAsc(
            UUID pricingPlanVersionId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Page<WebsitePricingPlanFeature>
    findAllByPricingPlanVersion_PricingPlanVersionIdOrderByDisplayOrderAscCreatedAtAsc(
            UUID pricingPlanVersionId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    @Query("""
            select feature
            from WebsitePricingPlanFeature feature
            where feature.pricingPlanVersion.pricingPlanVersionId =
                  :pricingPlanVersionId
              and (
                    :keyword is null
                    or lower(feature.featureText)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(feature.iconKey, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :isActive is null
                    or feature.isActive = :isActive
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    Page<WebsitePricingPlanFeature> searchPricingPlanFeatures(
            @Param("pricingPlanVersionId")
            UUID pricingPlanVersionId,

            @Param("keyword")
            String keyword,

            @Param("isActive")
            Boolean isActive,

            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select feature
            from WebsitePricingPlanFeature feature
            join fetch feature.pricingPlanVersion version
            join fetch version.pricingPlan pricingPlan
            where feature.pricingPlanFeatureId =
                  :pricingPlanFeatureId
            """)
    Optional<WebsitePricingPlanFeature> findByIdForUpdate(
            @Param("pricingPlanFeatureId")
            UUID pricingPlanFeatureId
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan"
    })
    @Query("""
            select feature
            from WebsitePricingPlanFeature feature
            join feature.pricingPlanVersion version
            join version.pricingPlan pricingPlan
            where pricingPlan.planCode = :planCode
              and pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and feature.isActive = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    List<WebsitePricingPlanFeature> findPublicByPlanCode(
            @Param("planCode")
            String planCode,

            @Param("currentTime")
            Instant currentTime
    );

    @EntityGraph(attributePaths = {
            "pricingPlanVersion",
            "pricingPlanVersion.pricingPlan"
    })
    @Query("""
            select feature
            from WebsitePricingPlanFeature feature
            join feature.pricingPlanVersion version
            join version.pricingPlan pricingPlan
            where pricingPlan.planSlug = :planSlug
              and pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and feature.isActive = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    List<WebsitePricingPlanFeature> findPublicByPlanSlug(
            @Param("planSlug")
            String planSlug,

            @Param("currentTime")
            Instant currentTime
    );

    long countByPricingPlanVersion_PricingPlanVersionId(
            UUID pricingPlanVersionId
    );

    long countByPricingPlanVersion_PricingPlanVersionIdAndIsActiveTrue(
            UUID pricingPlanVersionId
    );

    void deleteAllByPricingPlanVersion_PricingPlanVersionId(
            UUID pricingPlanVersionId
    );
}