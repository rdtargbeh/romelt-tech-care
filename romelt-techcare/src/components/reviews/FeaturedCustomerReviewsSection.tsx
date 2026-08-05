/**
 * ================================================================
 * ROMELT TECHCARE — FEATURED CUSTOMER REVIEWS SECTION
 * ================================================================
 *
 * Purpose:
 * Loads and displays real featured testimonials and the aggregate
 * customer rating on public pages.
 *
 * Responsibilities:
 * - Calls the real customer-review backend.
 * - Loads featured reviews and rating summary concurrently.
 * - Cancels requests when unmounted.
 * - Displays loading, empty, and failure states.
 * - Links visitors to the complete customer-reviews page.
 *
 * Real-data integration:
 * GET /api/v1/public/customer-reviews/featured
 * GET /api/v1/public/customer-reviews/rating-summary
 * ================================================================
 */

import { useEffect, useState } from "react";
import {
  ArrowRight,
  CircleAlert,
  LoaderCircle,
  MessageSquareQuote,
  RefreshCw,
} from "lucide-react";
import { Link } from "react-router";

import { CustomerRatingSummary } from "@/components/reviews/CustomerRatingSummary";
import { CustomerReviewCard } from "@/components/reviews/CustomerReviewCard";
import {
  getCustomerReviewRatingSummary,
  getFeaturedCustomerReviews,
} from "@/services/customer-review.service";
import type {
  CustomerReviewRatingSummary as CustomerReviewRatingSummaryType,
  PublicCustomerReview,
} from "@/types/customer-review.types";

export function FeaturedCustomerReviewsSection() {
  const [reviews, setReviews] = useState<PublicCustomerReview[]>([]);
  const [summary, setSummary] =
    useState<CustomerReviewRatingSummaryType | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadCustomerReviewContent() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [featuredReviews, ratingSummary] = await Promise.all([
          getFeaturedCustomerReviews(controller.signal),
          getCustomerReviewRatingSummary(controller.signal),
        ]);

        setReviews(featuredReviews);
        setSummary(ratingSummary);
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        setErrorMessage(
          error instanceof Error
            ? error.message
            : "Customer reviews could not be loaded.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadCustomerReviewContent();

    return () => controller.abort();
  }, [reloadKey]);

  return (
    <section className="relative overflow-hidden bg-[#F8FAFC] py-16 sm:py-20 lg:py-24">
      <div
        aria-hidden="true"
        className="absolute -right-40 top-12 h-96 w-96 rounded-full bg-[#1976D2]/10 blur-3xl"
      />

      <div
        aria-hidden="true"
        className="absolute -left-40 bottom-0 h-96 w-96 rounded-full bg-[#D4AF37]/10 blur-3xl"
      />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="inline-flex items-center gap-2 rounded-full bg-[#EAF4FD] px-4 py-2 text-sm font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
              <MessageSquareQuote className="size-4" aria-hidden="true" />
              Customer Experiences
            </div>

            <h2 className="mt-5 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl lg:text-5xl">
              Trusted technology care, shared by our customers
            </h2>

            <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-600">
              Read verified feedback from customers who trusted Romelt TechCare
              with their technology needs.
            </p>
          </div>

          <Link
            to="/reviews"
            className="inline-flex w-fit items-center gap-2 font-extrabold !text-[#1976D2] transition hover:gap-3 hover:!text-[#0B2545]"
          >
            Read all customer reviews
            <ArrowRight className="size-5" aria-hidden="true" />
          </Link>
        </header>

        {isLoading && <FeaturedReviewsLoadingState />}

        {!isLoading && errorMessage && (
          <div className="mt-12 rounded-3xl border border-red-200 bg-red-50 p-6 text-center">
            <CircleAlert
              className="mx-auto size-9 text-[#C62828]"
              aria-hidden="true"
            />

            <h3 className="mt-3 text-lg font-extrabold text-[#0B2545]">
              Customer reviews are temporarily unavailable
            </h3>

            <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-600">
              {errorMessage}
            </p>

            <button
              type="button"
              onClick={() => setReloadKey((current) => current + 1)}
              className="focus-ring mt-5 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-5 py-2.5 font-extrabold text-white transition hover:bg-[#1565C0]"
            >
              <RefreshCw className="size-4" aria-hidden="true" />
              Try Again
            </button>
          </div>
        )}

        {!isLoading && !errorMessage && summary && (
          <div className="mt-12 grid gap-7 lg:grid-cols-[320px_minmax(0,1fr)]">
            <CustomerRatingSummary summary={summary} />

            {reviews.length > 0 ? (
              <div className="grid gap-6 md:grid-cols-2">
                {reviews.slice(0, 4).map((review) => (
                  <CustomerReviewCard
                    key={review.customerReviewId}
                    review={review}
                    compact
                  />
                ))}
              </div>
            ) : (
              <div className="flex min-h-72 items-center justify-center rounded-3xl border border-dashed border-slate-300 bg-white p-8 text-center">
                <div className="max-w-md">
                  <MessageSquareQuote
                    className="mx-auto size-10 text-[#1976D2]"
                    aria-hidden="true"
                  />

                  <h3 className="mt-4 text-xl font-extrabold text-[#0B2545]">
                    Customer stories are coming soon
                  </h3>

                  <p className="mt-3 leading-7 text-slate-600">
                    Approved customer reviews will appear here after services
                    are completed and customers choose to share their
                    experience.
                  </p>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

function FeaturedReviewsLoadingState() {
  return (
    <div className="mt-12 grid gap-7 lg:grid-cols-[320px_minmax(0,1fr)]">
      <div className="flex min-h-80 items-center justify-center rounded-3xl border border-slate-200 bg-white">
        <div className="text-center">
          <LoaderCircle
            className="mx-auto size-9 animate-spin text-[#1976D2]"
            aria-hidden="true"
          />

          <p className="mt-3 font-bold text-slate-600">
            Loading customer ratings…
          </p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {Array.from({ length: 4 }, (_, index) => (
          <div
            key={index}
            className="min-h-72 animate-pulse rounded-3xl border border-slate-200 bg-white p-6"
          >
            <div className="h-5 w-28 rounded bg-slate-200" />
            <div className="mt-6 h-5 w-3/4 rounded bg-slate-200" />
            <div className="mt-4 h-4 w-full rounded bg-slate-100" />
            <div className="mt-2 h-4 w-5/6 rounded bg-slate-100" />
            <div className="mt-10 h-12 w-40 rounded bg-slate-200" />
          </div>
        ))}
      </div>
    </div>
  );
}
