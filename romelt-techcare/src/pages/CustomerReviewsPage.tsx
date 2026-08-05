/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEWS PAGE
 * ================================================================
 *
 * Purpose:
 * Displays all approved and published customer reviews.
 *
 * Responsibilities:
 * - Loads paginated reviews from the real backend.
 * - Displays aggregate customer-rating information.
 * - Supports pagination.
 * - Displays loading, empty, and backend-error states.
 * - Directs customers toward booking a service.
 *
 * Real-data integration:
 * GET /api/v1/public/customer-reviews
 * GET /api/v1/public/customer-reviews/rating-summary
 * ================================================================
 */

import { useEffect, useState } from "react";
import {
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  LoaderCircle,
  MessageSquareQuote,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import { Link } from "react-router";

import { CustomerRatingSummary } from "@/components/reviews/CustomerRatingSummary";
import { CustomerReviewCard } from "@/components/reviews/CustomerReviewCard";
import {
  getCustomerReviewRatingSummary,
  getPublicCustomerReviews,
} from "@/services/customer-review.service";
import type {
  CustomerReviewRatingSummary as CustomerReviewRatingSummaryType,
  PageResponse,
  PublicCustomerReview,
} from "@/types/customer-review.types";

const PAGE_SIZE = 9;

export function CustomerReviewsPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<PageResponse<PublicCustomerReview> | null>(
    null,
  );
  const [summary, setSummary] =
    useState<CustomerReviewRatingSummaryType | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadReviews() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [reviewsPage, ratingSummary] = await Promise.all([
          getPublicCustomerReviews(pageNumber, PAGE_SIZE, controller.signal),
          getCustomerReviewRatingSummary(controller.signal),
        ]);

        setPage(reviewsPage);
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

    void loadReviews();

    return () => controller.abort();
  }, [pageNumber, reloadKey]);

  return (
    <>
      <section className="relative isolate overflow-hidden bg-gradient-to-br from-[#0B2545] via-[#12365F] to-[#1976D2] py-16 text-white sm:py-20">
        <div
          aria-hidden="true"
          className="absolute -left-32 -top-32 -z-10 h-96 w-96 rounded-full bg-[#1976D2]/30 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -bottom-40 right-0 -z-10 h-96 w-96 rounded-full bg-[#D4AF37]/15 blur-3xl"
        />

        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-extrabold uppercase tracking-[0.16em] text-[#D4AF37] backdrop-blur-sm">
              <MessageSquareQuote className="size-4" aria-hidden="true" />
              Customer Reviews
            </div>

            <h1 className="mt-6 text-4xl font-black tracking-tight sm:text-5xl lg:text-6xl">
              Real experiences from customers we have served
            </h1>

            <p className="mt-6 max-w-2xl text-lg leading-8 text-[#EAF4FD]">
              See how Romelt TechCare helps customers solve technology problems
              through clear communication, honest recommendations, and
              dependable support.
            </p>

            <div className="mt-7 flex items-center gap-3 text-sm font-bold text-[#EAF4FD]">
              <ShieldCheck
                className="size-5 text-[#D4AF37]"
                aria-hidden="true"
              />
              Only approved public reviews are displayed.
            </div>
          </div>
        </div>
      </section>

      <section className="bg-[#F8FAFC] py-12 sm:py-16 lg:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          {isLoading && (
            <div className="flex min-h-96 items-center justify-center">
              <div className="text-center">
                <LoaderCircle
                  className="mx-auto size-10 animate-spin text-[#1976D2]"
                  aria-hidden="true"
                />

                <p className="mt-4 font-bold text-slate-600">
                  Loading customer reviews…
                </p>
              </div>
            </div>
          )}

          {!isLoading && errorMessage && (
            <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center">
              <CircleAlert
                className="mx-auto size-10 text-[#C62828]"
                aria-hidden="true"
              />

              <h2 className="mt-4 text-2xl font-black text-[#0B2545]">
                Reviews could not be loaded
              </h2>

              <p className="mx-auto mt-3 max-w-xl leading-7 text-slate-600">
                {errorMessage}
              </p>

              <button
                type="button"
                onClick={() => setReloadKey((current) => current + 1)}
                className="focus-ring mt-6 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-6 py-3 font-extrabold text-white transition hover:bg-[#1565C0]"
              >
                <RefreshCw className="size-5" aria-hidden="true" />
                Try Again
              </button>
            </div>
          )}

          {!isLoading && !errorMessage && summary && page && (
            <div className="grid gap-8 lg:grid-cols-[320px_minmax(0,1fr)]">
              <aside className="lg:sticky lg:top-24 lg:self-start">
                <CustomerRatingSummary summary={summary} />

                <div className="mt-5 rounded-3xl bg-[#0B2545] p-6 text-white shadow-lg">
                  <h2 className="text-xl font-black">
                    Need technology support?
                  </h2>

                  <p className="mt-3 leading-7 text-[#EAF4FD]">
                    Tell us what is not working and receive clear guidance on
                    the most practical next step.
                  </p>

                  <Link
                    to="/book"
                    className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#D4AF37] px-5 py-3 font-extrabold !text-[#0B2545] transition hover:bg-[#E0BE4C] hover:!text-[#0B2545]"
                  >
                    Book a Service
                    <ArrowRight className="size-5" aria-hidden="true" />
                  </Link>
                </div>
              </aside>

              <main>
                {page.content.length > 0 ? (
                  <>
                    <div className="grid items-stretch gap-6 md:grid-cols-2 xl:grid-cols-3">
                      {page.content.map((review) => (
                        <CustomerReviewCard
                          key={review.customerReviewId}
                          review={review}
                        />
                      ))}
                    </div>

                    {page.totalPages > 1 && (
                      <nav
                        className="mt-10 flex flex-col items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 sm:flex-row"
                        aria-label="Customer review pagination"
                      >
                        <p className="text-sm font-semibold text-slate-600">
                          Page {page.number + 1} of {page.totalPages}
                          <span className="mx-2" aria-hidden="true">
                            •
                          </span>
                          {page.totalElements.toLocaleString()} reviews
                        </p>

                        <div className="flex gap-3">
                          <button
                            type="button"
                            disabled={page.first}
                            onClick={() =>
                              setPageNumber((current) =>
                                Math.max(0, current - 1),
                              )
                            }
                            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 font-extrabold text-[#0B2545] transition hover:border-[#1976D2] disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <ChevronLeft
                              className="size-4"
                              aria-hidden="true"
                            />
                            Previous
                          </button>

                          <button
                            type="button"
                            disabled={page.last}
                            onClick={() =>
                              setPageNumber((current) => current + 1)
                            }
                            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-4 py-2 font-extrabold text-white transition hover:bg-[#1565C0] disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            Next
                            <ChevronRight
                              className="size-4"
                              aria-hidden="true"
                            />
                          </button>
                        </div>
                      </nav>
                    )}
                  </>
                ) : (
                  <div className="flex min-h-96 items-center justify-center rounded-3xl border border-dashed border-slate-300 bg-white p-8 text-center">
                    <div className="max-w-lg">
                      <MessageSquareQuote
                        className="mx-auto size-12 text-[#1976D2]"
                        aria-hidden="true"
                      />

                      <h2 className="mt-5 text-2xl font-black text-[#0B2545]">
                        No published reviews yet
                      </h2>

                      <p className="mt-3 leading-7 text-slate-600">
                        Approved customer reviews will appear here after
                        customers complete a service and consent to public
                        publication.
                      </p>
                    </div>
                  </div>
                )}
              </main>
            </div>
          )}
        </div>
      </section>
    </>
  );
}
