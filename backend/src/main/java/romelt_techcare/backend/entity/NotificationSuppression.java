package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION ENTITY
 * ================================================================
 *
 * Purpose:
 * Prevents EMAIL or SMS notifications from being sent to recipients
 * who opted out, have invalid addresses, produced hard bounces,
 * submitted spam complaints, or were blocked administratively.
 *
 * Responsibilities:
 * - Preserves the readable recipient address.
 * - Stores a normalized address for exact suppression matching.
 * - Supports category-specific or all-category suppression.
 * - Links suppression records to customers where available.
 * - Preserves suppression history through activation state.
 * - Supports optional expiration.
 * - Tracks administrator creation and deactivation.
 * - Supports optimistic locking.
 *
 * Category behavior:
 * - notificationCategory = null means all categories are suppressed.
 * - A non-null category suppresses only that category.
 *
 * Channel behavior:
 * - EMAIL addresses are normalized to lowercase.
 * - SMS numbers are normalized to digits only.
 * - IN_APP is intentionally unsupported because portal notifications
 *   do not use recipient-address suppression.
 *
 * Active uniqueness:
 * PostgreSQL allows only one active suppression for the same:
 *
 * channel + normalizedRecipientAddress + category
 * ================================================================
 */
@Entity
@Table(
        name = "notification_suppressions",
        indexes = {
                @Index(
                        name = "idx_notification_suppressions_customer",
                        columnList = "customer_id, created_at"
                ),
                @Index(
                        name = "idx_notification_suppressions_recipient",
                        columnList = "channel, normalized_recipient_address, created_at"
                ),
                @Index(
                        name = "idx_notification_suppressions_active",
                        columnList = "channel, normalized_recipient_address, notification_category"
                ),
                @Index(
                        name = "idx_notification_suppressions_expiration",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_notification_suppressions_reason",
                        columnList = "suppression_reason, created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSuppression {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_suppression_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationSuppressionId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "channel",
            nullable = false,
            length = 20,
            updatable = false
    )
    private NotificationChannel channel;

    /**
     * Null suppresses all notification categories for this recipient.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_category",
            length = 30,
            updatable = false
    )
    private NotificationCategory notificationCategory;

    @Column(
            name = "recipient_address",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String recipientAddress;

    @Column(
            name = "normalized_recipient_address",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String normalizedRecipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "suppression_reason",
            nullable = false,
            length = 40
    )
    private NotificationSuppressionReason suppressionReason;

    @Column(
            name = "reason_details",
            length = 1000
    )
    private String reasonDetails;

    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false,
            columnDefinition = "boolean default true"
    )
    private boolean active = true;

    @Column(
            name = "suppressed_at",
            nullable = false,
            updatable = false
    )
    private Instant suppressedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(
            name = "created_by_admin_user_id",
            updatable = false
    )
    private UUID createdByAdminUserId;

    @Column(name = "deactivated_by_admin_user_id")
    private UUID deactivatedByAdminUserId;

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

        normalizeFields();
        validateSuppression();

        if (suppressedAt == null) {
            suppressedAt = now;
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
        normalizeMutableFields();
        validateSuppression();

        updatedAt = Instant.now();
    }

    /**
     * Deactivates this suppression while preserving its history.
     */
    public void deactivate(
            UUID administratorId
    ) {
        if (!active) {
            return;
        }

        active = false;
        deactivatedAt = Instant.now();
        deactivatedByAdminUserId = administratorId;
    }

    /**
     * Reactivates a historical suppression.
     *
     * The service must first ensure no competing active suppression
     * exists for the same channel, recipient, and category.
     */
    public void reactivate() {
        if (active) {
            return;
        }

        active = true;
        deactivatedAt = null;
        deactivatedByAdminUserId = null;

        if (
                expiresAt != null
                        && !expiresAt.isAfter(Instant.now())
        ) {
            throw new IllegalStateException(
                    "An expired notification suppression cannot be reactivated."
            );
        }
    }

    /**
     * Updates editable reason and expiration information.
     */
    public void updateDetails(
            NotificationSuppressionReason newReason,
            String newReasonDetails,
            Instant newExpiresAt
    ) {
        if (newReason == null) {
            throw new IllegalArgumentException(
                    "Notification suppression reason is required."
            );
        }

        suppressionReason = newReason;
        reasonDetails = normalizeOptional(newReasonDetails);
        expiresAt = newExpiresAt;

        validateSuppression();
    }

    /**
     * Returns whether this suppression currently applies.
     */
    @Transient
    public boolean isEffective() {
        if (!active) {
            return false;
        }

        return expiresAt == null
                || expiresAt.isAfter(Instant.now());
    }

    /**
     * Returns whether this suppression applies to the supplied
     * notification category.
     */
    @Transient
    public boolean suppressesCategory(
            NotificationCategory category
    ) {
        if (!isEffective()) {
            return false;
        }

        return notificationCategory == null
                || notificationCategory == category;
    }

    private void validateSuppression() {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification suppression channel is required."
            );
        }

        if (channel == NotificationChannel.IN_APP) {
            throw new IllegalArgumentException(
                    "In-app notifications do not support recipient-address suppression."
            );
        }

        if (suppressionReason == null) {
            throw new IllegalArgumentException(
                    "Notification suppression reason is required."
            );
        }

        if (
                expiresAt != null
                        && suppressedAt != null
                        && !expiresAt.isAfter(suppressedAt)
        ) {
            throw new IllegalArgumentException(
                    "Notification suppression expiration must be after the suppression time."
            );
        }

        if (
                !active
                        && deactivatedAt == null
        ) {
            throw new IllegalArgumentException(
                    "Inactive notification suppression requires a deactivation time."
            );
        }

        if (
                deactivatedAt != null
                        && suppressedAt != null
                        && deactivatedAt.isBefore(suppressedAt)
        ) {
            throw new IllegalArgumentException(
                    "Notification suppression deactivation cannot occur before suppression."
            );
        }
    }

    private void normalizeFields() {
        recipientAddress =
                requireText(
                        recipientAddress,
                        "Notification suppression recipient address is required."
                );

        normalizedRecipientAddress =
                normalizeRecipientAddress(
                        recipientAddress,
                        channel
                );

        reasonDetails = normalizeOptional(reasonDetails);
    }

    private void normalizeMutableFields() {
        reasonDetails = normalizeOptional(reasonDetails);
    }

    private String normalizeRecipientAddress(
            String value,
            NotificationChannel suppressionChannel
    ) {
        String normalized =
                requireText(
                        value,
                        "Notification suppression recipient address is required."
                );

        if (suppressionChannel == null) {
            return normalized;
        }

        return switch (suppressionChannel) {
            case EMAIL ->
                    normalized.toLowerCase(Locale.ROOT);

            case SMS -> {
                String digitsOnly =
                        normalized.replaceAll("\\D", "");

                if (digitsOnly.isBlank()) {
                    throw new IllegalArgumentException(
                            "Notification suppression telephone number is invalid."
                    );
                }

                yield digitsOnly;
            }

            case IN_APP ->
                    throw new IllegalArgumentException(
                            "In-app notifications do not support recipient-address suppression."
                    );
        };
    }

    private String requireText(
            String value,
            String message
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}