package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.CustomerCreateRequest;
import romelt_techcare.backend.dto.CustomerMergeRequest;
import romelt_techcare.backend.dto.CustomerResolutionResult;
import romelt_techcare.backend.dto.CustomerResponse;
import romelt_techcare.backend.dto.CustomerSummaryResponse;
import romelt_techcare.backend.dto.CustomerUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.enums.CustomerSource;
import romelt_techcare.backend.enums.CustomerStatus;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.CustomerMapper;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.BookingRequestRepository;
import romelt_techcare.backend.repository.CustomerRepository;
import romelt_techcare.backend.service.CustomerService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Manages reusable customer profiles and automatically resolves
 * customers from booking submissions.
 *
 * Customer creation paths:
 *
 * BOOKING:
 * - BookingRequestServiceImpl creates the BookingRequest entity.
 * - This service receives the contact-person snapshot.
 * - Email and phone are normalized and searched independently.
 * - An existing matching customer is reused.
 * - A customer is created only when neither value matches.
 * - The booking is linked through bookingRequest.customerId.
 *
 * ADMINISTRATOR:
 * - An authenticated administrator manually creates the customer.
 * - Duplicate email and phone rules are still enforced.
 *
 * Matching rules:
 * 1. Exact normalized email match.
 * 2. Exact normalized telephone match.
 * 3. If both match the same customer, reuse that customer.
 * 4. If only one matches, reuse that customer.
 * 5. If they match different customers, reject automatic resolution.
 * 6. Never match automatically by name alone.
 *
 * Business booking rule:
 * The Customer entity represents the individual contact person.
 * Business information remains on BookingRequest.
 *
 * Reference format:
 * RTC-CUS-YYYY-XXXXXXXX
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImplementation implements CustomerService {

    private static final String CUSTOMER_NUMBER_PREFIX =
            "RTC-CUS";

    private static final int MAX_CUSTOMER_NUMBER_ATTEMPTS =
            10;

    private final CustomerRepository customerRepository;

    private final BookingRequestRepository bookingRequestRepository;

    private final AdminUserRepository adminUserRepository;

    private final CustomerMapper customerMapper;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Resolves an existing customer or creates a new customer from a
     * booking contact-person snapshot.
     *
     * This method is called by BookingRequestServiceImpl before the
     * final booking is persisted.
     */
    @Override
    @Transactional
    public CustomerResolutionResult resolveOrCreateFromBooking(
            BookingRequest bookingRequest
    ) {
        requireBookingRequest(bookingRequest);

        String normalizedEmail =
                resolveNormalizedEmail(bookingRequest);

        String normalizedPhone =
                resolveNormalizedPhone(bookingRequest);

        Optional<Customer> emailMatch =
                findByNormalizedEmail(normalizedEmail);

        Optional<Customer> phoneMatch =
                findByNormalizedPhone(normalizedPhone);

        Customer resolvedCustomer =
                resolveMatchingCustomer(
                        emailMatch,
                        phoneMatch
                );

        boolean created = false;

        if (resolvedCustomer == null) {
            resolvedCustomer =
                    createCustomerFromBooking(
                            bookingRequest
                    );

            created = true;
        } else {
            updateCustomerFromBookingWhenAppropriate(
                    resolvedCustomer,
                    bookingRequest
            );

            resolvedCustomer.recordBookingActivity(
                    effectiveBookingTime(
                            bookingRequest
                    )
            );

            resolvedCustomer =
                    customerRepository.saveAndFlush(
                            resolvedCustomer
                    );
        }

        bookingRequest.setCustomerId(
                resolvedCustomer.getCustomerId()
        );

        log.info(
                "Booking customer resolved. customerId={}, customerNumber={}, created={}, bookingReferenceNumber={}",
                resolvedCustomer.getCustomerId(),
                resolvedCustomer.getCustomerNumber(),
                created,
                bookingRequest.getReferenceNumber()
        );

        return new CustomerResolutionResult(
                resolvedCustomer,
                created
        );
    }

    /**
     * Creates a customer manually through the administrator portal.
     */
    @Override
    @Transactional
    public CustomerResponse createCustomer(
            AdminJwtPrincipal principal,
            CustomerCreateRequest request
    ) {
        requirePrincipal(principal);
        requireCreateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        String normalizedEmail =
                customerMapper.normalizeOptionalEmail(
                        request.primaryEmail()
                );

        String normalizedPhone =
                customerMapper.normalizeOptionalPhone(
                        request.primaryPhone()
                );

        requireAtLeastOneContact(
                normalizedEmail,
                normalizedPhone
        );

        validateNoConflictingExistingCustomer(
                normalizedEmail,
                normalizedPhone
        );

        String customerNumber =
                generateUniqueCustomerNumber();

        Customer customer =
                customerMapper.toEntity(
                        request,
                        customerNumber,
                        administrator.getAdminUserId()
                );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.CREATE,
                savedCustomer,
                null,
                createCustomerSnapshot(savedCustomer),
                "Administrator created a customer."
        );

        log.info(
                "Administrator created customer. customerId={}, customerNumber={}, createdByAdminUserId={}",
                savedCustomer.getCustomerId(),
                savedCustomer.getCustomerNumber(),
                administrator.getAdminUserId()
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }

    /**
     * Returns a filtered customer list.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getCustomers(
            String keyword,
            CustomerStatus customerStatus,
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Customer pagination information is required."
            );
        }

        String normalizedKeyword =
                normalizeOptional(keyword);

        return customerRepository
                .searchCustomers(
                        normalizedKeyword,
                        customerStatus,
                        pageable
                )
                .map(
                        customerMapper::toSummaryResponse
                );
    }

    /**
     * Returns one complete customer record.
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(
            UUID customerId
    ) {
        return customerMapper.toResponse(
                findNormalCustomer(customerId)
        );
    }

    /**
     * Updates the current reusable customer profile.
     */
    @Override
    @Transactional
    public CustomerResponse updateCustomer(
            AdminJwtPrincipal principal,
            UUID customerId,
            CustomerUpdateRequest request
    ) {
        requirePrincipal(principal);
        requireUpdateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        Customer customer =
                findNormalCustomer(
                        customerId
                );

        JsonNode beforeSnapshot =
                createCustomerSnapshot(
                        customer
                );

        validateUpdateContactUniqueness(
                customer,
                request
        );

        customerMapper.updateEntity(
                customer,
                request,
                administrator.getAdminUserId()
        );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.UPDATE,
                savedCustomer,
                beforeSnapshot,
                createCustomerSnapshot(savedCustomer),
                "Administrator updated a customer."
        );

        log.info(
                "Customer updated. customerId={}, customerNumber={}, updatedByAdminUserId={}",
                savedCustomer.getCustomerId(),
                savedCustomer.getCustomerNumber(),
                administrator.getAdminUserId()
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }

    /**
     * Archives a customer.
     */
    @Override
    @Transactional
    public CustomerResponse archiveCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    ) {
        requirePrincipal(principal);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        Customer customer =
                findNormalCustomer(
                        customerId
                );

        if (
                customer.getCustomerStatus()
                        == CustomerStatus.ARCHIVED
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Customer is already archived."
            );
        }

        JsonNode beforeSnapshot =
                createCustomerSnapshot(
                        customer
                );

        customer.archive(
                administrator.getAdminUserId()
        );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.ARCHIVE,
                savedCustomer,
                beforeSnapshot,
                createCustomerSnapshot(savedCustomer),
                "Administrator archived a customer."
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }

    /**
     * Restores an archived customer.
     */
    @Override
    @Transactional
    public CustomerResponse restoreCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    ) {
        requirePrincipal(principal);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        Customer customer =
                findCustomerIncludingArchived(
                        customerId
                );

        if (
                customer.getCustomerStatus()
                        != CustomerStatus.ARCHIVED
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Only archived customers can be restored."
            );
        }

        validateCustomerContactUniqueness(
                customer.getNormalizedEmail(),
                customer.getNormalizedPhone(),
                customer.getCustomerId()
        );

        JsonNode beforeSnapshot =
                createCustomerSnapshot(
                        customer
                );

        customer.restore(
                administrator.getAdminUserId()
        );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.RESTORE,
                savedCustomer,
                beforeSnapshot,
                createCustomerSnapshot(savedCustomer),
                "Administrator restored a customer."
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }

    /**
     * Soft-deletes a customer.
     */
    @Override
    @Transactional
    public void deleteCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    ) {
        requirePrincipal(principal);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        Customer customer =
                findNormalCustomer(
                        customerId
                );

        JsonNode beforeSnapshot =
                createCustomerSnapshot(
                        customer
                );

        customer.softDelete(
                administrator.getAdminUserId()
        );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.DELETE,
                savedCustomer,
                beforeSnapshot,
                createCustomerSnapshot(savedCustomer),
                "Administrator soft-deleted a customer."
        );

        log.info(
                "Customer soft-deleted. customerId={}, customerNumber={}, deletedByAdminUserId={}",
                savedCustomer.getCustomerId(),
                savedCustomer.getCustomerNumber(),
                administrator.getAdminUserId()
        );
    }

    /**
     * Merges a duplicate customer into a surviving customer.
     */
    @Override
    @Transactional
    public CustomerResponse mergeCustomer(
            AdminJwtPrincipal principal,
            UUID duplicateCustomerId,
            CustomerMergeRequest request
    ) {
        requirePrincipal(principal);
        requireMergeRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(
                        principal
                );

        Customer duplicateCustomer =
                findNormalCustomer(
                        duplicateCustomerId
                );

        Customer survivingCustomer =
                findNormalCustomer(
                        request.survivingCustomerId()
                );

        if (
                duplicateCustomer.getCustomerId()
                        .equals(
                                survivingCustomer.getCustomerId()
                        )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "A customer cannot be merged into itself."
            );
        }

        JsonNode duplicateBeforeSnapshot =
                createCustomerSnapshot(
                        duplicateCustomer
                );

        reassignBookingCustomerReferences(
                duplicateCustomer.getCustomerId(),
                survivingCustomer.getCustomerId()
        );

        mergeCustomerActivity(
                survivingCustomer,
                duplicateCustomer
        );

        survivingCustomer.setUpdatedByAdminUserId(
                administrator.getAdminUserId()
        );

        survivingCustomer.setLastActivityAt(
                Instant.now()
        );

        customerRepository.saveAndFlush(
                survivingCustomer
        );

        duplicateCustomer.mergeInto(
                survivingCustomer.getCustomerId(),
                administrator.getAdminUserId()
        );

        String mergeReason =
                normalizeOptional(
                        request.mergeReason()
                );

        if (mergeReason != null) {
            duplicateCustomer.setInternalNotes(
                    appendInternalNote(
                            duplicateCustomer.getInternalNotes(),
                            "Merged into customer "
                                    + survivingCustomer.getCustomerNumber()
                                    + ". Reason: "
                                    + mergeReason
                    )
            );
        }

        Customer savedDuplicate =
                customerRepository.saveAndFlush(
                        duplicateCustomer
                );

        recordCustomerAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.UPDATE,
                savedDuplicate,
                duplicateBeforeSnapshot,
                createCustomerSnapshot(savedDuplicate),
                "Duplicate customer merged into "
                        + survivingCustomer.getCustomerNumber()
                        + "."
        );

        log.info(
                "Customer merged. duplicateCustomerId={}, survivingCustomerId={}, mergedByAdminUserId={}",
                duplicateCustomer.getCustomerId(),
                survivingCustomer.getCustomerId(),
                administrator.getAdminUserId()
        );

        return customerMapper.toResponse(
                survivingCustomer
        );
    }

    /**
     * Creates a new reusable customer from a booking snapshot.
     */
    private Customer createCustomerFromBooking(
            BookingRequest bookingRequest
    ) {
        String customerNumber =
                generateUniqueCustomerNumber();

        Customer customer =
                customerMapper.fromBooking(
                        customerNumber,
                        bookingRequest.getFullName(),
                        bookingRequest.getEmail(),
                        bookingRequest.getPhone(),
                        bookingRequest.getPreferredContactMethod(),
                        bookingRequest.getStreetAddress(),
                        bookingRequest.getAddressLine2(),
                        bookingRequest.getCity(),
                        bookingRequest.getStateRegion(),
                        bookingRequest.getPostalCode(),
                        bookingRequest.getCountryCode(),
                        CustomerSource.BOOKING,
                        bookingRequest.getCreatedByAdminUserId(),
                        effectiveBookingTime(bookingRequest)
                );

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );

        recordCustomerAudit(
                bookingRequest.getCreatedByAdminUserId(),
                WebsiteContentAuditAction.CREATE,
                savedCustomer,
                null,
                createCustomerSnapshot(savedCustomer),
                "Customer automatically created from booking "
                        + bookingRequest.getReferenceNumber()
                        + "."
        );

        return savedCustomer;
    }

    /**
     * Updates only safe reusable profile fields from a new booking.
     *
     * Existing current customer data is not blindly overwritten.
     */
    private void updateCustomerFromBookingWhenAppropriate(
            Customer customer,
            BookingRequest bookingRequest
    ) {
        if (
                isBlank(customer.getDisplayName())
                        && !isBlank(
                        bookingRequest.getFullName()
                )
        ) {
            customer.setDisplayName(
                    bookingRequest.getFullName().trim()
            );
        }

        if (
                isBlank(customer.getPrimaryEmail())
                        && !isBlank(
                        bookingRequest.getEmail()
                )
        ) {
            customer.setPrimaryEmail(
                    bookingRequest.getEmail().trim()
            );

            customer.setNormalizedEmail(
                    resolveNormalizedEmail(
                            bookingRequest
                    )
            );
        }

        if (
                isBlank(customer.getPrimaryPhone())
                        && !isBlank(
                        bookingRequest.getPhone()
                )
        ) {
            customer.setPrimaryPhone(
                    bookingRequest.getPhone().trim()
            );

            customer.setNormalizedPhone(
                    resolveNormalizedPhone(
                            bookingRequest
                    )
            );
        }

        if (
                customer.getPreferredContactMethod() == null
                        && bookingRequest
                        .getPreferredContactMethod() != null
        ) {
            customer.setPreferredContactMethod(
                    bookingRequest
                            .getPreferredContactMethod()
            );
        }

        if (
                isBlank(customer.getStreetAddress())
                        && !isBlank(
                        bookingRequest.getStreetAddress()
                )
        ) {
            customer.setStreetAddress(
                    bookingRequest
                            .getStreetAddress()
                            .trim()
            );
        }

        if (
                isBlank(customer.getAddressLine2())
                        && !isBlank(
                        bookingRequest.getAddressLine2()
                )
        ) {
            customer.setAddressLine2(
                    bookingRequest
                            .getAddressLine2()
                            .trim()
            );
        }

        if (
                isBlank(customer.getCity())
                        && !isBlank(
                        bookingRequest.getCity()
                )
        ) {
            customer.setCity(
                    bookingRequest.getCity().trim()
            );
        }

        if (
                isBlank(customer.getStateRegion())
                        && !isBlank(
                        bookingRequest.getStateRegion()
                )
        ) {
            customer.setStateRegion(
                    bookingRequest
                            .getStateRegion()
                            .trim()
            );
        }

        if (
                isBlank(customer.getPostalCode())
                        && !isBlank(
                        bookingRequest.getPostalCode()
                )
        ) {
            customer.setPostalCode(
                    bookingRequest
                            .getPostalCode()
                            .trim()
            );
        }

        if (
                isBlank(customer.getCountryCode())
                        && !isBlank(
                        bookingRequest.getCountryCode()
                )
        ) {
            customer.setCountryCode(
                    bookingRequest
                            .getCountryCode()
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );
        }

        customer.setLastActivityAt(
                effectiveBookingTime(
                        bookingRequest
                )
        );
    }

    /**
     * Resolves email and phone matches conservatively.
     */
    private Customer resolveMatchingCustomer(
            Optional<Customer> emailMatch,
            Optional<Customer> phoneMatch
    ) {
        if (
                emailMatch.isPresent()
                        && phoneMatch.isPresent()
        ) {
            Customer emailCustomer =
                    emailMatch.get();

            Customer phoneCustomer =
                    phoneMatch.get();

            if (
                    !emailCustomer.getCustomerId()
                            .equals(
                                    phoneCustomer.getCustomerId()
                            )
            ) {
                reject(
                        HttpStatus.CONFLICT,
                        "The supplied email and telephone number belong to different customer records. Administrator review is required."
                );
            }

            return emailCustomer;
        }

        if (emailMatch.isPresent()) {
            return emailMatch.get();
        }

        return phoneMatch.orElse(null);
    }

    private Optional<Customer> findByNormalizedEmail(
            String normalizedEmail
    ) {
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        return customerRepository
                .findUsableByNormalizedEmail(
                        normalizedEmail
                );
    }

    private Optional<Customer> findByNormalizedPhone(
            String normalizedPhone
    ) {
        if (normalizedPhone == null) {
            return Optional.empty();
        }

        return customerRepository
                .findUsableByNormalizedPhone(
                        normalizedPhone
                );
    }

    /**
     * Reassigns bookings from a duplicate customer to the surviving
     * customer.
     */
    private void reassignBookingCustomerReferences(
            UUID duplicateCustomerId,
            UUID survivingCustomerId
    ) {
        Page<BookingRequest> bookings =
                bookingRequestRepository
                        .findAll(
                                Pageable.unpaged()
                        );

        boolean changed = false;

        for (BookingRequest booking : bookings) {
            if (
                    duplicateCustomerId.equals(
                            booking.getCustomerId()
                    )
            ) {
                booking.setCustomerId(
                        survivingCustomerId
                );

                changed = true;
            }
        }

        if (changed) {
            bookingRequestRepository.saveAll(
                    bookings.getContent()
            );

            bookingRequestRepository.flush();
        }
    }

    /**
     * Preserves the newest activity values when customers are merged.
     */
    private void mergeCustomerActivity(
            Customer survivingCustomer,
            Customer duplicateCustomer
    ) {
        survivingCustomer.setFirstContactAt(
                earliest(
                        survivingCustomer.getFirstContactAt(),
                        duplicateCustomer.getFirstContactAt()
                )
        );

        survivingCustomer.setLastContactedAt(
                latest(
                        survivingCustomer.getLastContactedAt(),
                        duplicateCustomer.getLastContactedAt()
                )
        );

        survivingCustomer.setLastBookingAt(
                latest(
                        survivingCustomer.getLastBookingAt(),
                        duplicateCustomer.getLastBookingAt()
                )
        );

        survivingCustomer.setLastServiceCompletedAt(
                latest(
                        survivingCustomer
                                .getLastServiceCompletedAt(),
                        duplicateCustomer
                                .getLastServiceCompletedAt()
                )
        );

        survivingCustomer.setLastActivityAt(
                latest(
                        survivingCustomer.getLastActivityAt(),
                        duplicateCustomer.getLastActivityAt()
                )
        );
    }


    private void validateNoConflictingExistingCustomer(
            String normalizedEmail,
            String normalizedPhone
    ) {
        Optional<Customer> emailMatch =
                findByNormalizedEmail(normalizedEmail);

        if (emailMatch.isPresent()) {
            reject(
                    HttpStatus.CONFLICT,
                    "A customer already exists with this email address: "
                            + emailMatch.get().getPrimaryEmail()
            );
        }


        Optional<Customer> phoneMatch =
                findByNormalizedPhone(normalizedPhone);

        if (phoneMatch.isPresent()) {
            reject(
                    HttpStatus.CONFLICT,
                    "A customer already exists with this telephone number: "
                            + phoneMatch.get().getPrimaryPhone()
            );
        }
    }


    private void validateUpdateContactUniqueness(
            Customer customer,
            CustomerUpdateRequest request
    ) {
        String normalizedEmail =
                request.primaryEmail() == null
                        ? customer.getNormalizedEmail()
                        : customerMapper
                        .normalizeOptionalEmail(
                                request.primaryEmail()
                        );

        String normalizedPhone =
                request.primaryPhone() == null
                        ? customer.getNormalizedPhone()
                        : customerMapper
                        .normalizeOptionalPhone(
                                request.primaryPhone()
                        );

        requireAtLeastOneContact(
                normalizedEmail,
                normalizedPhone
        );

        validateCustomerContactUniqueness(
                normalizedEmail,
                normalizedPhone,
                customer.getCustomerId()
        );
    }

    private void validateCustomerContactUniqueness(
            String normalizedEmail,
            String normalizedPhone,
            UUID excludedCustomerId
    ) {
        if (
                normalizedEmail != null
                        && customerRepository
                        .existsUsableByNormalizedEmailAndCustomerIdNot(
                                normalizedEmail,
                                excludedCustomerId
                        )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Another customer already uses this email address."
            );
        }

        if (
                normalizedPhone != null
                        && customerRepository
                        .existsUsableByNormalizedPhoneAndCustomerIdNot(
                                normalizedPhone,
                                excludedCustomerId
                        )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Another customer already uses this telephone number."
            );
        }
    }

    private Customer findNormalCustomer(
            UUID customerId
    ) {
        requireCustomerId(customerId);

        return customerRepository
                .findActiveRecordById(
                        customerId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer was not found."
                                )
                );
    }

    private Customer findCustomerIncludingArchived(
            UUID customerId
    ) {
        requireCustomerId(customerId);

        return customerRepository
                .findByCustomerIdAndDeletedAtIsNull(
                        customerId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer was not found."
                                )
                );
    }

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

    private String resolveNormalizedEmail(
            BookingRequest bookingRequest
    ) {
        if (
                !isBlank(
                        bookingRequest.getNormalizedEmail()
                )
        ) {
            return bookingRequest
                    .getNormalizedEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }

        return customerMapper
                .normalizeOptionalEmail(
                        bookingRequest.getEmail()
                );
    }

    private String resolveNormalizedPhone(
            BookingRequest bookingRequest
    ) {
        if (
                !isBlank(
                        bookingRequest.getNormalizedPhone()
                )
        ) {
            return bookingRequest
                    .getNormalizedPhone()
                    .trim();
        }

        return customerMapper
                .normalizeOptionalPhone(
                        bookingRequest.getPhone()
                );
    }

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

    private String generateUniqueCustomerNumber() {
        for (
                int attempt = 0;
                attempt < MAX_CUSTOMER_NUMBER_ATTEMPTS;
                attempt++
        ) {
            String customerNumber =
                    createCustomerNumber();

            if (
                    !customerRepository
                            .existsByCustomerNumber(
                                    customerNumber
                            )
            ) {
                return customerNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique customer number."
        );
    }

    private String createCustomerNumber() {
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
                CUSTOMER_NUMBER_PREFIX,
                Year.now().getValue(),
                randomSegment
        );
    }

    private JsonNode createCustomerSnapshot(
            Customer customer
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "customerId",
                customer.getCustomerId()
        );

        fields.put(
                "customerNumber",
                customer.getCustomerNumber()
        );

        fields.put(
                "displayName",
                customer.getDisplayName()
        );

        fields.put(
                "customerStatus",
                customer.getCustomerStatus()
        );

        fields.put(
                "customerSource",
                customer.getCustomerSource()
        );

        fields.put(
                "preferredContactMethod",
                customer.getPreferredContactMethod()
        );

        fields.put(
                "marketingConsent",
                customer.isMarketingConsent()
        );

        fields.put(
                "emailVerified",
                customer.isEmailVerified()
        );

        fields.put(
                "phoneVerified",
                customer.isPhoneVerified()
        );

        fields.put(
                "doNotEmail",
                customer.isDoNotEmail()
        );

        fields.put(
                "doNotCall",
                customer.isDoNotCall()
        );

        fields.put(
                "doNotText",
                customer.isDoNotText()
        );

        fields.put(
                "lastBookingAt",
                customer.getLastBookingAt()
        );

        fields.put(
                "lastServiceCompletedAt",
                customer.getLastServiceCompletedAt()
        );

        fields.put(
                "lastActivityAt",
                customer.getLastActivityAt()
        );

        fields.put(
                "mergedIntoCustomerId",
                customer.getMergedIntoCustomerId()
        );

        fields.put(
                "archivedAt",
                customer.getArchivedAt()
        );

        fields.put(
                "deletedAt",
                customer.getDeletedAt()
        );

        fields.put(
                "rowVersion",
                customer.getRowVersion()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(
                        fields
                );
    }

    private void recordCustomerAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            Customer customer,
            JsonNode beforeSnapshot,
            JsonNode afterSnapshot,
            String summary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.CUSTOMER,
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                beforeSnapshot,
                afterSnapshot,
                summary,
                null
        );
    }

    private String appendInternalNote(
            String currentNotes,
            String newNote
    ) {
        String normalizedCurrent =
                normalizeOptional(
                        currentNotes
                );

        String normalizedNew =
                normalizeOptional(
                        newNote
                );

        if (normalizedCurrent == null) {
            return normalizedNew;
        }

        if (normalizedNew == null) {
            return normalizedCurrent;
        }

        return normalizedCurrent
                + System.lineSeparator()
                + normalizedNew;
    }

    private Instant earliest(
            Instant first,
            Instant second
    ) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return first.isBefore(second)
                ? first
                : second;
    }

    private Instant latest(
            Instant first,
            Instant second
    ) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return first.isAfter(second)
                ? first
                : second;
    }

    private void requireAtLeastOneContact(
            String normalizedEmail,
            String normalizedPhone
    ) {
        if (
                normalizedEmail == null
                        && normalizedPhone == null
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Customer email or telephone number is required."
            );
        }
    }

    private void requireBookingRequest(
            BookingRequest bookingRequest
    ) {
        if (bookingRequest == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking request is required for customer resolution."
            );
        }

        if (
                isBlank(bookingRequest.getEmail())
                        && isBlank(
                        bookingRequest.getPhone()
                )
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Booking email or telephone number is required for customer resolution."
            );
        }
    }

    private void requireCreateRequest(
            CustomerCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Customer information is required."
            );
        }
    }

    private void requireUpdateRequest(
            CustomerUpdateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Customer update information is required."
            );
        }
    }

    private void requireMergeRequest(
            CustomerMergeRequest request
    ) {
        if (
                request == null
                        || request.survivingCustomerId() == null
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Surviving customer ID is required."
            );
        }
    }

    private void requireCustomerId(
            UUID customerId
    ) {
        if (customerId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID is required."
            );
        }
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || isBlank(principal.email())
                        || principal.role() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
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