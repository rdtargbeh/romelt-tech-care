/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CREATE CUSTOMER PAGE
 * ================================================================
 *
 * Purpose:
 * Provides a guided six-step workflow for authenticated
 * administrators to create reusable Romelt TechCare customer profiles.
 *
 * Responsibilities:
 * - Guides the administrator through customer creation in six steps.
 * - Collects customer identity information.
 * - Requires at least one reusable contact method: email or phone.
 * - Collects preferred communication method and customer source.
 * - Collects optional reusable address information.
 * - Records communication restrictions and marketing preferences.
 * - Supports communication and private administrator notes.
 * - Validates each step before allowing forward navigation.
 * - Presents a final customer review before submission.
 * - Sends the completed record to the real customer backend.
 * - Redirects to the newly created customer detail page.
 *
 * Real-data integration:
 *
 * POST /api/v1/admin/customers
 *
 * Navigation:
 *
 * Success:
 * /admin/customers/{customerId}
 *
 * Cancel:
 * /admin/customers
 *
 * Important:
 * This page creates the reusable Customer profile only.
 *
 * Business-specific information belongs to BookingRequest snapshots,
 * not the reusable Customer record.
 * ================================================================
 */

import {
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  LoaderCircle,
  Mail,
  MapPin,
  MessageSquareText,
  Phone,
  Save,
  ShieldCheck,
  UserRound,
} from "lucide-react";

import {
  useState,
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
} from "react";

import { Link, useNavigate } from "react-router-dom";

import { createAdminCustomer } from "@/services/admin-customer.service";

import type {
  AdminCustomerCreatePayload,
  CustomerContactMethod,
  CustomerSource,
} from "@/types/admin-customer.types";

// =====================================================================
// STEP CONFIGURATION
// =====================================================================

const TOTAL_STEPS = 6;

interface CustomerStep {
  number: number;
  title: string;
  shortTitle: string;
  description: string;
}

const CUSTOMER_STEPS: CustomerStep[] = [
  {
    number: 1,
    title: "Customer identity",
    shortTitle: "Identity",
    description: "Enter the customer's identifying information.",
  },
  {
    number: 2,
    title: "Contact information",
    shortTitle: "Contact",
    description: "Add the customer's reusable contact methods and source.",
  },
  {
    number: 3,
    title: "Customer address",
    shortTitle: "Address",
    description: "Add the customer's reusable address information.",
  },
  {
    number: 4,
    title: "Communication preferences",
    shortTitle: "Preferences",
    description: "Record consent and communication restrictions.",
  },
  {
    number: 5,
    title: "Customer notes",
    shortTitle: "Notes",
    description: "Add optional communication and internal notes.",
  },
  {
    number: 6,
    title: "Review customer",
    shortTitle: "Review",
    description: "Review the completed customer profile before creation.",
  },
];

// =====================================================================
// FORM STATE
// =====================================================================

interface CustomerFormState {
  firstName: string;

  lastName: string;

  preferredName: string;

  displayName: string;

  primaryEmail: string;

  primaryPhone: string;

  preferredContactMethod: CustomerContactMethod | "";

  streetAddress: string;

  addressLine2: string;

  city: string;

  stateRegion: string;

  postalCode: string;

  countryCode: string;

  customerSource: CustomerSource;

  marketingConsent: boolean;

  marketingConsentSource: string;

  doNotEmail: boolean;

  doNotCall: boolean;

  doNotText: boolean;

  communicationNotes: string;

  internalNotes: string;
}

// =====================================================================
// INITIAL STATE
// =====================================================================

const INITIAL_FORM: CustomerFormState = {
  firstName: "",

  lastName: "",

  preferredName: "",

  displayName: "",

  primaryEmail: "",

  primaryPhone: "",

  preferredContactMethod: "",

  streetAddress: "",

  addressLine2: "",

  city: "",

  stateRegion: "",

  postalCode: "",

  countryCode: "US",

  customerSource: "ADMIN_CREATED",

  marketingConsent: false,

  marketingConsentSource: "",

  doNotEmail: false,

  doNotCall: false,

  doNotText: false,

  communicationNotes: "",

  internalNotes: "",
};

// =====================================================================
// SOURCE OPTIONS
// =====================================================================

