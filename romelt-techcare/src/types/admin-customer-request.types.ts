/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REQUEST TYPES
 * ================================================================
 *
 * Purpose:
 * Defines frontend types for administrator booking requests and
 * customer contact inquiries.
 *
 * Responsibilities:
 * - Supports booking list, create, detail, and status updates.
 * - Supports contact-inquiry list, detail, and status updates.
 * - Represents the Spring Data paginated response structure.
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

export type ServiceMethod = "REMOTE" | "ON_SITE" | "DROP_OFF" | "NOT_SURE";

export type PreferredServiceTime =
  | "MORNING"
  | "AFTERNOON"
  | "EVENING"
  | "FLEXIBLE";

export type ContactMethod = "EMAIL" | "PHONE" | "TEXT";

export type BookingSource =
  | "WEBSITE"
  | "PHONE"
  | "EMAIL"
  | "WALK_IN"
  | "ADMIN_ENTRY"
  | "OTHER";

export type BookingRequestStatus =
  | "PENDING"
  | "UNDER_REVIEW"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED"
  | "DECLINED"
  | "EXPIRED";

export type ContactInquiryStatus =
  | "NEW"
  | "IN_PROGRESS"
  | "RESPONDED"
  | "CLOSED"
  | "SPAM";

export interface AdminBookingRequest {
  bookingRequestId: string;
  referenceNumber: string;
  fullName: string;
  email: string;
  phone: string;
  serviceType: string;
  serviceMethod: ServiceMethod;
  preferredDate: string;
  preferredTime: PreferredServiceTime;
  alternateDate: string | null;
  streetAddress: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  deviceType: string | null;
  problemDescription: string;
  preferredContactMethod: ContactMethod;
  status: BookingRequestStatus;
  bookingSource: BookingSource;
  createdByAdminUserId: string | null;
  createdByAdminName: string | null;
  adminNotes: string | null;
  consentAccepted: boolean;
  submittedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminBookingRequestCreatePayload {
  fullName: string;
  email: string;
  phone: string;
  serviceType: string;
  serviceMethod: ServiceMethod;
  preferredDate: string;
  preferredTime: PreferredServiceTime;
  alternateDate: string | null;
  streetAddress: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  deviceType: string | null;
  problemDescription: string;
  preferredContactMethod: ContactMethod;
  bookingSource: Exclude<BookingSource, "WEBSITE">;
  adminNotes: string | null;
}

export interface AdminBookingStatusUpdatePayload {
  status: BookingRequestStatus;
  adminNotes: string | null;
}

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

export interface AdminContactInquiryStatusUpdatePayload {
  status: ContactInquiryStatus;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
