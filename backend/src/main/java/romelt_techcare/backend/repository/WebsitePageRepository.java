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
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsitePageType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for stable website-page identities.
 *
 * Responsibilities:
 * - Retrieves pages by identifier, key, route, and type.
 * - Retrieves public active pages.
 * - Retrieves soft-deleted pages.
 * - Supports administrator filtering and pagination.
 * - Supports locked lifecycle and version-pointer updates.
 * - Enforces page-key and route uniqueness checks.
 * ================================================================
 */
@Repository
public interface WebsitePageRepository
        extends JpaRepository<WebsitePage, UUID> {

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePage>
    findByWebsitePageIdAndDeletedAtIsNull(
            UUID websitePageId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePage> findByWebsitePageId(
            UUID websitePageId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePage>
    findByPageKeyAndDeletedAtIsNull(
            String pageKey
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsitePage>
    findByRoutePathAndDeletedAtIsNull(
            String routePath
    );

    Optional<WebsitePage>
    findByPageKeyAndIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String pageKey
    );

    Optional<WebsitePage>
    findByRoutePathAndIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String routePath
    );

    List<WebsitePage>
    findAllByIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByPageNameAsc();

    Page<WebsitePage>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select page
            from WebsitePage page
            where page.websitePageId = :websitePageId
              and page.deletedAt is null
            """)
    Optional<WebsitePage> findByIdForUpdate(
            @Param("websitePageId")
            UUID websitePageId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select page
            from WebsitePage page
            where page.websitePageId = :websitePageId
            """)
    Optional<WebsitePage> findIncludingDeletedByIdForUpdate(
            @Param("websitePageId")
            UUID websitePageId
    );

    boolean existsByPageKey(
            String pageKey
    );

    boolean existsByPageKeyAndWebsitePageIdNot(
            String pageKey,
            UUID websitePageId
    );

    boolean existsByRoutePath(
            String routePath
    );

    boolean existsByRoutePathAndWebsitePageIdNot(
            String routePath,
            UUID websitePageId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    @Query("""
            select page
            from WebsitePage page
            where page.deletedAt is null
              and (
                    :keyword is null
                    or lower(page.pageKey)
                        like lower(concat('%', :keyword, '%'))
                    or lower(page.pageName)
                        like lower(concat('%', :keyword, '%'))
                    or lower(page.routePath)
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :pageType is null
                    or page.pageType = :pageType
                  )
              and (
                    :isSystemPage is null
                    or page.isSystemPage = :isSystemPage
                  )
              and (
                    :isActive is null
                    or page.isActive = :isActive
                  )
            order by page.pageName asc
            """)
    Page<WebsitePage> searchWebsitePages(
            @Param("keyword")
            String keyword,

            @Param("pageType")
            WebsitePageType pageType,

            @Param("isSystemPage")
            Boolean isSystemPage,

            @Param("isActive")
            Boolean isActive,

            Pageable pageable
    );

    long countByIsActiveTrueAndDeletedAtIsNull();

    long countByDeletedAtIsNotNull();
}