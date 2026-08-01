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
 * - Displays a live inquiry summary while the form is completed.
 * - Collects and validates general customer inquiries.
 * - Submits inquiries to the Spring Boot backend when enabled.
 * - Maps backend validation messages to matching form fields.
 * - Supports request cancellation when the page unmounts.
 * - Prevents duplicate submissions.
 * - Provides a development fallback when the contact API is disabled.
 * - Displays the backend reference number after successful submission.
 *
 * Real-data integration:
 * POST /api/v1/public/contact-inquiries
 *
 * Important:
 * The contact form is intended for general questions and support
 * inquiries. It does not automatically create or confirm an
 * appointment.
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
  Mail,
  MapPin,
  MessageSquareText,
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

    scrollToPageTop();
  };

  return (
    <>
      <ContactHero />

      <section className="bg-slate-50 py-8 sm:py-10 lg:py-12">
        <Container>
          <div className="mx-auto grid max-w-7xl items-start gap-5 lg:grid-cols-[280px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)]">
            <aside className="order-2 space-y-4 lg:order-1 lg:sticky lg:top-24">
              <InquirySummaryCard form={form} />

              <ContactInformationCard />

              <BusinessInformationCard />

              <BookingCalloutCard />
            </aside>

            <main className="order-1 min-w-0 lg:order-2">
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
                  className="overflow-hidden rounded-2xl border border-slate-200 bg-[#D4AF37] shadow-sm"
                >
                  <div className="border-b border-slate-200 px-5 py-5 sm:px-6">
                    <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
                      Send a Message
                    </p>

                    <h2 className="mt-1 font-display text-2xl font-black text-[#C62828]">
                      How can we help?
                    </h2>

                    <p className="mt-1 max-w-3xl text-sm leading-6 text-slate-600">
                      Tell us about your question or technology need. Fields
                      marked with an asterisk are required.
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
                      title="Your information"
                      description="Provide the best information for contacting you about this inquiry."
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
                      title="Inquiry details"
                      description="Identify the subject and any service related to your question."
                      icon={<Wrench />}
                    >
                      <div className="grid gap-4 md:grid-cols-2">
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
                            placeholder="Brief subject"
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
                              errors.serviceType
                                ? "serviceType-error"
                                : undefined
                            }
                            className={getInputClass(
                              Boolean(errors.serviceType),
                            )}
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

                      <div className="mt-4">
                        <FormField
                          label="Message"
                          name="message"
                          error={errors.message}
                          required
                        >
                          <textarea
                            id="message"
                            name="message"
                            rows={6}
                            maxLength={2_000}
                            placeholder="Describe your question, technology problem, or the assistance you need."
                            value={form.message}
                            onChange={handleTextChange}
                            aria-invalid={Boolean(errors.message)}
                            aria-describedby={
                              errors.message ? "message-error" : "message-help"
                            }
                            className={`${getInputClass(
                              Boolean(errors.message),
                            )} resize-y`}
                          />

                          <div
                            id="message-help"
                            className="mt-1.5 flex justify-between gap-4 text-[11px] text-slate-500"
                          >
                            <span>Minimum 20 characters</span>

                            <span>{form.message.length}/2,000</span>
                          </div>
                        </FormField>
                      </div>
                    </FormSection>

                    <section className="rounded-xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
                      <div className="flex items-start gap-3">
                        <Info className="mt-0.5 h-5 w-5 shrink-0 text-[#1976D2]" />

                        <div>
                          <h3 className="text-sm font-extrabold text-[#0B2545]">
                            Before sending
                          </h3>

                          <p className="mt-1 text-sm leading-6 text-slate-600">
                            Do not include passwords, payment-card information,
                            banking details, Social Security numbers, or account
                            recovery codes in your message.
                          </p>
                        </div>
                      </div>
                    </section>

                    <section
                      className={`rounded-xl border p-4 sm:p-5 ${
                        errors.consentAccepted
                          ? "border-red-300 bg-red-50"
                          : "border-slate-200 bg-white"
                      }`}
                    >
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
                          className="focus-ring mt-0.5 h-5 w-5 shrink-0 rounded border-slate-300 text-[#1976D2]"
                        />

                        <span className="text-sm leading-6 text-slate-700">
                          I confirm that the information provided is accurate. I
                          understand this form is for general inquiries and does
                          not automatically confirm an appointment.
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
                      <ShieldCheck className="h-4 w-4 shrink-0 text-[#1976D2]" />

                      <span>
                        Your message is sent securely to Romelt TechCare.
                      </span>
                    </div>

                    <button
                      type="submit"
                      disabled={isSubmitting}
                      aria-busy={isSubmitting}
                      className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-6 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#0B2545] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <Send className="h-4 w-4" />

                      {isSubmitting ? "Sending Message..." : "Send Message"}
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

