package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteBusinessHour;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for normal weekly website business
 * hours.
 *
 * Responsibilities:
 * - Retrieves all hours belonging to a business profile.
 * - Retrieves the active public profile's hours.
 * - Locates one profile/day combination.
 * - Supports locked updates.
 * - Detects duplicate profile/day records.
 * - Supports cascade-safe profile schedule management.
 * ================================================================
 */
@Repository
public interface WebsiteBusinessHourRepository
        extends JpaRepository<WebsiteBusinessHour, UUID> {

    /**
     * Retrieves one business hour with its profile and administrators.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessHour> findByBusinessHourId(
            UUID businessHourId
    );

    /**
     * Retrieves the complete weekly schedule for one profile.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsiteBusinessHour>
    findAllByBusinessProfile_BusinessProfileIdOrderByDisplayOrderAscDayOfWeekAsc(
            UUID businessProfileId
    );

    /**
     * Retrieves public hours for the currently active business profile.
     */
    @EntityGraph(attributePaths = "businessProfile")
    List<WebsiteBusinessHour>
    findAllByBusinessProfile_IsActiveTrueOrderByDisplayOrderAscDayOfWeekAsc();

    /**
     * Retrieves one day for one profile.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessHour>
    findByBusinessProfile_BusinessProfileIdAndDayOfWeek(
            UUID businessProfileId,
            Short dayOfWeek
    );

    /**
     * Locks one business-hour row for modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select businessHour
            from WebsiteBusinessHour businessHour
            where businessHour.businessHourId = :businessHourId
            """)
    Optional<WebsiteBusinessHour> findByIdForUpdate(
            @Param("businessHourId")
            UUID businessHourId
    );

    /**
     * Locks one profile/day row for an upsert operation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select businessHour
            from WebsiteBusinessHour businessHour
            where businessHour.businessProfile.businessProfileId =
                  :businessProfileId
              and businessHour.dayOfWeek = :dayOfWeek
            """)
    Optional<WebsiteBusinessHour> findByProfileAndDayForUpdate(
            @Param("businessProfileId")
            UUID businessProfileId,

            @Param("dayOfWeek")
            Short dayOfWeek
    );

    /**
     * Checks whether a profile already has a schedule row for a day.
     */
    boolean existsByBusinessProfile_BusinessProfileIdAndDayOfWeek(
            UUID businessProfileId,
            Short dayOfWeek
    );

    /**
     * Counts schedule rows for one business profile.
     */
    long countByBusinessProfile_BusinessProfileId(
            UUID businessProfileId
    );

    /**
     * Removes all normal weekly hours for one profile.
     */
    long deleteAllByBusinessProfile_BusinessProfileId(
            UUID businessProfileId
    );
}