/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides frontend API operations for public service-booking
 * requests.
 *
 * Responsibilities:
 * - Submits booking requests to the Spring Boot backend.
 * - Keeps booking endpoint details outside page components.
 * - Preserves exact form request and response types through generics.
 * - Supports request cancellation when a page is unmounted.
 *
 * Real-data integration:
 * The Spring Boot backend should expose:
 *
 * POST /api/v1/public/booking-requests
 *
 * The exact request and response DTOs can be supplied by the calling
 * booking form until shared generated API types are introduced.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

const BOOKING_REQUESTS_ENDPOINT = "/public/booking-requests";

export function submitBookingRequest<TRequest extends object, TResponse>(
  request: TRequest,
  signal?: AbortSignal,
): Promise<TResponse> {
  return apiClient.post<TResponse>(BOOKING_REQUESTS_ENDPOINT, request, {
    signal,
    requireAuthentication: false,
  });
}
