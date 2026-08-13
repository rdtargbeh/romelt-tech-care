/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR NOTIFICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides authenticated frontend API operations for administrator
 * in-app notifications.
 *
 * Responsibilities:
 * - Retrieves active administrator notifications.
 * - Supports unread-only filtering.
 * - Retrieves the administrator unread count.
 * - Marks notifications as read.
 * - Marks notifications as unread.
 * - Dismisses active notifications.
 * - Restores previously dismissed notifications.
 *
 * Security:
 * The authenticated administrator is determined by the backend JWT.
 * No administrator ID is sent from the browser.
 *
 * Real-data integration:
 *
 * GET   /api/v1/admin/notifications
 * GET   /api/v1/admin/notifications/unread-count
 * PATCH /api/v1/admin/notifications/{notificationId}/read
 * PATCH /api/v1/admin/notifications/{notificationId}/unread
 * PATCH /api/v1/admin/notifications/{notificationId}/dismiss
 * PATCH /api/v1/admin/notifications/{notificationId}/restore
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

import type {
  AdminInAppNotification,
  AdminNotificationCount,
  AdminNotificationPage,
  GetAdminNotificationsOptions,
} from "@/types/admin-notification.types";

// =====================================================================
// ENDPOINT
// =====================================================================

const ADMIN_NOTIFICATIONS_ENDPOINT = "/admin/notifications";

// =====================================================================
// LIST
// =====================================================================

/**
 * Returns active notifications belonging to the authenticated
 * administrator.
 *
 * The backend automatically excludes dismissed notifications from the
 * normal list.
 */
export function getAdminNotifications(
  options: GetAdminNotificationsOptions = {},
): Promise<AdminNotificationPage> {
  const { unreadOnly = false, page = 0, size = 10, signal } = options;

  const resolvedPage = Math.max(0, page);

  const resolvedSize = Math.min(Math.max(1, size), 50);

  const searchParams = new URLSearchParams();

  searchParams.set("unreadOnly", String(unreadOnly));

  searchParams.set("page", String(resolvedPage));

  searchParams.set("size", String(resolvedSize));

  return apiClient.get<AdminNotificationPage>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}?${searchParams.toString()}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// UNREAD COUNT
// =====================================================================

/**
 * Returns the unread notification count for the authenticated
 * administrator.
 */
export function getAdminNotificationUnreadCount(
  signal?: AbortSignal,
): Promise<AdminNotificationCount> {
  return apiClient.get<AdminNotificationCount>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}/unread-count`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// MARK READ
// =====================================================================

/**
 * Marks one notification belonging to the authenticated administrator
 * as read.
 */
export function markAdminNotificationRead(
  notificationId: string,
  signal?: AbortSignal,
): Promise<AdminInAppNotification> {
  const normalizedId = requireNotificationId(notificationId);

  return apiClient.patch<AdminInAppNotification>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}/${encodeURIComponent(normalizedId)}/read`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// MARK UNREAD
// =====================================================================

/**
 * Marks one notification belonging to the authenticated administrator
 * as unread.
 */
export function markAdminNotificationUnread(
  notificationId: string,
  signal?: AbortSignal,
): Promise<AdminInAppNotification> {
  const normalizedId = requireNotificationId(notificationId);

  return apiClient.patch<AdminInAppNotification>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}/${encodeURIComponent(
      normalizedId,
    )}/unread`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// DISMISS
// =====================================================================

/**
 * Dismisses one active administrator notification.
 *
 * Dismissed notifications are removed from the normal notification
 * list but remain stored in PostgreSQL.
 */
export function dismissAdminNotification(
  notificationId: string,
  signal?: AbortSignal,
): Promise<AdminInAppNotification> {
  const normalizedId = requireNotificationId(notificationId);

  return apiClient.patch<AdminInAppNotification>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}/${encodeURIComponent(
      normalizedId,
    )}/dismiss`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// RESTORE
// =====================================================================

/**
 * Restores one previously dismissed administrator notification.
 */
export function restoreAdminNotification(
  notificationId: string,
  signal?: AbortSignal,
): Promise<AdminInAppNotification> {
  const normalizedId = requireNotificationId(notificationId);

  return apiClient.patch<AdminInAppNotification>(
    `${ADMIN_NOTIFICATIONS_ENDPOINT}/${encodeURIComponent(
      normalizedId,
    )}/restore`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// HELPERS
// =====================================================================

function requireNotificationId(notificationId: string): string {
  const normalized = notificationId?.trim();

  if (!normalized) {
    throw new Error("Notification ID is required.");
  }

  return normalized;
}
