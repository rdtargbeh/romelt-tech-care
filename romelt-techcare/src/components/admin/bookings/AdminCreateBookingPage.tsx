/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CREATE CLIENT BOOKING PAGE
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to create a booking for a
 * customer who called, emailed, walked in, or contacted the business
 * through another channel.
 *
 * Five-step workflow:
 * 1. Customer
 * 2. Service
 * 3. Schedule
 * 4. Location & Notes
 * 5. Review & Submit
 *
 * Responsibilities:
 * - Captures customer contact information.
 * - Captures requested service information.
 * - Captures preferred scheduling information.
 * - Requires an address for on-site bookings.
 * - Records the booking source.
 * - Supports private administrator notes.
 * - Validates each step before moving forward.
 * - Displays a compact final review before submission.
 * - Saves the booking through the authenticated administrator API.
 *
 * Real-data integration:
 * POST /api/v1/admin/booking-requests
 * ================================================================
 */

import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  useMemo,
  useState,
} from "react";
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  CalendarPlus,
  Check,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  Clock3,
  LoaderCircle,
  MapPin,
  Save,
  StickyNote,
  UserRound,
  Wrench,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

import { createAdminBookingRequest } from "@/services/admin-customer-request.service";

import type {
  AdminBookingRequestCreatePayload,
  BookingSource,
  ContactMethod,
  PreferredServiceTime,
  ServiceMethod,
} from "@/types/admin-customer-request.types";

interface BookingFormState {
  fullName: string;
  email: string;
  phone: string;
  serviceType: string;
  serviceMethod: ServiceMethod;
  preferredDate: string;
  preferredTime: PreferredServiceTime;
  alternateDate: string;
  streetAddress: string;
  city: string;
  state: string;
  postalCode: string;
  deviceType: string;
  problemDescription: string;
  preferredContactMethod: ContactMethod;
  bookingSource: Exclude<BookingSource, "WEBSITE">;
  adminNotes: string;
}

interface ProgressStep {
  number: number;
  title: string;
  description: string;
  icon: ReactNode;
}

const TOTAL_STEPS = 5;

const PROGRESS_STEPS: ProgressStep[] = [
  {
    number: 1,
    title: "Customer",
    description: "Contact",
    icon: <UserRound />,
  },
  {
    number: 2,
    title: "Service",
    description: "Support request",
    icon: <Wrench />,
  },
  {
    number: 3,
    title: "Schedule",
    description: "Date and time",
    icon: <CalendarDays />,
  },
  {
    number: 4,
    title: "Location",
    description: "Address and notes",
    icon: <MapPin />,
  },
  {
    number: 5,
    title: "Review",
    description: "Confirm and submit",
    icon: <ClipboardCheck />,
  },
];

const INITIAL_FORM: BookingFormState = {
  fullName: "",
  email: "",
  phone: "",
  serviceType: "",
  serviceMethod: "REMOTE",
  preferredDate: "",
  preferredTime: "FLEXIBLE",
  alternateDate: "",
  streetAddress: "",
  city: "",
  state: "",
  postalCode: "",
  deviceType: "",
  problemDescription: "",
  preferredContactMethod: "PHONE",
  bookingSource: "PHONE",
  adminNotes: "",
};

