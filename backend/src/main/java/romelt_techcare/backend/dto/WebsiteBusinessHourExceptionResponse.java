package romelt_techcare.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing information for one
 * date-specific business-hour exception.
 *
 * Responsibilities:
 * - Identifies the owning business profile.
 * - Returns exception name, date, closure, appointment, and time data.
 * - Returns administrator attribution.
 * - Returns timestamps and optimistic-lock information.
 * ================================================================
 */
public record WebsiteBusinessHourExceptionResponse(

        UUID businessHourExceptionId,

        UUID businessProfileId,

        LocalDate exceptionDate,

        String exceptionName,

        Boolean isClosed,

        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        String displayText,

        Boolean hasOperatingTimes,

        Boolean completeClosure,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}