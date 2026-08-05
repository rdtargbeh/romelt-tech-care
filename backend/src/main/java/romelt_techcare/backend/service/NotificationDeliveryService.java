package romelt_techcare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationDeliveryCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryResponse;
import romelt_techcare.backend.dto.NotificationDeliverySummaryResponse;
import romelt_techcare.backend.entity.NotificationDelivery;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines delivery persistence, worker processing, provider outcome,
 * and administrator in-app notification operations.
 * ================================================================
 */
public interface NotificationDeliveryService {

    NotificationDeliveryResponse createDelivery(
            NotificationDeliveryCreateRequest request
    );

    NotificationDeliveryResponse getDelivery(
            UUID notificationDeliveryId
    );

    List<NotificationDeliveryResponse> getDeliveriesForEvent(
            UUID notificationEventId
    );

    Page<NotificationDeliverySummaryResponse> getDeliveries(
            String keyword,
            NotificationChannel channel,
            NotificationDeliveryStatus deliveryStatus,
            UUID notificationEventId,
            UUID customerId,
            UUID recipientAdminUserId,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    );

    List<NotificationDelivery> claimAvailableDeliveries(
            String workerId,
            int batchSize,
            Duration leaseDuration
    );

    List<NotificationDelivery> recoverExpiredLeaseDeliveries(
            int batchSize,
            Duration retryDelay
    );

    void renewLease(
            UUID notificationDeliveryId,
            String workerId,
            Duration leaseDuration
    );

    int beginAttempt(
            UUID notificationDeliveryId,
            String workerId
    );

    void markSent(
            UUID notificationDeliveryId,
            String workerId,
            String providerName,
            String providerMessageId,
            JsonNode providerResponse
    );

    void markInAppPublished(
            UUID notificationDeliveryId,
            String workerId
    );

    void scheduleRetry(
            UUID notificationDeliveryId,
            String workerId,
            String failureCode,
            String failureMessage,
            Instant retryAt,
            JsonNode providerResponse
    );

    void markFailed(
            UUID notificationDeliveryId,
            String workerId,
            String failureCode,
            String failureMessage,
            JsonNode providerResponse
    );

    void markDelivered(
            UUID notificationDeliveryId,
            Instant deliveredAt
    );

    void markOpened(
            UUID notificationDeliveryId,
            Instant openedAt
    );

    void markClicked(
            UUID notificationDeliveryId,
            Instant clickedAt
    );

    void markBounced(
            UUID notificationDeliveryId,
            String failureCode,
            String failureMessage,
            Instant bouncedAt
    );

    NotificationDeliveryResponse cancelDelivery(
            UUID notificationDeliveryId,
            String reason
    );

    NotificationDeliveryResponse suppressDelivery(
            UUID notificationDeliveryId,
            String reason
    );

    Page<NotificationDeliverySummaryResponse> getMyInAppNotifications(
            AdminJwtPrincipal principal,
            boolean unreadOnly,
            Pageable pageable
    );

    Page<NotificationDeliverySummaryResponse> getMyDismissedNotifications(
            AdminJwtPrincipal principal,
            Pageable pageable
    );

    long countMyUnreadNotifications(
            AdminJwtPrincipal principal
    );

    NotificationDeliveryResponse markMyNotificationRead(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    );

    NotificationDeliveryResponse markMyNotificationUnread(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    );

    NotificationDeliveryResponse dismissMyNotification(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    );

    NotificationDeliveryResponse restoreMyDismissedNotification(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    );
}