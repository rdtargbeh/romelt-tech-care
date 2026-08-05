/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING DETAIL PAGE
 * ================================================================
 *
 * Purpose:
 * Displays complete booking information and allows administrators to
 * update the booking lifecycle status.
 *
 * Responsibilities:
 * - Displays customer, service, schedule, source, and creator data.
 * - Displays private administrator notes.
 * - Updates booking status.
 * - Supports website and administrator-created bookings.
 *
 * Real-data integration:
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 * ================================================================
 */

import { type ChangeEvent, type FormEvent, useEffect, useState } from "react";
import {
  ArrowLeft,
  CalendarDays,
  CircleAlert,
  Clock3,
  LoaderCircle,
  Mail,
  MapPin,
  Phone,
  Save,
  UserRoundCog,
  Wrench,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";

import {
  getAdminBookingRequest,
  updateAdminBookingStatus,
} from "@/services/admin-customer-request.service";

import type {
  AdminBookingRequest,
  BookingRequestStatus,
} from "@/types/admin-customer-request.types";

const BOOKING_STATUSES: BookingRequestStatus[] = [
  "PENDING",
  "UNDER_REVIEW",
  "CONFIRMED",
  "COMPLETED",
  "CANCELLED",
  "DECLINED",
  "EXPIRED",
];

export default function AdminBookingDetailsPage() {
  const { bookingRequestId } = useParams();

  const [booking, setBooking] = useState<AdminBookingRequest | null>(null);

  const [selectedStatus, setSelectedStatus] =
    useState<BookingRequestStatus>("PENDING");

  const [adminNotes, setAdminNotes] = useState("");

  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadBooking() {
      if (!bookingRequestId) {
        setErrorMessage("Booking request ID is missing.");
        setIsLoading(false);
        return;
      }

      try {
        const response = await getAdminBookingRequest(
          bookingRequestId,
          controller.signal,
        );

        setBooking(response);
        setSelectedStatus(response.status);
        setAdminNotes(response.adminNotes ?? "");
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Booking request could not be loaded.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadBooking();

    return () => controller.abort();
  }, [bookingRequestId]);

  async function handleStatusUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!bookingRequestId || !booking) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);
    setIsUpdating(true);

    try {
      const updatedBooking = await updateAdminBookingStatus(bookingRequestId, {
        status: selectedStatus,
        adminNotes: toNullable(adminNotes),
      });

      setBooking(updatedBooking);
      setSelectedStatus(updatedBooking.status);
      setAdminNotes(updatedBooking.adminNotes ?? "");

      setSuccessMessage(
        `Booking status updated to ${formatLabel(updatedBooking.status)}.`,
      );
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "Booking status could not be updated.",
      );
    } finally {
      setIsUpdating(false);
    }
  }

  if (isLoading) {
    return (
      <div
        role="status"
        className="flex min-h-72 items-center justify-center gap-3 text-slate-600"
      >
        <LoaderCircle className="h-6 w-6 animate-spin" />
        Loading booking request...
      </div>
    );
  }

  if (errorMessage && !booking) {
    return (
      <section className="space-y-6">
        <BackLink />

        <ErrorMessage message={errorMessage} />
      </section>
    );
  }

  if (!booking) {
    return null;
  }

  const address = [
    booking.streetAddress,
    booking.city,
    booking.state,
    booking.postalCode,
  ]
    .filter(Boolean)
    .join(", ");

  return (
    <section className="space-y-6">
      <BackLink />

      <header className="rounded-3xl bg-navy-950 p-6 text-white shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-300">
              Booking request
            </p>

            <h1 className="mt-3 font-display text-3xl font-black">
              {booking.fullName}
            </h1>

            <p className="mt-2 text-slate-300">{booking.referenceNumber}</p>

            <p className="mt-3 text-sm font-bold text-brand-200">
              Source: {formatLabel(booking.bookingSource)}
            </p>
          </div>

          <span className="w-fit rounded-full bg-white/10 px-4 py-2 text-sm font-extrabold">
            {formatLabel(booking.status)}
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
          <DetailCard title="Service request" icon={<Wrench />}>
            <DetailRow
              label="Service"
              value={formatLabel(booking.serviceType)}
            />

            <DetailRow
              label="Service method"
              value={formatLabel(booking.serviceMethod)}
            />

            <DetailRow
              label="Device or equipment"
              value={booking.deviceType || "Not provided"}
            />

            <div className="mt-5">
              <p className="text-sm font-bold text-slate-500">
                Problem description
              </p>

              <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 leading-7 text-slate-800">
                {booking.problemDescription}
              </p>
            </div>
          </DetailCard>

          <DetailCard title="Requested schedule" icon={<CalendarDays />}>
            <DetailRow
              label="Preferred date"
              value={formatDate(booking.preferredDate)}
            />

            <DetailRow
              label="Preferred time"
              value={formatLabel(booking.preferredTime)}
            />

            <DetailRow
              label="Alternate date"
              value={
                booking.alternateDate
                  ? formatDate(booking.alternateDate)
                  : "Not provided"
              }
            />
          </DetailCard>

          <DetailCard title="Service location" icon={<MapPin />}>
            <DetailRow
              label="Address"
              value={address || "No service address provided"}
            />
          </DetailCard>

          <DetailCard title="Administrator notes" icon={<UserRoundCog />}>
            <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-4 leading-7 text-slate-800">
              {booking.adminNotes || "No administrator notes recorded."}
            </p>
          </DetailCard>
        </div>

        <div className="space-y-6">
          <DetailCard title="Update booking status" icon={<Save />}>
            <form onSubmit={handleStatusUpdate} className="space-y-5">
              <label className="block">
                <span className="text-sm font-bold text-slate-700">
                  Booking status
                </span>

                <select
                  value={selectedStatus}
                  onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                    setSelectedStatus(
                      event.target.value as BookingRequestStatus,
                    )
                  }
                  className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2"
                >
                  {BOOKING_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {formatLabel(status)}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block">
                <span className="text-sm font-bold text-slate-700">
                  Administrator notes
                </span>

                <textarea
                  value={adminNotes}
                  onChange={(event) => setAdminNotes(event.target.value)}
                  rows={5}
                  maxLength={2000}
                  className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
                />
              </label>

              <button
                type="submit"
                disabled={isUpdating}
                className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 py-2 font-extrabold text-white disabled:opacity-60"
              >
                {isUpdating ? (
                  <LoaderCircle className="h-5 w-5 animate-spin" />
                ) : (
                  <Save className="h-5 w-5" />
                )}

                {isUpdating ? "Updating..." : "Update Booking"}
              </button>
            </form>
          </DetailCard>

          <DetailCard title="Customer contact" icon={<Phone />}>
            <DetailRow label="Customer" value={booking.fullName} />

            <a
              href={`mailto:${booking.email}`}
              className="focus-ring mt-4 flex items-center gap-3 rounded-xl border border-slate-200 p-4 transition hover:border-brand-400"
            >
              <Mail className="h-5 w-5 text-brand-700" />

              <span className="break-all font-bold text-slate-800">
                {booking.email}
              </span>
            </a>

            <a
              href={`tel:${booking.phone}`}
              className="focus-ring mt-3 flex items-center gap-3 rounded-xl border border-slate-200 p-4 transition hover:border-brand-400"
            >
              <Phone className="h-5 w-5 text-brand-700" />

              <span className="font-bold text-slate-800">{booking.phone}</span>
            </a>

            <DetailRow
              label="Preferred contact"
              value={formatLabel(booking.preferredContactMethod)}
            />
          </DetailCard>

          <DetailCard title="Request record" icon={<Clock3 />}>
            <DetailRow
              label="Booking source"
              value={formatLabel(booking.bookingSource)}
            />

            <DetailRow
              label="Entered by"
              value={
                booking.createdByAdminName ||
                (booking.bookingSource === "WEBSITE"
                  ? "Customer website form"
                  : "Administrator")
              }
            />

            <DetailRow
              label="Submitted"
              value={formatDateTime(booking.submittedAt)}
            />

            <DetailRow
              label="Last updated"
              value={formatDateTime(booking.updatedAt)}
            />

            <DetailRow
              label="Website consent accepted"
              value={booking.consentAccepted ? "Yes" : "Not applicable"}
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
      to="/admin/bookings"
      className="focus-ring inline-flex items-center gap-2 rounded-lg font-bold text-slate-600 transition hover:text-brand-700"
    >
      <ArrowLeft className="h-4 w-4" />
      Back to bookings
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
        <h2 className="font-bold">Booking operation failed</h2>
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

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "long",
      }).format(date);
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

function toNullable(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}
