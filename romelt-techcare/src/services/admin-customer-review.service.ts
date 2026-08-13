/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEW SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides authenticated administrator API operations for customer
 * reviews.
 *
 * Core business rule:
 * Every customer review belongs to an actual completed,
 * review-eligible BookingRequest.
 *
 * Review ownership:
 *
 * Customer
 *   -> BookingRequest
 *       -> WebsiteService
 *           -> CustomerReview
 *
 * Administrator creation sends bookingRequestId as the authoritative
 * completed-service reference.
 *
 * The backend derives:
 * - Customer identity;
 * - reviewer email;
 * - reviewer phone;
 * - WebsiteService;
 * - verified-customer status.
 *
 * The browser must never independently supply those authoritative
 * values during customer-review creation or update.
 *
 * Responsibilities:
 * - Creates administrator-recorded customer reviews.
 * - Updates editable review content.
 * - Searches customer reviews using backend filtering and pagination.
 * - Retrieves one complete administrator-facing review.
 * - Approves and rejects reviews.
 * - Marks reviews as spam.
 * - Publishes and unpublishes reviews.
 * - Manages featured-review status.
 * - Hides and archives reviews.
 * - Creates and removes administrator public responses.
 *
 * Real-data integration:
 *
 * POST   /api/v1/admin/customer-reviews
 * GET    /api/v1/admin/customer-reviews
 * GET    /api/v1/admin/customer-reviews/{customerReviewId}
 * PUT    /api/v1/admin/customer-reviews/{customerReviewId}
 *
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/approve
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/reject
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/spam
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/publish
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/unpublish
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/featured
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/hide
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/archive
 *
 * PUT    /api/v1/admin/customer-reviews/{customerReviewId}/response
 * DELETE /api/v1/admin/customer-reviews/{customerReviewId}/response
 *
 * Important:
 * - All requests require administrator authentication.
 * - Public review APIs remain in customer-review.service.ts.
 * - Administrator responses contain private operational information
 *   and must not be reused directly by public website components.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

import type {
  AdminCustomerReview,
  AdminCustomerReviewApprovalPayload,
  AdminCustomerReviewCreatePayload,
  AdminCustomerReviewFeaturedPayload,
  AdminCustomerReviewHidePayload,
  AdminCustomerReviewPublicationPayload,
  AdminCustomerReviewRejectionPayload,
  AdminCustomerReviewResponsePayload,
  AdminCustomerReviewSpamPayload,
  AdminCustomerReviewUpdatePayload,
  GetAdminCustomerReviewsOptions,
  PageResponse,
} from "@/types/admin-customer-review.types";

// =====================================================================
// ENDPOINT
// =====================================================================

const ADMIN_CUSTOMER_REVIEWS_ENDPOINT = "/admin/customer-reviews";

// =====================================================================
// LIST / SEARCH
// =====================================================================

/**
 * Returns the administrator customer-review queue.
 *
 * Supported backend filters:
 * - keyword
 * - moderationStatus
 * - reviewSource
 * - rating
 * - serviceId
 * - isVerifiedCustomer
 * - isPublic
 * - isFeatured
 * - isSpam
 * - page
 * - size
 */
