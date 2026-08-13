/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMERS PAGE
 * ================================================================
 *
 * Purpose:
 * Displays and searches the reusable Romelt TechCare customer
 * directory for authenticated administrators.
 *
 * Responsibilities:
 * - Loads real customers from the Spring Boot backend.
 * - Searches by customer number, name, email, or phone.
 * - Filters customers by operational status.
 * - Supports backend pagination.
 * - Displays customer source and recent activity.
 * - Opens the complete customer profile.
 * - Provides entry to manual customer creation.
 * - Uses a compact directory-style layout for faster scanning.
 *
 * Real-data integration:
 *
 * GET /api/v1/admin/customers
 *
 * Query parameters:
 * - keyword
 * - customerStatus
 * - page
 * - size
 *
 * Navigation:
 * /admin/customers/new
 * /admin/customers/{customerId}
 *
 * Important:
 * This page uses CustomerSummaryResponse only.
 *
 * Private customer notes and other complete-profile fields are loaded
 * only by the customer detail page.
 * ================================================================
 */

import {
  Activity,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Clock3,
  Eye,
  LoaderCircle,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  UserRound,
  UsersRound,
  Wrench,
  X,
} from "lucide-react";

import { useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { getAdminCustomers } from "@/services/admin-customer.service";

import type {
  AdminCustomerSummary,
  CustomerSource,
  CustomerStatus,
} from "@/types/admin-customer.types";

import type { PageResponse } from "@/types/admin-customer-request.types";

// =====================================================================
// PAGINATION
// =====================================================================

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50] as const;

// =====================================================================
// STATUS FILTER
// =====================================================================

