/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT PAGE
 * ================================================================
 *
 * Purpose:
 * Provides customers and prospective clients with direct contact
 * information and a structured way to submit general inquiries.
 *
 * Responsibilities:
 * - Displays public business contact information.
 * - Collects and validates general customer inquiries.
 * - Submits inquiries to the Spring Boot backend when enabled.
 * - Maps backend validation messages to matching form fields.
 * - Supports request cancellation when the page unmounts.
 * - Prevents duplicate submissions.
 * - Provides a development fallback when the contact API is disabled.
 * - Displays the backend reference number after successful submission.
 *
 * Real-data integration:
 * When VITE_ENABLE_CONTACT_API=true, the form submits to:
 *
 * POST /api/v1/public/contact-inquiries
 *
 * Expected backend response:
 * {
 *   "success": true,
 *   "message": "Contact inquiry submitted successfully.",
 *   "data": {
 *     "inquiryId": "optional UUID",
 *     "referenceNumber": "RTCI-2026-000001",
 *     "message": "Contact inquiry submitted successfully.",
 *     "submittedAt": "2026-07-19T18:00:00Z"
 *   }
 * }
 *
 * The current public telephone number and email address are temporary
 * and should be replaced when dedicated business contact information
 * becomes available.
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
  CheckCircle2,
  CircleAlert,
  Clock3,
  Mail,
  MapPin,
  MessageSquareText,
  Phone,
  ShieldCheck,
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
import { submitContactInquiry } from "@/services/contact.service";
import type { ContactInquiryConfirmation } from "@/types/public-request.types";

interface ContactFormState {
  fullName: string;
  email: string;
  phone: string;
  subject: string;
  serviceType: string;
  message: string;
  preferredContactMethod: string;
  consentAccepted: boolean;
}

interface ContactInquiryRequest {
  fullName: string;
  email: string;
  phone: string | null;
  subject: string;
  serviceType: string | null;
  message: string;
  preferredContactMethod: ContactMethod;
  consentAccepted: boolean;
}

type ContactMethod = "EMAIL" | "PHONE" | "TEXT";

type ContactFieldName = keyof ContactFormState;

type ContactFormErrors = Partial<Record<ContactFieldName, string>>;

interface SubmissionError {
  title: string;
  message: string;
}

const initialFormState: ContactFormState = {
  fullName: "",
  email: "",
  phone: "",
  subject: "",
  serviceType: "",
  message: "",
  preferredContactMethod: "",
  consentAccepted: false,
};

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

