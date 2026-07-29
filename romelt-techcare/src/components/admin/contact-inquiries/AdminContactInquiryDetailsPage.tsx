/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRY DETAIL PAGE
 * ================================================================
 *
 * Purpose:
 * Displays a customer inquiry and allows administrators to update its
 * operational status.
 *
 * Real-data integration:
 * GET   /api/v1/admin/contact-inquiries/{contactInquiryId}
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
 * ================================================================
 */

import { type ChangeEvent, type FormEvent, useEffect, useState } from "react";
import {
  ArrowLeft,
  CircleAlert,
  Clock3,
  LoaderCircle,
  Mail,
  MessageSquareText,
  Phone,
  Save,
  Wrench,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";

import {
  getAdminContactInquiry,
  updateAdminContactInquiryStatus,
} from "@/services/admin-customer-request.service";

import type {
  AdminContactInquiry,
  ContactInquiryStatus,
} from "@/types/admin-customer-request.types";

const INQUIRY_STATUSES: ContactInquiryStatus[] = [
  "NEW",
  "IN_PROGRESS",
  "RESPONDED",
  "CLOSED",
  "SPAM",
];

export default function AdminContactInquiryDetailsPage() {
  const { contactInquiryId } = useParams();

  const [inquiry, setInquiry] = useState<AdminContactInquiry | null>(null);

  const [selectedStatus, setSelectedStatus] =
    useState<ContactInquiryStatus>("NEW");

  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadInquiry() {
      if (!contactInquiryId) {
        setErrorMessage("Contact inquiry ID is missing.");
        setIsLoading(false);
        return;
      }

      try {
        const response = await getAdminContactInquiry(
          contactInquiryId,
          controller.signal,
        );

        setInquiry(response);
        setSelectedStatus(response.status);
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Contact inquiry could not be loaded.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadInquiry();

    return () => controller.abort();
  }, [contactInquiryId]);

  async function handleStatusUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!contactInquiryId) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);
    setIsUpdating(true);

    try {
      const updatedInquiry = await updateAdminContactInquiryStatus(
        contactInquiryId,
        {
          status: selectedStatus,
        },
      );

      setInquiry(updatedInquiry);
      setSelectedStatus(updatedInquiry.status);

      setSuccessMessage(
        `Inquiry status updated to ${formatLabel(updatedInquiry.status)}.`,
      );
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "Contact inquiry status could not be updated.",
      );
    } finally {
      setIsUpdating(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-72 items-center justify-center gap-3 text-slate-600">
        <LoaderCircle className="h-6 w-6 animate-spin" />
        Loading contact inquiry...
      </div>
    );
  }

  if (errorMessage && !inquiry) {
    return (
      <section className="space-y-6">
        <BackLink />
        <ErrorMessage message={errorMessage} />
      </section>
    );
  }

  if (!inquiry) {
    return null;
  }

  return (
    <section className="space-y-6">
      <BackLink />

      <header className="rounded-3xl bg-navy-950 p-6 text-white shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-300">
              Contact inquiry
            </p>

            <h1 className="mt-3 font-display text-3xl font-black">
              {inquiry.subject}
            </h1>

            <p className="mt-2 text-slate-300">{inquiry.referenceNumber}</p>
          </div>

          <span className="w-fit rounded-full bg-white/10 px-4 py-2 text-sm font-extrabold">
            {formatLabel(inquiry.status)}
          </span>
        </div>
      </header>

      {errorMessage ? <ErrorMessage message={errorMessage} /> : null}

      {successMessage ? (
        <div
          role="status"
          className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 font-semibold text-emerald-900"
        >
          {successMessage}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1fr_0.7fr]">
        <div className="space-y-6">
          <DetailCard title="Customer message" icon={<MessageSquareText />}>
            <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-5 leading-8 text-slate-800">
              {inquiry.message}
            </p>
          </DetailCard>

          <DetailCard title="Related service" icon={<Wrench />}>
            <DetailRow
              label="Service"
              value={inquiry.serviceType || "General inquiry"}
            />

            <DetailRow label="Subject" value={inquiry.subject} />
          </DetailCard>
        </div>

        <div className="space-y-6">
          <DetailCard title="Update inquiry status" icon={<Save />}>
            <form onSubmit={handleStatusUpdate} className="space-y-5">
              <label className="block">
                <span className="text-sm font-bold text-slate-700">
                  Inquiry status
                </span>

                <select
                  value={selectedStatus}
                  onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                    setSelectedStatus(
                      event.target.value as ContactInquiryStatus,
                    )
                  }
                  className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2"
                >
                  {INQUIRY_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {formatLabel(status)}
                    </option>
                  ))}
                </select>
              </label>

              <button
                type="submit"
                disabled={isUpdating || selectedStatus === inquiry.status}
                className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 py-2 font-extrabold text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isUpdating ? (
                  <LoaderCircle className="h-5 w-5 animate-spin" />
                ) : (
                  <Save className="h-5 w-5" />
                )}

                {isUpdating ? "Updating..." : "Update Status"}
              </button>
            </form>
          </DetailCard>

          <DetailCard title="Customer contact" icon={<Phone />}>
            <DetailRow label="Customer" value={inquiry.fullName} />

            <a
              href={`mailto:${inquiry.email}?subject=${encodeURIComponent(
                `Re: ${inquiry.subject} — ${inquiry.referenceNumber}`,
              )}`}
              className="focus-ring mt-4 flex items-center gap-3 rounded-xl border border-slate-200 p-4 transition hover:border-brand-400"
            >
              <Mail className="h-5 w-5 shrink-0 text-brand-700" />

              <span className="break-all font-bold text-slate-800">
                {inquiry.email}
              </span>
            </a>

            {inquiry.phone ? (
              <a
                href={`tel:${inquiry.phone}`}
                className="focus-ring mt-3 flex items-center gap-3 rounded-xl border border-slate-200 p-4 transition hover:border-brand-400"
              >
                <Phone className="h-5 w-5 shrink-0 text-brand-700" />

                <span className="font-bold text-slate-800">
                  {inquiry.phone}
                </span>
              </a>
            ) : null}

            <DetailRow
              label="Preferred contact"
              value={formatLabel(inquiry.preferredContactMethod)}
            />
          </DetailCard>

          <DetailCard title="Inquiry record" icon={<Clock3 />}>
            <DetailRow
              label="Submitted"
              value={formatDateTime(inquiry.submittedAt)}
            />

            <DetailRow
              label="Last updated"
              value={formatDateTime(inquiry.updatedAt)}
            />

            <DetailRow
              label="Consent accepted"
              value={inquiry.consentAccepted ? "Yes" : "No"}
            />
          </DetailCard>
        </div>
      </div>
    </section>
  );
}

function BackLink() {
  return (
    <Link
      to="/admin/contact-inquiries"
      className="focus-ring inline-flex items-center gap-2 rounded-lg font-bold text-slate-600 transition hover:text-brand-700"
    >
      <ArrowLeft className="h-4 w-4" />
      Back to contact inquiries
    </Link>
  );
}

function ErrorMessage({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-6 w-6 shrink-0" />

      <div>
        <h2 className="font-bold">Inquiry operation failed</h2>
        <p className="mt-1 text-sm">{message}</p>
      </div>
    </div>
  );
}

function DetailCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <h2 className="font-display text-xl font-extrabold text-navy-950">
          {title}
        </h2>
      </div>

      <div className="mt-6">{children}</div>
    </article>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 border-b border-slate-100 py-3 first:pt-0 last:border-0 last:pb-0 sm:flex-row sm:justify-between sm:gap-6">
      <dt className="font-semibold text-slate-500">{label}</dt>

      <dd className="break-words font-bold text-slate-800 sm:text-right">
        {value}
      </dd>
    </div>
  );
}

function formatLabel(value: string): string {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatDateTime(value: string): string {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}
