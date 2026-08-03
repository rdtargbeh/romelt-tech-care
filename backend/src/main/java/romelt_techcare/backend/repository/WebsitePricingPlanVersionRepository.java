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
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for pricing-plan drafts, published
 * versions, archived history, and public pricing content.
 *
 * Responsibilities:
 * - Retrieves versions by ID, plan, status, and number.
 * - Retrieves current draft and published versions.
 * - Calculates sequential version numbers.
 * - Supports pessimistic lifecycle locking.
 * - Retrieves current public, featured, and recommended plans.
 * - Supports ordered version history.
 * ================================================================
 */
@Repository
public interface WebsitePricingPlanVersionRepository
        extends JpaRepository<WebsitePricingPlanVersion, UUID> {

    @EntityGraph(attributePaths = {
            "pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePricingPlanVersion>
    findByPricingPlanVersionId(
            UUID pricingPlanVersionId
    );

    @EntityGraph(attributePaths = {
            "pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePricingPlanVersion>
    findByPricingPlanVersionIdAndPricingPlan_PricingPlanId(
            UUID pricingPlanVersionId,
            UUID pricingPlanId
    );

    @EntityGraph(attributePaths = {
            "pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePricingPlanVersion>
    findByPricingPlan_PricingPlanIdAndVersionStatus(
            UUID pricingPlanId,
            WebsitePricingPlanVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = {
            "pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsitePricingPlanVersion>
    findAllByPricingPlan_PricingPlanIdOrderByVersionNumberDesc(
            UUID pricingPlanId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "pricingPlan",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsitePricingPlanVersion>
    findAllByPricingPlan_PricingPlanIdAndVersionStatusOrderByVersionNumberDesc(
            UUID pricingPlanId,
            WebsitePricingPlanVersionStatus versionStatus,
            Pageable pageable
    );

    boolean existsByPricingPlan_PricingPlanIdAndVersionStatus(
            UUID pricingPlanId,
            WebsitePricingPlanVersionStatus versionStatus
    );

    @Query("""
            select coalesce(max(version.versionNumber), 0)
            from WebsitePricingPlanVersion version
            where version.pricingPlan.pricingPlanId = :pricingPlanId
            """)
    int findMaximumVersionNumber(
            @Param("pricingPlanId")
            UUID pricingPlanId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsitePricingPlanVersion version
            join fetch version.pricingPlan pricingPlan
            where version.pricingPlanVersionId =
                  :pricingPlanVersionId
            """)
    Optional<WebsitePricingPlanVersion> findByIdForUpdate(
            @Param("pricingPlanVersionId")
            UUID pricingPlanVersionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsitePricingPlanVersion version
            where version.pricingPlan.pricingPlanId =
                  :pricingPlanId
              and version.versionStatus = :versionStatus
            """)
    Optional<WebsitePricingPlanVersion>
    findByPricingPlanAndStatusForUpdate(
            @Param("pricingPlanId")
            UUID pricingPlanId,

            @Param("versionStatus")
            WebsitePricingPlanVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = "pricingPlan")
    @Query("""
            select version
            from WebsitePricingPlanVersion version
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
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            """)
    Optional<WebsitePricingPlanVersion> findPublicByPlanCode(
            @Param("planCode")
            String planCode,

            @Param("currentTime")
            Instant currentTime
    );

    @EntityGraph(attributePaths = "pricingPlan")
    @Query("""
            select version
            from WebsitePricingPlanVersion version
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
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            """)
    Optional<WebsitePricingPlanVersion> findPublicByPlanSlug(
            @Param("planSlug")
            String planSlug,

            @Param("currentTime")
            Instant currentTime
    );

    @EntityGraph(attributePaths = "pricingPlan")
    @Query("""
            select version
            from WebsitePricingPlanVersion version
            join version.pricingPlan pricingPlan
            where pricingPlan.deletedAt is null
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
            order by version.displayOrder asc,
                     version.planName asc
            """)
    List<WebsitePricingPlanVersion> findAllPublicPricingPlans(
            @Param("currentTime")
            Instant currentTime
    );

    @EntityGraph(attributePaths = "pricingPlan")
    @Query("""
            select version
            from WebsitePricingPlanVersion version
            join version.pricingPlan pricingPlan
            where pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and version.isFeatured = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by version.displayOrder asc,
                     version.planName asc
            """)
    List<WebsitePricingPlanVersion>
    findAllFeaturedPublicPricingPlans(
            @Param("currentTime")
            Instant currentTime
    );

    @EntityGraph(attributePaths = "pricingPlan")
    @Query("""
            select version
            from WebsitePricingPlanVersion version
            join version.pricingPlan pricingPlan
            where pricingPlan.deletedAt is null
              and pricingPlan.planStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanStatus.ACTIVE
              and pricingPlan.publishedVersionId =
                  version.pricingPlanVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus.PUBLISHED
              and version.isPublic = true
              and version.isRecommended = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by version.displayOrder asc,
                     version.planName asc
            """)
    List<WebsitePricingPlanVersion>
    findAllRecommendedPublicPricingPlans(
            @Param("currentTime")
            Instant currentTime
    );

    long countByPricingPlan_PricingPlanId(
            UUID pricingPlanId
    );
}