export function ContactPage() {
  const [form, setForm] = useState<ContactFormState>(initialFormState);

  const [errors, setErrors] = useState<ContactFormErrors>({});

  const [submissionError, setSubmissionError] =
    useState<SubmissionError | null>(null);

  const [confirmation, setConfirmation] =
    useState<ContactInquiryConfirmation | null>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const activeRequestControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      activeRequestControllerRef.current?.abort();
    };
  }, []);

  const updateField = (field: ContactFieldName, value: string | boolean) => {
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
    const field = event.target.name as ContactFieldName;

    updateField(field, event.target.value);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setSubmissionError(null);

    const validationErrors = validateContactForm(form);

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
      const request = createContactRequest(form);

      let result: ContactInquiryConfirmation;

      if (environmentConfig.enableContactApi) {
        result = await submitContactInquiry<
          ContactInquiryRequest,
          ContactInquiryConfirmation
        >(request, requestController.signal);
      } else {
        logger.info("Contact API is disabled. Using development confirmation.");

        result = createDevelopmentConfirmation();
      }

      setConfirmation(normalizeConfirmation(result));

      window.scrollTo({
        top: 0,
        left: 0,
        behavior: prefersReducedMotion() ? "auto" : "smooth",
      });
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
    logger.error("Contact inquiry submission failed.", error);

    if (isApiError(error)) {
      const backendFieldErrors = extractBackendFieldErrors(error);

      if (Object.keys(backendFieldErrors).length > 0) {
        setErrors(backendFieldErrors);

        setSubmissionError({
          title: "Review the highlighted fields",
          message:
            "Some information was rejected by the service. Correct the highlighted fields and submit the form again.",
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
      title: "Message could not be sent",
      message:
        "An unexpected problem occurred. Please try again or contact us by telephone or email.",
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

    window.scrollTo({
      top: 0,
      left: 0,
      behavior: prefersReducedMotion() ? "auto" : "smooth",
    });
  };

  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <Container>
          <div className="max-w-4xl py-16 sm:py-20">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
              <MessageSquareText className="h-7 w-7" aria-hidden="true" />
            </div>

            <p className="mt-6 text-sm font-extrabold uppercase tracking-[0.18em] text-brand-700">
              Contact Us
            </p>

            <h1 className="mt-4 font-display text-4xl font-black tracking-tight text-navy-950 sm:text-5xl">
              Let&apos;s discuss your technology needs
            </h1>

            <p className="mt-6 max-w-3xl text-lg leading-8 text-slate-600">
              Contact {businessConfig.name} with questions about computer
              support, Wi-Fi, networking, device setup, remote assistance,
              appointments, or small-business technology services.
            </p>
          </div>
        </Container>
      </section>

      <section className="bg-slate-50 py-16">
        <Container>
          <div className="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[0.42fr_1fr]">
            <aside className="space-y-6">
              <ContactInformationCard />

              <BusinessHoursCard />

              <SensitiveInformationCard />

              <BookingCalloutCard />
            </aside>

            {confirmation ? (
              <ContactConfirmation
                fullName={form.fullName}
                email={form.email}
                preferredContactMethod={form.preferredContactMethod}
                confirmation={confirmation}
                isDevelopmentFallback={!environmentConfig.enableContactApi}
                onReset={resetForm}
              />
            ) : (
              <form
                onSubmit={handleSubmit}
                noValidate
                className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
              >
                <div>
                  <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
                    Send a Message
                  </p>

                  <h2 className="mt-3 font-display text-2xl font-black text-navy-950 sm:text-3xl">
                    How can we help?
                  </h2>

                  <p className="mt-3 leading-7 text-slate-600">
                    Complete the form below. Fields marked with an asterisk are
                    required.
                  </p>
                </div>

                {submissionError ? (
                  <SubmissionErrorAlert error={submissionError} />
                ) : Object.keys(errors).length > 0 ? (
                  <ValidationAlert />
                ) : null}

                <div className="mt-8 grid gap-6 md:grid-cols-2">
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
                    hint="Optional"
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
                      aria-invalid={Boolean(errors.preferredContactMethod)}
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

                  <FormField
                    label="Subject"
                    name="subject"
                    error={errors.subject}
                    required
                  >
                    <input
                      id="subject"
                      name="subject"
                      type="text"
                      maxLength={120}
                      value={form.subject}
                      onChange={handleTextChange}
                      aria-invalid={Boolean(errors.subject)}
                      aria-describedby={
                        errors.subject ? "subject-error" : undefined
                      }
                      className={getInputClass(Boolean(errors.subject))}
                    />
                  </FormField>

                  <FormField
                    label="Related service"
                    name="serviceType"
                    error={errors.serviceType}
                    hint="Optional"
                  >
                    <select
                      id="serviceType"
                      name="serviceType"
                      value={form.serviceType}
                      onChange={handleTextChange}
                      aria-invalid={Boolean(errors.serviceType)}
                      aria-describedby={
                        errors.serviceType ? "serviceType-error" : undefined
                      }
                      className={getInputClass(Boolean(errors.serviceType))}
                    >
                      <option value="">General question</option>

                      {serviceTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </FormField>
                </div>

                <div className="mt-6">
                  <FormField
                    label="Message"
                    name="message"
                    error={errors.message}
                    required
                  >
                    <textarea
                      id="message"
                      name="message"
                      rows={8}
                      maxLength={2_000}
                      placeholder="Describe your question, technology problem, or the assistance you need."
                      value={form.message}
                      onChange={handleTextChange}
                      aria-invalid={Boolean(errors.message)}
                      aria-describedby={
                        errors.message ? "message-error" : "message-help"
                      }
                      className={getInputClass(Boolean(errors.message))}
                    />

                    <div
                      id="message-help"
                      className="mt-2 flex justify-between gap-4 text-xs text-slate-500"
                    >
                      <span>Minimum 20 characters</span>

                      <span>{form.message.length}/2,000</span>
                    </div>
                  </FormField>
                </div>

                <div className="mt-8 border-t border-slate-200 pt-8">
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
                      className="focus-ring mt-1 h-5 w-5 shrink-0 rounded border-slate-300 text-brand-700"
                    />

                    <span className="text-sm leading-7 text-slate-600">
                      I confirm that the information provided is accurate and
                      understand that this form is for general inquiries and
                      does not automatically confirm an appointment.
                    </span>
                  </label>

                  {errors.consentAccepted ? (
                    <p
                      id="consentAccepted-error"
                      className="mt-2 text-sm font-semibold text-red-700"
                    >
                      {errors.consentAccepted}
                    </p>
                  ) : null}

                  <button
                    type="submit"
                    disabled={isSubmitting}
                    aria-busy={isSubmitting}
                    className="focus-ring mt-8 inline-flex min-h-12 w-full items-center justify-center rounded-xl bg-brand-700 px-6 py-3 font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
                  >
                    {isSubmitting ? "Sending Message..." : "Send Message"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </Container>
      </section>
    </>
  );
}

function ContactInformationCard() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="font-display text-xl font-extrabold text-navy-950">
        Contact information
      </h2>

      <div className="mt-6 space-y-5">
        <ContactMethodItem
          icon={Phone}
          label="Phone"
          value={businessConfig.phoneDisplay}
          href={businessPhoneHref}
        />

        <ContactMethodItem
          icon={Mail}
          label="Email"
          value={businessConfig.email}
          href={businessEmailHref}
        />

        <ContactMethodItem
          icon={MapPin}
          label="Service area"
          value={businessConfig.serviceArea}
        />
      </div>
    </div>
  );
}

function BusinessHoursCard() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <Clock3 className="h-7 w-7 text-brand-700" aria-hidden="true" />

      <h2 className="mt-4 font-display text-xl font-extrabold text-navy-950">
        Appointment availability
      </h2>

      <div className="mt-4 space-y-3">
        {businessConfig.businessHours.map((schedule) => (
          <div
            key={schedule.day}
            className="border-b border-slate-100 pb-3 last:border-b-0 last:pb-0"
          >
            <p className="font-bold text-slate-800">{schedule.day}</p>

            <p className="mt-1 text-sm text-slate-600">{schedule.hours}</p>
          </div>
        ))}
      </div>

      {businessConfig.appointmentOnly ? (
        <p className="mt-5 rounded-xl bg-brand-50 p-4 text-sm font-semibold leading-6 text-brand-900">
          Services are currently provided by scheduled appointment.
        </p>
      ) : null}
    </div>
  );
}

function SensitiveInformationCard() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <ShieldCheck className="h-7 w-7 text-brand-700" aria-hidden="true" />

      <h2 className="mt-4 font-display text-xl font-extrabold text-navy-950">
        Protect sensitive information
      </h2>

      <p className="mt-3 text-sm leading-7 text-slate-600">
        Do not send passwords, Social Security numbers, payment-card details,
        banking information, or other highly sensitive information through this
        form.
      </p>
    </div>
  );
}