export function getAdminCustomerReviews(
  options: GetAdminCustomerReviewsOptions = {},
): Promise<PageResponse<AdminCustomerReview>> {
  const {
    keyword,

    moderationStatus,

    reviewSource,

    rating,

    serviceId,

    isVerifiedCustomer,

    isPublic,

    isFeatured,

    isSpam,

    page = 0,

    size = 10,

    signal,
  } = options;

  const searchParams = new URLSearchParams();

  // -------------------------------------------------------------------
  // PAGINATION
  // -------------------------------------------------------------------

  searchParams.set("page", String(Math.max(0, page)));

  searchParams.set("size", String(Math.min(Math.max(1, size), 50)));

  // -------------------------------------------------------------------
  // DEFAULT SORT
  // -------------------------------------------------------------------

  searchParams.set("sort", "submittedAt,desc");

  // -------------------------------------------------------------------
  // KEYWORD
  // -------------------------------------------------------------------

  const normalizedKeyword = normalizeOptional(keyword);

  if (normalizedKeyword) {
    searchParams.set("keyword", normalizedKeyword);
  }

  // -------------------------------------------------------------------
  // MODERATION
  // -------------------------------------------------------------------

  if (moderationStatus) {
    searchParams.set("moderationStatus", moderationStatus);
  }

  // -------------------------------------------------------------------
  // SOURCE
  // -------------------------------------------------------------------

  if (reviewSource) {
    searchParams.set("reviewSource", reviewSource);
  }

  // -------------------------------------------------------------------
  // RATING
  // -------------------------------------------------------------------

  if (
    rating !== undefined &&
    rating !== null &&
    Number.isFinite(rating) &&
    rating >= 1 &&
    rating <= 5
  ) {
    searchParams.set("rating", String(rating));
  }

  // -------------------------------------------------------------------
  // SERVICE FILTER
  // -------------------------------------------------------------------

  /*
   * serviceId remains a valid SEARCH filter.
   *
   * It is only removed from CREATE/UPDATE payloads because the backend
   * derives the review's service from its completed booking.
   */
  const normalizedServiceId = normalizeOptional(serviceId);

  if (normalizedServiceId) {
    searchParams.set("serviceId", normalizedServiceId);
  }

  // -------------------------------------------------------------------
  // BOOLEAN FILTERS
  // -------------------------------------------------------------------

  appendBoolean(searchParams, "isVerifiedCustomer", isVerifiedCustomer);

  appendBoolean(searchParams, "isPublic", isPublic);

  appendBoolean(searchParams, "isFeatured", isFeatured);

  appendBoolean(searchParams, "isSpam", isSpam);

  // -------------------------------------------------------------------
  // REQUEST
  // -------------------------------------------------------------------

  return apiClient.get<PageResponse<AdminCustomerReview>>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}?${searchParams.toString()}`,
    {
      signal,

      requireAuthentication: true,
    },
  );
}

// =====================================================================
// GET ONE
// =====================================================================

/**
 * Returns the complete administrator-facing review record.
 */
export function getAdminCustomerReview(
  customerReviewId: string,
  signal?: AbortSignal,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.get<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}`,
    {
      signal,

      requireAuthentication: true,
    },
  );
}

// =====================================================================
// CREATE
// =====================================================================

/**
 * Creates a customer review recorded by an administrator.
 *
 * The completed booking is mandatory.
 *
 * The backend derives:
 * - Customer
 * - reviewerEmail
 * - reviewerPhone
 * - WebsiteService
 * - isVerifiedCustomer = true
 *
 * Administrator-controlled review values:
 * - display preference
 * - optional custom display name
 * - title
 * - review text
 * - rating
 * - review source
 * - external source URL
 * - optional photo
 * - customer consent evidence
 *
 * The review is created as PENDING and is not automatically public.
 */
export function createAdminCustomerReview(
  request: AdminCustomerReviewCreatePayload,
  signal?: AbortSignal,
): Promise<AdminCustomerReview> {
  if (!request) {
    throw new Error("Customer review information is required.");
  }

  validateCreateRequest(request);

  const bookingRequestId = requireIdentifier(
    request.bookingRequestId,
    "Completed booking request ID",
  );

  const reviewText = requireText(request.reviewText, "Review text");

  return apiClient.post<AdminCustomerReview>(
    ADMIN_CUSTOMER_REVIEWS_ENDPOINT,
    {
      // ---------------------------------------------------------------
      // AUTHORITATIVE COMPLETED SERVICE
      // ---------------------------------------------------------------

      bookingRequestId,

      // ---------------------------------------------------------------
      // DISPLAY
      // ---------------------------------------------------------------

      reviewerDisplayPreference: request.reviewerDisplayPreference,

      reviewerDisplayName:
        request.reviewerDisplayPreference === "ANONYMOUS"
          ? null
          : normalizeOptional(request.reviewerDisplayName),

      // ---------------------------------------------------------------
      // REVIEW
      // ---------------------------------------------------------------

      reviewTitle: normalizeOptional(request.reviewTitle),

      reviewText,

      rating: request.rating,

      reviewSource: request.reviewSource,

      externalSourceUrl: normalizeOptional(request.externalSourceUrl),

      // ---------------------------------------------------------------
      // PHOTO
      // ---------------------------------------------------------------

      customerPhotoMediaId: normalizeNullableIdentifier(
        request.customerPhotoMediaId,
      ),

      // ---------------------------------------------------------------
      // CUSTOMER CONSENT
      // ---------------------------------------------------------------

      customerConsentConfirmed: request.customerConsentConfirmed === true,

      customerConsentVersion:
        request.customerConsentConfirmed === true
          ? normalizeOptional(request.customerConsentVersion)
          : null,
    },
    {
      signal,

      requireAuthentication: true,
    },
  );
}

// =====================================================================
// UPDATE REVIEW CONTENT
// =====================================================================

