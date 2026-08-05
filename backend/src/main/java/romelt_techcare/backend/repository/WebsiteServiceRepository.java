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
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for stable website-service
 * identities.
 *
 * Responsibilities:
 * - Retrieves services by ID, code, slug, and status.
 * - Retrieves active public service identities.
 * - Supports administrator search and pagination.
 * - Retrieves soft-deleted services.
 * - Supports locked lifecycle updates.
 * - Enforces service-code and slug uniqueness checks.
 * ================================================================
 */
@Repository
public interface WebsiteServiceRepository
        extends JpaRepository<WebsiteService, UUID> {

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteService>
    findByServiceIdAndDeletedAtIsNull(
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteService> findByServiceId(
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteService>
    findByServiceCodeAndDeletedAtIsNull(
            String serviceCode
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Optional<WebsiteService>
    findByServiceSlugAndDeletedAtIsNull(
            String serviceSlug
    );

    Optional<WebsiteService>
    findByServiceCodeAndServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String serviceCode,
            WebsiteServiceStatus serviceStatus
    );

    Optional<WebsiteService>
    findByServiceSlugAndServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
            String serviceSlug,
            WebsiteServiceStatus serviceStatus
    );

    List<WebsiteService>
    findAllByServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByServiceCodeAsc(
            WebsiteServiceStatus serviceStatus
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    Page<WebsiteService>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select service
            from WebsiteService service
            where service.serviceId = :serviceId
              and service.deletedAt is null
            """)
    Optional<WebsiteService> findByIdForUpdate(
            @Param("serviceId")
            UUID serviceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select service
            from WebsiteService service
            where service.serviceId = :serviceId
            """)
    Optional<WebsiteService>
    findIncludingDeletedByIdForUpdate(
            @Param("serviceId")
            UUID serviceId
    );

    boolean existsByServiceCode(
            String serviceCode
    );

    boolean existsByServiceCodeAndServiceIdNot(
            String serviceCode,
            UUID serviceId
    );

    boolean existsByServiceSlug(
            String serviceSlug
    );

    boolean existsByServiceSlugAndServiceIdNot(
            String serviceSlug,
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser",
            "deletedByAdminUser"
    })
    @Query("""
            select service
            from WebsiteService service
            where service.deletedAt is null
              and (
                    :keyword is null
                    or lower(service.serviceCode)
                        like lower(concat('%', :keyword, '%'))
                    or lower(service.serviceSlug)
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :serviceStatus is null
                    or service.serviceStatus = :serviceStatus
                  )
            order by service.serviceCode asc
            """)
    Page<WebsiteService> searchWebsiteServices(
            @Param("keyword")
            String keyword,

            @Param("serviceStatus")
            WebsiteServiceStatus serviceStatus,

            Pageable pageable
    );

    long countByServiceStatusAndDeletedAtIsNull(
            WebsiteServiceStatus serviceStatus
    );

    long countByDeletedAtIsNotNull();
}