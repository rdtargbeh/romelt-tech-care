/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEW DETAIL PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the administrator moderation workspace for one customer
 * review.
 *
 * Responsibilities:
 * - Loads the complete customer review.
 * - Displays reviewer, rating, review text, booking relationship,
 *   verification, consent, moderation, publication, and timeline data.
 * - Approves or rejects a review.
 * - Marks inappropriate reviews as spam.
 * - Publishes and unpublishes approved reviews.
 * - Manages featured-review status.
 * - Hides or archives a review.
 * - Adds or removes the Romelt TechCare administrator response.
 *
 * Route:
 * /admin/customers/reviews/:customerReviewId
 *
 * Real-data integration:
 * GET    /api/v1/admin/customer-reviews/{customerReviewId}
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/approve
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/reject
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/spam
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/publish
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/unpublish
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/featured
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/hide
 * PATCH  /api/v1/admin/customer-reviews/{customerReviewId}/archive
 * PUT    /api/v1/admin/customer-reviews/{customerReviewId}/response
 * DELETE /api/v1/admin/customer-reviews/{customerReviewId}/response
 * ================================================================
 */

import {
  Archive,
  ArrowLeft,
  BadgeCheck,
  Ban,
  CheckCircle2,
  CircleAlert,
  EyeOff,
  Globe2,
  LoaderCircle,
  MessageSquareQuote,
  RefreshCw,
  Reply,
  ShieldCheck,
  Sparkles,
  Star,
  Trash2,
  XCircle,
} from "lucide-react";

import { type ReactNode, useCallback, useEffect, useState } from "react";

import { Link, useParams } from "react-router-dom";

import {
  approveAdminCustomerReview,
  archiveAdminCustomerReview,
  getAdminCustomerReview,
  hideAdminCustomerReview,
  markAdminCustomerReviewAsSpam,
  publishAdminCustomerReview,
  rejectAdminCustomerReview,
  removeAdminCustomerReviewResponse,
  saveAdminCustomerReviewResponse,
  unpublishAdminCustomerReview,
  updateAdminCustomerReviewFeaturedStatus,
} from "@/services/admin-customer-review.service";

import type {
  AdminCustomerReview,
  CustomerReviewModerationStatus,
} from "@/types/admin-customer-review.types";

// =====================================================================
// ACTION TYPES
// =====================================================================

