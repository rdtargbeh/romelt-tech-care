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
import romelt_techcare.backend.entity.WebsiteSocialLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides database access for social-media and external-profile links
 * displayed on the Romelt TechCare website.
 *
 * Responsibilities:
 * - Retrieves social links by identifier and platform.
 * - Retrieves active public links in display order.
 * - Supports administrator search and pagination.
 * - Enforces platform uniqueness checks.
 * - Supports locked update and deletion operations.
 * ================================================================
 */
@Repository
public interface WebsiteSocialLinkRepository
        extends JpaRepository<WebsiteSocialLink, UUID> {

    /**
     * Retrieves one social link with administrator attribution.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteSocialLink> findBySocialLinkId(
            UUID socialLinkId
    );

    /**
     * Retrieves one social link by normalized platform.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteSocialLink> findByPlatform(
            String platform
    );

    /**
     * Retrieves active links for the public website.
     */
    List<WebsiteSocialLink>
    findAllByIsActiveTrueOrderByDisplayOrderAscPlatformAsc();

    /**
     * Retrieves all links for administrator management.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Page<WebsiteSocialLink>
    findAllByOrderByDisplayOrderAscPlatformAsc(
            Pageable pageable
    );

    /**
     * Retrieves links filtered by active status.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Page<WebsiteSocialLink>
    findAllByIsActiveOrderByDisplayOrderAscPlatformAsc(
            Boolean isActive,
            Pageable pageable
    );

    /**
     * Locks one social link for modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select socialLink
            from WebsiteSocialLink socialLink
            where socialLink.socialLinkId = :socialLinkId
            """)
    Optional<WebsiteSocialLink> findByIdForUpdate(
            @Param("socialLinkId")
            UUID socialLinkId
    );

    /**
     * Checks whether a platform already exists.
     */
    boolean existsByPlatform(
            String platform
    );

    /**
     * Checks whether another record already uses the platform.
     */
    boolean existsByPlatformAndSocialLinkIdNot(
            String platform,
            UUID socialLinkId
    );

    /**
     * Searches social links using optional filters.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    @Query("""
            select socialLink
            from WebsiteSocialLink socialLink
            where (
                    :keyword is null
                    or lower(socialLink.platform)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(socialLink.label, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(socialLink.profileUrl)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(socialLink.iconKey, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :isActive is null
                    or socialLink.isActive = :isActive
                  )
            order by socialLink.displayOrder asc,
                     socialLink.platform asc
            """)
    Page<WebsiteSocialLink> searchSocialLinks(
            @Param("keyword")
            String keyword,

            @Param("isActive")
            Boolean isActive,

            Pageable pageable
    );

    /**
     * Counts active social links.
     */
    long countByIsActiveTrue();
}