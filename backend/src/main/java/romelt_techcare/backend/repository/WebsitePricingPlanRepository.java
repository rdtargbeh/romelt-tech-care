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
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for stable website pricing-plan
 * identities.
 *
 * Responsibilities:
 * - Retrieves plans by identifier, code, slug, and status.
 * - Retrieves active public plan identities.
 * - Supports administrator filtering and pagination.
 * - Retrieves soft-deleted plans.
 * - Supports locked lifecycle operations.
 * - Supports code and slug uniqueness checks.
 * ================================================================
 */
@Repository
public interface WebsitePricingPlanRepository
        extends JpaRepository<WebsitePricingPlan, UUID> {

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePricingPlan>
    findByPricingPlanIdAndDeletedAtIsNull(
            UUID pricingPlanId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePricingPlan>
    findByPricingPlanId(
            UUID pricingPlanId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePricingPlan>
    findByPlanCodeAndDeletedAtIsNull(
            String planCode
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePricingPlan>
    findByPlanSlugAndDeletedAtIsNull(
            String planSlug
    );

    Optional<WebsitePricingPlan>
    findByPlanCodeAndPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String planCode,
            WebsitePricingPlanStatus planStatus
    );

    Optional<WebsitePricingPlan>
    findByPlanSlugAndPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String planSlug,
            WebsitePricingPlanStatus planStatus
    );

    List<WebsitePricingPlan>
    findAllByPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByPlanCodeAsc(
            WebsitePricingPlanStatus planStatus
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Page<WebsitePricingPlan>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pricingPlan
            from WebsitePricingPlan pricingPlan
            where pricingPlan.pricingPlanId = :pricingPlanId
              and pricingPlan.deletedAt is null
            """)
    Optional<WebsitePricingPlan> findByIdForUpdate(
            @Param("pricingPlanId")
            UUID pricingPlanId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pricingPlan
            from WebsitePricingPlan pricingPlan
            where pricingPlan.pricingPlanId = :pricingPlanId
            """)
    Optional<WebsitePricingPlan>
    findIncludingDeletedByIdForUpdate(
            @Param("pricingPlanId")
            UUID pricingPlanId
    );

    boolean existsByPlanCode(
            String planCode
    );

    boolean existsByPlanCodeAndPricingPlanIdNot(
            String planCode,
            UUID pricingPlanId
    );

    boolean existsByPlanSlug(
            String planSlug
    );

    boolean existsByPlanSlugAndPricingPlanIdNot(
            String planSlug,
            UUID pricingPlanId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    @Query("""
            select pricingPlan
            from WebsitePricingPlan pricingPlan
            where pricingPlan.deletedAt is null
              and (
                    :keyword is null
                    or lower(pricingPlan.planCode)
                        like lower(concat('%', :keyword, '%'))
                    or lower(pricingPlan.planSlug)
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :planStatus is null
                    or pricingPlan.planStatus = :planStatus
                  )
            order by pricingPlan.planCode asc
            """)
    Page<WebsitePricingPlan> searchPricingPlans(
            @Param("keyword")
            String keyword,

            @Param("planStatus")
            WebsitePricingPlanStatus planStatus,

            Pageable pageable
    );

    long countByPlanStatusAndDeletedAtIsNull(
            WebsitePricingPlanStatus planStatus
    );

    long countByDeletedAtIsNotNull();
}