function BookingCalloutCard() {
  return (
    <div className="rounded-2xl bg-navy-950 p-6 text-white shadow-sm">
      <h2 className="font-display text-xl font-extrabold">
        Ready to request service?
      </h2>

      <p className="mt-3 text-sm leading-7 text-slate-300">
        Use the booking form when you already know the type of assistance you
        need and have a preferred date.
      </p>

      <Link
        to="/book"
        className="focus-ring mt-5 inline-flex min-h-11 items-center justify-center rounded-xl bg-white px-5 py-3 font-bold text-navy-950 transition hover:bg-brand-50"
      >
        Book a Service
      </Link>
    </div>
  );
}

interface ContactMethodItemProps {
  icon: typeof Phone;
  label: string;
  value: string;
  href?: string | null;
}

function ContactMethodItem({
  icon: Icon,
  label,
  value,
  href,
}: ContactMethodItemProps) {
  const content = (
    <span>
      <span className="block text-sm font-semibold text-slate-500">
        {label}
      </span>

      <span className="mt-1 block break-words font-bold text-navy-950">
        {value}
      </span>
    </span>
  );

  return (
    <div className="flex items-start gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
        <Icon className="h-5 w-5" aria-hidden="true" />
      </div>

      {href ? (
        <a
          href={href}
          className="focus-ring rounded-lg transition hover:text-brand-700"
        >
          {content}
        </a>
      ) : (
        content
      )}
    </div>
  );
}

