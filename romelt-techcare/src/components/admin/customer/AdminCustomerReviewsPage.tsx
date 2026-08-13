/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEWS PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the administrator customer-review management queue.
 *
 * Responsibilities:
 * - Loads all customer reviews from the protected backend.
 * - Searches customer reviews.
 * - Filters by moderation status, review source, rating, publication,
 *   and verified-customer status.
 * - Supports backend pagination.
 * - Displays review rating, moderation state, publication state,
 *   verification status, source, customer, and booking reference.
 * - Opens the administrator review detail page.
 * - Provides direct access to administrator review creation.
 *
 * Route:
 * /admin/customers/reviews
 *
 * Navigation:
 * /admin/customers
 * /admin/customers/reviews/new
 * /admin/customers/reviews/:customerReviewId
 *
 * Real-data integration:
 * GET /api/v1/admin/customer-reviews
 *
 * Important:
 * - This is an administrator page.
 * - Public review presentation remains separate.
 * - Review moderation actions are handled by
 *   AdminCustomerReviewDetailsPage.
 * ================================================================
 */

import {
  BadgeCheck,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Eye,
  Globe2,
  LoaderCircle,
  MessageSquareQuote,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
  Star,
  X,
} from "lucide-react";

import { type FormEvent, type ReactNode, useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { getAdminCustomerReviews } from "@/services/admin-customer-review.service";

import type {
  AdminCustomerReview,
  CustomerReviewModerationStatus,
  CustomerReviewSource,
  PageResponse,
} from "@/types/admin-customer-review.types";

// =====================================================================
// PAGINATION
// =====================================================================

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50] as const;

// =====================================================================
// FILTER OPTIONS
// =====================================================================

const MODERATION_STATUS_OPTIONS: Array<{
  value: CustomerReviewModerationStatus | "";
  label: string;
}> = [
  {
    value: "",
    label: "All moderation states",
  },
  {
    value: "PENDING",
    label: "Pending",
  },
  {
    value: "APPROVED",
    label: "Approved",
  },
  {
    value: "REJECTED",
    label: "Rejected",
  },
  {
    value: "SPAM",
    label: "Spam",
  },
  {
    value: "HIDDEN",
    label: "Hidden",
  },
  {
    value: "ARCHIVED",
    label: "Archived",
  },
];

const SOURCE_OPTIONS: Array<{
  value: CustomerReviewSource | "";
  label: string;
}> = [
  {
    value: "",
    label: "All sources",
  },
  {
    value: "BOOKING_FOLLOW_UP",
    label: "Booking follow-up",
  },
  {
    value: "WEBSITE",
    label: "Website",
  },
  {
    value: "EMAIL",
    label: "Email",
  },
  {
    value: "PHONE",
    label: "Phone",
  },
  {
    value: "GOOGLE",
    label: "Google",
  },
  {
    value: "FACEBOOK",
    label: "Facebook",
  },
  {
    value: "OTHER",
    label: "Other",
  },
];

