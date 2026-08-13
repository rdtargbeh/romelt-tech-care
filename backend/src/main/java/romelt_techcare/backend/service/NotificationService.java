package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminInAppNotificationResponse;
import romelt_techcare.backend.dto.AdminNotificationCountResponse;
import romelt_techcare.backend.dto.NotificationCreateRequest;
import romelt_techcare.backend.dto.NotificationResponse;
import romelt_techcare.backend.dto.NotificationSummaryResponse;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides one simple service for EMAIL, SMS, and administrator
 * IN_APP notifications.
 *
 * Responsibilities:
 * - Creates and sends customer EMAIL notifications.
 * - Creates and sends customer SMS notifications.
 * - Creates administrator IN_APP notifications.
 * - Stores all notification results in the notifications table.
 * - Retrieves notification history.
 * - Manages administrator read and dismissed state.
 * - Applies provider delivery callbacks.
 *
 * This service does not use:
 * - Transactional outbox workers.
 * - Worker leases.
 * - Delivery-attempt tables.
 * - Database template engines.
 * - Suppression tables.
 * ================================================================
 */
public interface NotificationService {

    NotificationResponse sendEmail(
            NotificationCreateRequest request
    );

    NotificationResponse sendSms(
            NotificationCreateRequest request
    );

    AdminInAppNotificationResponse createInApp(
            NotificationCreateRequest request
    );

    NotificationResponse getNotification(
            UUID notificationId
    );

    Page<NotificationSummaryResponse> search(
            NotificationChannel channel,
            NotificationStatus status,
            NotificationResourceType resourceType,
            UUID resourceId,
            UUID customerId,
            UUID adminUserId,
            String keyword,
            Pageable pageable
    );

    Page<AdminInAppNotificationResponse> getAdminNotifications(
            UUID adminUserId,
            boolean unreadOnly,
            Pageable pageable
    );

    AdminNotificationCountResponse getAdminUnreadCount(
            UUID adminUserId
    );

    AdminInAppNotificationResponse markAdminNotificationRead(
            UUID adminUserId,
            UUID notificationId
    );

    AdminInAppNotificationResponse markAdminNotificationUnread(
            UUID adminUserId,
            UUID notificationId
    );

    AdminInAppNotificationResponse dismissAdminNotification(
            UUID adminUserId,
            UUID notificationId
    );

    AdminInAppNotificationResponse restoreAdminNotification(
            UUID adminUserId,
            UUID notificationId
    );

    NotificationResponse markProviderDelivered(
            String providerMessageId,
            Instant deliveredAt
    );

    NotificationResponse markProviderFailed(
            String providerMessageId,
            String failureCode,
            String failureMessage
    );
}