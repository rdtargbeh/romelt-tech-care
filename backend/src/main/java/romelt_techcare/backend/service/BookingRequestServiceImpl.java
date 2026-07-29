package romelt_techcare.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.AdminBookingRequestResponse;
import romelt_techcare.backend.dto.AdminBookingStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.BookingRequestConfirmationResponse;
import romelt_techcare.backend.dto.BookingRequestCreateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ServiceMethod;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.BookingRequestMapper;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.BookingRequestRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Processes public and administrator-created customer bookings.
 *
 * Responsibilities:
 * - Creates public website booking requests.
 * - Creates bookings entered by authenticated administrators.
 * - Applies shared scheduling and service-location rules.
 * - Generates unique customer-facing reference numbers.
 * - Records booking source and creating administrator.
 * - Retrieves booking list and detail records.
 * - Updates booking lifecycle status.
 *
 * Booking lifecycle:
 * PENDING → UNDER_REVIEW → CONFIRMED → COMPLETED
 *
 * Alternative terminal statuses:
 * - CANCELLED
 * - DECLINED
 * - EXPIRED
 *
 * Reference format:
 * RTBR-YYYY-XXXXXXXX
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRequestServiceImpl
        implements BookingRequestService {

    private static final String REFERENCE_PREFIX =
            "RTBR";

    private static final int MAX_REFERENCE_ATTEMPTS =
            10;

    private final BookingRequestRepository
            bookingRequestRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final BookingRequestMapper
            bookingRequestMapper;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Creates a public website booking request.
     */
    @Override
    @Transactional
    public BookingRequestConfirmationResponse createBookingRequest(
            BookingRequestCreateRequest request
    ) {
        requirePublicRequest(request);

        validateBusinessRules(
                request.preferredDate(),
                request.alternateDate(),
                request.serviceMethod(),
                request.streetAddress(),
                request.city(),
                request.state(),
                request.postalCode()
        );

        String referenceNumber =
                generateUniqueReferenceNumber();

        BookingRequest bookingRequest =
                bookingRequestMapper.toEntity(
                        request,
                        referenceNumber
                );

        BookingRequest savedBookingRequest =
                bookingRequestRepository.save(
                        bookingRequest
                );

        log.info(
                "Public booking request created. bookingRequestId={}, referenceNumber={}, source={}, preferredDate={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                savedBookingRequest.getBookingSource(),
                savedBookingRequest.getPreferredDate()
        );

        return bookingRequestMapper
                .toConfirmationResponse(
                        savedBookingRequest
                );
    }

    /**
     * Creates a booking entered by an authenticated administrator.
     */
    @Override
    @Transactional
    public AdminBookingRequestResponse createAdminBookingRequest(
            AdminJwtPrincipal principal,
            AdminBookingRequestCreateRequest request
    ) {
        requirePrincipal(principal);
        requireAdminRequest(request);

        validateAdminBookingSource(
                request.bookingSource()
        );

        validateBusinessRules(
                request.preferredDate(),
                request.alternateDate(),
                request.serviceMethod(),
                request.streetAddress(),
                request.city(),
                request.state(),
                request.postalCode()
        );

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        String administratorName =
                createAdministratorDisplayName(
                        administrator
                );

        String referenceNumber =
                generateUniqueReferenceNumber();

        BookingRequest bookingRequest =
                bookingRequestMapper.toAdminEntity(
                        request,
                        referenceNumber,
                        administrator.getAdminUserId(),
                        administratorName
                );

        BookingRequest savedBookingRequest =
                bookingRequestRepository.save(
                        bookingRequest
                );

        log.info(
                "Administrator booking created. bookingRequestId={}, referenceNumber={}, source={}, createdByAdminUserId={}, preferredDate={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                savedBookingRequest.getBookingSource(),
                savedBookingRequest.getCreatedByAdminUserId(),
                savedBookingRequest.getPreferredDate()
        );

        return AdminBookingRequestResponse.from(
                savedBookingRequest
        );
    }

    /**
     * Returns administrator booking records.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminBookingRequestResponse> getAdminBookingRequests(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Booking pagination information is required."
            );
        }

        return bookingRequestRepository
                .findAll(pageable)
                .map(AdminBookingRequestResponse::from);
    }

    /**
     * Returns one booking by ID.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminBookingRequestResponse getAdminBookingRequest(
            UUID bookingRequestId
    ) {
        BookingRequest bookingRequest =
                findBookingRequest(bookingRequestId);

        return AdminBookingRequestResponse.from(
                bookingRequest
        );
    }

    /**
     * Updates a booking request's operational status.
     */
    @Override
    @Transactional
    public AdminBookingRequestResponse updateAdminBookingStatus(
            AdminJwtPrincipal principal,
            UUID bookingRequestId,
            AdminBookingStatusUpdateRequest request
    ) {
        requirePrincipal(principal);
        requireBookingStatusUpdateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        BookingRequest bookingRequest =
                findBookingRequest(bookingRequestId);

        BookingRequestStatus previousStatus =
                bookingRequest.getStatus();

        BookingRequestStatus requestedStatus =
                request.status();

        bookingRequest.setStatus(requestedStatus);

        String normalizedNotes =
                normalizeOptional(request.adminNotes());

        if (normalizedNotes != null) {
            bookingRequest.setAdminNotes(normalizedNotes);
        }

        BookingRequest savedBookingRequest =
                bookingRequestRepository.save(
                        bookingRequest
                );

        log.info(
                "Booking status updated. bookingRequestId={}, referenceNumber={}, previousStatus={}, newStatus={}, updatedByAdminUserId={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                previousStatus,
                savedBookingRequest.getStatus(),
                administrator.getAdminUserId()
        );

        return AdminBookingRequestResponse.from(
                savedBookingRequest
        );
    }

    /**
     * Loads one booking or raises a not-found response.
     */
    private BookingRequest findBookingRequest(
            UUID bookingRequestId
    ) {
        if (bookingRequestId == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Booking request ID is required."
            );
        }

        return bookingRequestRepository
                .findById(bookingRequestId)
                .orElseThrow(() ->
                        new PublicRequestRejectedException(
                                HttpStatus.NOT_FOUND,
                                "Booking request was not found."
                        )
                );
    }

    /**
     * Loads and validates the currently authenticated administrator.
     */
    private AdminUser findAuthenticatedAdministrator(
            AdminJwtPrincipal principal
    ) {
        AdminUser administrator =
                adminUserRepository
                        .findById(
                                principal.adminUserId()
                        )
                        .orElseThrow(
                                AdminAuthenticationException
                                        ::accountNotFound
                        );

        if (administrator.getAdminUserId() == null
                || !administrator.getAdminUserId()
                .equals(principal.adminUserId())) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (administrator.getEmail() == null
                || principal.email() == null
                || !administrator.getEmail()
                .trim()
                .equalsIgnoreCase(
                        principal.email().trim()
                )) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (administrator.getRole() == null
                || principal.role() == null
                || administrator.getRole()
                != principal.role()) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (!administrator.isActive()) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }

        return administrator;
    }

    /**
     * WEBSITE is reserved for the public booking endpoint.
     */
    private void validateAdminBookingSource(
            BookingSource bookingSource
    ) {
        if (bookingSource == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Booking source is required."
            );
        }

        if (bookingSource == BookingSource.WEBSITE) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "WEBSITE is reserved for customer-submitted public bookings."
            );
        }
    }

    /**
     * Applies business rules shared by public and administrator
     * bookings.
     */
    private void validateBusinessRules(
            LocalDate preferredDate,
            LocalDate alternateDate,
            ServiceMethod serviceMethod,
            String streetAddress,
            String city,
            String state,
            String postalCode
    ) {
        if (preferredDate == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Preferred date is required."
            );
        }

        if (serviceMethod == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Service method is required."
            );
        }

        if (alternateDate != null
                && alternateDate.equals(preferredDate)) {

            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Alternate date must be different from the preferred date."
            );
        }

        if (serviceMethod == ServiceMethod.ON_SITE) {
            validateOnSiteAddress(
                    streetAddress,
                    city,
                    state,
                    postalCode
            );
        }
    }

    private void validateOnSiteAddress(
            String streetAddress,
            String city,
            String state,
            String postalCode
    ) {
        if (isBlank(streetAddress)) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Street address is required for on-site service."
            );
        }

        if (isBlank(city)) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "City is required for on-site service."
            );
        }

        if (isBlank(state)) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "State is required for on-site service."
            );
        }

        if (isBlank(postalCode)) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Postal code is required for on-site service."
            );
        }
    }

    private String generateUniqueReferenceNumber() {
        for (
                int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++
        ) {
            String referenceNumber =
                    createReferenceNumber();

            if (!bookingRequestRepository
                    .existsByReferenceNumber(
                            referenceNumber
                    )) {

                return referenceNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique booking request reference number."
        );
    }

    private String createReferenceNumber() {
        String randomSegment = Long
                .toUnsignedString(
                        secureRandom.nextLong(),
                        36
                )
                .toUpperCase(Locale.ROOT);

        if (randomSegment.length() < 8) {
            randomSegment = String
                    .format(
                            "%8s",
                            randomSegment
                    )
                    .replace(' ', '0');
        }

        randomSegment =
                randomSegment.substring(0, 8);

        return "%s-%d-%s".formatted(
                REFERENCE_PREFIX,
                Year.now().getValue(),
                randomSegment
        );
    }

    private String createAdministratorDisplayName(
            AdminUser administrator
    ) {
        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        String fullName =
                String.join(
                        " ",
                        firstName == null ? "" : firstName,
                        lastName == null ? "" : lastName
                ).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        if (!isBlank(administrator.getEmail())) {
            return administrator.getEmail().trim();
        }

        return "Administrator";
    }

    private void requirePublicRequest(
            BookingRequestCreateRequest request
    ) {
        if (request == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Booking request information is required."
            );
        }
    }

    private void requireAdminRequest(
            AdminBookingRequestCreateRequest request
    ) {
        if (request == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Administrator booking information is required."
            );
        }
    }

    private void requireBookingStatusUpdateRequest(
            AdminBookingStatusUpdateRequest request
    ) {
        if (request == null || request.status() == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Booking status is required."
            );
        }
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (principal == null
                || principal.adminUserId() == null
                || isBlank(principal.email())
                || principal.role() == null) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }
}