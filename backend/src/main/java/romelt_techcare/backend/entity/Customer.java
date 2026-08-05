package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.CustomerSource;
import romelt_techcare.backend.enums.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the reusable customer/contact profile used across bookings,
 * contact inquiries, reviews, notifications, and future services.
 *
 * Responsibilities:
 * - Stores the customer's current contact information.
 * - Stores normalized email and telephone values for matching.
 * - Supports automatic customer creation from booking submissions.
 * - Supports administrator-created customer profiles.
 * - Tracks customer communication preferences and opt-outs.
 * - Tracks recent booking, contact, service, and general activity.
 * - Supports archiving, soft deletion, blocking, and duplicate merging.
 * - Preserves administrator attribution and optimistic locking.
 *
 * Booking integration:
 * - BookingRequest stores the historical contact snapshot submitted
 *   for a specific booking.
 * - Customer stores the current reusable contact profile.
 * - The booking service resolves an existing customer by normalized
 *   email and telephone number before creating a new customer.
 * - BookingRequest.customerId links the booking to this record.
 *
 * Business-booking integration:
 * This entity represents the individual contact person.
 *
 * Business details submitted when bookingFor = BUSINESS remain on the
 * BookingRequest as a historical business snapshot. They are not
 * copied permanently into Customer because one person may represent
 * different businesses over time.
 *
 * Duplicate handling:
 * Duplicate customer profiles are merged rather than physically
 * deleted. A MERGED record points to the surviving customer through
 * mergedIntoCustomerId.
 *
 * Security:
 * internalNotes and communicationNotes are administrator-only and
 * must never be returned through public APIs.
 * ================================================================
 */
