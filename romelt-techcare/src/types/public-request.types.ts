/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC REQUEST RESPONSE TYPES
 * ================================================================
 *
 * Purpose:
 * Defines shared response information returned after contact and
 * booking requests are accepted by the backend.
 *
 * Responsibilities:
 * - Provides confirmation and reference-number types.
 * - Keeps form confirmation screens independent of backend entities.
 * - Avoids exposing internal database records to public clients.
 *
 * Real-data integration:
 * The Spring Boot response DTOs should provide these values after
 * successfully creating a public inquiry or booking request.
 * ================================================================
 */

export interface PublicRequestConfirmation {
  referenceNumber: string;
  message: string;
  submittedAt: string;
}

export interface ContactInquiryConfirmation extends PublicRequestConfirmation {
  inquiryId?: string;
}

export interface BookingRequestConfirmation extends PublicRequestConfirmation {
  bookingRequestId?: string;
  requestedDate?: string;
  requestedTime?: string;
  status?: string;
}
