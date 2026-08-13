/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BOOKING PAGE
 * ================================================================
 *
 * Purpose:
 * Allows customers to submit personal or business technology-service
 * booking requests to the Spring Boot backend.
 *
 * Six-step workflow:
 * 1. About You
 * 2. Service
 * 3. Request Details
 * 4. Schedule
 * 5. Location
 * 6. Review, Acknowledge & Submit
 *
 * Responsibilities:
 * - Matches BookingRequestCreateRequest exactly.
 * - Supports PERSONAL and BUSINESS bookings.
 * - Collects primary customer contact information.
 * - Uses the customer's primary email and phone for notifications.
 * - Collects required business details for BUSINESS bookings.
 * - Collects service, schedule, device, and problem information.
 * - Requires complete location information for ON_SITE service.
 * - Requires customer acknowledgement immediately before submission.
 * - Keeps all review information read-only.
 * - Maps backend validation errors to matching form fields.
 * - Prevents duplicate submissions.
 * - Displays the backend booking reference after acceptance.
 *
 * Real-data integration:
 * POST /api/v1/public/booking-requests
 *
 * Important:
 * A successful submission means the booking request was received.
 * It does not mean the appointment has been confirmed.
 * ================================================================
 */

import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  useEffect,
  useRef,
  useState,
} from "react";

import {
  ArrowLeft,
  ArrowRight,
  Building2,
  CalendarCheck2,
  Check,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  Clock3,
  Info,
  Mail,
  MapPin,
  Phone,
  Send,
  ShieldCheck,
  UserRound,
  Wrench,
} from "lucide-react";

import { Link } from "react-router-dom";

import { Container } from "@/components/common/Container";

import {
  businessConfig,
  businessEmailHref,
  businessPhoneHref,
} from "@/config/business.config";

import { environmentConfig } from "@/config/environment.config";
import { serviceTypeOptions } from "@/data/booking";
import { ApiError, isApiError } from "@/lib/api-error";
import { logger } from "@/lib/logger";
import { submitBookingRequest } from "@/services/booking.service";

import type { BookingRequestConfirmation } from "@/types/public-request.types";

// =====================================================================
// BACKEND ENUM TYPES
// =====================================================================

type BookingFor = "PERSONAL" | "BUSINESS";

type ServiceMethod = "REMOTE" | "ON_SITE" | "DROP_OFF" | "NOT_SURE";

type PreferredTime = "MORNING" | "AFTERNOON" | "EVENING" | "FLEXIBLE";

type ContactMethod = "EMAIL" | "PHONE" | "TEXT";

// =====================================================================
// FORM STATE
// =====================================================================

interface BookingFormState {
  bookingFor: BookingFor;

  fullName: string;
  email: string;
  phone: string;
  preferredContactMethod: string;

  notificationEmail: string;
  notificationPhone: string;

  businessName: string;
  businessEmail: string;
  businessPhone: string;
  businessStreetAddress: string;
  businessCity: string;
  businessState: string;
  businessPostalCode: string;
  businessCountryCode: string;
  businessContactRole: string;

  serviceType: string;
  serviceMethod: string;
  preferredDate: string;
  preferredTime: string;
  alternateDate: string;
  deviceType: string;
  problemDescription: string;

  streetAddress: string;
  addressLine2: string;
  city: string;
  stateRegion: string;
  postalCode: string;
  countryCode: string;

  consentAccepted: boolean;
}

// =====================================================================
// EXACT BACKEND CREATE PAYLOAD
// =====================================================================

interface BookingRequestPayload {
  bookingFor: BookingFor;

  fullName: string;
  email: string;
  phone: string;
  preferredContactMethod: ContactMethod;

  notificationEmail: string | null;
  notificationPhone: string | null;

  businessName: string | null;
  businessEmail: string | null;
  businessPhone: string | null;
  businessStreetAddress: string | null;
  businessCity: string | null;
  businessState: string | null;
  businessPostalCode: string | null;
  businessCountryCode: string | null;
  businessContactRole: string | null;

  serviceType: string;
  serviceMethod: ServiceMethod;

  preferredDate: string;
  preferredTime: PreferredTime;
  alternateDate: string | null;

  deviceType: string | null;
  problemDescription: string;

  streetAddress: string | null;
  addressLine2: string | null;
  city: string | null;
  stateRegion: string | null;
  postalCode: string | null;
  countryCode: string | null;

  consentAccepted: boolean;
}

type BookingFieldName = keyof BookingFormState;

type BookingFormErrors = Partial<Record<BookingFieldName, string>>;

interface SubmissionError {
  title: string;
  message: string;
}

// =====================================================================
// INITIAL STATE
// =====================================================================

const initialFormState: BookingFormState = {
  bookingFor: "PERSONAL",

  fullName: "",
  email: "",
  phone: "",
  preferredContactMethod: "",

  notificationEmail: "",
  notificationPhone: "",

  businessName: "",
  businessEmail: "",
  businessPhone: "",
  businessStreetAddress: "",
  businessCity: "",
  businessState: "Iowa",
  businessPostalCode: "",
  businessCountryCode: "US",
  businessContactRole: "",

  serviceType: "",
  serviceMethod: "",
  preferredDate: "",
  preferredTime: "",
  alternateDate: "",
  deviceType: "",
  problemDescription: "",

  streetAddress: "",
  addressLine2: "",
  city: "",
  stateRegion: "Iowa",
  postalCode: "",
  countryCode: "US",

  consentAccepted: false,
};

// =====================================================================
// OPTIONS
// =====================================================================

const serviceMethodOptions = [
  {
    value: "REMOTE",
    label: "Remote support",
    description: "Get help through a secure remote-support session.",
  },
  {
    value: "ON_SITE",
    label: "On-site service",
    description: "Request support at your home, office, or organization.",
  },
  {
    value: "DROP_OFF",
    label: "Drop-off service",
    description: "Receive instructions for leaving an eligible device.",
  },
  {
    value: "NOT_SURE",
    label: "Not sure",
    description: "We will recommend the most appropriate service method.",
  },
] as const;

const preferredTimeOptions = [
  {
    value: "MORNING",
    label: "Morning",
  },
  {
    value: "AFTERNOON",
    label: "Afternoon",
  },
  {
    value: "EVENING",
    label: "Evening",
  },
  {
    value: "FLEXIBLE",
    label: "Flexible",
  },
] as const;

const preferredContactOptions = [
  {
    value: "EMAIL",
    label: "Email",
  },
  {
    value: "PHONE",
    label: "Phone call",
  },
  {
    value: "TEXT",
    label: "Text message",
  },
] as const;

// =====================================================================
// PROGRESS
// =====================================================================

const TOTAL_STEPS = 6;

interface BookingProgressStep {
  number: number;
  title: string;
  description: string;
  icon: ReactNode;
}

const BOOKING_PROGRESS_STEPS: BookingProgressStep[] = [
  {
    number: 1,
    title: "About you",
    description: "Contact information",
    icon: <UserRound />,
  },
  {
    number: 2,
    title: "Service",
    description: "Choose support",
    icon: <Wrench />,
  },
  {
    number: 3,
    title: "Request details",
    description: "Describe the problem",
    icon: <Info />,
  },
  {
    number: 4,
    title: "Schedule",
    description: "Preferred date and time",
    icon: <Clock3 />,
  },
  {
    number: 5,
    title: "Location",
    description: "Service address",
    icon: <MapPin />,
  },
  {
    number: 6,
    title: "Review",
    description: "Review, confirm and submit",
    icon: <ClipboardCheck />,
  },
];

// =====================================================================
// PAGE
// =====================================================================

