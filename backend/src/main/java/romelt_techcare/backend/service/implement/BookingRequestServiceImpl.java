package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.AdminBookingRequestResponse;
import romelt_techcare.backend.dto.AdminBookingStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.BookingCustomerPrefillResponse;
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
import romelt_techcare.backend.event.BookingStatusChangedEvent;
import romelt_techcare.backend.event.BookingSubmittedEvent;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.BookingRequestMapper;
import romelt_techcare.backend.mapper.CustomerMapper;
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
 * Processes public and administrator-created booking requests,
 * integrates reusable customer records, records booking history,
 * creates audit records, and publishes after-commit booking
 * notification events.
 *
 * Responsibilities:
 * - Creates public website booking requests.
 * - Creates administrator-entered booking requests.
 * - Returns existing-customer information for Create Booking prefill.
 * - Resolves or creates reusable customer records.
 * - Supports explicit administrator selection of an existing Customer.
 * - Links each booking to its customer.
 * - Preserves contact-person and business snapshots.
 * - Preserves the actual booking service-location snapshot.
 * - Validates personal and business booking requirements.
 * - Validates on-site service-location requirements.
 * - Generates unique customer-facing references.
 * - Retrieves administrator booking lists and details.
 * - Updates booking lifecycle status.
 * - Creates immutable status-history records.
 * - Updates customer booking and completion activity.
 * - Records booking audit events.
 * - Publishes booking events for after-commit customer and
 *   administrator notifications.
 *
 * Existing-customer administrator workflow:
 *
 * Administrator searches/selects Customer
 *      -> customerId
 *          -> getBookingCustomerPrefill(customerId)
 *              -> frontend fills reusable customer information
 *                  -> administrator enters booking-specific details
 *                      -> createAdminBookingRequest(...)
 *
 * Existing-customer authority rule:
 *
 * If request.customerId() is supplied, the reusable Customer record
 * is authoritative for:
 *
 * - customerId;
 * - contact-person display name;
 * - primary email;
 * - primary telephone number;
 * - preferred contact method, when configured.
 *
 * Service-location fields are NOT force-replaced from Customer because
 * a repeat customer may request a service at another location.
 *
 * Business fields are NOT derived from Customer because Customer
 * represents the reusable individual contact person while business
 * information is intentionally a BookingRequest historical snapshot.
 *
 * Notification behavior:
 *
 * BOOKING CREATION:
 * - Customer EMAIL acknowledgment when email exists.
 * - Customer SMS acknowledgment when phone exists.
 * - Administrator IN_APP notification for active administrators.
 *
 * STATUS CHANGE:
 * - Customer EMAIL status update when email exists.
 * - Customer SMS status update when phone exists.
 * - Administrator IN_APP notification for active administrators.
 *
 * Notification sending:
 * This service publishes Spring application events inside the active
 * booking transaction. BookingNotificationListener processes them only
 * after the transaction commits successfully.
 *
 * Status-history behavior:
 * - New booking: null -> PENDING.
 * - Status update: previousStatus -> requestedStatus.
 * - The legacy notificationEventId remains null in the simplified
 *   notification architecture.
 *
 * Transaction behavior:
 * Customer resolution, booking persistence, status history, customer
 * activity, audit logging, and application-event publication occur in
 * the same transaction. External email and SMS delivery occurs after
 * commit and cannot roll back a valid booking.
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

    private final CustomerMapper
            customerMapper;

    private final CustomerService
            customerService;

    private final ApplicationEventPublisher
            applicationEventPublisher;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    // =================================================================
    // PUBLIC BOOKING CREATE
    // =================================================================

    /**
     * Creates a public website booking.
     *
     * The complete transaction:
     * 1. Validates the booking.
     * 2. Resolves or creates the customer.
     * 3. Saves the booking.
     * 4. Saves initial status history.
     * 5. Records an audit entry.
     * 6. Publishes an after-commit booking notification event.
     */
    @Override
    @Transactional
    public BookingRequestConfirmationResponse createBookingRequest(
            BookingRequestCreateRequest request
    ) {
        requirePublicRequest(
                request
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
                savedBookingRequest,
                null,
                null
        );

        recordCreateAudit(
                null,
                savedBookingRequest,
                customerResolution.created()
                        ? "Public booking request submitted and a new customer profile was created."
                        : "Public booking request submitted and linked to an existing customer."
        );

        applicationEventPublisher.publishEvent(
                new BookingSubmittedEvent(
                        savedBookingRequest
                )
        );

        log.info(
                "Public booking created and after-commit notification event published. bookingRequestId={}, referenceNumber={}, customerId={}, customerCreated={}, status={}, source={}",
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

    // =================================================================
    // ADMIN CUSTOMER PREFILL
    // =================================================================

    /**
     * Returns current reusable Customer information needed by the
     * administrator Create Booking form.
     *
     * The customer must:
     * - exist;
     * - be active/non-deleted according to repository rules;
     * - not be merged;
     * - be usable for new booking activity.
     *
     * No Customer or BookingRequest is modified.
     */
    @Override
    @Transactional(readOnly = true)
    public BookingCustomerPrefillResponse getBookingCustomerPrefill(
            UUID customerId
    ) {
        Customer customer =
                findUsableBookingCustomer(
                        customerId
                );

        return BookingCustomerPrefillResponse.from(
                customer
        );
    }

    // =================================================================
    // ADMIN BOOKING CREATE
    // =================================================================

    /**
     * Creates a booking entered by an authenticated administrator.
     *
     * Existing Customer:
     * If request.customerId() is supplied, the selected Customer is
     * loaded and its current reusable contact identity is applied to
     * the booking snapshot before persistence.
     *
     * New/Unselected Customer:
     * If request.customerId() is null, the existing CustomerService
     * resolution workflow may resolve an existing Customer from the
     * supplied booking contact data or create a new Customer.
     */
    @Override
    @Transactional
    public AdminBookingRequestResponse createAdminBookingRequest(
            AdminJwtPrincipal principal,
            AdminBookingRequestCreateRequest request
    ) {
        requirePrincipal(
                principal
        );

        requireAdminRequest(
                request
        );

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
                        ? "Administrator created a booking, created a new customer profile, and queued notification events."
                        : "Administrator created a booking linked to an existing customer and queued notification events."
        );

        applicationEventPublisher.publishEvent(
                new BookingSubmittedEvent(
                        savedBookingRequest
                )
        );

        log.info(
                "Administrator booking created and after-commit notification event published. bookingRequestId={}, referenceNumber={}, customerId={}, customerCreated={}, source={}, createdByAdminUserId={}",
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

    // =================================================================
    // ADMIN BOOKING LIST
    // =================================================================

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
                .findAll(
                        pageable
                )
                .map(
                        AdminBookingRequestResponse::from
                );
    }

    // =================================================================
    // ADMIN BOOKING GET ONE
    // =================================================================

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

    // =================================================================
    // ADMIN BOOKING STATUS
    // =================================================================

    /**
     * Updates a booking lifecycle status.
     *
     * The complete transaction:
     * 1. Validates the administrator and status transition.
     * 2. Applies lifecycle details.
     * 3. Saves the booking status.
     * 4. Saves status history.
     * 5. Updates customer completion activity where applicable.
     * 6. Records the audit event.
     * 7. Publishes an after-commit booking notification event.
     */
    @Override
    @Transactional
    public AdminBookingRequestResponse updateAdminBookingStatus(
            AdminJwtPrincipal principal,
            UUID bookingRequestId,
            AdminBookingStatusUpdateRequest request
    ) {
        requirePrincipal(
                principal
        );

        requireBookingStatusUpdateRequest(
                request
        );

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

        String historyChangeReason =
                resolveStatusChangeReason(
                        request,
                        requestedStatus
                );

        saveStatusHistory(
                savedBookingRequest,
                previousStatus,
                requestedStatus,
                historyChangeReason,
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

        applicationEventPublisher.publishEvent(
                new BookingStatusChangedEvent(
                        savedBookingRequest,
                        previousStatus
                )
        );

        log.info(
                "Booking status updated and after-commit notification event published. bookingRequestId={}, referenceNumber={}, customerId={}, previousStatus={}, newStatus={}, changedByAdminUserId={}",
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

    // =================================================================
    // ADMIN CUSTOMER RESOLUTION
    // =================================================================

    /**
     * Resolves the reusable customer for an administrator-created
     * booking.
     *
     * If no customerId is selected, the existing customer-resolution
     * workflow may match or create a reusable customer from the booking
     * snapshot.
     *
     * If customerId is selected, that Customer becomes authoritative
     * for reusable contact identity.
     *
     * The booking retains its service-location and business snapshots
     * because those can legitimately differ from the Customer profile.
     */
    private CustomerResolutionResult resolveAdminBookingCustomer(
            BookingRequest bookingRequest,
            UUID selectedCustomerId
    ) {
        if (selectedCustomerId == null) {
            return customerService.resolveOrCreateFromBooking(
                    bookingRequest
            );
        }

        Customer selectedCustomer =
                findUsableBookingCustomer(
                        selectedCustomerId
                );

        applySelectedCustomerContactSnapshot(
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
     * Loads one Customer that may be selected for a new booking.
     */
    private Customer findUsableBookingCustomer(
            UUID customerId
    ) {
        if (customerId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID is required."
            );
        }

        Customer customer =
                customerRepository
                        .findActiveRecordById(
                                customerId
                        )
                        .orElseThrow(
                                () ->
                                        new PublicRequestRejectedException(
                                                HttpStatus.NOT_FOUND,
                                                "The selected customer was not found."
                                        )
                        );

        if (!customer.isUsableCustomer()) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer cannot be used for a new booking."
            );
        }

        return customer;
    }

    /**
     * Copies authoritative reusable contact identity from the selected
     * Customer into the BookingRequest snapshot.
     *
     * Customer-sourced:
     * - customerId
     * - fullName/displayName
     * - primary email
     * - normalized email
     * - primary phone
     * - normalized phone
     * - preferred contact method when configured
     *
     * Intentionally not overwritten:
     * - notification destinations;
     * - business information;
     * - service location;
     * - service information;
     * - schedule;
     * - problem/device information;
     * - administrator notes.
     *
     * A customer's saved address is a useful frontend default, but
     * BookingRequest.streetAddress/... represent where this particular
     * service will occur and therefore remain booking-specific.
     */
    private void applySelectedCustomerContactSnapshot(
            Customer customer,
            BookingRequest bookingRequest
    ) {
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer is required."
            );
        }

        if (bookingRequest == null) {
            throw new IllegalArgumentException(
                    "Booking request is required."
            );
        }

        String customerName =
                normalizeOptional(
                        customer.getDisplayName()
                );

        String customerEmail =
                normalizeOptional(
                        customer.getPrimaryEmail()
                );

        String customerPhone =
                normalizeOptional(
                        customer.getPrimaryPhone()
                );

        if (customerName == null) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer does not have a usable display name."
            );
        }

        if (customerEmail == null) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer does not have a usable email address."
            );
        }

        if (customerPhone == null) {
            reject(
                    HttpStatus.CONFLICT,
                    "The selected customer does not have a usable telephone number."
            );
        }

        bookingRequest.setFullName(
                customerName
        );

        bookingRequest.setEmail(
                customerEmail
        );

        bookingRequest.setNormalizedEmail(
                customerMapper.normalizeOptionalEmail(
                        customerEmail
                )
        );

        bookingRequest.setPhone(
                customerPhone
        );

        bookingRequest.setNormalizedPhone(
                customerMapper.normalizeOptionalPhone(
                        customerPhone
                )
        );

        if (
                customer.getPreferredContactMethod()
                        != null
        ) {
            bookingRequest.setPreferredContactMethod(
                    customer.getPreferredContactMethod()
            );
        }

        bookingRequest.setCustomerId(
                customer.getCustomerId()
        );
    }

    // =================================================================
    // CUSTOMER LINK VALIDATION
    // =================================================================

    /**
     * Ensures that customer resolution linked the persisted customer
     * to the booking.
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

    // =================================================================
    // CUSTOMER COMPLETION ACTIVITY
    // =================================================================

    /**
     * Updates the linked customer's service-completion activity.
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

    // =================================================================
    // STATUS UPDATE DETAILS
    // =================================================================

    /**
     * Applies status-specific form fields.
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

    // =================================================================
    // STATUS-SPECIFIC VALIDATION
    // =================================================================

    /**
     * Validates fields required for the requested status.
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

    // =================================================================
    // CONFIRMED BOOKING VALIDATION
    // =================================================================

    /**
     * Validates confirmed appointment scheduling.
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

        if (!scheduledEndAt.isAfter(
                scheduledStartAt
        )) {
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

    // =================================================================
    // STATUS TRANSITIONS
    // =================================================================

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

    // =================================================================
    // BOOKING BUSINESS VALIDATION
    // =================================================================

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
     * Prevents business data on personal bookings.
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
                !isBlank(
                        businessName
                )
                        || !isBlank(
                        businessEmail
                )
                        || !isBlank(
                        businessPhone
                )
                        || !isBlank(
                        businessStreetAddress
                )
                        || !isBlank(
                        businessCity
                )
                        || !isBlank(
                        businessState
                )
                        || !isBlank(
                        businessPostalCode
                )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Business information must be empty for a personal booking."
            );
        }
    }

    // =================================================================
    // INITIAL STATUS HISTORY
    // =================================================================

    /**
     * Saves the initial null-to-PENDING history row.
     */
    private void saveInitialStatusHistory(
            BookingRequest bookingRequest,
            UUID administratorId,
            String administratorName
    ) {
        if (
                bookingRequest == null
                        || bookingRequest
                        .getBookingRequestId()
                        == null
                        || bookingRequest.getStatus()
                        == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request with a status is required before creating status history."
            );
        }

        BookingRequestStatusHistory history =
                BookingRequestStatusHistory
                        .builder()
                        .bookingRequestId(
                                bookingRequest
                                        .getBookingRequestId()
                        )
                        .previousStatus(
                                null
                        )
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
                        .notificationEventId(
                                null
                        )
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

    // =================================================================
    // STATUS HISTORY
    // =================================================================

    /**
     * Saves an immutable booking status-transition history row.
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
                        || bookingRequest
                        .getBookingRequestId()
                        == null
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
                        || administrator
                        .getAdminUserId()
                        == null
        ) {
            throw new IllegalArgumentException(
                    "Administrator information is required for a booking status change."
            );
        }

        BookingRequestStatusHistory history =
                BookingRequestStatusHistory
                        .builder()
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
                        .notificationEventId(
                                null
                        )
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

    // =================================================================
    // STATUS CHANGE REASON
    // =================================================================

    /**
     * Resolves the internal history and audit reason.
     *
     * This value may contain administrator-entered operational
     * language. It is not automatically sent to the customer.
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

    // =================================================================
    // CREATE AUDIT
    // =================================================================

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

    // =================================================================
    // AUDIT SNAPSHOT
    // =================================================================

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

    // =================================================================
    // BOOKING LOOKUP
    // =================================================================

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

    // =================================================================
    // ADMINISTRATOR LOOKUP
    // =================================================================

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
                administrator.getAdminUserId()
                        == null
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
                administrator.getEmail()
                        == null
                        || principal.email()
                        == null
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
                administrator.getRole()
                        == null
                        || principal.role()
                        == null
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

    // =================================================================
    // ADMIN BOOKING SOURCE
    // =================================================================

    /**
     * Prevents administrator bookings from using WEBSITE source.
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

    // =================================================================
    // CUSTOMER BOOKING ACTIVITY TIME
    // =================================================================

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

    // =================================================================
    // BOOKING REFERENCE
    // =================================================================

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

    // =================================================================
    // ADMINISTRATOR DISPLAY NAME
    // =================================================================

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

        if (!isBlank(
                administrator.getEmail()
        )) {
            return administrator
                    .getEmail()
                    .trim();
        }

        return "Administrator";
    }

    // =================================================================
    // REQUEST VALIDATION
    // =================================================================

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
                        || request.status()
                        == null
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
                        || principal.adminUserId()
                        == null
                        || isBlank(
                        principal.email()
                )
                        || principal.role()
                        == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    private void requireBookingValue(
            String value,
            String message
    ) {
        if (isBlank(
                value
        )) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    // =================================================================
    // ERROR
    // =================================================================

    private void reject(
            HttpStatus status,
            String message
    ) {
        throw new PublicRequestRejectedException(
                status,
                message
        );
    }

    // =================================================================
    // NORMALIZATION
    // =================================================================

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