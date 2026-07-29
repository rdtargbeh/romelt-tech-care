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
 * Responsibilities:
 * - Captures customer contact information.
 * - Captures service and scheduling information.
 * - Requires an address for on-site bookings.
 * - Records the booking source.
 * - Supports private administrator notes.
 * - Saves the booking through the authenticated administrator API.
 *
 * Real-data integration:
 * POST /api/v1/admin/booking-requests
 * ================================================================
 */

import { type ChangeEvent, type FormEvent, useMemo, useState } from "react";
import {
  ArrowLeft,
  CalendarPlus,
  CircleAlert,
  LoaderCircle,
  Save,
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
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setErrorMessage(null);

    if (form.alternateDate && form.alternateDate === form.preferredDate) {
      setErrorMessage(
        "Alternate date must be different from the preferred date.",
      );
      return;
    }

    if (
      requiresAddress &&
      (!form.streetAddress.trim() ||
        !form.city.trim() ||
        !form.state.trim() ||
        !form.postalCode.trim())
    ) {
      setErrorMessage(
        "A complete service address is required for on-site service.",
      );
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
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="space-y-6">
      <Link
        to="/admin/bookings"
        className="focus-ring inline-flex items-center gap-2 rounded-lg font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to bookings
      </Link>

      <header>
        <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
          Administrator booking
        </p>

        <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
          Book Service for a Client
        </h1>

        <p className="mt-2 max-w-3xl leading-7 text-slate-600">
          Enter the customer’s information and requested service while speaking
          with them by telephone or handling their request through another
          channel.
        </p>
      </header>

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

          <div>
            <p className="font-bold">Booking could not be created</p>
            <p className="mt-1 text-sm">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      <form onSubmit={handleSubmit} className="space-y-6">
        <FormSection
          title="Customer information"
          description="Enter the customer’s contact information exactly as provided."
        >
          <FormGrid>
            <TextField
              label="Full name"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              maxLength={120}
              required
            />

            <TextField
              label="Email address"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              maxLength={254}
              required
            />

            <TextField
              label="Telephone number"
              name="phone"
              type="tel"
              value={form.phone}
              onChange={handleChange}
              maxLength={30}
              required
            />

            <SelectField
              label="Preferred contact method"
              name="preferredContactMethod"
              value={form.preferredContactMethod}
              onChange={handleChange}
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
              onChange={handleChange}
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

        <FormSection
          title="Service request"
          description="Describe the service the customer needs."
        >
          <FormGrid>
            <TextField
              label="Service type"
              name="serviceType"
              value={form.serviceType}
              onChange={handleChange}
              placeholder="Example: Computer repair"
              maxLength={120}
              required
            />

            <SelectField
              label="Service method"
              name="serviceMethod"
              value={form.serviceMethod}
              onChange={handleChange}
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
              onChange={handleChange}
              placeholder="Example: Dell laptop"
              maxLength={120}
            />
          </FormGrid>

          <TextAreaField
            label="Problem or requested service"
            name="problemDescription"
            value={form.problemDescription}
            onChange={handleChange}
            minLength={20}
            maxLength={2000}
            rows={6}
            required
          />
        </FormSection>

        <FormSection
          title="Requested schedule"
          description="Record the customer’s preferred service date and time."
        >
          <FormGrid>
            <TextField
              label="Preferred date"
              name="preferredDate"
              type="date"
              value={form.preferredDate}
              onChange={handleChange}
              min={minimumDate}
              required
            />

            <SelectField
              label="Preferred time"
              name="preferredTime"
              value={form.preferredTime}
              onChange={handleChange}
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
              onChange={handleChange}
              min={minimumDate}
            />
          </FormGrid>
        </FormSection>

        <FormSection
          title="Service location"
          description={
            requiresAddress
              ? "A complete address is required for on-site service."
              : "Address information is optional unless the service method is on-site."
          }
        >
          <FormGrid>
            <TextField
              label="Street address"
              name="streetAddress"
              value={form.streetAddress}
              onChange={handleChange}
              maxLength={180}
              required={requiresAddress}
            />

            <TextField
              label="City"
              name="city"
              value={form.city}
              onChange={handleChange}
              maxLength={100}
              required={requiresAddress}
            />

            <TextField
              label="State"
              name="state"
              value={form.state}
              onChange={handleChange}
              maxLength={100}
              required={requiresAddress}
            />

            <TextField
              label="Postal code"
              name="postalCode"
              value={form.postalCode}
              onChange={handleChange}
              maxLength={10}
              required={requiresAddress}
            />
          </FormGrid>
        </FormSection>

        <FormSection
          title="Internal notes"
          description="These notes are visible only to authenticated administrators."
        >
          <TextAreaField
            label="Administrator notes"
            name="adminNotes"
            value={form.adminNotes}
            onChange={handleChange}
            maxLength={2000}
            rows={5}
          />
        </FormSection>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Link
            to="/admin/bookings"
            className="focus-ring inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-300 bg-white px-5 py-2 font-bold text-slate-700"
          >
            Cancel
          </Link>

          <button
            type="submit"
            disabled={isSubmitting}
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-6 py-2 font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? (
              <LoaderCircle className="h-5 w-5 animate-spin" />
            ) : (
              <Save className="h-5 w-5" />
            )}

            {isSubmitting ? "Creating booking..." : "Create Client Booking"}
          </button>
        </div>
      </form>

      <div className="rounded-2xl border border-brand-200 bg-brand-50 p-5">
        <div className="flex items-start gap-3">
          <CalendarPlus className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

          <p className="text-sm leading-6 text-brand-950">
            Creating this record confirms only that Romelt TechCare received and
            recorded the customer’s request. The booking remains pending until
            an administrator reviews and confirms it.
          </p>
        </div>
      </div>
    </section>
  );
}

function FormSection({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <h2 className="font-display text-xl font-extrabold text-navy-950">
        {title}
      </h2>

      <p className="mt-1 text-sm leading-6 text-slate-600">{description}</p>

      <div className="mt-6 space-y-5">{children}</div>
    </section>
  );
}

function FormGrid({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{children}</div>
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
}) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-700">
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
        className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-900"
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
      <span className="text-sm font-bold text-slate-700">
        {label} <span className="text-red-600">*</span>
      </span>

      <select
        name={name}
        value={value}
        onChange={onChange}
        required
        className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-900"
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
  minLength?: number;
  maxLength?: number;
  rows?: number;
}) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-700">
        {label}
        {required ? <span className="text-red-600"> *</span> : null}
      </span>

      <textarea
        {...textAreaProps}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-slate-900"
      />
    </label>
  );
}

type ChangeEventHandler = (
  event: ChangeEvent<
    HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  >,
) => void;

function toNullable(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}