const CUSTOMER_SOURCE_OPTIONS: Array<{
  value: CustomerSource;
  label: string;
}> = [
  {
    value: "ADMIN_CREATED",
    label: "Administrator created",
  },
  {
    value: "REFERRAL",
    label: "Referral",
  },
  {
    value: "CONTACT_INQUIRY",
    label: "Contact inquiry",
  },
  {
    value: "BOOKING",
    label: "Booking",
  },
  {
    value: "REVIEW",
    label: "Review",
  },
  {
    value: "IMPORT",
    label: "Imported",
  },
  {
    value: "OTHER",
    label: "Other",
  },
];

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCreateCustomerPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState<CustomerFormState>(INITIAL_FORM);

  const [currentStep, setCurrentStep] = useState(1);

  const [highestCompletedStep, setHighestCompletedStep] = useState(0);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // ===================================================================
  // CHANGE
  // ===================================================================

  function handleChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) {
    const { name, value, type } = event.target;

    const checked =
      event.target instanceof HTMLInputElement &&
      event.target.type === "checkbox"
        ? event.target.checked
        : false;

    setForm((current) => ({
      ...current,

      [name]: type === "checkbox" ? checked : value,
    }));

    setErrorMessage(null);
  }

  // ===================================================================
  // NAME ASSISTANCE
  // ===================================================================

  function buildSuggestedDisplayName(current: CustomerFormState): string {
    return [current.firstName.trim(), current.lastName.trim()]
      .filter(Boolean)
      .join(" ");
  }

  function handleIdentityChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setForm((current) => {
      const updated = {
        ...current,

        [name]: value,
      };

      /*
       * Only auto-generate displayName while the administrator has
       * not deliberately entered a different display name.
       */
      const previousSuggestedName = buildSuggestedDisplayName(current);

      const displayNameWasAutomatic =
        !current.displayName.trim() ||
        current.displayName.trim() === previousSuggestedName;

      if (displayNameWasAutomatic) {
        updated.displayName = buildSuggestedDisplayName(updated);
      }

      return updated;
    });

    setErrorMessage(null);
  }

  // ===================================================================
  // STEP NAVIGATION
  // ===================================================================

  function handleNextStep() {
    const validationMessage = validateStep(currentStep, form);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      scrollToStepTop();

      return;
    }

    setErrorMessage(null);

    setHighestCompletedStep((current) => Math.max(current, currentStep));

    setCurrentStep((current) => Math.min(TOTAL_STEPS, current + 1));

    scrollToStepTop();
  }

  function handlePreviousStep() {
    setErrorMessage(null);

    setCurrentStep((current) => Math.max(1, current - 1));

    scrollToStepTop();
  }

  function handleStepSelect(stepNumber: number) {
    if (stepNumber === currentStep) {
      return;
    }

    /*
     * Administrators may always return to earlier steps.
     *
     * Forward step selection is only allowed when the immediately
     * preceding steps have already been successfully completed.
     */
    if (stepNumber > highestCompletedStep + 1) {
      return;
    }

    if (stepNumber > currentStep) {
      const validationMessage = validateStep(currentStep, form);

      if (validationMessage) {
        setErrorMessage(validationMessage);

        scrollToStepTop();

        return;
      }

      setHighestCompletedStep((current) => Math.max(current, currentStep));
    }

    setErrorMessage(null);

    setCurrentStep(stepNumber);

    scrollToStepTop();
  }

  // ===================================================================
  // SUBMIT
  // ===================================================================

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    /*
     * Only step five performs the actual backend submission.
     *
     * Pressing Enter on earlier steps behaves like Next.
     */
    if (currentStep < TOTAL_STEPS) {
      handleNextStep();

      return;
    }

    setErrorMessage(null);

    const validationMessage = validateCustomerForm(form);

    if (validationMessage) {
      const invalidStep = resolveInvalidStep(form);

      setCurrentStep(invalidStep);

      setErrorMessage(validationMessage);

      scrollToStepTop();

      return;
    }

    const payload: AdminCustomerCreatePayload = {
      firstName: toNullable(form.firstName),

      lastName: toNullable(form.lastName),

      preferredName: toNullable(form.preferredName),

      displayName: form.displayName.trim(),

      primaryEmail: toNullable(form.primaryEmail),

      primaryPhone: toNullable(form.primaryPhone),

      preferredContactMethod: form.preferredContactMethod || null,

      streetAddress: toNullable(form.streetAddress),

      addressLine2: toNullable(form.addressLine2),

      city: toNullable(form.city),

      stateRegion: toNullable(form.stateRegion),

      postalCode: toNullable(form.postalCode),

      countryCode: toNullable(form.countryCode),

      customerSource: form.customerSource,

      marketingConsent: form.marketingConsent,

      marketingConsentSource: form.marketingConsent
        ? toNullable(form.marketingConsentSource)
        : null,

      doNotEmail: form.doNotEmail,

      doNotCall: form.doNotCall,

      doNotText: form.doNotText,

      communicationNotes: toNullable(form.communicationNotes),

      internalNotes: toNullable(form.internalNotes),
    };

    setIsSubmitting(true);

    try {
      const customer = await createAdminCustomer(payload);

      navigate(`/admin/customers/${customer.customerId}`, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The customer could not be created.",
      );

      scrollToStepTop();
    } finally {
      setIsSubmitting(false);
    }
  }

  // ===================================================================
  // CURRENT STEP
  // ===================================================================

  const currentStepInfo = CUSTOMER_STEPS[currentStep - 1];

  const progressPercentage = (currentStep / TOTAL_STEPS) * 100;

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="mx-auto w-full max-w-6xl space-y-5">
      {/* =============================================================
       * BACK
       * ============================================================= */}

      <Link
        to="/admin/customers"
        className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to customers
      </Link>

      {/* =============================================================
       * HEADER
       * ============================================================= */}

      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer management
          </p>

          <h1 className="mt-1 font-display text-2xl font-black text-navy-950 sm:text-3xl">
            Create Customer
          </h1>

          <p className="mt-1.5 max-w-2xl text-sm leading-6 text-slate-600">
            Create a reusable customer profile through a guided five-step
            process.
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
      </header>

      {/* =============================================================
       * PROGRESS
       * ============================================================= */}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {/* ===========================================================
         * PROGRESS BAR
         * =========================================================== */}

        <div className="h-1.5 bg-slate-100">
          <div
            className="h-full bg-brand-700 transition-all duration-300"
            style={{
              width: `${progressPercentage}%`,
            }}
          />
        </div>

        {/* ===========================================================
         * DESKTOP STEPS
         * =========================================================== */}

        <div className="hidden grid-cols-6 lg:grid">
          {CUSTOMER_STEPS.map((step) => {
            const isCurrent = step.number === currentStep;

            const isCompleted =
              step.number < currentStep || step.number <= highestCompletedStep;

            const isAccessible = step.number <= highestCompletedStep + 1;

            return (
              <button
                key={step.number}
                type="button"
                onClick={() => handleStepSelect(step.number)}
                disabled={!isAccessible}
                className={[
                  "relative flex min-h-24 items-start gap-3 border-r border-slate-100 px-4 py-4 text-left transition last:border-r-0",
                  isCurrent
                    ? "bg-brand-50/70"
                    : isAccessible
                      ? "bg-white hover:bg-slate-50"
                      : "cursor-not-allowed bg-slate-50/60 opacity-60",
                ].join(" ")}
              >
                <StepNumber
                  number={step.number}
                  current={isCurrent}
                  completed={isCompleted && !isCurrent}
                />

                <div className="min-w-0">
                  <p
                    className={[
                      "text-xs font-extrabold uppercase tracking-wide",
                      isCurrent ? "text-brand-700" : "text-slate-400",
                    ].join(" ")}
                  >
                    Step {step.number}
                  </p>

                  <p className="mt-1 text-sm font-extrabold text-slate-900">
                    {step.shortTitle}
                  </p>
                </div>
              </button>
            );
          })}
        </div>

        {/* ===========================================================
         * MOBILE / TABLET STEP HEADER
         * =========================================================== */}

        <div className="p-4 lg:hidden">
          <div className="flex items-center gap-3">
            <StepNumber number={currentStep} current completed={false} />

            <div>
              <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-brand-700">
                Step {currentStep} of {TOTAL_STEPS}
              </p>

              <p className="mt-0.5 font-display text-lg font-extrabold text-navy-950">
                {currentStepInfo.title}
              </p>
            </div>
          </div>

          <div className="mt-4 flex gap-1.5">
            {CUSTOMER_STEPS.map((step) => (
              <button
                key={step.number}
                type="button"
                onClick={() => handleStepSelect(step.number)}
                disabled={step.number > highestCompletedStep + 1}
                className={[
                  "h-1.5 flex-1 rounded-full transition",
                  step.number <= currentStep ? "bg-brand-700" : "bg-slate-200",
                ].join(" ")}
                aria-label={`Go to step ${step.number}: ${step.shortTitle}`}
              />
            ))}
          </div>
        </div>
      </div>

      {/* =============================================================
       * ERROR
       * ============================================================= */}

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

          <div>
            <p className="text-sm font-bold">Check this step</p>

            <p className="mt-1 text-sm leading-6">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * FORM
       * ============================================================= */}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* ===========================================================
         * STEP PANEL
         * =========================================================== */}

        <div
          id="customer-form-step"
          className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
        >
          {/* =========================================================
           * STEP HEADER
           * ========================================================= */}

          <header className="flex items-start gap-3 border-b border-slate-100 bg-slate-50/60 px-4 py-4 sm:px-6">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
              {resolveStepIcon(currentStep)}
            </div>

            <div>
              <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-brand-700">
                Step {currentStep} of {TOTAL_STEPS}
              </p>

              <h2 className="mt-0.5 font-display text-lg font-extrabold text-navy-950 sm:text-xl">
                {currentStepInfo.title}
              </h2>

              <p className="mt-1 text-sm leading-5 text-slate-500">
                {currentStepInfo.description}
              </p>
            </div>
          </header>

          {/* =========================================================
           * STEP CONTENT
           * ========================================================= */}

          <div className="p-4 sm:p-6">
            {currentStep === 1 ? (
              <IdentityStep
                form={form}
                onChange={handleChange}
                onIdentityChange={handleIdentityChange}
              />
            ) : null}

            {currentStep === 2 ? (
              <ContactStep form={form} onChange={handleChange} />
            ) : null}

            {currentStep === 3 ? (
              <AddressStep form={form} onChange={handleChange} />
            ) : null}

            {currentStep === 4 ? (
              <PreferencesStep form={form} onChange={handleChange} />
            ) : null}

            {currentStep === 5 ? (
              <NotesStep form={form} onChange={handleChange} />
            ) : null}

            {currentStep === 6 ? (
              <ReviewStep form={form} onEditStep={handleStepSelect} />
            ) : null}
          </div>
        </div>

        {/* ===========================================================
         * ACTION BAR
         * =========================================================== */}

        <div className="sticky bottom-3 z-10 rounded-2xl border border-slate-200 bg-white/95 p-3 shadow-lg backdrop-blur sm:p-4">
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              {currentStep > 1 ? (
                <button
                  type="button"
                  onClick={handlePreviousStep}
                  disabled={isSubmitting}
                  className="focus-ring inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50 sm:w-auto"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Previous
                </button>
              ) : (
                <Link
                  to="/admin/customers"
                  className="focus-ring inline-flex min-h-10 w-full items-center justify-center rounded-xl border border-slate-300 bg-white px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 sm:w-auto"
                >
                  Cancel
                </Link>
              )}
            </div>

            <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
              <span className="hidden text-xs font-semibold text-slate-400 md:block">
                {currentStep < TOTAL_STEPS
                  ? `${TOTAL_STEPS - currentStep} ${
                      TOTAL_STEPS - currentStep === 1 ? "step" : "steps"
                    } remaining`
                  : "Ready to create customer"}
              </span>

              {currentStep < TOTAL_STEPS ? (
                <button
                  type="button"
                  onClick={handleNextStep}
                  className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 text-sm font-bold text-white transition hover:bg-brand-800"
                >
                  Continue
                  <ArrowRight className="h-4 w-4" />
                </button>
              ) : (
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 text-sm font-bold text-white transition hover:bg-navy-900 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? (
                    <LoaderCircle
                      className="h-4 w-4 animate-spin"
                      aria-hidden="true"
                    />
                  ) : (
                    <Save className="h-4 w-4" aria-hidden="true" />
                  )}

                  {isSubmitting ? "Creating customer..." : "Create customer"}
                </button>
              )}
            </div>
          </div>
        </div>
      </form>
    </section>
  );
}

