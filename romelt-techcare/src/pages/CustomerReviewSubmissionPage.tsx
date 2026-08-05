/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW SUBMISSION PAGE
 * ================================================================
 *
 * Purpose:
 * Allows a verified customer to submit one review using a secure
 * invitation token.
 *
 * Responsibilities:
 * - Reads the invitation token from the URL.
 * - Collects rating, title, review text, display preference, and
 *   publication consent.
 * - Validates the form before submission.
 * - Submits the review to the real backend.
 * - Handles invalid, expired, used, or revoked token failures.
 * - Prevents duplicate submissions.
 * - Cancels requests when the page unmounts.
 *
 * Route:
 * /reviews/submit?token=SECURE_TOKEN
 *
 * Real-data integration:
 * POST /api/v1/public/customer-reviews
 * ================================================================
 */

import {
  type ChangeEvent,
  type FormEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  CheckCircle2,
  CircleAlert,
  LoaderCircle,
  MessageSquareQuote,
  Send,
  ShieldCheck,
} from "lucide-react";
import { Link, useSearchParams } from "react-router";

import { StarRating } from "@/components/reviews/StarRating";
import { submitPublicCustomerReview } from "@/services/customer-review.service";
import type {
  CustomerReviewDisplayPreference,
  CustomerReviewPublicSubmissionRequest,
  PublicCustomerReview,
} from "@/types/customer-review.types";

interface ReviewFormState {
  reviewerDisplayPreference: CustomerReviewDisplayPreference;
  reviewerDisplayName: string;
  reviewTitle: string;
  reviewText: string;
  rating: number;
  customerConsentConfirmed: boolean;
}

type ReviewFieldName = keyof ReviewFormState;

type ReviewFormErrors = Partial<Record<ReviewFieldName | "token", string>>;

const CONSENT_VERSION = "PUBLIC_REVIEW_CONSENT_V1";

const initialFormState: ReviewFormState = {
  reviewerDisplayPreference: "FIRST_NAME_LAST_INITIAL",
  reviewerDisplayName: "",
  reviewTitle: "",
  reviewText: "",
  rating: 0,
  customerConsentConfirmed: false,
};

const displayPreferenceOptions: Array<{
  value: CustomerReviewDisplayPreference;
  label: string;
  description: string;
}> = [
  {
    value: "FULL_NAME",
    label: "Full name",
    description: "Display the name exactly as entered.",
  },
  {
    value: "FIRST_NAME_LAST_INITIAL",
    label: "First name and last initial",
    description: "Example: John D.",
  },
  {
    value: "FIRST_NAME_ONLY",
    label: "First name only",
    description: "Example: John",
  },
  {
    value: "ANONYMOUS",
    label: "Anonymous",
    description: "Do not display a customer name.",
  },
  {
    value: "CUSTOM",
    label: "Custom display name",
    description: "Display the name exactly as entered.",
  },
];

