package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.NotificationSuppressionCreateRequest;
import romelt_techcare.backend.dto.NotificationSuppressionResponse;
import romelt_techcare.backend.dto.NotificationSuppressionSummaryResponse;
import romelt_techcare.backend.dto.NotificationSuppressionUpdateRequest;
import romelt_techcare.backend.entity.NotificationSuppression;
import romelt_techcare.backend.enums.NotificationChannel;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION MAPPER
 * ================================================================
 *
 * Purpose:
 * Maps suppression requests, entities, and administrator responses.
 * ================================================================
 */
@Component
public class NotificationSuppressionMapper {

    public NotificationSuppression toEntity(
            NotificationSuppressionCreateRequest request,
            UUID administratorId
    ) {
        requireCreateRequest(request);
        validateCreateRequest(request);

        String recipientAddress =
                normalizeRequired(
                        request.recipientAddress(),
                        "Notification suppression recipient address is required."
                );

        return NotificationSuppression.builder()
                .customerId(request.customerId())
                .channel(request.channel())
                .notificationCategory(
                        request.notificationCategory()
                )
                .recipientAddress(recipientAddress)
                .normalizedRecipientAddress(
                        normalizeRecipientAddress(
                                recipientAddress,
                                request.channel()
                        )
                )
                .suppressionReason(
                        request.suppressionReason()
                )
                .reasonDetails(
                        normalizeOptional(request.reasonDetails())
                )
                .active(true)
                .suppressedAt(Instant.now())
                .expiresAt(request.expiresAt())
                .createdByAdminUserId(administratorId)
                .build();
    }

    public void updateEntity(
            NotificationSuppression suppression,
            NotificationSuppressionUpdateRequest request
    ) {
        requireEntity(suppression);
        requireUpdateRequest(request);

        if (!request.hasChanges()) {
            throw new IllegalArgumentException(
                    "At least one notification suppression change is required."
            );
        }

        suppression.updateDetails(
                request.suppressionReason() == null
                        ? suppression.getSuppressionReason()
                        : request.suppressionReason(),
                request.reasonDetails() == null
                        ? suppression.getReasonDetails()
                        : request.reasonDetails(),
                request.resolveExpiration(
                        suppression.getExpiresAt()
                )
        );
    }

    public NotificationSuppressionResponse toResponse(
            NotificationSuppression suppression
    ) {
        requireEntity(suppression);

        return new NotificationSuppressionResponse(
                suppression.getNotificationSuppressionId(),
                suppression.getCustomerId(),
                suppression.getChannel(),
                suppression.getNotificationCategory(),
                suppression.getRecipientAddress(),
                suppression.getNormalizedRecipientAddress(),
                suppression.getSuppressionReason(),
                suppression.getReasonDetails(),
                suppression.isActive(),
                suppression.isEffective(),
                suppression.getSuppressedAt(),
                suppression.getExpiresAt(),
                suppression.getDeactivatedAt(),
                suppression.getCreatedByAdminUserId(),
                suppression.getDeactivatedByAdminUserId(),
                suppression.getCreatedAt(),
                suppression.getUpdatedAt(),
                suppression.getRowVersion()
        );
    }

    public NotificationSuppressionSummaryResponse toSummaryResponse(
            NotificationSuppression suppression
    ) {
        requireEntity(suppression);

        return new NotificationSuppressionSummaryResponse(
                suppression.getNotificationSuppressionId(),
                suppression.getCustomerId(),
                suppression.getChannel(),
                suppression.getNotificationCategory(),
                suppression.getRecipientAddress(),
                suppression.getSuppressionReason(),
                suppression.isActive(),
                suppression.isEffective(),
                suppression.getSuppressedAt(),
                suppression.getExpiresAt(),
                suppression.getDeactivatedAt(),
                suppression.getCreatedAt()
        );
    }

    public String normalizeRecipientAddress(
            String value,
            NotificationChannel channel
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Notification recipient address is required."
                );

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification channel is required."
            );
        }

        return switch (channel) {
            case EMAIL ->
                    normalized.toLowerCase(Locale.ROOT);

            case SMS -> {
                String digitsOnly =
                        normalized.replaceAll("\\D", "");

                if (
                        digitsOnly.length() < 7
                                || digitsOnly.length() > 30
                ) {
                    throw new IllegalArgumentException(
                            "Notification recipient telephone number is invalid."
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

    private void validateCreateRequest(
            NotificationSuppressionCreateRequest request
    ) {
        if (!request.hasSupportedChannel()) {
            throw new IllegalArgumentException(
                    "Notification suppression supports only EMAIL and SMS channels."
            );
        }

        if (!request.hasValidRecipientAddress()) {
            throw new IllegalArgumentException(
                    "Notification suppression recipient address is invalid."
            );
        }

        if (request.suppressionReason() == null) {
            throw new IllegalArgumentException(
                    "Notification suppression reason is required."
            );
        }

        if (
                request.expiresAt() != null
                        && !request.expiresAt().isAfter(
                        Instant.now()
                )
        ) {
            throw new IllegalArgumentException(
                    "Notification suppression expiration must be in the future."
            );
        }
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

    private void requireCreateRequest(
            NotificationSuppressionCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification suppression create request is required."
            );
        }
    }

    private void requireUpdateRequest(
            NotificationSuppressionUpdateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification suppression update request is required."
            );
        }
    }

    private void requireEntity(
            NotificationSuppression suppression
    ) {
        if (suppression == null) {
            throw new IllegalArgumentException(
                    "Notification suppression is required."
            );
        }
    }
}