package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.BookingFor;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores customer service requests submitted through the public
 * website or entered by an authenticated administrator.
 *
 * Responsibilities:
 * - Links the request to a reusable customer record.
 * - Preserves customer contact details as a historical snapshot.
 * - Distinguishes personal and business service requests.
 * - Stores required business details for business bookings.
 * - Stores requested service, preferred schedule, and location.
 * - Stores the confirmed appointment schedule separately from the
 *   customer's requested date and time.
 * - Tracks the complete booking lifecycle.
 * - Stores lifecycle reasons and administrator attribution.
 * - Stores customer-safe and private service-completion information.
 * - Controls eligibility for a post-service review invitation.
 * - Provides a customer-facing reference number.
 *
 * Customer relationship:
 * customerId links this booking to the reusable Customer record.
 * The contact and business fields remain historical snapshots and
 * must not change when the linked customer profile changes.
 *
 * Booking classification:
 * PERSONAL:
 * - Customer/contact fields are required.
 * - Business fields must remain null.
 *
 * BUSINESS:
 * - Customer/contact fields are required.
 * - Business name, email, phone, and address are required.
 *
 * Scheduling:
 * - preferredDate and preferredTime represent what the customer
 *   requested.
 * - scheduledStartAt, scheduledEndAt, and scheduledTimezone represent
 *   the appointment confirmed by an administrator.
 *
 * Completion:
 * - completionSummary may be shared with the customer.
 * - completionNotes are private operational notes.
 *
 * Security:
 * This entity must never store:
 * - Passwords
 * - Access tokens
 * - Payment-card details
 * - Banking credentials
 * - Account recovery codes
 * ================================================================
 */
