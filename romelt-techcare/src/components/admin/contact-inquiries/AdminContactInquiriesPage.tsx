/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRIES PAGE
 * ================================================================
 *
 * Purpose:
 * Displays customer messages submitted through the public contact
 * form.
 *
 * Real-data integration:
 * GET /api/v1/admin/contact-inquiries
 * ================================================================
 */

import { useEffect, useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Eye,
  LoaderCircle,
  MessageSquareText,
  RefreshCw,
} from "lucide-react";
import { Link } from "react-router-dom";

import { getAdminContactInquiries } from "@/services/admin-customer-request.service";
import type {
  AdminContactInquiry,
  PageResponse,
} from "@/types/admin-customer-request.types";

const PAGE_SIZE = 10;

export default function AdminContactInquiriesPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<PageResponse<AdminContactInquiry> | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadInquiries() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const response = await getAdminContactInquiries(
          pageNumber,
          PAGE_SIZE,
          controller.signal,
        );

        setPage(response);
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Contact inquiries could not be loaded.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadInquiries();

    return () => controller.abort();
  }, [pageNumber, reloadKey]);

  return (
    <section className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer communication
          </p>

          <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
            Contact inquiries
          </h1>

          <p className="mt-2 max-w-3xl leading-7 text-slate-600">
            Read and respond to customer messages submitted through the Romelt
            TechCare contact form.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setReloadKey((current) => current + 1)}
          disabled={isLoading}
          className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 font-bold text-slate-700"
        >
          <RefreshCw className={`h-4 w-4 ${isLoading ? "animate-spin" : ""}`} />
          Refresh
        </button>
      </header>

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />
          <p>{errorMessage}</p>
        </div>
      ) : null}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isLoading ? (
          <div className="flex min-h-64 items-center justify-center gap-3 text-slate-600">
            <LoaderCircle className="h-6 w-6 animate-spin" />
            Loading contact inquiries...
          </div>
        ) : !page || page.content.length === 0 ? (
          <div className="flex min-h-64 flex-col items-center justify-center px-6 text-center">
            <MessageSquareText className="h-10 w-10 text-slate-400" />

            <h2 className="mt-4 font-display text-xl font-extrabold text-navy-950">
              No contact inquiries
            </h2>

            <p className="mt-2 text-sm text-slate-600">
              New customer messages will appear here.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-200">
            {page.content.map((inquiry) => (
              <Link
                key={inquiry.contactInquiryId}
                to={`/admin/contact-inquiries/${inquiry.contactInquiryId}`}
                className="block p-5 transition hover:bg-slate-50 sm:p-6"
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-display text-lg font-extrabold text-navy-950">
                        {inquiry.subject}
                      </p>

                      <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-extrabold text-brand-800">
                        {formatLabel(inquiry.status)}
                      </span>
                    </div>

                    <p className="mt-2 font-semibold text-slate-800">
                      {inquiry.fullName}
                    </p>

                    <p className="mt-1 break-all text-sm text-slate-500">
                      {inquiry.email}
                    </p>

                    <p className="mt-3 line-clamp-2 text-sm leading-6 text-slate-600">
                      {inquiry.message}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center justify-between gap-4 sm:flex-col sm:items-end">
                    <div className="text-sm text-slate-500 sm:text-right">
                      <p>{inquiry.referenceNumber}</p>
                      <p className="mt-1">
                        {formatDateTime(inquiry.submittedAt)}
                      </p>
                    </div>

                    <span className="inline-flex items-center gap-2 font-bold text-brand-700">
                      <Eye className="h-4 w-4" />
                      View
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
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
                className="focus-ring inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold disabled:opacity-40"
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </button>

              <button
                type="button"
                disabled={page.last || isLoading}
                onClick={() => setPageNumber((current) => current + 1)}
                className="focus-ring inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold disabled:opacity-40"
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
