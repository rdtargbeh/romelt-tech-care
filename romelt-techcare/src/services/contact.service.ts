/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides frontend API operations for general customer inquiries.
 *
 * Responsibilities:
 * - Submits public contact inquiries to the Spring Boot backend.
 * - Keeps the contact endpoint outside page components.
 * - Preserves exact page payload types through generics.
 * - Returns the backend-created inquiry result.
 *
 * Real-data integration:
 * The Spring Boot backend should expose:
 *
 * POST /api/v1/public/contact-inquiries
 *
 * The exact request and response DTOs can be supplied by the calling
 * form until shared generated API types are introduced.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

const CONTACT_INQUIRIES_ENDPOINT = "/public/contact-inquiries";

export function submitContactInquiry<TRequest extends object, TResponse>(
  request: TRequest,
  signal?: AbortSignal,
): Promise<TResponse> {
  return apiClient.post<TResponse>(CONTACT_INQUIRIES_ENDPOINT, request, {
    signal,
    requireAuthentication: false,
  });
}
