package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationPriority;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT ENTITY
 * ================================================================
 *
 * Purpose:
 * Represents one transactional notification-outbox event created by
 * a booking, contact inquiry, customer review, review invitation,
 * customer action, administrator action, or system process.
 *
 * Responsibilities:
 * - Preserves the business event requiring notification.
 * - Identifies the intended EMAIL, SMS, or IN_APP channel.
 * - Links the event to its customer, administrator, and resource.
 * - Preserves readable and normalized recipient snapshots.
 * - Identifies the exact notification template and version.
 * - Stores structured template-rendering data.
 * - Supports delayed and scheduled processing.
 * - Supports worker claiming through an expiring processing lease.
 * - Supports retry scheduling and permanent failure handling.
 * - Supports idempotency and correlation.
 * - Tracks administrator attribution and optimistic locking.
 *
 * IN_APP:
 * Administrator portal notifications use:
 * - channel = IN_APP
 * - recipientType = ADMIN
 * - recipientAdminUserId = the receiving administrator
 *
 * Transactional-outbox behavior:
 * Business services persist this entity in the same transaction as
 * the related booking, contact, review, or customer operation.
 *
 * Security:
 * templateDataJson must never contain passwords, authentication
 * tokens, plain review-invitation tokens, payment information,
 * credentials, private keys, or account-recovery secrets.
 * ================================================================
 */