export function CustomerReviewSubmissionPage() {
  const [searchParameters] = useSearchParams();
  const invitationToken = searchParameters.get("token")?.trim() ?? "";

  const [form, setForm] = useState<ReviewFormState>(initialFormState);
  const [errors, setErrors] = useState<ReviewFormErrors>({});
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const [submittedReview, setSubmittedReview] =
    useState<PublicCustomerReview | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const activeRequestControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      activeRequestControllerRef.current?.abort();
    };
  }, []);

  const updateField = <TField extends ReviewFieldName>(
    field: TField,
    value: ReviewFormState[TField],
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    setSubmissionError(null);

    if (errors[field]) {
      setErrors((current) => {
        const nextErrors = { ...current };
        delete nextErrors[field];
        return nextErrors;
      });
    }
  };

  const handleTextChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const field = event.target.name as
      | "reviewerDisplayName"
      | "reviewTitle"
      | "reviewText";

    updateField(field, event.target.value);
  };

  const handleDisplayPreferenceChange = (
    event: ChangeEvent<HTMLSelectElement>,
  ) => {
    const preference = event.target.value as CustomerReviewDisplayPreference;

    setForm((current) => ({
      ...current,
      reviewerDisplayPreference: preference,
      reviewerDisplayName:
        preference === "ANONYMOUS" ? "" : current.reviewerDisplayName,
    }));

    setErrors((current) => {
      const nextErrors = { ...current };
      delete nextErrors.reviewerDisplayPreference;

      if (preference === "ANONYMOUS") {
        delete nextErrors.reviewerDisplayName;
      }

      return nextErrors;
    });
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const validationErrors = validateReviewForm(form, invitationToken);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      focusFirstInvalidField(validationErrors);
      return;
    }

    setErrors({});
    setSubmissionError(null);
    setIsSubmitting(true);

    const controller = new AbortController();
    activeRequestControllerRef.current = controller;

    try {
      const request: CustomerReviewPublicSubmissionRequest = {
        token: invitationToken,
        reviewerDisplayPreference: form.reviewerDisplayPreference,
        reviewerDisplayName:
          form.reviewerDisplayPreference === "ANONYMOUS"
            ? null
            : normalizeOptional(form.reviewerDisplayName),
        reviewTitle: normalizeOptional(form.reviewTitle),
        reviewText: form.reviewText.trim(),
        rating: form.rating,
        serviceId: null,
        customerPhotoMediaId: null,
        customerConsentConfirmed: true,
        customerConsentVersion: CONSENT_VERSION,
      };

      const response = await submitPublicCustomerReview(
        request,
        controller.signal,
      );

      setSubmittedReview(response);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      if (controller.signal.aborted) {
        return;
      }

      setSubmissionError(
        error instanceof Error
          ? error.message
          : "Your review could not be submitted. The invitation may be invalid, expired, used, or revoked.",
      );
    } finally {
      if (activeRequestControllerRef.current === controller) {
        activeRequestControllerRef.current = null;
      }

      setIsSubmitting(false);
    }
  };

  if (!invitationToken) {
    return (
      <ReviewSubmissionMessage
        title="Review invitation required"
        message="This page requires a secure review-invitation link. Open the link provided by Romelt TechCare after your completed service."
        isError
      />
    );
  }

  if (submittedReview) {
    return (
      <ReviewSubmissionMessage
        title="Thank you for your review"
        message="Your review was submitted successfully and is awaiting moderation. It will appear publicly only after it has been reviewed and approved."
      />
    );
  }

  return (
    <>
      <section className="relative isolate overflow-hidden bg-gradient-to-br from-[#0B2545] via-[#12365F] to-[#1976D2] py-14 text-white sm:py-18">
        <div
          aria-hidden="true"
          className="absolute -left-32 -top-32 -z-10 h-96 w-96 rounded-full bg-[#1976D2]/30 blur-3xl"
        />

        <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl border border-white/20 bg-white/10">
            <MessageSquareQuote
              className="size-8 text-[#D4AF37]"
              aria-hidden="true"
            />
          </div>

          <h1 className="mt-6 text-4xl font-black tracking-tight sm:text-5xl">
            Share your Romelt TechCare experience
          </h1>

          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-[#EAF4FD]">
            Your feedback helps us improve and helps other customers make an
            informed decision.
          </p>
        </div>
      </section>

      <section className="bg-[#F8FAFC] py-10 sm:py-14">
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
          <form
            onSubmit={handleSubmit}
            noValidate
            className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-[#0B2545]/8"
          >
            <div className="border-b border-slate-200 bg-[#EAF4FD] px-5 py-5 sm:px-7">
              <h2 className="text-2xl font-black text-[#0B2545]">
                Customer Review
              </h2>

              <p className="mt-2 leading-7 text-slate-600">
                Fields marked with an asterisk are required.
              </p>
            </div>

            <div className="space-y-7 p-5 sm:p-7">
              {submissionError && (
                <div
                  role="alert"
                  className="rounded-2xl border border-red-200 bg-red-50 p-4"
                >
                  <div className="flex items-start gap-3">
                    <CircleAlert
                      className="mt-0.5 size-5 shrink-0 text-[#C62828]"
                      aria-hidden="true"
                    />

                    <div>
                      <p className="font-extrabold text-[#0B2545]">
                        Review could not be submitted
                      </p>

                      <p className="mt-1 text-sm leading-6 text-slate-600">
                        {submissionError}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              <fieldset>
                <legend className="font-extrabold text-[#0B2545]">
                  Overall rating <span className="text-[#C62828]">*</span>
                </legend>

                <div className="mt-3">
                  <StarRating
                    value={form.rating}
                    size="lg"
                    interactive
                    disabled={isSubmitting}
                    onChange={(rating) => updateField("rating", rating)}
                  />
                </div>

                {errors.rating && (
                  <p className="mt-2 text-sm font-semibold text-[#C62828]">
                    {errors.rating}
                  </p>
                )}
              </fieldset>

              <div>
                <label
                  htmlFor="reviewerDisplayPreference"
                  className="block font-extrabold text-[#0B2545]"
                >
                  Public name preference{" "}
                  <span className="text-[#C62828]">*</span>
                </label>

                <select
                  id="reviewerDisplayPreference"
                  name="reviewerDisplayPreference"
                  value={form.reviewerDisplayPreference}
                  onChange={handleDisplayPreferenceChange}
                  disabled={isSubmitting}
                  className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 text-[#0B2545]"
                >
                  {displayPreferenceOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>

                <p className="mt-2 text-sm text-slate-500">
                  {
                    displayPreferenceOptions.find(
                      (option) =>
                        option.value === form.reviewerDisplayPreference,
                    )?.description
                  }
                </p>
              </div>

              {form.reviewerDisplayPreference !== "ANONYMOUS" && (
                <div>
                  <label
                    htmlFor="reviewerDisplayName"
                    className="block font-extrabold text-[#0B2545]"
                  >
                    Display name <span className="text-[#C62828]">*</span>
                  </label>

                  <input
                    id="reviewerDisplayName"
                    name="reviewerDisplayName"
                    type="text"
                    maxLength={180}
                    value={form.reviewerDisplayName}
                    onChange={handleTextChange}
                    disabled={isSubmitting}
                    autoComplete="name"
                    className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 px-4 text-[#0B2545]"
                  />

                  {errors.reviewerDisplayName && (
                    <p className="mt-2 text-sm font-semibold text-[#C62828]">
                      {errors.reviewerDisplayName}
                    </p>
                  )}
                </div>
              )}

              <div>
                <label
                  htmlFor="reviewTitle"
                  className="block font-extrabold text-[#0B2545]"
                >
                  Review title
                </label>

                <input
                  id="reviewTitle"
                  name="reviewTitle"
                  type="text"
                  maxLength={255}
                  value={form.reviewTitle}
                  onChange={handleTextChange}
                  disabled={isSubmitting}
                  placeholder="Example: Fast and dependable service"
                  className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 px-4 text-[#0B2545]"
                />
              </div>

              <div>
                <label
                  htmlFor="reviewText"
                  className="block font-extrabold text-[#0B2545]"
                >
                  Your review <span className="text-[#C62828]">*</span>
                </label>

                <textarea
                  id="reviewText"
                  name="reviewText"
                  rows={7}
                  maxLength={10000}
                  value={form.reviewText}
                  onChange={handleTextChange}
                  disabled={isSubmitting}
                  placeholder="Tell us about the service you received..."
                  className="focus-ring mt-2 w-full resize-y rounded-xl border border-slate-300 px-4 py-3 text-[#0B2545]"
                />

                <div className="mt-2 flex items-start justify-between gap-3">
                  {errors.reviewText ? (
                    <p className="text-sm font-semibold text-[#C62828]">
                      {errors.reviewText}
                    </p>
                  ) : (
                    <span />
                  )}

                  <span className="text-xs font-semibold text-slate-500">
                    {form.reviewText.length.toLocaleString()} / 10,000
                  </span>
                </div>
              </div>

              <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-[#B9D8F7] bg-[#EAF4FD] p-4">
                <input
                  id="customerConsentConfirmed"
                  name="customerConsentConfirmed"
                  type="checkbox"
                  checked={form.customerConsentConfirmed}
                  onChange={(event) =>
                    updateField(
                      "customerConsentConfirmed",
                      event.target.checked,
                    )
                  }
                  disabled={isSubmitting}
                  className="mt-1 size-5 rounded border-slate-300 text-[#1976D2]"
                />

                <span>
                  <span className="font-extrabold text-[#0B2545]">
                    Public review consent{" "}
                    <span className="text-[#C62828]">*</span>
                  </span>

                  <span className="mt-1 block text-sm leading-6 text-slate-600">
                    I authorize Romelt TechCare to display my selected public
                    name, rating, title, review text, linked service, and any
                    approved business response on its website.
                  </span>
                </span>
              </label>

              {errors.customerConsentConfirmed && (
                <p className="-mt-4 text-sm font-semibold text-[#C62828]">
                  {errors.customerConsentConfirmed}
                </p>
              )}

              <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
                <ShieldCheck
                  className="mt-0.5 size-5 shrink-0 text-[#1976D2]"
                  aria-hidden="true"
                />

                <p>
                  Your email, phone number, invitation token, IP address, and
                  consent evidence are not included in the public review.
                </p>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="focus-ring inline-flex min-h-14 w-full items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-6 py-3.5 font-extrabold text-white shadow-lg shadow-[#1976D2]/20 transition hover:bg-[#1565C0] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? (
                  <>
                    <LoaderCircle
                      className="size-5 animate-spin"
                      aria-hidden="true"
                    />
                    Submitting Review…
                  </>
                ) : (
                  <>
                    <Send className="size-5" aria-hidden="true" />
                    Submit Review
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </section>
    </>
  );
}

interface ReviewSubmissionMessageProps {
  title: string;
  message: string;
  isError?: boolean;
}

function ReviewSubmissionMessage({
  title,
  message,
  isError = false,
}: ReviewSubmissionMessageProps) {
  return (
    <main className="flex min-h-[70vh] items-center justify-center bg-[#F8FAFC] px-4 py-16">
      <div className="w-full max-w-xl rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-xl">
        <div
          className={[
            "mx-auto flex size-16 items-center justify-center rounded-2xl",
            isError
              ? "bg-red-50 text-[#C62828]"
              : "bg-emerald-50 text-emerald-700",
          ].join(" ")}
        >
          {isError ? (
            <CircleAlert className="size-8" aria-hidden="true" />
          ) : (
            <CheckCircle2 className="size-8" aria-hidden="true" />
          )}
        </div>

        <h1 className="mt-6 text-3xl font-black text-[#0B2545]">{title}</h1>

        <p className="mt-4 leading-7 text-slate-600">{message}</p>

        <Link
          to="/"
          className="mt-7 inline-flex min-h-12 items-center justify-center rounded-xl bg-[#1976D2] px-6 py-3 font-extrabold !text-white transition hover:bg-[#1565C0] hover:!text-white"
        >
          Return to Homepage
        </Link>
      </div>
    </main>
  );
}

function validateReviewForm(
  form: ReviewFormState,
  token: string,
): ReviewFormErrors {
  const errors: ReviewFormErrors = {};

  if (!token.trim()) {
    errors.token = "Review invitation token is required.";
  }

  if (form.rating < 1 || form.rating > 5) {
    errors.rating = "Select a rating between one and five stars.";
  }

  if (
    form.reviewerDisplayPreference !== "ANONYMOUS" &&
    !form.reviewerDisplayName.trim()
  ) {
    errors.reviewerDisplayName = "Display name is required.";
  }

  if (form.reviewerDisplayName.trim().length > 180) {
    errors.reviewerDisplayName = "Display name must not exceed 180 characters.";
  }

  if (!form.reviewText.trim()) {
    errors.reviewText = "Review text is required.";
  } else if (form.reviewText.trim().length > 10000) {
    errors.reviewText = "Review text must not exceed 10,000 characters.";
  }

  if (!form.customerConsentConfirmed) {
    errors.customerConsentConfirmed = "You must confirm public review consent.";
  }

  return errors;
}

function focusFirstInvalidField(errors: ReviewFormErrors) {
  const firstField = Object.keys(errors)[0];

  if (!firstField || firstField === "token") {
    return;
  }

  window.requestAnimationFrame(() => {
    document.getElementById(firstField)?.focus();
  });
}

function normalizeOptional(value: string): string | null {
  const normalized = value.trim();
  return normalized || null;
}
