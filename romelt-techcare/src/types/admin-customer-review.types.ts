/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEW TYPES
 * ================================================================
 *
 * Purpose:
 * Defines administrator-facing customer-review TypeScript contracts.
 *
 * Core business rule:
 * Every customer review represents feedback about an actual completed,
 * review-eligible BookingRequest.
 *
 * Review ownership:
 *
 * Customer
 *   -> BookingRequest
 *       -> WebsiteService
 *           -> CustomerReview
 *
 * ContactInquiry is intentionally not part of review ownership.
 *
 * Customer-first review creation workflow:
 *
 * 1. Administrator searches/selects Customer.
 * 2. Frontend sends customerId only as an eligible-booking FILTER.
 * 3. Backend returns only that customer's completed,
 *    review-eligible bookings that do not already have a review.
 * 4. Administrator selects the exact BookingRequest.
 * 5. Review creation sends bookingRequestId.
 * 6. Backend derives Customer and WebsiteService from the booking.
 *
 * Important:
 * customerId is NOT part of AdminCustomerReviewCreatePayload.
 *
 * customerId exists only in the eligible-booking search contract.
 *
 * BookingRequest remains the authoritative review ownership record.
 *
 * Backend-derived values:
 * - customer identity
 * - customer email
 * - customer phone
 * - WebsiteService
 * - verified-customer status
 *
 * These values must not be supplied by administrator review-creation
 * payloads.
 *
 * Public customer-review contracts remain separate in:
 * src/types/customer-review.types.ts
 * ================================================================
 */

// =====================================================================
// ENUM TYPES
// =====================================================================

export type CustomerReviewDisplayPreference =
  | "FULL_NAME"
  | "FIRST_NAME_LAST_INITIAL"
  | "FIRST_NAME_ONLY"
  | "ANONYMOUS"
  | "CUSTOM";

export type CustomerReviewSource =
  | "WEBSITE"
  | "BOOKING_FOLLOW_UP"
  | "EMAIL"
  | "PHONE"
  | "GOOGLE"
  | "FACEBOOK"
  | "OTHER";

export type CustomerReviewModerationStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "SPAM"
  | "HIDDEN"
  | "ARCHIVED";

/**
 * Must remain aligned with the BookingRequest ServiceMethod enum.
 */
export type CustomerReviewEligibleBookingServiceMethod =
  | "REMOTE"
  | "ON_SITE"
  | "DROP_OFF"
  | "NOT_SURE";

// =====================================================================
// MEDIA
// =====================================================================

export interface AdminCustomerReviewMediaAsset {
  mediaAssetId?: string | null;

  assetKey?: string | null;

  publicUrl?: string | null;

  originalFileName?: string | null;

  mimeType?: string | null;

  title?: string | null;

  altText?: string | null;

  caption?: string | null;

  description?: string | null;

  widthPixels?: number | null;

  heightPixels?: number | null;

  isDecorative?: boolean | null;
}

// =====================================================================
// COMPLETE ADMIN REVIEW RESPONSE
// =====================================================================

/**
 * Mirrors the final backend CustomerReviewResponse.
 *
 * Private contact and moderation information may appear here because
 * this interface is administrator-only.
 */
export interface AdminCustomerReview {
  // -------------------------------------------------------------------
  // IDENTITY
  // -------------------------------------------------------------------

  customerReviewId: string;

  // -------------------------------------------------------------------
  // RELATED INVITATION
  // -------------------------------------------------------------------

  reviewInvitationId: string | null;

  // -------------------------------------------------------------------
  // COMPLETED BOOKING
  // -------------------------------------------------------------------

  bookingRequestId: string | null;

  bookingReferenceNumber: string | null;

  // -------------------------------------------------------------------
  // SERVICE
  // -------------------------------------------------------------------

  serviceId: string | null;

  serviceCode: string | null;

  serviceSlug: string | null;

  // -------------------------------------------------------------------
  // REVIEWER
  // -------------------------------------------------------------------

  reviewerDisplayName: string | null;

  resolvedPublicDisplayName: string | null;

  reviewerDisplayPreference: CustomerReviewDisplayPreference;

  /**
   * Backend-generated customer contact snapshot.
   *
   * This is returned for administrator use but is not accepted in the
   * administrator create/update payload.
   */
  reviewerEmail: string | null;

  reviewerPhone: string | null;

  // -------------------------------------------------------------------
  // REVIEW CONTENT
  // -------------------------------------------------------------------

  reviewTitle: string | null;

  reviewText: string | null;

  rating: number;

  reviewSource: CustomerReviewSource;

  externalSourceUrl: string | null;

  // -------------------------------------------------------------------
  // CUSTOMER PHOTO
  // -------------------------------------------------------------------

  customerPhoto: AdminCustomerReviewMediaAsset | null;