type ActionName =
  | "approve"
  | "reject"
  | "spam"
  | "publish"
  | "unpublish"
  | "feature"
  | "unfeature"
  | "hide"
  | "archive"
  | "save-response"
  | "remove-response";

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCustomerReviewDetailsPage() {
  const { customerReviewId = "" } = useParams();

  const [review, setReview] = useState<AdminCustomerReview | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [actionName, setActionName] = useState<ActionName | null>(null);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [moderationNotes, setModerationNotes] = useState("");

  const [rejectionReason, setRejectionReason] = useState("");

  const [spamScore, setSpamScore] = useState("");

  const [adminResponse, setAdminResponse] = useState("");

  // ===================================================================
  // LOAD REVIEW
  // ===================================================================

  const loadReview = useCallback(
    async (signal?: AbortSignal) => {
      if (!customerReviewId.trim()) {
        setErrorMessage("Customer review ID is required.");

        setIsLoading(false);

        return;
      }

      setIsLoading(true);

      setErrorMessage(null);

      try {
        const response = await getAdminCustomerReview(customerReviewId, signal);

        applyReviewState(response);

        setReview(response);
      } catch (error) {
        if (!signal?.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Customer review could not be loaded.",
          );
        }
      } finally {
        if (!signal?.aborted) {
          setIsLoading(false);
        }
      }
    },
    [customerReviewId],
  );

  useEffect(() => {
    const controller = new AbortController();

    void loadReview(controller.signal);

    return () => {
      controller.abort();
    };
  }, [loadReview]);

  // ===================================================================
  // REVIEW STATE
  // ===================================================================

  function applyReviewState(updatedReview: AdminCustomerReview) {
    setModerationNotes(updatedReview.moderationNotes ?? "");

    setRejectionReason(updatedReview.rejectionReason ?? "");

    setSpamScore(
      updatedReview.spamScore == null ? "" : String(updatedReview.spamScore),
    );

    setAdminResponse(updatedReview.adminResponse ?? "");
  }

  // ===================================================================
  // ACTION EXECUTION
  // ===================================================================

  async function runAction(
    name: ActionName,

    action: () => Promise<AdminCustomerReview>,

    message: string,
  ) {
    if (actionName) {
      return;
    }

    setActionName(name);

    setErrorMessage(null);

    setSuccessMessage(null);

    try {
      const updated = await action();

      setReview(updated);

      applyReviewState(updated);

      setSuccessMessage(message);

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The review action could not be completed.",
      );

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } finally {
      setActionName(null);
    }
  }

  // ===================================================================
  // LOADING
  // ===================================================================

  if (isLoading) {
    return (
      <section className="flex min-h-96 items-center justify-center">
        <div className="text-center">
          <LoaderCircle className="mx-auto h-9 w-9 animate-spin text-brand-700" />

          <p className="mt-3 font-semibold text-slate-600">
            Loading customer review…
          </p>
        </div>
      </section>
    );
  }

  // ===================================================================
  // NOT FOUND
  // ===================================================================

  if (!review) {
    return (
      <section className="space-y-5">
        <Link
          to="/admin/customers/reviews"
          className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to customer reviews
        </Link>

        <div className="rounded-2xl border border-red-200 bg-red-50 p-5">
          <div className="flex gap-3">
            <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-700" />

            <div>
              <h1 className="font-black text-red-950">
                Review could not be opened
              </h1>

              <p className="mt-1 text-sm leading-6 text-red-700">
                {errorMessage || "The selected customer review was not found."}
              </p>
            </div>
          </div>
        </div>
      </section>
    );
  }

  // ===================================================================
  // PERMITTED ACTIONS
  // ===================================================================

  const canApprove =
    review.moderationStatus === "PENDING" ||
    review.moderationStatus === "REJECTED";

  const canPublish =
    review.moderationStatus === "APPROVED" &&
    review.customerConsentConfirmed === true &&
    review.isSpam !== true &&
    review.isPublic !== true;

  const canFeature = review.isPublic === true && review.isFeatured !== true;

  const canUnfeature = review.isPublic === true && review.isFeatured === true;

  const isArchived = review.moderationStatus === "ARCHIVED";

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="space-y-6">
      {/* =============================================================
       * BACK
       * ============================================================= */}

      <Link
        to="/admin/customers/reviews"
        className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to customer reviews
      </Link>

      {/* =============================================================
       * HEADER
       * ============================================================= */}

      <header className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0">
          <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer review
          </p>

          <h1 className="mt-2 break-words font-display text-3xl font-black text-navy-950">
            {review.reviewTitle || "Customer review"}
          </h1>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <StatusBadge status={review.moderationStatus} />

            {review.isVerifiedCustomer ? (
              <Pill icon={<ShieldCheck />} label="Verified customer" />
            ) : null}

            {review.isPublic ? <Pill icon={<Globe2 />} label="Public" /> : null}

            {review.isFeatured ? (
              <Pill icon={<Sparkles />} label="Featured" />
            ) : null}

            {review.isSpam ? <Pill icon={<Ban />} label="Spam" /> : null}
          </div>
        </div>

        <button
          type="button"
          onClick={() => void loadReview()}
          disabled={Boolean(actionName)}
          className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh
        </button>
      </header>

      {/* =============================================================
       * SUCCESS
       * ============================================================= */}

      {successMessage ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

            <p className="text-sm font-bold leading-6 text-emerald-800">
              {successMessage}
            </p>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * ERROR
       * ============================================================= */}

      {errorMessage ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
          <div className="flex items-start gap-3">
            <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-700" />

            <p className="text-sm font-bold leading-6 text-red-800">
              {errorMessage}
            </p>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * CONTENT
       * ============================================================= */}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.8fr)]">
        {/* ===========================================================
         * LEFT COLUMN
         * =========================================================== */}

        <div className="space-y-5">
          {/* =========================================================
           * CUSTOMER REVIEW
           * ========================================================= */}

          <Card title="Customer review" icon={<MessageSquareQuote />}>
            <div className="space-y-5">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
                  Reviewer
                </p>

                <p className="mt-1 text-lg font-black text-navy-950">
                  {resolveReviewerName(review)}
                </p>
              </div>

              <RatingStars rating={review.rating} />

              <div>
                <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
                  Review title
                </p>

                <p className="mt-1 font-bold text-slate-800">
                  {review.reviewTitle || "Untitled review"}
                </p>
              </div>

              <div>
                <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
                  Review
                </p>

                <p className="mt-2 whitespace-pre-wrap leading-7 text-slate-700">
                  {review.reviewText || "No review text was provided."}
                </p>
              </div>
            </div>
          </Card>

          {/* =========================================================
           * MODERATION
           * ========================================================= */}

          <Card title="Moderation" icon={<BadgeCheck />}>
            <div className="space-y-5">
              <div>
                <label
                  htmlFor="moderationNotes"
                  className="text-sm font-extrabold text-navy-950"
                >
                  Moderation notes
                </label>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Internal administrator notes. These are not displayed
                  publicly.
                </p>

                <textarea
                  id="moderationNotes"
                  value={moderationNotes}
                  onChange={(event) => setModerationNotes(event.target.value)}
                  rows={4}
                  maxLength={5000}
                  className="focus-ring mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900"
                  placeholder="Optional moderation notes"
                />
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <ActionButton
                  label="Approve review"
                  icon={<CheckCircle2 />}
                  busy={actionName === "approve"}
                  disabled={!canApprove || Boolean(actionName)}
                  className="bg-emerald-700 text-white hover:bg-emerald-800"
                  onClick={() =>
                    void runAction(
                      "approve",

                      () =>
                        approveAdminCustomerReview(review.customerReviewId, {
                          moderationNotes: normalizeOptional(moderationNotes),
                        }),

                      "Customer review approved.",
                    )
                  }
                />

                <ActionButton
                  label="Hide review"
                  icon={<EyeOff />}
                  busy={actionName === "hide"}
                  disabled={isArchived || Boolean(actionName)}
                  className="border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  onClick={() =>
                    void runAction(
                      "hide",

                      () =>
                        hideAdminCustomerReview(review.customerReviewId, {
                          moderationNotes: normalizeOptional(moderationNotes),
                        }),

                      "Customer review hidden.",
                    )
                  }
                />
              </div>

              {/* =====================================================
               * REJECTION
               * ===================================================== */}

              <div className="border-t border-slate-200 pt-5">
                <label
                  htmlFor="rejectionReason"
                  className="text-sm font-extrabold text-navy-950"
                >
                  Rejection reason
                </label>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Required when rejecting the customer review.
                </p>

                <textarea
                  id="rejectionReason"
                  value={rejectionReason}
                  onChange={(event) => setRejectionReason(event.target.value)}
                  rows={3}
                  maxLength={500}
                  className="focus-ring mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900"
                  placeholder="Explain why this review is being rejected"
                />

                <ActionButton
                  label="Reject review"
                  icon={<XCircle />}
                  busy={actionName === "reject"}
                  disabled={
                    !rejectionReason.trim() || Boolean(actionName) || isArchived
                  }
                  className="mt-3 bg-red-700 text-white hover:bg-red-800"
                  onClick={() =>
                    void runAction(
                      "reject",

                      () =>
                        rejectAdminCustomerReview(review.customerReviewId, {
                          rejectionReason: rejectionReason.trim(),

                          moderationNotes: normalizeOptional(moderationNotes),
                        }),

                      "Customer review rejected.",
                    )
                  }
                />
              </div>

              {/* =====================================================
               * SPAM
               * ===================================================== */}

              <div className="border-t border-slate-200 pt-5">
                <label
                  htmlFor="spamScore"
                  className="text-sm font-extrabold text-navy-950"
                >
                  Spam score
                </label>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Optional administrator score from 0 to 100.
                </p>

                <input
                  id="spamScore"
                  type="number"
                  min={0}
                  max={100}
                  step={1}
                  value={spamScore}
                  onChange={(event) => setSpamScore(event.target.value)}
                  className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 px-3 text-sm text-slate-900"
                  placeholder="0 - 100"
                />

                <ActionButton
                  label="Mark as spam"
                  icon={<Ban />}
                  busy={actionName === "spam"}
                  disabled={Boolean(actionName) || isArchived}
                  className="mt-3 border border-red-200 bg-red-50 text-red-800 hover:bg-red-100"
                  onClick={() =>
                    void runAction(
                      "spam",

                      () =>
                        markAdminCustomerReviewAsSpam(review.customerReviewId, {
                          spamScore: parseSpamScore(spamScore),

                          moderationNotes: normalizeOptional(moderationNotes),
                        }),

                      "Customer review marked as spam.",
                    )
                  }
                />
              </div>
            </div>
          </Card>

          {/* =========================================================
           * ADMIN RESPONSE
           * ========================================================= */}

          <Card title="Administrator response" icon={<Reply />}>
            <p className="mb-3 text-sm leading-6 text-slate-600">
              This response may appear publicly with the review when the review
              is published.
            </p>

            <textarea
              value={adminResponse}
              onChange={(event) => setAdminResponse(event.target.value)}
              rows={5}
              maxLength={10000}
              className="focus-ring w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900"
              placeholder="Write Romelt TechCare's response to the customer..."
            />

            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <ActionButton
                label="Save response"
                icon={<Reply />}
                busy={actionName === "save-response"}
                disabled={!adminResponse.trim() || Boolean(actionName)}
                className="bg-navy-950 text-white hover:bg-navy-900"
                onClick={() =>
                  void runAction(
                    "save-response",

                    () =>
                      saveAdminCustomerReviewResponse(review.customerReviewId, {
                        adminResponse: adminResponse.trim(),
                      }),

                    "Administrator response saved.",
                  )
                }
              />

              <ActionButton
                label="Remove response"
                icon={<Trash2 />}
                busy={actionName === "remove-response"}
                disabled={!review.adminResponse || Boolean(actionName)}
                className="border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                onClick={() =>
                  void runAction(
                    "remove-response",

                    () =>
                      removeAdminCustomerReviewResponse(
                        review.customerReviewId,
                      ),

                    "Administrator response removed.",
                  )
                }
              />
            </div>
          </Card>
        </div>

        {/* ===========================================================
         * RIGHT COLUMN
         * =========================================================== */}

        <div className="space-y-5">
          {/* =========================================================
           * PUBLICATION
           * ========================================================= */}

          <Card title="Publication" icon={<Globe2 />}>
            <dl className="space-y-3">
              <KeyValue
                label="Moderation"
                value={formatEnumLabel(review.moderationStatus)}
              />

              <KeyValue label="Public" value={review.isPublic ? "Yes" : "No"} />

              <KeyValue
                label="Featured"
                value={review.isFeatured ? "Yes" : "No"}
              />

              <KeyValue
                label="Consent"
                value={
                  review.customerConsentConfirmed ? "Confirmed" : "Missing"
                }
              />

              <KeyValue label="Spam" value={review.isSpam ? "Yes" : "No"} />
            </dl>

            <div className="mt-5 space-y-3">
              {canPublish ? (
                <ActionButton
                  label="Publish review"
                  icon={<Globe2 />}
                  busy={actionName === "publish"}
                  disabled={Boolean(actionName)}
                  className="w-full bg-brand-700 text-white hover:bg-brand-800"
                  onClick={() =>
                    void runAction(
                      "publish",

                      () =>
                        publishAdminCustomerReview(review.customerReviewId, {
                          isFeatured: false,
                        }),

                      "Customer review published.",
                    )
                  }
                />
              ) : null}

              {review.isPublic ? (
                <ActionButton
                  label="Unpublish review"
                  icon={<EyeOff />}
                  busy={actionName === "unpublish"}
                  disabled={Boolean(actionName)}
                  className="w-full border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  onClick={() =>
                    void runAction(
                      "unpublish",

                      () =>
                        unpublishAdminCustomerReview(review.customerReviewId),

                      "Customer review removed from public display.",
                    )
                  }
                />
              ) : null}

              {canFeature ? (
                <ActionButton
                  label="Feature review"
                  icon={<Sparkles />}
                  busy={actionName === "feature"}
                  disabled={Boolean(actionName)}
                  className="w-full bg-amber-400 text-amber-950 hover:bg-amber-300"
                  onClick={() =>
                    void runAction(
                      "feature",

                      () =>
                        updateAdminCustomerReviewFeaturedStatus(
                          review.customerReviewId,
                          true,
                        ),

                      "Customer review featured.",
                    )
                  }
                />
              ) : null}

              {canUnfeature ? (
                <ActionButton
                  label="Remove featured status"
                  icon={<Sparkles />}
                  busy={actionName === "unfeature"}
                  disabled={Boolean(actionName)}
                  className="w-full border border-amber-300 bg-amber-50 text-amber-900 hover:bg-amber-100"
                  onClick={() =>
                    void runAction(
                      "unfeature",

                      () =>
                        updateAdminCustomerReviewFeaturedStatus(
                          review.customerReviewId,
                          false,
                        ),

                      "Featured status removed.",
                    )
                  }
                />
              ) : null}

              {!canPublish && !review.isPublic ? (
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs leading-5 text-slate-600">
                  A review must be approved, have customer consent, and not be
                  marked as spam before publication.
                </div>
              ) : null}
            </div>
          </Card>

          {/* =========================================================
           * RELATED RECORDS
           * ========================================================= */}

          <Card title="Related records" icon={<ShieldCheck />}>
            <dl className="space-y-3">
              <KeyValue
                label="Booking"
                value={review.bookingReferenceNumber || "—"}
              />

              <KeyValue
                label="Booking ID"
                value={review.bookingRequestId || "—"}
                mono
              />

              <KeyValue
                label="Invitation ID"
                value={review.reviewInvitationId || "—"}
                mono
              />

              <KeyValue
                label="Service"
                value={review.serviceCode || review.serviceSlug || "—"}
              />

              <KeyValue
                label="Source"
                value={formatEnumLabel(review.reviewSource)}
              />
            </dl>
          </Card>

          {/* =========================================================
           * CUSTOMER DETAILS
           * ========================================================= */}

          <Card title="Customer details" icon={<BadgeCheck />}>
            <dl className="space-y-3">
              <KeyValue label="Reviewer" value={resolveReviewerName(review)} />

              <KeyValue
                label="Display"
                value={formatEnumLabel(review.reviewerDisplayPreference)}
              />

              <KeyValue label="Email" value={review.reviewerEmail || "—"} />

              <KeyValue label="Phone" value={review.reviewerPhone || "—"} />

              <KeyValue
                label="Verified"
                value={review.isVerifiedCustomer ? "Yes" : "No"}
              />

              <KeyValue
                label="Consent"
                value={
                  review.customerConsentConfirmed
                    ? "Confirmed"
                    : "Not confirmed"
                }
              />
            </dl>
          </Card>

          {/* =========================================================
           * TIMELINE
           * ========================================================= */}

          <Card title="Timeline" icon={<MessageSquareQuote />}>
            <dl className="space-y-3">
              <KeyValue
                label="Submitted"
                value={formatDateTime(review.submittedAt)}
              />

              <KeyValue
                label="Moderated"
                value={formatDateTime(review.moderatedAt)}
              />

              <KeyValue
                label="Published"
                value={formatDateTime(review.publishedAt)}
              />

              <KeyValue
                label="Responded"
                value={formatDateTime(review.respondedAt)}
              />

              <KeyValue
                label="Updated"
                value={formatDateTime(review.updatedAt)}
              />
            </dl>
          </Card>

          {/* =========================================================
           * RECORD MANAGEMENT
           * ========================================================= */}

          <Card title="Record management" icon={<Archive />}>
            <p className="mb-4 text-sm leading-6 text-slate-600">
              Archive reviews that should remain available for historical
              records but no longer participate in the normal moderation
              workflow.
            </p>

            <ActionButton
              label={isArchived ? "Review archived" : "Archive review"}
              icon={<Archive />}
              busy={actionName === "archive"}
              disabled={isArchived || Boolean(actionName)}
              className="w-full border border-violet-200 bg-violet-50 text-violet-800 hover:bg-violet-100"
              onClick={() =>
                void runAction(
                  "archive",

                  () => archiveAdminCustomerReview(review.customerReviewId),

                  "Customer review archived.",
                )
              }
            />
          </Card>
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// CARD
// =====================================================================

