package romelt_techcare.backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.NotificationEventCreateRequest;
import romelt_techcare.backend.dto.NotificationEventResponse;
import romelt_techcare.backend.dto.NotificationEventSummaryResponse;
import romelt_techcare.backend.entity.NotificationEvent;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationPriority;
import romelt_techcare.backend.enums.NotificationRecipientType;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts internal notification-event requests into transactional
 * outbox entities and maps persisted events into administrator
 * responses.
 * ================================================================
 */
@Component
public class NotificationEventMapper {

    private static final Pattern NON_DIGIT_PATTERN =
            Pattern.compile("\\D");

    public NotificationEvent toEntity(
            NotificationEventCreateRequest request
    ) {
        requireRequest(request);
        validateRequest(request);

        String recipientEmail =
                normalizeOptional(request.recipientEmail());

        String recipientPhone =
                normalizeOptional(request.recipientPhone());

        Instant now = Instant.now();

        Instant availableAt =
                request.availableAt() != null
                        ? request.availableAt()
                        : request.scheduledFor() != null
                        ? request.scheduledFor()
                        : now;

        return NotificationEvent.builder()
                .eventType(request.eventType())
                .notificationCategory(
                        request.notificationCategory() == null
                                ? NotificationCategory.TRANSACTIONAL
                                : request.notificationCategory()
                )
                .channel(request.channel())
                .resourceType(request.resourceType())
                .resourceId(request.resourceId())
                .correlationKey(
                        normalizeOptional(request.correlationKey())
                )
                .idempotencyKey(
                        normalizeOptional(request.idempotencyKey())
                )
                .recipientType(
                        request.recipientType() == null
                                ? NotificationRecipientType.CUSTOMER
                                : request.recipientType()
                )
                .customerId(request.customerId())
                .recipientAdminUserId(
                        request.recipientAdminUserId()
                )
                .recipientEmail(recipientEmail)
                .normalizedRecipientEmail(
                        normalizeOptionalEmail(recipientEmail)
                )
                .recipientPhone(recipientPhone)
                .normalizedRecipientPhone(
                        normalizeOptionalPhone(recipientPhone)
                )
                .recipientName(
                        normalizeOptional(request.recipientName())
                )
                .notificationTemplateId(
                        request.notificationTemplateId()
                )
                .templateKey(
                        normalizeTemplateKey(request.templateKey())
                )
                .templateVersion(
                        request.templateVersion() == null
                                ? 1
                                : request.templateVersion()
                )
                .templateLocale(
                        normalizeLocale(request.templateLocale())
                )
                .templateDataJson(
                        copyTemplateData(request.templateDataJson())
                )
                .eventStatus(NotificationEventStatus.PENDING)
                .priority(
                        request.priority() == null
                                ? NotificationPriority.NORMAL
                                : request.priority()
                )
                .scheduledFor(request.scheduledFor())
                .availableAt(availableAt)
                .attemptCount(0)
                .maximumAttempts(
                        request.maximumAttempts() == null
                                ? 5
                                : request.maximumAttempts()
                )
                .createdByAdminUserId(
                        request.createdByAdminUserId()
                )
                .build();
    }

