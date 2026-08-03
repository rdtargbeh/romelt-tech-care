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
import romelt_techcare.backend.entity.WebsiteFaqVersion;
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for FAQ drafts, published content,
 * archived history, category retrieval, and public FAQ content.
 *
 * Responsibilities:
 * - Retrieves versions by ID, FAQ, status, and version number.
 * - Retrieves current draft and published versions.
 * - Calculates sequential version numbers.
 * - Supports pessimistic lifecycle locking.
 * - Retrieves public FAQ content in display order.
 * - Supports public filtering by category and featured status.
 * ================================================================
 */
@Repository
public interface WebsiteFaqVersionRepository
        extends JpaRepository<WebsiteFaqVersion, UUID> {

    @EntityGraph(attributePaths = {
            "faq",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteFaqVersion> findByFaqVersionId(
            UUID faqVersionId
    );

    @EntityGraph(attributePaths = {
            "faq",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteFaqVersion>
    findByFaqVersionIdAndFaq_FaqId(
            UUID faqVersionId,
            UUID faqId
    );

    @EntityGraph(attributePaths = {
            "faq",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteFaqVersion>
    findByFaq_FaqIdAndVersionStatus(
            UUID faqId,
            WebsiteFaqVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = {
            "faq",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsiteFaqVersion>
    findAllByFaq_FaqIdOrderByVersionNumberDesc(
            UUID faqId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "faq",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsiteFaqVersion>
    findAllByFaq_FaqIdAndVersionStatusOrderByVersionNumberDesc(
            UUID faqId,
            WebsiteFaqVersionStatus versionStatus,
            Pageable pageable
    );

    boolean existsByFaq_FaqIdAndVersionStatus(
            UUID faqId,
            WebsiteFaqVersionStatus versionStatus
    );

    @Query("""
            select coalesce(max(version.versionNumber), 0)
            from WebsiteFaqVersion version
            where version.faq.faqId = :faqId
            """)
    int findMaximumVersionNumber(
            @Param("faqId")
            UUID faqId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsiteFaqVersion version
            join fetch version.faq faq
            where version.faqVersionId = :faqVersionId
            """)
    Optional<WebsiteFaqVersion> findByIdForUpdate(
            @Param("faqVersionId")
            UUID faqVersionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsiteFaqVersion version
            join fetch version.faq faq
            where faq.faqId = :faqId
              and version.versionStatus = :versionStatus
            """)
    Optional<WebsiteFaqVersion> findByFaqAndStatusForUpdate(
            @Param("faqId")
            UUID faqId,

            @Param("versionStatus")
            WebsiteFaqVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = "faq")
    @Query("""
            select version
            from WebsiteFaqVersion version
            join version.faq faq
            where faq.faqKey = :faqKey
              and faq.deletedAt is null
              and faq.faqStatus =
                  romelt_techcare.backend.enums.WebsiteFaqStatus.ACTIVE
              and faq.publishedVersionId = version.faqVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteFaqVersionStatus.PUBLISHED
              and version.isPublic = true
            """)
    Optional<WebsiteFaqVersion> findPublicByFaqKey(
            @Param("faqKey")
            String faqKey
    );

    @EntityGraph(attributePaths = "faq")
    @Query("""
            select version
            from WebsiteFaqVersion version
            join version.faq faq
            where faq.deletedAt is null
              and faq.faqStatus =
                  romelt_techcare.backend.enums.WebsiteFaqStatus.ACTIVE
              and faq.publishedVersionId = version.faqVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteFaqVersionStatus.PUBLISHED
              and version.isPublic = true
            order by version.displayOrder asc,
                     version.question asc
            """)
    List<WebsiteFaqVersion> findAllPublicFaqs();

    @EntityGraph(attributePaths = "faq")
    @Query("""
            select version
            from WebsiteFaqVersion version
            join version.faq faq
            where faq.deletedAt is null
              and faq.faqStatus =
                  romelt_techcare.backend.enums.WebsiteFaqStatus.ACTIVE
              and faq.publishedVersionId = version.faqVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteFaqVersionStatus.PUBLISHED
              and version.isPublic = true
              and version.isFeatured = true
            order by version.displayOrder asc,
                     version.question asc
            """)
    List<WebsiteFaqVersion> findAllFeaturedPublicFaqs();

    @EntityGraph(attributePaths = "faq")
    @Query("""
            select version
            from WebsiteFaqVersion version
            join version.faq faq
            where faq.deletedAt is null
              and faq.faqStatus =
                  romelt_techcare.backend.enums.WebsiteFaqStatus.ACTIVE
              and faq.publishedVersionId = version.faqVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteFaqVersionStatus.PUBLISHED
              and version.isPublic = true
              and lower(version.faqCategory) = lower(:faqCategory)
            order by version.displayOrder asc,
                     version.question asc
            """)
    List<WebsiteFaqVersion> findAllPublicFaqsByCategory(
            @Param("faqCategory")
            String faqCategory
    );

    @Query("""
            select distinct version.faqCategory
            from WebsiteFaqVersion version
            join version.faq faq
            where faq.deletedAt is null
              and faq.faqStatus =
                  romelt_techcare.backend.enums.WebsiteFaqStatus.ACTIVE
              and faq.publishedVersionId = version.faqVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteFaqVersionStatus.PUBLISHED
              and version.isPublic = true
              and version.faqCategory is not null
            order by version.faqCategory asc
            """)
    List<String> findPublicFaqCategories();

    long countByFaq_FaqId(
            UUID faqId
    );
}