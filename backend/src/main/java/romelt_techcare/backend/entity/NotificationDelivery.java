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
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ENTITY
 * ================================================================
 *
 * Purpose:
 * Represents one channel-specific delivery created from a committed
 * notification event.
 *
 * Supported channels:
 * - EMAIL
 * - SMS
 * - IN_APP
 *
 * Responsibilities:
 * - Links the delivery to its parent notification event.
 * - Identifies the receiving customer or administrator.
 * - Preserves recipient and sender snapshots.
 * - Stores rendered title, text, and HTML content.
 * - Tracks provider submission for EMAIL and SMS.
 * - Supports worker leasing, retries, and failure handling.
 * - Tracks delivered, opened, clicked, bounced, cancelled, and
 *   suppressed states.
 * - Tracks administrator read and dismissal state for IN_APP
 *   notifications.
 *
 * IN_APP behavior:
 * - recipientAdminUserId is required.
 * - subject is the notification title.
 * - renderedBodyText is the notification message.
 * - renderedBodyHtml is not used.
 * - External provider fields remain null.
 * - readAt records when the administrator opens the notification.
 * - dismissedAt records when it is removed from the active portal
 *   notification list.
 *
 * Attempt history:
 * Every EMAIL or SMS provider call must create a corresponding
 * NotificationDeliveryAttempt record.
 *
 * IN_APP deliveries do not call an external provider and therefore do
 * not require provider-attempt records.
 *
 * Security:
 * Rendered content and provider responses may contain personal data.
 * They must never be exposed through public endpoints or copied into
 * ordinary application logs.
 * ================================================================
 */