interface ContactConfirmationProps {
  fullName: string;
  email: string;
  preferredContactMethod: string;
  confirmation: ContactInquiryConfirmation;
  isDevelopmentFallback: boolean;
  onReset: () => void;
}

function ContactConfirmation({
  fullName,
  email,
  preferredContactMethod,
  confirmation,
  isDevelopmentFallback,
  onReset,
}: ContactConfirmationProps) {
  const preferredContactLabel =
    preferredContactOptions.find(
      (option) => option.value === preferredContactMethod,
    )?.label ?? preferredContactMethod;

  return (
    <div
      role="status"
      aria-live="polite"
      className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm sm:p-12"
    >
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-700">
        <CheckCircle2 className="h-9 w-9" aria-hidden="true" />
      </div>

      <p className="mt-6 text-sm font-extrabold uppercase tracking-[0.18em] text-green-700">
        {isDevelopmentFallback
          ? "Development Confirmation"
          : "Message Received"}
      </p>

      <h2 className="mt-3 font-display text-3xl font-black text-navy-950 sm:text-4xl">
        Thank you, {fullName.trim()}
      </h2>

      <p className="mt-5 leading-8 text-slate-600">{confirmation.message}</p>

      {isDevelopmentFallback ? (
        <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold leading-6 text-amber-900">
          The contact API is currently disabled, so this message was not
          transmitted to the backend.
        </p>
      ) : null}

      <dl className="mt-7 rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left">
        <SummaryRow
          label="Reference number"
          value={confirmation.referenceNumber}
        />

        <SummaryRow label="Email" value={email.trim()} />

        <SummaryRow label="Preferred reply" value={preferredContactLabel} />

        <SummaryRow
          label="Submitted"
          value={formatSubmittedAt(confirmation.submittedAt)}
        />
      </dl>

      <button
        type="button"
        onClick={onReset}
        className="focus-ring mt-8 inline-flex min-h-12 items-center justify-center rounded-xl bg-brand-700 px-6 py-3 font-bold text-white transition hover:bg-brand-800"
      >
        Send Another Message
      </button>
    </div>
  );
}

