/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER RATING SUMMARY
 * ================================================================
 *
 * Purpose:
 * Displays the public aggregate rating and star distribution.
 *
 * Responsibilities:
 * - Displays average rating and total review count.
 * - Displays five-to-one-star distribution.
 * - Handles a zero-review state safely.
 * - Uses accessible progress indicators.
 * ================================================================
 */

import { MessageSquareText, ShieldCheck } from "lucide-react";

import { StarRating } from "@/components/reviews/StarRating";
import type { CustomerReviewRatingSummary } from "@/types/customer-review.types";

interface CustomerRatingSummaryProps {
  summary: CustomerReviewRatingSummary;
  compact?: boolean;
}

export function CustomerRatingSummary({
  summary,
  compact = false,
}: CustomerRatingSummaryProps) {
  const distributions = [
    { rating: 5, count: summary.fiveStarReviews },
    { rating: 4, count: summary.fourStarReviews },
    { rating: 3, count: summary.threeStarReviews },
    { rating: 2, count: summary.twoStarReviews },
    { rating: 1, count: summary.oneStarReviews },
  ];

  const hasReviews = summary.totalReviews > 0;

  return (
    <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="bg-gradient-to-br from-[#1976D2] to-[#0B2545] p-6 text-white sm:p-7">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-[#D4AF37]">
              Customer Rating
            </p>

            <div className="mt-3 flex items-end gap-3">
              <span className="text-5xl font-black leading-none">
                {hasReviews ? summary.averageRating.toFixed(1) : "—"}
              </span>

              <span className="pb-1 text-sm font-bold text-[#EAF4FD]">
                out of 5
              </span>
            </div>

            <div className="mt-4">
              <StarRating
                value={hasReviews ? summary.averageRating : 0}
                size="md"
              />
            </div>
          </div>

          <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-white/10">
            <MessageSquareText
              className="size-7 text-[#D4AF37]"
              aria-hidden="true"
            />
          </div>
        </div>

        <p className="mt-5 flex items-center gap-2 text-sm font-semibold text-[#EAF4FD]">
          <ShieldCheck className="size-4 text-[#D4AF37]" aria-hidden="true" />
          {hasReviews
            ? `Based on ${formatReviewCount(summary.totalReviews)}`
            : "No published customer reviews yet"}
        </p>
      </div>

      {!compact && hasReviews && (
        <div className="space-y-3 p-6 sm:p-7">
          {distributions.map(({ rating, count }) => {
            const percentage =
              summary.totalReviews === 0
                ? 0
                : Math.round((count / summary.totalReviews) * 100);

            return (
              <div
                key={rating}
                className="grid grid-cols-[48px_minmax(0,1fr)_42px] items-center gap-3"
              >
                <span className="text-sm font-extrabold text-[#0B2545]">
                  {rating} star
                </span>

                <div
                  className="h-2.5 overflow-hidden rounded-full bg-slate-100"
                  role="progressbar"
                  aria-label={`${rating}-star reviews`}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={percentage}
                >
                  <div
                    className="h-full rounded-full bg-[#D4AF37]"
                    style={{ width: `${percentage}%` }}
                  />
                </div>

                <span className="text-right text-sm font-bold text-slate-500">
                  {count}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function formatReviewCount(totalReviews: number): string {
  return `${totalReviews.toLocaleString()} ${
    totalReviews === 1 ? "review" : "reviews"
  }`;
}