@Entity
@Table(
        name = "notification_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_event_idempotency",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_event_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationEventId;

    // ================================================================
    // EVENT IDENTITY
    // ================================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 80
    )
    private NotificationEventType eventType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_category",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'TRANSACTIONAL'"
    )
    private NotificationCategory notificationCategory =
            NotificationCategory.TRANSACTIONAL;

    /**
     * Channel through which this event must be delivered.
     *
     * This is required because the same template key may have separate
     * EMAIL, SMS, and IN_APP template versions.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "channel",
            nullable = false,
            length = 20
    )
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            length = 60
    )
    private NotificationResourceType resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(
            name = "correlation_key",
            length = 255
    )
    private String correlationKey;

    @Column(
            name = "idempotency_key",
            unique = true,
            length = 255
    )
    private String idempotencyKey;

    // ================================================================
    // RECIPIENT IDENTITY
    // ================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "recipient_type",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'CUSTOMER'"
    )
    private NotificationRecipientType recipientType =
            NotificationRecipientType.CUSTOMER;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "recipient_admin_user_id")
    private UUID recipientAdminUserId;

    @Column(
            name = "recipient_email",
            length = 254
    )
    private String recipientEmail;

    @Column(
            name = "normalized_recipient_email",
            length = 254
    )
    private String normalizedRecipientEmail;

    @Column(
            name = "recipient_phone",
            length = 40
    )
    private String recipientPhone;

    @Column(
            name = "normalized_recipient_phone",
            length = 30
    )
    private String normalizedRecipientPhone;

    @Column(
            name = "recipient_name",
            length = 180
    )
    private String recipientName;

    // ================================================================
    // TEMPLATE IDENTITY AND DATA
    // ================================================================

    @Column(name = "notification_template_id")
    private UUID notificationTemplateId;

    @Column(
            name = "template_key",
            nullable = false,
            length = 160
    )
    private String templateKey;

    @Builder.Default
    @Column(
            name = "template_version",
            nullable = false,
            columnDefinition = "integer default 1"
    )
    private Integer templateVersion = 1;

    @Builder.Default
    @Column(
            name = "template_locale",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'en-US'"
    )
    private String templateLocale = "en-US";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "template_data_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode templateDataJson =
            JsonNodeFactory.instance.objectNode();

    // ================================================================
    // PROCESSING STATE
    // ================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_status",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'PENDING'"
    )
    private NotificationEventStatus eventStatus =
            NotificationEventStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "priority",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'NORMAL'"
    )
    private NotificationPriority priority =
            NotificationPriority.NORMAL;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(
            name = "available_at",
            nullable = false
    )
    private Instant availableAt;

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

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(
            name = "failure_code",
            length = 100
    )
    private String failureCode;

    @Lob
    @Column(
            name = "failure_reason",
            columnDefinition = "TEXT"
    )
    private String failureReason;

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
    // ADMINISTRATOR ATTRIBUTION
    // ================================================================

    @Column(name = "created_by_admin_user_id")
    private UUID createdByAdminUserId;

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
        validateEvent();

        if (availableAt == null) {
            availableAt =
                    scheduledFor == null
                            ? now
                            : scheduledFor;
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
        applyDefaults();
        normalizeFields();
        validateEvent();

        updatedAt = Instant.now();
    }

    private void applyDefaults() {
        if (notificationCategory == null) {
            notificationCategory =
                    NotificationCategory.TRANSACTIONAL;
        }

        if (recipientType == null) {
            recipientType =
                    NotificationRecipientType.CUSTOMER;
        }

        if (eventStatus == null) {
            eventStatus =
                    NotificationEventStatus.PENDING;
        }

        if (priority == null) {
            priority =
                    NotificationPriority.NORMAL;
        }

        if (templateVersion == null || templateVersion < 1) {
            templateVersion = 1;
        }

        if (attemptCount == null || attemptCount < 0) {
            attemptCount = 0;
        }

        if (maximumAttempts == null || maximumAttempts < 1) {
            maximumAttempts = 5;
        }

        if (templateDataJson == null) {
            templateDataJson =
                    JsonNodeFactory.instance.objectNode();
        }
    }

    private void validateEvent() {
        if (eventType == null) {
            throw new IllegalArgumentException(
                    "Notification event type is required."
            );
        }

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification channel is required."
            );
        }

        if (resourceType == null) {
            throw new IllegalArgumentException(
                    "Notification resource type is required."
            );
        }

        if (recipientType == null) {
            throw new IllegalArgumentException(
                    "Notification recipient type is required."
            );
        }

        if (!hasRecipient()) {
            throw new IllegalArgumentException(
                    "Notification recipient information is required."
            );
        }

        if (
                recipientType == NotificationRecipientType.ADMIN
                        && recipientAdminUserId == null
                        && recipientEmail == null
        ) {
            throw new IllegalArgumentException(
                    "Administrator notifications require an administrator ID or email address."
            );
        }

        if (
                channel == NotificationChannel.EMAIL
                        && recipientEmail == null
        ) {
            throw new IllegalArgumentException(
                    "Email notifications require a recipient email address."
            );
        }

        if (
                channel == NotificationChannel.SMS
                        && recipientPhone == null
        ) {
            throw new IllegalArgumentException(
                    "SMS notifications require a recipient telephone number."
            );
        }

        if (
                channel == NotificationChannel.IN_APP
                        && recipientAdminUserId == null
        ) {
            throw new IllegalArgumentException(
                    "In-app notifications require an administrator recipient ID."
            );
        }

        if (
                templateDataJson == null
                        || !templateDataJson.isObject()
        ) {
            throw new IllegalArgumentException(
                    "Notification template data must be a JSON object."
            );
        }

        if (
                scheduledFor != null
                        && availableAt != null
                        && availableAt.isBefore(scheduledFor)
        ) {
            throw new IllegalArgumentException(
                    "Notification availability cannot occur before its scheduled time."
            );
        }

        if (attemptCount > maximumAttempts) {
            throw new IllegalArgumentException(
                    "Notification attempt count cannot exceed maximum attempts."
            );
        }
    }

    // ================================================================
    // WORKER OPERATIONS
    // ================================================================

    public void claim(
            String workerId,
            Duration leaseDuration
    ) {
        String normalizedWorkerId =
                normalizeRequired(
                        workerId,
                        "Notification worker ID is required."
                );

        if (
                leaseDuration == null
                        || leaseDuration.isZero()
                        || leaseDuration.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "Notification worker lease duration must be positive."
            );
        }

        if (!isAvailableForClaim()) {
            throw new IllegalStateException(
                    "Notification event is not available for processing."
            );
        }

        Instant now = Instant.now();

        eventStatus = NotificationEventStatus.PROCESSING;
        processingStartedAt = now;
        lastAttemptedAt = now;

        lockedAt = now;
        lockedBy = normalizedWorkerId;
        lockExpiresAt = now.plus(leaseDuration);

        nextAttemptAt = null;
        failureCode = null;
        failureReason = null;
        failedAt = null;
    }

    public void renewLease(
            String workerId,
            Duration leaseDuration
    ) {
        if (eventStatus != NotificationEventStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only a processing notification event can renew its lease."
            );
        }

        String normalizedWorkerId =
                normalizeRequired(
                        workerId,
                        "Notification worker ID is required."
                );

        if (
                lockedBy == null
                        || !lockedBy.equals(normalizedWorkerId)
        ) {
            throw new IllegalArgumentException(
                    "Notification worker does not own this event lease."
            );
        }

        if (
                leaseDuration == null
                        || leaseDuration.isZero()
                        || leaseDuration.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "Notification worker lease duration must be positive."
            );
        }

        Instant now = Instant.now();

        lockedAt = now;
        lockExpiresAt = now.plus(leaseDuration);
    }

    public void markProcessed() {
        requireProcessingState();

        eventStatus = NotificationEventStatus.PROCESSED;
        processedAt = Instant.now();

        failedAt = null;
        nextAttemptAt = null;
        failureCode = null;
        failureReason = null;

        clearLease();
    }

    public void scheduleRetry(
            String code,
            String reason,
            Instant retryAt
    ) {
        requireProcessingState();

        String normalizedReason =
                normalizeRequired(
                        reason,
                        "Notification failure reason is required."
                );

        Instant now = Instant.now();

        if (retryAt == null || !retryAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Notification retry time must be in the future."
            );
        }

        incrementAttemptCount();

        if (attemptCount >= maximumAttempts) {
            markFailed(code, normalizedReason);
            return;
        }

        eventStatus =
                NotificationEventStatus.RETRY_PENDING;

        nextAttemptAt = retryAt;
        failureCode = normalizeOptional(code);
        failureReason = normalizedReason;

        processedAt = null;
        failedAt = null;

        clearLease();
    }

    public void markFailed(
            String code,
            String reason
    ) {
        String normalizedReason =
                normalizeRequired(
                        reason,
                        "Notification failure reason is required."
                );

        Instant now = Instant.now();

        if (attemptCount < maximumAttempts) {
            attemptCount = maximumAttempts;
        }

        eventStatus = NotificationEventStatus.FAILED;
        lastAttemptedAt = now;
        failedAt = now;

        failureCode = normalizeOptional(code);
        failureReason = normalizedReason;

        nextAttemptAt = null;
        processedAt = null;

        clearLease();
    }

    public void cancel(
            String reason
    ) {
        if (isTerminal()) {
            throw new IllegalStateException(
                    "Terminal notification events cannot be cancelled."
            );
        }

        eventStatus = NotificationEventStatus.CANCELLED;
        cancelledAt = Instant.now();

        failureCode = null;
        failureReason = normalizeOptional(reason);
        nextAttemptAt = null;

        clearLease();
    }

    public void recoverExpiredLease(
            Instant retryAt,
            String reason
    ) {
        if (eventStatus != NotificationEventStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only a processing event can recover an expired lease."
            );
        }

        Instant now = Instant.now();

        if (
                lockExpiresAt == null
                        || lockExpiresAt.isAfter(now)
        ) {
            throw new IllegalStateException(
                    "Notification event lease has not expired."
            );
        }

        scheduleRetry(
                "WORKER_LEASE_EXPIRED",
                normalizeOptional(reason) == null
                        ? "Notification worker lease expired before processing completed."
                        : reason,
                retryAt
        );
    }

    // ================================================================
    // TEMPLATE OPERATIONS
    // ================================================================

    public void assignTemplate(
            UUID templateId,
            String key,
            NotificationChannel templateChannel,
            Integer version,
            String locale
    ) {
        if (templateId == null) {
            throw new IllegalArgumentException(
                    "Notification template ID is required."
            );
        }

        if (templateChannel == null) {
            throw new IllegalArgumentException(
                    "Notification template channel is required."
            );
        }

        if (channel != templateChannel) {
            throw new IllegalArgumentException(
                    "Notification event channel does not match the selected template channel."
            );
        }

        if (version == null || version < 1) {
            throw new IllegalArgumentException(
                    "Notification template version must be greater than zero."
            );
        }

        notificationTemplateId = templateId;
        templateKey = normalizeTemplateKey(key);
        templateVersion = version;
        templateLocale = normalizeLocale(locale);
    }

    // ================================================================
    // STATE HELPERS
    // ================================================================

    @Transient
    public boolean isAvailableForClaim() {
        Instant now = Instant.now();

        if (eventStatus == NotificationEventStatus.PENDING) {
            return availableAt == null
                    || !availableAt.isAfter(now);
        }

        if (eventStatus == NotificationEventStatus.RETRY_PENDING) {
            return nextAttemptAt != null
                    && !nextAttemptAt.isAfter(now);
        }

        return eventStatus == NotificationEventStatus.PROCESSING
                && lockExpiresAt != null
                && !lockExpiresAt.isAfter(now);
    }

    @Transient
    public boolean isTerminal() {
        return eventStatus == NotificationEventStatus.PROCESSED
                || eventStatus == NotificationEventStatus.FAILED
                || eventStatus == NotificationEventStatus.CANCELLED;
    }

    @Transient
    public boolean hasExpiredLease() {
        return eventStatus == NotificationEventStatus.PROCESSING
                && lockExpiresAt != null
                && !lockExpiresAt.isAfter(Instant.now());
    }

    private void requireProcessingState() {
        if (eventStatus != NotificationEventStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Notification event is not currently processing."
            );
        }
    }

    private boolean hasRecipient() {
        return customerId != null
                || recipientAdminUserId != null
                || recipientEmail != null
                || recipientPhone != null;
    }

    private void clearLease() {
        lockedAt = null;
        lockedBy = null;
        lockExpiresAt = null;
    }

    private void incrementAttemptCount() {
        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (maximumAttempts == null || maximumAttempts < 1) {
            maximumAttempts = 5;
        }

        if (attemptCount < maximumAttempts) {
            attemptCount++;
        }

        lastAttemptedAt = Instant.now();
    }

    // ================================================================
    // NORMALIZATION
    // ================================================================

    private void normalizeFields() {
        recipientEmail =
                normalizeOptional(recipientEmail);

        normalizedRecipientEmail =
                recipientEmail == null
                        ? null
                        : recipientEmail.toLowerCase(Locale.ROOT);

        recipientPhone =
                normalizeOptional(recipientPhone);

        if (recipientPhone == null) {
            normalizedRecipientPhone = null;
        } else {
            String digitsOnly =
                    recipientPhone.replaceAll("\\D", "");

            normalizedRecipientPhone =
                    digitsOnly.isBlank()
                            ? null
                            : digitsOnly;
        }

        recipientName = normalizeOptional(recipientName);
        correlationKey = normalizeOptional(correlationKey);
        idempotencyKey = normalizeOptional(idempotencyKey);

        templateKey = normalizeTemplateKey(templateKey);
        templateLocale = normalizeLocale(templateLocale);

        failureCode = normalizeOptional(failureCode);
        failureReason = normalizeOptional(failureReason);
        lockedBy = normalizeOptional(lockedBy);
    }

    private String normalizeTemplateKey(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Notification template key is required."
                );

        return normalized
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }

    private String normalizeLocale(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return "en-US";
        }

        String[] parts =
                normalized
                        .replace('_', '-')
                        .split("-", 3);

        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }

        return parts[0].toLowerCase(Locale.ROOT)
                + "-"
                + parts[1].toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(
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