type BooleanFilter = "" | "true" | "false";

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCustomerReviewsPage() {
  const [pageNumber, setPageNumber] = useState(0);

  const [pageSize, setPageSize] = useState<number>(10);

  const [searchInput, setSearchInput] = useState("");

  const [appliedKeyword, setAppliedKeyword] = useState("");

  const [moderationStatus, setModerationStatus] = useState<
    CustomerReviewModerationStatus | ""
  >("");

  const [reviewSource, setReviewSource] = useState<CustomerReviewSource | "">(
    "",
  );

  const [rating, setRating] = useState("");

  const [publicFilter, setPublicFilter] = useState<BooleanFilter>("");

  const [verifiedFilter, setVerifiedFilter] = useState<BooleanFilter>("");

  const [page, setPage] = useState<PageResponse<AdminCustomerReview> | null>(
    null,
  );

  const [isLoading, setIsLoading] = useState(true);

  const [reloadKey, setReloadKey] = useState(0);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // ===================================================================
  // LOAD REVIEWS
  // ===================================================================

  useEffect(() => {
    const controller = new AbortController();

    async function loadReviews() {
      setIsLoading(true);

      setErrorMessage(null);

      try {
        const response = await getAdminCustomerReviews({
          keyword: appliedKeyword || undefined,

          moderationStatus: moderationStatus || undefined,

          reviewSource: reviewSource || undefined,

          rating: rating ? Number(rating) : undefined,

          isPublic: parseBooleanFilter(publicFilter),

          isVerifiedCustomer: parseBooleanFilter(verifiedFilter),

          page: pageNumber,

          size: pageSize,

          signal: controller.signal,
        });

        setPage(response);
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Customer reviews could not be loaded.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadReviews();

    return () => {
      controller.abort();
    };
  }, [
    pageNumber,
    pageSize,
    appliedKeyword,
    moderationStatus,
    reviewSource,
    rating,
    publicFilter,
    verifiedFilter,
    reloadKey,
  ]);

  // ===================================================================
  // DERIVED VALUES
  // ===================================================================

  const reviews = page?.content ?? [];

  const totalElements = page?.totalElements ?? 0;

  const totalPages = page?.totalPages ?? 0;

  const currentPage = page ? page.number + 1 : pageNumber + 1;

  const pendingCount = reviews.filter(
    (review) => review.moderationStatus === "PENDING",
  ).length;

  const publicCount = reviews.filter(
    (review) => review.isPublic === true,
  ).length;

  // ===================================================================
  // SEARCH
  // ===================================================================

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setPageNumber(0);

    setAppliedKeyword(searchInput.trim());
  }

  function handleClearSearch() {
    setSearchInput("");

    setAppliedKeyword("");

    setPageNumber(0);
  }

  function handleClearFilters() {
    setSearchInput("");

    setAppliedKeyword("");

    setModerationStatus("");

    setReviewSource("");

    setRating("");

    setPublicFilter("");

    setVerifiedFilter("");

    setPageNumber(0);
  }

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="space-y-6">
      {/* =============================================================
       * HEADER
       * ============================================================= */}

      <header className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        <div>
          <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer management
          </p>

          <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
            Customer reviews
          </h1>

          <p className="mt-2 max-w-3xl leading-7 text-slate-600">
            Review customer feedback, manage moderation decisions, record
            customer-authorized feedback, and control what appears on the public
            Romelt TechCare website.
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row">
          {/* =========================================================
           * CREATE REVIEW
           * ========================================================= */}

          <Link
            to="/admin/customers/reviews/new"
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 py-2 font-bold text-white transition hover:bg-brand-800"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            Create review
          </Link>

          {/* =========================================================
           * CUSTOMERS
           * ========================================================= */}

          <Link
            to="/admin/customers"
            className="focus-ring inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-300 bg-white px-4 py-2 font-bold text-slate-700 transition hover:bg-slate-50"
          >
            Customers
          </Link>

          {/* =========================================================
           * REFRESH
           * ========================================================= */}

          <button
            type="button"
            onClick={() => setReloadKey((current) => current + 1)}
            disabled={isLoading}
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-navy-950 px-4 py-2 font-bold text-white transition hover:bg-navy-900 disabled:opacity-60"
          >
            <RefreshCw
              className={["h-4 w-4", isLoading ? "animate-spin" : ""].join(" ")}
              aria-hidden="true"
            />
            Refresh
          </button>
        </div>
      </header>

      {/* =============================================================
       * SUMMARY
       * ============================================================= */}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          icon={<MessageSquareQuote />}
          label="Matching reviews"
          value={totalElements.toLocaleString("en-US")}
        />

        <SummaryCard
          icon={<ShieldCheck />}
          label="Pending on page"
          value={pendingCount.toLocaleString("en-US")}
        />

        <SummaryCard
          icon={<Globe2 />}
          label="Public on page"
          value={publicCount.toLocaleString("en-US")}
        />

        <SummaryCard
          icon={<BadgeCheck />}
          label="Verification"
          value={
            verifiedFilter === ""
              ? "All reviews"
              : verifiedFilter === "true"
                ? "Verified only"
                : "Unverified only"
          }
        />
      </div>

      {/* =============================================================
       * FILTERS
       * ============================================================= */}

      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-3 xl:grid-cols-[minmax(260px,1fr)_190px_180px_130px_170px_170px_auto]">
          <form onSubmit={handleSearchSubmit} className="relative">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />

            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Search customer, email, title, review, booking..."
              className="focus-ring min-h-11 w-full rounded-xl border border-slate-300 bg-white py-2 pl-10 pr-10 text-sm text-slate-900 placeholder:text-slate-400"
            />

            {searchInput ? (
              <button
                type="button"
                onClick={handleClearSearch}
                className="focus-ring absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                aria-label="Clear search"
              >
                <X className="h-4 w-4" />
              </button>
            ) : null}
          </form>

          <select
            value={moderationStatus}
            onChange={(event) => {
              setModerationStatus(
                event.target.value as CustomerReviewModerationStatus | "",
              );

              setPageNumber(0);
            }}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            {MODERATION_STATUS_OPTIONS.map((option) => (
              <option key={option.value || "ALL"} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            value={reviewSource}
            onChange={(event) => {
              setReviewSource(event.target.value as CustomerReviewSource | "");

              setPageNumber(0);
            }}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            {SOURCE_OPTIONS.map((option) => (
              <option key={option.value || "ALL"} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            value={rating}
            onChange={(event) => {
              setRating(event.target.value);

              setPageNumber(0);
            }}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            <option value="">All ratings</option>

            <option value="5">5 stars</option>

            <option value="4">4 stars</option>

            <option value="3">3 stars</option>

            <option value="2">2 stars</option>

            <option value="1">1 star</option>
          </select>

          <select
            value={publicFilter}
            onChange={(event) => {
              setPublicFilter(event.target.value as BooleanFilter);

              setPageNumber(0);
            }}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            <option value="">Public + private</option>

            <option value="true">Public only</option>

            <option value="false">Not public</option>
          </select>

          <select
            value={verifiedFilter}
            onChange={(event) => {
              setVerifiedFilter(event.target.value as BooleanFilter);

              setPageNumber(0);
            }}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            <option value="">Verified + unverified</option>

            <option value="true">Verified only</option>

            <option value="false">Unverified only</option>
          </select>

          <button
            type="button"
            onClick={handleClearFilters}
            className="focus-ring min-h-11 rounded-xl border border-slate-300 bg-slate-50 px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-100"
          >
            Clear
          </button>
        </div>
      </div>

      {/* =============================================================
       * ERROR
       * ============================================================= */}

      {errorMessage ? (
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-4"
        >
          <div className="flex items-start gap-3">
            <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-700" />

            <div>
              <p className="font-extrabold text-red-900">
                Reviews could not be loaded
              </p>

              <p className="mt-1 text-sm leading-6 text-red-700">
                {errorMessage}
              </p>
            </div>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * REVIEW LIST
       * ============================================================= */}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isLoading ? (
          <div className="flex min-h-72 items-center justify-center">
            <div className="text-center">
              <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-brand-700" />

              <p className="mt-3 text-sm font-semibold text-slate-600">
                Loading customer reviews…
              </p>
            </div>
          </div>
        ) : reviews.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <MessageSquareQuote className="mx-auto h-10 w-10 text-slate-300" />

            <h2 className="mt-4 text-lg font-black text-navy-950">
              No reviews found
            </h2>

            <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-500">
              No customer reviews match the selected search and filters.
            </p>

            <Link
              to="/admin/customers/reviews/new"
              className="focus-ring mt-5 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-brand-800"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
              Create first review
            </Link>
          </div>
        ) : (
          <>
            {/* =====================================================
             * DESKTOP TABLE
             * ===================================================== */}

            <div className="hidden overflow-x-auto lg:block">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <TableHeading>Customer / review</TableHeading>

                    <TableHeading>Rating</TableHeading>

                    <TableHeading>Source</TableHeading>

                    <TableHeading>Moderation</TableHeading>

                    <TableHeading>Publication</TableHeading>

                    <TableHeading>Submitted</TableHeading>

                    <TableHeading>Action</TableHeading>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {reviews.map((review) => (
                    <tr
                      key={review.customerReviewId}
                      className="transition hover:bg-slate-50"
                    >
                      <td className="max-w-md px-4 py-4">
                        <p className="font-extrabold text-navy-950">
                          {resolveReviewerName(review)}
                        </p>

                        <p className="mt-1 truncate text-sm font-semibold text-slate-700">
                          {review.reviewTitle || "Untitled review"}
                        </p>

                        <p className="mt-1 line-clamp-2 text-sm leading-6 text-slate-500">
                          {review.reviewText || "No review text."}
                        </p>

                        {review.bookingReferenceNumber ? (
                          <p className="mt-2 text-xs font-bold text-brand-700">
                            {review.bookingReferenceNumber}
                          </p>
                        ) : null}
                      </td>

                      <td className="px-4 py-4">
                        <RatingStars rating={review.rating} />
                      </td>

                      <td className="px-4 py-4">
                        <p className="text-sm font-semibold text-slate-700">
                          {formatEnumLabel(review.reviewSource)}
                        </p>

                        {review.isVerifiedCustomer ? (
                          <span className="mt-2 inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-extrabold text-emerald-800">
                            <ShieldCheck className="h-3.5 w-3.5" />
                            Verified
                          </span>
                        ) : null}
                      </td>

                      <td className="px-4 py-4">
                        <StatusBadge status={review.moderationStatus} />
                      </td>

                      <td className="px-4 py-4">
                        <div className="flex flex-wrap gap-1.5">
                          {review.isPublic ? (
                            <Badge label="Public" variant="blue" />
                          ) : (
                            <Badge label="Private" variant="slate" />
                          )}

                          {review.isFeatured ? (
                            <Badge label="Featured" variant="gold" />
                          ) : null}

                          {review.isSpam ? (
                            <Badge label="Spam" variant="red" />
                          ) : null}
                        </div>
                      </td>

                      <td className="whitespace-nowrap px-4 py-4 text-sm text-slate-600">
                        {formatDateTime(review.submittedAt)}
                      </td>

                      <td className="px-4 py-4">
                        <Link
                          to={`/admin/customers/reviews/${review.customerReviewId}`}
                          className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 transition hover:border-brand-300 hover:text-brand-700"
                        >
                          <Eye className="h-4 w-4" />
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* =====================================================
             * MOBILE CARDS
             * ===================================================== */}

            <div className="divide-y divide-slate-100 lg:hidden">
              {reviews.map((review) => (
                <article key={review.customerReviewId} className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-extrabold text-navy-950">
                        {resolveReviewerName(review)}
                      </p>

                      <p className="mt-1 truncate text-sm font-semibold text-slate-700">
                        {review.reviewTitle || "Untitled review"}
                      </p>
                    </div>

                    <StatusBadge status={review.moderationStatus} />
                  </div>

                  <div className="mt-3">
                    <RatingStars rating={review.rating} />
                  </div>

                  <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-600">
                    {review.reviewText || "No review text."}
                  </p>

                  <div className="mt-4 flex flex-wrap gap-2">
                    {review.isVerifiedCustomer ? (
                      <Badge label="Verified" variant="green" />
                    ) : null}

                    {review.isPublic ? (
                      <Badge label="Public" variant="blue" />
                    ) : null}

                    {review.isFeatured ? (
                      <Badge label="Featured" variant="gold" />
                    ) : null}

                    {review.isSpam ? (
                      <Badge label="Spam" variant="red" />
                    ) : null}
                  </div>

                  <Link
                    to={`/admin/customers/reviews/${review.customerReviewId}`}
                    className="focus-ring mt-4 inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-xl bg-navy-950 px-3 text-sm font-bold text-white transition hover:bg-navy-900"
                  >
                    <Eye className="h-4 w-4" />
                    Review details
                  </Link>
                </article>
              ))}
            </div>
          </>
        )}
      </div>

      {/* =============================================================
       * PAGINATION
       * ============================================================= */}

      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <p className="text-sm font-semibold text-slate-600">
            Page {Math.min(currentPage, Math.max(totalPages, 1))} of{" "}
            {Math.max(totalPages, 1)}
          </p>

          <select
            value={pageSize}
            onChange={(event) => {
              const parsed = Number(event.target.value);

              if (
                PAGE_SIZE_OPTIONS.includes(
                  parsed as (typeof PAGE_SIZE_OPTIONS)[number],
                )
              ) {
                setPageSize(parsed);

                setPageNumber(0);
              }
            }}
            className="focus-ring min-h-10 rounded-lg border border-slate-300 bg-white px-2 text-sm font-semibold text-slate-700"
            aria-label="Reviews per page"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {size} / page
              </option>
            ))}
          </select>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            disabled={!page || page.first}
            onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-1 rounded-lg border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
            Previous
          </button>

          <button
            type="button"
            disabled={!page || page.last}
            onClick={() => setPageNumber((current) => current + 1)}
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-1 rounded-lg border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Next
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// TABLE
// =====================================================================

function TableHeading({ children }: { children: ReactNode }) {
  return (
    <th className="px-4 py-3 text-left text-xs font-extrabold uppercase tracking-wide text-slate-500">
      {children}
    </th>
  );
}

// =====================================================================
// SUMMARY CARD
// =====================================================================

function SummaryCard({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <div className="min-w-0">
          <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-1 truncate font-black text-navy-950">{value}</p>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// RATING
// =====================================================================

function RatingStars({ rating }: { rating: number }) {
  const normalizedRating = Math.min(Math.max(Number(rating) || 0, 0), 5);

  return (
    <div
      className="flex items-center gap-0.5"
      aria-label={`${normalizedRating} out of 5 stars`}
    >
      {Array.from(
        {
          length: 5,
        },
        (_, index) => (
          <Star
            key={index}
            className={[
              "h-4 w-4",
              index < normalizedRating
                ? "fill-amber-400 text-amber-400"
                : "text-slate-300",
            ].join(" ")}
          />
        ),
      )}

      <span className="ml-1 text-xs font-bold text-slate-600">
        {normalizedRating}/5
      </span>
    </div>
  );
}

// =====================================================================
// STATUS
// =====================================================================

function StatusBadge({ status }: { status: CustomerReviewModerationStatus }) {
  return (
    <span
      className={[
        "inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-extrabold",
        resolveModerationStatusClass(status),
      ].join(" ")}
    >
      {formatEnumLabel(status)}
    </span>
  );
}

// =====================================================================
// BADGE
// =====================================================================

function Badge({
  label,
  variant,
}: {
  label: string;

  variant: "blue" | "slate" | "gold" | "green" | "red";
}) {
  const variants = {
    blue: "bg-blue-100 text-blue-800",

    slate: "bg-slate-100 text-slate-700",

    gold: "bg-amber-100 text-amber-800",

    green: "bg-emerald-100 text-emerald-800",

    red: "bg-red-100 text-red-800",
  };

  return (
    <span
      className={[
        "inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-extrabold",
        variants[variant],
      ].join(" ")}
    >
      {variant === "gold" ? <Sparkles className="h-3.5 w-3.5" /> : null}

      {label}
    </span>
  );
}

// =====================================================================
// HELPERS
// =====================================================================

function resolveReviewerName(review: AdminCustomerReview): string {
  return (
    review.resolvedPublicDisplayName ||
    review.reviewerDisplayName ||
    "Anonymous customer"
  );
}

function resolveModerationStatusClass(
  status: CustomerReviewModerationStatus,
): string {
  switch (status) {
    case "PENDING":
      return "bg-amber-100 text-amber-800";

    case "APPROVED":
      return "bg-emerald-100 text-emerald-800";

    case "REJECTED":
      return "bg-red-100 text-red-800";

    case "SPAM":
      return "bg-rose-100 text-rose-800";

    case "HIDDEN":
      return "bg-slate-200 text-slate-800";

    case "ARCHIVED":
      return "bg-violet-100 text-violet-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

function parseBooleanFilter(value: BooleanFilter): boolean | undefined {
  if (value === "true") {
    return true;
  }

  if (value === "false") {
    return false;
  }

  return undefined;
}

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}