function Card({
  title,
  icon,
  children,
}: {
  title: string;

  icon: ReactNode;

  children: ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <h2 className="font-black text-navy-950">{title}</h2>
      </div>

      <div className="p-5">{children}</div>
    </section>
  );
}

// =====================================================================
// ACTION BUTTON
// =====================================================================

function ActionButton({
  label,
  icon,
  busy,
  disabled,
  className,
  onClick,
}: {
  label: string;

  icon: ReactNode;

  busy: boolean;

  disabled: boolean;

  className: string;

  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={[
        "focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-extrabold transition disabled:cursor-not-allowed disabled:opacity-50",
        className,
      ].join(" ")}
    >
      {busy ? (
        <LoaderCircle className="h-4 w-4 animate-spin" />
      ) : (
        <span className="[&>svg]:h-4 [&>svg]:w-4">{icon}</span>
      )}

      {label}
    </button>
  );
}

// =====================================================================
// RATING
// =====================================================================

function RatingStars({ rating }: { rating: number }) {
  const normalizedRating = Math.min(Math.max(Number(rating) || 0, 0), 5);

  return (
    <div
      className="flex items-center gap-1"
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
              "h-5 w-5",
              index < normalizedRating
                ? "fill-amber-400 text-amber-400"
                : "text-slate-300",
            ].join(" ")}
          />
        ),
      )}

      <span className="ml-2 text-sm font-black text-slate-700">
        {normalizedRating}/5
      </span>
    </div>
  );
}

