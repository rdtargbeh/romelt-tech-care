/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CREATE CUSTOMER REVIEW PAGE
 * ================================================================
 *
 * Purpose:
 * Allows an administrator to record legitimate customer feedback for
 * an actual completed, review-eligible Romelt TechCare service.
 *
 * Core business rule:
 *
 * Customer
 *   -> BookingRequest
 *       -> WebsiteService
 *           -> CustomerReview
 *
 * Every review must be anchored to a completed BookingRequest.
 *
 * The administrator does NOT manually supply:
 * - customer email;
 * - customer phone;
 * - customer verification status;
 * - service ID;
 * - contact inquiry ID.
 *
 * The backend derives:
 * - Customer identity from bookingRequest.customerId;
 * - customer email from Customer.primaryEmail;
 * - customer phone from Customer.primaryPhone;
 * - WebsiteService from bookingRequest.serviceId;
 * - verified-customer status from the completed booking.
 *
 * Administrator responsibilities:
 * - identify the completed booking;
 * - record how the feedback was received;
 * - record the customer's review accurately;
 * - record public display preference;
 * - record customer consent when actually granted;
 * - optionally associate an approved public customer-photo asset;
 * - review the information before submission.
 *
 * Important:
 * - Administrator-created reviews are created as PENDING.
 * - Creating a review does not approve or publish it.
 * - Approval and publication remain separate moderation actions.
 * - A booking may have only one customer review.
 *
 * Route:
 * /admin/customers/reviews/new
 *
 * Real-data integration:
 * POST /api/v1/admin/customer-reviews
 *
 * Future enhancement:
 * The booking ID field can later be replaced by a searchable completed
 * booking selector once the eligible-bookings endpoint is connected.
 * ================================================================
 */

import {
  ArrowLeft,
  BadgeCheck,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  ClipboardCheck,
  FileText,
  Image,
  LoaderCircle,
  MessageSquareQuote,
  ShieldCheck,
  Star,
  UserRound,
} from "lucide-react";

import { type ChangeEvent, type ReactNode, useMemo, useState } from "react";

import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { createAdminCustomerReview } from "@/services/admin-customer-review.service";

import type {
  AdminCustomerReviewCreatePayload,
  CustomerReviewDisplayPreference,
  CustomerReviewSource,
} from "@/types/admin-customer-review.types";

// =====================================================================
// CONFIGURATION
// =====================================================================

const TOTAL_STEPS = 4;

const ADMIN_REVIEW_CONSENT_VERSION = "ADMIN_RECORDED_REVIEW_CONSENT_V1";

interface ReviewStepDefinition {
  number: number;

  title: string;

  shortTitle: string;

  description: string;
}

const REVIEW_STEPS: ReviewStepDefinition[] = [
  {
    number: 1,
    title: "Completed service",
    shortTitle: "Service",
    description:
      "Identify the completed booking and record how the customer provided the feedback.",
  },
  {
    number: 2,
    title: "Customer review",
    shortTitle: "Review",
    description:
      "Record the customer's rating and feedback exactly as it was provided.",
  },
  {
    number: 3,
    title: "Display and consent",
    shortTitle: "Consent",
    description:
      "Record the customer's public display preference and publication consent.",
  },
  {
    number: 4,
    title: "Review and create",
    shortTitle: "Confirm",
    description:
      "Confirm the completed booking and customer feedback before creating the pending review.",
  },
];

// =====================================================================
// FORM STATE
// =====================================================================

interface ReviewFormState {
  bookingRequestId: string;

  reviewerDisplayName: string;

  reviewerDisplayPreference: CustomerReviewDisplayPreference;

  reviewTitle: string;

  reviewText: string;

  rating: number;

  reviewSource: CustomerReviewSource;

  externalSourceUrl: string;

  customerPhotoMediaId: string;

  customerConsentConfirmed: boolean;
}

// =====================================================================
// REVIEW SOURCE OPTIONS
// =====================================================================

