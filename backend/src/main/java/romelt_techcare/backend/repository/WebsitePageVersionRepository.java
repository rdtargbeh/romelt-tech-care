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
import romelt_techcare.backend.entity.WebsitePageVersion;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for page drafts, published versions,
 * and archived version history.
 *
 * Responsibilities:
 * - Retrieves versions by ID, page, status, and number.
 * - Retrieves the current draft and published version.
 * - Retrieves public content by page key and route.
 * - Calculates the next sequential version number.
 * - Supports locked publication and archive transitions.
 * - Supports ordered version history.
 * ================================================================
 */
@Repository
public interface WebsitePageVersionRepository
        extends JpaRepository<WebsitePageVersion, UUID> {

    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePageVersion> findByPageVersionId(
            UUID pageVersionId
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePageVersion>
    findByPageVersionIdAndWebsitePage_WebsitePageId(
            UUID pageVersionId,
            UUID websitePageId
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsitePageVersion>
    findByWebsitePage_WebsitePageIdAndVersionStatus(
            UUID websitePageId,
            WebsitePageVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsitePageVersion>
    findAllByWebsitePage_WebsitePageIdOrderByVersionNumberDesc(
            UUID websitePageId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsitePageVersion>
    findAllByWebsitePage_WebsitePageIdAndVersionStatusOrderByVersionNumberDesc(
            UUID websitePageId,
            WebsitePageVersionStatus versionStatus,
            Pageable pageable
    );

    boolean existsByWebsitePage_WebsitePageIdAndVersionStatus(
            UUID websitePageId,
            WebsitePageVersionStatus versionStatus
    );

    /**
     * Returns zero when the page has no versions.
     */
    @Query("""
            select coalesce(max(version.versionNumber), 0)
            from WebsitePageVersion version
            where version.websitePage.websitePageId = :websitePageId
            """)
    int findMaximumVersionNumber(
            @Param("websitePageId")
            UUID websitePageId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsitePageVersion version
            where version.pageVersionId = :pageVersionId
            """)
    Optional<WebsitePageVersion> findByIdForUpdate(
            @Param("pageVersionId")
            UUID pageVersionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsitePageVersion version
            where version.websitePage.websitePageId =
                  :websitePageId
              and version.versionStatus = :versionStatus
            """)
    Optional<WebsitePageVersion> findByPageAndStatusForUpdate(
            @Param("websitePageId")
            UUID websitePageId,

            @Param("versionStatus")
            WebsitePageVersionStatus versionStatus
    );

    /**
     * Retrieves public content by page key.
     */
    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia"
    })
    @Query("""
            select version
            from WebsitePageVersion version
            join version.websitePage page
            where page.pageKey = :pageKey
              and page.deletedAt is null
              and page.isActive = true
              and page.publishedVersionId = version.pageVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePageVersionStatus.PUBLISHED
            """)
    Optional<WebsitePageVersion> findPublicByPageKey(
            @Param("pageKey")
            String pageKey
    );

    /**
     * Retrieves public content by normalized React route.
     */
    @EntityGraph(attributePaths = {
            "websitePage",
            "socialImageMedia"
    })
    @Query("""
            select version
            from WebsitePageVersion version
            join version.websitePage page
            where page.routePath = :routePath
              and page.deletedAt is null
              and page.isActive = true
              and page.publishedVersionId = version.pageVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsitePageVersionStatus.PUBLISHED
            """)
    Optional<WebsitePageVersion> findPublicByRoutePath(
            @Param("routePath")
            String routePath
    );

    long countByWebsitePage_WebsitePageId(
            UUID websitePageId
    );
}