function ValidationAlert() {
  return (
    <div
      role="alert"
      className="mt-8 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-6 w-6 shrink-0" aria-hidden="true" />

      <div>
        <h3 className="font-bold">Review the highlighted fields</h3>

        <p className="mt-1 text-sm leading-6">
          Some required information is missing or invalid.
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
      className="mt-8 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-6 w-6 shrink-0" aria-hidden="true" />

      <div>
        <h3 className="font-bold">{error.title}</h3>

        <p className="mt-1 text-sm leading-6">{error.message}</p>
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
      <label htmlFor={name} className="text-sm font-bold text-slate-800">
        {label}

        {required ? (
          <span aria-hidden="true" className="ml-1 text-red-600">
            *
          </span>
        ) : null}
      </label>

      {hint ? (
        <span className="ml-2 text-xs font-medium text-slate-500">{hint}</span>
      ) : null}

      <div className="mt-2">{children}</div>

      {error ? (
        <p
          id={`${name}-error`}
          className="mt-2 text-sm font-semibold text-red-700"
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
    <div className="flex items-start justify-between gap-4 border-b border-slate-200 py-3 first:pt-0 last:border-b-0 last:pb-0">
      <dt className="font-semibold text-slate-500">{label}</dt>

      <dd className="break-all text-right font-bold text-slate-800">{value}</dd>
    </div>
  );
}

function createContactRequest(form: ContactFormState): ContactInquiryRequest {
  return {
    fullName: form.fullName.trim(),
    email: form.email.trim().toLowerCase(),
    phone: normalizeOptionalValue(form.phone),
    subject: form.subject.trim(),
    serviceType: normalizeOptionalValue(form.serviceType),
    message: form.message.trim(),
    preferredContactMethod: form.preferredContactMethod as ContactMethod,
    consentAccepted: form.consentAccepted,
  };
}

function createDevelopmentConfirmation(): ContactInquiryConfirmation {
  const now = new Date();

  return {
    inquiryId: undefined,
    referenceNumber: `DEV-CONTACT-${now.getTime()}`,
    message:
      "Your contact form passed validation successfully. Enable the contact API to transmit inquiries to the backend.",
    submittedAt: now.toISOString(),
  };
}

function normalizeConfirmation(
  confirmation: ContactInquiryConfirmation,
): ContactInquiryConfirmation {
  return {
    inquiryId: confirmation.inquiryId,
    referenceNumber:
      confirmation.referenceNumber?.trim() || "REFERENCE-PENDING",
    message:
      confirmation.message?.trim() || "Your message was received successfully.",
    submittedAt: confirmation.submittedAt || new Date().toISOString(),
  };
}

function validateContactForm(form: ContactFormState): ContactFormErrors {
  const errors: ContactFormErrors = {};

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

  if (!isValidPhone(form.phone)) {
    errors.phone = "Enter a valid telephone number.";
  }

  if (form.subject.trim().length < 3) {
    errors.subject = "Enter a brief subject.";
  }

  if (form.subject.trim().length > 120) {
    errors.subject = "Subject cannot exceed 120 characters.";
  }

  if (!isContactMethod(form.preferredContactMethod)) {
    errors.preferredContactMethod = "Select how you prefer to be contacted.";
  }

  if (form.message.trim().length < 20) {
    errors.message = "Provide at least 20 characters describing your inquiry.";
  }

  if (form.message.trim().length > 2_000) {
    errors.message = "Message cannot exceed 2,000 characters.";
  }

  if (!form.consentAccepted) {
    errors.consentAccepted =
      "Confirm that the information provided is accurate.";
  }

  return errors;
}

function extractBackendFieldErrors(error: ApiError): ContactFormErrors {
  const result: ContactFormErrors = {};

  const supportedFields: ContactFieldName[] = [
    "fullName",
    "email",
    "phone",
    "subject",
    "serviceType",
    "message",
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

function focusFirstInvalidField(errors: ContactFormErrors) {
  window.requestAnimationFrame(() => {
    const firstInvalidField = Object.keys(errors)[0];

    if (!firstInvalidField) {
      return;
    }

    const firstInvalidElement = document.querySelector<HTMLElement>(
      `[name="${firstInvalidField}"]`,
    );

    firstInvalidElement?.focus();
  });
}

function getApiErrorTitle(error: ApiError): string {
  if (error.isNetworkError) {
    return "Unable to connect";
  }

  if (error.status === 429) {
    return "Too many requests";
  }

  if (error.isServerError) {
    return "Service temporarily unavailable";
  }

  return "Message could not be sent";
}

function normalizeOptionalValue(value: string): string | null {
  const normalizedValue = value.trim();

  return normalizedValue || null;
}

function isContactMethod(value: string): value is ContactMethod {
  return value === "EMAIL" || value === "PHONE" || value === "TEXT";
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function isValidPhone(value: string): boolean {
  const digits = value.replace(/\D/g, "");

  return digits.length === 0 || (digits.length >= 10 && digits.length <= 15);
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

function prefersReducedMotion(): boolean {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function getInputClass(hasError: boolean): string {
  const baseClass =
    "focus-ring min-h-12 w-full rounded-xl border bg-white px-4 py-3 outline-none transition";

  return hasError
    ? `${baseClass} border-red-400 focus:border-red-600`
    : `${baseClass} border-slate-300 focus:border-brand-600`;
}
