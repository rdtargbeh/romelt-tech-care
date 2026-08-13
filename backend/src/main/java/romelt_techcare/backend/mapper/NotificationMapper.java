package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.AdminInAppNotificationResponse;
import romelt_techcare.backend.dto.NotificationCreateRequest;
import romelt_techcare.backend.dto.NotificationResponse;
import romelt_techcare.backend.dto.NotificationSummaryResponse;
import romelt_techcare.backend.entity.Notification;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts notification entities and DTOs for the simplified
 * notification module.
 *
 * Responsibilities:
 * - Creates pending notification records.
 * - Maps complete notification responses.
 * - Maps administrator notification summaries.
 * - Maps administrator in-app portal notifications.
 * - Resolves frontend paths for supported resources.
 *
 * Important:
 * Every notification begins as PENDING.
 *
 * The NotificationService changes the status after:
 * - EMAIL is accepted or rejected.
 * - SMS is accepted or rejected.
 * - IN_APP is persisted and published.
 * ================================================================
 */
@Component
public class NotificationMapper {

    /**
     * Converts an internal request into a new pending notification.
     */
    public Notification toEntity(
            NotificationCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification create request is required."
            );
        }

        if (!request.hasValidRecipient()) {
            throw new IllegalArgumentException(
                    "Notification recipient information is invalid."
            );
        }

        if (!request.hasValidContent()) {
            throw new IllegalArgumentException(
                    "Notification content is invalid for the selected channel."
            );
        }

        return Notification.builder()
                .channel(
                        request.channel()
                )
                .recipientType(
                        request.recipientType()
                )
                .customerId(
                        request.customerId()
                )
                .adminUserId(
                        request.adminUserId()
                )
                .resourceType(
                        request.resourceType()
                )
                .resourceId(
                        request.resourceId()
                )
                .recipientAddress(
                        normalizeOptional(
                                request.recipientAddress()
                        )
                )
                .recipientName(
                        normalizeOptional(
                                request.recipientName()
                        )
                )
                .title(
                        normalizeRequired(
                                request.title(),
                                "Notification title is required."
                        )
                )
                .messageText(
                        normalizeRequired(
                                request.messageText(),
                                "Notification message is required."
                        )
                )
                .messageHtml(
                        normalizeMessageHtml(request)
                )
                .notificationStatus(
                        NotificationStatus.PENDING
                )
                .providerName(
                        normalizeOptional(
                                request.providerName()
                        )
                )
                .providerMessageId(
                        normalizeOptional(
                                request.providerMessageId()
                        )
                )
                .providerResponseJson(
                        request.providerResponseJson() == null
                                ? null
                                : request
                                .providerResponseJson()
                                .deepCopy()
                )
                .build();
    }

    /**
     * Converts an entity into a complete response.
     */
    public NotificationResponse toResponse(
            Notification notification
    ) {
        if (notification == null) {
            return null;
        }

        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getChannel(),
                notification.getRecipientType(),
                notification.getCustomerId(),
                notification.getAdminUserId(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getRecipientAddress(),
                notification.getRecipientName(),
                notification.getTitle(),
                notification.getMessageText(),
                notification.getMessageHtml(),
                notification.getNotificationStatus(),
                notification.getProviderName(),
                notification.getProviderMessageId(),
                notification.getProviderResponseJson() == null
                        ? null
                        : notification
                        .getProviderResponseJson()
                        .deepCopy(),
                notification.getAttemptCount(),
                notification.getLastAttemptedAt(),
                notification.getSentAt(),
                notification.getDeliveredAt(),
                notification.getFailedAt(),
                notification.getFailureCode(),
                notification.getFailureMessage(),
                notification.getReadAt(),
                notification.getDismissedAt(),
                notification.isRead(),
                notification.isDismissed(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getRowVersion()
        );
    }

    /**
     * Converts an entity into a compact administrator summary.
     */
    public NotificationSummaryResponse toSummaryResponse(
            Notification notification
    ) {
        if (notification == null) {
            return null;
        }

        return new NotificationSummaryResponse(
                notification.getNotificationId(),
                notification.getChannel(),
                notification.getRecipientType(),
                notification.getCustomerId(),
                notification.getAdminUserId(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getRecipientName(),
                notification.getRecipientAddress(),
                notification.getTitle(),
                notification.getMessageText(),
                notification.getNotificationStatus(),
                notification.getProviderName(),
                notification.getProviderMessageId(),
                notification.getAttemptCount(),
                notification.isRead(),
                notification.isDismissed(),
                notification.getSentAt(),
                notification.getDeliveredAt(),
                notification.getFailedAt(),
                notification.getCreatedAt()
        );
    }

    /**
     * Converts an IN_APP entity into a portal response.
     */
    public AdminInAppNotificationResponse toAdminInAppResponse(
            Notification notification
    ) {
        if (notification == null) {
            return null;
        }

        if (
                notification.getChannel()
                        != NotificationChannel.IN_APP
        ) {
            throw new IllegalArgumentException(
                    "Only in-app notifications can be mapped to an administrator portal response."
            );
        }

        return new AdminInAppNotificationResponse(
                notification.getNotificationId(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getTitle(),
                notification.getMessageText(),
                resolvePortalPath(notification),
                notification.isRead(),
                notification.isDismissed(),
                notification.getReadAt(),
                notification.getDismissedAt(),
                notification.getCreatedAt()
        );
    }

    /**
     * Resolves the frontend route for the related business resource.
     */
    private String resolvePortalPath(
            Notification notification
    ) {
        if (
                notification == null
                        || notification.getResourceType() == null
                        || notification.getResourceId() == null
        ) {
            return "/admin";
        }

        return switch (notification.getResourceType()) {
            case BOOKING_REQUEST ->
                    "/admin/bookings/"
                            + notification.getResourceId();

            case CONTACT_INQUIRY ->
                    "/admin/contact-inquiries/"
                            + notification.getResourceId();

            default ->
                    "/admin";
        };
    }

    private String normalizeMessageHtml(
            NotificationCreateRequest request
    ) {
        if (
                request.channel()
                        != NotificationChannel.EMAIL
        ) {
            return null;
        }

        return normalizeOptional(
                request.messageHtml()
        );
    }

    private String normalizeRequired(
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