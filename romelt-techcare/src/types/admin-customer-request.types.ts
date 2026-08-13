/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REQUEST TYPES
 * ================================================================
 *
 * Purpose:
 * Defines frontend contracts for authenticated administrator booking
 * and contact-inquiry operations.
 *
 * Responsibilities:
 * - Mirrors the current Spring Boot administrator booking DTOs.
 * - Mirrors administrator contact-inquiry response contracts.
 * - Defines pagination structures returned by Spring Data.
 * - Defines booking lifecycle update payloads.
 * - Defines administrator-created booking payloads.
 *
 * Real-data integration:
 *
 * Booking:
 * GET   /api/v1/admin/booking-requests
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 * POST  /api/v1/admin/booking-requests
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 *
 * Contact inquiries:
 * GET   /api/v1/admin/contact-inquiries
 * GET   /api/v1/admin/contact-inquiries/{contactInquiryId}
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
 *
 * Important:
 * These interfaces must stay aligned with the backend DTOs.
 * ================================================================
 */

// =====================================================================
// SHARED ENUM TYPES
// =====================================================================

export type BookingFor = "PERSONAL" | "BUSINESS";

export type ServiceMethod = "REMOTE" | "ON_SITE" | "DROP_OFF" | "NOT_SURE";

export type PreferredServiceTime =
  | "MORNING"
  | "AFTERNOON"
  | "EVENING"
  | "FLEXIBLE";

export type ContactMethod = "EMAIL" | "PHONE" | "TEXT";

export type BookingRequestStatus =
  | "PENDING"
  | "UNDER_REVIEW"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED"
  | "DECLINED"
  | "EXPIRED";

export type BookingSource =
  | "WEBSITE"
  | "PHONE"
  | "EMAIL"
  | "WALK_IN"
  | "REFERRAL"
  | "OTHER";

export type AdminBookingSource = Exclude<BookingSource, "WEBSITE">;

export type ContactInquiryStatus =
  | "NEW"
  | "IN_PROGRESS"
  | "RESPONDED"
  | "CLOSED"
  | "SPAM";

// =====================================================================
// SPRING PAGINATION
// =====================================================================

export interface PageSort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface PageableResponse {
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
  sort: PageSort;
}

export interface PageResponse<T> {
  content: T[];

  pageable?: PageableResponse;

  totalPages: number;
  totalElements: number;

  last: boolean;

  size: number;
  number: number;

  sort?: PageSort;

  numberOfElements: number;

  first: boolean;
  empty: boolean;
}

// =====================================================================
// ADMIN BOOKING — CREATE REQUEST
// =====================================================================

/**
 * Mirrors:
 * AdminBookingRequestCreateRequest.java
 *
 * Important:
 * WEBSITE must not be sent by the administrator-create flow.
 */
export interface AdminBookingRequestCreatePayload {
  /**
   * Existing customer may be selected.
   *
   * When null, backend customer resolution may find or create the
   * customer from the booking contact information.
   */
  customerId: string | null;

  bookingFor: BookingFor;

  fullName: string;
  email: string;
  phone: string;

  preferredContactMethod: ContactMethod;

  /**
   * Optional dedicated destinations.
   *
   * Backend may fall back to email/phone when these are null.
   */
  notificationEmail: string | null;
  notificationPhone: string | null;

  // -------------------------------------------------------------------
  // BUSINESS SNAPSHOT
  // -------------------------------------------------------------------

  businessName: string | null;
  businessEmail: string | null;
  businessPhone: string | null;

  businessStreetAddress: string | null;
  businessCity: string | null;
  businessState: string | null;
  businessPostalCode: string | null;
  businessCountryCode: string | null;

  businessContactRole: string | null;

  // -------------------------------------------------------------------
  // SERVICE
  // -------------------------------------------------------------------

  serviceId: string | null;

  serviceType: string;

  serviceMethod: ServiceMethod;

  // -------------------------------------------------------------------
  // REQUESTED SCHEDULE
  // -------------------------------------------------------------------

  preferredDate: string;

  preferredTime: PreferredServiceTime;

  alternateDate: string | null;

  // -------------------------------------------------------------------
  // SERVICE INFORMATION
  // -------------------------------------------------------------------

  deviceType: string | null;

  problemDescription: string;

  // -------------------------------------------------------------------
  // SERVICE LOCATION
  // -------------------------------------------------------------------

  streetAddress: string | null;

  addressLine2: string | null;

  city: string | null;

  stateRegion: string | null;

  postalCode: string | null;

  countryCode: string | null;

  // -------------------------------------------------------------------
  // ADMINISTRATION
  // -------------------------------------------------------------------

  bookingSource: AdminBookingSource;

  assignedAdminUserId: string | null;

  adminNotes: string | null;
}

// =====================================================================
// ADMIN BOOKING — RESPONSE
// =====================================================================

/**
 * Mirrors:
 * AdminBookingRequestResponse.java
 */
export interface AdminBookingRequest {
  bookingRequestId: string;

  customerId: string | null;

  referenceNumber: string;

  bookingFor: BookingFor;

  // -------------------------------------------------------------------
  // CUSTOMER / CONTACT PERSON
  // -------------------------------------------------------------------

