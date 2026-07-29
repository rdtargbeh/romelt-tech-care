/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUESTS PAGE
 * ================================================================
 *
 * Purpose:
 * Displays all customer booking requests, including website bookings
 * and bookings entered by administrators for clients.
 *
 * Responsibilities:
 * - Loads real booking records from the backend.
 * - Displays booking source, status, customer, and schedule data.
 * - Supports pagination and refreshing.
 * - Provides the Book for Client action.
 * - Opens the complete booking detail page.
 *
 * Real-data integration:
 * GET /api/v1/admin/booking-requests
 * ================================================================
 */

import { useEffect, useState } from "react";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Eye,
  LoaderCircle,
  Plus,
  RefreshCw,
} from "lucide-react";
import { Link } from "react-router-dom";

import { getAdminBookingRequests } from "@/services/admin-customer-request.service";

import type {
  AdminBookingRequest,
  PageResponse,
} from "@/types/admin-customer-request.types";

const PAGE_SIZE = 10;

export default function AdminBookingsPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<PageResponse<AdminBookingRequest> | null>(
    null,
  );

  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadBookings() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const response = await getAdminBookingRequests(
          pageNumber,
          PAGE_SIZE,
          controller.signal,
        );

        setPage(response);
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        setErrorMessage(
          error instanceof Error
            ? error.message
            : "Booking requests could not be loaded.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadBookings();

    return () => controller.abort();
  }, [pageNumber, reloadKey]);

  return (
    <section className="space-y-6">
      <header className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer service requests
          </p>

          <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
            Bookings
          </h1>

          <p className="mt-2 max-w-3xl leading-7 text-slate-600">
            Review website requests and create bookings for customers who call,
            email, or visit Romelt TechCare.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <button
            type="button"
            onClick={() => setReloadKey((current) => current + 1)}
            disabled={isLoading}
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 font-bold text-slate-700 transition hover:border-brand-400 hover:text-brand-700 disabled:opacity-60"
          >
            <RefreshCw
              className={`h-4 w-4 ${isLoading ? "animate-spin" : ""}`}
              aria-hidden="true"
            />
            Refresh
          </button>

          <Link
            to="/admin/bookings/new"
            className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 py-2 font-extrabold text-white transition hover:bg-brand-800"
          >
            <Plus className="h-5 w-5" />
            Book for Client
          </Link>
        </div>
      </header>

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

          <div>
            <p className="font-bold">Bookings could not be loaded</p>
            <p className="mt-1 text-sm">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isLoading ? (
          <div
            role="status"
            className="flex min-h-64 items-center justify-center gap-3 text-slate-600"
          >
            <LoaderCircle className="h-6 w-6 animate-spin" />
            Loading booking requests...
          </div>
        ) : !page || page.content.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center px-6 text-center">
            <CalendarDays className="h-10 w-10 text-slate-400" />

            <h2 className="mt-4 font-display text-xl font-extrabold text-navy-950">
              No booking requests
            </h2>

            <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
              Website bookings and bookings created by administrators will
              appear here.
            </p>

            <Link
              to="/admin/bookings/new"
              className="focus-ring mt-5 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 py-2 font-extrabold text-white"
            >
              <Plus className="h-5 w-5" />
              Book for Client
            </Link>
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <TableHeading>Reference</TableHeading>
                    <TableHeading>Customer</TableHeading>
                    <TableHeading>Service</TableHeading>
                    <TableHeading>Source</TableHeading>
                    <TableHeading>Date</TableHeading>
                    <TableHeading>Status</TableHeading>
                    <TableHeading alignRight>Action</TableHeading>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100 bg-white">
                  {page.content.map((booking) => (
                    <tr
                      key={booking.bookingRequestId}
                      className="transition hover:bg-slate-50"
                    >
                      <TableCell>
                        <p className="font-bold text-navy-950">
                          {booking.referenceNumber}
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          {formatDateTime(booking.submittedAt)}
                        </p>
                      </TableCell>

                      <TableCell>
                        <p className="font-semibold text-slate-900">
                          {booking.fullName}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          {booking.email}
                        </p>
                      </TableCell>

                      <TableCell>
                        <p className="font-semibold text-slate-800">
                          {formatLabel(booking.serviceType)}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          {formatLabel(booking.serviceMethod)}
                        </p>
                      </TableCell>

                      <TableCell>
                        <SourceBadge source={booking.bookingSource} />
                      </TableCell>

                      <TableCell>
                        <p className="font-semibold text-slate-800">
                          {formatDate(booking.preferredDate)}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          {formatLabel(booking.preferredTime)}
                        </p>
                      </TableCell>

                      <TableCell>
                        <StatusBadge status={booking.status} />
                      </TableCell>

                      <TableCell alignRight>
                        <Link
                          to={`/admin/bookings/${booking.bookingRequestId}`}
                          className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold text-slate-700 transition hover:border-brand-500 hover:text-brand-700"
                        >
                          <Eye className="h-4 w-4" />
                          View
                        </Link>
                      </TableCell>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-200 md:hidden">
              {page.content.map((booking) => (
                <Link
                  key={booking.bookingRequestId}
                  to={`/admin/bookings/${booking.bookingRequestId}`}
                  className="block p-5 transition hover:bg-slate-50"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-bold text-navy-950">
                        {booking.fullName}
                      </p>

                      <p className="mt-1 text-sm text-slate-500">
                        {booking.referenceNumber}
                      </p>
                    </div>

                    <StatusBadge status={booking.status} />
                  </div>

                  <div className="mt-3">
                    <SourceBadge source={booking.bookingSource} />
                  </div>

                  <dl className="mt-4 grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <dt className="font-semibold text-slate-500">Service</dt>

                      <dd className="mt-1 font-bold text-slate-800">
                        {formatLabel(booking.serviceType)}
                      </dd>
                    </div>

                    <div>
                      <dt className="font-semibold text-slate-500">Date</dt>

                      <dd className="mt-1 font-bold text-slate-800">
                        {formatDate(booking.preferredDate)}
                      </dd>
                    </div>
                  </dl>
                </Link>
              ))}
            </div>
          </>
        )}

        {page && page.totalElements > 0 ? (
          <div className="flex flex-col gap-3 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-slate-600">
              Page <strong>{page.number + 1}</strong> of{" "}
              <strong>{Math.max(page.totalPages, 1)}</strong> ·{" "}
              {page.totalElements} total
            </p>

            <div className="flex gap-2">
              <button
                type="button"
                disabled={page.first || isLoading}
                onClick={() =>
                  setPageNumber((current) => Math.max(0, current - 1))
                }
                className="focus-ring inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold text-slate-700 disabled:opacity-40"
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </button>

              <button
                type="button"
                disabled={page.last || isLoading}
                onClick={() => setPageNumber((current) => current + 1)}
                className="focus-ring inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold text-slate-700 disabled:opacity-40"
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}

function TableHeading({
  children,
  alignRight = false,
}: {
  children: React.ReactNode;
  alignRight?: boolean;
}) {
  return (
    <th
      className={`px-5 py-4 text-xs font-extrabold uppercase tracking-wider text-slate-500 ${
        alignRight ? "text-right" : "text-left"
      }`}
    >
      {children}
    </th>
  );
}

function TableCell({
  children,
  alignRight = false,
}: {
  children: React.ReactNode;
  alignRight?: boolean;
}) {
  return (
    <td
      className={`px-5 py-4 align-middle ${
        alignRight ? "text-right" : "text-left"
      }`}
    >
      {children}
    </td>
  );
}

function SourceBadge({ source }: { source: string }) {
  return (
    <span className="inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-extrabold text-slate-700">
      {formatLabel(source)}
    </span>
  );
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span className="inline-flex rounded-full bg-brand-50 px-3 py-1 text-xs font-extrabold text-brand-800">
      {formatLabel(status)}
    </span>
  );
}

function formatLabel(value: string | null | undefined): string {
  if (!value) {
    return "Not provided";
  }

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
        dateStyle: "medium",
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