export function BookingPage() {
  const [form, setForm] = useState<BookingFormState>(initialFormState);

  const [currentStep, setCurrentStep] = useState(1);

  const [errors, setErrors] = useState<BookingFormErrors>({});

  const [submissionError, setSubmissionError] =
    useState<SubmissionError | null>(null);

  const [confirmation, setConfirmation] =
    useState<BookingRequestConfirmation | null>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const activeRequestControllerRef = useRef<AbortController | null>(null);

  const minimumBookingDate = getMinimumBookingDate();

  const requiresServiceAddress = form.serviceMethod === "ON_SITE";

  const isBusinessBooking = form.bookingFor === "BUSINESS";

  useEffect(() => {
    return () => {
      activeRequestControllerRef.current?.abort();
    };
  }, []);

  const updateField = (field: BookingFieldName, value: string | boolean) => {
    setForm((current) => {
      const next = {
        ...current,
        [field]: value,
      };

      /*
       * Backend requires all business fields to be empty for
       * PERSONAL bookings.
       */
      if (field === "bookingFor" && value === "PERSONAL") {
        return {
          ...next,
          businessName: "",
          businessEmail: "",
          businessPhone: "",
          businessStreetAddress: "",
          businessCity: "",
          businessState: "Iowa",
          businessPostalCode: "",
          businessCountryCode: "US",
          businessContactRole: "",
        };
      }

      return next;
    });

    setSubmissionError(null);

    if (errors[field]) {
      setErrors((current) => {
        const nextErrors = {
          ...current,
        };

        delete nextErrors[field];

        return nextErrors;
      });
    }
  };

  const handleTextChange = (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => {
    const field = event.target.name as BookingFieldName;

    updateField(field, event.target.value);
  };

  const handleNextStep = () => {
    const stepErrors = validateBookingStep(currentStep, form);

    if (Object.keys(stepErrors).length > 0) {
      setErrors(stepErrors);
      setSubmissionError(null);
      focusFirstInvalidField(stepErrors);
      return;
    }

    setErrors({});
    setSubmissionError(null);
    setCurrentStep((step) => Math.min(step + 1, TOTAL_STEPS));
    scrollToBookingForm();
  };

  const handlePreviousStep = () => {
    setErrors({});
    setSubmissionError(null);
    setCurrentStep((step) => Math.max(step - 1, 1));
    scrollToBookingForm();
  };

  const handleStepSelection = (stepNumber: number) => {
    if (stepNumber >= currentStep) {
      return;
    }

    setErrors({});
    setSubmissionError(null);
    setCurrentStep(stepNumber);
    scrollToBookingForm();
  };

  const handleReviewEdit = (stepNumber: number) => {
    setErrors({});
    setSubmissionError(null);
    setCurrentStep(stepNumber);
    scrollToBookingForm();
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    if (currentStep < TOTAL_STEPS) {
      handleNextStep();
      return;
    }

    setSubmissionError(null);

    const validationErrors = validateBookingForm(form);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);

      const invalidStep = resolveBookingStepFromErrors(validationErrors, form);
      setCurrentStep(invalidStep);

      window.requestAnimationFrame(() => {
        focusFirstInvalidField(validationErrors);
      });

      return;
    }

    setErrors({});
    setIsSubmitting(true);

    const requestController = new AbortController();

    activeRequestControllerRef.current = requestController;

    try {
      const request = createBookingRequest(form);

      let result: BookingRequestConfirmation;

      if (environmentConfig.enableBookingApi) {
        result = await submitBookingRequest<
          BookingRequestPayload,
          BookingRequestConfirmation
        >(request, requestController.signal);
      } else {
        logger.info("Booking API is disabled. Using development confirmation.");

        result = createDevelopmentConfirmation(request);
      }

      setConfirmation(normalizeConfirmation(result, request));

      scrollToPageTop();
    } catch (error) {
      if (requestController.signal.aborted) {
        return;
      }

      handleSubmissionFailure(error);
    } finally {
      if (activeRequestControllerRef.current === requestController) {
        activeRequestControllerRef.current = null;
      }

      setIsSubmitting(false);
    }
  };

  const handleSubmissionFailure = (error: unknown) => {
    logger.error("Booking request submission failed.", error);

    if (isApiError(error)) {
      const backendFieldErrors = extractBackendFieldErrors(error);

      if (Object.keys(backendFieldErrors).length > 0) {
        setErrors(backendFieldErrors);

        setSubmissionError({
          title: "Review the highlighted fields",
          message:
            "Some booking information was rejected. Correct the highlighted fields and submit the request again.",
        });

        const invalidStep = resolveBookingStepFromErrors(
          backendFieldErrors,
          form,
        );

        setCurrentStep(invalidStep);

        window.requestAnimationFrame(() => {
          focusFirstInvalidField(backendFieldErrors);
        });

        return;
      }

      setSubmissionError({
        title: getApiErrorTitle(error),
        message: error.message,
      });

      return;
    }

    setSubmissionError({
      title: "Booking request could not be sent",
      message:
        "An unexpected problem occurred. Please try again or contact Romelt TechCare directly.",
    });
  };

  const resetForm = () => {
    activeRequestControllerRef.current?.abort();

    activeRequestControllerRef.current = null;

    setForm(initialFormState);
    setCurrentStep(1);
    setErrors({});
    setSubmissionError(null);
    setConfirmation(null);
    setIsSubmitting(false);

    scrollToPageTop();
  };

  return (
    <>
      <BookingHero />

      <section className="bg-slate-50 py-8 sm:py-10 lg:py-12">
        <Container>
          <div className="mx-auto grid max-w-7xl items-start gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
            <aside className="order-2 space-y-4 lg:order-1 lg:sticky lg:top-24">
              <BookingSummaryCard form={form} />

              <AvailabilityCard />

              <SafetyAndContactCard />
            </aside>

            <main className="order-1 min-w-0 lg:order-2">
              {confirmation ? (
                <BookingConfirmation
                  fullName={form.fullName}
                  email={form.email}
                  serviceType={form.serviceType}
                  serviceMethod={form.serviceMethod}
                  confirmation={confirmation}
                  isDevelopmentFallback={!environmentConfig.enableBookingApi}
                  onReset={resetForm}
                />
              ) : (
                <div id="public-booking-form" className="space-y-4">
                  <PublicBookingProgress
                    currentStep={currentStep}
                    onStepSelection={handleStepSelection}
                  />

                  <form
                    onSubmit={handleSubmit}
                    noValidate
                    className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                  >
                    <div className="border-b border-slate-200 px-5 py-5 sm:px-6">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                        <div>
                          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
                            Booking Request
                          </p>

                          <h2 className="mt-1 font-display text-2xl font-black text-navy-950">
                            {BOOKING_PROGRESS_STEPS[currentStep - 1]?.title}
                          </h2>

                          <p className="mt-1 text-sm leading-6 text-slate-600">
                            {getPublicStepDescription(currentStep, form)}
                          </p>
                        </div>

                        <div className="text-left sm:text-right">
                          <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                            Progress
                          </p>
                          <p className="mt-1 text-sm font-extrabold text-navy-950">
                            Step {currentStep} of {TOTAL_STEPS}
                          </p>
                        </div>
                      </div>
                    </div>

                    {submissionError ? (
                      <div className="px-5 pt-5 sm:px-6">
                        <SubmissionErrorAlert error={submissionError} />
                      </div>
                    ) : Object.keys(errors).length > 0 ? (
                      <div className="px-5 pt-5 sm:px-6">
                        <ValidationAlert />
                      </div>
                    ) : null}

                    <div className="p-4 sm:p-5 lg:p-6">
                      {currentStep === 1 ? (
                        <PublicCustomerStep
                          form={form}
                          errors={errors}
                          isBusinessBooking={isBusinessBooking}
                          onTextChange={handleTextChange}
                          onBookingTypeChange={(value) =>
                            updateField("bookingFor", value)
                          }
                        />
                      ) : null}

                      {currentStep === 2 ? (
                        <PublicServiceStep
                          form={form}
                          errors={errors}
                          onTextChange={handleTextChange}
                        />
                      ) : null}

                      {currentStep === 3 ? (
                        <PublicRequestDetailsStep
                          form={form}
                          errors={errors}
                          onTextChange={handleTextChange}
                        />
                      ) : null}

                      {currentStep === 4 ? (
                        <PublicScheduleStep
                          form={form}
                          errors={errors}
                          minimumBookingDate={minimumBookingDate}
                          onTextChange={handleTextChange}
                        />
                      ) : null}

                      {currentStep === 5 ? (
                        <PublicLocationStep
                          form={form}
                          errors={errors}
                          requiresServiceAddress={requiresServiceAddress}
                          onTextChange={handleTextChange}
                        />
                      ) : null}

                      {currentStep === 6 ? (
                        <PublicReviewStep
                          form={form}
                          errors={errors}
                          requiresServiceAddress={requiresServiceAddress}
                          onEditStep={handleReviewEdit}
                          onConsentChange={(checked) =>
                            updateField("consentAccepted", checked)
                          }
                        />
                      ) : null}
                    </div>

                    <PublicBookingNavigation
                      currentStep={currentStep}
                      isSubmitting={isSubmitting}
                      consentAccepted={form.consentAccepted}
                      onPrevious={handlePreviousStep}
                    />
                  </form>
                </div>
              )}
            </main>
          </div>
        </Container>
      </section>
    </>
  );
}

// =====================================================================
// PUBLIC BOOKING PROGRESS
// =====================================================================

