/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC CUSTOMER REVIEW SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides all public customer-review API operations used by the
 * Romelt TechCare website.
 *
 * Responsibilities:
 * - Retrieves approved public reviews.
 * - Retrieves featured testimonials.
 * - Retrieves service-specific reviews.
 * - Retrieves public rating aggregates.
 * - Submits secure invitation-based customer reviews.
 * - Supports request cancellation.
 *
 * Real-data integration:
 * GET  /api/v1/public/customer-reviews
 * GET  /api/v1/public/customer-reviews/featured
 * GET  /api/v1/public/customer-reviews/service/{serviceSlug}
 * GET  /api/v1/public/customer-reviews/rating-summary
 * POST /api/v1/public/customer-reviews
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";
import type {
  CustomerReviewPublicSubmissionRequest,
  CustomerReviewRatingSummary,
  PageResponse,
  PublicCustomerReview,
} from "@/types/customer-review.types";

const CUSTOMER_REVIEWS_ENDPOINT = "/public/customer-reviews";

export function getPublicCustomerReviews(
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<PublicCustomerReview>> {
  const searchParameters = new URLSearchParams({
    page: String(Math.max(0, page)),
    size: String(Math.max(1, size)),
    sort: "publishedAt,desc",
  });

  return apiClient.get<PageResponse<PublicCustomerReview>>(
    `${CUSTOMER_REVIEWS_ENDPOINT}?${searchParameters.toString()}`,
    {
      signal,
      requireAuthentication: false,
    },
  );
}

export function getFeaturedCustomerReviews(
  signal?: AbortSignal,
): Promise<PublicCustomerReview[]> {
  return apiClient.get<PublicCustomerReview[]>(
    `${CUSTOMER_REVIEWS_ENDPOINT}/featured`,
    {
      signal,
      requireAuthentication: false,
    },
  );
}

export function getCustomerReviewsByServiceSlug(
  serviceSlug: string,
  page = 0,
  size = 10,
  signal?: AbortSignal,
): Promise<PageResponse<PublicCustomerReview>> {
  const normalizedSlug = serviceSlug.trim();

  if (!normalizedSlug) {
    return Promise.reject(new Error("Service slug is required."));
  }

  const searchParameters = new URLSearchParams({
    page: String(Math.max(0, page)),
    size: String(Math.max(1, size)),
    sort: "publishedAt,desc",
  });

  return apiClient.get<PageResponse<PublicCustomerReview>>(
    `${CUSTOMER_REVIEWS_ENDPOINT}/service/${encodeURIComponent(
      normalizedSlug,
    )}?${searchParameters.toString()}`,
    {
      signal,
      requireAuthentication: false,
    },
  );
}

export function getCustomerReviewRatingSummary(
  signal?: AbortSignal,
): Promise<CustomerReviewRatingSummary> {
  return apiClient.get<CustomerReviewRatingSummary>(
    `${CUSTOMER_REVIEWS_ENDPOINT}/rating-summary`,
    {
      signal,
      requireAuthentication: false,
    },
  );
}

export function submitPublicCustomerReview(
  request: CustomerReviewPublicSubmissionRequest,
  signal?: AbortSignal,
): Promise<PublicCustomerReview> {
  return apiClient.post<PublicCustomerReview>(
    CUSTOMER_REVIEWS_ENDPOINT,
    request,
    {
      signal,
      requireAuthentication: false,
    },
  );
}
