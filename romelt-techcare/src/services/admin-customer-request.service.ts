/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REQUEST SERVICE
 * ================================================================
 *
 * Purpose:
 * Connects administrator booking and contact-inquiry pages to the
 * authenticated backend APIs.
 *
 * Responsibilities:
 * - Retrieves paginated booking requests.
 * - Retrieves one booking request.
 * - Creates a booking for a client.
 * - Updates booking status.
 * - Retrieves paginated contact inquiries.
 * - Retrieves one contact inquiry.
 * - Updates contact-inquiry status.
 *
 * Security:
 * All administrator operations require the stored administrator JWT.
 *
 * Real-data integration:
 * GET   /api/v1/admin/booking-requests
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 * POST  /api/v1/admin/booking-requests
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 *
 * GET   /api/v1/admin/contact-inquiries
 * GET   /api/v1/admin/contact-inquiries/{contactInquiryId}
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
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

const ADMIN_BOOKING_ENDPOINT = "/admin/booking-requests";
const ADMIN_CONTACT_ENDPOINT = "/admin/contact-inquiries";

/**
 * Retrieves paginated booking requests.
 */
export async function getAdminBookingRequests(
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<AdminBookingRequest>> {
  return apiClient.get<PageResponse<AdminBookingRequest>>(
    `${ADMIN_BOOKING_ENDPOINT}?page=${page}&size=${size}&sort=submittedAt,desc`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Retrieves one complete booking request.
 */
export async function getAdminBookingRequest(
  bookingRequestId: string,
  signal?: AbortSignal,
): Promise<AdminBookingRequest> {
  return apiClient.get<AdminBookingRequest>(
    `${ADMIN_BOOKING_ENDPOINT}/${encodeURIComponent(bookingRequestId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Creates a booking for a client on behalf of an administrator.
 *
 * The request payload type is inferred from the payload argument.
 * The single generic type represents the API response.
 */
export async function createAdminBookingRequest(
  payload: AdminBookingRequestCreatePayload,
): Promise<AdminBookingRequest> {
  return apiClient.post<AdminBookingRequest>(ADMIN_BOOKING_ENDPOINT, payload, {
    requireAuthentication: true,
  });
}

/**
 * Updates the operational status and optional administrator notes for
 * an existing booking request.
 */
export async function updateAdminBookingStatus(
  bookingRequestId: string,
  payload: AdminBookingStatusUpdatePayload,
): Promise<AdminBookingRequest> {
  return apiClient.patch<AdminBookingRequest>(
    `${ADMIN_BOOKING_ENDPOINT}/${encodeURIComponent(bookingRequestId)}/status`,
    payload,
    {
      requireAuthentication: true,
    },
  );
}

/**
 * Retrieves paginated contact inquiries.
 */
export async function getAdminContactInquiries(
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<AdminContactInquiry>> {
  return apiClient.get<PageResponse<AdminContactInquiry>>(
    `${ADMIN_CONTACT_ENDPOINT}?page=${page}&size=${size}&sort=submittedAt,desc`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Retrieves one complete contact inquiry.
 */
export async function getAdminContactInquiry(
  contactInquiryId: string,
  signal?: AbortSignal,
): Promise<AdminContactInquiry> {
  return apiClient.get<AdminContactInquiry>(
    `${ADMIN_CONTACT_ENDPOINT}/${encodeURIComponent(contactInquiryId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

/**
 * Updates the operational status of an existing contact inquiry.
 */
export async function updateAdminContactInquiryStatus(
  contactInquiryId: string,
  payload: AdminContactInquiryStatusUpdatePayload,
): Promise<AdminContactInquiry> {
  return apiClient.patch<AdminContactInquiry>(
    `${ADMIN_CONTACT_ENDPOINT}/${encodeURIComponent(contactInquiryId)}/status`,
    payload,
    {
      requireAuthentication: true,
    },
  );
}
