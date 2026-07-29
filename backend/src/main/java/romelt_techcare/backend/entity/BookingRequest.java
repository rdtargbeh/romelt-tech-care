package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
 * - Stores customer contact information.
 * - Stores requested service and service method.
 * - Stores preferred and alternate service dates.
 * - Stores optional service-location information.
 * - Tracks operational status.
 * - Tracks how the booking entered the system.
 * - Tracks which administrator entered an internal booking.
 * - Stores optional private administrator notes.
 * - Provides a customer-facing reference number.
 *
 * Booking sources:
 * - Public website submissions use WEBSITE.
 * - Administrator-created records may use PHONE, EMAIL, WALK_IN,
 *   ADMIN_ENTRY, or OTHER.
 *
 * Important:
 * A BookingRequest is not automatically a confirmed appointment.
 * A request becomes confirmed only after review and approval.
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
                        name = "idx_booking_requests_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_booking_requests_source",
                        columnList = "booking_source"
                ),
                @Index(
                        name = "idx_booking_requests_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_booking_requests_preferred_date",
                        columnList = "preferred_date"
                ),
                @Index(
                        name = "idx_booking_requests_created_by_admin",
                        columnList = "created_by_admin_user_id"
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
    @Column(
            name = "booking_request_id",
            nullable = false,
            updatable = false
    )
    private UUID bookingRequestId;

    @Column(
            name = "reference_number",
            nullable = false,
            unique = true,
            updatable = false,
            length = 40
    )
    private String referenceNumber;

    @Column(
            name = "full_name",
            nullable = false,
            length = 120
    )
    private String fullName;

    @Column(
            name = "email",
            nullable = false,
            length = 254
    )
    private String email;

    @Column(
            name = "phone",
            nullable = false,
            length = 30
    )
    private String phone;

    @Column(
            name = "service_type",
            nullable = false,
            length = 120
    )
    private String serviceType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "service_method",
            nullable = false,
            length = 30
    )
    private ServiceMethod serviceMethod;

    @Column(
            name = "preferred_date",
            nullable = false
    )
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "preferred_time",
            nullable = false,
            length = 30
    )
    private PreferredServiceTime preferredTime;

    @Column(
            name = "alternate_date"
    )
    private LocalDate alternateDate;

    @Column(
            name = "street_address",
            length = 180
    )
    private String streetAddress;

    @Column(
            name = "city",
            length = 100
    )
    private String city;

    @Column(
            name = "state",
            length = 100
    )
    private String state;

    @Column(
            name = "postal_code",
            length = 15
    )
    private String postalCode;

    @Column(
            name = "device_type",
            length = 120
    )
    private String deviceType;

    @Lob
    @Column(
            name = "problem_description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String problemDescription;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "preferred_contact_method",
            nullable = false,
            length = 20
    )
    private ContactMethod preferredContactMethod;

    /**
     * True only when the customer personally accepted the disclaimer
     * on the public website booking form.
     *
     * Administrator-created bookings use false because the customer
     * did not personally select the website checkbox.
     */
    @Column(
            name = "consent_accepted",
            nullable = false
    )
    private boolean consentAccepted;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private BookingRequestStatus status;

    /**
     * Identifies whether this booking originated from the website,
     * telephone, email, walk-in request, or another channel.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "booking_source",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'WEBSITE'"
    )
    private BookingSource bookingSource;

    /**
     * Administrator account that entered this booking.
     *
     * Null for customer-submitted public website bookings.
     */
    @Column(
            name = "created_by_admin_user_id"
    )
    private UUID createdByAdminUserId;

    /**
     * Snapshot of the administrator's name when the booking was
     * created.
     *
     * This preserves a readable audit value even if the administrator
     * account is later renamed or removed.
     */
    @Column(
            name = "created_by_admin_name",
            length = 160
    )
    private String createdByAdminName;

    /**
     * Private operational notes entered by an administrator.
     *
     * These notes must never be returned through the public booking
     * confirmation endpoint.
     */
    @Lob
    @Column(
            name = "admin_notes",
            columnDefinition = "TEXT"
    )
    private String adminNotes;

    @Column(
            name = "submitted_at",
            nullable = false,
            updatable = false
    )
    private Instant submittedAt;

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

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}