function PublicBookingProgress({
  currentStep,
  onStepSelection,
}: {
  currentStep: number;
  onStepSelection: (stepNumber: number) => void;
}) {
  return (
    <nav
      aria-label="Booking request progress"
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
    >
      <div className="h-1.5 bg-slate-100">
        <div
          className="h-full bg-brand-700 transition-all duration-300"
          style={{ width: `${(currentStep / TOTAL_STEPS) * 100}%` }}
        />
      </div>

      <div className="hidden lg:grid lg:grid-cols-6">
        {BOOKING_PROGRESS_STEPS.map((step) => {
          const isActive = step.number === currentStep;
          const isCompleted = step.number < currentStep;

          return (
            <button
              key={step.number}
              type="button"
              onClick={() => onStepSelection(step.number)}
              disabled={!isCompleted}
              aria-current={isActive ? "step" : undefined}
              className={[
                "flex min-h-24 items-start gap-3 border-r border-slate-100 px-3 py-4 text-left transition last:border-r-0",
                isActive
                  ? "bg-brand-50/70"
                  : isCompleted
                    ? "bg-white hover:bg-slate-50"
                    : "cursor-default bg-slate-50/60",
              ].join(" ")}
            >
              <span
                className={[
                  "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-extrabold [&>svg]:h-4 [&>svg]:w-4",
                  isActive
                    ? "bg-brand-700 text-white"
                    : isCompleted
                      ? "bg-emerald-600 text-white"
                      : "bg-slate-200 text-slate-500",
                ].join(" ")}
              >
                {isCompleted ? <Check /> : step.icon}
              </span>

              <span className="min-w-0">
                <span
                  className={[
                    "block text-[10px] font-extrabold uppercase tracking-wide",
                    isActive
                      ? "text-brand-700"
                      : isCompleted
                        ? "text-emerald-700"
                        : "text-slate-400",
                  ].join(" ")}
                >
                  Step {step.number}
                </span>

                <span className="mt-1 block text-sm font-extrabold text-navy-950">
                  {step.title}
                </span>

                <span className="mt-1 block text-[11px] leading-4 text-slate-500">
                  {step.description}
                </span>
              </span>
            </button>
          );
        })}
      </div>

      <div className="p-4 lg:hidden">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-700 text-sm font-extrabold text-white">
            {currentStep}
          </span>

          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-brand-700">
              Step {currentStep} of {TOTAL_STEPS}
            </p>
            <p className="mt-0.5 font-display text-base font-extrabold text-navy-950">
              {BOOKING_PROGRESS_STEPS[currentStep - 1]?.title}
            </p>
          </div>
        </div>

        <div className="mt-4 flex gap-1.5">
          {BOOKING_PROGRESS_STEPS.map((step) => (
            <button
              key={step.number}
              type="button"
              onClick={() => onStepSelection(step.number)}
              disabled={step.number >= currentStep}
              aria-label={`Go to step ${step.number}: ${step.title}`}
              className={[
                "h-1.5 flex-1 rounded-full transition",
                step.number <= currentStep ? "bg-brand-700" : "bg-slate-200",
              ].join(" ")}
            />
          ))}
        </div>
      </div>
    </nav>
  );
}

// =====================================================================
// STEP 1 — ABOUT YOU
// =====================================================================

function PublicCustomerStep({
  form,
  errors,
  isBusinessBooking,
  onTextChange,
  onBookingTypeChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  isBusinessBooking: boolean;
  onTextChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
  onBookingTypeChange: (value: BookingFor) => void;
}) {
  return (
    <div className="space-y-4">
      <FormSection
        number={1}
        title="Who is this service for?"
        description="Start with your primary contact information. We will use this information for booking updates."
        icon={<UserRound />}
      >
        <fieldset>
          <legend className="sr-only">Booking type</legend>

          <div className="grid gap-3 sm:grid-cols-2">
            <BookingTypeCard
              value="PERSONAL"
              label="Personal"
              description="Technology support for you, your home, or a personal device."
              selected={form.bookingFor === "PERSONAL"}
              onChange={() => onBookingTypeChange("PERSONAL")}
            />

            <BookingTypeCard
              value="BUSINESS"
              label="Business"
              description="Technology support for a company, office, nonprofit, or organization."
              selected={form.bookingFor === "BUSINESS"}
              onChange={() => onBookingTypeChange("BUSINESS")}
            />
          </div>

          {errors.bookingFor ? (
            <FieldError id="bookingFor-error" message={errors.bookingFor} />
          ) : null}
        </fieldset>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <FormField
            label="Full name"
            name="fullName"
            error={errors.fullName}
            required
          >
            <input
              id="fullName"
              name="fullName"
              type="text"
              autoComplete="name"
              maxLength={120}
              placeholder="Your full name"
              value={form.fullName}
              onChange={onTextChange}
              aria-invalid={Boolean(errors.fullName)}
              className={getInputClass(Boolean(errors.fullName))}
            />
          </FormField>

          <FormField
            label="Email address"
            name="email"
            error={errors.email}
            required
          >
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              maxLength={254}
              placeholder="you@email.com"
              value={form.email}
              onChange={onTextChange}
              aria-invalid={Boolean(errors.email)}
              className={getInputClass(Boolean(errors.email))}
            />
          </FormField>

          <FormField
            label="Phone number"
            name="phone"
            error={errors.phone}
            required
          >
            <input
              id="phone"
              name="phone"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              maxLength={40}
              placeholder="(515) 555-1234"
              value={form.phone}
              onChange={onTextChange}
              aria-invalid={Boolean(errors.phone)}
              className={getInputClass(Boolean(errors.phone))}
            />
          </FormField>

          <FormField
            label="Preferred contact method"
            name="preferredContactMethod"
            error={errors.preferredContactMethod}
            required
          >
            <select
              id="preferredContactMethod"
              name="preferredContactMethod"
              value={form.preferredContactMethod}
              onChange={onTextChange}
              aria-invalid={Boolean(errors.preferredContactMethod)}
              className={getInputClass(Boolean(errors.preferredContactMethod))}
            >
              <option value="">Select contact method</option>

              {preferredContactOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </FormField>
        </div>
      </FormSection>

      {isBusinessBooking ? (
        <FormSection
          number={1}
          title="Business information"
          description="Provide the organization information associated with this service request."
          icon={<Building2 />}
        >
          <div className="grid gap-4 md:grid-cols-2">
            <FormField
              label="Business name"
              name="businessName"
              error={errors.businessName}
              required
            >
              <input
                id="businessName"
                name="businessName"
                type="text"
                autoComplete="organization"
                maxLength={180}
                value={form.businessName}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessName))}
              />
            </FormField>

            <FormField
              label="Your role"
              name="businessContactRole"
              error={errors.businessContactRole}
              hint="Optional"
            >
              <input
                id="businessContactRole"
                name="businessContactRole"
                type="text"
                maxLength={120}
                placeholder="Owner, manager, office administrator..."
                value={form.businessContactRole}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessContactRole))}
              />
            </FormField>

            <FormField
              label="Business email"
              name="businessEmail"
              error={errors.businessEmail}
              required
            >
              <input
                id="businessEmail"
                name="businessEmail"
                type="email"
                maxLength={254}
                value={form.businessEmail}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessEmail))}
              />
            </FormField>

            <FormField
              label="Business phone"
              name="businessPhone"
              error={errors.businessPhone}
              required
            >
              <input
                id="businessPhone"
                name="businessPhone"
                type="tel"
                inputMode="tel"
                maxLength={40}
                value={form.businessPhone}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessPhone))}
              />
            </FormField>

            <div className="md:col-span-2">
              <FormField
                label="Business street address"
                name="businessStreetAddress"
                error={errors.businessStreetAddress}
                required
              >
                <input
                  id="businessStreetAddress"
                  name="businessStreetAddress"
                  type="text"
                  maxLength={180}
                  autoComplete="street-address"
                  value={form.businessStreetAddress}
                  onChange={onTextChange}
                  className={getInputClass(
                    Boolean(errors.businessStreetAddress),
                  )}
                />
              </FormField>
            </div>

            <FormField
              label="Business city"
              name="businessCity"
              error={errors.businessCity}
              required
            >
              <input
                id="businessCity"
                name="businessCity"
                type="text"
                maxLength={100}
                value={form.businessCity}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessCity))}
              />
            </FormField>

            <FormField
              label="Business state"
              name="businessState"
              error={errors.businessState}
              required
            >
              <input
                id="businessState"
                name="businessState"
                type="text"
                maxLength={100}
                value={form.businessState}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessState))}
              />
            </FormField>

            <FormField
              label="Business postal code"
              name="businessPostalCode"
              error={errors.businessPostalCode}
              required
            >
              <input
                id="businessPostalCode"
                name="businessPostalCode"
                type="text"
                maxLength={30}
                value={form.businessPostalCode}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessPostalCode))}
              />
            </FormField>

            <FormField
              label="Business country code"
              name="businessCountryCode"
              error={errors.businessCountryCode}
              required
            >
              <input
                id="businessCountryCode"
                name="businessCountryCode"
                type="text"
                maxLength={2}
                placeholder="US"
                value={form.businessCountryCode}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.businessCountryCode))}
              />
            </FormField>
          </div>
        </FormSection>
      ) : null}
    </div>
  );
}