@Entity
@Table(
        name = "booking_requests",
        indexes = {
                @Index(
                        name = "idx_booking_requests_reference_number",
                        columnList = "reference_number",
                        unique = true
                ),
                @Index(
                        name = "idx_booking_requests_customer",
                        columnList = "customer_id, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_status",
                        columnList = "status, preferred_date, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_booking_for",
                        columnList = "booking_for, status, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_source",
                        columnList = "booking_source, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_email",
                        columnList = "normalized_email, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_phone",
                        columnList = "normalized_phone, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_business_name",
                        columnList = "business_name, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_business_email",
                        columnList = "normalized_business_email, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_business_phone",
                        columnList = "normalized_business_phone, submitted_at"
                ),
                @Index(
                        name = "idx_booking_requests_service",
                        columnList = "service_id, status, preferred_date"
                ),
                @Index(
                        name = "idx_booking_requests_service_type",
                        columnList = "service_type, status, preferred_date"
                ),
                @Index(
                        name = "idx_booking_requests_preferred_date",
                        columnList = "preferred_date, preferred_time, status"
                ),
                @Index(
                        name = "idx_booking_requests_schedule",
                        columnList = "scheduled_start_at, scheduled_end_at, status"
                ),
                @Index(
                        name = "idx_booking_requests_assigned_admin",
                        columnList = "assigned_admin_user_id, status, scheduled_start_at, preferred_date"
                ),
                @Index(
                        name = "idx_booking_requests_created_by_admin",
                        columnList = "created_by_admin_user_id, created_at"
                ),
                @Index(
                        name = "idx_booking_requests_completed",
                        columnList = "completed_at"
                ),
                @Index(
                        name = "idx_booking_requests_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_request_id", nullable = false, updatable = false)
    private UUID bookingRequestId;

    /**
     * Reusable customer/contact profile linked to this request.
     *
     * Nullable during migration and for historical records that have
     * not yet been resolved to a customer profile.
     */
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "reference_number", nullable = false, unique = true, updatable = false, length = 40)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_for", nullable = false, length = 20, columnDefinition = "varchar(20) default 'PERSONAL'")
    private BookingFor bookingFor;

    // ================================================================
    // CONTACT-PERSON SNAPSHOT
    // ================================================================

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 254)
    private String normalizedEmail;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Column(name = "normalized_phone", nullable = false, length = 30)
    private String normalizedPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", nullable = false, length = 20)
    private ContactMethod preferredContactMethod;

    @Column(name = "notification_email", length = 254)
    private String notificationEmail;

    @Column(name = "notification_phone", length = 40)
    private String notificationPhone;

    // ================================================================
    // BUSINESS SNAPSHOT
    // ================================================================

    @Column(name = "business_name", length = 180)
    private String businessName;

    @Column(name = "business_email", length = 254)
    private String businessEmail;

    @Column(name = "normalized_business_email", length = 254)
    private String normalizedBusinessEmail;

    @Column(name = "business_phone", length = 40)
    private String businessPhone;

    @Column(name = "normalized_business_phone", length = 30)
    private String normalizedBusinessPhone;

    @Column(name = "business_street_address", length = 180)
    private String businessStreetAddress;

    @Column(name = "business_city", length = 100)
    private String businessCity;

    @Column(name = "business_state", length = 100)
    private String businessState;

    @Column(name = "business_postal_code", length = 30)
    private String businessPostalCode;

    @Column(name = "business_country_code", length = 2)
    private String businessCountryCode;

    @Column(name = "business_contact_role", length = 120)
    private String businessContactRole;

    // ================================================================
    // REQUESTED SERVICE
    // ================================================================

    /**
     * Optional link to the stable website service identity.
     *
     * serviceType remains the historical service-name snapshot.
     */
    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "service_type", nullable = false, length = 120)
    private String serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_method", nullable = false, length = 30)
    private ServiceMethod serviceMethod;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time", nullable = false, length = 30)
    private PreferredServiceTime preferredTime;

    @Column(name = "alternate_date")
    private LocalDate alternateDate;

    @Column(name = "device_type", length = 120)
    private String deviceType;

    @Lob
    @Column(name = "problem_description", nullable = false, columnDefinition = "TEXT")
    private String problemDescription;

    // ================================================================
    // SERVICE LOCATION SNAPSHOT
    // ================================================================

    @Column(name = "street_address", length = 180)
    private String streetAddress;

    @Column(name = "address_line_2", length = 180)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_region", length = 100)
    private String stateRegion;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "varchar(2) default 'US'")
    private String countryCode;

    // ================================================================
    // CONFIRMED SCHEDULE
    // ================================================================

    @Column(name = "scheduled_start_at")
    private Instant scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private Instant scheduledEndAt;

    @Column(name = "scheduled_timezone", length = 80)
    private String scheduledTimezone;

    // ================================================================
    // CUSTOMER CONSENT
    // ================================================================

    /**
     * True only when the customer personally accepted the public
     * booking-form acknowledgement.
     *
     * Administrator-created bookings use false because the customer
     * did not directly select the public website checkbox.
     */
    @Column(name = "consent_accepted", nullable = false)
    private boolean consentAccepted;

    @Column(name = "consent_accepted_at")
    private Instant consentAcceptedAt;

    @Column(name = "consent_version", length = 50)
    private String consentVersion;

    @Column(name = "consent_ip_address", length = 64)
    private String consentIpAddress;

    // ================================================================
    // BOOKING LIFECYCLE
    // ================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false,
            length = 30, columnDefinition = "varchar(30) default 'WEBSITE'")
    private BookingSource bookingSource;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    @Column(name = "expiration_reason", length = 500)
    private String expirationReason;

    /**
     * Customer-safe summary of the completed work.
     *
     * This value may be included in the service-completion email.
     */
    @Lob
    @Column(name = "completion_summary", columnDefinition = "TEXT")
    private String completionSummary;

    /**
     * Private administrator or technician completion notes.
     *
     * This value must never be returned from public endpoints.
     */
    @Lob
    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Builder.Default
    @Column(name = "review_eligible", nullable = false,columnDefinition = "boolean default true")
    private boolean reviewEligible = true;


    @Column(name = "review_eligibility_notes", length = 500)
    private String reviewEligibilityNotes;

    // ================================================================
    // ADMINISTRATOR OWNERSHIP AND ATTRIBUTION
    // ================================================================

    @Column(name = "assigned_admin_user_id")
    private UUID assignedAdminUserId;

    /**
     * Administrator account that manually created this booking.
     *
     * Null for public website submissions.
     */
    @Column(name = "created_by_admin_user_id")
    private UUID createdByAdminUserId;

    /**
     * Readable administrator-name snapshot retained for audit history.
     */
    @Column(name = "created_by_admin_name", length = 160)
    private String createdByAdminName;

    @Column(name = "updated_by_admin_user_id")
    private UUID updatedByAdminUserId;

    @Column(name = "confirmed_by_admin_user_id")
    private UUID confirmedByAdminUserId;

    @Column(name = "completed_by_admin_user_id")
    private UUID completedByAdminUserId;

    @Column(name = "cancelled_by_admin_user_id")
    private UUID cancelledByAdminUserId;

    @Column(name = "declined_by_admin_user_id")
    private UUID declinedByAdminUserId;

    @Column(name = "expired_by_admin_user_id")
    private UUID expiredByAdminUserId;

    /**
     * Private general operational notes entered by administrators.
     *
     * This field must never be returned through public endpoints.
     */
    @Lob
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ================================================================
    // AUDIT TIMESTAMPS AND OPTIMISTIC LOCKING
    // ================================================================

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;


    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (status == null) {
            status = BookingRequestStatus.PENDING;
        }

        if (bookingSource == null) {
            bookingSource = BookingSource.WEBSITE;
        }

        if (bookingFor == null) {
            bookingFor = BookingFor.PERSONAL;
        }

        if (countryCode == null || countryCode.isBlank()) {
            countryCode = "US";
        }

        if (
                bookingFor == BookingFor.BUSINESS
                        && (
                        businessCountryCode == null
                                || businessCountryCode.isBlank()
                )
        ) {
            businessCountryCode = "US";
        }

        if (notificationEmail == null || notificationEmail.isBlank()) {
            notificationEmail = email;
        }

        if (notificationPhone == null || notificationPhone.isBlank()) {
            notificationPhone = phone;
        }

        /*
         * Public bookings accepted through the website have already
         * passed consent validation before persistence.
         */
        if (consentAccepted && consentAcceptedAt == null) {
            consentAcceptedAt = now;
        }

        /*
         * Review eligibility defaults to true for normal bookings.
         * The primitive boolean remains false when not initialized by
         * Lombok's builder, so @Builder.Default is used below.
         */

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
     * Applies lifecycle timestamps and administrator attribution for
     * a status transition.
     *
     * Required status-specific details must be assigned before calling
     * this method:
     *
     * CONFIRMED:
     * - scheduledStartAt
     * - scheduledEndAt
     * - scheduledTimezone
     *
     * COMPLETED:
     * - completionSummary
     *
     * CANCELLED:
     * - cancellationReason
     *
     * DECLINED:
     * - declineReason
     *
     * EXPIRED:
     * - expirationReason
     */
    public void applyStatus(
            BookingRequestStatus newStatus,
            UUID administratorId
    ) {
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Booking status is required."
            );
        }

        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        Instant now = Instant.now();

        this.status = newStatus;
        this.updatedByAdminUserId = administratorId;

        switch (newStatus) {
            case PENDING,
                 UNDER_REVIEW -> {
                // No terminal lifecycle timestamp is required.
            }

            case CONFIRMED -> {
                this.confirmedAt = now;
                this.confirmedByAdminUserId = administratorId;
            }

            case COMPLETED -> {
                if (this.confirmedAt == null) {
                    this.confirmedAt = now;
                }

                if (this.confirmedByAdminUserId == null) {
                    this.confirmedByAdminUserId = administratorId;
                }

                this.completedAt = now;
                this.completedByAdminUserId = administratorId;
            }

            case CANCELLED -> {
                this.cancelledAt = now;
                this.cancelledByAdminUserId = administratorId;
            }

            case DECLINED -> {
                this.declinedAt = now;
                this.declinedByAdminUserId = administratorId;
            }

            case EXPIRED -> {
                this.expiredAt = now;
                this.expiredByAdminUserId = administratorId;
            }
        }
    }

    /**
     * Clears all business snapshot fields when the booking is personal.
     */
    public void clearBusinessDetails() {
        this.businessName = null;
        this.businessEmail = null;
        this.normalizedBusinessEmail = null;
        this.businessPhone = null;
        this.normalizedBusinessPhone = null;
        this.businessStreetAddress = null;
        this.businessCity = null;
        this.businessState = null;
        this.businessPostalCode = null;
        this.businessCountryCode = null;
        this.businessContactRole = null;
    }
}