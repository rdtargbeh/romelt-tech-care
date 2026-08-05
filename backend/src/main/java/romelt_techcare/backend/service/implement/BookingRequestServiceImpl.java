package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
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
import romelt_techcare.backend.dto.CustomerResolutionResult;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.BookingRequestStatusHistory;
import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.enums.BookingFor;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ServiceMethod;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.BookingRequestMapper;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.BookingRequestRepository;
import romelt_techcare.backend.repository.BookingRequestStatusHistoryRepository;
import romelt_techcare.backend.repository.CustomerRepository;
import romelt_techcare.backend.service.BookingRequestService;
import romelt_techcare.backend.service.CustomerService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Processes public and administrator-created booking requests while
 * preserving the existing booking workflow and integrating reusable
 * customer records.
 *
 * Responsibilities:
 * - Creates public website booking requests.
 * - Creates administrator-entered booking requests.
 * - Resolves or creates customers from booking contact information.
 * - Supports administrator selection of an existing customer.
 * - Links every new booking to its resolved customer.
 * - Preserves booking contact and business snapshots.
 * - Validates personal and business booking requirements.
 * - Validates on-site service-location requirements.
 * - Generates unique customer-facing booking reference numbers.
 * - Retrieves administrator booking lists and booking details.
 * - Updates booking lifecycle status.
 * - Automatically records initial and subsequent status history.
 * - Updates customer booking and service-completion activity.
 * - Records booking audit events.
 *
 * Customer creation paths:
 *
 * PUBLIC BOOKING:
 * - CustomerService searches independently by normalized email and
 *   normalized telephone number.
 * - An existing matching customer is reused.
 * - A new customer is created only when neither value matches.
 * - If email and telephone match different customers, the transaction
 *   is rejected for administrator review.
 *
 * ADMIN BOOKING:
 * - An administrator may select an existing customer.
 * - When no customer is selected, the normal customer-resolution
 *   workflow is used.
 * - A selected customer must match the booking email and telephone
 *   identity when both values are available.
 *
 * Business bookings:
 * - Customer represents the individual contact person.
 * - Business information remains a historical BookingRequest
 *   snapshot.
 *
 * Status-history behavior:
 * - New booking: null → PENDING.
 * - Status update: previousStatus → requestedStatus.
 *
 * Transaction behavior:
 * Customer resolution, booking persistence, status history, customer
 * activity, and audit logging occur inside the same transaction.
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

    private static final String INITIAL_STATUS_REASON =
            "Booking request created.";

    private final BookingRequestRepository
            bookingRequestRepository;

    private final BookingRequestStatusHistoryRepository
            bookingRequestStatusHistoryRepository;

    private final CustomerRepository
            customerRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final BookingRequestMapper
            bookingRequestMapper;

    private final CustomerService
            customerService;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Creates a public booking request.
     *
     * Customer resolution occurs before the booking is persisted.
     * CustomerService assigns bookingRequest.customerId.
     */
    @Override
    @Transactional
    public BookingRequestConfirmationResponse createBookingRequest(
            BookingRequestCreateRequest request
    ) {
        requirePublicRequest(request);

        validateBookingRules(
                request.bookingFor(),
                request.businessName(),
                request.businessEmail(),
                request.businessPhone(),
                request.businessStreetAddress(),
                request.businessCity(),
                request.businessState(),
                request.businessPostalCode(),
                request.preferredDate(),
                request.alternateDate(),
                request.serviceMethod(),
                request.streetAddress(),
                request.city(),
                request.stateRegion(),
                request.postalCode()
        );

        String referenceNumber =
                generateUniqueReferenceNumber();

        BookingRequest bookingRequest =
                bookingRequestMapper.toEntity(
                        request,
                        referenceNumber
                );

        CustomerResolutionResult customerResolution =
                customerService.resolveOrCreateFromBooking(
                        bookingRequest
                );

        requireResolvedCustomer(
                customerResolution.customer(),
                bookingRequest
        );

        BookingRequest savedBookingRequest =
                bookingRequestRepository.saveAndFlush(
                        bookingRequest
                );

        saveInitialStatusHistory(
                savedBookingRequest
        );

        recordCreateAudit(
                null,
                savedBookingRequest,
                customerResolution.created()
                        ? "Public booking request submitted and a new customer profile was created."
                        : "Public booking request submitted and linked to an existing customer."
        );

        log.info(
                "Public booking created. bookingRequestId={}, referenceNumber={}, customerId={}, customerCreated={}, status={}, source={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                savedBookingRequest.getCustomerId(),
                customerResolution.created(),
                savedBookingRequest.getStatus(),
                savedBookingRequest.getBookingSource()
        );

        return bookingRequestMapper
                .toConfirmationResponse(
                        savedBookingRequest
                );
    }

    /**
     * Creates a booking entered by an authenticated administrator.
     *
     * The administrator may select an existing customer or allow the
     * customer-resolution service to resolve or create one.
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

        validateBookingRules(
                request.bookingFor(),
                request.businessName(),
                request.businessEmail(),
                request.businessPhone(),
                request.businessStreetAddress(),
                request.businessCity(),
                request.businessState(),
                request.businessPostalCode(),
                request.preferredDate(),
                request.alternateDate(),
                request.serviceMethod(),
                request.streetAddress(),
                request.city(),
                request.stateRegion(),
                request.postalCode()
        );

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

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

        CustomerResolutionResult customerResolution =
                resolveAdminBookingCustomer(
                        bookingRequest,
                        request.customerId()
                );

        requireResolvedCustomer(
                customerResolution.customer(),
                bookingRequest
        );

        BookingRequest savedBookingRequest =
                bookingRequestRepository.saveAndFlush(
                        bookingRequest
                );

        saveInitialStatusHistory(
                savedBookingRequest,
                administrator.getAdminUserId(),
                administratorName
        );

        recordCreateAudit(
                administrator.getAdminUserId(),
                savedBookingRequest,
                customerResolution.created()
                        ? "Administrator created a booking and a new customer profile."
                        : "Administrator created a booking linked to an existing customer."
        );

        log.info(
                "Administrator booking created. bookingRequestId={}, referenceNumber={}, customerId={}, customerCreated={}, source={}, createdByAdminUserId={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                savedBookingRequest.getCustomerId(),
                customerResolution.created(),
                savedBookingRequest.getBookingSource(),
                administrator.getAdminUserId()
        );

        return AdminBookingRequestResponse.from(
                savedBookingRequest
        );
    }

    /**
     * Returns paginated administrator booking records.
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
                .map(
                        AdminBookingRequestResponse::from
                );
    }

    /**
     * Returns one administrator booking record.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminBookingRequestResponse getAdminBookingRequest(
            UUID bookingRequestId
    ) {
        BookingRequest bookingRequest =
                findBookingRequest(
                        bookingRequestId
                );

        return AdminBookingRequestResponse.from(
                bookingRequest
        );
    }

    /**
     * Updates a booking lifecycle status.
     *
     * A status-history row is created automatically after the booking
     * update succeeds.
     *
     * When a booking becomes COMPLETED, the linked customer's
     * lastServiceCompletedAt and lastActivityAt fields are updated.
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
                findAuthenticatedAdministrator(
                        principal
                );

        BookingRequest bookingRequest =
                findBookingRequest(
                        bookingRequestId
                );

        BookingRequestStatus previousStatus =
                bookingRequest.getStatus();

        BookingRequestStatus requestedStatus =
                request.status();

        validateStatusTransition(
                previousStatus,
                requestedStatus
        );

        /*
         * Capture the actual pre-update state before any booking field
         * is modified.
         */
        JsonNode beforeSnapshot =
                createBookingSnapshot(
                        bookingRequest
                );

        applyStatusUpdateDetails(
                bookingRequest,
                request
        );

        validateStatusSpecificRequirements(
                bookingRequest,
                requestedStatus
        );

        bookingRequest.applyStatus(
                requestedStatus,
                administrator.getAdminUserId()
        );

        BookingRequest savedBookingRequest =
                bookingRequestRepository.saveAndFlush(
                        bookingRequest
                );

        String changeReason =
                resolveStatusChangeReason(
                        request,
                        requestedStatus
                );

        saveStatusHistory(
                savedBookingRequest,
                previousStatus,
                requestedStatus,
                changeReason,
                administrator
        );

        if (
                requestedStatus
                        == BookingRequestStatus.COMPLETED
        ) {
            updateCustomerServiceCompletionActivity(
                    savedBookingRequest
            );
        }

        JsonNode afterSnapshot =
                createBookingSnapshot(
                        savedBookingRequest
                );

        websiteContentAuditLogService.recordAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.BOOKING_REQUEST,
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                beforeSnapshot,
                afterSnapshot,
                "Booking status changed from "
                        + previousStatus
                        + " to "
                        + requestedStatus
                        + ".",
                null
        );

        log.info(
                "Booking status updated. bookingRequestId={}, referenceNumber={}, customerId={}, previousStatus={}, newStatus={}, changedByAdminUserId={}",
                savedBookingRequest.getBookingRequestId(),
                savedBookingRequest.getReferenceNumber(),
                savedBookingRequest.getCustomerId(),
                previousStatus,
                requestedStatus,
                administrator.getAdminUserId()
        );

        return AdminBookingRequestResponse.from(
                savedBookingRequest
        );
    }

    /**
     * Resolves the reusable customer for an administrator-created
     * booking.
     *
     * When no customer ID was selected, normal email/phone resolution
     * is used.
     */
    private CustomerResolutionResult resolveAdminBookingCustomer(
            BookingRequest bookingRequest,
            UUID selectedCustomerId
    ) {
        if (selectedCustomerId == null) {
            return customerService
                    .resolveOrCreateFromBooking(
                            bookingRequest
                    );
        }

        Customer selectedCustomer =
                customerRepository
                        .findActiveRecordById(
                                selectedCustomerId
                        )
                        .orElseThrow(
                                () ->
                                        new PublicRequestRejectedException(
                                                HttpStatus.NOT_FOUND,
                                                "The selected customer was not found."
                                        )
                        );

        if (!selectedCustomer.isUsableCustomer()) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer cannot be used for a new booking."
            );
        }

        validateSelectedCustomerMatchesBooking(
                selectedCustomer,
                bookingRequest
        );

        selectedCustomer.recordBookingActivity(
                effectiveBookingTime(
                        bookingRequest
                )
        );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        selectedCustomer
                );

        bookingRequest.setCustomerId(
                savedCustomer.getCustomerId()
        );

        return new CustomerResolutionResult(
                savedCustomer,
                false
        );
    }

    /**
     * Validates that the selected customer does not conflict with the
     * booking contact-person identity.
     *
     * A missing customer email or phone does not create a conflict.
     * When both values exist, they must match.
     */
    private void validateSelectedCustomerMatchesBooking(
            Customer customer,
            BookingRequest bookingRequest
    ) {
        String customerEmail =
                normalizeOptionalLowercase(
                        customer.getNormalizedEmail()
                );

        String bookingEmail =
                normalizeOptionalLowercase(
                        bookingRequest.getNormalizedEmail()
                );

        String customerPhone =
                normalizeOptional(
                        customer.getNormalizedPhone()
                );

        String bookingPhone =
                normalizeOptional(
                        bookingRequest.getNormalizedPhone()
                );

        if (
                customerEmail != null
                        && bookingEmail != null
                        && !customerEmail.equals(
                        bookingEmail
                )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer's email does not match the booking email."
            );
        }

        if (
                customerPhone != null
                        && bookingPhone != null
                        && !customerPhone.equals(
                        bookingPhone
                )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer's telephone number does not match the booking telephone number."
            );
        }
    }

    /**
     * Ensures customer resolution returned a persisted customer and
     * assigned the same customer ID to the booking.
     */
    private void requireResolvedCustomer(
            Customer customer,
            BookingRequest bookingRequest
    ) {
        if (
                customer == null
                        || customer.getCustomerId() == null
        ) {
            throw new IllegalStateException(
                    "Customer resolution did not return a persisted customer."
            );
        }

        if (
                bookingRequest == null
                        || bookingRequest.getCustomerId() == null
                        || !bookingRequest
                        .getCustomerId()
                        .equals(
                                customer.getCustomerId()
                        )
        ) {
            throw new IllegalStateException(
                    "Booking request was not linked to the resolved customer."
            );
        }
    }

    /**
     * Updates the linked customer when service work is completed.
     */
    private void updateCustomerServiceCompletionActivity(
            BookingRequest bookingRequest
    ) {
        UUID customerId =
                bookingRequest.getCustomerId();

        if (customerId == null) {
            log.warn(
                    "Completed booking has no customer link. bookingRequestId={}, referenceNumber={}",
                    bookingRequest.getBookingRequestId(),
                    bookingRequest.getReferenceNumber()
            );

            return;
        }

        Customer customer =
                customerRepository
                        .findByCustomerIdAndDeletedAtIsNull(
                                customerId
                        )
                        .orElse(null);

        if (customer == null) {
            log.warn(
                    "Linked customer was not found for completed booking. bookingRequestId={}, customerId={}",
                    bookingRequest.getBookingRequestId(),
                    customerId
            );

            return;
        }

        customer.recordServiceCompletion(
                bookingRequest.getCompletedAt()
        );

        customerRepository.saveAndFlush(
                customer
        );
    }

    /**
     * Applies status-specific form fields before changing the booking
     * status.
     */
    private void applyStatusUpdateDetails(
            BookingRequest bookingRequest,
            AdminBookingStatusUpdateRequest request
    ) {
        if (request.assignedAdminUserId() != null) {
            bookingRequest.setAssignedAdminUserId(
                    request.assignedAdminUserId()
            );
        }

        if (
                request.scheduledStartAt() != null
                        || request.scheduledEndAt() != null
                        || normalizeOptional(
                        request.scheduledTimezone()
                ) != null
        ) {
            bookingRequest.setScheduledStartAt(
                    request.scheduledStartAt()
            );

            bookingRequest.setScheduledEndAt(
                    request.scheduledEndAt()
            );

            bookingRequest.setScheduledTimezone(
                    normalizeOptional(
                            request.scheduledTimezone()
                    )
            );
        }

        String cancellationReason =
                normalizeOptional(
                        request.cancellationReason()
                );

        if (cancellationReason != null) {
            bookingRequest.setCancellationReason(
                    cancellationReason
            );
        }

        String declineReason =
                normalizeOptional(
                        request.declineReason()
                );

        if (declineReason != null) {
            bookingRequest.setDeclineReason(
                    declineReason
            );
        }

        String expirationReason =
                normalizeOptional(
                        request.expirationReason()
                );

        if (expirationReason != null) {
            bookingRequest.setExpirationReason(
                    expirationReason
            );
        }

        String completionSummary =
                normalizeOptional(
                        request.completionSummary()
                );

        if (completionSummary != null) {
            bookingRequest.setCompletionSummary(
                    completionSummary
            );
        }

        String completionNotes =
                normalizeOptional(
                        request.completionNotes()
                );

        if (completionNotes != null) {
            bookingRequest.setCompletionNotes(
                    completionNotes
            );
        }

        if (request.reviewEligible() != null) {
            bookingRequest.setReviewEligible(
                    request.reviewEligible()
            );
        }

        String reviewEligibilityNotes =
                normalizeOptional(
                        request.reviewEligibilityNotes()
                );

        if (reviewEligibilityNotes != null) {
            bookingRequest.setReviewEligibilityNotes(
                    reviewEligibilityNotes
            );
        }

        String adminNotes =
                normalizeOptional(
                        request.adminNotes()
                );

        if (adminNotes != null) {
            bookingRequest.setAdminNotes(
                    adminNotes
            );
        }
    }

    /**
     * Validates fields required by the requested lifecycle status.
     */
    private void validateStatusSpecificRequirements(
            BookingRequest bookingRequest,
            BookingRequestStatus requestedStatus
    ) {
        switch (requestedStatus) {
            case PENDING,
                 UNDER_REVIEW -> {
                // No additional lifecycle fields are required.
            }

            case CONFIRMED ->
                    validateConfirmedBooking(
                            bookingRequest
                    );

            case COMPLETED -> {
                validateConfirmedBooking(
                        bookingRequest
                );

                if (
                        isBlank(
                                bookingRequest
                                        .getCompletionSummary()
                        )
                ) {
                    reject(
                            HttpStatus.BAD_REQUEST,
                            "Completion summary is required when completing a booking."
                    );
                }
            }

            case CANCELLED -> {
                if (
                        isBlank(
                                bookingRequest
                                        .getCancellationReason()
                        )
                ) {
                    reject(
                            HttpStatus.BAD_REQUEST,
                            "Cancellation reason is required when cancelling a booking."
                    );
                }
            }

            case DECLINED -> {
                if (
                        isBlank(
                                bookingRequest
                                        .getDeclineReason()
                        )
                ) {
                    reject(
                            HttpStatus.BAD_REQUEST,
                            "Decline reason is required when declining a booking."
                    );
                }
            }

            case EXPIRED -> {
                if (
                        isBlank(
                                bookingRequest
                                        .getExpirationReason()
                        )
                ) {
                    reject(
                            HttpStatus.BAD_REQUEST,
                            "Expiration reason is required when expiring a booking."
                    );
                }
            }
        }

        if (
                !bookingRequest.isReviewEligible()
                        && isBlank(
                        bookingRequest
                                .getReviewEligibilityNotes()
                )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Review eligibility notes are required when the booking is not review eligible."
            );
        }
    }

    /**
     * Validates the confirmed appointment window.
     */
    private void validateConfirmedBooking(
            BookingRequest bookingRequest
    ) {
        Instant scheduledStartAt =
                bookingRequest.getScheduledStartAt();

        Instant scheduledEndAt =
                bookingRequest.getScheduledEndAt();

        String scheduledTimezone =
                normalizeOptional(
                        bookingRequest.getScheduledTimezone()
                );

        if (scheduledStartAt == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled start time is required when confirming a booking."
            );
        }

        if (scheduledEndAt == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled end time is required when confirming a booking."
            );
        }

        if (
                !scheduledEndAt.isAfter(
                        scheduledStartAt
                )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled end time must be after the start time."
            );
        }

        if (scheduledTimezone == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled timezone is required when confirming a booking."
            );
        }
    }

    /**
     * Enforces supported lifecycle transitions.
     */
    private void validateStatusTransition(
            BookingRequestStatus currentStatus,
            BookingRequestStatus requestedStatus
    ) {
        if (currentStatus == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "The booking does not have a current status."
            );
        }

        if (requestedStatus == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking status is required."
            );
        }

        if (currentStatus == requestedStatus) {
            reject(
                    HttpStatus.CONFLICT,
                    "The booking already has the requested status."
            );
        }

        boolean allowed =
                switch (currentStatus) {
                    case PENDING ->
                            requestedStatus
                                    == BookingRequestStatus.UNDER_REVIEW
                                    || requestedStatus
                                    == BookingRequestStatus.CONFIRMED
                                    || requestedStatus
                                    == BookingRequestStatus.CANCELLED
                                    || requestedStatus
                                    == BookingRequestStatus.DECLINED
                                    || requestedStatus
                                    == BookingRequestStatus.EXPIRED;

                    case UNDER_REVIEW ->
                            requestedStatus
                                    == BookingRequestStatus.CONFIRMED
                                    || requestedStatus
                                    == BookingRequestStatus.CANCELLED
                                    || requestedStatus
                                    == BookingRequestStatus.DECLINED
                                    || requestedStatus
                                    == BookingRequestStatus.EXPIRED;

                    case CONFIRMED ->
                            requestedStatus
                                    == BookingRequestStatus.COMPLETED
                                    || requestedStatus
                                    == BookingRequestStatus.CANCELLED;

                    case COMPLETED,
                         CANCELLED,
                         DECLINED,
                         EXPIRED -> false;
                };

        if (!allowed) {
            reject(
                    HttpStatus.CONFLICT,
                    "Booking status cannot change from "
                            + currentStatus
                            + " to "
                            + requestedStatus
                            + "."
            );
        }
    }

    /**
     * Validates public and administrator booking data.
     */
    private void validateBookingRules(
            BookingFor bookingFor,
            String businessName,
            String businessEmail,
            String businessPhone,
            String businessStreetAddress,
            String businessCity,
            String businessState,
            String businessPostalCode,
            LocalDate preferredDate,
            LocalDate alternateDate,
            ServiceMethod serviceMethod,
            String streetAddress,
            String city,
            String state,
            String postalCode
    ) {
        if (bookingFor == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Select whether the service is personal or business."
            );
        }

        if (bookingFor == BookingFor.BUSINESS) {
            requireBookingValue(
                    businessName,
                    "Business name is required."
            );

            requireBookingValue(
                    businessEmail,
                    "Business email is required."
            );

            requireBookingValue(
                    businessPhone,
                    "Business telephone number is required."
            );

            requireBookingValue(
                    businessStreetAddress,
                    "Business street address is required."
            );

            requireBookingValue(
                    businessCity,
                    "Business city is required."
            );

            requireBookingValue(
                    businessState,
                    "Business state is required."
            );

            requireBookingValue(
                    businessPostalCode,
                    "Business postal code is required."
            );
        } else {
            validatePersonalBookingHasNoBusinessDetails(
                    businessName,
                    businessEmail,
                    businessPhone,
                    businessStreetAddress,
                    businessCity,
                    businessState,
                    businessPostalCode
            );
        }

        if (preferredDate == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Preferred date is required."
            );
        }

        if (serviceMethod == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Service method is required."
            );
        }

        if (
                alternateDate != null
                        && alternateDate.equals(
                        preferredDate
                )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Alternate date must be different from the preferred date."
            );
        }

        if (serviceMethod == ServiceMethod.ON_SITE) {
            requireBookingValue(
                    streetAddress,
                    "Street address is required for on-site service."
            );

            requireBookingValue(
                    city,
                    "City is required for on-site service."
            );

            requireBookingValue(
                    state,
                    "State is required for on-site service."
            );

            requireBookingValue(
                    postalCode,
                    "Postal code is required for on-site service."
            );
        }
    }

    /**
     * Prevents business data from being stored on a personal booking.
     */
    private void validatePersonalBookingHasNoBusinessDetails(
            String businessName,
            String businessEmail,
            String businessPhone,
            String businessStreetAddress,
            String businessCity,
            String businessState,
            String businessPostalCode
    ) {
        if (
                !isBlank(businessName)
                        || !isBlank(businessEmail)
                        || !isBlank(businessPhone)
                        || !isBlank(businessStreetAddress)
                        || !isBlank(businessCity)
                        || !isBlank(businessState)
                        || !isBlank(businessPostalCode)
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Business information must be empty for a personal booking."
            );
        }
    }

    /**
     * Saves initial public booking status history.
     */
    private void saveInitialStatusHistory(
            BookingRequest bookingRequest
    ) {
        saveInitialStatusHistory(
                bookingRequest,
                null,
                null
        );
    }

    /**
     * Saves the initial null-to-PENDING status transition.
     */
    private void saveInitialStatusHistory(
            BookingRequest bookingRequest,
            UUID administratorId,
            String administratorName
    ) {
        if (
                bookingRequest == null
                        || bookingRequest.getBookingRequestId() == null
                        || bookingRequest.getStatus() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request with a status is required before creating status history."
            );
        }

        BookingRequestStatusHistory history =
                BookingRequestStatusHistory.builder()
                        .bookingRequestId(
                                bookingRequest
                                        .getBookingRequestId()
                        )
                        .previousStatus(null)
                        .newStatus(
                                bookingRequest.getStatus()
                        )
                        .changeReason(
                                INITIAL_STATUS_REASON
                        )
                        .changedByAdminUserId(
                                administratorId
                        )
                        .changedByAdminName(
                                normalizeOptional(
                                        administratorName
                                )
                        )
                        .notificationEventId(null)
                        .build();

        BookingRequestStatusHistory savedHistory =
                bookingRequestStatusHistoryRepository
                        .saveAndFlush(
                                history
                        );

        log.debug(
                "Initial booking status history created. bookingStatusHistoryId={}, bookingRequestId={}, newStatus={}",
                savedHistory.getBookingStatusHistoryId(),
                savedHistory.getBookingRequestId(),
                savedHistory.getNewStatus()
        );
    }

    /**
     * Saves one immutable status-history record.
     */
    private void saveStatusHistory(
            BookingRequest bookingRequest,
            BookingRequestStatus previousStatus,
            BookingRequestStatus newStatus,
            String changeReason,
            AdminUser administrator
    ) {
        if (
                bookingRequest == null
                        || bookingRequest.getBookingRequestId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request is required before creating status history."
            );
        }

        if (previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous booking status is required."
            );
        }

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New booking status is required."
            );
        }

        if (previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Previous and new booking statuses must be different."
            );
        }

        if (
                administrator == null
                        || administrator.getAdminUserId() == null
        ) {
            throw new IllegalArgumentException(
                    "Administrator information is required for a booking status change."
            );
        }

        BookingRequestStatusHistory history =
                BookingRequestStatusHistory.builder()
                        .bookingRequestId(
                                bookingRequest
                                        .getBookingRequestId()
                        )
                        .previousStatus(
                                previousStatus
                        )
                        .newStatus(
                                newStatus
                        )
                        .changeReason(
                                normalizeOptional(
                                        changeReason
                                )
                        )
                        .changedByAdminUserId(
                                administrator.getAdminUserId()
                        )
                        .changedByAdminName(
                                createAdministratorDisplayName(
                                        administrator
                                )
                        )
                        .notificationEventId(null)
                        .build();

        BookingRequestStatusHistory savedHistory =
                bookingRequestStatusHistoryRepository
                        .saveAndFlush(
                                history
                        );

        log.debug(
                "Booking status history created. bookingStatusHistoryId={}, bookingRequestId={}, previousStatus={}, newStatus={}, changedByAdminUserId={}",
                savedHistory.getBookingStatusHistoryId(),
                savedHistory.getBookingRequestId(),
                savedHistory.getPreviousStatus(),
                savedHistory.getNewStatus(),
                savedHistory.getChangedByAdminUserId()
        );
    }

    /**
     * Resolves a readable status-history reason.
     */
    private String resolveStatusChangeReason(
            AdminBookingStatusUpdateRequest request,
            BookingRequestStatus requestedStatus
    ) {
        String explicitReason =
                normalizeOptional(
                        request.changeReason()
                );

        if (explicitReason != null) {
            return explicitReason;
        }

        return switch (requestedStatus) {
            case CANCELLED ->
                    normalizeOptional(
                            request.cancellationReason()
                    );

            case DECLINED ->
                    normalizeOptional(
                            request.declineReason()
                    );

            case EXPIRED ->
                    normalizeOptional(
                            request.expirationReason()
                    );

            case COMPLETED ->
                    normalizeOptional(
                            request.completionSummary()
                    );

            case CONFIRMED ->
                    "Booking appointment confirmed.";

            case UNDER_REVIEW ->
                    "Booking request moved under review.";

            case PENDING ->
                    "Booking request returned to pending status.";
        };
    }

    /**
     * Records booking creation in the audit log.
     */
    private void recordCreateAudit(
            UUID administratorId,
            BookingRequest bookingRequest,
            String summary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.BOOKING_REQUEST,
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                null,
                createBookingSnapshot(
                        bookingRequest
                ),
                summary,
                null
        );
    }

    /**
     * Creates a non-sensitive booking audit snapshot.
     */
    private JsonNode createBookingSnapshot(
            BookingRequest bookingRequest
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "bookingRequestId",
                bookingRequest.getBookingRequestId()
        );

        fields.put(
                "customerId",
                bookingRequest.getCustomerId()
        );

        fields.put(
                "referenceNumber",
                bookingRequest.getReferenceNumber()
        );

        fields.put(
                "bookingFor",
                bookingRequest.getBookingFor()
        );

        fields.put(
                "businessName",
                bookingRequest.getBusinessName()
        );

        fields.put(
                "status",
                bookingRequest.getStatus()
        );

        fields.put(
                "bookingSource",
                bookingRequest.getBookingSource()
        );

        fields.put(
                "serviceId",
                bookingRequest.getServiceId()
        );

        fields.put(
                "serviceType",
                bookingRequest.getServiceType()
        );

        fields.put(
                "serviceMethod",
                bookingRequest.getServiceMethod()
        );

        fields.put(
                "preferredDate",
                bookingRequest.getPreferredDate()
        );

        fields.put(
                "alternateDate",
                bookingRequest.getAlternateDate()
        );

        fields.put(
                "scheduledStartAt",
                bookingRequest.getScheduledStartAt()
        );

        fields.put(
                "scheduledEndAt",
                bookingRequest.getScheduledEndAt()
        );

        fields.put(
                "scheduledTimezone",
                bookingRequest.getScheduledTimezone()
        );

        fields.put(
                "confirmedAt",
                bookingRequest.getConfirmedAt()
        );

        fields.put(
                "completedAt",
                bookingRequest.getCompletedAt()
        );

        fields.put(
                "cancelledAt",
                bookingRequest.getCancelledAt()
        );

        fields.put(
                "declinedAt",
                bookingRequest.getDeclinedAt()
        );

        fields.put(
                "expiredAt",
                bookingRequest.getExpiredAt()
        );

        fields.put(
                "assignedAdminUserId",
                bookingRequest.getAssignedAdminUserId()
        );

        fields.put(
                "updatedByAdminUserId",
                bookingRequest.getUpdatedByAdminUserId()
        );

        fields.put(
                "reviewEligible",
                bookingRequest.isReviewEligible()
        );

        fields.put(
                "rowVersion",
                bookingRequest.getRowVersion()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(
                        fields
                );
    }

    /**
     * Finds one booking request.
     */
    private BookingRequest findBookingRequest(
            UUID bookingRequestId
    ) {
        if (bookingRequestId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking request ID is required."
            );
        }

        return bookingRequestRepository
                .findById(
                        bookingRequestId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Booking request was not found."
                                )
                );
    }

    /**
     * Finds and validates the authenticated administrator.
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

        if (
                administrator.getAdminUserId() == null
                        || !administrator
                        .getAdminUserId()
                        .equals(
                                principal.adminUserId()
                        )
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getEmail() == null
                        || principal.email() == null
                        || !administrator
                        .getEmail()
                        .trim()
                        .equalsIgnoreCase(
                                principal.email().trim()
                        )
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getRole() == null
                        || principal.role() == null
                        || administrator.getRole()
                        != principal.role()
        ) {
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
     * Prevents administrator-created bookings from using the public
     * WEBSITE source.
     */
    private void validateAdminBookingSource(
            BookingSource bookingSource
    ) {
        if (bookingSource == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking source is required."
            );
        }

        if (bookingSource == BookingSource.WEBSITE) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "WEBSITE is reserved for public customer submissions."
            );
        }
    }

    /**
     * Returns the timestamp used for customer booking activity.
     */
    private Instant effectiveBookingTime(
            BookingRequest bookingRequest
    ) {
        if (bookingRequest.getSubmittedAt() != null) {
            return bookingRequest.getSubmittedAt();
        }

        if (bookingRequest.getCreatedAt() != null) {
            return bookingRequest.getCreatedAt();
        }

        return Instant.now();
    }

    /**
     * Generates a unique customer-facing booking reference.
     */
    private String generateUniqueReferenceNumber() {
        for (
                int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++
        ) {
            String referenceNumber =
                    createReferenceNumber();

            if (
                    !bookingRequestRepository
                            .existsByReferenceNumber(
                                    referenceNumber
                            )
            ) {
                return referenceNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique booking reference number."
        );
    }

    /**
     * Creates one candidate booking reference.
     */
    private String createReferenceNumber() {
        String randomSegment =
                Long.toUnsignedString(
                                secureRandom.nextLong(),
                                36
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (randomSegment.length() < 8) {
            randomSegment =
                    String.format(
                                    "%8s",
                                    randomSegment
                            )
                            .replace(
                                    ' ',
                                    '0'
                            );
        }

        randomSegment =
                randomSegment.substring(
                        0,
                        8
                );

        return "%s-%d-%s".formatted(
                REFERENCE_PREFIX,
                Year.now().getValue(),
                randomSegment
        );
    }

    /**
     * Creates a readable administrator-name snapshot.
     */
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
                                firstName == null
                                        ? ""
                                        : firstName,
                                lastName == null
                                        ? ""
                                        : lastName
                        )
                        .trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        if (
                !isBlank(
                        administrator.getEmail()
                )
        ) {
            return administrator
                    .getEmail()
                    .trim();
        }

        return "Administrator";
    }

    private void requirePublicRequest(
            BookingRequestCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking request information is required."
            );
        }
    }

    private void requireAdminRequest(
            AdminBookingRequestCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Administrator booking information is required."
            );
        }
    }

    private void requireBookingStatusUpdateRequest(
            AdminBookingStatusUpdateRequest request
    ) {
        if (
                request == null
                        || request.status() == null
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking status is required."
            );
        }
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || isBlank(
                        principal.email()
                )
                        || principal.role() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    private void requireBookingValue(
            String value,
            String message
    ) {
        if (isBlank(value)) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private void reject(
            HttpStatus status,
            String message
    ) {
        throw new PublicRequestRejectedException(
                status,
                message
        );
    }

    private String normalizeOptionalLowercase(
            String value
    ) {
        String normalizedValue =
                normalizeOptional(
                        value
                );

        return normalizedValue == null
                ? null
                : normalizedValue.toLowerCase(
                Locale.ROOT
        );
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue =
                value.trim();

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