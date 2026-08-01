package romelt_techcare.backend.service;

import romelt_techcare.backend.entity.WebsiteBusinessHour;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for managing the normal weekly public
 * schedule of Romelt TechCare.
 *
 * Responsibilities:
 * - Creates or updates one profile/day schedule.
 * - Updates an entire weekly schedule transactionally.
 * - Retrieves administrator and public schedules.
 * - Deletes one day or the complete weekly schedule.
 * - Enforces one record per profile/day.
 * - Attributes schedule changes to an administrator.
 * ================================================================
 */
public interface WebsiteBusinessHourService {

    /**
     * Creates or updates one business-profile/day schedule row.
     */
    WebsiteBusinessHour upsertBusinessHour(
            UUID businessProfileId,
            WebsiteBusinessHour requestedBusinessHour,
            UUID administratorId
    );

    /**
     * Creates or updates several days in one transaction.
     *
     * The request may contain between one and seven unique days.
     */
    List<WebsiteBusinessHour> upsertWeeklyBusinessHours(
            UUID businessProfileId,
            List<WebsiteBusinessHour> requestedBusinessHours,
            UUID administratorId
    );

    /**
     * Retrieves one business-hour record.
     */
    WebsiteBusinessHour getBusinessHour(
            UUID businessHourId
    );

    /**
     * Retrieves one profile/day record.
     */
    WebsiteBusinessHour getBusinessHourByDay(
            UUID businessProfileId,
            short dayOfWeek
    );

    /**
     * Retrieves the complete schedule for one business profile.
     */
    List<WebsiteBusinessHour> getBusinessHoursByProfile(
            UUID businessProfileId
    );

    /**
     * Retrieves the active public business profile's schedule.
     */
    List<WebsiteBusinessHour> getActivePublicBusinessHours();

    /**
     * Deletes one normal weekly schedule row.
     */
    void deleteBusinessHour(
            UUID businessHourId,
            UUID administratorId
    );

    /**
     * Deletes the complete normal weekly schedule for one profile.
     */
    long deleteBusinessHoursByProfile(
            UUID businessProfileId,
            UUID administratorId
    );
}