  // -------------------------------------------------------------------
  // VERIFICATION
  // -------------------------------------------------------------------

  /**
   * Backend-generated from the validated completed booking.
   */
  isVerifiedCustomer: boolean;

  // -------------------------------------------------------------------
  // CUSTOMER CONSENT
  // -------------------------------------------------------------------

  customerConsentConfirmed: boolean;

  customerConsentConfirmedAt: string | null;

  customerConsentVersion: string | null;

  customerConsentIpAddress: string | null;

  // -------------------------------------------------------------------
  // MODERATION
  // -------------------------------------------------------------------

  moderationStatus: CustomerReviewModerationStatus;

  moderationNotes: string | null;

  rejectionReason: string | null;

  // -------------------------------------------------------------------
  // PUBLICATION
  // -------------------------------------------------------------------

  isPublic: boolean;

  isFeatured: boolean;

  publiclyVisible: boolean;

  // -------------------------------------------------------------------
  // ADMINISTRATOR RESPONSE
  // -------------------------------------------------------------------

  adminResponse: string | null;

  respondedAt: string | null;

  respondedByAdminUserId: string | null;

  respondedByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // SUBMISSION METADATA
  // -------------------------------------------------------------------

  submissionIpAddress: string | null;

  submissionUserAgent: string | null;

  // -------------------------------------------------------------------
  // SPAM
  // -------------------------------------------------------------------

  spamScore: number | null;

  isSpam: boolean;

  // -------------------------------------------------------------------
  // TIMELINE
  // -------------------------------------------------------------------

  submittedAt: string | null;

  moderatedAt: string | null;

  publishedAt: string | null;

  hiddenAt: string | null;

  archivedAt: string | null;

  // -------------------------------------------------------------------
  // CREATED BY
  // -------------------------------------------------------------------

  createdByAdminUserId: string | null;

  createdByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // UPDATED BY
  // -------------------------------------------------------------------

  updatedByAdminUserId: string | null;

  updatedByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // MODERATED BY
  // -------------------------------------------------------------------

  moderatedByAdminUserId: string | null;

  moderatedByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // PUBLISHED BY
  // -------------------------------------------------------------------

  publishedByAdminUserId: string | null;

  publishedByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // HIDDEN BY
  // -------------------------------------------------------------------

  hiddenByAdminUserId: string | null;

  hiddenByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // ARCHIVED BY
  // -------------------------------------------------------------------

  archivedByAdminUserId: string | null;

  archivedByAdminUserDisplayName: string | null;

  // -------------------------------------------------------------------
  // SYSTEM TIMESTAMPS
  // -------------------------------------------------------------------

  createdAt: string | null;

  updatedAt: string | null;

  // -------------------------------------------------------------------
  // OPTIMISTIC LOCK
  // -------------------------------------------------------------------

  rowVersion: number | null;
}

// =====================================================================
// PAGINATION
// =====================================================================

export interface PageResponse<TContent> {
  content: TContent[];

  pageable?: {
    pageNumber?: number;

    pageSize?: number;

    offset?: number;

    paged?: boolean;

    unpaged?: boolean;

    sort?: {
      sorted?: boolean;

      unsorted?: boolean;

      empty?: boolean;
    };
  };

  totalPages: number;

  totalElements: number;

  last: boolean;

  size: number;

  number: number;

  sort?: {
    sorted?: boolean;

    unsorted?: boolean;

    empty?: boolean;
  };

  numberOfElements: number;

  first: boolean;

  empty: boolean;
}

// =====================================================================
// CUSTOMER-FIRST ELIGIBLE BOOKING RESPONSE
// =====================================================================

/**
 * Mirrors:
 * CustomerReviewEligibleBookingResponse.java
 *
 * Used by:
 *
 * GET /api/v1/admin/customer-reviews/eligible-bookings
 *
 * Required workflow:
 *
 * Customer selected
 *      -> customerId
 *          -> backend filters BookingRequest
 *              -> completed
 *              -> completedAt exists
 *              -> reviewEligible = true
 *              -> serviceId exists
 *              -> no CustomerReview exists for booking
 *
 * The frontend must NOT load all completed bookings and filter them
 * locally.
 *
 * Filtering belongs to the backend.
 */
export interface AdminCustomerReviewEligibleBooking {
  // -------------------------------------------------------------------
  // BOOKING
  // -------------------------------------------------------------------

  bookingRequestId: string;

  referenceNumber: string;

  // -------------------------------------------------------------------
  // CUSTOMER
  // -------------------------------------------------------------------

  /**
   * Returned for confirmation/display.
   *
   * This is not sent in the review-create payload.
   */
  customerId: string;

  customerNumber: string;

  customerDisplayName: string;

  customerEmail: string | null;

  customerPhone: string | null;