@Entity
@Table(
        name = "notification_deliveries",
        indexes = {
                @Index(
                        name = "idx_notification_deliveries_event",
                        columnList = "notification_event_id, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_pending",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_retry_pending",
                        columnList = "next_attempt_at, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_expired_locks",
                        columnList = "lock_expires_at, processing_started_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_customer",
                        columnList = "customer_id, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_recipient_admin",
                        columnList = "recipient_admin_user_id, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_recipient",
                        columnList = "channel, normalized_recipient_address, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_status",
                        columnList = "delivery_status, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_provider",
                        columnList = "provider_name, created_at"
                ),
                @Index(
                        name = "idx_notification_deliveries_provider_message",
                        columnList = "provider_message_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_delivery_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationDeliveryId;

    // ================================================================
    // PARENT EVENT AND RECIPIENT RELATIONSHIPS
    // ================================================================

    @Column(
            name = "notification_event_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationEventId;

    @Column(name = "customer_id")
    private UUID customerId;

    /**
     * Required for administrator IN_APP notifications.
     *
     * May also be populated for administrator EMAIL notifications.
     */
    @Column(name = "recipient_admin_user_id")
    private UUID recipientAdminUserId;

    // ================================================================
    // CHANNEL AND RECIPIENT SNAPSHOT
    // ================================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "channel",
            nullable = false,
            length = 20,
            updatable = false
    )
    private NotificationChannel channel;

    /**
     * Readable recipient address.
     *
     * EMAIL:
     * Email address.
     *
     * SMS:
     * Telephone number.
     *
     * IN_APP:
     * Administrator UUID as a string.
     */
    @Column(
            name = "recipient_address",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String recipientAddress;

    /**
     * Normalized recipient address.
     *
     * EMAIL:
     * Lowercase email.
     *
     * SMS:
     * Digits-only phone number.
     *
     * IN_APP:
     * Lowercase administrator UUID.
     */
    @Column(
            name = "normalized_recipient_address",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String normalizedRecipientAddress;

    @Column(
            name = "recipient_name",
            length = 180,
            updatable = false
    )
    private String recipientName;

    @Column(
            name = "sender_address",
            length = 500,
            updatable = false
    )
    private String senderAddress;

    @Column(
            name = "sender_name",
            length = 180,
            updatable = false
    )
    private String senderName;

    // ================================================================
    // RENDERED MESSAGE SNAPSHOT
    // ================================================================

    /**
     * EMAIL subject or IN_APP notification title.
     */
    @Column(
            name = "subject",
            length = 500,
            updatable = false
    )
    private String subject;

    /**
     * EMAIL plain-text body, SMS content, or IN_APP message.
     */
    @Lob
    @Column(
            name = "rendered_body_text",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String renderedBodyText;

    /**
     * EMAIL HTML body.
     *
     * Must remain null for SMS and IN_APP deliveries.
     */
    @Lob
    @Column(
            name = "rendered_body_html",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String renderedBodyHtml;

    // ================================================================
    // DELIVERY STATE
    // ================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "delivery_status",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'PENDING'"
    )
    private NotificationDeliveryStatus deliveryStatus =
            NotificationDeliveryStatus.PENDING;

    // ================================================================
    // PROVIDER SNAPSHOT
    // ================================================================

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

    // ================================================================
    // RETRY CONTROL
    // ================================================================

    @Builder.Default
    @Column(
            name = "attempt_count",
            nullable = false,
            columnDefinition = "integer default 0"
    )
    private Integer attemptCount = 0;

    @Builder.Default
    @Column(
            name = "maximum_attempts",
            nullable = false,
            columnDefinition = "integer default 5"
    )
    private Integer maximumAttempts = 5;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "first_attempted_at")
    private Instant firstAttemptedAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    // ================================================================
    // WORKER LEASE
    // ================================================================

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(
            name = "locked_by",
            length = 160
    )
    private String lockedBy;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    // ================================================================
    // DELIVERY LIFECYCLE TIMESTAMPS
    // ================================================================

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "bounced_at")
    private Instant bouncedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "suppressed_at")
    private Instant suppressedAt;

    // ================================================================
    // IN-APP USER STATE
    // ================================================================

    /**
     * Time the receiving administrator read the portal notification.
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Time the receiving administrator dismissed the portal
     * notification.
     */
    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    // ================================================================
    // FAILURE AND SUPPRESSION DETAILS
    // ================================================================

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

    @Column(
            name = "suppression_reason",
            length = 100
    )
    private String suppressionReason;

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
        validateDelivery();

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
        validateDelivery();

        updatedAt = Instant.now();
    }

    private void applyDefaults() {
        if (deliveryStatus == null) {
            deliveryStatus =
                    NotificationDeliveryStatus.PENDING;
        }

        if (attemptCount == null || attemptCount < 0) {
            attemptCount = 0;
        }

        if (maximumAttempts == null || maximumAttempts < 1) {
            maximumAttempts = 5;
        }
    }

    // ================================================================
    // WORKER OPERATIONS
    // ================================================================

    /**
     * Claims the delivery for an exclusive worker lease.
     */
    public void claim(
            String workerId,
            Duration leaseDuration
    ) {
        String normalizedWorkerId =
                requireText(
                        workerId,
                        "Notification delivery worker ID is required."
                );

        if (
                leaseDuration == null
                        || leaseDuration.isZero()
                        || leaseDuration.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery lease duration must be positive."
            );
        }

        if (!isAvailableForClaim()) {
            throw new IllegalStateException(
                    "Notification delivery is not available for processing."
            );
        }

        Instant now = Instant.now();

        deliveryStatus =
                NotificationDeliveryStatus.PROCESSING;

        processingStartedAt = now;

        lockedAt = now;
        lockedBy = normalizedWorkerId;
        lockExpiresAt = now.plus(leaseDuration);

        nextAttemptAt = null;
        failureCode = null;
        failureMessage = null;
        failedAt = null;
    }

    /**
     * Renews the active worker lease.
     */
    public void renewLease(
            String workerId,
            Duration leaseDuration
    ) {
        requireProcessingState();
        requireLeaseOwner(workerId);

        if (
                leaseDuration == null
                        || leaseDuration.isZero()
                        || leaseDuration.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery lease duration must be positive."
            );
        }

        Instant now = Instant.now();

        lockedAt = now;
        lockExpiresAt = now.plus(leaseDuration);
    }

    /**
     * Records the beginning of an EMAIL or SMS provider attempt.
     */
    public int beginAttempt() {
        requireExternalProviderChannel();
        requireProcessingState();

        if (attemptCount >= maximumAttempts) {
            throw new IllegalStateException(
                    "Maximum notification delivery attempts have been reached."
            );
        }

        Instant now = Instant.now();

        attemptCount++;

        if (firstAttemptedAt == null) {
            firstAttemptedAt = now;
        }

        lastAttemptedAt = now;

        return attemptCount;
    }

    /**
     * Marks an EMAIL or SMS delivery accepted by the provider.
     */
    public void markSent(
            String provider,
            String messageId,
            JsonNode providerResponse
    ) {
        requireExternalProviderChannel();
        requireProcessingState();

        providerName =
                requireText(
                        provider,
                        "Notification provider name is required."
                );

        providerMessageId =
                normalizeOptional(messageId);

        providerResponseJson =
                copyJson(providerResponse);

        deliveryStatus =
                NotificationDeliveryStatus.SENT;

        sentAt = Instant.now();

        nextAttemptAt = null;
        failedAt = null;
        failureCode = null;
        failureMessage = null;

        clearLease();
    }

    /**
     * Marks an IN_APP notification available in the administrator
     * portal.
     */
    public void markInAppPublished() {
        requireInAppChannel();
        requireProcessingState();

        Instant now = Instant.now();

        deliveryStatus =
                NotificationDeliveryStatus.DELIVERED;

        if (sentAt == null) {
            sentAt = now;
        }

        deliveredAt = now;

        providerName = null;
        providerMessageId = null;
        providerResponseJson = null;

        nextAttemptAt = null;
        failureCode = null;
        failureMessage = null;

        clearLease();
    }

    /**
     * Schedules another external-provider attempt.
     */
    public void scheduleRetry(
            String code,
            String message,
            Instant retryAt,
            JsonNode providerResponse
    ) {
        requireExternalProviderChannel();
        requireProcessingState();

        String normalizedMessage =
                requireText(
                        message,
                        "Notification delivery failure message is required."
                );

        Instant now = Instant.now();

        if (retryAt == null || !retryAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Notification delivery retry time must be in the future."
            );
        }

        if (attemptCount < 1) {
            throw new IllegalStateException(
                    "A delivery attempt must be recorded before scheduling a retry."
            );
        }

        providerResponseJson =
                copyJson(providerResponse);

        failureCode = normalizeOptional(code);
        failureMessage = normalizedMessage;

        if (attemptCount >= maximumAttempts) {
            markFailed(
                    code,
                    normalizedMessage,
                    providerResponse
            );

            return;
        }

        deliveryStatus =
                NotificationDeliveryStatus.RETRY_PENDING;

        nextAttemptAt = retryAt;
        failedAt = null;

        clearLease();
    }

    /**
     * Marks a delivery permanently failed.
     */
    public void markFailed(
            String code,
            String message,
            JsonNode providerResponse
    ) {
        String normalizedMessage =
                requireText(
                        message,
                        "Notification delivery failure message is required."
                );

        Instant now = Instant.now();

        if (attemptCount < maximumAttempts) {
            attemptCount = maximumAttempts;
        }

        if (channel == NotificationChannel.IN_APP) {
            providerName = null;
            providerMessageId = null;
            providerResponseJson = null;
        } else {
            providerResponseJson =
                    copyJson(providerResponse);
        }

        deliveryStatus =
                NotificationDeliveryStatus.FAILED;

        failureCode = normalizeOptional(code);
        failureMessage = normalizedMessage;

        failedAt = now;
        nextAttemptAt = null;

        clearLease();
    }

    /**
     * Returns an abandoned delivery to the retry queue or permanently
     * fails it when no attempts remain.
     */
    public void recoverExpiredLease(
            Instant retryAt
    ) {
        if (
                deliveryStatus
                        != NotificationDeliveryStatus.PROCESSING
        ) {
            throw new IllegalStateException(
                    "Only a processing delivery can recover an expired lease."
            );
        }

        Instant now = Instant.now();

        if (
                lockExpiresAt == null
                        || lockExpiresAt.isAfter(now)
        ) {
            throw new IllegalStateException(
                    "Notification delivery lease has not expired."
            );
        }

        if (retryAt == null || !retryAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Notification delivery retry time must be in the future."
            );
        }

        if (attemptCount >= maximumAttempts) {
            markFailed(
                    "WORKER_LEASE_EXPIRED",
                    "Notification delivery worker lease expired after the maximum number of attempts.",
                    providerResponseJson
            );

            return;
        }

        deliveryStatus =
                NotificationDeliveryStatus.RETRY_PENDING;

        failureCode = "WORKER_LEASE_EXPIRED";
        failureMessage =
                "Notification delivery worker lease expired before processing completed.";

        nextAttemptAt = retryAt;

        clearLease();
    }

    // ================================================================
    // PROVIDER CALLBACK OPERATIONS
    // ================================================================

    public void markDelivered(
            Instant providerDeliveredAt
    ) {
        requireExternalProviderChannel();
        requireSuccessfulTransitionAllowed();

        Instant effectiveAt =
                providerDeliveredAt == null
                        ? Instant.now()
                        : providerDeliveredAt;

        if (sentAt == null) {
            sentAt = effectiveAt;
        }

        deliveredAt = effectiveAt;

        deliveryStatus =
                NotificationDeliveryStatus.DELIVERED;
    }

    public void markOpened(
            Instant providerOpenedAt
    ) {
        if (channel == NotificationChannel.IN_APP) {
            markRead(providerOpenedAt);
            return;
        }

        requireSuccessfulTransitionAllowed();

        Instant effectiveAt =
                providerOpenedAt == null
                        ? Instant.now()
                        : providerOpenedAt;

        if (sentAt == null) {
            sentAt = effectiveAt;
        }

        if (deliveredAt == null) {
            deliveredAt = effectiveAt;
        }

        openedAt = effectiveAt;

        deliveryStatus =
                NotificationDeliveryStatus.OPENED;
    }

    public void markClicked(
            Instant providerClickedAt
    ) {
        requireExternalProviderChannel();
        requireSuccessfulTransitionAllowed();

        Instant effectiveAt =
                providerClickedAt == null
                        ? Instant.now()
                        : providerClickedAt;

        if (sentAt == null) {
            sentAt = effectiveAt;
        }

        if (deliveredAt == null) {
            deliveredAt = effectiveAt;
        }

        clickedAt = effectiveAt;

        deliveryStatus =
                NotificationDeliveryStatus.CLICKED;
    }

    public void markBounced(
            String code,
            String message,
            Instant providerBouncedAt
    ) {
        requireExternalProviderChannel();

        Instant effectiveAt =
                providerBouncedAt == null
                        ? Instant.now()
                        : providerBouncedAt;

        deliveryStatus =
                NotificationDeliveryStatus.BOUNCED;

        bouncedAt = effectiveAt;
        failureCode = normalizeOptional(code);
        failureMessage =
                requireText(
                        message,
                        "Notification bounce message is required."
                );

        nextAttemptAt = null;

        clearLease();
    }

    // ================================================================
    // IN-APP OPERATIONS
    // ================================================================

    /**
     * Marks an administrator portal notification as read.
     */
    public void markRead(
            Instant readTime
    ) {
        requireInAppChannel();

        if (
                deliveryStatus
                        == NotificationDeliveryStatus.CANCELLED
                        || deliveryStatus
                        == NotificationDeliveryStatus.FAILED
                        || deliveryStatus
                        == NotificationDeliveryStatus.SUPPRESSED
        ) {
            throw new IllegalStateException(
                    "An unsuccessful in-app notification cannot be marked as read."
            );
        }

        Instant effectiveAt =
                readTime == null
                        ? Instant.now()
                        : readTime;

        if (sentAt == null) {
            sentAt = effectiveAt;
        }

        if (deliveredAt == null) {
            deliveredAt = effectiveAt;
        }

        readAt = effectiveAt;
        openedAt = effectiveAt;

        deliveryStatus =
                NotificationDeliveryStatus.OPENED;
    }

    /**
     * Restores an IN_APP notification to unread state.
     */
    public void markUnread() {
        requireInAppChannel();

        readAt = null;
        openedAt = null;

        if (deliveredAt != null) {
            deliveryStatus =
                    NotificationDeliveryStatus.DELIVERED;
        } else {
            deliveryStatus =
                    NotificationDeliveryStatus.PENDING;
        }
    }

    /**
     * Dismisses an IN_APP notification from the active portal list.
     */
    public void dismiss(
            Instant dismissalTime
    ) {
        requireInAppChannel();

        dismissedAt =
                dismissalTime == null
                        ? Instant.now()
                        : dismissalTime;
    }

    /**
     * Restores a dismissed IN_APP notification.
     */
    public void restoreDismissed() {
        requireInAppChannel();
        dismissedAt = null;
    }

    // ================================================================
    // CANCELLATION AND SUPPRESSION
    // ================================================================

    public void cancel(
            String reason
    ) {
        if (isTerminal()) {
            throw new IllegalStateException(
                    "Terminal notification delivery cannot be cancelled."
            );
        }

        deliveryStatus =
                NotificationDeliveryStatus.CANCELLED;

        cancelledAt = Instant.now();

        failureCode = null;
        failureMessage = normalizeOptional(reason);
        nextAttemptAt = null;

        clearLease();
    }

    public void suppress(
            String reason
    ) {
        String normalizedReason =
                requireText(
                        reason,
                        "Notification suppression reason is required."
                );

        if (
                deliveryStatus == NotificationDeliveryStatus.SENT
                        || deliveryStatus
                        == NotificationDeliveryStatus.DELIVERED
                        || deliveryStatus
                        == NotificationDeliveryStatus.OPENED
                        || deliveryStatus
                        == NotificationDeliveryStatus.CLICKED
        ) {
            throw new IllegalStateException(
                    "A submitted notification delivery cannot be suppressed."
            );
        }

        deliveryStatus =
                NotificationDeliveryStatus.SUPPRESSED;

        suppressedAt = Instant.now();
        suppressionReason = normalizedReason;

        nextAttemptAt = null;

        clearLease();
    }

    // ================================================================
    // STATE HELPERS
    // ================================================================

    @Transient
    public boolean isAvailableForClaim() {
        Instant now = Instant.now();

        if (
                deliveryStatus
                        == NotificationDeliveryStatus.PENDING
        ) {
            return true;
        }

        if (
                deliveryStatus
                        == NotificationDeliveryStatus.RETRY_PENDING
        ) {
            return nextAttemptAt != null
                    && !nextAttemptAt.isAfter(now);
        }

        return deliveryStatus
                == NotificationDeliveryStatus.PROCESSING
                && lockExpiresAt != null
                && !lockExpiresAt.isAfter(now);
    }

    @Transient
    public boolean isTerminal() {
        return deliveryStatus
                == NotificationDeliveryStatus.BOUNCED
                || deliveryStatus
                == NotificationDeliveryStatus.FAILED
                || deliveryStatus
                == NotificationDeliveryStatus.CANCELLED
                || deliveryStatus
                == NotificationDeliveryStatus.SUPPRESSED;
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

    @Transient
    public boolean hasExpiredLease() {
        return deliveryStatus
                == NotificationDeliveryStatus.PROCESSING
                && lockExpiresAt != null
                && !lockExpiresAt.isAfter(Instant.now());
    }

    // ================================================================
    // VALIDATION
    // ================================================================

    private void validateDelivery() {
        if (notificationEventId == null) {
            throw new IllegalArgumentException(
                    "Notification event ID is required."
            );
        }

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification delivery channel is required."
            );
        }

        recipientAddress =
                requireText(
                        recipientAddress,
                        "Notification recipient address is required."
                );

        normalizedRecipientAddress =
                requireText(
                        normalizedRecipientAddress,
                        "Normalized notification recipient address is required."
                );

        if (attemptCount > maximumAttempts) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt count cannot exceed maximum attempts."
            );
        }

        switch (channel) {
            case EMAIL -> validateEmailDelivery();
            case SMS -> validateSmsDelivery();
            case IN_APP -> validateInAppDelivery();
        }
    }

    private void validateEmailDelivery() {
        if (isBlank(subject)) {
            throw new IllegalArgumentException(
                    "Email notification subject is required."
            );
        }

        if (
                isBlank(renderedBodyText)
                        && isBlank(renderedBodyHtml)
        ) {
            throw new IllegalArgumentException(
                    "Email notification requires a text or HTML body."
            );
        }
    }

    private void validateSmsDelivery() {
        if (isBlank(renderedBodyText)) {
            throw new IllegalArgumentException(
                    "SMS notification text body is required."
            );
        }

        subject = null;
        renderedBodyHtml = null;
    }

    private void validateInAppDelivery() {
        if (recipientAdminUserId == null) {
            throw new IllegalArgumentException(
                    "In-app notification requires an administrator recipient."
            );
        }

        if (isBlank(subject)) {
            throw new IllegalArgumentException(
                    "In-app notification title is required."
            );
        }

        if (isBlank(renderedBodyText)) {
            throw new IllegalArgumentException(
                    "In-app notification message is required."
            );
        }

        renderedBodyHtml = null;

        providerName = null;
        providerMessageId = null;
        providerResponseJson = null;
    }

    private void requireProcessingState() {
        if (
                deliveryStatus
                        != NotificationDeliveryStatus.PROCESSING
        ) {
            throw new IllegalStateException(
                    "Notification delivery is not currently processing."
            );
        }
    }

    private void requireExternalProviderChannel() {
        if (channel == NotificationChannel.IN_APP) {
            throw new IllegalStateException(
                    "In-app notifications do not use an external delivery provider."
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

    private void requireSuccessfulTransitionAllowed() {
        if (
                deliveryStatus == NotificationDeliveryStatus.FAILED
                        || deliveryStatus
                        == NotificationDeliveryStatus.CANCELLED
                        || deliveryStatus
                        == NotificationDeliveryStatus.SUPPRESSED
        ) {
            throw new IllegalStateException(
                    "An unsuccessful terminal delivery cannot enter a successful state."
            );
        }
    }

    private void requireLeaseOwner(
            String workerId
    ) {
        String normalizedWorkerId =
                requireText(
                        workerId,
                        "Notification delivery worker ID is required."
                );

        if (
                lockedBy == null
                        || !lockedBy.equals(normalizedWorkerId)
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery is owned by another worker."
            );
        }

        if (
                lockExpiresAt == null
                        || !lockExpiresAt.isAfter(Instant.now())
        ) {
            throw new IllegalStateException(
                    "Notification delivery worker lease has expired."
            );
        }
    }

    // ================================================================
    // NORMALIZATION
    // ================================================================

    private void normalizeFields() {
        recipientAddress =
                requireText(
                        recipientAddress,
                        "Notification recipient address is required."
                );

        normalizedRecipientAddress =
                normalizeRecipientAddress(
                        recipientAddress,
                        channel
                );

        recipientName = normalizeOptional(recipientName);
        senderAddress = normalizeOptional(senderAddress);
        senderName = normalizeOptional(senderName);

        subject = normalizeOptional(subject);
        renderedBodyText = normalizeOptional(renderedBodyText);
        renderedBodyHtml = normalizeOptional(renderedBodyHtml);

        providerName = normalizeOptional(providerName);
        providerMessageId = normalizeOptional(providerMessageId);

        failureCode = normalizeOptional(failureCode);
        failureMessage = normalizeOptional(failureMessage);
        suppressionReason = normalizeOptional(suppressionReason);

        lockedBy = normalizeOptional(lockedBy);
    }

    private void normalizeMutableFields() {
        providerName = normalizeOptional(providerName);
        providerMessageId = normalizeOptional(providerMessageId);

        failureCode = normalizeOptional(failureCode);
        failureMessage = normalizeOptional(failureMessage);
        suppressionReason = normalizeOptional(suppressionReason);

        lockedBy = normalizeOptional(lockedBy);
    }

    private String normalizeRecipientAddress(
            String address,
            NotificationChannel deliveryChannel
    ) {
        String normalizedAddress =
                requireText(
                        address,
                        "Notification recipient address is required."
                );

        if (deliveryChannel == null) {
            return normalizedAddress;
        }

        return switch (deliveryChannel) {
            case EMAIL ->
                    normalizedAddress.toLowerCase(Locale.ROOT);

            case SMS -> {
                String digitsOnly =
                        normalizedAddress.replaceAll("\\D", "");

                if (digitsOnly.isBlank()) {
                    throw new IllegalArgumentException(
                            "SMS recipient telephone number is invalid."
                    );
                }

                yield digitsOnly;
            }

            case IN_APP -> {
                if (recipientAdminUserId == null) {
                    throw new IllegalArgumentException(
                            "In-app notification administrator ID is required."
                    );
                }

                yield recipientAdminUserId
                        .toString()
                        .toLowerCase(Locale.ROOT);
            }
        };
    }

    private JsonNode copyJson(
            JsonNode source
    ) {
        return source == null
                ? null
                : source.deepCopy();
    }

    private void clearLease() {
        lockedAt = null;
        lockedBy = null;
        lockExpiresAt = null;
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

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }
}