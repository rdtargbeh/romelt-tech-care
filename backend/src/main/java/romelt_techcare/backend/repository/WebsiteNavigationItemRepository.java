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
import romelt_techcare.backend.entity.WebsiteNavigationItem;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for website navigation items.
 *
 * Responsibilities:
 * - Retrieves items by ID, location, key, page, and visibility.
 * - Retrieves ordered public navigation.
 * - Supports administrator search and pagination.
 * - Supports soft-deleted item retrieval and restoration.
 * - Supports locked write operations.
 * - Enforces location-and-key uniqueness checks.
 * ================================================================
 */
@Repository
public interface WebsiteNavigationItemRepository
        extends JpaRepository<WebsiteNavigationItem, UUID> {

    @EntityGraph(attributePaths = {
            "websitePage",
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteNavigationItem>
    findByNavigationItemIdAndDeletedAtIsNull(
            UUID navigationItemId
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteNavigationItem>
    findByNavigationItemId(
            UUID navigationItemId
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteNavigationItem>
    findByNavigationLocationAndItemKeyAndDeletedAtIsNull(
            WebsiteNavigationLocation navigationLocation,
            String itemKey
    );

    @EntityGraph(attributePaths = "websitePage")
    List<WebsiteNavigationItem>
    findAllByNavigationLocationAndIsVisibleTrueAndDeletedAtIsNullOrderByDisplayOrderAscLabelAsc(
            WebsiteNavigationLocation navigationLocation
    );

    @EntityGraph(attributePaths = "websitePage")
    List<WebsiteNavigationItem>
    findAllByIsVisibleTrueAndDeletedAtIsNullOrderByNavigationLocationAscDisplayOrderAscLabelAsc();

    @EntityGraph(attributePaths = {
            "websitePage",
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Page<WebsiteNavigationItem>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from WebsiteNavigationItem item
            where item.navigationItemId = :navigationItemId
              and item.deletedAt is null
            """)
    Optional<WebsiteNavigationItem> findByIdForUpdate(
            @Param("navigationItemId")
            UUID navigationItemId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from WebsiteNavigationItem item
            where item.navigationItemId = :navigationItemId
            """)
    Optional<WebsiteNavigationItem>
    findIncludingDeletedByIdForUpdate(
            @Param("navigationItemId")
            UUID navigationItemId
    );

    boolean existsByNavigationLocationAndItemKey(
            WebsiteNavigationLocation navigationLocation,
            String itemKey
    );

    boolean existsByNavigationLocationAndItemKeyAndNavigationItemIdNot(
            WebsiteNavigationLocation navigationLocation,
            String itemKey,
            UUID navigationItemId
    );

    @EntityGraph(attributePaths = {
            "websitePage",
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    @Query("""
            select item
            from WebsiteNavigationItem item
            where item.deletedAt is null
              and (
                    :keyword is null
                    or lower(item.itemKey)
                        like lower(concat('%', :keyword, '%'))
                    or lower(item.label)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(item.destinationUrl, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(item.iconKey, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :navigationLocation is null
                    or item.navigationLocation = :navigationLocation
                  )
              and (
                    :destinationType is null
                    or item.destinationType = :destinationType
                  )
              and (
                    :isVisible is null
                    or item.isVisible = :isVisible
                  )
            order by item.navigationLocation asc,
                     item.displayOrder asc,
                     item.label asc
            """)
    Page<WebsiteNavigationItem> searchNavigationItems(
            @Param("keyword")
            String keyword,

            @Param("navigationLocation")
            WebsiteNavigationLocation navigationLocation,

            @Param("destinationType")
            WebsiteNavigationDestinationType destinationType,

            @Param("isVisible")
            Boolean isVisible,

            Pageable pageable
    );

    long countByNavigationLocationAndIsVisibleTrueAndDeletedAtIsNull(
            WebsiteNavigationLocation navigationLocation
    );

    long countByDeletedAtIsNotNull();
}