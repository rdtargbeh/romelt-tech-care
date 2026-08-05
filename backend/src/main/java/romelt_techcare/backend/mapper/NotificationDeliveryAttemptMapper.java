package romelt_techcare.backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptResponse;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptSummaryResponse;
import romelt_techcare.backend.entity.NotificationDeliveryAttempt;

import java.time.Duration;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts completed provider-attempt commands into immutable
 * persistence records and maps them into administrator responses.
 * ================================================================
 */
@Component
public class NotificationDeliveryAttemptMapper {

    public NotificationDeliveryAttempt toEntity(
            NotificationDeliveryAttemptCreateRequest request
    ) {
        requireRequest(request);
        validateRequest(request);

        Long durationMs =
                request.durationMs() != null
                        ? request.durationMs()
                        : calculateDurationMillis(
                        request.startedAt(),
                        request.completedAt()
                );

        return NotificationDeliveryAttempt.builder()
                .notificationDeliveryId(
                        request.notificationDeliveryId()
                )
                .attemptNumber(
                        request.attemptNumber()
                )
                .providerName(
                        normalizeOptional(request.providerName())
                )
                .providerRequestId(
                        normalizeOptional(request.providerRequestId())
                )
                .providerMessageId(
                        normalizeOptional(request.providerMessageId())
                )
                .providerStatus(
                        normalizeOptional(request.providerStatus())
                )
                .startedAt(request.startedAt())
                .completedAt(request.completedAt())
                .durationMs(durationMs)
                .success(request.success())
                .retryable(
                        request.success()
                                ? false
                                : request.retryable()
                )
                .httpStatus(request.httpStatus())
                .smtpStatus(
                        normalizeOptional(request.smtpStatus())
                )
                .failureCode(
                        request.success()
                                ? null
                                : normalizeOptional(
                                request.failureCode()
                        )
                )
                .failureMessage(
                        request.success()
                                ? null
                                : normalizeRequired(
                                request.failureMessage(),
                                "Failed notification delivery attempts require a failure message."
                        )
                )
                .providerRequestJson(
                        copyJson(request.providerRequestJson())
                )
                .providerResponseJson(
                        copyJson(request.providerResponseJson())
                )
                .build();
    }

    public NotificationDeliveryAttemptResponse toResponse(
            NotificationDeliveryAttempt attempt
    ) {
        requireEntity(attempt);

        return new NotificationDeliveryAttemptResponse(
                attempt.getNotificationDeliveryAttemptId(),
                attempt.getNotificationDeliveryId(),
                attempt.getAttemptNumber(),
                attempt.getProviderName(),
                attempt.getProviderRequestId(),
                attempt.getProviderMessageId(),
                attempt.getProviderStatus(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getDurationMs(),
                attempt.isSuccess(),
                attempt.isRetryable(),
                attempt.getHttpStatus(),
                attempt.getSmtpStatus(),
                attempt.getFailureCode(),
                attempt.getFailureMessage(),
                copyJson(attempt.getProviderRequestJson()),
                copyJson(attempt.getProviderResponseJson()),
                attempt.getCreatedAt(),
                attempt.getRowVersion()
        );
    }

    public NotificationDeliveryAttemptSummaryResponse toSummaryResponse(
            NotificationDeliveryAttempt attempt
    ) {
        requireEntity(attempt);

        return new NotificationDeliveryAttemptSummaryResponse(
                attempt.getNotificationDeliveryAttemptId(),
                attempt.getNotificationDeliveryId(),
                attempt.getAttemptNumber(),
                attempt.getProviderName(),
                attempt.getProviderMessageId(),
                attempt.getProviderStatus(),
                attempt.isSuccess(),
                attempt.isRetryable(),
                attempt.getHttpStatus(),
                attempt.getSmtpStatus(),
                attempt.getFailureCode(),
                attempt.getDurationMs(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getCreatedAt()
        );
    }

    private void validateRequest(
            NotificationDeliveryAttemptCreateRequest request
    ) {
        if (request.notificationDeliveryId() == null) {
            throw new IllegalArgumentException(
                    "Notification delivery ID is required."
            );
        }

        if (
                request.attemptNumber() == null
                        || request.attemptNumber() < 1
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt number must be greater than zero."
            );
        }

        if (request.startedAt() == null) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt start time is required."
            );
        }

        if (!request.hasValidCompletion()) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt completion time cannot be before its start time."
            );
        }

        if (!request.hasValidOutcome()) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt outcome is invalid."
            );
        }

        if (!request.hasValidProviderJson()) {
            throw new IllegalArgumentException(
                    "Notification provider request and response data must be JSON objects."
            );
        }

        if (
                request.httpStatus() != null
                        && (
                        request.httpStatus() < 100
                                || request.httpStatus() > 599
                )
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery HTTP status must be between 100 and 599."
            );
        }

        if (
                request.durationMs() != null
                        && request.durationMs() < 0
        ) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt duration cannot be negative."
            );
        }
    }

    private Long calculateDurationMillis(
            java.time.Instant startedAt,
            java.time.Instant completedAt
    ) {
        if (startedAt == null || completedAt == null) {
            return null;
        }

        return Duration
                .between(startedAt, completedAt)
                .toMillis();
    }

    private JsonNode copyJson(
            JsonNode source
    ) {
        return source == null
                ? null
                : source.deepCopy();
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

    private void requireRequest(
            NotificationDeliveryAttemptCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt request is required."
            );
        }
    }

    private void requireEntity(
            NotificationDeliveryAttempt attempt
    ) {
        if (attempt == null) {
            throw new IllegalArgumentException(
                    "Notification delivery attempt is required."
            );
        }
    }
}