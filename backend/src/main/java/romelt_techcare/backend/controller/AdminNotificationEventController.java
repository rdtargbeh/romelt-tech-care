package romelt_techcare.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.NotificationEventResponse;
import romelt_techcare.backend.dto.NotificationEventSummaryResponse;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.service.NotificationEventService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION EVENT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for viewing,
 * searching, and cancelling transactional notification-outbox events.
 *
 * Responsibilities:
 * - Returns paginated notification-event lists.
 * - Supports operational event filters.
 * - Returns complete notification-event details.
 * - Returns events related to a specific business resource.
 * - Cancels non-terminal notification events.
 *
 * Security:
 * - This controller must remain behind administrator authentication.
 * - Notification template data and recipient information may contain
 *   personal information.
 * - No public notification-event controller should be created.
 *
 * Worker operations:
 * Event claiming, lease renewal, retry handling, permanent failure,
 * and processing completion are internal worker operations and are
 * intentionally not exposed through this controller.
 *
 * Base path:
 * /api/v1/admin/notification-events
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notification-events")
@RequiredArgsConstructor
public class AdminNotificationEventController {

    private final NotificationEventService
            notificationEventService;

    /**
     * Returns a paginated and filtered notification-event list.
     *
     * Supported filters:
     * - keyword
     * - eventStatus
     * - eventType
     * - resourceType
     * - resourceId
     * - customerId
     * - recipientAdminUserId
     * - createdFrom
     * - createdTo
     *
     * Endpoint:
     * GET /api/v1/admin/notification-events
     */
    @GetMapping
    public ResponseEntity<
            Page<NotificationEventSummaryResponse>
            > getNotificationEvents(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            NotificationEventStatus eventStatus,

            @RequestParam(required = false)
            NotificationEventType eventType,

            @RequestParam(required = false)
            NotificationResourceType resourceType,

            @RequestParam(required = false)
            UUID resourceId,

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
        Page<NotificationEventSummaryResponse> response =
                notificationEventService.getEvents(
                        keyword,
                        eventStatus,
                        eventType,
                        resourceType,
                        resourceId,
                        customerId,
                        recipientAdminUserId,
                        createdFrom,
                        createdTo,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns one complete notification event.
     *
     * Endpoint:
     * GET /api/v1/admin/notification-events/{notificationEventId}
     */
    @GetMapping("/{notificationEventId}")
    public ResponseEntity<NotificationEventResponse>
    getNotificationEvent(
            @PathVariable
            UUID notificationEventId
    ) {
        NotificationEventResponse response =
                notificationEventService.getEvent(
                        notificationEventId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns all notification events associated with one business
     * resource.
     *
     * Example:
     * GET
     * /api/v1/admin/notification-events/resource
     * ?resourceType=BOOKING_REQUEST
     * &resourceId={bookingRequestId}
     *
     * Endpoint:
     * GET /api/v1/admin/notification-events/resource
     */
    @GetMapping("/resource")
    public ResponseEntity<List<NotificationEventResponse>>
    getNotificationEventsForResource(

            @RequestParam
            NotificationResourceType resourceType,

            @RequestParam
            UUID resourceId
    ) {
        List<NotificationEventResponse> response =
                notificationEventService
                        .getEventsForResource(
                                resourceType,
                                resourceId
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a non-terminal notification event.
     *
     * Events already marked PROCESSED, FAILED, or CANCELLED cannot be
     * cancelled.
     *
     * Endpoint:
     * POST
     * /api/v1/admin/notification-events/{notificationEventId}/cancel
     */
    @PostMapping("/{notificationEventId}/cancel")
    public ResponseEntity<NotificationEventResponse>
    cancelNotificationEvent(

            @PathVariable
            UUID notificationEventId,

            @RequestParam(required = false)
            String reason
    ) {
        NotificationEventResponse response =
                notificationEventService.cancelEvent(
                        notificationEventId,
                        reason
                );

        return ResponseEntity.ok(response);
    }
}