export default function AdminCreateBookingPage() {
  const navigate = useNavigate();

  const [currentStep, setCurrentStep] = useState(1);
  const [form, setForm] = useState<BookingFormState>(INITIAL_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const minimumDate = useMemo(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    return tomorrow.toISOString().slice(0, 10);
  }, []);

  const requiresAddress = form.serviceMethod === "ON_SITE";

  function handleChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setErrorMessage(null);
  }

  function handleNextStep() {
    const validationMessage = validateStep(currentStep, form);

    if (validationMessage) {
      setErrorMessage(validationMessage);
      scrollToFormTop();
      return;
    }

    setErrorMessage(null);
    setCurrentStep((step) => Math.min(step + 1, TOTAL_STEPS));
    scrollToFormTop();
  }

  function handlePreviousStep() {
    setErrorMessage(null);
    setCurrentStep((step) => Math.max(step - 1, 1));
    scrollToFormTop();
  }

  function handleStepSelection(stepNumber: number) {
    if (stepNumber >= currentStep) {
      return;
    }

    setErrorMessage(null);
    setCurrentStep(stepNumber);
    scrollToFormTop();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (currentStep < TOTAL_STEPS) {
      handleNextStep();
      return;
    }

    setErrorMessage(null);

    const validationMessage = validateEntireForm(form);

    if (validationMessage) {
      setErrorMessage(validationMessage);
      scrollToFormTop();
      return;
    }

    const payload: AdminBookingRequestCreatePayload = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      serviceType: form.serviceType.trim(),
      serviceMethod: form.serviceMethod,
      preferredDate: form.preferredDate,
      preferredTime: form.preferredTime,
      alternateDate: toNullable(form.alternateDate),
      streetAddress: toNullable(form.streetAddress),
      city: toNullable(form.city),
      state: toNullable(form.state),
      postalCode: toNullable(form.postalCode),
      deviceType: toNullable(form.deviceType),
      problemDescription: form.problemDescription.trim(),
      preferredContactMethod: form.preferredContactMethod,
      bookingSource: form.bookingSource,
      adminNotes: toNullable(form.adminNotes),
    };

    setIsSubmitting(true);

    try {
      const createdBooking = await createAdminBookingRequest(payload);

      navigate(`/admin/bookings/${createdBooking.bookingRequestId}`, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The client booking could not be created.",
      );

      scrollToFormTop();
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="space-y-4">
      <Link
        to="/admin/bookings"
        className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to bookings
      </Link>

      <header className="space-y-1">
        <p className="text-xs font-extrabold uppercase tracking-[0.15em] text-brand-700">
          Administrator booking
        </p>

        <h1 className="font-display text-2xl font-black text-navy-950 sm:text-3xl">
          Book Service for a Client
        </h1>

        <p className="max-w-3xl text-sm leading-6 text-slate-600">
          Create a booking for a customer who contacted Romelt TechCare by
          telephone, email, walk-in, or another supported channel.
        </p>
      </header>

      <ProgressIndicator
        currentStep={currentStep}
        onStepSelection={handleStepSelection}
      />

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

          <div>
            <p className="text-sm font-bold">
              {currentStep === TOTAL_STEPS
                ? "Booking could not be created"
                : "Complete this step"}
            </p>

            <p className="mt-0.5 text-sm">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {currentStep === 1 ? (
          <CustomerStep form={form} onChange={handleChange} />
        ) : null}

        {currentStep === 2 ? (
          <ServiceStep form={form} onChange={handleChange} />
        ) : null}

        {currentStep === 3 ? (
          <ScheduleStep
            form={form}
            minimumDate={minimumDate}
            onChange={handleChange}
          />
        ) : null}

        {currentStep === 4 ? (
          <LocationAndNotesStep
            form={form}
            requiresAddress={requiresAddress}
            onChange={handleChange}
          />
        ) : null}

        {currentStep === 5 ? (
          <ReviewStep form={form} requiresAddress={requiresAddress} />
        ) : null}

        <FormNavigation
          currentStep={currentStep}
          isSubmitting={isSubmitting}
          onPrevious={handlePreviousStep}
        />
      </form>

      <div className="rounded-xl border border-brand-200 bg-brand-50 px-4 py-3">
        <div className="flex items-start gap-3">
          <CalendarPlus className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

          <p className="text-sm leading-6 text-brand-950">
            Creating this record confirms that Romelt TechCare received the
            customer’s request. The booking remains pending until an
            administrator reviews and confirms it.
          </p>
        </div>
      </div>
    </section>
  );
}