/**
 * Updates administrator-editable review content.
 *
 * This endpoint deliberately does NOT send:
 * - bookingRequestId
 * - customer identity
 * - reviewerEmail
 * - reviewerPhone
 * - serviceId
 * - isVerifiedCustomer
 *
 * Those values are immutable booking-derived history.
 */
export function updateAdminCustomerReview(
  customerReviewId: string,
  request: AdminCustomerReviewUpdatePayload,
  signal?: AbortSignal,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  if (!request) {
    throw new Error("Updated customer review information is required.");
  }

  validateUpdateRequest(request);

  const reviewText = requireText(request.reviewText, "Review text");

  return apiClient.put<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}`,
    {
      reviewerDisplayPreference: request.reviewerDisplayPreference,

      reviewerDisplayName:
        request.reviewerDisplayPreference === "ANONYMOUS"
          ? null
          : normalizeOptional(request.reviewerDisplayName),

      reviewTitle: normalizeOptional(request.reviewTitle),

      reviewText,

      rating: request.rating,

      reviewSource: request.reviewSource,

      externalSourceUrl: normalizeOptional(request.externalSourceUrl),

      customerPhotoMediaId: normalizeNullableIdentifier(
        request.customerPhotoMediaId,
      ),
    },
    {
      signal,

      requireAuthentication: true,
    },
  );
}

// =====================================================================
// APPROVE
// =====================================================================

/**
 * Approves a customer review.
 *
 * Approval does not automatically publish the review.
 */
export function approveAdminCustomerReview(
  customerReviewId: string,
  request: AdminCustomerReviewApprovalPayload = {},
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/approve`,
    {
      moderationNotes: normalizeOptional(request.moderationNotes),
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// REJECT
// =====================================================================

/**
 * Rejects a customer review.
 */
export function rejectAdminCustomerReview(
  customerReviewId: string,
  request: AdminCustomerReviewRejectionPayload,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  if (!request) {
    throw new Error("Review rejection information is required.");
  }

  const rejectionReason = normalizeOptional(request.rejectionReason);

  if (!rejectionReason) {
    throw new Error("Rejection reason is required.");
  }

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/reject`,
    {
      rejectionReason,

      moderationNotes: normalizeOptional(request.moderationNotes),
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// SPAM
// =====================================================================

/**
 * Marks a customer review as spam.
 */
export function markAdminCustomerReviewAsSpam(
  customerReviewId: string,
  request: AdminCustomerReviewSpamPayload = {},
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  const normalizedSpamScore = normalizeSpamScore(request.spamScore);

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/spam`,
    {
      spamScore: normalizedSpamScore,

      moderationNotes: normalizeOptional(request.moderationNotes),
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// PUBLISH
// =====================================================================

/**
 * Publishes an approved customer review.
 *
 * Backend publication rules determine whether publication is allowed,
 * including approval, customer consent, and spam state.
 */
export function publishAdminCustomerReview(
  customerReviewId: string,
  request: AdminCustomerReviewPublicationPayload = {},
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/publish`,
    {
      isFeatured: request.isFeatured ?? false,
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// UNPUBLISH
// =====================================================================

/**
 * Removes a customer review from public display.
 */
export function unpublishAdminCustomerReview(
  customerReviewId: string,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/unpublish`,
    {},
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// FEATURE / UNFEATURE
// =====================================================================

/**
 * Updates the featured state of a published review.
 */
export function updateAdminCustomerReviewFeaturedStatus(
  customerReviewId: string,
  isFeatured: boolean,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  const request: AdminCustomerReviewFeaturedPayload = {
    isFeatured,
  };

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/featured`,
    request,
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// HIDE
// =====================================================================

/**
 * Hides a customer review.
 */
export function hideAdminCustomerReview(
  customerReviewId: string,
  request: AdminCustomerReviewHidePayload = {},
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/hide`,
    {
      moderationNotes: normalizeOptional(request.moderationNotes),
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// ARCHIVE
// =====================================================================

/**
 * Archives a customer review while retaining its historical record.
 */
export function archiveAdminCustomerReview(
  customerReviewId: string,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.patch<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/archive`,
    {},
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// SAVE ADMIN RESPONSE
// =====================================================================

/**
 * Adds or replaces the Romelt TechCare administrator response
 * associated with a review.
 */
export function saveAdminCustomerReviewResponse(
  customerReviewId: string,
  request: AdminCustomerReviewResponsePayload,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  if (!request) {
    throw new Error("Administrator response information is required.");
  }

  const adminResponse = normalizeOptional(request.adminResponse);

  if (!adminResponse) {
    throw new Error("Administrator response is required.");
  }

  return apiClient.put<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/response`,
    {
      adminResponse,
    },
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// REMOVE ADMIN RESPONSE
// =====================================================================

/**
 * Removes the administrator's public response from a review.
 */
export function removeAdminCustomerReviewResponse(
  customerReviewId: string,
): Promise<AdminCustomerReview> {
  const normalizedReviewId = requireIdentifier(
    customerReviewId,
    "Customer review ID",
  );

  return apiClient.delete<AdminCustomerReview>(
    `${ADMIN_CUSTOMER_REVIEWS_ENDPOINT}/${encodeURIComponent(
      normalizedReviewId,
    )}/response`,
    {
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// CREATE VALIDATION
// =====================================================================

function validateCreateRequest(request: AdminCustomerReviewCreatePayload) {
  requireIdentifier(request.bookingRequestId, "Completed booking request ID");

  validateEditableReviewFields(request);

  if (
    request.customerConsentConfirmed &&
    !normalizeOptional(request.customerConsentVersion)
  ) {
    throw new Error(
      "Customer consent version is required when customer consent is confirmed.",
    );
  }
}

// =====================================================================
// UPDATE VALIDATION
// =====================================================================

function validateUpdateRequest(request: AdminCustomerReviewUpdatePayload) {
  validateEditableReviewFields(request);
}

// =====================================================================
// SHARED REVIEW VALIDATION
// =====================================================================

function validateEditableReviewFields(
  request: AdminCustomerReviewCreatePayload | AdminCustomerReviewUpdatePayload,
) {
  if (!request.reviewerDisplayPreference) {
    throw new Error("Reviewer display preference is required.");
  }

  if (!request.reviewSource) {
    throw new Error("Review source is required.");
  }

  if (
    request.reviewerDisplayPreference === "CUSTOM" &&
    !normalizeOptional(request.reviewerDisplayName)
  ) {
    throw new Error(
      "Custom reviewer display name is required when CUSTOM is selected.",
    );
  }

  const reviewText = requireText(request.reviewText, "Review text");

  if (reviewText.length > 10_000) {
    throw new Error("Review text must not exceed 10,000 characters.");
  }

  if (
    !Number.isFinite(request.rating) ||
    request.rating < 1 ||
    request.rating > 5
  ) {
    throw new Error("Review rating must be between 1 and 5.");
  }

  const reviewTitle = normalizeOptional(request.reviewTitle);

  if (reviewTitle && reviewTitle.length > 255) {
    throw new Error("Review title must not exceed 255 characters.");
  }

  const reviewerDisplayName = normalizeOptional(request.reviewerDisplayName);

  if (reviewerDisplayName && reviewerDisplayName.length > 180) {
    throw new Error("Reviewer display name must not exceed 180 characters.");
  }

  const externalSourceUrl = normalizeOptional(request.externalSourceUrl);

  if (externalSourceUrl && externalSourceUrl.length > 1500) {
    throw new Error("External source URL must not exceed 1,500 characters.");
  }
}

// =====================================================================
// BOOLEAN QUERY HELPER
// =====================================================================

function appendBoolean(
  searchParams: URLSearchParams,
  key: string,
  value: boolean | undefined,
) {
  if (typeof value === "boolean") {
    searchParams.set(key, String(value));
  }
}

// =====================================================================
// REQUIRED IDENTIFIER
// =====================================================================

function requireIdentifier(
  value: string | null | undefined,
  label: string,
): string {
  const normalized = value?.trim();

  if (!normalized) {
    throw new Error(`${label} is required.`);
  }

  return normalized;
}

// =====================================================================
// NULLABLE IDENTIFIER
// =====================================================================

function normalizeNullableIdentifier(
  value: string | null | undefined,
): string | null {
  return normalizeOptional(value);
}

// =====================================================================
// REQUIRED TEXT
// =====================================================================

function requireText(value: string | null | undefined, label: string): string {
  const normalized = normalizeOptional(value);

  if (!normalized) {
    throw new Error(`${label} is required.`);
  }

  return normalized;
}

// =====================================================================
// OPTIONAL STRING NORMALIZATION
// =====================================================================

function normalizeOptional(value: string | null | undefined): string | null {
  if (value == null) {
    return null;
  }

  const normalized = value.trim();

  return normalized || null;
}

// =====================================================================
// SPAM SCORE
// =====================================================================

function normalizeSpamScore(value: number | null | undefined): number | null {
  if (value == null) {
    return null;
  }

  if (!Number.isFinite(value)) {
    return null;
  }

  return Math.min(100, Math.max(0, value));
}