@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_display_name", columnList = "display_name"),
                @Index(name = "idx_customers_preferred_name", columnList = "preferred_name"),
                @Index(name = "idx_customers_status", columnList = "customer_status, created_at"),
                @Index(name = "idx_customers_email", columnList = "normalized_email"),
                @Index(name = "idx_customers_phone", columnList = "normalized_phone"),
                @Index(name = "idx_customers_last_contacted", columnList = "last_contacted_at"),
                @Index(name = "idx_customers_last_booking", columnList = "last_booking_at"),
                @Index(name = "idx_customers_last_activity", columnList = "last_activity_at"),
                @Index(name = "idx_customers_last_service_completed", columnList = "last_service_completed_at"),
                @Index(name = "idx_customers_merged_into", columnList = "merged_into_customer_id, merged_at"),
                @Index(name = "idx_customers_deleted", columnList = "deleted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_number", columnNames = "customer_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    /**
     * Public-safe internal customer reference.
     *
     * Recommended format:
     * RTC-CUS-YYYY-XXXXXXXX
     */
    @Column(name = "customer_number", nullable = false, unique = true, updatable = false, length = 40)
    private String customerNumber;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "preferred_name", length = 100)
    private String preferredName;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    // ================================================================
    // CURRENT CONTACT INFORMATION
    // ================================================================

    /**
     * Customer's current readable email address.
     *
     * Booking records retain their own historical email snapshots.
     */
    @Column(
            name = "primary_email",
            length = 254
    )
    private String primaryEmail;

    /**
     * Lowercase email used for exact matching and duplicate
     * prevention.
     */
    @Column(
            name = "normalized_email",
            length = 254
    )
    private String normalizedEmail;

    /**
     * Customer's current readable telephone number.
     */
    @Column(
            name = "primary_phone",
            length = 40
    )
    private String primaryPhone;

    /**
     * Digits-only telephone number used for matching.
     */
    @Column(
            name = "normalized_phone",
            length = 30
    )
    private String normalizedPhone;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "preferred_contact_method",
            length = 20
    )
    private ContactMethod preferredContactMethod;

    // ================================================================
    // CURRENT CUSTOMER ADDRESS
    // ================================================================

    @Column(
            name = "street_address",
            length = 180
    )
    private String streetAddress;

    @Column(
            name = "address_line_2",
            length = 180
    )
    private String addressLine2;

    @Column(
            name = "city",
            length = 100
    )
    private String city;

    @Column(
            name = "state_region",
            length = 100
    )
    private String stateRegion;

    @Column(
            name = "postal_code",
            length = 30
    )
    private String postalCode;

    @Builder.Default
    @Column(
            name = "country_code",
            nullable = false,
            length = 2,
            columnDefinition = "varchar(2) default 'US'"
    )
    private String countryCode = "US";

    // ================================================================
    // CUSTOMER CLASSIFICATION
    // ================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "customer_status",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'ACTIVE'"
    )
    private CustomerStatus customerStatus = CustomerStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "customer_source",
            nullable = false,
            length = 40,
            columnDefinition = "varchar(40) default 'OTHER'"
    )
    private CustomerSource customerSource = CustomerSource.OTHER;

    // ================================================================
    // MARKETING AND VERIFICATION
    // ================================================================

    @Builder.Default
    @Column(
            name = "marketing_consent",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean marketingConsent = false;

    @Column(
            name = "marketing_consent_at"
    )
    private Instant marketingConsentAt;

    @Column(
            name = "marketing_consent_source",
            length = 50
    )
    private String marketingConsentSource;

    @Builder.Default
    @Column(
            name = "email_verified",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean emailVerified = false;

    @Column(
            name = "email_verified_at"
    )
    private Instant emailVerifiedAt;

    @Builder.Default
    @Column(
            name = "phone_verified",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean phoneVerified = false;

    @Column(
            name = "phone_verified_at"
    )
    private Instant phoneVerifiedAt;

    // ================================================================
    // COMMUNICATION RESTRICTIONS
    // ================================================================

    @Builder.Default
    @Column(
            name = "do_not_email",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean doNotEmail = false;

    @Builder.Default
    @Column(
            name = "do_not_call",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean doNotCall = false;

    @Builder.Default
    @Column(
            name = "do_not_text",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean doNotText = false;

    /**
     * Practical communication instructions.
     *
     * Examples:
     * - Call after 5 PM.
     * - Do not leave voicemail.
     * - Send scheduling messages by email.
     */
    @Column(
            name = "communication_notes",
            length = 1000
    )
    private String communicationNotes;

    /**
     * Private administrator notes.
     *
     * Must never be exposed through public endpoints.
     */
    @Lob
    @Column(
            name = "internal_notes",
            columnDefinition = "TEXT"
    )
    private String internalNotes;

    // ================================================================
    // CUSTOMER ACTIVITY
    // ================================================================

    @Column(
            name = "first_contact_at"
    )
    private Instant firstContactAt;

    @Column(
            name = "last_contacted_at"
    )
    private Instant lastContactedAt;

    @Column(
            name = "last_booking_at"
    )
    private Instant lastBookingAt;

    @Column(
            name = "last_service_completed_at"
    )
    private Instant lastServiceCompletedAt;

    @Column(
            name = "last_activity_at"
    )
    private Instant lastActivityAt;

    // ================================================================
    // DUPLICATE CUSTOMER MERGING
    // ================================================================

    /**
     * Surviving customer profile when this record has been merged.
     *
     * Required only when customerStatus = MERGED.
     */
    @Column(
            name = "merged_into_customer_id"
    )
    private UUID mergedIntoCustomerId;

    @Column(
            name = "merged_at"
    )
    private Instant mergedAt;

    @Column(
            name = "merged_by_admin_user_id"
    )
    private UUID mergedByAdminUserId;

    // ================================================================
    // ADMINISTRATOR ATTRIBUTION
    // ================================================================

    @Column(
            name = "created_by_admin_user_id"
    )
    private UUID createdByAdminUserId;

    @Column(
            name = "updated_by_admin_user_id"
    )
    private UUID updatedByAdminUserId;

    @Column(
            name = "archived_by_admin_user_id"
    )
    private UUID archivedByAdminUserId;

    @Column(
            name = "deleted_by_admin_user_id"
    )
    private UUID deletedByAdminUserId;

    @Column(
            name = "archived_at"
    )
    private Instant archivedAt;

    @Column(
            name = "deleted_at"
    )
    private Instant deletedAt;

    // ================================================================
    // AUDIT TIMESTAMPS AND OPTIMISTIC LOCKING
    // ================================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(
            name = "row_version",
            nullable = false
    )
    private Long rowVersion;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (customerStatus == null) {
            customerStatus = CustomerStatus.ACTIVE;
        }

        if (customerSource == null) {
            customerSource = CustomerSource.OTHER;
        }

        if (countryCode == null || countryCode.isBlank()) {
            countryCode = "US";
        } else {
            countryCode = countryCode.trim().toUpperCase();
        }

        if (firstContactAt == null) {
            firstContactAt = now;
        }

        if (lastActivityAt == null) {
            lastActivityAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (rowVersion == null) {
            rowVersion = 0L;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Records activity created by a new booking.
     */
    public void recordBookingActivity(
            Instant activityAt
    ) {
        Instant effectiveAt =
                activityAt == null
                        ? Instant.now()
                        : activityAt;

        if (firstContactAt == null) {
            firstContactAt = effectiveAt;
        }

        lastBookingAt = effectiveAt;
        lastActivityAt = effectiveAt;
    }

    /**
     * Records completion of a service linked to this customer.
     */
    public void recordServiceCompletion(
            Instant completedAt
    ) {
        Instant effectiveAt =
                completedAt == null
                        ? Instant.now()
                        : completedAt;

        lastServiceCompletedAt = effectiveAt;
        lastActivityAt = effectiveAt;
    }

    /**
     * Records a general customer-contact activity.
     */
    public void recordContactActivity(
            Instant contactedAt
    ) {
        Instant effectiveAt =
                contactedAt == null
                        ? Instant.now()
                        : contactedAt;

        if (firstContactAt == null) {
            firstContactAt = effectiveAt;
        }

        lastContactedAt = effectiveAt;
        lastActivityAt = effectiveAt;
    }

    /**
     * Marks this customer as merged into a surviving profile.
     */
    public void mergeInto(
            UUID survivingCustomerId,
            UUID administratorId
    ) {
        if (survivingCustomerId == null) {
            throw new IllegalArgumentException(
                    "Surviving customer ID is required."
            );
        }

        if (
                customerId != null
                        && customerId.equals(
                        survivingCustomerId
                )
        ) {
            throw new IllegalArgumentException(
                    "A customer cannot be merged into itself."
            );
        }

        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        customerStatus = CustomerStatus.MERGED;
        mergedIntoCustomerId = survivingCustomerId;
        mergedByAdminUserId = administratorId;
        mergedAt = Instant.now();
        lastActivityAt = mergedAt;
    }

    /**
     * Archives the customer without physically deleting the record.
     */
    public void archive(
            UUID administratorId
    ) {
        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        customerStatus = CustomerStatus.ARCHIVED;
        archivedByAdminUserId = administratorId;
        archivedAt = Instant.now();
        updatedByAdminUserId = administratorId;
        lastActivityAt = archivedAt;
    }

    /**
     * Restores an archived customer.
     */
    public void restore(
            UUID administratorId
    ) {
        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        customerStatus = CustomerStatus.ACTIVE;
        archivedByAdminUserId = null;
        archivedAt = null;
        updatedByAdminUserId = administratorId;
        lastActivityAt = Instant.now();
    }

    /**
     * Soft-deletes the customer.
     */
    public void softDelete(
            UUID administratorId
    ) {
        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        customerStatus = CustomerStatus.DELETED;
        deletedByAdminUserId = administratorId;
        deletedAt = Instant.now();
        updatedByAdminUserId = administratorId;
        lastActivityAt = deletedAt;
    }

    /**
     * Returns true when this customer can be used for new bookings,
     * inquiries, and notifications.
     */
    @Transient
    public boolean isUsableCustomer() {
        return deletedAt == null
                && customerStatus != CustomerStatus.MERGED
                && customerStatus != CustomerStatus.DELETED
                && customerStatus != CustomerStatus.BLOCKED;
    }
}