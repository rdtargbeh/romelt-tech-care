package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.CustomerCreateRequest;
import romelt_techcare.backend.dto.CustomerResponse;
import romelt_techcare.backend.dto.CustomerSummaryResponse;
import romelt_techcare.backend.dto.CustomerUpdateRequest;
import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.enums.CustomerSource;
import romelt_techcare.backend.enums.CustomerStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts customer DTOs and booking contact snapshots into reusable
 * Customer entities and administrator response DTOs.
 *
 * Responsibilities:
 * - Maps administrator-created customer requests.
 * - Creates customer records automatically from booking submissions.
 * - Updates existing customer profiles without changing historical
 *   BookingRequest snapshots.
 * - Preserves readable email and telephone values.
 * - Generates normalized email and telephone matching values.
 * - Maps complete and compact administrator responses.
 *
 * Customer-resolution rules:
 * - Customer is the reusable individual contact person.
 * - Business information remains on BookingRequest.
 * - Email is normalized to lowercase for matching.
 * - Telephone numbers are normalized to digits only.
 * - Names must never be used as the sole automatic matching key.
 *
 * Security:
 * Internal and communication notes are included only in the complete
 * administrator response.
 * ================================================================
 */
@Component
public class CustomerMapper {

    private static final Pattern NON_DIGIT_PATTERN =
            Pattern.compile("\\D");

    /**
     * Maps an administrator-created customer request.
     */
    public Customer toEntity(
            CustomerCreateRequest request,
            String customerNumber,
            java.util.UUID administratorId
    ) {
        requireCreateRequest(request);

        String primaryEmail =
                normalizeOptionalEmail(
                        request.primaryEmail()
                );

        String primaryPhone =
                normalizeOptional(
                        request.primaryPhone()
                );

        validateAtLeastOneContact(
                primaryEmail,
                primaryPhone
        );

        Instant now = Instant.now();

        boolean marketingConsent =
                request.marketingConsent();

        return Customer.builder()
                .customerNumber(
                        normalizeRequired(customerNumber)
                )
                .firstName(
                        normalizeOptional(request.firstName())
                )
                .lastName(
                        normalizeOptional(request.lastName())
                )
                .preferredName(
                        normalizeOptional(request.preferredName())
                )
                .displayName(
                        normalizeRequired(request.displayName())
                )
                .primaryEmail(primaryEmail)
                .normalizedEmail(
                        normalizeOptionalEmail(primaryEmail)
                )
                .primaryPhone(primaryPhone)
                .normalizedPhone(
                        normalizeOptionalPhone(primaryPhone)
                )
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                .streetAddress(
                        normalizeOptional(request.streetAddress())
                )
                .addressLine2(
                        normalizeOptional(request.addressLine2())
                )
                .city(
                        normalizeOptional(request.city())
                )
                .stateRegion(
                        normalizeOptional(request.stateRegion())
                )
                .postalCode(
                        normalizeOptional(request.postalCode())
                )
                .countryCode(
                        normalizeCountryCode(request.countryCode())
                )
                .customerStatus(CustomerStatus.ACTIVE)
                .customerSource(
                        request.customerSource() == null
                                ? CustomerSource.ADMIN_CREATED
                                : request.customerSource()
                )
                .marketingConsent(marketingConsent)
                .marketingConsentAt(
                        marketingConsent ? now : null
                )
                .marketingConsentSource(
                        marketingConsent
                                ? normalizeOptional(
                                request.marketingConsentSource()
                        )
                                : null
                )
                .emailVerified(false)
                .emailVerifiedAt(null)
                .phoneVerified(false)
                .phoneVerifiedAt(null)
                .doNotEmail(request.doNotEmail())
                .doNotCall(request.doNotCall())
                .doNotText(request.doNotText())
                .communicationNotes(
                        normalizeOptional(
                                request.communicationNotes()
                        )
                )
                .internalNotes(
                        normalizeOptional(request.internalNotes())
                )
                .firstContactAt(now)
                .lastActivityAt(now)
                .createdByAdminUserId(administratorId)
                .updatedByAdminUserId(administratorId)
                .build();
    }

