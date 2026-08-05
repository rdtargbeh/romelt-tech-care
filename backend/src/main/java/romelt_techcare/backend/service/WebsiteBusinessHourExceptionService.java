package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteBusinessHourException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for managing date-specific exceptions
 * to the normal Romelt TechCare weekly schedule.
 *
 * Responsibilities:
 * - Creates one date-specific exception.
 * - Updates an existing exception.
 * - Creates or updates an exception by profile/date.
 * - Retrieves exceptions by ID, date, profile, and date range.
 * - Retrieves active public-profile exceptions.
 * - Deletes one exception or all exceptions for a profile.
 * - Enforces one exception per profile/date.
 * - Attributes changes to an administrator.
 * ================================================================
 */
public interface WebsiteBusinessHourExceptionService {

    WebsiteBusinessHourException createBusinessHourException(
            UUID businessProfileId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    );

    WebsiteBusinessHourException updateBusinessHourException(
            UUID businessHourExceptionId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    );

    WebsiteBusinessHourException upsertBusinessHourException(
            UUID businessProfileId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    );

    WebsiteBusinessHourException getBusinessHourException(
            UUID businessHourExceptionId
    );

    WebsiteBusinessHourException getBusinessHourExceptionByDate(
            UUID businessProfileId,
            LocalDate exceptionDate
    );

    Page<WebsiteBusinessHourException>
    getBusinessHourExceptionsByProfile(
            UUID businessProfileId,
            Pageable pageable
    );

    List<WebsiteBusinessHourException>
    getBusinessHourExceptionsByDateRange(
            UUID businessProfileId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<WebsiteBusinessHourException>
    getUpcomingBusinessHourExceptions(
            UUID businessProfileId,
            LocalDate startDate
    );

    List<WebsiteBusinessHourException>
    getPublicBusinessHourExceptions(
            LocalDate startDate,
            LocalDate endDate
    );

    WebsiteBusinessHourException
    getPublicBusinessHourExceptionByDate(
            LocalDate exceptionDate
    );

    void deleteBusinessHourException(
            UUID businessHourExceptionId,
            UUID administratorId
    );

    long deleteBusinessHourExceptionsByProfile(
            UUID businessProfileId,
            UUID administratorId
    );
}