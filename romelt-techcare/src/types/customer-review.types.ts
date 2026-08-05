/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW TYPES
 * ================================================================
 *
 * Purpose:
 * Defines all public customer-review, testimonial, rating-summary,
 * review-submission, media, and pagination structures used by the
 * React frontend.
 *
 * Responsibilities:
 * - Mirrors PublicCustomerReviewResponse.
 * - Mirrors CustomerReviewRatingSummaryResponse.
 * - Mirrors CustomerReviewPublicSubmissionRequest.
 * - Supports Spring Data paginated public-review responses.
 * - Keeps private customer and moderation data out of public UI.
 *
 * Real-data integration:
 * GET  /api/v1/public/customer-reviews
 * GET  /api/v1/public/customer-reviews/featured
 * GET  /api/v1/public/customer-reviews/service/{serviceSlug}
 * GET  /api/v1/public/customer-reviews/rating-summary
 * POST /api/v1/public/customer-reviews
 * ================================================================
 */

export type CustomerReviewSource =
  | "WEBSITE"
  | "BOOKING_FOLLOW_UP"
  | "EMAIL"
  | "PHONE"
  | "GOOGLE"
  | "FACEBOOK"
  | "OTHER";

export type CustomerReviewDisplayPreference =
  | "FULL_NAME"
  | "FIRST_NAME_LAST_INITIAL"
  | "FIRST_NAME_ONLY"
  | "ANONYMOUS"
  | "CUSTOM";

export interface PublicWebsiteMediaAsset {
  mediaAssetId?: string;
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

export interface PublicCustomerReview {
  customerReviewId: string;
  reviewerDisplayName: string | null;
  reviewTitle: string | null;
  reviewText: string | null;
  rating: number;
  reviewSource: CustomerReviewSource;
  externalSourceUrl: string | null;
  customerPhoto: PublicWebsiteMediaAsset | null;
  isVerifiedCustomer: boolean;
  isFeatured: boolean;
  serviceName: string | null;
  serviceSlug: string | null;
  adminResponse: string | null;
  respondedAt: string | null;
  publishedAt: string;
}

export interface CustomerReviewRatingSummary {
  totalReviews: number;
  averageRating: number;
  fiveStarReviews: number;
  fourStarReviews: number;
  threeStarReviews: number;
  twoStarReviews: number;
  oneStarReviews: number;
}

export interface CustomerReviewPublicSubmissionRequest {
  token: string;
  reviewerDisplayPreference: CustomerReviewDisplayPreference;
  reviewerDisplayName: string | null;
  reviewTitle: string | null;
  reviewText: string;
  rating: number;
  serviceId: string | null;
  customerPhotoMediaId: string | null;
  customerConsentConfirmed: true;
  customerConsentVersion: string;
}

export interface PageResponse<TContent> {
  content: TContent[];
  pageable?: {
    pageNumber?: number;
    pageSize?: number;
    offset?: number;
    paged?: boolean;
    unpaged?: boolean;
  };
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort?: {
    sorted?: boolean;
    unsorted?: boolean;
    empty?: boolean;
  };
}