function CustomerStep({
  form,
  onChange,
}: {
  form: BookingFormState;
  onChange: ChangeEventHandler;
}) {
  return (
    <FormSection
      stepNumber={1}
      title="Customer Information"
      description="Enter the customer’s contact details and how they reached Romelt TechCare."
      icon={<UserRound />}
    >
      <FormGrid>
        <TextField
          label="Full name"
          name="fullName"
          value={form.fullName}
          onChange={onChange}
          placeholder="Example: John Doe"
          maxLength={120}
          autoComplete="name"
          required
        />

        <TextField
          label="Email address"
          name="email"
          type="email"
          value={form.email}
          onChange={onChange}
          placeholder="Example: john@email.com"
          maxLength={254}
          autoComplete="email"
          required
        />

        <TextField
          label="Telephone number"
          name="phone"
          type="tel"
          value={form.phone}
          onChange={onChange}
          placeholder="Example: +1 (515) 555-0100"
          maxLength={30}
          autoComplete="tel"
          required
        />

        <SelectField
          label="Preferred contact method"
          name="preferredContactMethod"
          value={form.preferredContactMethod}
          onChange={onChange}
          options={[
            ["PHONE", "Phone"],
            ["TEXT", "Text message"],
            ["EMAIL", "Email"],
          ]}
        />

        <SelectField
          label="How the customer contacted us"
          name="bookingSource"
          value={form.bookingSource}
          onChange={onChange}
          options={[
            ["PHONE", "Telephone"],
            ["EMAIL", "Email"],
            ["WALK_IN", "Walk-in"],
            ["ADMIN_ENTRY", "Administrator entry"],
            ["OTHER", "Other"],
          ]}
        />
      </FormGrid>
    </FormSection>
  );
}

function ServiceStep({
  form,
  onChange,
}: {
  form: BookingFormState;
  onChange: ChangeEventHandler;
}) {
  return (
    <FormSection
      stepNumber={2}
      title="Service Request"
      description="Record the requested service and the customer’s problem."
      icon={<Wrench />}
    >
      <FormGrid>
        <TextField
          label="Service type"
          name="serviceType"
          value={form.serviceType}
          onChange={onChange}
          placeholder="Example: Computer repair"
          maxLength={120}
          required
        />

        <SelectField
          label="Service method"
          name="serviceMethod"
          value={form.serviceMethod}
          onChange={onChange}
          options={[
            ["REMOTE", "Remote support"],
            ["ON_SITE", "On-site service"],
            ["DROP_OFF", "Device drop-off"],
            ["NOT_SURE", "Not sure"],
          ]}
        />

        <TextField
          label="Device or equipment"
          name="deviceType"
          value={form.deviceType}
          onChange={onChange}
          placeholder="Example: Dell laptop"
          maxLength={120}
        />
      </FormGrid>

      <TextAreaField
        label="Problem or requested service"
        name="problemDescription"
        value={form.problemDescription}
        onChange={onChange}
        placeholder="Describe the issue, symptoms, requested work, and any details provided by the customer."
        minLength={20}
        maxLength={2000}
        rows={5}
        required
      />
    </FormSection>
  );
}

function ScheduleStep({
  form,
  minimumDate,
  onChange,
}: {
  form: BookingFormState;
  minimumDate: string;
  onChange: ChangeEventHandler;
}) {
  return (
    <FormSection
      stepNumber={3}
      title="Requested Schedule"
      description="Record the customer’s preferred date and time."
      icon={<Clock3 />}
    >
      <FormGrid>
        <TextField
          label="Preferred date"
          name="preferredDate"
          type="date"
          value={form.preferredDate}
          onChange={onChange}
          min={minimumDate}
          required
        />

        <SelectField
          label="Preferred time"
          name="preferredTime"
          value={form.preferredTime}
          onChange={onChange}
          options={[
            ["MORNING", "Morning"],
            ["AFTERNOON", "Afternoon"],
            ["EVENING", "Evening"],
            ["FLEXIBLE", "Flexible"],
          ]}
        />

        <TextField
          label="Alternate date"
          name="alternateDate"
          type="date"
          value={form.alternateDate}
          onChange={onChange}
          min={minimumDate}
        />
      </FormGrid>
    </FormSection>
  );
}