    /**
     * Creates a reusable customer profile from a public or
     * administrator booking contact-person snapshot.
     *
     * Business fields are intentionally excluded.
     */
    public Customer fromBooking(
            String customerNumber,
            String fullName,
            String email,
            String phone,
            romelt_techcare.backend.enums.ContactMethod
                    preferredContactMethod,
            String streetAddress,
            String addressLine2,
            String city,
            String stateRegion,
            String postalCode,
            String countryCode,
            CustomerSource customerSource,
            java.util.UUID createdByAdminUserId,
            Instant bookingAt
    ) {
        String normalizedFullName =
                normalizeRequired(fullName);

        String primaryEmail =
                normalizeOptionalEmail(email);

        String primaryPhone =
                normalizeOptional(phone);

        validateAtLeastOneContact(
                primaryEmail,
                primaryPhone
        );

        NameParts nameParts =
                splitName(normalizedFullName);

        Instant effectiveBookingAt =
                bookingAt == null
                        ? Instant.now()
                        : bookingAt;

        Customer customer = Customer.builder()
                .customerNumber(
                        normalizeRequired(customerNumber)
                )
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .preferredName(nameParts.firstName())
                .displayName(normalizedFullName)
                .primaryEmail(primaryEmail)
                .normalizedEmail(
                        normalizeOptionalEmail(primaryEmail)
                )
                .primaryPhone(primaryPhone)
                .normalizedPhone(
                        normalizeOptionalPhone(primaryPhone)
                )
                .preferredContactMethod(
                        preferredContactMethod
                )
                .streetAddress(
                        normalizeOptional(streetAddress)
                )
                .addressLine2(
                        normalizeOptional(addressLine2)
                )
                .city(
                        normalizeOptional(city)
                )
                .stateRegion(
                        normalizeOptional(stateRegion)
                )
                .postalCode(
                        normalizeOptional(postalCode)
                )
                .countryCode(
                        normalizeCountryCode(countryCode)
                )
                .customerStatus(CustomerStatus.ACTIVE)
                .customerSource(
                        customerSource == null
                                ? CustomerSource.BOOKING
                                : customerSource
                )
                .marketingConsent(false)
                .emailVerified(false)
                .phoneVerified(false)
                .doNotEmail(false)
                .doNotCall(false)
                .doNotText(false)
                .firstContactAt(effectiveBookingAt)
                .lastBookingAt(effectiveBookingAt)
                .lastActivityAt(effectiveBookingAt)
                .createdByAdminUserId(createdByAdminUserId)
                .updatedByAdminUserId(createdByAdminUserId)
                .build();

        return customer;
    }

