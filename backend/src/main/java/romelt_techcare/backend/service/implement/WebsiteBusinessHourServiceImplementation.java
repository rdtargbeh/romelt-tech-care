package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessHour;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteBusinessHourRepository;
import romelt_techcare.backend.repository.WebsiteBusinessProfileRepository;
import romelt_techcare.backend.service.WebsiteBusinessHourService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for the normal weekly public
 * operating schedule.
 *
 * Responsibilities:
 * - Creates or updates one profile/day row.
 * - Performs bulk weekly updates in one transaction.
 * - Enforces one row per profile/day.
 * - Validates opening and closing time combinations.
 * - Clears times automatically when a day is closed.
 * - Preserves creation attribution during updates.
 * - Records the administrator responsible for each change.
 * - Records immutable audit events.
 * - Returns ordered schedules for administrator and public consumers.
 *
 * Publishing behavior:
 * Normal business-hour changes become public immediately because this
 * table is read directly by the public website endpoint.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteBusinessHourServiceImplementation
        implements WebsiteBusinessHourService {

    private final WebsiteBusinessHourRepository
            websiteBusinessHourRepository;

    private final WebsiteBusinessProfileRepository
            websiteBusinessProfileRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsiteBusinessHour upsertBusinessHour(
            UUID businessProfileId,
            WebsiteBusinessHour requestedBusinessHour,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        if (requestedBusinessHour == null) {
            throw badRequest(
                    "Business-hour information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile businessProfile =
                getRequiredBusinessProfile(
                        businessProfileId
                );

        validateRequestedBusinessHour(
                requestedBusinessHour
        );

        Short dayOfWeek =
                requestedBusinessHour.getDayOfWeek();

        WebsiteBusinessHour existingBusinessHour =
                websiteBusinessHourRepository
                        .findByProfileAndDayForUpdate(
                                businessProfileId,
                                dayOfWeek
                        )
                        .orElse(null);

        boolean creating =
                existingBusinessHour == null;

        JsonNode beforeSnapshot =
                creating
                        ? null
                        : createBusinessHourSnapshot(
                        existingBusinessHour
                );

        WebsiteBusinessHour businessHour;

        if (creating) {
            businessHour =
                    WebsiteBusinessHour.builder()
                            .businessProfile(
                                    businessProfile
                            )
                            .dayOfWeek(dayOfWeek)
                            .createdByAdminUser(
                                    administrator
                            )
                            .updatedByAdminUser(
                                    administrator
                            )
                            .build();
        } else {
            businessHour = existingBusinessHour;
        }

        applyRequestedSchedule(
                businessHour,
                requestedBusinessHour,
                administrator
        );

        WebsiteBusinessHour savedBusinessHour =
                saveBusinessHour(businessHour);

        recordBusinessHourAudit(
                administratorId,
                creating
                        ? WebsiteContentAuditAction.CREATE
                        : WebsiteContentAuditAction.UPDATE,
                savedBusinessHour,
                beforeSnapshot,
                creating
                        ? "Website business-hour entry created."
                        : "Website business-hour entry updated."
        );

        return savedBusinessHour;
    }

    @Override
    @Transactional
    public List<WebsiteBusinessHour> upsertWeeklyBusinessHours(
            UUID businessProfileId,
            List<WebsiteBusinessHour> requestedBusinessHours,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        if (
                requestedBusinessHours == null
                        || requestedBusinessHours.isEmpty()
        ) {
            throw badRequest(
                    "At least one business-hour entry is required."
            );
        }

        if (requestedBusinessHours.size() > 7) {
            throw badRequest(
                    "A weekly schedule cannot contain more than seven entries."
            );
        }

        validateUniqueDays(requestedBusinessHours);

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile businessProfile =
                getRequiredBusinessProfile(
                        businessProfileId
                );

        List<AuditPendingBusinessHour> auditEntries =
                new ArrayList<>();

        List<WebsiteBusinessHour> savedBusinessHours =
                new ArrayList<>();

        for (
                WebsiteBusinessHour requestedBusinessHour
                : requestedBusinessHours
        ) {
            if (requestedBusinessHour == null) {
                throw badRequest(
                        "Business-hour entries must not be null."
                );
            }

            validateRequestedBusinessHour(
                    requestedBusinessHour
            );

            Short dayOfWeek =
                    requestedBusinessHour.getDayOfWeek();

            WebsiteBusinessHour existingBusinessHour =
                    websiteBusinessHourRepository
                            .findByProfileAndDayForUpdate(
                                    businessProfileId,
                                    dayOfWeek
                            )
                            .orElse(null);

            boolean creating =
                    existingBusinessHour == null;

            JsonNode beforeSnapshot =
                    creating
                            ? null
                            : createBusinessHourSnapshot(
                            existingBusinessHour
                    );

            WebsiteBusinessHour businessHour;

            if (creating) {
                businessHour =
                        WebsiteBusinessHour.builder()
                                .businessProfile(
                                        businessProfile
                                )
                                .dayOfWeek(dayOfWeek)
                                .createdByAdminUser(
                                        administrator
                                )
                                .updatedByAdminUser(
                                        administrator
                                )
                                .build();
            } else {
                businessHour =
                        existingBusinessHour;
            }

            applyRequestedSchedule(
                    businessHour,
                    requestedBusinessHour,
                    administrator
            );

            WebsiteBusinessHour savedBusinessHour =
                    websiteBusinessHourRepository.save(
                            businessHour
                    );

            savedBusinessHours.add(
                    savedBusinessHour
            );

            auditEntries.add(
                    new AuditPendingBusinessHour(
                            savedBusinessHour,
                            beforeSnapshot,
                            creating
                    )
            );
        }

        try {
            websiteBusinessHourRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The weekly schedule contains a duplicate business "
                            + "profile and day combination.",
                    exception
            );
        }

        for (
                AuditPendingBusinessHour auditEntry
                : auditEntries
        ) {
            recordBusinessHourAudit(
                    administratorId,
                    auditEntry.creating()
                            ? WebsiteContentAuditAction.CREATE
                            : WebsiteContentAuditAction.UPDATE,
                    auditEntry.businessHour(),
                    auditEntry.beforeSnapshot(),
                    auditEntry.creating()
                            ? "Weekly schedule business-hour entry created."
                            : "Weekly schedule business-hour entry updated."
            );
        }

        return websiteBusinessHourRepository
                .findAllByBusinessProfile_BusinessProfileIdOrderByDisplayOrderAscDayOfWeekAsc(
                        businessProfileId
                );
    }

    @Override
    public WebsiteBusinessHour getBusinessHour(
            UUID businessHourId
    ) {
        requireIdentifier(
                businessHourId,
                "Business hour ID"
        );

        return websiteBusinessHourRepository
                .findByBusinessHourId(businessHourId)
                .orElseThrow(() -> notFound(
                        "Website business hour was not found."
                ));
    }

    @Override
    public WebsiteBusinessHour getBusinessHourByDay(
            UUID businessProfileId,
            short dayOfWeek
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        validateDayOfWeek(dayOfWeek);

        return websiteBusinessHourRepository
                .findByBusinessProfile_BusinessProfileIdAndDayOfWeek(
                        businessProfileId,
                        dayOfWeek
                )
                .orElseThrow(() -> notFound(
                        "Website business hour was not found for the "
                                + "specified day."
                ));
    }

    @Override
    public List<WebsiteBusinessHour> getBusinessHoursByProfile(
            UUID businessProfileId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        if (
                !websiteBusinessProfileRepository.existsById(
                        businessProfileId
                )
        ) {
            throw notFound(
                    "Website business profile was not found."
            );
        }

        return websiteBusinessHourRepository
                .findAllByBusinessProfile_BusinessProfileIdOrderByDisplayOrderAscDayOfWeekAsc(
                        businessProfileId
                );
    }

    @Override
    public List<WebsiteBusinessHour>
    getActivePublicBusinessHours() {
        if (
                !websiteBusinessProfileRepository
                        .existsByIsActiveTrue()
        ) {
            throw notFound(
                    "An active website business profile was not found."
            );
        }

        return websiteBusinessHourRepository
                .findAllByBusinessProfile_IsActiveTrueOrderByDisplayOrderAscDayOfWeekAsc();
    }

    @Override
    @Transactional
    public void deleteBusinessHour(
            UUID businessHourId,
            UUID administratorId
    ) {
        requireIdentifier(
                businessHourId,
                "Business hour ID"
        );

        getRequiredAdministrator(administratorId);

        WebsiteBusinessHour businessHour =
                websiteBusinessHourRepository
                        .findByIdForUpdate(businessHourId)
                        .orElseThrow(() -> notFound(
                                "Website business hour was not found."
                        ));

        JsonNode beforeSnapshot =
                createBusinessHourSnapshot(
                        businessHour
                );

        String resourceName =
                createBusinessHourResourceName(
                        businessHour
                );

        UUID resourceId =
                businessHour.getBusinessHourId();

        websiteBusinessHourRepository.delete(
                businessHour
        );

        websiteBusinessHourRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType.BUSINESS_HOUR,
                resourceId,
                resourceName,
                beforeSnapshot,
                null,
                "Website business-hour entry deleted.",
                null
        );
    }

    @Override
    @Transactional
    public long deleteBusinessHoursByProfile(
            UUID businessProfileId,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile profile =
                getRequiredBusinessProfile(
                        businessProfileId
                );

        List<WebsiteBusinessHour> existingHours =
                websiteBusinessHourRepository
                        .findAllByBusinessProfile_BusinessProfileIdOrderByDisplayOrderAscDayOfWeekAsc(
                                businessProfileId
                        );

        long deletedCount =
                websiteBusinessHourRepository
                        .deleteAllByBusinessProfile_BusinessProfileId(
                                businessProfileId
                        );

        websiteBusinessHourRepository.flush();

        for (
                WebsiteBusinessHour businessHour
                : existingHours
        ) {
            websiteContentAuditLogService.recordAudit(
                    administratorId,
                    WebsiteContentAuditAction.DELETE,
                    WebsiteContentAuditResourceType.BUSINESS_HOUR,
                    businessHour.getBusinessHourId(),
                    createBusinessHourResourceName(
                            businessHour
                    ),
                    createBusinessHourSnapshot(
                            businessHour
                    ),
                    null,
                    "Website business-hour entry deleted with profile schedule.",
                    null
            );
        }

        if (
                existingHours.isEmpty()
                        && deletedCount > 0
        ) {
            websiteContentAuditLogService.recordAudit(
                    administratorId,
                    WebsiteContentAuditAction.DELETE,
                    WebsiteContentAuditResourceType.BUSINESS_HOUR,
                    businessProfileId,
                    profile.getBusinessName(),
                    null,
                    null,
                    "Deleted "
                            + deletedCount
                            + " business-hour entries for the profile.",
                    null
            );
        }

        return deletedCount;
    }

    private void recordBusinessHourAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteBusinessHour businessHour,
            JsonNode beforeSnapshot,
            String summary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.BUSINESS_HOUR,
                businessHour.getBusinessHourId(),
                createBusinessHourResourceName(
                        businessHour
                ),
                beforeSnapshot,
                createBusinessHourSnapshot(
                        businessHour
                ),
                summary,
                null
        );
    }

    private JsonNode createBusinessHourSnapshot(
            WebsiteBusinessHour businessHour
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "businessHourId",
                businessHour.getBusinessHourId()
        );

        fields.put(
                "businessProfileId",
                businessHour.getBusinessProfile() == null
                        ? null
                        : businessHour
                        .getBusinessProfile()
                        .getBusinessProfileId()
        );

        fields.put(
                "dayOfWeek",
                businessHour.getDayOfWeek()
        );

        fields.put(
                "isClosed",
                businessHour.getIsClosed()
        );

        fields.put(
                "isByAppointment",
                businessHour.getIsByAppointment()
        );

        fields.put(
                "openingTime",
                businessHour.getOpeningTime()
        );

        fields.put(
                "closingTime",
                businessHour.getClosingTime()
        );

        fields.put(
                "displayText",
                businessHour.getDisplayText()
        );

        fields.put(
                "displayOrder",
                businessHour.getDisplayOrder()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private String createBusinessHourResourceName(
            WebsiteBusinessHour businessHour
    ) {
        return "Business Hour - Day "
                + businessHour.getDayOfWeek();
    }

    private void applyRequestedSchedule(
            WebsiteBusinessHour businessHour,
            WebsiteBusinessHour requestedBusinessHour,
            AdminUser administrator
    ) {
        boolean isClosed =
                Boolean.TRUE.equals(
                        requestedBusinessHour.getIsClosed()
                );

        boolean isByAppointment =
                requestedBusinessHour.getIsByAppointment() == null
                        || requestedBusinessHour.getIsByAppointment();

        int displayOrder =
                requestedBusinessHour.getDisplayOrder() == null
                        ? requestedBusinessHour
                        .getDayOfWeek()
                        .intValue()
                        : requestedBusinessHour.getDisplayOrder();

        businessHour.updateSchedule(
                isClosed,
                isByAppointment,
                requestedBusinessHour.getOpeningTime(),
                requestedBusinessHour.getClosingTime(),
                requestedBusinessHour.getDisplayText(),
                displayOrder,
                administrator
        );
    }

    private void validateRequestedBusinessHour(
            WebsiteBusinessHour businessHour
    ) {
        if (businessHour.getDayOfWeek() == null) {
            throw badRequest(
                    "Day of week is required."
            );
        }

        validateDayOfWeek(
                businessHour.getDayOfWeek()
        );

        if (
                businessHour.getDisplayOrder() != null
                        && businessHour.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        boolean isClosed =
                Boolean.TRUE.equals(
                        businessHour.getIsClosed()
                );

        LocalTime openingTime =
                businessHour.getOpeningTime();

        LocalTime closingTime =
                businessHour.getClosingTime();

        if (isClosed) {
            return;
        }

        boolean onlyOneTimeProvided =
                (openingTime == null)
                        != (closingTime == null);

        if (onlyOneTimeProvided) {
            throw badRequest(
                    "Opening time and closing time must either both be "
                            + "provided or both be empty."
            );
        }

        if (
                openingTime != null
                        && !openingTime.isBefore(closingTime)
        ) {
            throw badRequest(
                    "Opening time must be before closing time."
            );
        }

        if (
                businessHour.getDisplayText() != null
                        && businessHour
                        .getDisplayText()
                        .trim()
                        .length() > 120
        ) {
            throw badRequest(
                    "Display text must not exceed 120 characters."
            );
        }
    }

    private void validateUniqueDays(
            List<WebsiteBusinessHour> requestedBusinessHours
    ) {
        Set<Short> encounteredDays =
                new HashSet<>();

        for (
                WebsiteBusinessHour businessHour
                : requestedBusinessHours
        ) {
            if (
                    businessHour == null
                            || businessHour.getDayOfWeek() == null
            ) {
                throw badRequest(
                        "Every business-hour entry must include a day of week."
                );
            }

            if (
                    !encounteredDays.add(
                            businessHour.getDayOfWeek()
                    )
            ) {
                throw badRequest(
                        "The weekly schedule contains duplicate entries "
                                + "for day "
                                + businessHour.getDayOfWeek()
                                + "."
                );
            }
        }
    }

    private WebsiteBusinessProfile getRequiredBusinessProfile(
            UUID businessProfileId
    ) {
        return websiteBusinessProfileRepository
                .findById(businessProfileId)
                .orElseThrow(() -> notFound(
                        "Website business profile was not found."
                ));
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private WebsiteBusinessHour saveBusinessHour(
            WebsiteBusinessHour businessHour
    ) {
        try {
            return websiteBusinessHourRepository
                    .saveAndFlush(businessHour);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A business-hour entry already exists for this "
                            + "business profile and day.",
                    exception
            );
        }
    }

    private void validateDayOfWeek(
            short dayOfWeek
    ) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw badRequest(
                    "Day of week must be between 1 and 7."
            );
        }
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private record AuditPendingBusinessHour(
            WebsiteBusinessHour businessHour,
            JsonNode beforeSnapshot,
            boolean creating
    ) {
    }
}