// =====================================================================
// STEP 2 — SERVICE
// =====================================================================

function PublicServiceStep({
  form,
  errors,
  onTextChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  onTextChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
}) {
  return (
    <FormSection
      number={2}
      title="What service do you need?"
      description="Choose the service and the support method that best match your request."
      icon={<Wrench />}
    >
      <div className="grid gap-4 md:grid-cols-2">
        <FormField
          label="Service needed"
          name="serviceType"
          error={errors.serviceType}
          required
        >
          <select
            id="serviceType"
            name="serviceType"
            value={form.serviceType}
            onChange={onTextChange}
            className={getInputClass(Boolean(errors.serviceType))}
          >
            <option value="">Select a service</option>

            {serviceTypeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <fieldset className="mt-4">
        <legend className="text-xs font-extrabold text-slate-800">
          Preferred service method
          <span aria-hidden="true" className="ml-1 text-red-600">
            *
          </span>
        </legend>

        <div className="mt-2 grid gap-3 md:grid-cols-2">
          {serviceMethodOptions.map((option) => {
            const selected = form.serviceMethod === option.value;

            return (
              <label
                key={option.value}
                className={[
                  "cursor-pointer rounded-xl border px-4 py-3 transition",
                  selected
                    ? "border-brand-600 bg-brand-50 shadow-sm"
                    : "border-slate-200 bg-white hover:border-brand-300 hover:bg-slate-50",
                ].join(" ")}
              >
                <span className="flex items-start gap-3">
                  <input
                    name="serviceMethod"
                    type="radio"
                    value={option.value}
                    checked={selected}
                    onChange={onTextChange}
                    className="focus-ring mt-1 h-4 w-4"
                  />

                  <span>
                    <span className="block text-sm font-extrabold text-navy-950">
                      {option.label}
                    </span>

                    <span className="mt-0.5 block text-xs leading-5 text-slate-600">
                      {option.description}
                    </span>
                  </span>
                </span>
              </label>
            );
          })}
        </div>

        {errors.serviceMethod ? (
          <FieldError id="serviceMethod-error" message={errors.serviceMethod} />
        ) : null}
      </fieldset>
    </FormSection>
  );
}

// =====================================================================
// STEP 3 — REQUEST DETAILS
// =====================================================================

function PublicRequestDetailsStep({
  form,
  errors,
  onTextChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  onTextChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
}) {
  return (
    <FormSection
      number={3}
      title="Tell us what is happening"
      description="Give us enough detail to understand the device, symptoms, and work you need."
      icon={<Info />}
    >
      <div className="grid gap-4 md:grid-cols-2">
        <FormField
          label="Device or equipment"
          name="deviceType"
          error={errors.deviceType}
          hint="Optional"
        >
          <input
            id="deviceType"
            name="deviceType"
            type="text"
            maxLength={120}
            placeholder="Laptop, printer, router..."
            value={form.deviceType}
            onChange={onTextChange}
            className={getInputClass(Boolean(errors.deviceType))}
          />
        </FormField>
      </div>

      <div className="mt-4">
        <FormField
          label="Describe the problem or requested service"
          name="problemDescription"
          error={errors.problemDescription}
          required
        >
          <textarea
            id="problemDescription"
            name="problemDescription"
            rows={6}
            maxLength={2000}
            placeholder="Describe the symptoms, error messages, when the problem began, and anything already attempted."
            value={form.problemDescription}
            onChange={onTextChange}
            className={`${getInputClass(
              Boolean(errors.problemDescription),
            )} resize-y`}
          />

          <div className="mt-1.5 flex justify-between gap-4 text-[11px] text-slate-500">
            <span>Minimum 20 characters</span>

            <span>{form.problemDescription.length}/2,000</span>
          </div>
        </FormField>
      </div>
    </FormSection>
  );
}

// =====================================================================
// STEP 4 — SCHEDULE
// =====================================================================

function PublicScheduleStep({
  form,
  errors,
  minimumBookingDate,
  onTextChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  minimumBookingDate: string;
  onTextChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
}) {
  return (
    <FormSection
      number={4}
      title="When would you prefer service?"
      description="Choose your preferred date and time. Availability will be confirmed separately."
      icon={<Clock3 />}
    >
      <div className="grid gap-4 md:grid-cols-3">
        <FormField
          label="Preferred date"
          name="preferredDate"
          error={errors.preferredDate}
          required
        >
          <input
            id="preferredDate"
            name="preferredDate"
            type="date"
            min={minimumBookingDate}
            value={form.preferredDate}
            onChange={onTextChange}
            className={getInputClass(Boolean(errors.preferredDate))}
          />
        </FormField>

        <FormField
          label="Preferred time"
          name="preferredTime"
          error={errors.preferredTime}
          required
        >
          <select
            id="preferredTime"
            name="preferredTime"
            value={form.preferredTime}
            onChange={onTextChange}
            className={getInputClass(Boolean(errors.preferredTime))}
          >
            <option value="">Select a time</option>

            {preferredTimeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField
          label="Alternate date"
          name="alternateDate"
          error={errors.alternateDate}
          hint="Optional"
        >
          <input
            id="alternateDate"
            name="alternateDate"
            type="date"
            min={minimumBookingDate}
            value={form.alternateDate}
            onChange={onTextChange}
            className={getInputClass(Boolean(errors.alternateDate))}
          />
        </FormField>
      </div>
    </FormSection>
  );
}

// =====================================================================
// STEP 5 — LOCATION & ACKNOWLEDGEMENT
// =====================================================================

function PublicLocationStep({
  form,
  errors,
  requiresServiceAddress,
  onTextChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  requiresServiceAddress: boolean;
  onTextChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
}) {
  return (
    <div className="space-y-4">
      <FormSection
        number={5}
        title="Where is service needed?"
        description={
          requiresServiceAddress
            ? "Enter the complete address where on-site service is requested."
            : "Address information is optional unless you selected on-site service."
        }
        icon={<MapPin />}
      >
        {requiresServiceAddress ? (
          <div className="mb-4 flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-900">
            <Info className="mt-0.5 h-4 w-4 shrink-0" />

            <p>
              Street address, city, state, postal code, and country code are
              required for on-site service.
            </p>
          </div>
        ) : null}

        <div className="grid gap-4 md:grid-cols-2">
          <div className="md:col-span-2">
            <FormField
              label="Street address"
              name="streetAddress"
              error={errors.streetAddress}
              required={requiresServiceAddress}
            >
              <input
                id="streetAddress"
                name="streetAddress"
                type="text"
                autoComplete="address-line1"
                maxLength={180}
                placeholder="Street address"
                value={form.streetAddress}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.streetAddress))}
              />
            </FormField>
          </div>

          <div className="md:col-span-2">
            <FormField
              label="Address line 2"
              name="addressLine2"
              error={errors.addressLine2}
              hint="Optional"
            >
              <input
                id="addressLine2"
                name="addressLine2"
                type="text"
                autoComplete="address-line2"
                maxLength={180}
                placeholder="Apartment, suite, unit..."
                value={form.addressLine2}
                onChange={onTextChange}
                className={getInputClass(Boolean(errors.addressLine2))}
              />
            </FormField>
          </div>

          <FormField
            label="City"
            name="city"
            error={errors.city}
            required={requiresServiceAddress}
          >
            <input
              id="city"
              name="city"
              type="text"
              autoComplete="address-level2"
              maxLength={100}
              value={form.city}
              onChange={onTextChange}
              className={getInputClass(Boolean(errors.city))}
            />
          </FormField>

          <FormField
            label="State / region"
            name="stateRegion"
            error={errors.stateRegion}
            required={requiresServiceAddress}
          >
            <input
              id="stateRegion"
              name="stateRegion"
              type="text"
              autoComplete="address-level1"
              maxLength={100}
              value={form.stateRegion}
              onChange={onTextChange}
              className={getInputClass(Boolean(errors.stateRegion))}
            />
          </FormField>

          <FormField
            label="Postal code"
            name="postalCode"
            error={errors.postalCode}
            required={requiresServiceAddress}
          >
            <input
              id="postalCode"
              name="postalCode"
              type="text"
              autoComplete="postal-code"
              maxLength={30}
              placeholder="50309"
              value={form.postalCode}
              onChange={onTextChange}
              className={getInputClass(Boolean(errors.postalCode))}
            />
          </FormField>

          <FormField
            label="Country code"
            name="countryCode"
            error={errors.countryCode}
            required={requiresServiceAddress}
          >
            <input
              id="countryCode"
              name="countryCode"
              type="text"
              maxLength={2}
              placeholder="US"
              value={form.countryCode}
              onChange={onTextChange}
              className={getInputClass(Boolean(errors.countryCode))}
            />
          </FormField>
        </div>
      </FormSection>
    </div>
  );
}

