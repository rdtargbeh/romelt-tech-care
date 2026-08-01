/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BOOKING PAGE
 * ================================================================
 *
 * Purpose:
 * Allows customers to request a technology service appointment.
 *
 * Responsibilities:
 * - Collects customer and service-request information.
 * - Presents the booking form in compact, organized sections.
 * - Displays a live booking summary while the customer completes it.
 * - Validates booking information before submission.
 * - Prevents requests for past dates.
 * - Requires address information for on-site service.
 * - Submits booking requests to the Spring Boot backend when enabled.
 * - Uses a development confirmation when the booking API is disabled.
 * - Maps backend validation errors to matching form fields.
 * - Prevents duplicate submissions.
 * - Cancels an active request when the page unmounts.
 * - Displays the backend reference number after submission.
 *
 * Real-data integration:
 * POST /api/v1/public/booking-requests
 *
 * Important:
 * A submitted request is not a confirmed appointment. Romelt
 * TechCare must review availability and contact the customer.
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
  CalendarCheck2,
  Check,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Info,
  Laptop,
  Mail,
  MapPin,
  Phone,
  Send,
  ShieldCheck,
  UserRound,
  Wrench,
} from "lucide-react";
import { Link } from "react-router";

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

interface BookingFormState {
  fullName: string;
  email: string;
  phone: string;
  serviceType: string;
  serviceMethod: string;
  preferredDate: string;
  preferredTime: string;
  alternateDate: string;
  streetAddress: string;
  city: string;
  state: string;
  postalCode: string;
  deviceType: string;
  problemDescription: string;
  preferredContactMethod: string;
  consentAccepted: boolean;
}

interface BookingRequestPayload {
  fullName: string;
  email: string;
  phone: string;
  serviceType: string;
  serviceMethod: ServiceMethod;
  preferredDate: string;
  preferredTime: PreferredTime;
  alternateDate: string | null;
  streetAddress: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  deviceType: string | null;
  problemDescription: string;
  preferredContactMethod: ContactMethod;
  consentAccepted: boolean;
}

type BookingFieldName = keyof BookingFormState;

type BookingFormErrors = Partial<Record<BookingFieldName, string>>;

type ServiceMethod = "REMOTE" | "ON_SITE" | "DROP_OFF" | "NOT_SURE";

type PreferredTime = "MORNING" | "AFTERNOON" | "EVENING" | "FLEXIBLE";

type ContactMethod = "EMAIL" | "PHONE" | "TEXT";

interface SubmissionError {
  title: string;
  message: string;
}

const initialFormState: BookingFormState = {
  fullName: "",
  email: "",
  phone: "",
  serviceType: "",
  serviceMethod: "",
  preferredDate: "",
  preferredTime: "",
  alternateDate: "",
  streetAddress: "",
  city: "",
  state: "Iowa",
  postalCode: "",
  deviceType: "",
  problemDescription: "",
  preferredContactMethod: "",
  consentAccepted: false,
};

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

