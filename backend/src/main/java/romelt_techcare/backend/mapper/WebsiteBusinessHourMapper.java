package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteBusinessHourResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHourResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHourUpsertRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessHour;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts business-hour request DTOs into service-compatible entities
 * and converts persisted entities into administrator and public API
 * responses.
 *
 * Responsibilities:
 * - Maps complete weekday schedule input.
 * - Excludes protected persistence fields from request mapping.
 * - Prevents direct JPA relationship serialization.
 * - Produces readable weekday names.
 * ================================================================
 */
@Component
public class WebsiteBusinessHourMapper {

    /**
     * Converts an upsert request into a detached schedule entity.
     */
    public WebsiteBusinessHour toEntity(
            WebsiteBusinessHourUpsertRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteBusinessHour.builder()
                .dayOfWeek(request.dayOfWeek())
                .isClosed(request.isClosed())
                .isByAppointment(request.isByAppointment())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .displayText(request.displayText())
                .displayOrder(
                        request.displayOrder() == null
                                ? request.dayOfWeek().intValue()
                                : request.displayOrder()
                )
                .build();
    }

    /**
     * Converts a persisted schedule row into an administrator response.
     */
    public WebsiteBusinessHourResponse toResponse(
            WebsiteBusinessHour businessHour
    ) {
        if (businessHour == null) {
            return null;
        }

        AdminUser createdBy =
                businessHour.getCreatedByAdminUser();

        AdminUser updatedBy =
                businessHour.getUpdatedByAdminUser();

        return new WebsiteBusinessHourResponse(
                businessHour.getBusinessHourId(),
                businessHour.getBusinessProfile() == null
                        ? null
                        : businessHour
                        .getBusinessProfile()
                        .getBusinessProfileId(),
                businessHour.getDayOfWeek(),
                businessHour.getDayName(),
                businessHour.getIsClosed(),
                businessHour.getIsByAppointment(),
                businessHour.getOpeningTime(),
                businessHour.getClosingTime(),
                businessHour.getDisplayText(),
                businessHour.getDisplayOrder(),
                businessHour.hasOperatingTimes(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                businessHour.getCreatedAt(),
                businessHour.getUpdatedAt(),
                businessHour.getRowVersion()
        );
    }

    /**
     * Converts a persisted schedule row into a safe public response.
     */
    public PublicWebsiteBusinessHourResponse toPublicResponse(
            WebsiteBusinessHour businessHour
    ) {
        if (businessHour == null) {
            return null;
        }

        return new PublicWebsiteBusinessHourResponse(
                businessHour.getDayOfWeek(),
                businessHour.getDayName(),
                businessHour.getIsClosed(),
                businessHour.getIsByAppointment(),
                businessHour.getOpeningTime(),
                businessHour.getClosingTime(),
                businessHour.getDisplayText(),
                businessHour.getDisplayOrder()
        );
    }

    private UUID getAdminUserId(
            AdminUser adminUser
    ) {
        return adminUser == null
                ? null
                : adminUser.getAdminUserId();
    }

    private String getAdminUserDisplayName(
            AdminUser adminUser
    ) {
        if (adminUser == null) {
            return null;
        }

        String firstName =
                normalizeOptional(adminUser.getFirstName());

        String lastName =
                normalizeOptional(adminUser.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(adminUser.getEmail());
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}