// =====================================================================
// STEP 6 — REVIEW
// =====================================================================

function PublicReviewStep({
  form,
  errors,
  requiresServiceAddress,
  onEditStep,
  onConsentChange,
}: {
  form: BookingFormState;
  errors: BookingFormErrors;
  requiresServiceAddress: boolean;
  onEditStep: (stepNumber: number) => void;
  onConsentChange: (checked: boolean) => void;
}) {
  const serviceLabel =
    serviceTypeOptions.find((option) => option.value === form.serviceType)
      ?.label ?? form.serviceType;

  const serviceMethodLabel =
    serviceMethodOptions.find((option) => option.value === form.serviceMethod)
      ?.label ?? form.serviceMethod;

  const serviceAddress = [
    form.streetAddress,
    form.addressLine2,
    form.city,
    form.stateRegion,
    form.postalCode,
    form.countryCode,
  ]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  const businessAddress = [
    form.businessStreetAddress,
    form.businessCity,
    form.businessState,
    form.businessPostalCode,
    form.businessCountryCode,
  ]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  return (
    <FormSection
      number={6}
      title="Review your booking request"
      description="Review the information below, confirm the acknowledgement, and submit your request. Review details are read-only."
      icon={<ClipboardCheck />}
    >
      <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
        <div className="flex items-start gap-3">
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

          <div>
            <p className="text-sm font-extrabold text-emerald-950">
              Ready to submit
            </p>
            <p className="mt-1 text-sm leading-6 text-emerald-900">
              Use Edit if something needs to change. Submitting sends this
              request to Romelt TechCare for review; it does not confirm an
              appointment.
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <PublicReviewCard
          title="About you"
          icon={<UserRound />}
          step={1}
          onEdit={onEditStep}
        >
          <PublicReviewRow
            label="Request for"
            value={formatEnumLabel(form.bookingFor)}
          />
          <PublicReviewRow label="Full name" value={form.fullName} />
          <PublicReviewRow label="Email" value={form.email} />
          <PublicReviewRow label="Phone" value={form.phone} />
          <PublicReviewRow
            label="Preferred contact"
            value={formatEnumLabel(form.preferredContactMethod)}
          />
        </PublicReviewCard>

        {form.bookingFor === "BUSINESS" ? (
          <PublicReviewCard
            title="Business"
            icon={<Building2 />}
            step={1}
            onEdit={onEditStep}
          >
            <PublicReviewRow label="Business name" value={form.businessName} />
            <PublicReviewRow
              label="Your role"
              value={form.businessContactRole || "Not provided"}
            />
            <PublicReviewRow
              label="Business email"
              value={form.businessEmail}
            />
            <PublicReviewRow
              label="Business phone"
              value={form.businessPhone}
            />
            <PublicReviewRow
              label="Business address"
              value={businessAddress || "Not provided"}
            />
          </PublicReviewCard>
        ) : null}

        <PublicReviewCard
          title="Service"
          icon={<Wrench />}
          step={2}
          onEdit={onEditStep}
        >
          <PublicReviewRow label="Service" value={serviceLabel} />
          <PublicReviewRow
            label="Method"
            value={serviceMethodLabel || "Not provided"}
          />
        </PublicReviewCard>

        <PublicReviewCard
          title="Request details"
          icon={<Info />}
          step={3}
          onEdit={onEditStep}
        >
          <PublicReviewRow
            label="Device or equipment"
            value={form.deviceType || "Not provided"}
          />

          <div className="mt-3">
            <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
              Problem or requested service
            </p>
            <p className="mt-1 whitespace-pre-wrap text-sm font-semibold leading-6 text-slate-800">
              {form.problemDescription}
            </p>
          </div>
        </PublicReviewCard>

        <PublicReviewCard
          title="Schedule"
          icon={<Clock3 />}
          step={4}
          onEdit={onEditStep}
        >
          <PublicReviewRow
            label="Preferred date"
            value={formatDate(form.preferredDate)}
          />
          <PublicReviewRow
            label="Preferred time"
            value={formatEnumLabel(form.preferredTime)}
          />
          <PublicReviewRow
            label="Alternate date"
            value={
              form.alternateDate
                ? formatDate(form.alternateDate)
                : "Not provided"
            }
          />
        </PublicReviewCard>

        <PublicReviewCard
          title="Location"
          icon={<MapPin />}
          step={5}
          onEdit={onEditStep}
        >
          <PublicReviewRow
            label="Service address"
            value={
              serviceAddress ||
              (requiresServiceAddress
                ? "Required address missing"
                : "No service address provided")
            }
          />
        </PublicReviewCard>
      </div>

      <section className="mt-5 rounded-xl border border-brand-200 bg-brand-50/60 p-4 sm:p-5">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

          <div className="min-w-0 flex-1">
            <p className="text-sm font-extrabold text-navy-950">
              Booking acknowledgement
            </p>

            <p className="mt-1 text-xs leading-5 text-slate-600">
              Confirm this acknowledgement after reviewing the information
              above.
            </p>

            <label className="mt-3 flex cursor-pointer items-start gap-3 rounded-lg border border-brand-200 bg-white p-3">
              <input
                name="consentAccepted"
                type="checkbox"
                checked={form.consentAccepted}
                onChange={(event) => onConsentChange(event.target.checked)}
                className="focus-ring mt-0.5 h-5 w-5 shrink-0 rounded border-slate-300 text-brand-700"
              />

              <span className="text-sm leading-6 text-slate-700">
                I confirm that the information provided is accurate. I
                understand this is a service request and the appointment is not
                confirmed until Romelt TechCare contacts me.
              </span>
            </label>

            {errors.consentAccepted ? (
              <FieldError
                id="consentAccepted-error"
                message={errors.consentAccepted}
              />
            ) : null}
          </div>
        </div>
      </section>
    </FormSection>
  );
}

function PublicReviewCard({
  title,
  icon,
  step,
  onEdit,
  children,
}: {
  title: string;
  icon: ReactNode;
  step: number;
  onEdit: (stepNumber: number) => void;
  children: ReactNode;
}) {
  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
      <div className="flex items-center justify-between gap-3 border-b border-slate-200 pb-3">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-brand-700 shadow-sm [&>svg]:h-4 [&>svg]:w-4">
            {icon}
          </div>
          <h3 className="font-display text-sm font-extrabold text-navy-950">
            {title}
          </h3>
        </div>

        <button
          type="button"
          onClick={() => onEdit(step)}
          className="focus-ring rounded-lg px-2 py-1 text-xs font-bold text-brand-700 transition hover:bg-brand-50"
        >
          Edit
        </button>
      </div>

      <dl className="mt-3 space-y-2.5">{children}</dl>
    </article>
  );
}

function PublicReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
        {label}
      </dt>
      <dd className="mt-0.5 break-words text-sm font-semibold leading-5 text-slate-800">
        {value || "Not provided"}
      </dd>
    </div>
  );
}

// =====================================================================
// PUBLIC BOOKING NAVIGATION
// =====================================================================

function PublicBookingNavigation({
  currentStep,
  isSubmitting,
  consentAccepted,
  onPrevious,
}: {
  currentStep: number;
  isSubmitting: boolean;
  consentAccepted: boolean;
  onPrevious: () => void;
}) {
  const isFirstStep = currentStep === 1;
  const isFinalStep = currentStep === TOTAL_STEPS;

  return (
    <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-white px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
      <div>
        {isFirstStep ? (
          <Link
            to="/"
            className="focus-ring inline-flex min-h-10 w-full items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50 sm:w-auto"
          >
            Cancel
          </Link>
        ) : (
          <button
            type="button"
            onClick={onPrevious}
            disabled={isSubmitting}
            className="focus-ring inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:border-brand-400 hover:text-brand-700 disabled:opacity-50 sm:w-auto"
          >
            <ArrowLeft className="h-4 w-4" />
            Previous
          </button>
        )}
      </div>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <span className="hidden text-xs font-semibold text-slate-400 md:block">
          {isFinalStep
            ? "Ready to submit request"
            : `${TOTAL_STEPS - currentStep} ${
                TOTAL_STEPS - currentStep === 1 ? "step" : "steps"
              } remaining`}
        </span>

        <button
          type="submit"
          disabled={isSubmitting || (isFinalStep && !consentAccepted)}
          aria-busy={isSubmitting}
          className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-6 py-2.5 text-sm font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
        >
          {isFinalStep ? (
            <Send className="h-4 w-4" />
          ) : (
            <ArrowRight className="h-4 w-4" />
          )}

          {isSubmitting
            ? "Submitting Request..."
            : isFinalStep
              ? "Submit Booking Request"
              : "Continue"}
        </button>
      </div>
    </div>
  );
}

