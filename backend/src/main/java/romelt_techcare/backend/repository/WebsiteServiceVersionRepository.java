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
import romelt_techcare.backend.entity.WebsiteServiceVersion;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for service drafts, published
 * versions, and archived service history.
 *
 * Responsibilities:
 * - Retrieves versions by ID, service, status, and number.
 * - Retrieves current draft and published versions.
 * - Calculates the next sequential version number.
 * - Supports locked lifecycle transitions.
 * - Retrieves public, featured, and bookable service content.
 * - Supports ordered service-version history.
 * ================================================================
 */
@Repository
public interface WebsiteServiceVersionRepository
        extends JpaRepository<WebsiteServiceVersion, UUID> {

    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteServiceVersion> findByServiceVersionId(
            UUID serviceVersionId
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteServiceVersion>
    findByServiceVersionIdAndWebsiteService_ServiceId(
            UUID serviceVersionId,
            UUID serviceId
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Optional<WebsiteServiceVersion>
    findByWebsiteService_ServiceIdAndVersionStatus(
            UUID serviceId,
            WebsiteServiceVersionStatus versionStatus
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsiteServiceVersion>
    findAllByWebsiteService_ServiceIdOrderByVersionNumberDesc(
            UUID serviceId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser",
            "publishedByAdminUser",
            "archivedByAdminUser"
    })
    Page<WebsiteServiceVersion>
    findAllByWebsiteService_ServiceIdAndVersionStatusOrderByVersionNumberDesc(
            UUID serviceId,
            WebsiteServiceVersionStatus versionStatus,
            Pageable pageable
    );

    boolean existsByWebsiteService_ServiceIdAndVersionStatus(
            UUID serviceId,
            WebsiteServiceVersionStatus versionStatus
    );

    @Query("""
            select coalesce(max(version.versionNumber), 0)
            from WebsiteServiceVersion version
            where version.websiteService.serviceId = :serviceId
            """)
    int findMaximumVersionNumber(
            @Param("serviceId")
            UUID serviceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsiteServiceVersion version
            where version.serviceVersionId = :serviceVersionId
            """)
    Optional<WebsiteServiceVersion> findByIdForUpdate(
            @Param("serviceVersionId")
            UUID serviceVersionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select version
            from WebsiteServiceVersion version
            where version.websiteService.serviceId = :serviceId
              and version.versionStatus = :versionStatus
            """)
    Optional<WebsiteServiceVersion> findByServiceAndStatusForUpdate(
            @Param("serviceId")
            UUID serviceId,

            @Param("versionStatus")
            WebsiteServiceVersionStatus versionStatus
    );

    /**
     * Retrieves one currently effective published service by code.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia"
    })
    @Query("""
            select version
            from WebsiteServiceVersion version
            join version.websiteService service
            where service.serviceCode = :serviceCode
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId = version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
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
    Optional<WebsiteServiceVersion> findPublicByServiceCode(
            @Param("serviceCode")
            String serviceCode,

            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves one currently effective published service by slug.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia"
    })
    @Query("""
            select version
            from WebsiteServiceVersion version
            join version.websiteService service
            where service.serviceSlug = :serviceSlug
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId = version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
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
    Optional<WebsiteServiceVersion> findPublicByServiceSlug(
            @Param("serviceSlug")
            String serviceSlug,

            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves all currently effective published public services.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia"
    })
    @Query("""
            select version
            from WebsiteServiceVersion version
            join version.websiteService service
            where service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId = version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
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
                     version.serviceName asc
            """)
    List<WebsiteServiceVersion> findAllPublicServices(
            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves currently effective featured services.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia"
    })
    @Query("""
            select version
            from WebsiteServiceVersion version
            join version.websiteService service
            where service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId = version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
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
                     version.serviceName asc
            """)
    List<WebsiteServiceVersion> findAllFeaturedPublicServices(
            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves currently effective services available for booking.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "cardImageMedia",
            "heroImageMedia"
    })
    @Query("""
            select version
            from WebsiteServiceVersion version
            join version.websiteService service
            where service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId = version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
              and version.isPublic = true
              and version.isBookable = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by version.displayOrder asc,
                     version.serviceName asc
            """)
    List<WebsiteServiceVersion> findAllBookablePublicServices(
            @Param("currentTime")
            Instant currentTime
    );

    long countByWebsiteService_ServiceId(
            UUID serviceId
    );
}