// =====================================================================
// STEP 1 — IDENTITY
// =====================================================================

interface StepProps {
  form: CustomerFormState;

  onChange: (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => void;
}

interface IdentityStepProps extends StepProps {
  onIdentityChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

function IdentityStep({ form, onChange, onIdentityChange }: IdentityStepProps) {
  return (
    <div className="space-y-5">
      <StepIntro
        title="Who is this customer?"
        description="Use the customer's real identifying information. The display name is what administrators will see throughout the portal."
      />

      <FormGrid>
        <TextField
          label="First name"
          name="firstName"
          value={form.firstName}
          onChange={onIdentityChange}
          autoComplete="given-name"
          maxLength={100}
        />

        <TextField
          label="Last name"
          name="lastName"
          value={form.lastName}
          onChange={onIdentityChange}
          autoComplete="family-name"
          maxLength={100}
        />

        <TextField
          label="Preferred name"
          name="preferredName"
          value={form.preferredName}
          onChange={onChange}
          maxLength={100}
          hint="Optional name the customer prefers to be called."
        />

        <TextField
          label="Display name"
          name="displayName"
          value={form.displayName}
          onChange={onChange}
          required
          maxLength={180}
          hint="Required. This name appears throughout the administrator portal."
        />
      </FormGrid>

      {form.displayName.trim() ? (
        <div className="rounded-xl border border-brand-100 bg-brand-50/50 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-brand-700 shadow-sm">
              <UserRound className="h-4 w-4" />
            </div>

            <div>
              <p className="text-[11px] font-bold uppercase tracking-wide text-brand-700">
                Customer preview
              </p>

              <p className="mt-0.5 font-display text-base font-extrabold text-navy-950">
                {form.displayName}
              </p>

              {form.preferredName.trim() ? (
                <p className="mt-0.5 text-xs text-slate-500">
                  Preferred: {form.preferredName}
                </p>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

// =====================================================================
// STEP 2 — CONTACT
// =====================================================================

function ContactStep({ form, onChange }: StepProps) {
  return (
    <div className="space-y-5">
      <StepIntro
        title="How can Romelt TechCare reach this customer?"
        description="At least one contact method is required. Select a preferred method only when the corresponding email address or phone number is available."
      />

      <FormGrid>
        <TextField
          label="Email address"
          name="primaryEmail"
          value={form.primaryEmail}
          onChange={onChange}
          type="email"
          autoComplete="email"
          maxLength={254}
          icon={<Mail />}
        />

        <TextField
          label="Phone number"
          name="primaryPhone"
          value={form.primaryPhone}
          onChange={onChange}
          type="tel"
          autoComplete="tel"
          maxLength={40}
          icon={<Phone />}
        />

        <SelectField
          label="Preferred contact method"
          name="preferredContactMethod"
          value={form.preferredContactMethod}
          onChange={onChange}
        >
          <option value="">No preference</option>

          <option value="EMAIL">Email</option>

          <option value="PHONE">Phone</option>

          <option value="TEXT">Text message</option>
        </SelectField>

        <SelectField
          label="Customer source"
          name="customerSource"
          value={form.customerSource}
          onChange={onChange}
          required
        >
          {CUSTOMER_SOURCE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </SelectField>
      </FormGrid>

      <div className="grid gap-3 sm:grid-cols-2">
        <ContactPreviewCard
          icon={<Mail />}
          title="Email"
          value={form.primaryEmail.trim() || "Not provided"}
          selected={form.preferredContactMethod === "EMAIL"}
        />

        <ContactPreviewCard
          icon={<Phone />}
          title="Phone"
          value={form.primaryPhone.trim() || "Not provided"}
          selected={
            form.preferredContactMethod === "PHONE" ||
            form.preferredContactMethod === "TEXT"
          }
        />
      </div>
    </div>
  );
}

// =====================================================================
// STEP 3 — ADDRESS
// =====================================================================

function AddressStep({ form, onChange }: StepProps) {
  return (
    <div className="space-y-5">
      <StepIntro
        title="Where is the customer located?"
        description="Address information is optional, but it can help with onsite service planning and customer identification."
      />

      <FormGrid>
        <TextField
          label="Street address"
          name="streetAddress"
          value={form.streetAddress}
          onChange={onChange}
          autoComplete="street-address"
          maxLength={180}
        />

        <TextField
          label="Address line 2"
          name="addressLine2"
          value={form.addressLine2}
          onChange={onChange}
          maxLength={180}
        />

        <TextField
          label="City"
          name="city"
          value={form.city}
          onChange={onChange}
          autoComplete="address-level2"
          maxLength={100}
        />

        <TextField
          label="State / region"
          name="stateRegion"
          value={form.stateRegion}
          onChange={onChange}
          autoComplete="address-level1"
          maxLength={100}
        />

        <TextField
          label="Postal code"
          name="postalCode"
          value={form.postalCode}
          onChange={onChange}
          autoComplete="postal-code"
          maxLength={30}
        />

        <TextField
          label="Country code"
          name="countryCode"
          value={form.countryCode}
          onChange={onChange}
          autoComplete="country"
          maxLength={2}
          hint="Two-letter country code, for example US."
        />
      </FormGrid>

      <div className="rounded-xl bg-slate-50 p-4">
        <div className="flex items-start gap-3">
          <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />

          <div>
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
              Address preview
            </p>

            <p className="mt-1 whitespace-pre-line text-sm font-semibold leading-6 text-slate-800">
              {formatAddressPreview(form) || "No address entered"}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// STEP 4 — COMMUNICATION PREFERENCES
// =====================================================================

function PreferencesStep({ form, onChange }: StepProps) {
  return (
    <div className="space-y-5">
      <StepIntro
        title="Set communication preferences"
        description="Record customer consent and restrictions carefully. These settings should guide future non-essential communication."
      />

      <div className="grid gap-3 md:grid-cols-2">
        <CheckboxCard
          name="marketingConsent"
          checked={form.marketingConsent}
          onChange={onChange}
          title="Marketing consent"
          description="Customer has agreed to receive optional marketing communications."
        />

        <CheckboxCard
          name="doNotEmail"
          checked={form.doNotEmail}
          onChange={onChange}
          title="Do not email"
          description="Prevent non-essential email communication."
        />

        <CheckboxCard
          name="doNotCall"
          checked={form.doNotCall}
          onChange={onChange}
          title="Do not call"
          description="Prevent non-essential telephone communication."
        />

        <CheckboxCard
          name="doNotText"
          checked={form.doNotText}
          onChange={onChange}
          title="Do not text"
          description="Prevent non-essential text-message communication."
        />
      </div>

      {form.marketingConsent ? (
        <div className="rounded-xl border border-brand-100 bg-brand-50/40 p-4">
          <TextField
            label="Marketing consent source"
            name="marketingConsentSource"
            value={form.marketingConsentSource}
            onChange={onChange}
            maxLength={50}
            hint="Example: PHONE, EMAIL, IN_PERSON, or WRITTEN."
          />
        </div>
      ) : null}

      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-slate-500" />

          <div>
            <p className="text-sm font-bold text-slate-900">
              Communication check
            </p>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              Preferred contact:{" "}
              <strong className="text-slate-700">
                {form.preferredContactMethod
                  ? formatEnumLabel(form.preferredContactMethod)
                  : "No preference"}
              </strong>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// STEP 5 — NOTES & REVIEW
// =====================================================================

function NotesStep({ form, onChange }: StepProps) {
  return (
    <div className="space-y-5">
      <StepIntro
        title="Add any useful notes"
        description="Notes are optional. Internal notes are intended only for Romelt TechCare administrators."
      />

      <div className="grid gap-4 xl:grid-cols-2">
        <TextAreaField
          label="Communication notes"
          name="communicationNotes"
          value={form.communicationNotes}
          onChange={onChange}
          maxLength={1000}
          rows={4}
          placeholder="Example: Customer prefers calls after 5 PM."
        />

        <TextAreaField
          label="Internal notes"
          name="internalNotes"
          value={form.internalNotes}
          onChange={onChange}
          maxLength={20000}
          rows={4}
          placeholder="Private administrator notes about the customer."
        />
      </div>
    </div>
  );
}

interface ReviewStepProps {
  form: CustomerFormState;
  onEditStep: (step: number) => void;
}

function ReviewStep({ form, onEditStep }: ReviewStepProps) {
  return (
    <div className="space-y-6">
      {/* =============================================================
       * DIVIDER
       * ============================================================= */}

      <div className="border-t border-slate-200" />

      {/* =============================================================
       * REVIEW
       * ============================================================= */}

      <div>
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700">
            <ClipboardCheck className="h-5 w-5" />
          </div>

          <div>
            <h3 className="font-display text-lg font-extrabold text-navy-950">
              Review customer
            </h3>

            <p className="mt-1 text-sm leading-5 text-slate-500">
              Confirm the information below before creating the reusable
              customer record.
            </p>
          </div>
        </div>

        <div className="mt-5 grid gap-3 lg:grid-cols-2">
          <ReviewCard title="Identity" step={1} onEdit={onEditStep}>
            <ReviewItem label="Display name" value={form.displayName} />

            <ReviewItem
              label="Full name"
              value={
                [form.firstName, form.lastName]
                  .filter((value) => value.trim())
                  .join(" ") || "Not recorded"
              }
            />

            <ReviewItem
              label="Preferred name"
              value={form.preferredName || "Not recorded"}
            />
          </ReviewCard>

          <ReviewCard title="Contact" step={2} onEdit={onEditStep}>
            <ReviewItem
              label="Email"
              value={form.primaryEmail || "Not provided"}
            />

            <ReviewItem
              label="Phone"
              value={form.primaryPhone || "Not provided"}
            />

            <ReviewItem
              label="Preferred contact"
              value={
                form.preferredContactMethod
                  ? formatEnumLabel(form.preferredContactMethod)
                  : "No preference"
              }
            />

            <ReviewItem
              label="Source"
              value={formatEnumLabel(form.customerSource)}
            />
          </ReviewCard>

          <ReviewCard title="Address" step={3} onEdit={onEditStep}>
            <ReviewItem
              label="Location"
              value={formatAddressPreview(form) || "No address recorded"}
              preserveWhitespace
            />
          </ReviewCard>

          <ReviewCard title="Communication" step={4} onEdit={onEditStep}>
            <ReviewBoolean
              label="Marketing consent"
              value={form.marketingConsent}
            />

            <ReviewBoolean label="Email restricted" value={form.doNotEmail} />

            <ReviewBoolean label="Calls restricted" value={form.doNotCall} />

            <ReviewBoolean label="Text restricted" value={form.doNotText} />
          </ReviewCard>

          <ReviewCard title="Notes" step={5} onEdit={onEditStep}>
            <ReviewItem
              label="Communication notes"
              value={form.communicationNotes || "No communication notes"}
              preserveWhitespace
            />

            <ReviewItem
              label="Internal notes"
              value={form.internalNotes || "No internal notes"}
              preserveWhitespace
            />
          </ReviewCard>
        </div>

        <div className="mt-4 flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

          <div>
            <p className="text-sm font-bold text-emerald-900">
              Ready for final review
            </p>

            <p className="mt-1 text-xs leading-5 text-emerald-800">
              Selecting Create customer will save this reusable profile to the
              Romelt TechCare backend.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// STEP NUMBER
// =====================================================================

interface StepNumberProps {
  number: number;

  current: boolean;

  completed: boolean;
}

function StepNumber({ number, current, completed }: StepNumberProps) {
  return (
    <span
      className={[
        "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-extrabold transition",
        current
          ? "bg-brand-700 text-white shadow-sm"
          : completed
            ? "bg-emerald-100 text-emerald-700"
            : "bg-slate-100 text-slate-500",
      ].join(" ")}
    >
      {completed ? <Check className="h-4 w-4" /> : number}
    </span>
  );
}

// =====================================================================
// STEP ICON
// =====================================================================

function resolveStepIcon(step: number): ReactNode {
  switch (step) {
    case 1:
      return <UserRound className="h-5 w-5" />;

    case 2:
      return <Phone className="h-5 w-5" />;

    case 3:
      return <MapPin className="h-5 w-5" />;

    case 4:
      return <ShieldCheck className="h-5 w-5" />;

    case 5:
      return <MessageSquareText className="h-5 w-5" />;

    case 6:
      return <ClipboardCheck className="h-5 w-5" />;

    default:
      return <UserRound className="h-5 w-5" />;
  }
}

// =====================================================================
// STEP INTRO
// =====================================================================

function StepIntro({
  title,
  description,
}: {
  title: string;

  description: string;
}) {
  return (
    <div>
      <h3 className="font-display text-base font-extrabold text-navy-950">
        {title}
      </h3>

      <p className="mt-1 max-w-3xl text-sm leading-6 text-slate-500">
        {description}
      </p>
    </div>
  );
}

// =====================================================================
// FORM GRID
// =====================================================================

function FormGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-4 md:grid-cols-2">{children}</div>;
}

// =====================================================================
// TEXT FIELD
// =====================================================================

interface TextFieldProps {
  label: string;

  name: string;

  value: string;

  onChange: (event: ChangeEvent<HTMLInputElement>) => void;

  type?: string;

  required?: boolean;

  maxLength?: number;

  autoComplete?: string;

  hint?: string;

  icon?: ReactNode;
}

function TextField({
  label,
  name,
  value,
  onChange,
  type = "text",
  required = false,
  maxLength,
  autoComplete,
  hint,
  icon,
}: TextFieldProps) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-800">
        {label}

        {required ? <span className="ml-1 text-red-600">*</span> : null}
      </span>

      <div className="relative mt-1.5">
        {icon ? (
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 [&>svg]:h-4 [&>svg]:w-4">
            {icon}
          </span>
        ) : null}

        <input
          type={type}
          name={name}
          value={value}
          onChange={onChange}
          required={required}
          maxLength={maxLength}
          autoComplete={autoComplete}
          className={[
            "focus-ring min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 transition placeholder:text-slate-400 hover:border-slate-400 focus:border-brand-500",
            icon ? "pl-10" : "",
          ].join(" ")}
        />
      </div>

      {hint ? (
        <span className="mt-1 block text-xs leading-5 text-slate-500">
          {hint}
        </span>
      ) : null}
    </label>
  );
}

// =====================================================================
// SELECT FIELD
// =====================================================================

interface SelectFieldProps {
  label: string;

  name: string;

  value: string;

  onChange: (event: ChangeEvent<HTMLSelectElement>) => void;

  required?: boolean;

  children: ReactNode;
}

function SelectField({
  label,
  name,
  value,
  onChange,
  required = false,
  children,
}: SelectFieldProps) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-800">
        {label}

        {required ? <span className="ml-1 text-red-600">*</span> : null}
      </span>

      <select
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="focus-ring mt-1.5 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-800 transition hover:border-slate-400 focus:border-brand-500"
      >
        {children}
      </select>
    </label>
  );
}

// =====================================================================
// TEXT AREA
// =====================================================================

interface TextAreaFieldProps {
  label: string;

  name: string;

  value: string;

  onChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;

  maxLength: number;

  rows?: number;

  placeholder?: string;
}

function TextAreaField({
  label,
  name,
  value,
  onChange,
  maxLength,
  rows = 5,
  placeholder,
}: TextAreaFieldProps) {
  return (
    <label className="block">
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-bold text-slate-800">{label}</span>

        <span className="text-xs text-slate-400">
          {value.length.toLocaleString("en-US")}/
          {maxLength.toLocaleString("en-US")}
        </span>
      </div>

      <textarea
        name={name}
        value={value}
        onChange={onChange}
        maxLength={maxLength}
        rows={rows}
        placeholder={placeholder}
        className="focus-ring mt-1.5 w-full resize-y rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm leading-6 text-slate-900 transition placeholder:text-slate-400 hover:border-slate-400 focus:border-brand-500"
      />
    </label>
  );
}

// =====================================================================
// CHECKBOX CARD
// =====================================================================

interface CheckboxCardProps {
  name: string;

  checked: boolean;

  onChange: (event: ChangeEvent<HTMLInputElement>) => void;

  title: string;

  description: string;
}

function CheckboxCard({
  name,
  checked,
  onChange,
  title,
  description,
}: CheckboxCardProps) {
  return (
    <label
      className={[
        "flex cursor-pointer items-start gap-3 rounded-xl border p-4 transition",
        checked
          ? "border-brand-300 bg-brand-50/60 shadow-sm"
          : "border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-white",
      ].join(" ")}
    >
      <span
        className={[
          "mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded border transition",
          checked
            ? "border-brand-700 bg-brand-700 text-white"
            : "border-slate-400 bg-white",
        ].join(" ")}
      >
        {checked ? <Check className="h-3.5 w-3.5" /> : null}
      </span>

      <input
        type="checkbox"
        name={name}
        checked={checked}
        onChange={onChange}
        className="sr-only"
      />

      <span>
        <span className="block text-sm font-bold text-slate-900">{title}</span>

        <span className="mt-1 block text-xs leading-5 text-slate-500">
          {description}
        </span>
      </span>
    </label>
  );
}

// =====================================================================
// CONTACT PREVIEW CARD
// =====================================================================

function ContactPreviewCard({
  icon,
  title,
  value,
  selected,
}: {
  icon: ReactNode;

  title: string;

  value: string;

  selected: boolean;
}) {
  return (
    <div
      className={[
        "rounded-xl border p-3.5",
        selected
          ? "border-brand-200 bg-brand-50/50"
          : "border-slate-200 bg-slate-50",
      ].join(" ")}
    >
      <div className="flex items-start gap-3">
        <div
          className={[
            "flex h-8 w-8 shrink-0 items-center justify-center rounded-lg [&>svg]:h-4 [&>svg]:w-4",
            selected
              ? "bg-brand-100 text-brand-700"
              : "bg-white text-slate-500",
          ].join(" ")}
        >
          {icon}
        </div>

        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
              {title}
            </p>

            {selected ? (
              <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-wide text-brand-800">
                Preferred
              </span>
            ) : null}
          </div>

          <p className="mt-1 break-words text-sm font-semibold text-slate-800">
            {value}
          </p>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// REVIEW CARD
// =====================================================================

interface ReviewCardProps {
  title: string;

  step: number;

  onEdit: (step: number) => void;

  children: ReactNode;
}

function ReviewCard({ title, step, onEdit, children }: ReviewCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
      <div className="flex items-center justify-between gap-3 border-b border-slate-200 pb-3">
        <h4 className="font-display text-sm font-extrabold text-navy-950">
          {title}
        </h4>

        <button
          type="button"
          onClick={() => onEdit(step)}
          className="focus-ring rounded-lg px-2 py-1 text-xs font-bold text-brand-700 transition hover:bg-brand-50"
        >
          Edit
        </button>
      </div>

      <div className="mt-3 space-y-2.5">{children}</div>
    </div>
  );
}

// =====================================================================
// REVIEW ITEM
// =====================================================================

function ReviewItem({
  label,
  value,
  preserveWhitespace = false,
}: {
  label: string;

  value: string;

  preserveWhitespace?: boolean;
}) {
  return (
    <div>
      <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
        {label}
      </p>

      <p
        className={[
          "mt-0.5 break-words text-sm font-semibold leading-5 text-slate-800",
          preserveWhitespace ? "whitespace-pre-line" : "",
        ].join(" ")}
      >
        {value}
      </p>
    </div>
  );
}

// =====================================================================
// REVIEW BOOLEAN
// =====================================================================

function ReviewBoolean({
  label,
  value,
}: {
  label: string;

  value: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-sm text-slate-600">{label}</span>

      <span
        className={[
          "inline-flex rounded-full px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-wide",
          value
            ? "bg-amber-100 text-amber-800"
            : "bg-emerald-100 text-emerald-800",
        ].join(" ")}
      >
        {value ? "Yes" : "No"}
      </span>
    </div>
  );
}

// =====================================================================
// STEP VALIDATION
// =====================================================================

function validateStep(step: number, form: CustomerFormState): string | null {
  switch (step) {
    case 1:
      return validateIdentityStep(form);

    case 2:
      return validateContactStep(form);

    case 3:
      return validateAddressStep(form);

    case 4:
      return validatePreferenceStep(form);

    case 5:
      return null;

    case 6:
      return validateCustomerForm(form);

    default:
      return null;
  }
}

// =====================================================================
// STEP 1 VALIDATION
// =====================================================================

function validateIdentityStep(form: CustomerFormState): string | null {
  const displayName = form.displayName.trim();

  if (displayName.length < 2) {
    return "Display name must contain at least 2 characters.";
  }

  if (displayName.length > 180) {
    return "Display name cannot exceed 180 characters.";
  }

  return null;
}

// =====================================================================
// STEP 2 VALIDATION
// =====================================================================

function validateContactStep(form: CustomerFormState): string | null {
  const email = form.primaryEmail.trim();

  const phone = form.primaryPhone.trim();

  if (!email && !phone) {
    return "Enter at least one customer contact method: email address or phone number.";
  }

  if (email && !isValidEmail(email)) {
    return "Enter a valid customer email address.";
  }

  if (phone && !/^[0-9+().\-\s]{10,40}$/.test(phone)) {
    return "Enter a valid customer telephone number.";
  }

  if (form.preferredContactMethod === "EMAIL" && !email) {
    return "An email address is required when Email is the preferred contact method.";
  }

  if (
    (form.preferredContactMethod === "PHONE" ||
      form.preferredContactMethod === "TEXT") &&
    !phone
  ) {
    return "A phone number is required for the selected preferred contact method.";
  }

  return null;
}

// =====================================================================
// STEP 3 VALIDATION
// =====================================================================

function validateAddressStep(form: CustomerFormState): string | null {
  if (
    form.countryCode.trim() &&
    !/^[A-Za-z]{2}$/.test(form.countryCode.trim())
  ) {
    return "Country code must contain exactly two letters.";
  }

  return null;
}

// =====================================================================
// STEP 4 VALIDATION
// =====================================================================

function validatePreferenceStep(form: CustomerFormState): string | null {
  if (form.doNotEmail && form.preferredContactMethod === "EMAIL") {
    return "Email cannot be both the preferred contact method and marked Do not email.";
  }

  if (form.doNotCall && form.preferredContactMethod === "PHONE") {
    return "Phone cannot be both the preferred contact method and marked Do not call.";
  }

  if (form.doNotText && form.preferredContactMethod === "TEXT") {
    return "Text cannot be both the preferred contact method and marked Do not text.";
  }

  return null;
}

// =====================================================================
// COMPLETE VALIDATION
// =====================================================================

function validateCustomerForm(form: CustomerFormState): string | null {
  return (
    validateIdentityStep(form) ||
    validateContactStep(form) ||
    validateAddressStep(form) ||
    validatePreferenceStep(form)
  );
}

// =====================================================================
// INVALID STEP RESOLUTION
// =====================================================================

function resolveInvalidStep(form: CustomerFormState): number {
  if (validateIdentityStep(form)) {
    return 1;
  }

  if (validateContactStep(form)) {
    return 2;
  }

  if (validateAddressStep(form)) {
    return 3;
  }

  if (validatePreferenceStep(form)) {
    return 4;
  }

  return 5;
}

// =====================================================================
// ADDRESS PREVIEW
// =====================================================================

function formatAddressPreview(form: CustomerFormState): string {
  const lines: string[] = [];

  if (form.streetAddress.trim()) {
    lines.push(form.streetAddress.trim());
  }

  if (form.addressLine2.trim()) {
    lines.push(form.addressLine2.trim());
  }

  const locality = [
    form.city.trim(),
    form.stateRegion.trim(),
    form.postalCode.trim(),
  ]
    .filter(Boolean)
    .join(", ");

  if (locality) {
    lines.push(locality);
  }

  if (form.countryCode.trim()) {
    lines.push(form.countryCode.trim().toUpperCase());
  }

  return lines.join("\n");
}

// =====================================================================
// ENUM FORMATTER
// =====================================================================

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

// =====================================================================
// HELPERS
// =====================================================================

function toNullable(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized : null;
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function scrollToStepTop() {
  window.setTimeout(() => {
    const element = document.getElementById("customer-form-step");

    element?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }, 0);
}
