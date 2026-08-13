package romelt_techcare.backend.service;

import romelt_techcare.backend.dto.AdminInAppNotificationResponse;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION WEBSOCKET SERVICE
 * ================================================================
 *
 * Purpose:
 * Pushes a persisted administrator in-app notification to the
 * administrator's active portal session.
 *
 * Important:
 * The notification must be stored in PostgreSQL before this service
 * publishes it.
 *
 * If the administrator is offline, the WebSocket message is not
 * retained. The administrator can still retrieve the persisted
 * notification through the REST notification endpoints.
 * ================================================================
 */
public interface AdminNotificationWebSocketService {

    void publishNotification(
            UUID adminUserId,
            AdminInAppNotificationResponse notification
    );
}