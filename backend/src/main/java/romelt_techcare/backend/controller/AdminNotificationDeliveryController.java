package romelt_techcare.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationDeliveryResponse;
import romelt_techcare.backend.dto.NotificationDeliverySummaryResponse;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;
import romelt_techcare.backend.service.NotificationDeliveryService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION DELIVERY CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes administrator delivery monitoring and personal in-app
 * notification-center endpoints.
 *
 * Internal worker and provider operations are not exposed here.
 *
 * Base path:
 * /api/v1/admin/notification-deliveries
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notification-deliveries")
@RequiredArgsConstructor
public class AdminNotificationDeliveryController {

    private final NotificationDeliveryService
            notificationDeliveryService;

    @GetMapping
    public ResponseEntity<
            Page<NotificationDeliverySummaryResponse>
            > getDeliveries(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            NotificationChannel channel,

            @RequestParam(required = false)
            NotificationDeliveryStatus deliveryStatus,

            @RequestParam(required = false)
            UUID notificationEventId,

            @RequestParam(required = false)
            UUID customerId,

            @RequestParam(required = false)
            UUID recipientAdminUserId,

            @RequestParam(required = false)
            Instant createdFrom,

            @RequestParam(required = false)
            Instant createdTo,

            Pageable pageable
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService.getDeliveries(
                        keyword,
                        channel,
                        deliveryStatus,
                        notificationEventId,
                        customerId,
                        recipientAdminUserId,
                        createdFrom,
                        createdTo,
                        pageable
                )
        );
    }

    @GetMapping("/{notificationDeliveryId}")
    public ResponseEntity<NotificationDeliveryResponse>
    getDelivery(
            @PathVariable
            UUID notificationDeliveryId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService.getDelivery(
                        notificationDeliveryId
                )
        );
    }

    @GetMapping("/event/{notificationEventId}")
    public ResponseEntity<List<NotificationDeliveryResponse>>
    getDeliveriesForEvent(
            @PathVariable
            UUID notificationEventId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .getDeliveriesForEvent(
                                notificationEventId
                        )
        );
    }

    @PostMapping("/{notificationDeliveryId}/cancel")
    public ResponseEntity<NotificationDeliveryResponse>
    cancelDelivery(
            @PathVariable
            UUID notificationDeliveryId,

            @RequestParam(required = false)
            String reason
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService.cancelDelivery(
                        notificationDeliveryId,
                        reason
                )
        );
    }

    @PostMapping("/{notificationDeliveryId}/suppress")
    public ResponseEntity<NotificationDeliveryResponse>
    suppressDelivery(
            @PathVariable
            UUID notificationDeliveryId,

            @RequestParam
            String reason
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService.suppressDelivery(
                        notificationDeliveryId,
                        reason
                )
        );
    }

    @GetMapping("/in-app/me")
    public ResponseEntity<
            Page<NotificationDeliverySummaryResponse>
            > getMyInAppNotifications(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @RequestParam(
                    required = false,
                    defaultValue = "false"
            )
            boolean unreadOnly,

            Pageable pageable
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .getMyInAppNotifications(
                                principal,
                                unreadOnly,
                                pageable
                        )
        );
    }

    @GetMapping("/in-app/me/dismissed")
    public ResponseEntity<
            Page<NotificationDeliverySummaryResponse>
            > getMyDismissedNotifications(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            Pageable pageable
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .getMyDismissedNotifications(
                                principal,
                                pageable
                        )
        );
    }

    @GetMapping("/in-app/me/unread-count")
    public ResponseEntity<Map<String, Long>>
    countMyUnreadNotifications(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal
    ) {
        long unreadCount =
                notificationDeliveryService
                        .countMyUnreadNotifications(
                                principal
                        );

        return ResponseEntity.ok(
                Map.of(
                        "unreadCount",
                        unreadCount
                )
        );
    }

    @PostMapping(
            "/in-app/me/{notificationDeliveryId}/read"
    )
    public ResponseEntity<NotificationDeliveryResponse>
    markMyNotificationRead(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationDeliveryId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .markMyNotificationRead(
                                principal,
                                notificationDeliveryId
                        )
        );
    }

    @PostMapping(
            "/in-app/me/{notificationDeliveryId}/unread"
    )
    public ResponseEntity<NotificationDeliveryResponse>
    markMyNotificationUnread(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationDeliveryId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .markMyNotificationUnread(
                                principal,
                                notificationDeliveryId
                        )
        );
    }

    @PostMapping(
            "/in-app/me/{notificationDeliveryId}/dismiss"
    )
    public ResponseEntity<NotificationDeliveryResponse>
    dismissMyNotification(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationDeliveryId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .dismissMyNotification(
                                principal,
                                notificationDeliveryId
                        )
        );
    }

    @PostMapping(
            "/in-app/me/{notificationDeliveryId}/restore"
    )
    public ResponseEntity<NotificationDeliveryResponse>
    restoreMyDismissedNotification(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationDeliveryId
    ) {
        return ResponseEntity.ok(
                notificationDeliveryService
                        .restoreMyDismissedNotification(
                                principal,
                                notificationDeliveryId
                        )
        );
    }
}