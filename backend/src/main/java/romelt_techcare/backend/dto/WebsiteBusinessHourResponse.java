package romelt_techcare.backend.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one normal weekly schedule row without serializing JPA
 * relationships directly.
 *
 * Responsibilities:
 * - Identifies the owning business profile.
 * - Returns the numeric and readable day of week.
 * - Returns closed, appointment, time, and display information.
 * - Returns administrator attribution for protected endpoints.
 * - Returns optimistic-lock and timestamp information.
 * ================================================================
 */
public record WebsiteBusinessHourResponse(

        UUID businessHourId,

        UUID businessProfileId,

        Short dayOfWeek,

        String dayName,

        Boolean isClosed,

        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        String displayText,

        Integer displayOrder,

        Boolean hasOperatingTimes,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}