/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER TYPES
 * ================================================================
 *
 * Purpose:
 * Defines frontend contracts for the reusable Romelt TechCare
 * customer directory in the authenticated administrator portal.
 *
 * Responsibilities:
 * - Mirrors CustomerSummaryResponse.java.
 * - Mirrors CustomerResponse.java.
 * - Mirrors CustomerCreateRequest.java.
 * - Mirrors CustomerUpdateRequest.java.
 * - Mirrors CustomerMergeRequest.java.
 * - Defines customer status, source, and contact-method values.
 * - Defines customer list/search options.
 *
 * Real-data integration:
 *
 * POST   /api/v1/admin/customers
 * GET    /api/v1/admin/customers
 * GET    /api/v1/admin/customers/{customerId}
 * PUT    /api/v1/admin/customers/{customerId}
 * POST   /api/v1/admin/customers/{customerId}/archive
 * POST   /api/v1/admin/customers/{customerId}/restore
 * DELETE /api/v1/admin/customers/{customerId}
 * POST   /api/v1/admin/customers/{customerId}/merge
 *
 * Important:
 * Customer represents the reusable current customer/contact profile.
 *
 * Existing BookingRequest records retain their own historical contact
 * and business snapshots when the reusable customer profile changes.
 * ================================================================
 */

// =====================================================================
// ENUM TYPES
// =====================================================================

export type CustomerStatus =
  | "ACTIVE"
  | "INACTIVE"
  | "BLOCKED"
  | "ARCHIVED"
  | "MERGED"
  | "DELETED";

export type CustomerSource =
  | "BOOKING"
  | "CONTACT_INQUIRY"
  | "ADMIN_CREATED"
  | "REVIEW"
  | "REFERRAL"
  | "IMPORT"
  | "OTHER";

export type CustomerContactMethod = "EMAIL" | "PHONE" | "TEXT";

// =====================================================================
// CUSTOMER SUMMARY
// =====================================================================

/**
 * Mirrors:
 * CustomerSummaryResponse.java
 *
 * Used by:
 * GET /api/v1/admin/customers
 *
 * This deliberately excludes private/internal customer fields.
 */
export interface AdminCustomerSummary {
  customerId: string;

  customerNumber: string;

  displayName: string;

  preferredName: string | null;

  primaryEmail: string | null;

  primaryPhone: string | null;

  preferredContactMethod: CustomerContactMethod | null;

  customerStatus: CustomerStatus;

  customerSource: CustomerSource;

  lastBookingAt: string | null;

  lastServiceCompletedAt: string | null;

  lastActivityAt: string | null;
}

// =====================================================================
// COMPLETE CUSTOMER RESPONSE
// =====================================================================

export interface AdminCustomer {
  customerId: string;

  customerNumber: string;

  firstName: string | null;

  lastName: string | null;

  preferredName: string | null;

  displayName: string;

  primaryEmail: string | null;

  primaryPhone: string | null;

  preferredContactMethod: CustomerContactMethod | null;

  streetAddress: string | null;

  addressLine2: string | null;

  city: string | null;

  stateRegion: string | null;

  postalCode: string | null;

  countryCode: string | null;

  customerStatus: CustomerStatus;

  customerSource: CustomerSource;

  marketingConsent: boolean;

  marketingConsentAt: string | null;

  marketingConsentSource: string | null;

  emailVerified: boolean;

  emailVerifiedAt: string | null;

  phoneVerified: boolean;

  phoneVerifiedAt: string | null;

  doNotEmail: boolean;

  doNotCall: boolean;

  doNotText: boolean;

  communicationNotes: string | null;

  internalNotes: string | null;

  firstContactAt: string | null;

  lastContactedAt: string | null;

  lastBookingAt: string | null;

  lastServiceCompletedAt: string | null;

  lastActivityAt: string | null;

  mergedIntoCustomerId: string | null;

  mergedAt: string | null;

  mergedByAdminUserId: string | null;

  createdByAdminUserId: string | null;

  updatedByAdminUserId: string | null;

  archivedByAdminUserId: string | null;

  deletedByAdminUserId: string | null;

  archivedAt: string | null;

  deletedAt: string | null;

  createdAt: string;

  updatedAt: string;

  rowVersion: number;
}

// =====================================================================
// CREATE REQUEST
// =====================================================================

export interface AdminCustomerCreatePayload {
  firstName: string | null;

  lastName: string | null;

  preferredName: string | null;

  displayName: string;

  primaryEmail: string | null;

  primaryPhone: string | null;

  preferredContactMethod: CustomerContactMethod | null;

  streetAddress: string | null;

  addressLine2: string | null;

  city: string | null;

  stateRegion: string | null;

  postalCode: string | null;

  countryCode: string | null;

  customerSource: CustomerSource | null;

  marketingConsent: boolean;

  marketingConsentSource: string | null;

  doNotEmail: boolean;

  doNotCall: boolean;

  doNotText: boolean;

  communicationNotes: string | null;

  internalNotes: string | null;
}

// =====================================================================
// UPDATE REQUEST
// =====================================================================

export interface AdminCustomerUpdatePayload {
  firstName: string | null;

  lastName: string | null;

  preferredName: string | null;

  displayName: string | null;

  primaryEmail: string | null;

  primaryPhone: string | null;

  preferredContactMethod: CustomerContactMethod | null;

  streetAddress: string | null;

  addressLine2: string | null;

  city: string | null;

  stateRegion: string | null;

  postalCode: string | null;

  countryCode: string | null;

  customerStatus: CustomerStatus | null;

  marketingConsent: boolean | null;

  marketingConsentSource: string | null;

  emailVerified: boolean | null;

  phoneVerified: boolean | null;

  doNotEmail: boolean | null;

  doNotCall: boolean | null;

  doNotText: boolean | null;

  communicationNotes: string | null;

  internalNotes: string | null;
}

// =====================================================================
// MERGE REQUEST
// =====================================================================

export interface AdminCustomerMergePayload {
  survivingCustomerId: string;

  mergeReason: string | null;
}

// =====================================================================
// LIST OPTIONS
// =====================================================================

export interface GetAdminCustomersOptions {
  keyword?: string;

  customerStatus?: CustomerStatus;

  page?: number;

  size?: number;

  signal?: AbortSignal;
}