  fullName: string;

  email: string;

  phone: string;

  preferredContactMethod: ContactMethod;

  notificationEmail: string | null;

  notificationPhone: string | null;

  // -------------------------------------------------------------------
  // BUSINESS SNAPSHOT
  // -------------------------------------------------------------------

  businessName: string | null;

  businessEmail: string | null;

  businessPhone: string | null;

  businessStreetAddress: string | null;

  businessCity: string | null;

  businessState: string | null;

  businessPostalCode: string | null;

  businessCountryCode: string | null;

  businessContactRole: string | null;

  // -------------------------------------------------------------------
  // SERVICE
  // -------------------------------------------------------------------

  serviceId: string | null;

  serviceType: string;

  serviceMethod: ServiceMethod;

  preferredDate: string;

  preferredTime: PreferredServiceTime;

  alternateDate: string | null;

  deviceType: string | null;

  problemDescription: string;

  // -------------------------------------------------------------------
  // LOCATION
  // -------------------------------------------------------------------

  streetAddress: string | null;

  addressLine2: string | null;

  city: string | null;

  stateRegion: string | null;

  postalCode: string | null;

  countryCode: string | null;

  // -------------------------------------------------------------------
  // CONFIRMED SCHEDULE
  // -------------------------------------------------------------------

  scheduledStartAt: string | null;

  scheduledEndAt: string | null;

  scheduledTimezone: string | null;

  // -------------------------------------------------------------------
  // CONSENT
  // -------------------------------------------------------------------

  consentAccepted: boolean;

  consentAcceptedAt: string | null;

  consentVersion: string | null;

  // -------------------------------------------------------------------
  // LIFECYCLE
  // -------------------------------------------------------------------

  status: BookingRequestStatus;

  bookingSource: BookingSource;

  confirmedAt: string | null;

  completedAt: string | null;

  cancelledAt: string | null;

  declinedAt: string | null;

  expiredAt: string | null;

  cancellationReason: string | null;

  declineReason: string | null;

  expirationReason: string | null;

  completionSummary: string | null;

  completionNotes: string | null;

  reviewEligible: boolean;

  reviewEligibilityNotes: string | null;

  // -------------------------------------------------------------------
  // ADMINISTRATOR OWNERSHIP
  // -------------------------------------------------------------------

  assignedAdminUserId: string | null;

  createdByAdminUserId: string | null;

  createdByAdminName: string | null;

  updatedByAdminUserId: string | null;

  confirmedByAdminUserId: string | null;

  completedByAdminUserId: string | null;

  cancelledByAdminUserId: string | null;

  declinedByAdminUserId: string | null;

  expiredByAdminUserId: string | null;

  adminNotes: string | null;

  // -------------------------------------------------------------------
  // DATABASE AUDIT
  // -------------------------------------------------------------------

  submittedAt: string;

  createdAt: string;

  updatedAt: string;

  rowVersion: number;
}

// =====================================================================
// ADMIN BOOKING — STATUS UPDATE
// =====================================================================

/**
 * Mirrors:
 * AdminBookingStatusUpdateRequest.java
 *
 * Required fields depend on the requested status:
 *
 * CONFIRMED:
 * - scheduledStartAt
 * - scheduledEndAt
 * - scheduledTimezone
 *
 * COMPLETED:
 * - completionSummary
 *
 * CANCELLED:
 * - cancellationReason
 *
 * DECLINED:
 * - declineReason
 *
 * EXPIRED:
 * - expirationReason
 */
export interface AdminBookingStatusUpdatePayload {
  status: BookingRequestStatus;

  assignedAdminUserId: string | null;

  scheduledStartAt: string | null;

  scheduledEndAt: string | null;

  scheduledTimezone: string | null;

  cancellationReason: string | null;

  declineReason: string | null;

  expirationReason: string | null;

  completionSummary: string | null;

  completionNotes: string | null;

  reviewEligible: boolean | null;

  reviewEligibilityNotes: string | null;

  adminNotes: string | null;

  changeReason: string | null;
}

// =====================================================================
// BOOKING STATUS HISTORY
// =====================================================================

export interface BookingRequestStatusHistory {
  bookingStatusHistoryId: string;

  bookingRequestId: string;

  previousStatus: BookingRequestStatus | null;

  newStatus: BookingRequestStatus;

  changeReason: string | null;

  changedByAdminUserId: string | null;

  changedByAdminName: string | null;

  notificationEventId: string | null;

  changedAt: string;
}

// =====================================================================
// ADMIN CONTACT INQUIRY
// =====================================================================

export interface AdminContactInquiry {
  contactInquiryId: string;

  referenceNumber: string;

  fullName: string;

  email: string;

  phone: string | null;

  subject: string;

  serviceType: string | null;

  message: string;

  preferredContactMethod: ContactMethod;

  status: ContactInquiryStatus;

  consentAccepted: boolean;

  submittedAt: string;

  createdAt: string;

  updatedAt: string;
}

// =====================================================================
// ADMIN CONTACT INQUIRY — STATUS UPDATE
// =====================================================================

export interface AdminContactInquiryStatusUpdatePayload {
  status: ContactInquiryStatus;
}