// =====================================================================
// STATUS BADGE
// =====================================================================

function StatusBadge({ status }: { status: CustomerReviewModerationStatus }) {
  return (
    <span
      className={[
        "inline-flex rounded-full px-3 py-1 text-xs font-extrabold",
        resolveStatusClass(status),
      ].join(" ")}
    >
      {formatEnumLabel(status)}
    </span>
  );
}

// =====================================================================
// PILL
// =====================================================================

function Pill({
  icon,
  label,
}: {
  icon: ReactNode;

  label: string;
}) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-extrabold text-slate-700 [&>svg]:h-3.5 [&>svg]:w-3.5">
      {icon}

      {label}
    </span>
  );
}

// =====================================================================
// KEY / VALUE
// =====================================================================

function KeyValue({
  label,
  value,
  mono = false,
}: {
  label: string;

  value: string;

  mono?: boolean;
}) {
  return (
    <div className="grid gap-1 sm:grid-cols-[120px_minmax(0,1fr)] sm:gap-3">
      <dt className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
        {label}
      </dt>

      <dd
        className={[
          "break-words text-sm font-semibold text-slate-700",
          mono ? "font-mono text-xs" : "",
        ].join(" ")}
      >
        {value}
      </dd>
    </div>
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

function resolveStatusClass(status: CustomerReviewModerationStatus): string {
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

function normalizeOptional(value: string): string | null {
  const normalized = value.trim();

  return normalized || null;
}

function parseSpamScore(value: string): number | null {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);

  if (!Number.isFinite(parsed)) {
    return null;
  }

  return Math.min(100, Math.max(0, parsed));
}
