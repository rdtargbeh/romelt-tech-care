package romelt_techcare.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.AdminInAppNotificationResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminNotificationCountResponse;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.service.NotificationService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for managing
 * database-backed in-app notifications.
 *
 * Responsibilities:
 * - Returns active administrator notifications.
 * - Supports unread-only filtering.
 * - Returns the unread notification count.
 * - Marks notifications as read.
 * - Marks notifications as unread.
 * - Dismisses notifications.
 * - Restores dismissed notifications.
 *
 * Security:
 * - The administrator ID always comes from the authenticated JWT.
 * - An administrator cannot pass another administrator ID.
 * - NotificationService verifies notification ownership before any
 *   read-state or dismissal-state change.
 *
 * Pagination:
 * - Default page: 0
 * - Default size: 10
 * - Maximum size: 50
 * - Results are ordered by createdAt descending.
 *
 * Base path:
 * /api/v1/admin/notifications
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAXIMUM_PAGE_SIZE = 50;

    private final NotificationService notificationService;

    /**
     * Returns active administrator notifications.
     *
     * Examples:
     *
     * GET /api/v1/admin/notifications
     *
     * GET /api/v1/admin/notifications?unreadOnly=true
     *
     * GET /api/v1/admin/notifications?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<AdminInAppNotificationResponse>>
    getNotifications(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @RequestParam(
                    defaultValue = "false"
            )
            boolean unreadOnly,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        Page<AdminInAppNotificationResponse> response =
                notificationService.getAdminNotifications(
                        adminUserId,
                        unreadOnly,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the authenticated administrator's unread notification
     * count.
     *
     * GET /api/v1/admin/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<AdminNotificationCountResponse>
    getUnreadCount(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        AdminNotificationCountResponse response =
                notificationService.getAdminUnreadCount(
                        adminUserId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Marks one administrator notification as read.
     *
     * PATCH /api/v1/admin/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<AdminInAppNotificationResponse>
    markRead(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationId
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        AdminInAppNotificationResponse response =
                notificationService
                        .markAdminNotificationRead(
                                adminUserId,
                                notificationId
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Marks one administrator notification as unread.
     *
     * PATCH /api/v1/admin/notifications/{notificationId}/unread
     */
    @PatchMapping("/{notificationId}/unread")
    public ResponseEntity<AdminInAppNotificationResponse>
    markUnread(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationId
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        AdminInAppNotificationResponse response =
                notificationService
                        .markAdminNotificationUnread(
                                adminUserId,
                                notificationId
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Dismisses one administrator notification.
     *
     * Dismissed notifications no longer appear in the normal active
     * notification list.
     *
     * PATCH /api/v1/admin/notifications/{notificationId}/dismiss
     */
    @PatchMapping("/{notificationId}/dismiss")
    public ResponseEntity<AdminInAppNotificationResponse>
    dismiss(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationId
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        AdminInAppNotificationResponse response =
                notificationService
                        .dismissAdminNotification(
                                adminUserId,
                                notificationId
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Restores one previously dismissed administrator notification.
     *
     * PATCH /api/v1/admin/notifications/{notificationId}/restore
     */
    @PatchMapping("/{notificationId}/restore")
    public ResponseEntity<AdminInAppNotificationResponse>
    restore(

            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationId
    ) {
        UUID adminUserId =
                resolveAdminUserId(principal);

        AdminInAppNotificationResponse response =
                notificationService
                        .restoreAdminNotification(
                                adminUserId,
                                notificationId
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Resolves the administrator identity from the authenticated JWT.
     */
    private UUID resolveAdminUserId(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        return principal.adminUserId();
    }

    /**
     * Creates safe pagination settings.
     */
    private Pageable createPageable(
            int page,
            int size
    ) {
        int resolvedPage =
                Math.max(
                        page,
                        0
                );

        int resolvedSize;

        if (size <= 0) {
            resolvedSize =
                    DEFAULT_PAGE_SIZE;
        } else {
            resolvedSize =
                    Math.min(
                            size,
                            MAXIMUM_PAGE_SIZE
                    );
        }

        return PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }
}