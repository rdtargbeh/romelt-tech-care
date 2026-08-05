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
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for stable website FAQ identities.
 *
 * Responsibilities:
 * - Retrieves FAQs by identifier and stable key.
 * - Supports administrator filtering and pagination.
 * - Retrieves soft-deleted FAQ records.
 * - Retrieves active public FAQ identities.
 * - Supports pessimistic lifecycle locking.
 * - Supports FAQ-key uniqueness validation.
 * - Returns status and deletion counts.
 * ================================================================
 */
@Repository
public interface WebsiteFaqRepository
        extends JpaRepository<WebsiteFaq, UUID> {

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteFaq> findByFaqIdAndDeletedAtIsNull(
            UUID faqId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteFaq> findByFaqId(
            UUID faqId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteFaq> findByFaqKeyAndDeletedAtIsNull(
            String faqKey
    );

    Optional<WebsiteFaq>
    findByFaqKeyAndFaqStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String faqKey,
            WebsiteFaqStatus faqStatus
    );

    List<WebsiteFaq>
    findAllByFaqStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByFaqKeyAsc(
            WebsiteFaqStatus faqStatus
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Page<WebsiteFaq>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select faq
            from WebsiteFaq faq
            where faq.faqId = :faqId
              and faq.deletedAt is null
            """)
    Optional<WebsiteFaq> findByIdForUpdate(
            @Param("faqId")
            UUID faqId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select faq
            from WebsiteFaq faq
            where faq.faqId = :faqId
            """)
    Optional<WebsiteFaq> findIncludingDeletedByIdForUpdate(
            @Param("faqId")
            UUID faqId
    );

    boolean existsByFaqKey(
            String faqKey
    );

    boolean existsByFaqKeyAndFaqIdNot(
            String faqKey,
            UUID faqId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    @Query("""
            select faq
            from WebsiteFaq faq
            where faq.deletedAt is null
              and (
                    :keyword is null
                    or lower(faq.faqKey)
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :faqStatus is null
                    or faq.faqStatus = :faqStatus
                  )
            order by faq.faqKey asc
            """)
    Page<WebsiteFaq> searchFaqs(
            @Param("keyword")
            String keyword,

            @Param("faqStatus")
            WebsiteFaqStatus faqStatus,

            Pageable pageable
    );

    long countByFaqStatusAndDeletedAtIsNull(
            WebsiteFaqStatus faqStatus
    );

    long countByDeletedAtIsNotNull();
}