export function BookingPage() {
  const [form, setForm] = useState<BookingFormState>(initialFormState);

  const [errors, setErrors] = useState<BookingFormErrors>({});

  const [submissionError, setSubmissionError] =
    useState<SubmissionError | null>(null);

  const [confirmation, setConfirmation] =
    useState<BookingRequestConfirmation | null>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const activeRequestControllerRef = useRef<AbortController | null>(null);

  const minimumBookingDate = getMinimumBookingDate();

  const requiresServiceAddress = form.serviceMethod === "ON_SITE";

  useEffect(() => {
    return () => {
      activeRequestControllerRef.current?.abort();
    };
  }, []);

  const updateField = (field: BookingFieldName, value: string | boolean) => {
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
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => {
    const field = event.target.name as BookingFieldName;

    updateField(field, event.target.value);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setSubmissionError(null);

    const validationErrors = validateBookingForm(form);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      focusFirstInvalidField(validationErrors);
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

        focusFirstInvalidField(backendFieldErrors);

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
          <div className="mx-auto grid max-w-7xl items-start gap-5 lg:grid-cols-[270px_minmax(0,1fr)] xl:grid-cols-[290px_minmax(0,1fr)]">
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
                <form
                  onSubmit={handleSubmit}
                  noValidate
                  className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                >
                  <div className="border-b border-slate-200 px-5 py-5 sm:px-6">
                    <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
                      Booking Request
                    </p>

                    <h2 className="mt-1 font-display text-2xl font-black text-navy-950">
                      Tell us how we can help
                    </h2>

                    <p className="mt-1 text-sm leading-6 text-slate-600">
                      Complete the sections below. Fields marked with an
                      asterisk are required.
                    </p>
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

                  <div className="space-y-4 p-4 sm:p-5 lg:p-6">
                    <FormSection
                      number={1}
                      title="Customer information"
                      description="Provide the best information for contacting you about this request."
                      icon={<UserRound />}
                    >
                      <div className="grid gap-4 md:grid-cols-2">
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.fullName)}
                            aria-describedby={
                              errors.fullName ? "fullName-error" : undefined
                            }
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.email)}
                            aria-describedby={
                              errors.email ? "email-error" : undefined
                            }
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
                            maxLength={30}
                            placeholder="(515) 555-1234"
                            value={form.phone}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.phone)}
                            aria-describedby={
                              errors.phone ? "phone-error" : undefined
                            }
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(
                              errors.preferredContactMethod,
                            )}
                            aria-describedby={
                              errors.preferredContactMethod
                                ? "preferredContactMethod-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.preferredContactMethod),
                            )}
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

                    <FormSection
                      number={2}
                      title="Service information"
                      description="Select the service and support method that best match your request."
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.serviceType)}
                            aria-describedby={
                              errors.serviceType
                                ? "serviceType-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.serviceType),
                            )}
                          >
                            <option value="">Select a service</option>

                            {serviceTypeOptions.map((option) => (
                              <option key={option.value} value={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </select>
                        </FormField>

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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.deviceType)}
                            aria-describedby={
                              errors.deviceType ? "deviceType-error" : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.deviceType),
                            )}
                          />
                        </FormField>
                      </div>

                      <fieldset className="mt-4">
                        <legend className="text-xs font-extrabold text-slate-800">
                          Preferred service method
                          <span
                            aria-hidden="true"
                            className="ml-1 text-red-600"
                          >
                            *
                          </span>
                        </legend>

                        <div className="mt-2 grid gap-3 md:grid-cols-2">
                          {serviceMethodOptions.map((option) => {
                            const isSelected =
                              form.serviceMethod === option.value;

                            return (
                              <label
                                key={option.value}
                                className={[
                                  "cursor-pointer rounded-xl border px-4 py-3 transition",
                                  isSelected
                                    ? "border-brand-600 bg-brand-50 shadow-sm"
                                    : "border-slate-200 bg-white hover:border-brand-300 hover:bg-slate-50",
                                ].join(" ")}
                              >
                                <span className="flex items-start gap-3">
                                  <input
                                    name="serviceMethod"
                                    type="radio"
                                    value={option.value}
                                    checked={isSelected}
                                    onChange={handleTextChange}
                                    aria-invalid={Boolean(errors.serviceMethod)}
                                    className="focus-ring mt-1 h-4 w-4 shrink-0"
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
                          <p
                            id="serviceMethod-error"
                            className="mt-2 text-sm font-semibold text-red-700"
                          >
                            {errors.serviceMethod}
                          </p>
                        ) : null}
                      </fieldset>

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
                            rows={5}
                            maxLength={2_000}
                            placeholder="Describe the symptoms, error messages, when the problem began, and anything already attempted."
                            value={form.problemDescription}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.problemDescription)}
                            aria-describedby={
                              errors.problemDescription
                                ? "problemDescription-error"
                                : "problemDescription-help"
                            }
                            className={`${getInputClass(
                              Boolean(errors.problemDescription),
                            )} resize-y`}
                          />

                          <div
                            id="problemDescription-help"
                            className="mt-1.5 flex justify-between gap-4 text-[11px] text-slate-500"
                          >
                            <span>Minimum 20 characters</span>

                            <span>
                              {form.problemDescription.length}
                              /2,000
                            </span>
                          </div>
                        </FormField>
                      </div>
                    </FormSection>

                    <FormSection
                      number={3}
                      title="Requested schedule"
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.preferredDate)}
                            aria-describedby={
                              errors.preferredDate
                                ? "preferredDate-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.preferredDate),
                            )}
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.preferredTime)}
                            aria-describedby={
                              errors.preferredTime
                                ? "preferredTime-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.preferredTime),
                            )}
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
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.alternateDate)}
                            aria-describedby={
                              errors.alternateDate
                                ? "alternateDate-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.alternateDate),
                            )}
                          />
                        </FormField>
                      </div>
                    </FormSection>

                    <FormSection
                      number={4}
                      title="Service location"
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
                            Street address, city, state, and postal code are
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
                              autoComplete="street-address"
                              maxLength={180}
                              placeholder="Street address"
                              value={form.streetAddress}
                              onChange={handleTextChange}
                              aria-invalid={Boolean(errors.streetAddress)}
                              aria-describedby={
                                errors.streetAddress
                                  ? "streetAddress-error"
                                  : undefined
                              }
                              className={getInputClass(
                                Boolean(errors.streetAddress),
                              )}
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
                            placeholder="City"
                            value={form.city}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.city)}
                            aria-describedby={
                              errors.city ? "city-error" : undefined
                            }
                            className={getInputClass(Boolean(errors.city))}
                          />
                        </FormField>

                        <FormField
                          label="State"
                          name="state"
                          error={errors.state}
                          required={requiresServiceAddress}
                        >
                          <input
                            id="state"
                            name="state"
                            type="text"
                            autoComplete="address-level1"
                            maxLength={100}
                            value={form.state}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.state)}
                            aria-describedby={
                              errors.state ? "state-error" : undefined
                            }
                            className={getInputClass(Boolean(errors.state))}
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
                            inputMode="numeric"
                            autoComplete="postal-code"
                            maxLength={10}
                            placeholder="50309"
                            value={form.postalCode}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.postalCode)}
                            aria-describedby={
                              errors.postalCode ? "postalCode-error" : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.postalCode),
                            )}
                          />
                        </FormField>
                      </div>
                    </FormSection>

                    <section className="rounded-xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
                      <label className="flex cursor-pointer items-start gap-3">
                        <input
                          name="consentAccepted"
                          type="checkbox"
                          checked={form.consentAccepted}
                          onChange={(event) =>
                            updateField("consentAccepted", event.target.checked)
                          }
                          aria-invalid={Boolean(errors.consentAccepted)}
                          aria-describedby={
                            errors.consentAccepted
                              ? "consentAccepted-error"
                              : undefined
                          }
                          className="focus-ring mt-0.5 h-5 w-5 shrink-0 rounded border-slate-300 text-brand-700"
                        />

                        <span className="text-sm leading-6 text-slate-700">
                          I confirm that the information is accurate. I
                          understand this is a service request and the
                          appointment is not confirmed until Romelt TechCare
                          contacts me.
                        </span>
                      </label>

                      {errors.consentAccepted ? (
                        <p
                          id="consentAccepted-error"
                          className="mt-2 pl-8 text-sm font-semibold text-red-700"
                        >
                          {errors.consentAccepted}
                        </p>
                      ) : null}
                    </section>
                  </div>

                  <div className="flex flex-col gap-3 border-t border-slate-200 bg-white px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <ShieldCheck className="h-4 w-4 text-brand-700" />

                      <span>
                        Never submit passwords or financial account information.
                      </span>
                    </div>

                    <button
                      type="submit"
                      disabled={isSubmitting}
                      aria-busy={isSubmitting}
                      className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-6 py-2.5 text-sm font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <Send className="h-4 w-4" />

                      {isSubmitting
                        ? "Submitting Request..."
                        : "Submit Booking Request"}
                    </button>
                  </div>
                </form>
              )}
            </main>
          </div>
        </Container>
      </section>
    </>
  );
}