const CUSTOMER_STATUS_OPTIONS: Array<{
  value: CustomerStatus | "";
  label: string;
}> = [
  {
    value: "",
    label: "All statuses",
  },
  {
    value: "ACTIVE",
    label: "Active",
  },
  {
    value: "INACTIVE",
    label: "Inactive",
  },
  {
    value: "BLOCKED",
    label: "Blocked",
  },
  {
    value: "ARCHIVED",
    label: "Archived",
  },
  {
    value: "MERGED",
    label: "Merged",
  },
  {
    value: "DELETED",
    label: "Deleted",
  },
];

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCustomersPage() {
  const [pageNumber, setPageNumber] = useState(0);

  const [pageSize, setPageSize] = useState<number>(10);

  const [searchInput, setSearchInput] = useState("");

  const [appliedKeyword, setAppliedKeyword] = useState("");

  const [customerStatus, setCustomerStatus] = useState<CustomerStatus | "">("");

  const [page, setPage] = useState<PageResponse<AdminCustomerSummary> | null>(
    null,
  );

  const [isLoading, setIsLoading] = useState(true);

  const [reloadKey, setReloadKey] = useState(0);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // ===================================================================
  // LOAD CUSTOMERS
  // ===================================================================

  useEffect(() => {
    const controller = new AbortController();

    async function loadCustomers() {
      setIsLoading(true);

      setErrorMessage(null);

      try {
        const response = await getAdminCustomers({
          keyword: appliedKeyword || undefined,

          customerStatus: customerStatus || undefined,

          page: pageNumber,

          size: pageSize,

          signal: controller.signal,
        });

        setPage(response);
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "Customers could not be loaded.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadCustomers();

    return () => {
      controller.abort();
    };
  }, [pageNumber, pageSize, appliedKeyword, customerStatus, reloadKey]);

  // ===================================================================
  // SEARCH
  // ===================================================================

  function handleSearchSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setPageNumber(0);

    setAppliedKeyword(searchInput.trim());
  }

  function handleClearSearch() {
    setSearchInput("");

    setAppliedKeyword("");

    setPageNumber(0);
  }

  function handleStatusChange(value: string) {
    setCustomerStatus(value as CustomerStatus | "");

    setPageNumber(0);
  }

  function handlePageSizeChange(value: string) {
    const parsed = Number(value);

    if (
      !PAGE_SIZE_OPTIONS.includes(parsed as (typeof PAGE_SIZE_OPTIONS)[number])
    ) {
      return;
    }

    setPageSize(parsed);

    setPageNumber(0);
  }

  // ===================================================================
  // DERIVED VALUES
  // ===================================================================

  const customers = page?.content ?? [];

  const totalElements = page?.totalElements ?? 0;

  const totalPages = page?.totalPages ?? 0;

  const currentPage = page ? page.number + 1 : pageNumber + 1;

  const hasFilters = Boolean(appliedKeyword || customerStatus);

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="space-y-4">
      {/* =============================================================
       * HEADER
       * ============================================================= */}

      <header className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
            Customer management
          </p>

          <h1 className="mt-1 font-display text-2xl font-black text-navy-950 sm:text-3xl">
            Customers
          </h1>

          <p className="mt-1.5 max-w-3xl text-sm leading-6 text-slate-600">
            Search and manage reusable customer profiles created from bookings
            or entered directly by administrators.
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            onClick={() => setReloadKey((current) => current + 1)}
            disabled={isLoading}
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
          >
            <RefreshCw
              className={["h-4 w-4", isLoading ? "animate-spin" : ""].join(" ")}
            />
            Refresh
          </button>

          <Link
            to="/admin/customers/new"
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-navy-950 px-4 py-2 text-sm font-bold text-white transition hover:bg-navy-900"
          >
            <Plus className="h-4 w-4" />
            New customer
          </Link>
        </div>
      </header>

      {/* =============================================================
       * SUMMARY
       * ============================================================= */}

      <div className="grid gap-2 sm:grid-cols-3">
        <SummaryCard
          icon={<UsersRound />}
          label="Matching customers"
          value={totalElements.toLocaleString("en-US")}
        />

        <SummaryCard
          icon={<Search />}
          label="Search"
          value={appliedKeyword ? `"${appliedKeyword}"` : "All customers"}
        />

        <SummaryCard
          icon={<Activity />}
          label="Status"
          value={
            customerStatus ? formatEnumLabel(customerStatus) : "All statuses"
          }
        />
      </div>

      {/* =============================================================
       * FILTERS
       * ============================================================= */}

      <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
        <div className="grid gap-2 lg:grid-cols-[minmax(0,1fr)_180px_auto]">
          <form onSubmit={handleSearchSubmit} className="relative">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />

            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Search customer number, name, email, or phone"
              className="focus-ring min-h-10 w-full rounded-lg border border-slate-300 bg-white py-2 pl-9 pr-9 text-sm text-slate-900 placeholder:text-slate-400"
            />

            {searchInput ? (
              <button
                type="button"
                onClick={handleClearSearch}
                className="focus-ring absolute right-1.5 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                aria-label="Clear search"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            ) : null}
          </form>

          <select
            value={customerStatus}
            onChange={(event) => handleStatusChange(event.target.value)}
            className="focus-ring min-h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700"
          >
            {CUSTOMER_STATUS_OPTIONS.map((option) => (
              <option key={option.value || "ALL"} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={() => {
              setPageNumber(0);

              setAppliedKeyword(searchInput.trim());
            }}
            className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-brand-700 px-4 py-2 text-sm font-bold text-white transition hover:bg-brand-800"
          >
            <Search className="h-4 w-4" />
            Search
          </button>
        </div>

        {hasFilters ? (
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            <span className="text-[11px] font-bold uppercase tracking-wide text-slate-500">
              Active filters:
            </span>

            {appliedKeyword ? (
              <FilterChip
                label={`Search: ${appliedKeyword}`}
                onRemove={handleClearSearch}
              />
            ) : null}

            {customerStatus ? (
              <FilterChip
                label={`Status: ${formatEnumLabel(customerStatus)}`}
                onRemove={() => {
                  setCustomerStatus("");

                  setPageNumber(0);
                }}
              />
            ) : null}
          </div>
        ) : null}
      </div>

      {/* =============================================================
       * ERROR
       * ============================================================= */}

      {errorMessage ? (
        <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-3.5 text-red-900">
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />

          <div>
            <p className="text-sm font-bold">Customers could not be loaded</p>

            <p className="mt-1 text-sm leading-6">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * LOADING
       * ============================================================= */}

      {isLoading && !page ? (
        <div className="flex min-h-52 items-center justify-center rounded-xl border border-slate-200 bg-white">
          <div className="text-center">
            <LoaderCircle
              className="mx-auto h-7 w-7 animate-spin text-brand-700"
              aria-hidden="true"
            />

            <p className="mt-3 text-sm font-semibold text-slate-600">
              Loading customers...
            </p>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * EMPTY STATE
       * ============================================================= */}

      {!isLoading && !errorMessage && customers.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white px-6 py-10 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
            <UsersRound className="h-6 w-6" />
          </div>

          <h2 className="mt-3 font-display text-lg font-extrabold text-navy-950">
            No customers found
          </h2>

          <p className="mx-auto mt-1.5 max-w-lg text-sm leading-6 text-slate-600">
            {hasFilters
              ? "No customer records match the current search or status filter."
              : "Customer profiles will appear here when customers submit bookings or administrators create them manually."}
          </p>

          {hasFilters ? (
            <button
              type="button"
              onClick={() => {
                setSearchInput("");

                setAppliedKeyword("");

                setCustomerStatus("");

                setPageNumber(0);
              }}
              className="focus-ring mt-4 inline-flex min-h-10 items-center justify-center rounded-lg border border-slate-300 bg-white px-4 text-sm font-bold text-slate-700 hover:bg-slate-50"
            >
              Clear filters
            </button>
          ) : null}
        </div>
      ) : null}

      {/* =============================================================
       * CUSTOMER LIST
       * ============================================================= */}

      {customers.length > 0 ? (
        <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          {customers.map((customer, index) => (
            <CustomerRow
              key={customer.customerId}
              customer={customer}
              showBorder={index < customers.length - 1}
            />
          ))}
        </div>
      ) : null}

      {/* =============================================================
       * PAGINATION
       * ============================================================= */}

      {page && page.totalElements > 0 ? (
        <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white px-3.5 py-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-sm text-slate-600">
              Page{" "}
              <span className="font-bold text-slate-900">{currentPage}</span> of{" "}
              <span className="font-bold text-slate-900">
                {Math.max(totalPages, 1)}
              </span>
            </p>

            <span className="hidden h-4 w-px bg-slate-300 sm:block" />

            <p className="text-sm text-slate-500">
              {totalElements.toLocaleString("en-US")}{" "}
              {totalElements === 1 ? "customer" : "customers"}
            </p>

            <label className="flex items-center gap-2 text-sm text-slate-600">
              Show
              <select
                value={pageSize}
                onChange={(event) => handlePageSizeChange(event.target.value)}
                className="focus-ring min-h-8 rounded-md border border-slate-300 bg-white px-2 text-sm font-semibold text-slate-800"
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="flex items-center justify-between gap-2 sm:justify-end">
            <button
              type="button"
              onClick={() =>
                setPageNumber((current) => Math.max(0, current - 1))
              }
              disabled={isLoading || page.first}
              className="focus-ring inline-flex min-h-9 items-center justify-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </button>

            <button
              type="button"
              onClick={() => setPageNumber((current) => current + 1)}
              disabled={isLoading || page.last}
              className="focus-ring inline-flex min-h-9 items-center justify-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}

// =====================================================================
// CUSTOMER ROW
// =====================================================================

interface CustomerRowProps {
  customer: AdminCustomerSummary;

  showBorder: boolean;
}

function CustomerRow({ customer, showBorder }: CustomerRowProps) {
  return (
    <article
      className={[
        "group px-3.5 py-3 transition hover:bg-slate-50 sm:px-4",
        showBorder ? "border-b border-slate-200" : "",
      ].join(" ")}
    >
      <div className="grid gap-3 xl:grid-cols-[minmax(220px,1.4fr)_minmax(170px,1fr)_minmax(130px,0.8fr)_minmax(145px,0.9fr)_minmax(145px,0.9fr)_auto] xl:items-center">
        {/* ===========================================================
         * CUSTOMER IDENTITY
         * =========================================================== */}

        <div className="flex min-w-0 items-start gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
            <UserRound className="h-4 w-4" />
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-1.5">
              <Link
                to={`/admin/customers/${customer.customerId}`}
                className="focus-ring truncate rounded-sm font-display text-sm font-extrabold text-navy-950 hover:text-brand-700 sm:text-base"
              >
                {customer.displayName}
              </Link>

              <CustomerStatusBadge status={customer.customerStatus} />

              <CustomerSourceBadge source={customer.customerSource} />
            </div>

            <p className="mt-0.5 truncate font-mono text-[11px] font-semibold text-slate-500">
              {customer.customerNumber}
            </p>

            {customer.preferredName ? (
              <p className="mt-0.5 truncate text-[11px] text-slate-500">
                Preferred:{" "}
                <span className="font-semibold text-slate-700">
                  {customer.preferredName}
                </span>
              </p>
            ) : null}
          </div>
        </div>

        {/* ===========================================================
         * EMAIL
         * =========================================================== */}

        <CompactDataItem
          icon={<Mail />}
          label="Email"
          value={customer.primaryEmail || "Not provided"}
        />

        {/* ===========================================================
         * PHONE
         * =========================================================== */}

        <CompactDataItem
          icon={<Phone />}
          label="Phone"
          value={customer.primaryPhone || "Not provided"}
        />

        {/* ===========================================================
         * LAST BOOKING
         * =========================================================== */}

        <CompactDataItem
          icon={<CalendarDays />}
          label="Last booking"
          value={formatDateTimeCompact(customer.lastBookingAt)}
        />

        {/* ===========================================================
         * LAST SERVICE
         * =========================================================== */}

        <CompactDataItem
          icon={<Wrench />}
          label="Last service"
          value={formatDateTimeCompact(customer.lastServiceCompletedAt)}
        />

        {/* ===========================================================
         * ACTION
         * =========================================================== */}

        <div className="flex items-center justify-between gap-3 border-t border-slate-100 pt-2 xl:block xl:border-0 xl:pt-0">
          <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-slate-500 xl:hidden">
            <span className="inline-flex items-center gap-1">
              <Clock3 className="h-3 w-3" />

              {formatDateTimeCompact(customer.lastActivityAt)}
            </span>

            {customer.preferredContactMethod ? (
              <span>
                Contact:{" "}
                <strong className="font-semibold text-slate-700">
                  {formatEnumLabel(customer.preferredContactMethod)}
                </strong>
              </span>
            ) : null}
          </div>

          <Link
            to={`/admin/customers/${customer.customerId}`}
            className="focus-ring inline-flex min-h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-slate-300 bg-white px-2.5 text-xs font-bold text-slate-700 transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-800"
          >
            <Eye className="h-3.5 w-3.5" />
            View
          </Link>
        </div>
      </div>

      {/* =============================================================
       * DESKTOP SECONDARY META
       * ============================================================= */}

      <div className="mt-1.5 hidden items-center gap-x-4 pl-12 text-[11px] text-slate-500 xl:flex">
        <span className="inline-flex items-center gap-1">
          <Clock3 className="h-3 w-3" />
          Last activity:{" "}
          <strong className="font-semibold text-slate-700">
            {formatDateTimeCompact(customer.lastActivityAt)}
          </strong>
        </span>

        {customer.preferredContactMethod ? (
          <span>
            Preferred contact:{" "}
            <strong className="font-semibold text-slate-700">
              {formatEnumLabel(customer.preferredContactMethod)}
            </strong>
          </span>
        ) : null}
      </div>
    </article>
  );
}

// =====================================================================
// COMPACT DATA ITEM
// =====================================================================

interface CompactDataItemProps {
  icon: React.ReactNode;

  label: string;

  value: string;
}

function CompactDataItem({ icon, label, value }: CompactDataItemProps) {
  return (
    <div className="min-w-0">
      <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-slate-400">
        <span className="[&>svg]:h-3 [&>svg]:w-3">{icon}</span>

        {label}
      </div>

      <p className="mt-0.5 truncate text-xs font-semibold text-slate-800 sm:text-sm">
        {value}
      </p>
    </div>
  );
}

// =====================================================================
// SUMMARY CARD
// =====================================================================

interface SummaryCardProps {
  icon: React.ReactNode;

  label: string;

  value: string;
}

function SummaryCard({ icon, label, value }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3.5 py-3 shadow-sm">
      <div className="flex items-center gap-2.5">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-700 [&>svg]:h-4 [&>svg]:w-4">
          {icon}
        </div>

        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-0.5 truncate font-display text-sm font-extrabold text-navy-950">
            {value}
          </p>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// STATUS BADGE
// =====================================================================

function CustomerStatusBadge({ status }: { status: CustomerStatus }) {
  const classes = resolveStatusClasses(status);

  return (
    <span
      className={[
        "inline-flex rounded-full px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-wide",
        classes,
      ].join(" ")}
    >
      {formatEnumLabel(status)}
    </span>
  );
}

// =====================================================================
// SOURCE BADGE
// =====================================================================

function CustomerSourceBadge({ source }: { source: CustomerSource }) {
  return (
    <span className="inline-flex rounded-full bg-slate-100 px-2 py-0.5 text-[9px] font-bold text-slate-600">
      {formatEnumLabel(source)}
    </span>
  );
}

// =====================================================================
// FILTER CHIP
// =====================================================================

function FilterChip({
  label,
  onRemove,
}: {
  label: string;

  onRemove: () => void;
}) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-2.5 py-1 text-[11px] font-bold text-brand-800">
      {label}

      <button
        type="button"
        onClick={onRemove}
        className="focus-ring flex h-4 w-4 items-center justify-center rounded-full hover:bg-brand-100"
        aria-label={`Remove ${label}`}
      >
        <X className="h-2.5 w-2.5" />
      </button>
    </span>
  );
}

// =====================================================================
// STATUS STYLES
// =====================================================================

function resolveStatusClasses(status: CustomerStatus): string {
  switch (status) {
    case "ACTIVE":
      return "bg-emerald-100 text-emerald-800";

    case "INACTIVE":
      return "bg-slate-200 text-slate-700";

    case "BLOCKED":
      return "bg-red-100 text-red-800";

    case "ARCHIVED":
      return "bg-amber-100 text-amber-800";

    case "MERGED":
      return "bg-violet-100 text-violet-800";

    case "DELETED":
      return "bg-rose-100 text-rose-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

// =====================================================================
// FORMATTERS
// =====================================================================

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDateTimeCompact(value: string | null): string {
  if (!value) {
    return "No activity";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}
