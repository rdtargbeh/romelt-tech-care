package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteBusinessHourExceptionResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHourExceptionResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHourExceptionUpsertRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessHourException;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts exception request DTOs into service-compatible entities
 * and persisted entities into administrator and public responses.
 *
 * Responsibilities:
 * - Excludes profile and administrator ownership from request input.
 * - Prevents direct JPA relationship serialization.
 * - Produces administrator attribution snapshots.
 * - Produces safe public exception responses.
 * ================================================================
 */
@Component
public class WebsiteBusinessHourExceptionMapper {

    public WebsiteBusinessHourException toEntity(
            WebsiteBusinessHourExceptionUpsertRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteBusinessHourException.builder()
                .exceptionDate(request.exceptionDate())
                .exceptionName(request.exceptionName())
                .isClosed(request.isClosed())
                .isByAppointment(request.isByAppointment())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .displayText(request.displayText())
                .build();
    }

    public WebsiteBusinessHourExceptionResponse toResponse(
            WebsiteBusinessHourException exception
    ) {
        if (exception == null) {
            return null;
        }

        AdminUser createdBy =
                exception.getCreatedByAdminUser();

        AdminUser updatedBy =
                exception.getUpdatedByAdminUser();

        return new WebsiteBusinessHourExceptionResponse(
                exception.getBusinessHourExceptionId(),
                exception.getBusinessProfile() == null
                        ? null
                        : exception
                        .getBusinessProfile()
                        .getBusinessProfileId(),
                exception.getExceptionDate(),
                exception.getExceptionName(),
                exception.getIsClosed(),
                exception.getIsByAppointment(),
                exception.getOpeningTime(),
                exception.getClosingTime(),
                exception.getDisplayText(),
                exception.hasOperatingTimes(),
                exception.isCompleteClosure(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                exception.getCreatedAt(),
                exception.getUpdatedAt(),
                exception.getRowVersion()
        );
    }

    public PublicWebsiteBusinessHourExceptionResponse
    toPublicResponse(
            WebsiteBusinessHourException exception
    ) {
        if (exception == null) {
            return null;
        }

        return new PublicWebsiteBusinessHourExceptionResponse(
                exception.getExceptionDate(),
                exception.getExceptionName(),
                exception.getIsClosed(),
                exception.getIsByAppointment(),
                exception.getOpeningTime(),
                exception.getClosingTime(),
                exception.getDisplayText()
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