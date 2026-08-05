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
import romelt_techcare.backend.entity.WebsiteServiceFeature;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for version-owned service features.
 *
 * Responsibilities:
 * - Retrieves features by identifier and service version.
 * - Retrieves ordered administrator-facing features.
 * - Retrieves active public features.
 * - Supports keyword and status filtering.
 * - Supports locked write operations.
 * - Counts features by version.
 * ================================================================
 */
@Repository
public interface WebsiteServiceFeatureRepository
        extends JpaRepository<WebsiteServiceFeature, UUID> {

    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteServiceFeature>
    findByServiceFeatureId(
            UUID serviceFeatureId
    );

    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteServiceFeature>
    findByServiceFeatureIdAndServiceVersion_ServiceVersionId(
            UUID serviceFeatureId,
            UUID serviceVersionId
    );

    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsiteServiceFeature>
    findAllByServiceVersion_ServiceVersionIdOrderByDisplayOrderAscCreatedAtAsc(
            UUID serviceVersionId
    );

    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Page<WebsiteServiceFeature>
    findAllByServiceVersion_ServiceVersionIdOrderByDisplayOrderAscCreatedAtAsc(
            UUID serviceVersionId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    @Query("""
            select feature
            from WebsiteServiceFeature feature
            where feature.serviceVersion.serviceVersionId =
                  :serviceVersionId
              and (
                    :keyword is null
                    or lower(feature.featureText)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(feature.iconKey, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :isActive is null
                    or feature.isActive = :isActive
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    Page<WebsiteServiceFeature> searchServiceFeatures(
            @Param("serviceVersionId")
            UUID serviceVersionId,

            @Param("keyword")
            String keyword,

            @Param("isActive")
            Boolean isActive,

            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select feature
            from WebsiteServiceFeature feature
            join fetch feature.serviceVersion version
            join fetch version.websiteService service
            where feature.serviceFeatureId = :serviceFeatureId
            """)
    Optional<WebsiteServiceFeature> findByIdForUpdate(
            @Param("serviceFeatureId")
            UUID serviceFeatureId
    );

    /**
     * Retrieves active public features by service code.
     */
    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService"
    })
    @Query("""
            select feature
            from WebsiteServiceFeature feature
            join feature.serviceVersion version
            join version.websiteService service
            where service.serviceCode = :serviceCode
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId =
                  version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
              and version.isPublic = true
              and feature.isActive = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    List<WebsiteServiceFeature> findPublicByServiceCode(
            @Param("serviceCode")
            String serviceCode,

            @Param("currentTime")
            Instant currentTime
    );

    /**
     * Retrieves active public features by service slug.
     */
    @EntityGraph(attributePaths = {
            "serviceVersion",
            "serviceVersion.websiteService"
    })
    @Query("""
            select feature
            from WebsiteServiceFeature feature
            join feature.serviceVersion version
            join version.websiteService service
            where service.serviceSlug = :serviceSlug
              and service.deletedAt is null
              and service.serviceStatus =
                  romelt_techcare.backend.enums.WebsiteServiceStatus.ACTIVE
              and service.publishedVersionId =
                  version.serviceVersionId
              and version.versionStatus =
                  romelt_techcare.backend.enums.WebsiteServiceVersionStatus.PUBLISHED
              and version.isPublic = true
              and feature.isActive = true
              and (
                    version.effectiveFrom is null
                    or version.effectiveFrom <= :currentTime
                  )
              and (
                    version.effectiveUntil is null
                    or version.effectiveUntil > :currentTime
                  )
            order by feature.displayOrder asc,
                     feature.createdAt asc
            """)
    List<WebsiteServiceFeature> findPublicByServiceSlug(
            @Param("serviceSlug")
            String serviceSlug,

            @Param("currentTime")
            Instant currentTime
    );

    long countByServiceVersion_ServiceVersionId(
            UUID serviceVersionId
    );

    long countByServiceVersion_ServiceVersionIdAndIsActiveTrue(
            UUID serviceVersionId
    );

    void deleteAllByServiceVersion_ServiceVersionId(
            UUID serviceVersionId
    );
}