function ContactHero() {
  return (
    <section className="border-b border-slate-200 bg-white">
      <Container>
        <div className="flex max-w-4xl items-start gap-4 py-9 sm:py-11 lg:py-12">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-text-[#1976D2]">
            <MessageSquareText className="h-5 w-5" aria-hidden="true" />
          </div>

          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-text-[#1976D2]">
              Contact Us
            </p>

            <h1 className="mt-1.5 font-display text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl">
              Let&apos;s discuss your technology needs
            </h1>

            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600 sm:text-base">
              Contact {businessConfig.name} about computer support, Wi-Fi,
              networking, device setup, remote assistance, appointments, or
              small-business technology services.
            </p>
          </div>
        </div>
      </Container>
    </section>
  );
}

function InquirySummaryCard({ form }: { form: ContactFormState }) {
  const serviceLabel =
    serviceTypeOptions.find((option) => option.value === form.serviceType)
      ?.label ?? form.serviceType;

  const contactLabel =
    preferredContactOptions.find(
      (option) => option.value === form.preferredContactMethod,
    )?.label ?? form.preferredContactMethod;

  const hasName = Boolean(form.fullName.trim());
  const hasSubject = Boolean(form.subject.trim());
  const hasMessage = form.message.trim().length >= 20;

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
          <MessageSquareText className="h-4 w-4" />
        </div>

        <div>
          <h2 className="font-display text-base font-extrabold text-navy-950">
            Inquiry summary
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
          value={hasName ? form.fullName.trim() : "Not added"}
          complete={hasName}
        />

        <SummaryItem
          icon={<MessageSquareText />}
          label="Subject"
          value={hasSubject ? form.subject.trim() : "Not added"}
          complete={hasSubject}
        />

        <SummaryItem
          icon={<Wrench />}
          label="Related service"
          value={serviceLabel || "General question"}
          complete={Boolean(form.serviceType)}
        />

        <SummaryItem
          icon={<Mail />}
          label="Preferred reply"
          value={contactLabel || "Not selected"}
          complete={Boolean(contactLabel)}
        />

        <SummaryItem
          icon={<Check />}
          label="Message"
          value={
            hasMessage
              ? "Ready to send"
              : `${form.message.trim().length}/20 minimum`
          }
          complete={hasMessage}
        />
      </div>
    </section>
  );
}

function SummaryItem({
  icon,
  label,
  value,
  complete,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  complete: boolean;
}) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-slate-50 px-3 py-2.5">
      <div
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg [&>svg]:h-4 [&>svg]:w-4 ${
          complete
            ? "bg-emerald-100 text-[#1976D2]"
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
      </div>
    </div>
  );
}

function ContactInformationCard() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-slate-200 p-4 shadow-sm">
      <h2 className="font-display text-base font-extrabold text-[#0B2545]">
        Contact information
      </h2>

      <div className="mt-3 space-y-3">
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
    </section>
  );
}

function BusinessInformationCard() {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="p-4">
        <div className="flex items-center gap-3">
          <Clock3 className="h-5 w-5 text-[#1976D2]" aria-hidden="true" />

          <h2 className="font-display text-base font-extrabold text-navy-950">
            Appointment availability
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

        {businessConfig.appointmentOnly ? (
          <p className="mt-3 rounded-lg bg-brand-50 px-3 py-2.5 text-xs font-semibold leading-5 text-brand-900">
            Services are currently provided by scheduled appointment.
          </p>
        ) : null}
      </div>

      <div className="border-t border-slate-200 bg-slate-50 p-4">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-5 w-5 text-[#1976D2]" aria-hidden="true" />

          <h3 className="font-display text-base font-extrabold text-navy-950">
            Protect your information
          </h3>
        </div>

        <p className="mt-2 text-xs leading-5 text-slate-600">
          Do not send passwords, Social Security numbers, payment-card details,
          banking information, or account recovery codes.
        </p>
      </div>
    </section>
  );
}

function BookingCalloutCard() {
  return (
    <section className="rounded-2xl bg-[#1976D2] not-only:p-4 text-white shadow-sm">
      <div className="flex items-center gap-3">
        <CalendarCheck2 className="h-5 w-5 text-brand-300" />

        <h2 className="font-display text-base font-extrabold">
          Ready to request service?
        </h2>
      </div>

      <p className="mt-2 text-xs leading-5 text-slate-300">
        Use the booking form when you know the assistance you need and have a
        preferred service date.
      </p>

      <Link
        to="/book"
        className="focus-ring mt-4 inline-flex min-h-10 w-full items-center justify-center rounded-lg bg-[#D4AF37] px-4 py-2 text-sm font-bold text-navy-950 transition hover:bg-[#866804]"
      >
        Book a Service
      </Link>
    </section>
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
    <span className="min-w-0">
      <span className="block text-[11px] font-extrabold uppercase tracking-wide text-slate-500">
        {label}
      </span>

      <span className="mt-0.5 block break-words text-sm font-bold text-navy-950">
        {value}
      </span>
    </span>
  );

  return (
    <div className="flex items-start gap-3 rounded-xl bg-slate-50 px-3 py-2.5">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-100 text-[#1976D2]">
        <Icon className="h-4 w-4" aria-hidden="true" />
      </div>

      {href ? (
        <a
          href={href}
          className="focus-ring min-w-0 rounded-lg transition hover:text-[#1976D2]"
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
      className="rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm sm:p-8"
    >
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-[#1976D2]">
        <CheckCircle2 className="h-8 w-8" aria-hidden="true" />
      </div>

      <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
        {isDevelopmentFallback
          ? "Development Confirmation"
          : "Message Received"}
      </p>

      <h2 className="mt-2 font-display text-2xl font-black text-navy-950 sm:text-3xl">
        Thank you, {fullName.trim()}
      </h2>

      <p className="mx-auto mt-3 max-w-2xl text-sm leading-7 text-slate-600">
        {confirmation.message}
      </p>

      {isDevelopmentFallback ? (
        <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold leading-6 text-amber-900">
          The contact API is currently disabled, so this message was not
          transmitted to the backend.
        </p>
      ) : null}

      <dl className="mx-auto mt-6 max-w-2xl rounded-xl border border-slate-200 bg-slate-50 p-4 text-left">
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
        className="focus-ring mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-[#1976D2] px-6 py-2.5 text-sm font-bold text-white transition hover:bg-brand-800"
      >
        Send Another Message
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
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-[#1976D2] [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <div>
          <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-[#1976D2]">
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

      <dd className="break-all text-right text-sm font-bold text-slate-800">
        {value}
      </dd>
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

    document
      .querySelector<HTMLElement>(`[name="${firstInvalidField}"]`)
      ?.focus();
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
