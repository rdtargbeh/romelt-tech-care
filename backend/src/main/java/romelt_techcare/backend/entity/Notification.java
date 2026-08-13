package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SIMPLE NOTIFICATION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores customer EMAIL/SMS delivery history and administrator
 * IN_APP notifications in one table.
 *
 * Responsibilities:
 * - Links a notification to a booking or contact inquiry.
 * - Identifies the customer or administrator recipient.
 * - Stores the final rendered title and message.
 * - Tracks provider submission and delivery results.
 * - Tracks administrator read and dismissed state.
 * - Preserves basic failure and retry information.
 *
 * Supported channels:
 * - EMAIL
 * - SMS
 * - IN_APP
 *
 * Design:
 * This entity intentionally avoids the previous enterprise outbox,
 * worker-leasing, delivery-attempt, suppression, and database-template
 * architecture.
 * ================================================================
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notifications_customer",
                        columnList = "customer_id, created_at"
                ),
                @Index(
                        name = "idx_notifications_admin",
                        columnList = "admin_user_id, created_at"
                ),
                @Index(
                        name = "idx_notifications_resource",
                        columnList = "resource_type, resource_id, created_at"
                ),
                @Index(
                        name = "idx_notifications_status",
                        columnList = "notification_status, created_at"
                ),
                @Index(
                        name = "idx_notifications_provider_message",
                        columnList = "provider_message_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationId;

    // ================================================================
    // CHANNEL AND RECIPIENT
    // ================================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "channel",
            nullable = false,
            length = 20,
            updatable = false
    )
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recipient_type",
            nullable = false,
            length = 20,
            updatable = false
    )
    private NotificationRecipientType recipientType;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            length = 40,
            updatable = false
    )
    private NotificationResourceType resourceType;

    @Column(
            name = "resource_id",
            nullable = false,
            updatable = false
    )
    private UUID resourceId;

    /**
     * EMAIL:
     * Customer email address.
     *
     * SMS:
     * Customer telephone number.
     *
     * IN_APP:
     * May remain null because adminUserId identifies the recipient.
     */
    @Column(
            name = "recipient_address",
            length = 500,
            updatable = false
    )
    private String recipientAddress;

    @Column(
            name = "recipient_name",
            length = 180,
            updatable = false
    )
    private String recipientName;

    // ================================================================
    // FINAL MESSAGE CONTENT
    // ================================================================

    /**
     * EMAIL subject or IN_APP notification title.
     *
     * SMS may use a short descriptive title for administrator history,
     * even though Twilio only receives messageText.
     */
    @Column(
            name = "title",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String title;

    /**
     * Plain-text email body, SMS message, or IN_APP message.
     */
    @Lob
    @Column(
            name = "message_text",
            nullable = false,
            columnDefinition = "TEXT",
            updatable = false
    )
    private String messageText;

    /**
     * Optional HTML email body.
     *
     * Must remain null for SMS and IN_APP notifications.
     */
    @Lob
    @Column(
            name = "message_html",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String messageHtml;

    // ================================================================
    // DELIVERY STATUS
    // ================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_status",
            nullable = false,
            length = 30
    )
    private NotificationStatus notificationStatus =
            NotificationStatus.PENDING;

    @Column(
            name = "provider_name",
            length = 100
    )
    private String providerName;

    @Column(
            name = "provider_message_id",
            length = 255
    )
    private String providerMessageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "provider_response_json",
            columnDefinition = "jsonb"
    )
    private JsonNode providerResponseJson;

    @Builder.Default
    @Column(
            name = "attempt_count",
            nullable = false
    )
    private Integer attemptCount = 0;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(
            name = "failure_code",
            length = 100
    )
    private String failureCode;

    @Lob
    @Column(
            name = "failure_message",
            columnDefinition = "TEXT"
    )
    private String failureMessage;

    // ================================================================
    // ADMINISTRATOR IN-APP STATE
    // ================================================================

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    // ================================================================
    // AUDIT AND OPTIMISTIC LOCKING
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

    // ================================================================
    // ENTITY LIFECYCLE
    // ================================================================

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        applyDefaults();
        normalizeFields();
        validateNotification();

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
        applyDefaults();
        normalizeMutableFields();
        validateNotification();

        updatedAt = Instant.now();
    }

    private void applyDefaults() {
        if (notificationStatus == null) {
            notificationStatus =
                    NotificationStatus.PENDING;
        }

        if (attemptCount == null || attemptCount < 0) {
            attemptCount = 0;
        }
    }

    // ================================================================
    // DELIVERY OPERATIONS
    // ================================================================

    /**
     * Records the beginning of an EMAIL or SMS provider call.
     */
    public void beginAttempt() {
        requireExternalChannel();

        attemptCount++;
        lastAttemptedAt = Instant.now();

        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    /**
     * Marks an EMAIL or SMS message accepted by its provider.
     */
    public void markSent(
            String provider,
            String messageId,
            JsonNode providerResponse
    ) {
        requireExternalChannel();

        providerName =
                requireText(
                        provider,
                        "Notification provider name is required."
                );

        providerMessageId =
                normalizeOptional(messageId);

        providerResponseJson =
                copyJson(providerResponse);

        notificationStatus =
                NotificationStatus.SENT;

        sentAt = Instant.now();

        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    /**
     * Marks provider-confirmed delivery.
     */
    public void markDelivered(
            Instant deliveryTime
    ) {
        Instant effectiveTime =
                deliveryTime == null
                        ? Instant.now()
                        : deliveryTime;

        if (sentAt == null) {
            sentAt = effectiveTime;
        }

        deliveredAt = effectiveTime;

        notificationStatus =
                NotificationStatus.DELIVERED;

        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    /**
     * Publishes an administrator IN_APP notification.
     */
    public void markInAppDelivered() {
        requireInAppChannel();

        Instant now = Instant.now();

        sentAt = now;
        deliveredAt = now;

        notificationStatus =
                NotificationStatus.DELIVERED;

        providerName = null;
        providerMessageId = null;
        providerResponseJson = null;

        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    /**
     * Marks a notification delivery failure.
     */
    public void markFailed(
            String code,
            String message,
            JsonNode providerResponse
    ) {
        String normalizedMessage =
                requireText(
                        message,
                        "Notification failure message is required."
                );

        notificationStatus =
                NotificationStatus.FAILED;

        failedAt = Instant.now();

        failureCode =
                normalizeOptional(code);

        failureMessage =
                normalizedMessage;

        providerResponseJson =
                channel == NotificationChannel.IN_APP
                        ? null
                        : copyJson(providerResponse);
    }

    public void cancel(
            String reason
    ) {
        notificationStatus =
                NotificationStatus.CANCELLED;

        failureCode = null;
        failureMessage =
                normalizeOptional(reason);
    }

    // ================================================================
    // ADMINISTRATOR PORTAL OPERATIONS
    // ================================================================

    public void markRead() {
        requireInAppChannel();

        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public void markUnread() {
        requireInAppChannel();
        readAt = null;
    }

    public void dismiss() {
        requireInAppChannel();

        if (dismissedAt == null) {
            dismissedAt = Instant.now();
        }
    }

    public void restore() {
        requireInAppChannel();
        dismissedAt = null;
    }

    @Transient
    public boolean isRead() {
        return channel == NotificationChannel.IN_APP
                && readAt != null;
    }

    @Transient
    public boolean isDismissed() {
        return channel == NotificationChannel.IN_APP
                && dismissedAt != null;
    }

    // ================================================================
    // VALIDATION
    // ================================================================

    private void validateNotification() {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification channel is required."
            );
        }

        if (recipientType == null) {
            throw new IllegalArgumentException(
                    "Notification recipient type is required."
            );
        }

        if (resourceType == null) {
            throw new IllegalArgumentException(
                    "Notification resource type is required."
            );
        }

        if (resourceId == null) {
            throw new IllegalArgumentException(
                    "Notification resource ID is required."
            );
        }

        title =
                requireText(
                        title,
                        "Notification title is required."
                );

        messageText =
                requireText(
                        messageText,
                        "Notification message is required."
                );

        if (
                recipientType
                        == NotificationRecipientType.ADMIN
                        && adminUserId == null
        ) {
            throw new IllegalArgumentException(
                    "Administrator notification requires an administrator ID."
            );
        }

        if (
                channel == NotificationChannel.EMAIL
                        || channel == NotificationChannel.SMS
        ) {
            recipientAddress =
                    requireText(
                            recipientAddress,
                            "Email and SMS notifications require a recipient address."
                    );
        }

        if (channel == NotificationChannel.IN_APP) {
            if (
                    recipientType
                            != NotificationRecipientType.ADMIN
            ) {
                throw new IllegalArgumentException(
                        "In-app notifications currently support administrators only."
                );
            }

            if (adminUserId == null) {
                throw new IllegalArgumentException(
                        "In-app notification requires an administrator ID."
                );
            }

            messageHtml = null;
        }

        if (channel == NotificationChannel.SMS) {
            messageHtml = null;
        }

        if (
                readAt != null
                        && channel != NotificationChannel.IN_APP
        ) {
            throw new IllegalArgumentException(
                    "Only in-app notifications can be marked as read."
            );
        }

        if (
                dismissedAt != null
                        && channel != NotificationChannel.IN_APP
        ) {
            throw new IllegalArgumentException(
                    "Only in-app notifications can be dismissed."
            );
        }
    }

    private void requireExternalChannel() {
        if (channel == NotificationChannel.IN_APP) {
            throw new IllegalStateException(
                    "In-app notifications do not use an external provider."
            );
        }
    }

    private void requireInAppChannel() {
        if (channel != NotificationChannel.IN_APP) {
            throw new IllegalStateException(
                    "This operation is available only for in-app notifications."
            );
        }
    }

    // ================================================================
    // NORMALIZATION
    // ================================================================

    private void normalizeFields() {
        recipientAddress =
                normalizeRecipientAddress(
                        recipientAddress
                );

        recipientName =
                normalizeOptional(recipientName);

        title =
                normalizeOptional(title);

        messageText =
                normalizeOptional(messageText);

        messageHtml =
                normalizeOptional(messageHtml);

        providerName =
                normalizeOptional(providerName);

        providerMessageId =
                normalizeOptional(providerMessageId);

        failureCode =
                normalizeOptional(failureCode);

        failureMessage =
                normalizeOptional(failureMessage);
    }

    private void normalizeMutableFields() {
        providerName =
                normalizeOptional(providerName);

        providerMessageId =
                normalizeOptional(providerMessageId);

        failureCode =
                normalizeOptional(failureCode);

        failureMessage =
                normalizeOptional(failureMessage);
    }

    private String normalizeRecipientAddress(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null || channel == null) {
            return normalized;
        }

        return switch (channel) {
            case EMAIL ->
                    normalized.toLowerCase(Locale.ROOT);

            case SMS ->
                    normalized;

            case IN_APP ->
                    normalized;
        };
    }

    private JsonNode copyJson(
            JsonNode value
    ) {
        return value == null
                ? null
                : value.deepCopy();
    }

    private String requireText(
            String value,
            String message
    ) {
        String normalized =
                normalizeOptional(value);

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