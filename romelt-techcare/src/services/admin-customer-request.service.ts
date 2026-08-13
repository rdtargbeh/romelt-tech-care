/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REQUEST SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides authenticated frontend API operations for customer booking
 * requests and contact inquiries.
 *
 * Responsibilities:
 * - Retrieves paginated booking requests.
 * - Retrieves one complete booking.
 * - Creates administrator-entered bookings.
 * - Updates booking lifecycle status.
 * - Retrieves contact inquiries.
 * - Retrieves one contact inquiry.
 * - Updates inquiry lifecycle status.
 *
 * Real-data integration:
 *
 * Booking:
 * GET   /api/v1/admin/booking-requests
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 * POST  /api/v1/admin/booking-requests
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 *
 * Contact:
 * GET   /api/v1/admin/contact-inquiries
 * GET   /api/v1/admin/contact-inquiries/{contactInquiryId}
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
 *
 * Authentication:
 * All operations in this file require the administrator JWT.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

import type {
  AdminBookingRequest,
  AdminBookingRequestCreatePayload,
  AdminBookingStatusUpdatePayload,
  AdminContactInquiry,
  AdminContactInquiryStatusUpdatePayload,
  PageResponse,
} from "@/types/admin-customer-request.types";

// =====================================================================
// ENDPOINTS
// =====================================================================

const ADMIN_BOOKING_REQUESTS_ENDPOINT = "/admin/booking-requests";

const ADMIN_CONTACT_INQUIRIES_ENDPOINT = "/admin/contact-inquiries";

// =====================================================================
// BOOKINGS
// =====================================================================

/**
 * Returns paginated booking requests.
 */
export function getAdminBookingRequests(
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<AdminBookingRequest>> {
  const searchParams = new URLSearchParams();

  searchParams.set("page", String(page));

  searchParams.set("size", String(size));

  return apiClient.get<PageResponse<AdminBookingRequest>>(
    `${ADMIN_BOOKING_REQUESTS_ENDPOINT}?${searchParams.toString()}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Returns one complete booking request.
 */
export function getAdminBookingRequest(
  bookingRequestId: string,
  signal?: AbortSignal,
): Promise<AdminBookingRequest> {
  const normalizedId = requireIdentifier(
    bookingRequestId,
    "Booking request ID",
  );

  return apiClient.get<AdminBookingRequest>(
    `${ADMIN_BOOKING_REQUESTS_ENDPOINT}/${encodeURIComponent(normalizedId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Creates an administrator-entered booking.
 */
export function createAdminBookingRequest(
  request: AdminBookingRequestCreatePayload,
  signal?: AbortSignal,
): Promise<AdminBookingRequest> {
  if (!request) {
    throw new Error("Booking request information is required.");
  }

  return apiClient.post<AdminBookingRequest>(
    ADMIN_BOOKING_REQUESTS_ENDPOINT,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Updates one booking lifecycle status.
 */
export function updateAdminBookingStatus(
  bookingRequestId: string,
  request: AdminBookingStatusUpdatePayload,
  signal?: AbortSignal,
): Promise<AdminBookingRequest> {
  const normalizedId = requireIdentifier(
    bookingRequestId,
    "Booking request ID",
  );

  if (!request) {
    throw new Error("Booking status information is required.");
  }

  return apiClient.patch<AdminBookingRequest>(
    `${ADMIN_BOOKING_REQUESTS_ENDPOINT}/${encodeURIComponent(
      normalizedId,
    )}/status`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// CONTACT INQUIRIES
// =====================================================================

/**
 * Returns paginated contact inquiries.
 */
export function getAdminContactInquiries(
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<AdminContactInquiry>> {
  const searchParams = new URLSearchParams();

  searchParams.set("page", String(page));

  searchParams.set("size", String(size));

  return apiClient.get<PageResponse<AdminContactInquiry>>(
    `${ADMIN_CONTACT_INQUIRIES_ENDPOINT}?${searchParams.toString()}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Returns one complete contact inquiry.
 */
export function getAdminContactInquiry(
  contactInquiryId: string,
  signal?: AbortSignal,
): Promise<AdminContactInquiry> {
  const normalizedId = requireIdentifier(
    contactInquiryId,
    "Contact inquiry ID",
  );

  return apiClient.get<AdminContactInquiry>(
    `${ADMIN_CONTACT_INQUIRIES_ENDPOINT}/${encodeURIComponent(normalizedId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Updates one contact-inquiry lifecycle status.
 */
export function updateAdminContactInquiryStatus(
  contactInquiryId: string,
  request: AdminContactInquiryStatusUpdatePayload,
  signal?: AbortSignal,
): Promise<AdminContactInquiry> {
  const normalizedId = requireIdentifier(
    contactInquiryId,
    "Contact inquiry ID",
  );

  if (!request) {
    throw new Error("Contact inquiry status information is required.");
  }

  return apiClient.patch<AdminContactInquiry>(
    `${ADMIN_CONTACT_INQUIRIES_ENDPOINT}/${encodeURIComponent(
      normalizedId,
    )}/status`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// HELPERS
// =====================================================================

function requireIdentifier(value: string, label: string): string {
  const normalized = value?.trim();

  if (!normalized) {
    throw new Error(`${label} is required.`);
  }

  return normalized;
}