    /**
     * Applies an administrator customer-profile update.
     *
     * Existing values are preserved when a request field is null.
     * Empty strings clear optional text fields.
     */
    public void updateEntity(
            Customer customer,
            CustomerUpdateRequest request,
            java.util.UUID administratorId
    ) {
        requireCustomer(customer);
        requireUpdateRequest(request);

        if (request.firstName() != null) {
            customer.setFirstName(
                    normalizeOptional(request.firstName())
            );
        }

        if (request.lastName() != null) {
            customer.setLastName(
                    normalizeOptional(request.lastName())
            );
        }

        if (request.preferredName() != null) {
            customer.setPreferredName(
                    normalizeOptional(request.preferredName())
            );
        }

        if (request.displayName() != null) {
            customer.setDisplayName(
                    normalizeRequired(request.displayName())
            );
        }

        if (request.primaryEmail() != null) {
            String primaryEmail =
                    normalizeOptionalEmail(
                            request.primaryEmail()
                    );

            customer.setPrimaryEmail(primaryEmail);
            customer.setNormalizedEmail(
                    normalizeOptionalEmail(primaryEmail)
            );

            /*
             * Changing the email invalidates prior verification.
             */
            customer.setEmailVerified(false);
            customer.setEmailVerifiedAt(null);
        }

        if (request.primaryPhone() != null) {
            String primaryPhone =
                    normalizeOptional(
                            request.primaryPhone()
                    );

            customer.setPrimaryPhone(primaryPhone);
            customer.setNormalizedPhone(
                    normalizeOptionalPhone(primaryPhone)
            );

            /*
             * Changing the telephone number invalidates prior
             * verification.
             */
            customer.setPhoneVerified(false);
            customer.setPhoneVerifiedAt(null);
        }

        validateAtLeastOneContact(
                customer.getPrimaryEmail(),
                customer.getPrimaryPhone()
        );

        if (request.preferredContactMethod() != null) {
            customer.setPreferredContactMethod(
                    request.preferredContactMethod()
            );
        }

        if (request.streetAddress() != null) {
            customer.setStreetAddress(
                    normalizeOptional(request.streetAddress())
            );
        }

        if (request.addressLine2() != null) {
            customer.setAddressLine2(
                    normalizeOptional(request.addressLine2())
            );
        }

        if (request.city() != null) {
            customer.setCity(
                    normalizeOptional(request.city())
            );
        }

        if (request.stateRegion() != null) {
            customer.setStateRegion(
                    normalizeOptional(request.stateRegion())
            );
        }

        if (request.postalCode() != null) {
            customer.setPostalCode(
                    normalizeOptional(request.postalCode())
            );
        }

        if (request.countryCode() != null) {
            customer.setCountryCode(
                    normalizeCountryCode(request.countryCode())
            );
        }

        applyCustomerStatus(
                customer,
                request.customerStatus()
        );

        applyMarketingConsent(
                customer,
                request
        );

        applyVerificationState(
                customer,
                request
        );

        if (request.doNotEmail() != null) {
            customer.setDoNotEmail(
                    request.doNotEmail()
            );
        }

        if (request.doNotCall() != null) {
            customer.setDoNotCall(
                    request.doNotCall()
            );
        }

        if (request.doNotText() != null) {
            customer.setDoNotText(
                    request.doNotText()
            );
        }

        if (request.communicationNotes() != null) {
            customer.setCommunicationNotes(
                    normalizeOptional(
                            request.communicationNotes()
                    )
            );
        }

        if (request.internalNotes() != null) {
            customer.setInternalNotes(
                    normalizeOptional(request.internalNotes())
            );
        }

        customer.setUpdatedByAdminUserId(
                administratorId
        );

        customer.setLastActivityAt(
                Instant.now()
        );
    }

