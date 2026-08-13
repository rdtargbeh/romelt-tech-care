package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.dto.AdminInAppNotificationResponse;
import romelt_techcare.backend.service.AdminNotificationWebSocketService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION WEBSOCKET IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Publishes persisted in-app notifications to one authenticated
 * administrator through Spring STOMP user destinations.
 *
 * Client subscription:
 * /user/queue/notifications
 *
 * Delivery rule:
 * WebSocket publication is best-effort.
 *
 * A WebSocket failure must not delete, roll back, or mark the
 * persisted in-app notification as failed. The REST notification
 * endpoints remain available when the administrator reconnects.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationWebSocketServiceImpl
        implements AdminNotificationWebSocketService {

    private static final String DESTINATION =
            "/queue/notifications";

    private final SimpMessagingTemplate
            simpMessagingTemplate;

    @Override
    public void publishNotification(
            UUID adminUserId,
            AdminInAppNotificationResponse notification
    ) {
        if (adminUserId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required for WebSocket notification delivery."
            );
        }

        if (
                notification == null
                        || notification.notificationId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted administrator notification is required."
            );
        }

        try {
            /*
             * The user value must match the authenticated WebSocket
             * Principal name.
             *
             * The JWT WebSocket authentication bridge added next will
             * use the administrator UUID as Principal.getName().
             */
            simpMessagingTemplate.convertAndSendToUser(
                    adminUserId.toString(),
                    DESTINATION,
                    notification
            );

            log.debug(
                    "Administrator WebSocket notification published. adminUserId={}, notificationId={}",
                    adminUserId,
                    notification.notificationId()
            );

        } catch (MessagingException exception) {
            /*
             * The notification is already persisted. WebSocket
             * publication failure should not destroy the database
             * notification or affect the booking/contact operation.
             */
            log.warn(
                    "Administrator WebSocket notification could not be published. adminUserId={}, notificationId={}, errorType={}",
                    adminUserId,
                    notification.notificationId(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}