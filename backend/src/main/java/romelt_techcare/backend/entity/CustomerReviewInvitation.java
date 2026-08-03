package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW INVITATION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores a secure invitation that permits a customer to submit one
 * verified review for a completed booking.
 *
 * Security:
 * - Only the SHA-256 hash of the invitation token is stored.
 * - The plain token must never be persisted or logged.
 * - Tokens are single-use.
 * - Expired and revoked tokens cannot be used.
 *
 * Responsibilities:
 * - Associates the invitation with one booking request.
 * - Stores the verified customer email snapshot.
 * - Tracks pending, sent, used, expired, and revoked states.
 * - Records creation and revocation administrators.
 * ================================================================
 */
@Entity
@Table(
        name = "customer_review_invitations",
        indexes = {
                @Index(
                        name = "idx_review_invitations_booking",
                        columnList = "booking_request_id, created_at"
                ),
                @Index(
                        name = "idx_review_invitations_status",
                        columnList = "invitation_status, expires_at"
                ),
                @Index(
                        name = "idx_review_invitations_email",
                        columnList = "customer_email"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReviewInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "review_invitation_id",
            nullable = false,
            updatable = false
    )
    private UUID reviewInvitationId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "booking_request_id",
            nullable = false,
            updatable = false
    )
    private BookingRequest bookingRequest;

    @Column(
            name = "customer_email",
            nullable = false,
            length = 254
    )
    private String customerEmail;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            updatable = false,
            length = 255
    )
    private String tokenHash;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "invitation_status",
            nullable = false,
            length = 30
    )
    private CustomerReviewInvitationStatus invitationStatus =
            CustomerReviewInvitationStatus.PENDING;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_admin_user_id")
    private AdminUser revokedByAdminUser;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        customerEmail = normalizeEmail(customerEmail);
        tokenHash = normalizeRequired(tokenHash, "Token hash");

        if (invitationStatus == null) {
            invitationStatus =
                    CustomerReviewInvitationStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        validateState(now);
    }

    public void markSent() {
        requireUsableState();

        Instant now = Instant.now();

        requireNotExpired(now);

        invitationStatus =
                CustomerReviewInvitationStatus.SENT;

        if (sentAt == null) {
            sentAt = now;
        }
    }

    public void markUsed() {
        requireUsableState();

        Instant now = Instant.now();

        requireNotExpired(now);

        invitationStatus =
                CustomerReviewInvitationStatus.USED;

        usedAt = now;
    }

    public void markExpired() {
        if (
                invitationStatus
                        == CustomerReviewInvitationStatus.USED
                        || invitationStatus
                        == CustomerReviewInvitationStatus.REVOKED
        ) {
            return;
        }

        invitationStatus =
                CustomerReviewInvitationStatus.EXPIRED;
    }

    public void revoke(
            AdminUser administrator
    ) {
        if (
                invitationStatus
                        == CustomerReviewInvitationStatus.USED
        ) {
            throw new IllegalStateException(
                    "A used review invitation cannot be revoked."
            );
        }

        if (
                invitationStatus
                        == CustomerReviewInvitationStatus.REVOKED
        ) {
            return;
        }

        invitationStatus =
                CustomerReviewInvitationStatus.REVOKED;

        revokedAt = Instant.now();
        revokedByAdminUser = administrator;
    }

    public boolean isExpired() {
        return expiresAt != null
                && !expiresAt.isAfter(Instant.now());
    }

    public boolean isUsable() {
        return (
                invitationStatus
                        == CustomerReviewInvitationStatus.PENDING
                        || invitationStatus
                        == CustomerReviewInvitationStatus.SENT
        )
                && !isExpired();
    }

    private void requireUsableState() {
        if (
                invitationStatus
                        != CustomerReviewInvitationStatus.PENDING
                        && invitationStatus
                        != CustomerReviewInvitationStatus.SENT
        ) {
            throw new IllegalStateException(
                    "The review invitation is no longer usable."
            );
        }
    }

    private void requireNotExpired(
            Instant now
    ) {
        if (
                expiresAt == null
                        || !expiresAt.isAfter(now)
        ) {
            markExpired();

            throw new IllegalStateException(
                    "The review invitation has expired."
            );
        }
    }

    private void validateState(
            Instant now
    ) {
        if (bookingRequest == null) {
            throw new IllegalStateException(
                    "Booking request is required."
            );
        }

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalStateException(
                    "Invitation expiration must be in the future."
            );
        }

        if (
                invitationStatus
                        == CustomerReviewInvitationStatus.USED
                        && usedAt == null
        ) {
            throw new IllegalStateException(
                    "A used invitation requires a used timestamp."
            );
        }

        if (
                invitationStatus
                        == CustomerReviewInvitationStatus.REVOKED
                        && revokedAt == null
        ) {
            throw new IllegalStateException(
                    "A revoked invitation requires a revoked timestamp."
            );
        }
    }

    private String normalizeEmail(
            String value
    ) {
        return normalizeRequired(
                value,
                "Customer email"
        ).toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }
}