    /**
     * Maps a complete administrator customer response.
     */
    public CustomerResponse toResponse(
            Customer customer
    ) {
        requireCustomer(customer);

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPreferredName(),
                customer.getDisplayName(),
                customer.getPrimaryEmail(),
                customer.getPrimaryPhone(),
                customer.getPreferredContactMethod(),
                customer.getStreetAddress(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getStateRegion(),
                customer.getPostalCode(),
                customer.getCountryCode(),
                customer.getCustomerStatus(),
                customer.getCustomerSource(),
                customer.isMarketingConsent(),
                customer.getMarketingConsentAt(),
                customer.getMarketingConsentSource(),
                customer.isEmailVerified(),
                customer.getEmailVerifiedAt(),
                customer.isPhoneVerified(),
                customer.getPhoneVerifiedAt(),
                customer.isDoNotEmail(),
                customer.isDoNotCall(),
                customer.isDoNotText(),
                customer.getCommunicationNotes(),
                customer.getInternalNotes(),
                customer.getFirstContactAt(),
                customer.getLastContactedAt(),
                customer.getLastBookingAt(),
                customer.getLastServiceCompletedAt(),
                customer.getLastActivityAt(),
                customer.getMergedIntoCustomerId(),
                customer.getMergedAt(),
                customer.getMergedByAdminUserId(),
                customer.getCreatedByAdminUserId(),
                customer.getUpdatedByAdminUserId(),
                customer.getArchivedByAdminUserId(),
                customer.getDeletedByAdminUserId(),
                customer.getArchivedAt(),
                customer.getDeletedAt(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getRowVersion()
        );
    }

    /**
     * Maps a compact administrator list and selector response.
     */
    public CustomerSummaryResponse toSummaryResponse(
            Customer customer
    ) {
        requireCustomer(customer);

        return new CustomerSummaryResponse(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getDisplayName(),
                customer.getPreferredName(),
                customer.getPrimaryEmail(),
                customer.getPrimaryPhone(),
                customer.getPreferredContactMethod(),
                customer.getCustomerStatus(),
                customer.getCustomerSource(),
                customer.getLastBookingAt(),
                customer.getLastServiceCompletedAt(),
                customer.getLastActivityAt()
        );
    }

    public String normalizeEmail(
            String value
    ) {
        return normalizeRequired(value)
                .toLowerCase(Locale.ROOT);
    }

    public String normalizeOptionalEmail(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(
            String value
    ) {
        String normalized =
                normalizeOptionalPhone(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    "Telephone number cannot be normalized."
            );
        }

        return normalized;
    }

    public String normalizeOptionalPhone(
            String value
    ) {
        String readableValue =
                normalizeOptional(value);

        if (readableValue == null) {
            return null;
        }

        String normalized =
                NON_DIGIT_PATTERN
                        .matcher(readableValue)
                        .replaceAll("");

        return normalized.isBlank()
                ? null
                : normalized;
    }

    public String normalizeCountryCode(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return "US";
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    public String normalizeRequired(
            String value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Required customer value must not be null."
            );
        }

        String normalized =
                value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required customer value must not be blank."
            );
        }

        return normalized;
    }

    public String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void applyCustomerStatus(
            Customer customer,
            CustomerStatus requestedStatus
    ) {
        if (requestedStatus == null) {
            return;
        }

        if (
                requestedStatus == CustomerStatus.MERGED
                        || requestedStatus == CustomerStatus.ARCHIVED
                        || requestedStatus == CustomerStatus.DELETED
        ) {
            throw new IllegalArgumentException(
                    "Merge, archive, and delete statuses require dedicated customer operations."
            );
        }

        customer.setCustomerStatus(
                requestedStatus
        );
    }

    private void applyMarketingConsent(
            Customer customer,
            CustomerUpdateRequest request
    ) {
        if (request.marketingConsent() == null) {
            if (request.marketingConsentSource() != null) {
                customer.setMarketingConsentSource(
                        normalizeOptional(
                                request.marketingConsentSource()
                        )
                );
            }

            return;
        }

        boolean consent =
                request.marketingConsent();

        customer.setMarketingConsent(consent);

        if (consent) {
            if (customer.getMarketingConsentAt() == null) {
                customer.setMarketingConsentAt(
                        Instant.now()
                );
            }

            customer.setMarketingConsentSource(
                    normalizeOptional(
                            request.marketingConsentSource()
                    )
            );
        } else {
            customer.setMarketingConsentAt(null);
            customer.setMarketingConsentSource(null);
        }
    }

    private void applyVerificationState(
            Customer customer,
            CustomerUpdateRequest request
    ) {
        if (request.emailVerified() != null) {
            customer.setEmailVerified(
                    request.emailVerified()
            );

            customer.setEmailVerifiedAt(
                    request.emailVerified()
                            ? Instant.now()
                            : null
            );
        }

        if (request.phoneVerified() != null) {
            customer.setPhoneVerified(
                    request.phoneVerified()
            );

            customer.setPhoneVerifiedAt(
                    request.phoneVerified()
                            ? Instant.now()
                            : null
            );
        }
    }

    private void validateAtLeastOneContact(
            String email,
            String phone
    ) {
        if (
                normalizeOptional(email) == null
                        && normalizeOptional(phone) == null
        ) {
            throw new IllegalArgumentException(
                    "Customer email or telephone number is required."
            );
        }
    }

    private NameParts splitName(
            String displayName
    ) {
        String normalizedName =
                normalizeRequired(displayName);

        String[] parts =
                normalizedName.split("\\s+", 2);

        String firstName =
                parts[0];

        String lastName =
                parts.length > 1
                        ? normalizeOptional(parts[1])
                        : null;

        return new NameParts(
                firstName,
                lastName
        );
    }

    private void requireCreateRequest(
            CustomerCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Customer create request must not be null."
            );
        }
    }

    private void requireUpdateRequest(
            CustomerUpdateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Customer update request must not be null."
            );
        }
    }

    private void requireCustomer(
            Customer customer
    ) {
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer must not be null."
            );
        }
    }

    private record NameParts(
            String firstName,
            String lastName
    ) {
    }
}