/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW CARD
 * ================================================================
 *
 * Purpose:
 * Displays one approved public customer review as a testimonial.
 *
 * Responsibilities:
 * - Displays reviewer identity without exposing private information.
 * - Displays rating, title, review text, service, and publication date.
 * - Displays verified-customer and featured indicators.
 * - Displays an optional customer photo or initials fallback.
 * - Displays an optional Romelt TechCare administrator response.
 * ================================================================
 */

import {
  BadgeCheck,
  MessageSquareQuote,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

import { StarRating } from "@/components/reviews/StarRating";
import type { PublicCustomerReview } from "@/types/customer-review.types";

interface CustomerReviewCardProps {
  review: PublicCustomerReview;
  compact?: boolean;
}

export function CustomerReviewCard({
  review,
  compact = false,
}: CustomerReviewCardProps) {
  const displayName =
    review.reviewerDisplayName?.trim() || "Anonymous Customer";

  const photoUrl = review.customerPhoto?.publicUrl?.trim() || null;

  const photoAlt =
    review.customerPhoto?.altText?.trim() ||
    `${displayName} customer profile photo`;

  return (
    <article className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition duration-300 hover:-translate-y-1 hover:border-[#1976D2]/30 hover:shadow-xl sm:p-7">
      <div
        aria-hidden="true"
        className="absolute right-0 top-0 h-28 w-28 rounded-bl-[5rem] bg-gradient-to-bl from-[#EAF4FD] to-transparent transition duration-300 group-hover:scale-110"
      />

      <div className="relative flex h-full flex-col">
        <div className="flex items-start justify-between gap-4">
          <StarRating value={review.rating} size="sm" />

          <div className="flex items-center gap-2">
            {review.isFeatured && (
              <span className="inline-flex items-center gap-1 rounded-full bg-[#FFF8E1] px-2.5 py-1 text-xs font-extrabold text-[#8A6700]">
                <Sparkles className="size-3.5" aria-hidden="true" />
                Featured
              </span>
            )}

            <MessageSquareQuote
              className="size-7 text-[#1976D2]/25"
              aria-hidden="true"
            />
          </div>
        </div>

        {review.reviewTitle && (
          <h3 className="mt-5 text-xl font-black text-[#0B2545]">
            {review.reviewTitle}
          </h3>
        )}

        <blockquote
          className={[
            "mt-4 leading-7 text-slate-600",
            compact ? "line-clamp-5" : "",
          ].join(" ")}
        >
          “
          {review.reviewText ||
            "The customer did not provide written feedback."}
          ”
        </blockquote>

        {review.adminResponse && (
          <div className="mt-5 rounded-2xl border border-[#B9D8F7] bg-[#EAF4FD] p-4">
            <div className="flex items-center gap-2 text-sm font-extrabold text-[#0B2545]">
              <ShieldCheck
                className="size-4 text-[#1976D2]"
                aria-hidden="true"
              />
              Response from Romelt TechCare
            </div>

            <p className="mt-2 text-sm leading-6 text-slate-600">
              {review.adminResponse}
            </p>
          </div>
        )}

        <footer className="mt-auto flex items-center gap-4 border-t border-slate-100 pt-6">
          {photoUrl ? (
            <img
              src={photoUrl}
              alt={photoAlt}
              className="size-12 shrink-0 rounded-full border-2 border-white object-cover shadow"
              loading="lazy"
            />
          ) : (
            <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#1976D2] to-[#0B2545] text-sm font-black text-white shadow">
              {createInitials(displayName)}
            </div>
          )}

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <p className="truncate font-extrabold text-[#0B2545]">
                {displayName}
              </p>

              {review.isVerifiedCustomer && (
                <span
                  title="Verified customer"
                  className="inline-flex items-center text-[#1976D2]"
                >
                  <BadgeCheck className="size-4" aria-hidden="true" />
                  <span className="sr-only">Verified customer</span>
                </span>
              )}
            </div>

            <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs font-semibold text-slate-500">
              {review.serviceName && <span>{review.serviceName}</span>}

              {review.serviceName && review.publishedAt && (
                <span aria-hidden="true">•</span>
              )}

              {review.publishedAt && (
                <time dateTime={review.publishedAt}>
                  {formatReviewDate(review.publishedAt)}
                </time>
              )}
            </div>
          </div>
        </footer>
      </div>
    </article>
  );
}

function createInitials(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).filter(Boolean).slice(0, 2);

  if (parts.length === 0) {
    return "C";
  }

  return parts.map((part) => part.charAt(0).toUpperCase()).join("");
}

function formatReviewDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    year: "numeric",
  }).format(date);
}
