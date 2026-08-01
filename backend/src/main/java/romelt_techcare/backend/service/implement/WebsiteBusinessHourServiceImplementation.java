package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessHour;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteBusinessHourRepository;
import romelt_techcare.backend.repository.WebsiteBusinessProfileRepository;
import romelt_techcare.backend.service.WebsiteBusinessHourService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * - Returns ordered schedules for administrator and public consumers.
 *
 * Publishing behavior:
 * Normal business-hour changes become public immediately because this
 * table is read directly by the public website endpoint.
 *
 * Special-date behavior:
 * Holiday and one-date overrides must be managed through the separate
 * WebsiteBusinessHourException module.
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

    private final AdminUserRepository adminUserRepository;

    /**
     * Creates or updates one day.
     */
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
                getRequiredBusinessProfile(businessProfileId);

        validateRequestedBusinessHour(requestedBusinessHour);

        Short dayOfWeek =
                requestedBusinessHour.getDayOfWeek();

        WebsiteBusinessHour existingBusinessHour =
                websiteBusinessHourRepository
                        .findByProfileAndDayForUpdate(
                                businessProfileId,
                                dayOfWeek
                        )
                        .orElse(null);

        WebsiteBusinessHour businessHour;

        if (existingBusinessHour == null) {
            businessHour = WebsiteBusinessHour.builder()
                    .businessProfile(businessProfile)
                    .dayOfWeek(dayOfWeek)
                    .createdByAdminUser(administrator)
                    .updatedByAdminUser(administrator)
                    .build();
        } else {
            businessHour = existingBusinessHour;
        }

        applyRequestedSchedule(
                businessHour,
                requestedBusinessHour,
                administrator
        );

        return saveBusinessHour(businessHour);
    }

    /**
     * Upserts several unique weekdays in one transaction.
     */
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
                getRequiredBusinessProfile(businessProfileId);

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

            WebsiteBusinessHour businessHour =
                    websiteBusinessHourRepository
                            .findByProfileAndDayForUpdate(
                                    businessProfileId,
                                    dayOfWeek
                            )
                            .orElseGet(() ->
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
                                            .build()
                            );

            applyRequestedSchedule(
                    businessHour,
                    requestedBusinessHour,
                    administrator
            );

            savedBusinessHours.add(
                    websiteBusinessHourRepository.save(
                            businessHour
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

        /*
         * Verifies that the acting administrator exists even though the
         * current schema does not retain deleted-by attribution.
         */
        getRequiredAdministrator(administratorId);

        WebsiteBusinessHour businessHour =
                websiteBusinessHourRepository
                        .findByIdForUpdate(businessHourId)
                        .orElseThrow(() -> notFound(
                                "Website business hour was not found."
                        ));

        websiteBusinessHourRepository.delete(
                businessHour
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
        getRequiredBusinessProfile(businessProfileId);

        return websiteBusinessHourRepository
                .deleteAllByBusinessProfile_BusinessProfileId(
                        businessProfileId
                );
    }

    /**
     * Copies the requested editable fields onto the persistent row.
     */
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

    /**
     * Validates one requested schedule entry.
     */
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
                (openingTime == null) != (closingTime == null);

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

    /**
     * Rejects duplicate weekday entries in one bulk request.
     */
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
}