function getPublicStepDescription(
  step: number,
  form: BookingFormState,
): string {
  switch (step) {
    case 1:
      return form.bookingFor === "BUSINESS"
        ? "Tell us who to contact and provide the business information for this request."
        : "Tell us who to contact about this service request.";
    case 2:
      return "Select the service and support method that best match what you need.";
    case 3:
      return "Describe the device, symptoms, or work you want Romelt TechCare to review.";
    case 4:
      return "Choose the date and time you would prefer. Availability will be confirmed separately.";
    case 5:
      return "Provide the service location when needed.";
    case 6:
      return "Review everything, confirm the booking acknowledgement, and submit your request.";
    default:
      return "Complete your booking request.";
  }
}

// =====================================================================
// HERO
// =====================================================================

function BookingHero() {
  return (
    <section className="bg-navy-950 py-10 text-white sm:py-12">
      <Container>
        <div className="max-w-3xl">
          <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-brand-300">
            Service Booking
          </p>

          <h1 className="mt-2 font-display text-3xl font-black sm:text-4xl">
            Request technology support
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300 sm:text-base">
            Tell us what you need and when you would prefer service. We will
            review your request and contact you before an appointment is
            confirmed.
          </p>
        </div>
      </Container>
    </section>
  );
}

// =====================================================================
// BOOKING TYPE
// =====================================================================

interface BookingTypeCardProps {
  value: BookingFor;
  label: string;
  description: string;
  selected: boolean;
  onChange: () => void;
}

function BookingTypeCard({
  value,
  label,
  description,
  selected,
  onChange,
}: BookingTypeCardProps) {
  return (
    <label
      className={[
        "cursor-pointer rounded-xl border p-4 transition",
        selected
          ? "border-brand-600 bg-brand-50 shadow-sm"
          : "border-slate-200 bg-white hover:border-brand-300",
      ].join(" ")}
    >
      <div className="flex items-start gap-3">
        <input
          type="radio"
          name="bookingFor"
          value={value}
          checked={selected}
          onChange={onChange}
          className="focus-ring mt-1 h-4 w-4"
        />

        <div>
          <p className="text-sm font-extrabold text-navy-950">{label}</p>

          <p className="mt-1 text-xs leading-5 text-slate-600">{description}</p>
        </div>
      </div>
    </label>
  );
}

// =====================================================================
// FORM SECTION
// =====================================================================

interface FormSectionProps {
  number: number;
  title: string;
  description: string;
  icon: ReactNode;
  children: ReactNode;
}

function FormSection({
  number,
  title,
  description,
  icon,
  children,
}: FormSectionProps) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4 sm:p-5">
      <div className="mb-4 flex items-start gap-3 border-b border-slate-100 pb-4">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <div>
          <p className="text-[11px] font-extrabold uppercase tracking-[0.15em] text-brand-700">
            Step {number}
          </p>

          <h2 className="font-display text-lg font-extrabold text-navy-950">
            {title}
          </h2>

          <p className="mt-0.5 text-xs leading-5 text-slate-600">
            {description}
          </p>
        </div>
      </div>

      {children}
    </section>
  );
}

// =====================================================================
// FORM FIELD
// =====================================================================

interface FormFieldProps {
  label: string;
  name: BookingFieldName;
  error?: string;
  hint?: string;
  required?: boolean;
  children: ReactNode;
}

function FormField({
  label,
  name,
  error,
  hint,
  required = false,
  children,
}: FormFieldProps) {
  return (
    <div>
      <label htmlFor={name} className="text-xs font-extrabold text-slate-800">
        {label}

        {required ? (
          <span aria-hidden="true" className="ml-1 text-red-600">
            *
          </span>
        ) : null}

        {hint ? (
          <span className="ml-2 text-[11px] font-medium text-slate-500">
            {hint}
          </span>
        ) : null}
      </label>

      <div className="mt-1.5">{children}</div>

      {error ? <FieldError id={`${name}-error`} message={error} /> : null}
    </div>
  );
}

function FieldError({ id, message }: { id: string; message: string }) {
  return (
    <p id={id} className="mt-1.5 text-sm font-semibold text-red-700">
      {message}
    </p>
  );
}

// =====================================================================
// ALERTS
// =====================================================================

function SubmissionErrorAlert({ error }: { error: SubmissionError }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

      <div>
        <h3 className="text-sm font-bold">{error.title}</h3>

        <p className="mt-0.5 text-sm leading-5">{error.message}</p>
      </div>
    </div>
  );
}

function ValidationAlert() {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-950"
    >
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

      <div>
        <p className="text-sm font-bold">Review the highlighted fields</p>

        <p className="mt-0.5 text-sm leading-5">
          Complete the required information before submitting your booking
          request.
        </p>
      </div>
    </div>
  );
}

// =====================================================================
// SUMMARY
// =====================================================================

function BookingSummaryCard({ form }: { form: BookingFormState }) {
  const serviceLabel =
    serviceTypeOptions.find((option) => option.value === form.serviceType)
      ?.label ?? form.serviceType;

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 p-4">
        <h2 className="font-display text-base font-extrabold text-navy-950">
          Request summary
        </h2>
      </div>

      <dl className="space-y-3 p-4 text-sm">
        <SummaryRow
          label="For"
          value={
            form.bookingFor === "BUSINESS"
              ? form.businessName || "Business"
              : "Personal"
          }
        />

        <SummaryRow label="Service" value={serviceLabel || "Not selected"} />

        <SummaryRow
          label="Method"
          value={formatEnumLabel(form.serviceMethod)}
        />

        <SummaryRow
          label="Preferred date"
          value={formatDate(form.preferredDate)}
        />

        <SummaryRow
          label="Preferred time"
          value={formatEnumLabel(form.preferredTime)}
        />
      </dl>
    </section>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <dt className="text-xs font-semibold text-slate-500">{label}</dt>

      <dd className="max-w-[60%] text-right text-xs font-bold text-slate-800">
        {value}
      </dd>
    </div>
  );
}

// =====================================================================
// AVAILABILITY
// =====================================================================

function AvailabilityCard() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <CalendarCheck2 className="h-5 w-5 text-brand-700" />

        <h2 className="font-display text-base font-extrabold text-navy-950">
          Booking request
        </h2>
      </div>

      <p className="mt-2 text-xs leading-5 text-slate-600">
        Your requested date and time are preferences only. Romelt TechCare will
        confirm availability separately.
      </p>
    </section>
  );
}

// =====================================================================
// SAFETY / CONTACT
// =====================================================================

function SafetyAndContactCard() {
  const hasPhone =
    Boolean(businessPhoneHref) && Boolean(businessConfig.phoneDisplay);

  const hasEmail = Boolean(businessEmailHref) && Boolean(businessConfig.email);

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="p-4">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-5 w-5 text-brand-700" />

          <h2 className="font-display text-base font-extrabold text-navy-950">
            Protect your information
          </h2>
        </div>

        <p className="mt-2 text-xs leading-5 text-slate-600">
          Never include passwords, payment-card details, banking information,
          Social Security numbers, or account recovery codes.
        </p>
      </div>

      <div className="bg-navy-950 p-4 text-white">
        <h3 className="font-display text-base font-extrabold">
          Need help first?
        </h3>

        <div className="mt-3 space-y-2.5">
          {hasPhone ? (
            <a
              href={businessPhoneHref ?? undefined}
              className="focus-ring flex items-center gap-2 text-sm text-slate-200 transition hover:text-white"
            >
              <Phone className="h-4 w-4 shrink-0" />

              <span>{businessConfig.phoneDisplay}</span>
            </a>
          ) : null}

          {hasEmail ? (
            <a
              href={businessEmailHref ?? undefined}
              className="focus-ring flex items-center gap-2 text-sm text-slate-200 transition hover:text-white"
            >
              <Mail className="h-4 w-4 shrink-0" />

              <span className="break-all">{businessConfig.email}</span>
            </a>
          ) : null}
        </div>

        <Link
          to="/contact"
          className="focus-ring mt-4 inline-flex min-h-9 w-full items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-bold text-navy-950 transition hover:bg-brand-50"
        >
          Send a General Message
        </Link>
      </div>
    </section>
  );
}

// =====================================================================
// CONFIRMATION
// =====================================================================

interface BookingConfirmationProps {
  fullName: string;
  email: string;
  serviceType: string;
  serviceMethod: string;
  confirmation: BookingRequestConfirmation;
  isDevelopmentFallback: boolean;
  onReset: () => void;
}

