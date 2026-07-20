package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import romelt_techcare.backend.enums.ContactInquiryStatus;
import romelt_techcare.backend.enums.ContactMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores general inquiries submitted through the public contact form.
 *
 * Responsibilities:
 * - Preserves customer contact information.
 * - Stores the inquiry subject and message.
 * - Tracks the related service and preferred reply method.
 * - Generates a public reference number.
 * - Tracks the inquiry's operational status.
 *
 * Security:
 * This table must not be used to store passwords, payment-card data,
 * banking credentials, access tokens, or Social Security numbers.
 * ================================================================
 */
@Entity
@Table(
        name = "contact_inquiries",
        indexes = {
                @Index(
                        name = "idx_contact_inquiries_reference_number",
                        columnList = "reference_number",
                        unique = true
                ),
                @Index(
                        name = "idx_contact_inquiries_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_contact_inquiries_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_contact_inquiries_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "contact_inquiry_id",
            nullable = false,
            updatable = false
    )
    private UUID contactInquiryId;

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
            length = 30
    )
    private String phone;

    @Column(
            name = "subject",
            nullable = false,
            length = 120
    )
    private String subject;

    @Column(
            name = "service_type",
            length = 100
    )
    private String serviceType;

    @Lob
    @Column(
            name = "message",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "preferred_contact_method",
            nullable = false,
            length = 20
    )
    private ContactMethod preferredContactMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ContactInquiryStatus status;

    @Column(
            name = "consent_accepted",
            nullable = false
    )
    private boolean consentAccepted;

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

        updatedAt = now;

        if (status == null) {
            status = ContactInquiryStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}