  // -------------------------------------------------------------------
  // SERVICE
  // -------------------------------------------------------------------

  serviceId: string;

  serviceCode: string;

  serviceSlug: string;

  serviceType: string;

  serviceMethod: CustomerReviewEligibleBookingServiceMethod;

  // -------------------------------------------------------------------
  // COMPLETION
  // -------------------------------------------------------------------

  completedAt: string;

  completionSummary: string | null;
}

// =====================================================================
// CUSTOMER-FIRST ELIGIBLE BOOKING SEARCH
// =====================================================================

/**
 * Query parameters used to retrieve eligible completed bookings for
 * one selected Customer.
 *
 * customerId is REQUIRED.
 *
 * Endpoint:
 *
 * GET /api/v1/admin/customer-reviews/eligible-bookings
 *     ?customerId=<selected-customer-id>
 *     &keyword=<optional>
 *     &page=0
 *     &size=10
 *
 * customerId is only a workflow/query filter.
 *
 * It must not be copied into AdminCustomerReviewCreatePayload.
 */
export interface GetAdminCustomerReviewEligibleBookingsOptions {
  customerId: string;

  keyword?: string;

  page?: number;

  size?: number;

  sort?: string;

  signal?: AbortSignal;
}

// =====================================================================
// ADMIN CREATE REVIEW
// =====================================================================

/**
 * Final backend contract:
 * CustomerReviewAdminCreateRequest
 *
 * bookingRequestId is REQUIRED.
 *
 * The backend resolves:
 * - Customer
 * - reviewerEmail
 * - reviewerPhone
 * - WebsiteService
 * - isVerifiedCustomer
 *
 * Customer selection occurs before booking selection, but customerId
 * is intentionally absent from this create payload.
 *
 * The selected BookingRequest is the authoritative ownership record.
 */
export interface AdminCustomerReviewCreatePayload {
  bookingRequestId: string;

  reviewerDisplayPreference: CustomerReviewDisplayPreference;

  /**
   * Used directly only for CUSTOM display preference.
   *
   * Standard preferences are resolved from the reusable Customer by
   * the backend.
   */
  reviewerDisplayName: string | null;

  reviewTitle: string | null;

  reviewText: string;

  rating: number;

  reviewSource: CustomerReviewSource;

  externalSourceUrl: string | null;

  customerPhotoMediaId: string | null;

  customerConsentConfirmed: boolean;

  customerConsentVersion: string | null;
}

// =====================================================================
// ADMIN UPDATE REVIEW
// =====================================================================

/**
 * Final backend contract:
 * CustomerReviewUpdateRequest
 *
 * Booking/customer/service identity cannot be updated.
 */
export interface AdminCustomerReviewUpdatePayload {
  reviewerDisplayName: string | null;

  reviewerDisplayPreference: CustomerReviewDisplayPreference;

  reviewTitle: string | null;

  reviewText: string;

  rating: number;

  reviewSource: CustomerReviewSource;

  externalSourceUrl: string | null;

  customerPhotoMediaId: string | null;
}

// =====================================================================
// ADMIN SEARCH OPTIONS
// =====================================================================

export interface GetAdminCustomerReviewsOptions {
  keyword?: string;

  moderationStatus?: CustomerReviewModerationStatus;

  reviewSource?: CustomerReviewSource;

  rating?: number;

  /**
   * Valid review-list/search filter.
   *
   * This is unrelated to review creation ownership.
   */
  serviceId?: string;

  isVerifiedCustomer?: boolean;

  isPublic?: boolean;

  isFeatured?: boolean;

  isSpam?: boolean;

  page?: number;

  size?: number;

  signal?: AbortSignal;
}

// =====================================================================
// APPROVAL
// =====================================================================

export interface AdminCustomerReviewApprovalPayload {
  moderationNotes?: string | null;
}

// =====================================================================
// REJECTION
// =====================================================================

export interface AdminCustomerReviewRejectionPayload {
  rejectionReason: string;

  moderationNotes?: string | null;
}

// =====================================================================
// SPAM
// =====================================================================

export interface AdminCustomerReviewSpamPayload {
  spamScore?: number | null;

  moderationNotes?: string | null;
}

// =====================================================================
// PUBLICATION
// =====================================================================

export interface AdminCustomerReviewPublicationPayload {
  isFeatured?: boolean | null;
}

// =====================================================================
// FEATURED STATUS
// =====================================================================

export interface AdminCustomerReviewFeaturedPayload {
  isFeatured: boolean;
}

// =====================================================================
// HIDE
// =====================================================================

export interface AdminCustomerReviewHidePayload {
  moderationNotes?: string | null;
}

// =====================================================================
// ADMIN RESPONSE
// =====================================================================

export interface AdminCustomerReviewResponsePayload {
  adminResponse: string;
}