    public NotificationEventResponse toResponse(
            NotificationEvent event
    ) {
        requireEntity(event);

        return new NotificationEventResponse(
                event.getNotificationEventId(),
                event.getEventType(),
                event.getNotificationCategory(),
                event.getChannel(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCorrelationKey(),
                event.getIdempotencyKey(),
                event.getRecipientType(),
                event.getCustomerId(),
                event.getRecipientAdminUserId(),
                event.getRecipientEmail(),
                event.getRecipientPhone(),
                event.getRecipientName(),
                event.getNotificationTemplateId(),
                event.getTemplateKey(),
                event.getTemplateVersion(),
                event.getTemplateLocale(),
                copyTemplateData(event.getTemplateDataJson()),
                event.getEventStatus(),
                event.getPriority(),
                event.getScheduledFor(),
                event.getAvailableAt(),
                event.getAttemptCount(),
                event.getMaximumAttempts(),
                event.getNextAttemptAt(),
                event.getLastAttemptedAt(),
                event.getProcessingStartedAt(),
                event.getProcessedAt(),
                event.getFailedAt(),
                event.getCancelledAt(),
                event.getFailureCode(),
                event.getFailureReason(),
                event.getLockedAt(),
                event.getLockedBy(),
                event.getLockExpiresAt(),
                event.getCreatedByAdminUserId(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getRowVersion()
        );
    }

    public NotificationEventSummaryResponse toSummaryResponse(
            NotificationEvent event
    ) {
        requireEntity(event);

        return new NotificationEventSummaryResponse(
                event.getNotificationEventId(),
                event.getEventType(),
                event.getNotificationCategory(),
                event.getChannel(),
                event.getResourceType(),
                event.getResourceId(),
                event.getRecipientType(),
                event.getRecipientAdminUserId(),
                event.getRecipientName(),
                event.getRecipientEmail(),
                event.getRecipientPhone(),
                event.getTemplateKey(),
                event.getTemplateVersion(),
                event.getEventStatus(),
                event.getPriority(),
                event.getAttemptCount(),
                event.getMaximumAttempts(),
                event.getAvailableAt(),
                event.getNextAttemptAt(),
                event.getProcessedAt(),
                event.getFailedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public String normalizeOptionalEmail(
            String value
    ) {
        String normalized = normalizeOptional(value);

        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    public String normalizeOptionalPhone(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        String digitsOnly =
                NON_DIGIT_PATTERN
                        .matcher(normalized)
                        .replaceAll("");

        return digitsOnly.isBlank()
                ? null
                : digitsOnly;
    }

    private void validateRequest(
            NotificationEventCreateRequest request
    ) {
        if (request.channel() == null) {
            throw new IllegalArgumentException(
                    "Notification channel is required."
            );
        }

        if (!request.hasRecipient()) {
            throw new IllegalArgumentException(
                    "A notification recipient is required."
            );
        }

        if (!request.isCustomerRecipientValid()) {
            throw new IllegalArgumentException(
                    "Customer notifications require a customer, email, or telephone recipient."
            );
        }

        if (!request.isAdminRecipientValid()) {
            throw new IllegalArgumentException(
                    "Administrator notifications require an administrator or email recipient."
            );
        }

        if (!request.hasValidChannelRecipient()) {
            throw new IllegalArgumentException(
                    resolveChannelRecipientError(request.channel())
            );
        }

        if (!request.hasObjectTemplateData()) {
            throw new IllegalArgumentException(
                    "Notification template data must be a JSON object."
            );
        }

        if (!request.hasValidSchedule()) {
            throw new IllegalArgumentException(
                    "Notification availability cannot occur before the scheduled time."
            );
        }

        if (
                request.templateVersion() != null
                        && request.templateVersion() < 1
        ) {
            throw new IllegalArgumentException(
                    "Notification template version must be greater than zero."
            );
        }

        if (
                request.maximumAttempts() != null
                        && request.maximumAttempts() < 1
        ) {
            throw new IllegalArgumentException(
                    "Maximum notification attempts must be greater than zero."
            );
        }
    }

    private String resolveChannelRecipientError(
            NotificationChannel channel
    ) {
        return switch (channel) {
            case EMAIL ->
                    "Email notifications require a recipient email address.";

            case SMS ->
                    "SMS notifications require a recipient telephone number.";

            case IN_APP ->
                    "In-app notifications require an administrator recipient ID.";
        };
    }

    private JsonNode copyTemplateData(
            JsonNode source
    ) {
        return source == null
                ? JsonNodeFactory.instance.objectNode()
                : source.deepCopy();
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

    private void requireRequest(
            NotificationEventCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification event request is required."
            );
        }
    }

    private void requireEntity(
            NotificationEvent event
    ) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Notification event is required."
            );
        }
    }
}