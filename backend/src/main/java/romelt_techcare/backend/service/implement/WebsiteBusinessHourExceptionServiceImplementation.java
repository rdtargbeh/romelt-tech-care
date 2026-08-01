package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessHourException;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteBusinessHourExceptionRepository;
import romelt_techcare.backend.repository.WebsiteBusinessProfileRepository;
import romelt_techcare.backend.service.WebsiteBusinessHourExceptionService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for date-specific exceptions
 * to the normal weekly website schedule.
 *
 * Responsibilities:
 * - Creates and updates holiday, vacation, emergency, and special-hour
 *   records.
 * - Enforces one exception per profile/date.
 * - Supports safe upsert behavior.
 * - Validates complete time pairs.
 * - Clears times when an exception represents a closure.
 * - Retrieves ordered public and administrator exception data.
 * - Attributes writes to an authenticated administrator.
 *
 * Publishing behavior:
 * Exception changes become public immediately after persistence.
 *
 * Date behavior:
 * Past exceptions are retained unless explicitly deleted. This supports
 * historical schedule review and audit logging.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteBusinessHourExceptionServiceImplementation
        implements WebsiteBusinessHourExceptionService {

    private final WebsiteBusinessHourExceptionRepository
            websiteBusinessHourExceptionRepository;

    private final WebsiteBusinessProfileRepository
            websiteBusinessProfileRepository;

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public WebsiteBusinessHourException createBusinessHourException(
            UUID businessProfileId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        validateRequestedException(requestedException);

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile businessProfile =
                getRequiredBusinessProfile(businessProfileId);

        if (
                websiteBusinessHourExceptionRepository
                        .existsByBusinessProfile_BusinessProfileIdAndExceptionDate(
                                businessProfileId,
                                requestedException.getExceptionDate()
                        )
        ) {
            throw conflict(
                    "A business-hour exception already exists for the "
                            + "specified profile and date."
            );
        }

        WebsiteBusinessHourException exception =
                WebsiteBusinessHourException.builder()
                        .businessProfile(businessProfile)
                        .exceptionDate(
                                requestedException.getExceptionDate()
                        )
                        .exceptionName(
                                requestedException.getExceptionName()
                        )
                        .isClosed(
                                resolveClosed(
                                        requestedException.getIsClosed()
                                )
                        )
                        .isByAppointment(
                                resolveByAppointment(
                                        requestedException
                                                .getIsByAppointment()
                                )
                        )
                        .openingTime(
                                requestedException.getOpeningTime()
                        )
                        .closingTime(
                                requestedException.getClosingTime()
                        )
                        .displayText(
                                requestedException.getDisplayText()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        return saveException(exception);
    }

    @Override
    @Transactional
    public WebsiteBusinessHourException updateBusinessHourException(
            UUID businessHourExceptionId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    ) {
        requireIdentifier(
                businessHourExceptionId,
                "Business-hour exception ID"
        );

        validateRequestedException(requestedException);

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessHourException existingException =
                getExceptionForUpdate(
                        businessHourExceptionId
                );

        UUID businessProfileId =
                existingException
                        .getBusinessProfile()
                        .getBusinessProfileId();

        if (
                websiteBusinessHourExceptionRepository
                        .existsByBusinessProfile_BusinessProfileIdAndExceptionDateAndBusinessHourExceptionIdNot(
                                businessProfileId,
                                requestedException.getExceptionDate(),
                                businessHourExceptionId
                        )
        ) {
            throw conflict(
                    "Another business-hour exception already exists for "
                            + "the specified profile and date."
            );
        }

        existingException.updateException(
                requestedException.getExceptionDate(),
                requestedException.getExceptionName(),
                resolveClosed(
                        requestedException.getIsClosed()
                ),
                resolveByAppointment(
                        requestedException.getIsByAppointment()
                ),
                requestedException.getOpeningTime(),
                requestedException.getClosingTime(),
                requestedException.getDisplayText(),
                administrator
        );

        return saveException(existingException);
    }

    @Override
    @Transactional
    public WebsiteBusinessHourException upsertBusinessHourException(
            UUID businessProfileId,
            WebsiteBusinessHourException requestedException,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        validateRequestedException(requestedException);

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile businessProfile =
                getRequiredBusinessProfile(businessProfileId);

        WebsiteBusinessHourException exception =
                websiteBusinessHourExceptionRepository
                        .findByProfileAndDateForUpdate(
                                businessProfileId,
                                requestedException.getExceptionDate()
                        )
                        .orElse(null);

        if (exception == null) {
            exception = WebsiteBusinessHourException.builder()
                    .businessProfile(businessProfile)
                    .exceptionDate(
                            requestedException.getExceptionDate()
                    )
                    .createdByAdminUser(administrator)
                    .updatedByAdminUser(administrator)
                    .build();
        }

        exception.updateException(
                requestedException.getExceptionDate(),
                requestedException.getExceptionName(),
                resolveClosed(
                        requestedException.getIsClosed()
                ),
                resolveByAppointment(
                        requestedException.getIsByAppointment()
                ),
                requestedException.getOpeningTime(),
                requestedException.getClosingTime(),
                requestedException.getDisplayText(),
                administrator
        );

        return saveException(exception);
    }

    @Override
    public WebsiteBusinessHourException getBusinessHourException(
            UUID businessHourExceptionId
    ) {
        requireIdentifier(
                businessHourExceptionId,
                "Business-hour exception ID"
        );

        return websiteBusinessHourExceptionRepository
                .findByBusinessHourExceptionId(
                        businessHourExceptionId
                )
                .orElseThrow(() -> notFound(
                        "Website business-hour exception was not found."
                ));
    }

    @Override
    public WebsiteBusinessHourException
    getBusinessHourExceptionByDate(
            UUID businessProfileId,
            LocalDate exceptionDate
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        requireDate(
                exceptionDate,
                "Exception date"
        );

        return websiteBusinessHourExceptionRepository
                .findByBusinessProfile_BusinessProfileIdAndExceptionDate(
                        businessProfileId,
                        exceptionDate
                )
                .orElseThrow(() -> notFound(
                        "Website business-hour exception was not found "
                                + "for the specified date."
                ));
    }

    @Override
    public Page<WebsiteBusinessHourException>
    getBusinessHourExceptionsByProfile(
            UUID businessProfileId,
            Pageable pageable
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        requirePageable(pageable);
        getRequiredBusinessProfile(businessProfileId);

        return websiteBusinessHourExceptionRepository
                .findAllByBusinessProfile_BusinessProfileId(
                        businessProfileId,
                        pageable
                );
    }

    @Override
    public List<WebsiteBusinessHourException>
    getBusinessHourExceptionsByDateRange(
            UUID businessProfileId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        validateDateRange(startDate, endDate);
        getRequiredBusinessProfile(businessProfileId);

        return websiteBusinessHourExceptionRepository
                .findAllByBusinessProfile_BusinessProfileIdAndExceptionDateBetweenOrderByExceptionDateAsc(
                        businessProfileId,
                        startDate,
                        endDate
                );
    }

    @Override
    public List<WebsiteBusinessHourException>
    getUpcomingBusinessHourExceptions(
            UUID businessProfileId,
            LocalDate startDate
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        LocalDate resolvedStartDate =
                startDate == null
                        ? LocalDate.now()
                        : startDate;

        getRequiredBusinessProfile(businessProfileId);

        return websiteBusinessHourExceptionRepository
                .findAllByBusinessProfile_BusinessProfileIdAndExceptionDateGreaterThanEqualOrderByExceptionDateAsc(
                        businessProfileId,
                        resolvedStartDate
                );
    }

    @Override
    public List<WebsiteBusinessHourException>
    getPublicBusinessHourExceptions(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        if (
                !websiteBusinessProfileRepository
                        .existsByIsActiveTrue()
        ) {
            throw notFound(
                    "An active website business profile was not found."
            );
        }

        return websiteBusinessHourExceptionRepository
                .findAllByBusinessProfile_IsActiveTrueAndExceptionDateBetweenOrderByExceptionDateAsc(
                        startDate,
                        endDate
                );
    }

    @Override
    public WebsiteBusinessHourException
    getPublicBusinessHourExceptionByDate(
            LocalDate exceptionDate
    ) {
        requireDate(
                exceptionDate,
                "Exception date"
        );

        return websiteBusinessHourExceptionRepository
                .findByBusinessProfile_IsActiveTrueAndExceptionDate(
                        exceptionDate
                )
                .orElseThrow(() -> notFound(
                        "No public business-hour exception exists for "
                                + "the specified date."
                ));
    }

    @Override
    @Transactional
    public void deleteBusinessHourException(
            UUID businessHourExceptionId,
            UUID administratorId
    ) {
        requireIdentifier(
                businessHourExceptionId,
                "Business-hour exception ID"
        );

        getRequiredAdministrator(administratorId);

        WebsiteBusinessHourException exception =
                getExceptionForUpdate(
                        businessHourExceptionId
                );

        websiteBusinessHourExceptionRepository.delete(
                exception
        );
    }

    @Override
    @Transactional
    public long deleteBusinessHourExceptionsByProfile(
            UUID businessProfileId,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        getRequiredAdministrator(administratorId);
        getRequiredBusinessProfile(businessProfileId);

        return websiteBusinessHourExceptionRepository
                .deleteAllByBusinessProfile_BusinessProfileId(
                        businessProfileId
                );
    }

    private WebsiteBusinessHourException getExceptionForUpdate(
            UUID businessHourExceptionId
    ) {
        return websiteBusinessHourExceptionRepository
                .findByIdForUpdate(
                        businessHourExceptionId
                )
                .orElseThrow(() -> notFound(
                        "Website business-hour exception was not found."
                ));
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

    private void validateRequestedException(
            WebsiteBusinessHourException exception
    ) {
        if (exception == null) {
            throw badRequest(
                    "Business-hour exception information is required."
            );
        }

        requireDate(
                exception.getExceptionDate(),
                "Exception date"
        );

        if (
                exception.getExceptionName() != null
                        && exception
                        .getExceptionName()
                        .trim()
                        .length() > 180
        ) {
            throw badRequest(
                    "Exception name must not exceed 180 characters."
            );
        }

        if (
                exception.getDisplayText() != null
                        && exception
                        .getDisplayText()
                        .trim()
                        .length() > 180
        ) {
            throw badRequest(
                    "Display text must not exceed 180 characters."
            );
        }

        boolean closed =
                resolveClosed(exception.getIsClosed());

        LocalTime openingTime =
                exception.getOpeningTime();

        LocalTime closingTime =
                exception.getClosingTime();

        if (closed) {
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
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        requireDate(startDate, "Start date");
        requireDate(endDate, "End date");

        if (endDate.isBefore(startDate)) {
            throw badRequest(
                    "End date must not be before start date."
            );
        }
    }

    private boolean resolveClosed(
            Boolean value
    ) {
        return Boolean.TRUE.equals(value);
    }

    private boolean resolveByAppointment(
            Boolean value
    ) {
        return value == null || value;
    }

    private WebsiteBusinessHourException saveException(
            WebsiteBusinessHourException exception
    ) {
        try {
            return websiteBusinessHourExceptionRepository
                    .saveAndFlush(exception);
        } catch (DataIntegrityViolationException dataException) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A business-hour exception already exists for this "
                            + "business profile and date.",
                    dataException
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    private void requireDate(
            LocalDate value,
            String fieldName
    ) {
        if (value == null) {
            throw badRequest(
                    fieldName + " is required."
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

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}