function LocationAndNotesStep({
  form,
  requiresAddress,
  onChange,
}: {
  form: BookingFormState;
  requiresAddress: boolean;
  onChange: ChangeEventHandler;
}) {
  return (
    <FormSection
      stepNumber={4}
      title="Service Location and Internal Notes"
      description={
        requiresAddress
          ? "Enter the service address and any private administrator notes."
          : "Address information is optional unless the booking is for on-site service."
      }
      icon={<MapPin />}
    >
      {requiresAddress ? (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-900">
          On-site service requires the street address, city, state, and postal
          code.
        </div>
      ) : null}

      <FormGrid>
        <TextField
          label="Street address"
          name="streetAddress"
          value={form.streetAddress}
          onChange={onChange}
          placeholder="Street address"
          maxLength={180}
          autoComplete="street-address"
          required={requiresAddress}
        />

        <TextField
          label="City"
          name="city"
          value={form.city}
          onChange={onChange}
          placeholder="City"
          maxLength={100}
          autoComplete="address-level2"
          required={requiresAddress}
        />

        <TextField
          label="State"
          name="state"
          value={form.state}
          onChange={onChange}
          placeholder="State"
          maxLength={100}
          autoComplete="address-level1"
          required={requiresAddress}
        />

        <TextField
          label="Postal code"
          name="postalCode"
          value={form.postalCode}
          onChange={onChange}
          placeholder="Example: 50309"
          maxLength={10}
          autoComplete="postal-code"
          required={requiresAddress}
        />
      </FormGrid>

      <div className="border-t border-slate-100 pt-4">
        <div className="mb-3 flex items-center gap-2">
          <StickyNote className="h-4 w-4 text-brand-700" />

          <p className="text-sm font-extrabold text-navy-950">Internal Notes</p>

          <span className="text-xs font-medium text-slate-500">Optional</span>
        </div>

        <TextAreaField
          label="Administrator notes"
          name="adminNotes"
          value={form.adminNotes}
          onChange={onChange}
          placeholder="Example: Customer requested a callback after 5:00 PM."
          maxLength={2000}
          rows={4}
        />
      </div>
    </FormSection>
  );
}

function ReviewStep({
  form,
  requiresAddress,
}: {
  form: BookingFormState;
  requiresAddress: boolean;
}) {
  const address = [form.streetAddress, form.city, form.state, form.postalCode]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  return (
    <FormSection
      stepNumber={5}
      title="Review and Submit"
      description="Confirm the booking information before creating the record."
      icon={<ClipboardCheck />}
    >
      <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3">
        <div className="flex items-start gap-3">
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

          <div>
            <p className="text-sm font-bold text-emerald-950">
              Ready for final review
            </p>

            <p className="mt-0.5 text-sm text-emerald-900">
              Verify the information below before creating the client booking.
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <ReviewCard title="Customer" icon={<UserRound />}>
          <ReviewRow label="Full name" value={form.fullName} />
          <ReviewRow label="Email" value={form.email} />
          <ReviewRow label="Telephone" value={form.phone} />
          <ReviewRow
            label="Preferred contact"
            value={formatLabel(form.preferredContactMethod)}
          />
          <ReviewRow
            label="Booking source"
            value={formatLabel(form.bookingSource)}
          />
        </ReviewCard>

        <ReviewCard title="Service" icon={<Wrench />}>
          <ReviewRow label="Service type" value={form.serviceType} />
          <ReviewRow
            label="Service method"
            value={formatLabel(form.serviceMethod)}
          />
          <ReviewRow label="Device" value={form.deviceType || "Not provided"} />

          <ReviewText
            label="Problem or requested service"
            value={form.problemDescription}
          />
        </ReviewCard>

        <ReviewCard title="Schedule" icon={<CalendarDays />}>
          <ReviewRow
            label="Preferred date"
            value={formatDate(form.preferredDate)}
          />
          <ReviewRow
            label="Preferred time"
            value={formatLabel(form.preferredTime)}
          />
          <ReviewRow
            label="Alternate date"
            value={
              form.alternateDate
                ? formatDate(form.alternateDate)
                : "Not provided"
            }
          />
        </ReviewCard>

        <ReviewCard title="Location and Notes" icon={<MapPin />}>
          <ReviewRow
            label="Service location"
            value={
              address ||
              (requiresAddress
                ? "Required address missing"
                : "No address provided")
            }
          />

          <ReviewText
            label="Administrator notes"
            value={form.adminNotes || "No administrator notes provided."}
          />
        </ReviewCard>
      </div>
    </FormSection>
  );
}

