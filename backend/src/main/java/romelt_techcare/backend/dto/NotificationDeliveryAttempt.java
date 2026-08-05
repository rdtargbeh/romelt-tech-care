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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT ENTITY
 * ================================================================
 *
 * Purpose:
 * Preserves one immutable provider interaction performed for a
 * notification delivery.
 *
 * Responsibilities:
 * - Links the attempt to its parent delivery.
 * - Stores the sequential attempt number.
 * - Preserves provider request and response identifiers.
 * - Tracks provider status and transport-specific status values.
 * - Tracks start, completion, and duration.
 * - Records success, retryability, and failure details.
 * - Stores sanitized provider request and response snapshots.
 * - Supports optimistic locking.
 *
 * Important:
 * This entity represents provider history and should not normally be
 * updated after completion.
 *
 * Security:
 * providerRequestJson and providerResponseJson must never contain:
 * - API keys
 * - Authorization headers
 * - Passwords
 * - Access tokens
 * - Refresh tokens
 * - SMTP credentials
 * - Payment information
 * - Private cryptographic material
 * ================================================================
 */
@Entity
@Table(
        name = "notification_delivery_attempts",
        indexes = {
                @Index(
                        name = "idx_notification_attempt_delivery",
                        columnList = "notification_delivery_id, attempt_number"
                ),
                @Index(
                        name = "idx_notification_attempt_provider",
                        columnList = "provider_name, created_at"
                ),
                @Index(
                        name = "idx_notification_attempt_success",
                        columnList = "success, created_at"
                ),
                @Index(
                        name = "idx_notification_attempt_retryable",
                        columnList = "retryable, created_at"
                ),
                @Index(
                        name = "idx_notification_attempt_started",
                        columnList = "started_at"
                ),
                @Index(
                        name = "idx_notification_attempt_provider_message",
                        columnList = "provider_message_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_attempt_number",
                        columnNames = {
                                "notification_delivery_id",
                                "attempt_number"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_delivery_attempt_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationDeliveryAttemptId;

    @Column(
            name = "notification_delivery_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationDeliveryId;

    @Column(
            name = "attempt_number",
            nullable = false,
            updatable = false
    )
    private Integer attemptNumber;

    @Column(
            name = "provider_name",
            length = 100,
            updatable = false
    )
    private String providerName;

    @Column(
            name = "provider_request_id",
            length = 255,
            updatable = false
    )
    private String providerRequestId;

    @Column(
            name = "provider_message_id",
            length = 255,
            updatable = false
    )
    private String providerMessageId;

    @Column(
            name = "provider_status",
            length = 100,
            updatable = false
    )
    private String providerStatus;

    @Column(
            name = "started_at",
            nullable = false,
            updatable = false
    )
    private Instant startedAt;

    @Column(
            name = "completed_at",
            updatable = false
    )
    private Instant completedAt;

    @Column(
            name = "duration_ms",
            updatable = false
    )
    private Long durationMs;

    @Column(
            name = "success",
            nullable = false,
            updatable = false
    )
    private boolean success;

    @Builder.Default
    @Column(
            name = "retryable",
            nullable = false,
            updatable = false,
            columnDefinition = "boolean default false"
    )
    private boolean retryable = false;

    @Column(
            name = "http_status",
            updatable = false
    )
    private Integer httpStatus;

    @Column(
            name = "smtp_status",
            length = 50,
            updatable = false
    )
    private String smtpStatus;

    @Column(
            name = "failure_code",
            length = 100,
            updatable = false
    )
    private String failureCode;

    @Lob
    @Column(
            name = "failure_message",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String failureMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "provider_request_json",
            columnDefinition = "jsonb",
            updatable = false
    )
    private JsonNode providerRequestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "provider_response_json",
            columnDefinition = "jsonb",
            updatable = false
    )
    private JsonNode providerResponseJson;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

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
        validateAttempt();

        if (startedAt == null) {
            startedAt = now;
        }

        if (completedAt != null && durationMs == null) {
            durationMs = calculateDurationMillis(
                    startedAt,
                    completedAt
            );
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (rowVersion == null) {
            rowVersion = 0L;
        }
    }

    private void normalizeFields() {
        providerName = normalizeOptional(providerName);
        providerRequestId = normalizeOptional(providerRequestId);
        providerMessageId = normalizeOptional(providerMessageId);
        providerStatus = normalizeOptional(providerStatus);
        smtpStatus = normalizeOptional(smtpStatus);
        failureCode = normalizeOptional(failureCode);
        failureMessage = normalizeOptional(failureMessage);

        providerRequestJson = copyJson(providerRequestJson);
        providerResponseJson = copyJson(providerResponseJson);
    }

    private void validateAttempt() {
        if (notificationDeliveryId == null) {
            throw new IllegalArgumentException(
                    "Notification delivery ID is required."
            );
        }

        if (attemptNumber == null || attemptNumber < 1) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt number must be greater than zero."
            );
        }

        if (
                completedAt != null
                        && startedAt != null
                        && completedAt.isBefore(startedAt)
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt completion time cannot be before its start time."
            );
        }

        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt duration cannot be negative."
            );
        }

        if (
                httpStatus != null
                        && (
                        httpStatus < 100
                                || httpStatus > 599
                )
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery HTTP status must be between 100 and 599."
            );
        }

        if (success) {
            retryable = false;
            failureCode = null;
            failureMessage = null;
        } else if (failureMessage == null) {
            throw new IllegalArgumentException(
                    "Failed notification delivery attempts require a failure message."
            );
        }

        if (
                providerRequestJson != null
                        && !providerRequestJson.isObject()
        ) {
            throw new IllegalArgumentException(
                    "Notification provider request data must be a JSON object."
            );
        }

        if (
                providerResponseJson != null
                        && !providerResponseJson.isObject()
        ) {
            throw new IllegalArgumentException(
                    "Notification provider response data must be a JSON object."
            );
        }
    }

    private Long calculateDurationMillis(
            Instant start,
            Instant end
    ) {
        if (start == null || end == null) {
            return null;
        }

        return Duration
                .between(start, end)
                .toMillis();
    }

    private JsonNode copyJson(
            JsonNode source
    ) {
        return source == null
                ? null
                : source.deepCopy();
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