function BookingHero() {
  return (
    <section className="border-b border-slate-200 bg-white">
      <Container>
        <div className="flex max-w-4xl items-start gap-4 py-9 sm:py-11 lg:py-12">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
            <CalendarCheck2 className="h-5 w-5" aria-hidden="true" />
          </div>

          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
              Request Service
            </p>

            <h1 className="mt-1.5 font-display text-3xl font-black tracking-tight text-navy-950 sm:text-4xl">
              Book technology support
            </h1>

            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600 sm:text-base">
              Tell us what assistance you need and when you would prefer
              service. Submitting this form creates a booking request, not a
              confirmed appointment.
            </p>
          </div>
        </div>
      </Container>
    </section>
  );
}

function BookingSummaryCard({ form }: { form: BookingFormState }) {
  const serviceLabel =
    serviceTypeOptions.find((option) => option.value === form.serviceType)
      ?.label ?? form.serviceType;

  const methodLabel =
    serviceMethodOptions.find((option) => option.value === form.serviceMethod)
      ?.label ?? form.serviceMethod;

  const hasCustomer = Boolean(form.fullName.trim());

  const hasService = Boolean(form.serviceType);

  const hasSchedule = Boolean(form.preferredDate);

  const address = [form.streetAddress, form.city, form.state, form.postalCode]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
          <CalendarCheck2 className="h-4 w-4" />
        </div>

        <div>
          <h2 className="font-display text-base font-extrabold text-navy-950">
            Booking summary
          </h2>

          <p className="text-xs text-slate-500">
            Updates as you complete the form
          </p>
        </div>
      </div>

      <div className="mt-4 space-y-3">
        <SummaryItem
          icon={<UserRound />}
          label="Customer"
          value={hasCustomer ? form.fullName.trim() : "Not added"}
          complete={hasCustomer}
        />

        <SummaryItem
          icon={<Wrench />}
          label="Service"
          value={hasService ? serviceLabel : "Not selected"}
          secondaryValue={methodLabel || undefined}
          complete={hasService}
        />

        <SummaryItem
          icon={<Clock3 />}
          label="Schedule"
          value={hasSchedule ? formatDate(form.preferredDate) : "Not selected"}
          secondaryValue={
            form.preferredTime ? formatEnumLabel(form.preferredTime) : undefined
          }
          complete={hasSchedule}
        />

        <SummaryItem
          icon={<MapPin />}
          label="Location"
          value={
            address ||
            (form.serviceMethod === "ON_SITE"
              ? "Address required"
              : "Not required")
          }
          complete={form.serviceMethod !== "ON_SITE" || Boolean(address)}
        />
      </div>
    </section>
  );
}