function BookingConfirmation({
  fullName,
  email,
  serviceType,
  serviceMethod,
  confirmation,
  isDevelopmentFallback,
  onReset,
}: BookingConfirmationProps) {
  const serviceLabel =
    serviceTypeOptions.find((option) => option.value === serviceType)?.label ??
    serviceType;

  const methodLabel =
    serviceMethodOptions.find((option) => option.value === serviceMethod)
      ?.label ?? serviceMethod;

  return (
    <section className="overflow-hidden rounded-2xl border border-emerald-200 bg-white shadow-sm">
      <div className="bg-emerald-50 p-6">
        <div className="flex items-start gap-4">
          <CheckCircle2 className="mt-0.5 h-8 w-8 shrink-0 text-emerald-700" />

          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-emerald-700">
              Request received
            </p>

            <h2 className="mt-1 font-display text-2xl font-black text-navy-950">
              Thank you, {fullName}.
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-700">
              {confirmation.message}
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-5 p-6">
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
            Reference number
          </p>

          <p className="mt-1 break-all font-mono text-lg font-black text-navy-950">
            {confirmation.referenceNumber}
          </p>
        </div>

        <dl className="grid gap-4 sm:grid-cols-2">
          <ConfirmationItem
            label="Status"
            value={formatEnumLabel(confirmation.status)}
          />

          <ConfirmationItem
            label="Submitted"
            value={formatSubmittedAt(confirmation.submittedAt)}
          />

          <ConfirmationItem
            label="Requested date"
            value={formatDate(confirmation.requestedDate)}
          />

          <ConfirmationItem
            label="Requested time"
            value={formatEnumLabel(confirmation.requestedTime)}
          />

          <ConfirmationItem label="Service" value={serviceLabel} />

          <ConfirmationItem label="Service method" value={methodLabel} />
        </dl>

        {!isDevelopmentFallback ? (
          <div className="rounded-xl border border-brand-200 bg-brand-50 p-4">
            <div className="flex items-start gap-3">
              <Mail className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

              <div>
                <p className="text-sm font-extrabold text-navy-950">
                  Watch for your booking update
                </p>

                <p className="mt-1 text-sm leading-6 text-slate-700">
                  Romelt TechCare will send booking updates to{" "}
                  <strong>{email}</strong>. Your appointment is not confirmed
                  until you receive a confirmation update.
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            Development mode is active. The request was not transmitted to the
            backend.
          </div>
        )}

        <button
          type="button"
          onClick={onReset}
          className="focus-ring inline-flex min-h-10 items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50"
        >
          Submit another request
        </button>
      </div>
    </section>
  );
}

function ConfirmationItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-bold uppercase tracking-wide text-slate-500">
        {label}
      </dt>

      <dd className="mt-1 text-sm font-bold text-slate-900">{value}</dd>
    </div>
  );
}

// =====================================================================
// REQUEST CREATION
// =====================================================================

function createBookingRequest(form: BookingFormState): BookingRequestPayload {
  const isBusiness = form.bookingFor === "BUSINESS";

  return {
    bookingFor: form.bookingFor,

    fullName: form.fullName.trim(),

    email: form.email.trim().toLowerCase(),

    phone: form.phone.trim(),

    preferredContactMethod: form.preferredContactMethod as ContactMethod,

    notificationEmail: null,

    notificationPhone: null,

    businessName: isBusiness ? normalizeOptionalValue(form.businessName) : null,

    businessEmail: isBusiness
      ? normalizeOptionalEmail(form.businessEmail)
      : null,

    businessPhone: isBusiness
      ? normalizeOptionalValue(form.businessPhone)
      : null,

    businessStreetAddress: isBusiness
      ? normalizeOptionalValue(form.businessStreetAddress)
      : null,

    businessCity: isBusiness ? normalizeOptionalValue(form.businessCity) : null,

    businessState: isBusiness
      ? normalizeOptionalValue(form.businessState)
      : null,

    businessPostalCode: isBusiness
      ? normalizeOptionalValue(form.businessPostalCode)
      : null,

    businessCountryCode: isBusiness
      ? normalizeCountryCode(form.businessCountryCode)
      : null,

    businessContactRole: isBusiness
      ? normalizeOptionalValue(form.businessContactRole)
      : null,

    serviceType: form.serviceType.trim(),

    serviceMethod: form.serviceMethod as ServiceMethod,

    preferredDate: form.preferredDate,

    preferredTime: form.preferredTime as PreferredTime,

    alternateDate: normalizeOptionalValue(form.alternateDate),

    deviceType: normalizeOptionalValue(form.deviceType),

    problemDescription: form.problemDescription.trim(),

    streetAddress: normalizeOptionalValue(form.streetAddress),

    addressLine2: normalizeOptionalValue(form.addressLine2),

    city: normalizeOptionalValue(form.city),

    stateRegion: normalizeOptionalValue(form.stateRegion),

    postalCode: normalizeOptionalValue(form.postalCode),

    countryCode: normalizeCountryCode(form.countryCode),

    consentAccepted: form.consentAccepted,
  };
}

// =====================================================================
// DEVELOPMENT CONFIRMATION
// =====================================================================

function createDevelopmentConfirmation(
  request: BookingRequestPayload,
): BookingRequestConfirmation {
  const now = new Date();

  return {
    bookingRequestId: undefined,

    referenceNumber: `DEV-BOOKING-${now.getTime()}`,

    message:
      "Your booking form passed validation successfully. Enable the booking API to transmit requests to the backend.",

    submittedAt: now.toISOString(),

    requestedDate: request.preferredDate,

    requestedTime: request.preferredTime,

    status: "PENDING",
  };
}

function normalizeConfirmation(
  confirmation: BookingRequestConfirmation,
  request: BookingRequestPayload,
): BookingRequestConfirmation {
  return {
    bookingRequestId: confirmation.bookingRequestId,

    referenceNumber:
      confirmation.referenceNumber?.trim() || "REFERENCE-PENDING",

    message:
      confirmation.message?.trim() ||
      "Your booking request was received successfully.",

    submittedAt: confirmation.submittedAt || new Date().toISOString(),

    requestedDate: confirmation.requestedDate || request.preferredDate,

    requestedTime: confirmation.requestedTime || request.preferredTime,

    status: confirmation.status || "PENDING",
  };
}

// =====================================================================
// VALIDATION
// =====================================================================

function validateBookingForm(form: BookingFormState): BookingFormErrors {
  const errors: BookingFormErrors = {};

  if (form.bookingFor !== "PERSONAL" && form.bookingFor !== "BUSINESS") {
    errors.bookingFor =
      "Select whether this service is personal or business-related.";
  }

  if (form.fullName.trim().length < 2) {
    errors.fullName = "Enter your full name.";
  } else if (form.fullName.trim().length > 120) {
    errors.fullName = "Full name cannot exceed 120 characters.";
  }

  if (!isValidEmail(form.email)) {
    errors.email = "Enter a valid email address.";
  }

  if (!isValidRequiredPhone(form.phone)) {
    errors.phone = "Enter a valid telephone number.";
  }

  if (!isContactMethod(form.preferredContactMethod)) {
    errors.preferredContactMethod = "Select how you prefer to be contacted.";
  }

  if (form.notificationEmail.trim() && !isValidEmail(form.notificationEmail)) {
    errors.notificationEmail = "Enter a valid notification email address.";
  }

  if (
    form.notificationPhone.trim() &&
    !isValidRequiredPhone(form.notificationPhone)
  ) {
    errors.notificationPhone = "Enter a valid notification telephone number.";
  }

  if (
    form.preferredContactMethod === "EMAIL" &&
    !form.email.trim() &&
    !form.notificationEmail.trim()
  ) {
    errors.notificationEmail =
      "An email address is required when email is the preferred contact method.";
  }

  if (
    (form.preferredContactMethod === "PHONE" ||
      form.preferredContactMethod === "TEXT") &&
    !form.phone.trim() &&
    !form.notificationPhone.trim()
  ) {
    errors.notificationPhone =
      "A telephone number is required for phone or text contact.";
  }

  if (form.bookingFor === "BUSINESS") {
    if (!form.businessName.trim()) {
      errors.businessName = "Enter the business name.";
    }

    if (!isValidEmail(form.businessEmail)) {
      errors.businessEmail = "Enter a valid business email address.";
    }

    if (!isValidRequiredPhone(form.businessPhone)) {
      errors.businessPhone = "Enter a valid business telephone number.";
    }

    if (!form.businessStreetAddress.trim()) {
      errors.businessStreetAddress = "Enter the business street address.";
    }

    if (!form.businessCity.trim()) {
      errors.businessCity = "Enter the business city.";
    }

    if (!form.businessState.trim()) {
      errors.businessState = "Enter the business state.";
    }

    if (!form.businessPostalCode.trim()) {
      errors.businessPostalCode = "Enter the business postal code.";
    }

    if (!isValidCountryCode(form.businessCountryCode)) {
      errors.businessCountryCode =
        "Country code must contain exactly two letters.";
    }
  }

  if (!form.serviceType.trim()) {
    errors.serviceType = "Select the service you need.";
  }

  if (!isServiceMethod(form.serviceMethod)) {
    errors.serviceMethod = "Select a preferred service method.";
  }

  if (!form.preferredDate) {
    errors.preferredDate = "Select your preferred service date.";
  } else if (isDateBeforeMinimum(form.preferredDate)) {
    errors.preferredDate = "Preferred date must be a future date.";
  }

  if (!isPreferredTime(form.preferredTime)) {
    errors.preferredTime = "Select your preferred service time.";
  }

  if (form.alternateDate && isDateBeforeMinimum(form.alternateDate)) {
    errors.alternateDate = "Alternate date must be a future date.";
  }

  if (form.alternateDate && form.alternateDate === form.preferredDate) {
    errors.alternateDate =
      "Alternate date must be different from the preferred date.";
  }

  if (form.deviceType.trim().length > 120) {
    errors.deviceType = "Device type cannot exceed 120 characters.";
  }

  if (form.problemDescription.trim().length < 20) {
    errors.problemDescription =
      "Provide at least 20 characters describing the requested service.";
  } else if (form.problemDescription.trim().length > 2000) {
    errors.problemDescription = "Description cannot exceed 2,000 characters.";
  }

  if (form.addressLine2.trim().length > 180) {
    errors.addressLine2 = "Address line 2 cannot exceed 180 characters.";
  }

  if (form.serviceMethod === "ON_SITE") {
    if (!form.streetAddress.trim()) {
      errors.streetAddress = "Enter the on-site service street address.";
    }

    if (!form.city.trim()) {
      errors.city = "Enter the service city.";
    }

    if (!form.stateRegion.trim()) {
      errors.stateRegion = "Enter the service state or region.";
    }

    if (!form.postalCode.trim()) {
      errors.postalCode = "Enter the service postal code.";
    }

    if (!isValidCountryCode(form.countryCode)) {
      errors.countryCode = "Country code must contain exactly two letters.";
    }
  } else if (form.countryCode.trim() && !isValidCountryCode(form.countryCode)) {
    errors.countryCode = "Country code must contain exactly two letters.";
  }

  if (!form.consentAccepted) {
    errors.consentAccepted =
      "Confirm that you understand this is a booking request.";
  }

  return errors;
}

