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
import romelt_techcare.backend.entity.WebsiteBusinessHourException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for date-specific website
 * business-hour exceptions.
 *
 * Responsibilities:
 * - Retrieves one exception by identifier.
 * - Retrieves one profile/date exception.
 * - Retrieves exceptions within a date range.
 * - Retrieves upcoming exceptions.
 * - Retrieves active-profile public exceptions.
 * - Supports pessimistic locking for update and deletion.
 * - Detects duplicate profile/date records.
 * ================================================================
 */
@Repository
public interface WebsiteBusinessHourExceptionRepository
        extends JpaRepository<WebsiteBusinessHourException, UUID> {

    /**
     * Retrieves one exception with profile and administrator data.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessHourException>
    findByBusinessHourExceptionId(
            UUID businessHourExceptionId
    );

    /**
     * Retrieves one exception for a profile and date.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessHourException>
    findByBusinessProfile_BusinessProfileIdAndExceptionDate(
            UUID businessProfileId,
            LocalDate exceptionDate
    );

    /**
     * Retrieves paginated exceptions for one profile.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Page<WebsiteBusinessHourException>
    findAllByBusinessProfile_BusinessProfileId(
            UUID businessProfileId,
            Pageable pageable
    );

    /**
     * Retrieves profile exceptions within an inclusive date range.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsiteBusinessHourException>
    findAllByBusinessProfile_BusinessProfileIdAndExceptionDateBetweenOrderByExceptionDateAsc(
            UUID businessProfileId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Retrieves upcoming exceptions for one profile.
     */
    @EntityGraph(attributePaths = {
            "businessProfile",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsiteBusinessHourException>
    findAllByBusinessProfile_BusinessProfileIdAndExceptionDateGreaterThanEqualOrderByExceptionDateAsc(
            UUID businessProfileId,
            LocalDate startDate
    );

    /**
     * Retrieves public exceptions for the active business profile
     * within an inclusive date range.
     */
    @EntityGraph(attributePaths = "businessProfile")
    List<WebsiteBusinessHourException>
    findAllByBusinessProfile_IsActiveTrueAndExceptionDateBetweenOrderByExceptionDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Retrieves one active-profile public exception by date.
     */
    @EntityGraph(attributePaths = "businessProfile")
    Optional<WebsiteBusinessHourException>
    findByBusinessProfile_IsActiveTrueAndExceptionDate(
            LocalDate exceptionDate
    );

    /**
     * Locks one exception for modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select exception
            from WebsiteBusinessHourException exception
            where exception.businessHourExceptionId =
                  :businessHourExceptionId
            """)
    Optional<WebsiteBusinessHourException> findByIdForUpdate(
            @Param("businessHourExceptionId")
            UUID businessHourExceptionId
    );

    /**
     * Locks one profile/date row for upsert behavior.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select exception
            from WebsiteBusinessHourException exception
            where exception.businessProfile.businessProfileId =
                  :businessProfileId
              and exception.exceptionDate = :exceptionDate
            """)
    Optional<WebsiteBusinessHourException>
    findByProfileAndDateForUpdate(
            @Param("businessProfileId")
            UUID businessProfileId,

            @Param("exceptionDate")
            LocalDate exceptionDate
    );

    /**
     * Checks whether a profile/date exception already exists.
     */
    boolean existsByBusinessProfile_BusinessProfileIdAndExceptionDate(
            UUID businessProfileId,
            LocalDate exceptionDate
    );

    /**
     * Checks whether another row uses a profile/date combination.
     */
    boolean existsByBusinessProfile_BusinessProfileIdAndExceptionDateAndBusinessHourExceptionIdNot(
            UUID businessProfileId,
            LocalDate exceptionDate,
            UUID businessHourExceptionId
    );

    /**
     * Counts all exceptions belonging to one profile.
     */
    long countByBusinessProfile_BusinessProfileId(
            UUID businessProfileId
    );

    /**
     * Deletes all date-specific exceptions for one profile.
     */
    long deleteAllByBusinessProfile_BusinessProfileId(
            UUID businessProfileId
    );
}