const REVIEW_SOURCE_OPTIONS: Array<{
  value: CustomerReviewSource;

  label: string;

  description: string;
}> = [
  {
    value: "PHONE",
    label: "Phone",
    description: "The customer provided the review verbally by phone.",
  },
  {
    value: "EMAIL",
    label: "Email",
    description: "The customer sent the feedback directly by email.",
  },
  {
    value: "BOOKING_FOLLOW_UP",
    label: "Booking follow-up",
    description:
      "The feedback was collected during follow-up for the completed service.",
  },
  {
    value: "WEBSITE",
    label: "Website",
    description: "The feedback originated through the Romelt TechCare website.",
  },
  {
    value: "GOOGLE",
    label: "Google",
    description: "The customer originally published the review on Google.",
  },
  {
    value: "FACEBOOK",
    label: "Facebook",
    description: "The customer originally published the review on Facebook.",
  },
  {
    value: "OTHER",
    label: "Other",
    description:
      "The feedback was received through another documented channel.",
  },
];

// =====================================================================
// DISPLAY PREFERENCE OPTIONS
// =====================================================================

const DISPLAY_PREFERENCE_OPTIONS: Array<{
  value: CustomerReviewDisplayPreference;

  label: string;

  description: string;
}> = [
  {
    value: "FULL_NAME",
    label: "Full name",
    description: "Use the customer's full name from their customer record.",
  },
  {
    value: "FIRST_NAME_LAST_INITIAL",
    label: "First name and last initial",
    description: "Example: John D.",
  },
  {
    value: "FIRST_NAME_ONLY",
    label: "First name only",
    description: "Example: John.",
  },
  {
    value: "ANONYMOUS",
    label: "Anonymous",
    description: "Do not display the customer's name publicly.",
  },
  {
    value: "CUSTOM",
    label: "Custom display name",
    description:
      "Use a specific public display name requested by the customer.",
  },
];

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCreateCustomerReviewPage() {
  const navigate = useNavigate();

  const [searchParams] = useSearchParams();

  const initialBookingRequestId =
    searchParams.get("bookingRequestId")?.trim() ?? "";

  const [currentStep, setCurrentStep] = useState(1);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [form, setForm] = useState<ReviewFormState>({
    bookingRequestId: initialBookingRequestId,

    reviewerDisplayName: "",

    reviewerDisplayPreference: "FIRST_NAME_LAST_INITIAL",

    reviewTitle: "",

    reviewText: "",

    rating: 0,

    reviewSource: initialBookingRequestId ? "BOOKING_FOLLOW_UP" : "PHONE",

    externalSourceUrl: "",

    customerPhotoMediaId: "",

    customerConsentConfirmed: false,
  });

  // ===================================================================
  // DERIVED VALUES
  // ===================================================================

  const activeStep =
    REVIEW_STEPS.find((step) => step.number === currentStep) ?? REVIEW_STEPS[0];

  const currentSource = useMemo(
    () =>
      REVIEW_SOURCE_OPTIONS.find(
        (option) => option.value === form.reviewSource,
      ),
    [form.reviewSource],
  );

  const usesCustomDisplayName = form.reviewerDisplayPreference === "CUSTOM";

  // ===================================================================
  // CHANGE HANDLER
  // ===================================================================

  function handleChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) {
    const target = event.target;

    const { name, value } = target;

    const isCheckbox =
      target instanceof HTMLInputElement && target.type === "checkbox";

    const checked = target instanceof HTMLInputElement ? target.checked : false;

    setForm((current) => ({
      ...current,

      [name]: isCheckbox ? checked : value,
    }));

    setErrorMessage(null);
  }

  function handleRatingChange(rating: number) {
    setForm((current) => ({
      ...current,

      rating,
    }));

    setErrorMessage(null);
  }

  // ===================================================================
  // STEP NAVIGATION
  // ===================================================================

  function handleNext() {
    const validationMessage = validateStep(currentStep, form);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      scrollToTop();

      return;
    }

    if (currentStep < TOTAL_STEPS) {
      setCurrentStep((current) => current + 1);

      setErrorMessage(null);

      scrollToTop();
    }
  }

  function handlePrevious() {
    if (currentStep <= 1) {
      return;
    }

    setCurrentStep((current) => current - 1);

    setErrorMessage(null);

    scrollToTop();
  }

  function handleStepSelection(stepNumber: number) {
    if (stepNumber >= currentStep) {
      return;
    }

    setCurrentStep(stepNumber);

    setErrorMessage(null);

    scrollToTop();
  }

  // ===================================================================
  // CREATE REVIEW
  // ===================================================================

  async function handleCreateReview() {
    const validationMessage = validateReviewForm(form);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      setCurrentStep(resolveInvalidStep(form));

      scrollToTop();

      return;
    }

    const bookingRequestId = form.bookingRequestId.trim();

    const reviewText = form.reviewText.trim();

    const payload: AdminCustomerReviewCreatePayload = {
      bookingRequestId,

      reviewerDisplayName: usesCustomDisplayName
        ? toNullable(form.reviewerDisplayName)
        : null,

      reviewerDisplayPreference: form.reviewerDisplayPreference,

      reviewTitle: toNullable(form.reviewTitle),

      reviewText,

      rating: form.rating,

      reviewSource: form.reviewSource,

      externalSourceUrl: toNullable(form.externalSourceUrl),

      customerPhotoMediaId: toNullable(form.customerPhotoMediaId),

      customerConsentConfirmed: form.customerConsentConfirmed,

      customerConsentVersion: form.customerConsentConfirmed
        ? ADMIN_REVIEW_CONSENT_VERSION
        : null,
    };

    setIsSubmitting(true);

    setErrorMessage(null);

    try {
      const createdReview = await createAdminCustomerReview(payload);

      navigate(`/admin/customers/reviews/${createdReview.customerReviewId}`, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The customer review could not be created.",
      );

      scrollToTop();
    } finally {
      setIsSubmitting(false);
    }
  }

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="mx-auto w-full max-w-5xl space-y-5">
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

      <header>
        <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
          Customer management
        </p>

        <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
          Record customer review
        </h1>

        <p className="mt-2 max-w-3xl leading-7 text-slate-600">
          Record feedback about an actual completed Romelt TechCare service.
          Customer and service identity are verified by the completed booking
          and cannot be entered manually.
        </p>
      </header>

      {/* =============================================================
       * BUSINESS RULE NOTICE
       * ============================================================= */}

      <div className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3.5">
        <div className="flex items-start gap-3">
          <BadgeCheck className="mt-0.5 h-5 w-5 shrink-0 text-blue-700" />

          <div>
            <p className="font-extrabold text-blue-950">
              Completed service required
            </p>

            <p className="mt-1 text-sm leading-6 text-blue-800">
              Every review must belong to a completed, review-eligible booking.
              Romelt TechCare will resolve the customer, customer contact
              information, service, and verified-customer status from that
              booking.
            </p>
          </div>
        </div>
      </div>

      {/* =============================================================
       * PROGRESS
       * ============================================================= */}

      <div className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
        <div className="grid grid-cols-4">
          {REVIEW_STEPS.map((step, index) => {
            const isCurrent = step.number === currentStep;

            const isComplete = step.number < currentStep;

            const isClickable = step.number < currentStep;

            return (
              <div key={step.number} className="relative min-w-0">
                {index < REVIEW_STEPS.length - 1 ? (
                  <div
                    className={[
                      "absolute left-1/2 right-[-50%] top-4 h-px",

                      step.number < currentStep
                        ? "bg-emerald-300"
                        : "bg-slate-200",
                    ].join(" ")}
                    aria-hidden="true"
                  />
                ) : null}

                <button
                  type="button"
                  disabled={!isClickable}
                  onClick={() => handleStepSelection(step.number)}
                  className={[
                    "focus-ring relative z-10 flex w-full flex-col items-center rounded-lg px-1 py-1 text-center",

                    isClickable ? "cursor-pointer" : "cursor-default",
                  ].join(" ")}
                >
                  <span
                    className={[
                      "flex h-8 w-8 items-center justify-center rounded-full border text-xs font-black transition",

                      isCurrent
                        ? "border-brand-700 bg-brand-700 text-white"
                        : isComplete
                          ? "border-emerald-600 bg-emerald-600 text-white"
                          : "border-slate-300 bg-white text-slate-500",
                    ].join(" ")}
                  >
                    {isComplete ? <Check className="h-4 w-4" /> : step.number}
                  </span>

                  <span
                    className={[
                      "mt-2 block max-w-full truncate text-xs font-extrabold sm:text-sm",

                      isCurrent ? "text-brand-700" : "text-slate-600",
                    ].join(" ")}
                  >
                    {step.shortTitle}
                  </span>
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* =============================================================
       * STEP HEADER
       * ============================================================= */}

      <div className="px-1 py-1">
        <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-brand-700">
          Step {currentStep} of {TOTAL_STEPS}
        </p>

        <h2 className="mt-1 font-display text-2xl font-black text-navy-950">
          {activeStep.title}
        </h2>

        <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">
          {activeStep.description}
        </p>
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
              <p className="font-extrabold text-red-950">Check this step</p>

              <p className="mt-1 text-sm leading-6 text-red-700">
                {errorMessage}
              </p>
            </div>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * STEP CONTENT
       * ============================================================= */}

      {currentStep === 1 ? (
        <CompletedServiceStep
          form={form}
          sourceDescription={currentSource?.description ?? ""}
          onChange={handleChange}
        />
      ) : null}

      {currentStep === 2 ? (
        <CustomerReviewStep
          form={form}
          onChange={handleChange}
          onRatingChange={handleRatingChange}
        />
      ) : null}

      {currentStep === 3 ? (
        <DisplayAndConsentStep form={form} onChange={handleChange} />
      ) : null}

      {currentStep === 4 ? (
        <ReviewStep form={form} onEditStep={handleStepSelection} />
      ) : null}

      {/* =============================================================
       * NAVIGATION
       * ============================================================= */}

      <div className="sticky bottom-4 z-10 rounded-2xl border border-slate-200 bg-white/95 p-3 shadow-xl backdrop-blur">
        <div className="flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={handlePrevious}
            disabled={currentStep === 1 || isSubmitting}
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
            Previous
          </button>

          {currentStep < TOTAL_STEPS ? (
            <button
              type="button"
              onClick={handleNext}
              disabled={isSubmitting}
              className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 font-bold text-white transition hover:bg-navy-900 disabled:opacity-50"
            >
              Continue
              <ChevronRight className="h-4 w-4" />
            </button>
          ) : (
            <button
              type="button"
              onClick={() => void handleCreateReview()}
              disabled={isSubmitting}
              className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting ? (
                <LoaderCircle className="h-4 w-4 animate-spin" />
              ) : (
                <ClipboardCheck className="h-4 w-4" />
              )}

              {isSubmitting ? "Creating review..." : "Create review"}
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// STEP 1 — COMPLETED SERVICE
// =====================================================================

function CompletedServiceStep({
  form,
  sourceDescription,
  onChange,
}: {
  form: ReviewFormState;

  sourceDescription: string;

  onChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => void;
}) {
  return (
    <div className="space-y-5">
      <FormSection
        title="Completed service"
        description="Identify the completed booking that proves which customer received the service being reviewed."
        icon={<BadgeCheck />}
      >
        <Field label="Completed booking request ID" required>
          <input
            type="text"
            name="bookingRequestId"
            value={form.bookingRequestId}
            onChange={onChange}
            placeholder="Booking UUID"
            autoComplete="off"
            className={inputClass}
          />

          <FieldHint>
            Required. The backend will verify that this booking is completed,
            review eligible, linked to a customer, and linked to a service. One
            booking may have only one customer review.
          </FieldHint>
        </Field>

        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

            <div>
              <p className="font-extrabold text-emerald-950">
                Customer and service are automatic
              </p>

              <p className="mt-1 text-sm leading-6 text-emerald-800">
                Do not enter a customer email, phone number, service ID,
                verification flag, or contact inquiry. The backend obtains the
                authoritative customer and service information from this
                completed booking.
              </p>
            </div>
          </div>
        </div>
      </FormSection>

      <FormSection
        title="Review source"
        description="Record how or where the customer communicated this feedback."
        icon={<MessageSquareQuote />}
      >
        <FormGrid>
          <Field label="Review source" required>
            <select
              name="reviewSource"
              value={form.reviewSource}
              onChange={onChange}
              className={inputClass}
            >
              {REVIEW_SOURCE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <FieldHint>{sourceDescription}</FieldHint>
          </Field>

          <Field label="External source URL">
            <input
              type="url"
              name="externalSourceUrl"
              value={form.externalSourceUrl}
              onChange={onChange}
              maxLength={1500}
              placeholder="https://..."
              className={inputClass}
            />

            <FieldHint>
              Use this for the original Google, Facebook, or other external
              review location.
            </FieldHint>
          </Field>
        </FormGrid>
      </FormSection>

      <FormSection
        title="Optional customer photo"
        description="Associate an existing public media asset with the review when appropriate."
        icon={<Image />}
      >
        <Field label="Customer photo media ID">
          <input
            type="text"
            name="customerPhotoMediaId"
            value={form.customerPhotoMediaId}
            onChange={onChange}
            placeholder="Optional media asset UUID"
            autoComplete="off"
            className={inputClass}
          />

          <FieldHint>
            Optional. The backend will reject media that is not approved for
            public use.
          </FieldHint>
        </Field>
      </FormSection>
    </div>
  );
}

// =====================================================================
// STEP 2 — CUSTOMER REVIEW
// =====================================================================

function CustomerReviewStep({
  form,
  onChange,
  onRatingChange,
}: {
  form: ReviewFormState;

  onChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => void;

  onRatingChange: (rating: number) => void;
}) {
  return (
    <FormSection
      title="Customer feedback"
      description="Record the customer's rating and feedback as accurately as possible."
      icon={<FileText />}
    >
      <div className="space-y-5">
        <Field label="Customer rating" required>
          <div className="flex flex-wrap gap-2">
            {[1, 2, 3, 4, 5].map((rating) => (
              <button
                key={rating}
                type="button"
                onClick={() => onRatingChange(rating)}
                className={[
                  "focus-ring flex h-12 w-12 items-center justify-center rounded-xl border transition",

                  rating <= form.rating
                    ? "border-amber-300 bg-amber-50 text-amber-500"
                    : "border-slate-300 bg-white text-slate-300 hover:border-amber-300",
                ].join(" ")}
                aria-label={`${rating} star rating`}
              >
                <Star
                  className={[
                    "h-6 w-6",

                    rating <= form.rating ? "fill-current" : "",
                  ].join(" ")}
                />
              </button>
            ))}
          </div>

          <FieldHint>
            {form.rating
              ? `${form.rating} out of 5 stars`
              : "Select the rating provided by the customer."}
          </FieldHint>
        </Field>

        <Field label="Review title">
          <input
            type="text"
            name="reviewTitle"
            value={form.reviewTitle}
            onChange={onChange}
            maxLength={255}
            placeholder="Optional review title"
            className={inputClass}
          />
        </Field>

        <Field label="Customer review" required>
          <textarea
            name="reviewText"
            value={form.reviewText}
            onChange={onChange}
            rows={8}
            maxLength={10000}
            placeholder="Enter the customer's feedback exactly as it was provided..."
            className={inputClass}
          />

          <div className="mt-1 flex justify-between gap-3 text-xs text-slate-500">
            <span>Preserve the customer's meaning and tone.</span>

            <span>
              {form.reviewText.length}
              /10,000
            </span>
          </div>
        </Field>
      </div>
    </FormSection>
  );
}

// =====================================================================
// STEP 3 — DISPLAY AND CONSENT
// =====================================================================

function DisplayAndConsentStep({
  form,
  onChange,
}: {
  form: ReviewFormState;

  onChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => void;
}) {
  const isCustom = form.reviewerDisplayPreference === "CUSTOM";

  return (
    <div className="space-y-5">
      <FormSection
        title="Public display preference"
        description="Record how the customer wants their identity displayed if the review is later published."
        icon={<UserRound />}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {DISPLAY_PREFERENCE_OPTIONS.map((option) => (
            <label
              key={option.value}
              className={[
                "cursor-pointer rounded-xl border p-4 transition",

                form.reviewerDisplayPreference === option.value
                  ? "border-brand-300 bg-brand-50"
                  : "border-slate-200 bg-white hover:border-slate-300",
              ].join(" ")}
            >
              <div className="flex items-start gap-3">
                <input
                  type="radio"
                  name="reviewerDisplayPreference"
                  value={option.value}
                  checked={form.reviewerDisplayPreference === option.value}
                  onChange={onChange}
                  className="mt-1 h-4 w-4 accent-brand-700"
                />

                <div>
                  <p className="font-extrabold text-navy-950">{option.label}</p>

                  <p className="mt-1 text-sm leading-5 text-slate-600">
                    {option.description}
                  </p>
                </div>
              </div>
            </label>
          ))}
        </div>

        {isCustom ? (
          <div className="mt-5">
            <Field label="Custom display name" required>
              <input
                type="text"
                name="reviewerDisplayName"
                value={form.reviewerDisplayName}
                onChange={onChange}
                maxLength={180}
                placeholder="Display name requested by the customer"
                className={inputClass}
              />

              <FieldHint>
                Required only when Custom display name is selected.
              </FieldHint>
            </Field>
          </div>
        ) : (
          <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-extrabold text-navy-950">
              Customer name comes from the customer record
            </p>

            <p className="mt-1 text-sm leading-6 text-slate-600">
              The administrator does not type the customer's name for standard
              display preferences. The backend resolves the canonical customer
              display name through the completed booking.
            </p>
          </div>
        )}
      </FormSection>

      <FormSection
        title="Verified customer"
        description="Verification is established automatically from the completed booking."
        icon={<BadgeCheck />}
      >
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

            <div>
              <p className="font-extrabold text-emerald-950">
                Automatically verified
              </p>

              <p className="mt-1 text-sm leading-6 text-emerald-800">
                The administrator cannot manually mark a review verified. The
                backend verifies the completed booking, its customer
                relationship, service relationship, and review eligibility
                before creating the review.
              </p>
            </div>
          </div>
        </div>
      </FormSection>

      <FormSection
        title="Customer consent"
        description="Public publication requires the customer's permission."
        icon={<ShieldCheck />}
      >
        <label
          className={[
            "flex cursor-pointer items-start gap-3 rounded-xl border p-4",

            form.customerConsentConfirmed
              ? "border-emerald-300 bg-emerald-50"
              : "border-amber-300 bg-amber-50",
          ].join(" ")}
        >
          <input
            type="checkbox"
            name="customerConsentConfirmed"
            checked={form.customerConsentConfirmed}
            onChange={onChange}
            className="mt-1 h-4 w-4 accent-brand-700"
          />

          <div>
            <p className="font-extrabold text-navy-950">
              Customer authorized Romelt TechCare to use this review publicly
            </p>

            <p className="mt-1 text-sm leading-6 text-slate-600">
              Select this only when the customer explicitly agreed that Romelt
              TechCare may publish the review on its website or other public
              business channels.
            </p>
          </div>
        </label>

        <p className="mt-3 text-xs leading-5 text-slate-500">
          Recording consent does not publish the review. Administrator approval
          and a separate publish action are still required.
        </p>
      </FormSection>
    </div>
  );
}

// =====================================================================
// STEP 4 — REVIEW
// =====================================================================

function ReviewStep({
  form,
  onEditStep,
}: {
  form: ReviewFormState;

  onEditStep: (step: number) => void;
}) {
  return (
    <div className="space-y-5">
      <ReadOnlyCard
        title="Completed service"
        icon={<BadgeCheck />}
        onEdit={() => onEditStep(1)}
      >
        <ReviewGrid>
          <ReviewValue
            label="Completed booking"
            value={form.bookingRequestId || "Not selected"}
            mono
          />

          <ReviewValue
            label="Review source"
            value={formatEnumLabel(form.reviewSource)}
          />

          <ReviewValue
            label="External source"
            value={form.externalSourceUrl || "Not applicable"}
          />

          <ReviewValue
            label="Customer photo media"
            value={form.customerPhotoMediaId || "Not provided"}
            mono={Boolean(form.customerPhotoMediaId)}
          />
        </ReviewGrid>

        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4">
          <p className="text-sm font-extrabold text-navy-950">
            Backend-derived service identity
          </p>

          <p className="mt-1 text-sm leading-6 text-slate-600">
            Customer name, email, phone, WebsiteService, and verified status
            will be resolved from the completed booking when this review is
            created.
          </p>
        </div>
      </ReadOnlyCard>

      <ReadOnlyCard
        title="Customer review"
        icon={<MessageSquareQuote />}
        onEdit={() => onEditStep(2)}
      >
        <div className="space-y-4">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
              Rating
            </p>

            <div className="mt-2 flex items-center gap-1">
              {Array.from(
                {
                  length: 5,
                },
                (_, index) => (
                  <Star
                    key={index}
                    className={[
                      "h-5 w-5",

                      index < form.rating
                        ? "fill-amber-400 text-amber-400"
                        : "text-slate-300",
                    ].join(" ")}
                  />
                ),
              )}

              <span className="ml-2 text-sm font-black text-slate-700">
                {form.rating}/5
              </span>
            </div>
          </div>

          <ReviewValue label="Title" value={form.reviewTitle || "No title"} />

          <div>
            <p className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
              Review
            </p>

            <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-700">
              {form.reviewText}
            </p>
          </div>
        </div>
      </ReadOnlyCard>

      <ReadOnlyCard
        title="Display and consent"
        icon={<ShieldCheck />}
        onEdit={() => onEditStep(3)}
      >
        <ReviewGrid>
          <ReviewValue
            label="Display preference"
            value={formatEnumLabel(form.reviewerDisplayPreference)}
          />

          {form.reviewerDisplayPreference === "CUSTOM" ? (
            <ReviewValue
              label="Custom display name"
              value={form.reviewerDisplayName || "Not provided"}
            />
          ) : null}

          <ReviewValue
            label="Verified customer"
            value="Backend verified from completed booking"
          />

          <ReviewValue
            label="Customer consent"
            value={
              form.customerConsentConfirmed ? "Confirmed" : "Not confirmed"
            }
          />

          <ReviewValue
            label="Consent version"
            value={
              form.customerConsentConfirmed
                ? ADMIN_REVIEW_CONSENT_VERSION
                : "Not applicable"
            }
          />
        </ReviewGrid>
      </ReadOnlyCard>

      <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
        <div className="flex items-start gap-3">
          <ClipboardCheck className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />

          <div>
            <p className="font-extrabold text-amber-950">
              Review will remain pending
            </p>

            <p className="mt-1 text-sm leading-6 text-amber-800">
              Creating this record does not approve or publish it. The review
              enters the administrator moderation queue first.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// SHARED FORM COMPONENTS
// =====================================================================

function FormSection({
  title,
  description,
  icon,
  children,
}: {
  title: string;

  description: string;

  icon: ReactNode;

  children: ReactNode;
}) {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center gap-3 border-b border-slate-100 bg-slate-50/60 px-5 py-3.5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <div className="min-w-0">
          <h3 className="font-black text-navy-950">{title}</h3>

          <p className="mt-0.5 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>
      </div>

      <div className="p-5 sm:p-6">{children}</div>
    </section>
  );
}

function FormGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-x-5 gap-y-4 md:grid-cols-2">{children}</div>;
}

function Field({
  label,
  required = false,
  children,
}: {
  label: string;

  required?: boolean;

  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="text-sm font-extrabold text-navy-950">
        {label}

        {required ? <span className="ml-1 text-red-600">*</span> : null}
      </span>

      <div className="mt-2">{children}</div>
    </label>
  );
}

function FieldHint({ children }: { children: ReactNode }) {
  return <p className="mt-1 text-xs leading-5 text-slate-500">{children}</p>;
}

// =====================================================================
// REVIEW COMPONENTS
// =====================================================================

function ReadOnlyCard({
  title,
  icon,
  onEdit,
  children,
}: {
  title: string;

  icon: ReactNode;

  onEdit: () => void;

  children: ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-5 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
            {icon}
          </div>

          <h3 className="font-black text-navy-950">{title}</h3>
        </div>

        <button
          type="button"
          onClick={onEdit}
          className="focus-ring rounded-lg px-3 py-2 text-sm font-bold text-brand-700 transition hover:bg-brand-50"
        >
          Edit
        </button>
      </div>

      <div className="p-5">{children}</div>
    </section>
  );
}

function ReviewGrid({ children }: { children: ReactNode }) {
  return <dl className="grid gap-4 md:grid-cols-2">{children}</dl>;
}

function ReviewValue({
  label,
  value,
  mono = false,
}: {
  label: string;

  value: string;

  mono?: boolean;
}) {
  return (
    <div>
      <dt className="text-xs font-extrabold uppercase tracking-wide text-slate-500">
        {label}
      </dt>

      <dd
        className={[
          "mt-1 break-words text-sm font-semibold text-slate-700",

          mono ? "font-mono text-xs" : "",
        ].join(" ")}
      >
        {value}
      </dd>
    </div>
  );
}

// =====================================================================
// STYLES
// =====================================================================

const inputClass =
  "focus-ring min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition focus:border-brand-500";

// =====================================================================
// VALIDATION
// =====================================================================

function validateStep(step: number, form: ReviewFormState): string | null {
  switch (step) {
    case 1: {
      if (!form.bookingRequestId.trim()) {
        return "Select or enter the completed booking for this customer review.";
      }

      if (!form.reviewSource) {
        return "Select how the customer provided the review.";
      }

      if (
        requiresExternalUrl(form.reviewSource) &&
        !form.externalSourceUrl.trim()
      ) {
        return `Enter the original ${formatEnumLabel(
          form.reviewSource,
        )} review URL.`;
      }

      if (
        form.externalSourceUrl.trim() &&
        !isValidHttpUrl(form.externalSourceUrl)
      ) {
        return "Enter a valid external review URL.";
      }

      return null;
    }

    case 2: {
      if (form.rating < 1 || form.rating > 5) {
        return "Select the customer's rating from 1 to 5 stars.";
      }

      if (!form.reviewText.trim()) {
        return "Enter the customer's review.";
      }

      return null;
    }

    case 3: {
      if (
        form.reviewerDisplayPreference === "CUSTOM" &&
        !form.reviewerDisplayName.trim()
      ) {
        return "Enter the custom public display name requested by the customer.";
      }

      return null;
    }

    case 4:
      return validateReviewForm(form);

    default:
      return null;
  }
}

function validateReviewForm(form: ReviewFormState): string | null {
  for (let step = 1; step <= 3; step++) {
    const error = validateStep(step, form);

    if (error) {
      return error;
    }
  }

  return null;
}

function resolveInvalidStep(form: ReviewFormState): number {
  for (let step = 1; step <= 3; step++) {
    if (validateStep(step, form)) {
      return step;
    }
  }

  return 4;
}

// =====================================================================
// HELPERS
// =====================================================================

function requiresExternalUrl(source: CustomerReviewSource): boolean {
  return source === "GOOGLE" || source === "FACEBOOK";
}

function isValidHttpUrl(value: string): boolean {
  try {
    const url = new URL(value.trim());

    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function toNullable(value: string): string | null {
  const normalized = value.trim();

  return normalized || null;
}

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function scrollToTop() {
  window.scrollTo({
    top: 0,

    behavior: "smooth",
  });
}
