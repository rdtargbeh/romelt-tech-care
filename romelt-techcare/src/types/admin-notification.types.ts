/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR NOTIFICATION TYPES
 * ================================================================
 *
 * Purpose:
 * Defines the frontend contracts for authenticated administrator
 * in-app notifications.
 *
 * Responsibilities:
 * - Mirrors AdminInAppNotificationResponse.
 * - Mirrors AdminNotificationCountResponse.
 * - Defines supported notification resource types.
 * - Defines paginated notification responses.
 *
 * Real-data integration:
 *
 * GET   /api/v1/admin/notifications
 * GET   /api/v1/admin/notifications/unread-count
 * PATCH /api/v1/admin/notifications/{notificationId}/read
 * PATCH /api/v1/admin/notifications/{notificationId}/unread
 * PATCH /api/v1/admin/notifications/{notificationId}/dismiss
 * PATCH /api/v1/admin/notifications/{notificationId}/restore
 *
 * Important:
 * The backend determines notification ownership from the authenticated
 * JWT. The frontend must never send an administrator ID when reading
 * or mutating notification state.
 * ================================================================
 */

// =====================================================================
// RESOURCE TYPES
// =====================================================================

/**
 * Operational resources currently capable of opening directly from
 * the administrator notification center.
 *
 * Keep this aligned with NotificationResourceType.java.
 */
export type AdminNotificationResourceType =
  | "BOOKING_REQUEST"
  | "CONTACT_INQUIRY"
  | "CUSTOMER"
  | "CUSTOMER_REVIEW"
  | "REVIEW_INVITATION"
  | "SERVICE"
  | "ADMIN_USER"
  | "OTHER";

// =====================================================================
// IN-APP NOTIFICATION
// =====================================================================

/**
 * Mirrors:
 * AdminInAppNotificationResponse.java
 */
export interface AdminInAppNotification {
  notificationId: string;

  resourceType: AdminNotificationResourceType;

  resourceId: string;

  title: string;

  message: string;

  /**
   * Backend-generated administrator route.
   *
   * Examples:
   * /admin/bookings/{bookingRequestId}
   * /admin/contact-inquiries/{contactInquiryId}
   */
  portalPath: string;

  read: boolean;

  dismissed: boolean;

  readAt: string | null;

  dismissedAt: string | null;

  createdAt: string;
}

// =====================================================================
// UNREAD COUNT
// =====================================================================

/**
 * Mirrors:
 * AdminNotificationCountResponse.java
 */
export interface AdminNotificationCount {
  unreadCount: number;
}

// =====================================================================
// PAGINATION
// =====================================================================

export interface AdminNotificationPageSort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface AdminNotificationPageable {
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
  sort: AdminNotificationPageSort;
}

export interface AdminNotificationPage {
  content: AdminInAppNotification[];

  pageable?: AdminNotificationPageable;

  totalPages: number;
  totalElements: number;

  last: boolean;

  size: number;
  number: number;

  sort?: AdminNotificationPageSort;

  numberOfElements: number;

  first: boolean;
  empty: boolean;
}

// =====================================================================
// LIST OPTIONS
// =====================================================================

export interface GetAdminNotificationsOptions {
  unreadOnly?: boolean;

  page?: number;

  size?: number;

  signal?: AbortSignal;
}