function SummaryItem({
  icon,
  label,
  value,
  secondaryValue,
  complete,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  secondaryValue?: string;
  complete: boolean;
}) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-slate-50 px-3 py-2.5">
      <div
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg [&>svg]:h-4 [&>svg]:w-4 ${
          complete
            ? "bg-emerald-100 text-emerald-700"
            : "bg-slate-200 text-slate-500"
        }`}
      >
        {complete ? <Check /> : icon}
      </div>

      <div className="min-w-0">
        <p className="text-[11px] font-extrabold uppercase tracking-wide text-slate-500">
          {label}
        </p>

        <p className="truncate text-sm font-bold text-slate-800">{value}</p>

        {secondaryValue ? (
          <p className="truncate text-xs text-slate-500">{secondaryValue}</p>
        ) : null}
      </div>
    </div>
  );
}

function AvailabilityCard() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <Clock3 className="h-5 w-5 text-brand-700" aria-hidden="true" />

        <h2 className="font-display text-base font-extrabold text-navy-950">
          Service availability
        </h2>
      </div>

      <div className="mt-3 divide-y divide-slate-100">
        {businessConfig.businessHours.map((schedule) => (
          <div
            key={schedule.day}
            className="flex items-start justify-between gap-4 py-2.5 first:pt-0 last:pb-0"
          >
            <p className="text-sm font-bold text-slate-800">{schedule.day}</p>

            <p className="text-right text-xs leading-5 text-slate-600">
              {schedule.hours}
            </p>
          </div>
        ))}
      </div>

      <p className="mt-3 rounded-lg bg-brand-50 px-3 py-2.5 text-xs font-semibold leading-5 text-brand-900">
        Requested dates remain pending until confirmed.
      </p>
    </section>
  );
}

function SafetyAndContactCard() {
  const hasPhone =
    Boolean(businessPhoneHref) && Boolean(businessConfig.phoneDisplay);

  const hasEmail = Boolean(businessEmailHref) && Boolean(businessConfig.email);

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="p-4">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-5 w-5 text-brand-700" aria-hidden="true" />

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

  const serviceMethodLabel =
    serviceMethodOptions.find((option) => option.value === serviceMethod)
      ?.label ?? serviceMethod;

  return (
    <div
      role="status"
      aria-live="polite"
      className="rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm sm:p-8"
    >
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
        <CheckCircle2 className="h-8 w-8" aria-hidden="true" />
      </div>

      <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.16em] text-emerald-700">
        {isDevelopmentFallback
          ? "Development Confirmation"
          : "Request Received"}
      </p>

      <h2 className="mt-2 font-display text-2xl font-black text-navy-950 sm:text-3xl">
        Thank you, {fullName.trim()}
      </h2>

      <p className="mx-auto mt-3 max-w-2xl text-sm leading-7 text-slate-600">
        {confirmation.message}
      </p>

      <p className="mt-3 text-sm font-semibold text-slate-700">
        This request is pending and is not yet a confirmed appointment.
      </p>

      {isDevelopmentFallback ? (
        <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold leading-6 text-amber-900">
          The booking API is currently disabled, so this request was not
          transmitted to the backend.
        </p>
      ) : null}

      <dl className="mx-auto mt-6 max-w-2xl rounded-xl border border-slate-200 bg-slate-50 p-4 text-left">
        <SummaryRow
          label="Reference number"
          value={confirmation.referenceNumber}
        />

        <SummaryRow label="Email" value={email.trim()} />

        <SummaryRow label="Service" value={serviceLabel} />

        <SummaryRow label="Service method" value={serviceMethodLabel} />

        <SummaryRow
          label="Requested date"
          value={formatDate(confirmation.requestedDate)}
        />

        <SummaryRow
          label="Requested time"
          value={formatEnumLabel(confirmation.requestedTime)}
        />

        <SummaryRow
          label="Status"
          value={formatEnumLabel(confirmation.status ?? "PENDING")}
        />

        <SummaryRow
          label="Submitted"
          value={formatSubmittedAt(confirmation.submittedAt)}
        />
      </dl>

      <button
        type="button"
        onClick={onReset}
        className="focus-ring mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-brand-700 px-6 py-2.5 text-sm font-bold text-white transition hover:bg-brand-800"
      >
        Submit Another Request
      </button>
    </div>
  );
}

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
      <div className="flex items-start gap-3">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <div>
          <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-brand-700">
            Section {number}
          </p>

          <h3 className="font-display text-lg font-extrabold text-navy-950">
            {title}
          </h3>

          <p className="mt-0.5 text-xs leading-5 text-slate-600">
            {description}
          </p>
        </div>
      </div>

      <div className="mt-4">{children}</div>
    </section>
  );
}

function ValidationAlert() {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />

      <div>
        <h3 className="text-sm font-bold">Review the highlighted fields</h3>

        <p className="mt-0.5 text-sm leading-5">
          Some required booking information is missing or invalid.
        </p>
      </div>
    </div>
  );
}

interface SubmissionErrorAlertProps {
  error: SubmissionError;
}

function SubmissionErrorAlert({ error }: SubmissionErrorAlertProps) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />

      <div>
        <h3 className="text-sm font-bold">{error.title}</h3>

        <p className="mt-0.5 text-sm leading-5">{error.message}</p>
      </div>
    </div>
  );
}

interface FormFieldProps {
  label: string;
  name: string;
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
      </label>

      {hint ? (
        <span className="ml-2 text-[11px] font-medium text-slate-500">
          {hint}
        </span>
      ) : null}

      <div className="mt-1.5">{children}</div>

      {error ? (
        <p
          id={`${name}-error`}
          className="mt-1.5 text-xs font-semibold text-red-700"
        >
          {error}
        </p>
      ) : null}
    </div>
  );
}

interface SummaryRowProps {
  label: string;
  value: string;
}

function SummaryRow({ label, value }: SummaryRowProps) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-slate-200 py-2.5 first:pt-0 last:border-b-0 last:pb-0">
      <dt className="text-sm font-semibold text-slate-500">{label}</dt>

      <dd className="break-words text-right text-sm font-bold text-slate-800">
        {value}
      </dd>
    </div>
  );
}

function createBookingRequest(form: BookingFormState): BookingRequestPayload {
  return {
    fullName: form.fullName.trim(),
    email: form.email.trim().toLowerCase(),
    phone: form.phone.trim(),
    serviceType: form.serviceType,
    serviceMethod: form.serviceMethod as ServiceMethod,
    preferredDate: form.preferredDate,
    preferredTime: form.preferredTime as PreferredTime,
    alternateDate: normalizeOptionalValue(form.alternateDate),
    streetAddress: normalizeOptionalValue(form.streetAddress),
    city: normalizeOptionalValue(form.city),
    state: normalizeOptionalValue(form.state),
    postalCode: normalizeOptionalValue(form.postalCode),
    deviceType: normalizeOptionalValue(form.deviceType),
    problemDescription: form.problemDescription.trim(),
    preferredContactMethod: form.preferredContactMethod as ContactMethod,
    consentAccepted: form.consentAccepted,
  };
}

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

function validateBookingForm(form: BookingFormState): BookingFormErrors {
  const errors: BookingFormErrors = {};

  if (form.fullName.trim().length < 2) {
    errors.fullName = "Enter your full name.";
  }

  if (form.fullName.trim().length > 120) {
    errors.fullName = "Full name cannot exceed 120 characters.";
  }

  if (!isValidEmail(form.email.trim())) {
    errors.email = "Enter a valid email address.";
  }

  if (form.email.trim().length > 254) {
    errors.email = "Email address cannot exceed 254 characters.";
  }

  if (!isValidRequiredPhone(form.phone)) {
    errors.phone = "Enter a valid telephone number.";
  }

  if (!form.serviceType) {
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

  if (form.problemDescription.trim().length < 20) {
    errors.problemDescription =
      "Provide at least 20 characters describing the requested service.";
  }

  if (form.problemDescription.trim().length > 2_000) {
    errors.problemDescription = "Description cannot exceed 2,000 characters.";
  }

  if (form.deviceType.trim().length > 120) {
    errors.deviceType = "Device type cannot exceed 120 characters.";
  }

  if (!isContactMethod(form.preferredContactMethod)) {
    errors.preferredContactMethod = "Select how you prefer to be contacted.";
  }

  if (form.serviceMethod === "ON_SITE") {
    if (form.streetAddress.trim().length < 4) {
      errors.streetAddress = "Enter the on-site service address.";
    }

    if (form.city.trim().length < 2) {
      errors.city = "Enter the service city.";
    }

    if (form.state.trim().length < 2) {
      errors.state = "Enter the service state.";
    }

    if (!isValidPostalCode(form.postalCode)) {
      errors.postalCode = "Enter a valid postal code.";
    }
  } else if (form.postalCode && !isValidPostalCode(form.postalCode)) {
    errors.postalCode = "Enter a valid postal code.";
  }

  if (!form.consentAccepted) {
    errors.consentAccepted =
      "Confirm that you understand this is a booking request.";
  }

  return errors;
}

function extractBackendFieldErrors(error: ApiError): BookingFormErrors {
  const result: BookingFormErrors = {};

  const supportedFields: BookingFieldName[] = [
    "fullName",
    "email",
    "phone",
    "serviceType",
    "serviceMethod",
    "preferredDate",
    "preferredTime",
    "alternateDate",
    "streetAddress",
    "city",
    "state",
    "postalCode",
    "deviceType",
    "problemDescription",
    "preferredContactMethod",
    "consentAccepted",
  ];

  supportedFields.forEach((field) => {
    const message = error.getFieldMessage(field);

    if (message) {
      result[field] = message;
    }
  });

  return result;
}

function focusFirstInvalidField(errors: BookingFormErrors) {
  window.requestAnimationFrame(() => {
    const firstInvalidField = Object.keys(errors)[0];

    if (!firstInvalidField) {
      return;
    }

    document
      .querySelector<HTMLElement>(`[name="${firstInvalidField}"]`)
      ?.focus();
  });
}

function getApiErrorTitle(error: ApiError): string {
  if (error.isNetworkError) {
    return "Unable to connect";
  }

  if (error.status === 409) {
    return "Requested time is unavailable";
  }

  if (error.status === 429) {
    return "Too many requests";
  }

  if (error.isServerError) {
    return "Service temporarily unavailable";
  }

  return "Booking request could not be sent";
}

function normalizeOptionalValue(value: string): string | null {
  const normalizedValue = value.trim();

  return normalizedValue || null;
}

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

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function isValidRequiredPhone(value: string): boolean {
  const digits = value.replace(/\D/g, "");

  return digits.length >= 10 && digits.length <= 15;
}

function isValidPostalCode(value: string): boolean {
  return /^\d{5}(?:-\d{4})?$/.test(value.trim());
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
  const baseClass =
    "focus-ring min-h-10 w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 outline-none transition placeholder:text-slate-400";

  return hasError
    ? `${baseClass} border-red-400 focus:border-red-600`
    : `${baseClass} border-slate-300 focus:border-brand-600`;
}