function ProgressIndicator({
  currentStep,
  onStepSelection,
}: {
  currentStep: number;
  onStepSelection: (stepNumber: number) => void;
}) {
  return (
    <nav
      aria-label="Booking creation progress"
      className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm sm:p-4"
    >
      <ol className="grid grid-cols-2 gap-2 md:grid-cols-5">
        {PROGRESS_STEPS.map((step) => {
          const isActive = step.number === currentStep;
          const isCompleted = step.number < currentStep;

          return (
            <li key={step.number}>
              <button
                type="button"
                onClick={() => onStepSelection(step.number)}
                disabled={!isCompleted}
                aria-current={isActive ? "step" : undefined}
                className={`focus-ring flex min-h-[68px] w-full items-center gap-2 rounded-lg border px-3 py-2 text-left transition ${
                  isActive
                    ? "border-brand-500 bg-brand-50"
                    : isCompleted
                      ? "border-emerald-200 bg-emerald-50 hover:border-emerald-400"
                      : "border-slate-200 bg-slate-50"
                } disabled:cursor-default`}
              >
                <span
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full [&>svg]:h-4 [&>svg]:w-4 ${
                    isActive
                      ? "bg-brand-700 text-white"
                      : isCompleted
                        ? "bg-emerald-600 text-white"
                        : "bg-slate-200 text-slate-500"
                  }`}
                >
                  {isCompleted ? <Check /> : step.icon}
                </span>

                <span className="min-w-0">
                  <span
                    className={`block text-[10px] font-extrabold uppercase tracking-wide ${
                      isActive
                        ? "text-brand-700"
                        : isCompleted
                          ? "text-emerald-700"
                          : "text-slate-500"
                    }`}
                  >
                    Step {step.number}
                  </span>

                  <span className="block truncate text-sm font-extrabold text-navy-950">
                    {step.title}
                  </span>

                  <span className="hidden truncate text-[11px] text-slate-500 xl:block">
                    {step.description}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ol>

      <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-brand-700 transition-all duration-300"
          style={{
            width: `${(currentStep / TOTAL_STEPS) * 100}%`,
          }}
        />
      </div>

      <p className="mt-2 text-right text-[11px] font-bold text-slate-500">
        Step {currentStep} of {TOTAL_STEPS}
      </p>
    </nav>
  );
}

function FormNavigation({
  currentStep,
  isSubmitting,
  onPrevious,
}: {
  currentStep: number;
  isSubmitting: boolean;
  onPrevious: () => void;
}) {
  const isFirstStep = currentStep === 1;
  const isFinalStep = currentStep === TOTAL_STEPS;

  return (
    <div className="sticky bottom-3 z-10 rounded-xl border border-slate-200 bg-white/95 px-4 py-3 shadow-lg backdrop-blur">
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-between">
        {isFirstStep ? (
          <Link
            to="/admin/bookings"
            className="focus-ring inline-flex min-h-10 items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:border-slate-400"
          >
            Cancel
          </Link>
        ) : (
          <button
            type="button"
            onClick={onPrevious}
            disabled={isSubmitting}
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:border-brand-400 hover:text-brand-700 disabled:opacity-50"
          >
            <ArrowLeft className="h-4 w-4" />
            Previous
          </button>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-brand-700 px-5 py-2 text-sm font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? (
            <LoaderCircle className="h-4 w-4 animate-spin" />
          ) : isFinalStep ? (
            <Save className="h-4 w-4" />
          ) : (
            <ArrowRight className="h-4 w-4" />
          )}

          {isSubmitting
            ? "Creating booking..."
            : isFinalStep
              ? "Create Client Booking"
              : "Continue"}
        </button>
      </div>
    </div>
  );
}

function FormSection({
  stepNumber,
  title,
  description,
  icon,
  children,
}: {
  stepNumber?: number;
  title: string;
  description: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <section
      id="booking-form-section"
      className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"
    >
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <div>
          {stepNumber ? (
            <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-brand-700">
              Step {stepNumber} of {TOTAL_STEPS}
            </p>
          ) : null}

          <h2 className="mt-0.5 font-display text-lg font-extrabold text-navy-950 sm:text-xl">
            {title}
          </h2>

          <p className="mt-0.5 text-sm leading-5 text-slate-600">
            {description}
          </p>
        </div>
      </div>

      <div className="mt-4 space-y-4">{children}</div>
    </section>
  );
}

function FormGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{children}</div>
  );
}

function TextField({
  label,
  name,
  value,
  onChange,
  type = "text",
  required = false,
  ...inputProps
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  type?: string;
  required?: boolean;
  placeholder?: string;
  min?: string;
  minLength?: number;
  maxLength?: number;
  autoComplete?: string;
}) {
  return (
    <label className="block">
      <span className="text-xs font-extrabold text-slate-700">
        {label}
        {required ? <span className="text-red-600"> *</span> : null}
      </span>

      <input
        {...inputProps}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="focus-ring mt-1.5 min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400"
      />
    </label>
  );
}

function SelectField({
  label,
  name,
  value,
  onChange,
  options,
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  options: Array<[string, string]>;
}) {
  return (
    <label className="block">
      <span className="text-xs font-extrabold text-slate-700">
        {label} <span className="text-red-600">*</span>
      </span>

      <select
        name={name}
        value={value}
        onChange={onChange}
        required
        className="focus-ring mt-1.5 min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900"
      >
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextAreaField({
  label,
  name,
  value,
  onChange,
  required = false,
  ...textAreaProps
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  required?: boolean;
  placeholder?: string;
  minLength?: number;
  maxLength?: number;
  rows?: number;
}) {
  return (
    <label className="block">
      <span className="text-xs font-extrabold text-slate-700">
        {label}
        {required ? <span className="text-red-600"> *</span> : null}
      </span>

      <textarea
        {...textAreaProps}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="focus-ring mt-1.5 w-full resize-y rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400"
      />

      {textAreaProps.maxLength ? (
        <span className="mt-1 block text-right text-[11px] text-slate-500">
          {value.length}/{textAreaProps.maxLength}
        </span>
      ) : null}
    </label>
  );
}

function ReviewCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <article className="rounded-xl border border-slate-200 p-4">
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <h3 className="font-display text-base font-extrabold text-navy-950">
          {title}
        </h3>
      </div>

      <div className="mt-3">{children}</div>
    </article>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5 border-b border-slate-100 py-2 first:pt-0 last:border-0 last:pb-0 sm:flex-row sm:justify-between sm:gap-4">
      <dt className="text-xs font-semibold text-slate-500">{label}</dt>

      <dd className="break-words text-sm font-bold text-slate-800 sm:max-w-[65%] sm:text-right">
        {value || "Not provided"}
      </dd>
    </div>
  );
}

function ReviewText({ label, value }: { label: string; value: string }) {
  return (
    <div className="mt-3">
      <p className="text-xs font-semibold text-slate-500">{label}</p>

      <p className="mt-1.5 max-h-32 overflow-y-auto whitespace-pre-wrap rounded-lg bg-slate-50 px-3 py-2.5 text-sm leading-6 text-slate-800">
        {value}
      </p>
    </div>
  );
}

type ChangeEventHandler = (
  event: ChangeEvent<
    HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  >,
) => void;

function validateStep(step: number, form: BookingFormState): string | null {
  switch (step) {
    case 1:
      return validateCustomerStep(form);

    case 2:
      return validateServiceStep(form);

    case 3:
      return validateScheduleStep(form);

    case 4:
      return validateLocationAndNotesStep(form);

    case 5:
      return validateEntireForm(form);

    default:
      return "The selected booking step is invalid.";
  }
}

function validateCustomerStep(form: BookingFormState): string | null {
  if (!form.fullName.trim()) {
    return "Enter the customer’s full name.";
  }

  if (form.fullName.trim().length < 2) {
    return "Customer name must contain at least 2 characters.";
  }

  if (!form.email.trim()) {
    return "Enter the customer’s email address.";
  }

  if (!isValidEmail(form.email)) {
    return "Enter a valid customer email address.";
  }

  if (!form.phone.trim()) {
    return "Enter the customer’s telephone number.";
  }

  if (!isValidPhone(form.phone)) {
    return "Enter a valid telephone number containing at least 10 digits.";
  }

  if (!form.preferredContactMethod) {
    return "Select the customer’s preferred contact method.";
  }

  if (!form.bookingSource) {
    return "Select how the customer contacted Romelt TechCare.";
  }

  return null;
}

function validateServiceStep(form: BookingFormState): string | null {
  if (!form.serviceType.trim()) {
    return "Enter the service the customer needs.";
  }

  if (!form.serviceMethod) {
    return "Select the requested service method.";
  }

  if (!form.problemDescription.trim()) {
    return "Describe the customer’s problem or requested service.";
  }

  if (form.problemDescription.trim().length < 20) {
    return "The problem description must contain at least 20 characters.";
  }

  return null;
}

function validateScheduleStep(form: BookingFormState): string | null {
  if (!form.preferredDate) {
    return "Select the customer’s preferred service date.";
  }

  if (!form.preferredTime) {
    return "Select the customer’s preferred service time.";
  }

  if (form.alternateDate && form.alternateDate === form.preferredDate) {
    return "Alternate date must be different from the preferred date.";
  }

  return null;
}

function validateLocationAndNotesStep(form: BookingFormState): string | null {
  if (form.serviceMethod !== "ON_SITE") {
    return null;
  }

  if (!form.streetAddress.trim()) {
    return "Street address is required for on-site service.";
  }

  if (!form.city.trim()) {
    return "City is required for on-site service.";
  }

  if (!form.state.trim()) {
    return "State is required for on-site service.";
  }

  if (!form.postalCode.trim()) {
    return "Postal code is required for on-site service.";
  }

  if (!isValidPostalCode(form.postalCode)) {
    return "Enter a valid U.S. postal code.";
  }

  return null;
}

function validateEntireForm(form: BookingFormState): string | null {
  return (
    validateCustomerStep(form) ??
    validateServiceStep(form) ??
    validateScheduleStep(form) ??
    validateLocationAndNotesStep(form)
  );
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function isValidPhone(value: string): boolean {
  const digitCount = value.replace(/\D/g, "").length;

  return digitCount >= 10 && digitCount <= 20;
}

function isValidPostalCode(value: string): boolean {
  return /^\d{5}(?:-\d{4})?$/.test(value.trim());
}

function toNullable(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized : null;
}

function formatLabel(value: string): string {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "long",
      }).format(date);
}

function scrollToFormTop() {
  window.requestAnimationFrame(() => {
    document.getElementById("booking-form-section")?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  });
}