// =====================================================================
// STEP VALIDATION
// =====================================================================

function validateBookingStep(
  step: number,
  form: BookingFormState,
): BookingFormErrors {
  const allErrors = validateBookingForm(form);
  const allowedFields = getBookingStepFields(step, form);
  const stepErrors: BookingFormErrors = {};

  allowedFields.forEach((field) => {
    if (allErrors[field]) {
      stepErrors[field] = allErrors[field];
    }
  });

  return stepErrors;
}

function getBookingStepFields(
  step: number,
  form: BookingFormState,
): BookingFieldName[] {
  switch (step) {
    case 1:
      return [
        "bookingFor",
        "fullName",
        "email",
        "phone",
        "preferredContactMethod",
        ...(form.bookingFor === "BUSINESS"
          ? ([
              "businessName",
              "businessEmail",
              "businessPhone",
              "businessStreetAddress",
              "businessCity",
              "businessState",
              "businessPostalCode",
              "businessCountryCode",
              "businessContactRole",
            ] as BookingFieldName[])
          : []),
      ];

    case 2:
      return ["serviceType", "serviceMethod"];

    case 3:
      return ["deviceType", "problemDescription"];

    case 4:
      return ["preferredDate", "preferredTime", "alternateDate"];

    case 5:
      return [
        "streetAddress",
        "addressLine2",
        "city",
        "stateRegion",
        "postalCode",
        "countryCode",
      ];

    case 6:
      return ["consentAccepted"];

    default:
      return [];
  }
}

function resolveBookingStepFromErrors(
  errors: BookingFormErrors,
  form: BookingFormState,
): number {
  for (let step = 1; step <= 6; step += 1) {
    const fields = getBookingStepFields(step, form);

    if (fields.some((field) => Boolean(errors[field]))) {
      return step;
    }
  }

  return 6;
}

// =====================================================================
// BACKEND FIELD ERRORS
// =====================================================================

function extractBackendFieldErrors(error: ApiError): BookingFormErrors {
  const result: BookingFormErrors = {};

  const fields: BookingFieldName[] = [
    "bookingFor",

    "fullName",
    "email",
    "phone",
    "preferredContactMethod",

    "notificationEmail",
    "notificationPhone",

    "businessName",
    "businessEmail",
    "businessPhone",
    "businessStreetAddress",
    "businessCity",
    "businessState",
    "businessPostalCode",
    "businessCountryCode",
    "businessContactRole",

    "serviceType",
    "serviceMethod",
    "preferredDate",
    "preferredTime",
    "alternateDate",
    "deviceType",
    "problemDescription",

    "streetAddress",
    "addressLine2",
    "city",
    "stateRegion",
    "postalCode",
    "countryCode",

    "consentAccepted",
  ];

  fields.forEach((field) => {
    const message = error.getFieldMessage(field);

    if (message) {
      result[field] = message;
    }
  });

  return result;
}

function focusFirstInvalidField(errors: BookingFormErrors) {
  window.requestAnimationFrame(() => {
    const firstField = Object.keys(errors)[0];

    if (!firstField) {
      return;
    }

    document.querySelector<HTMLElement>(`[name="${firstField}"]`)?.focus();
  });
}

function getApiErrorTitle(error: ApiError): string {
  if (error.isNetworkError) {
    return "Unable to connect";
  }

  if (error.status === 409) {
    return "Booking request conflict";
  }

  if (error.status === 429) {
    return "Too many requests";
  }

  if (error.isServerError) {
    return "Service temporarily unavailable";
  }

  return "Booking request could not be sent";
}

// =====================================================================
// NORMALIZATION
// =====================================================================

function normalizeOptionalValue(value: string): string | null {
  const normalized = value.trim();

  return normalized || null;
}

function normalizeOptionalEmail(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized.toLowerCase() : null;
}

function normalizeCountryCode(value: string): string | null {
  const normalized = value.trim().toUpperCase();

  return normalized || null;
}

// =====================================================================
// VALIDATION HELPERS
// =====================================================================

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function isValidRequiredPhone(value: string): boolean {
  if (!/^[0-9+()\-\.\s]{10,40}$/.test(value.trim())) {
    return false;
  }

  const digits = value.replace(/\D/g, "");

  return digits.length >= 10 && digits.length <= 15;
}

function isValidCountryCode(value: string): boolean {
  return /^[A-Za-z]{2}$/.test(value.trim());
}

function isServiceMethod(value: string): value is ServiceMethod {
  return (
    value === "REMOTE" ||
    value === "ON_SITE" ||
    value === "DROP_OFF" ||
    value === "NOT_SURE"
  );
}

function isPreferredTime(value: string): value is PreferredTime {
  return (
    value === "MORNING" ||
    value === "AFTERNOON" ||
    value === "EVENING" ||
    value === "FLEXIBLE"
  );
}

function isContactMethod(value: string): value is ContactMethod {
  return value === "EMAIL" || value === "PHONE" || value === "TEXT";
}

// =====================================================================
// DATE HELPERS
// =====================================================================

function getMinimumBookingDate(): string {
  const date = new Date();

  date.setDate(date.getDate() + 1);

  return formatDateInputValue(date);
}

function formatDateInputValue(date: Date): string {
  const year = date.getFullYear();

  const month = String(date.getMonth() + 1).padStart(2, "0");

  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function isDateBeforeMinimum(value: string): boolean {
  return value < getMinimumBookingDate();
}

function formatDate(value?: string): string {
  if (!value) {
    return "Not provided";
  }

  const date = new Date(`${value}T12:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "long",
  }).format(date);
}

function formatSubmittedAt(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatEnumLabel(value?: string): string {
  if (!value) {
    return "Not provided";
  }

  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

// =====================================================================
// UI HELPERS
// =====================================================================

function scrollToBookingForm() {
  window.requestAnimationFrame(() => {
    document.getElementById("public-booking-form")?.scrollIntoView({
      behavior: prefersReducedMotion() ? "auto" : "smooth",
      block: "start",
    });
  });
}

function scrollToPageTop() {
  window.scrollTo({
    top: 0,
    left: 0,
    behavior: prefersReducedMotion() ? "auto" : "smooth",
  });
}

function prefersReducedMotion(): boolean {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function getInputClass(hasError: boolean): string {
  const base =
    "focus-ring min-h-10 w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 outline-none transition placeholder:text-slate-400";

  return hasError
    ? `${base} border-red-400 focus:border-red-600`
    : `${base} border-slate-300 focus:border-brand-600`;
}
