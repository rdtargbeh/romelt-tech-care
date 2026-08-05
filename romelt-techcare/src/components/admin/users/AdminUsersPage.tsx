/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USERS PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the main Administrator User Management workspace.
 *
 * Responsibilities:
 * - Loads administrator accounts from the protected backend.
 * - Provides search, role, and status filtering.
 * - Displays responsive table and mobile card layouts.
 * - Provides navigation to create, view, and edit workflows.
 * - Displays loading, error, success, and empty states.
 * - Supports manually refreshing administrator data.
 *
 * Real-data integration:
 * Loads records through:
 *
 * GET /api/v1/admin/users
 *
 * Authorization:
 * The backend remains the final authority for SUPER_ADMIN access.
 * ================================================================
 */

import {
  CircleAlert,
  LoaderCircle,
  Plus,
  RefreshCw,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { AdminUserFilters } from "@/components/admin/users/AdminUserFilters";
import { AdminUserTable } from "@/components/admin/users/AdminUserTable";
import { ApiError } from "@/lib/api-error";
import { getAdminUsers } from "@/services/admin-user.service";
import {
  INITIAL_ADMIN_USER_FILTERS,
  type AdminUser,
  type AdminUserSearchFilters,
  type AdminUsersLocationState,
} from "@/types/admin-user.types";

export default function AdminUsersPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const [administrators, setAdministrators] = useState<AdminUser[]>([]);
  const [filters, setFilters] = useState<AdminUserSearchFilters>(
    INITIAL_ADMIN_USER_FILTERS,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const locationState = location.state as AdminUsersLocationState | null;

  const filteredAdministrators = useMemo(
    () => filterAdministrators(administrators, filters),
    [administrators, filters],
  );

  useEffect(() => {
    const controller = new AbortController();

    void loadAdministrators(controller.signal, false);

    return () => {
      controller.abort();
    };
  }, []);

  async function loadAdministrators(signal?: AbortSignal, refreshing = true) {
    if (refreshing) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }

    setErrorMessage(null);

    try {
      const response = await getAdminUsers(signal);
      setAdministrators(response);
    } catch (error) {
      if (signal?.aborted) {
        return;
      }

      setErrorMessage(resolveErrorMessage(error));
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }

  function clearSuccessMessage() {
    navigate(location.pathname, {
      replace: true,
      state: null,
    });
  }

  return (
    <div className="mx-auto max-w-7xl">
      {locationState?.successMessage && (
        <div
          className="mb-6 flex items-start justify-between gap-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-emerald-900"
          role="status"
          aria-live="polite"
        >
          <div className="flex items-start gap-3">
            <ShieldCheck
              className="mt-0.5 h-5 w-5 shrink-0"
              aria-hidden="true"
            />

            <div>
              <p className="font-bold">Administrator update completed</p>

              <p className="mt-1 text-sm leading-6 text-emerald-800">
                {locationState.successMessage}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={clearSuccessMessage}
            className="focus-ring rounded-lg px-2 py-1 text-sm font-bold text-emerald-800 transition hover:bg-emerald-100"
          >
            Dismiss
          </button>
        </div>
      )}

      <header className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
            Access management
          </p>

          <h1 className="mt-2 font-display text-3xl font-extrabold text-navy-950">
            Administrator users
          </h1>

          <p className="mt-3 max-w-3xl leading-7 text-slate-600">
            Create and maintain the accounts used to manage Romelt TechCare
            operations.
          </p>
        </div>

        <Link
          to="/admin/users/new"
          className="focus-ring inline-flex min-h-12 shrink-0 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 py-3 font-bold text-white transition hover:bg-navy-900"
        >
          <Plus className="h-5 w-5" aria-hidden="true" />
          New administrator
        </Link>
      </header>

      <section
        className="mt-6 grid gap-4 sm:grid-cols-3"
        aria-label="Administrator account summary"
      >
        <SummaryCard
          label="Total accounts"
          value={administrators.length}
          description="All administrator accounts"
        />

        <SummaryCard
          label="Active"
          value={
            administrators.filter(
              (administrator) => administrator.status === "ACTIVE",
            ).length
          }
          description="Accounts permitted to sign in"
        />

        <SummaryCard
          label="Restricted"
          value={
            administrators.filter(
              (administrator) => administrator.status !== "ACTIVE",
            ).length
          }
          description="Inactive or locked accounts"
        />
      </section>

      <div className="mt-6">
        <AdminUserFilters
          filters={filters}
          disabled={isLoading}
          onChange={setFilters}
          onReset={() =>
            setFilters({
              ...INITIAL_ADMIN_USER_FILTERS,
            })
          }
        />
      </div>

      <div className="mt-5 flex items-center justify-between gap-4">
        <p className="text-sm font-semibold text-slate-600">
          {isLoading
            ? "Loading administrators..."
            : `${filteredAdministrators.length} of ${administrators.length} administrator accounts`}
        </p>

        <button
          type="button"
          onClick={() => void loadAdministrators(undefined, true)}
          disabled={isLoading || isRefreshing}
          className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <RefreshCw
            className={`h-4 w-4 ${isRefreshing ? "animate-spin" : ""}`}
            aria-hidden="true"
          />
          Refresh
        </button>
      </div>

      <div className="mt-4">
        {isLoading ? (
          <LoadingState />
        ) : errorMessage ? (
          <ErrorState
            message={errorMessage}
            onRetry={() => void loadAdministrators(undefined, true)}
          />
        ) : (
          <AdminUserTable administrators={filteredAdministrators} />
        )}
      </div>
    </div>
  );
}

interface SummaryCardProps {
  label: string;
  value: number;
  description: string;
}

function SummaryCard({ label, value, description }: SummaryCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-500">{label}</p>

          <p className="mt-2 font-display text-3xl font-extrabold text-navy-950">
            {value}
          </p>
        </div>

        <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
          <UsersRound className="h-5 w-5" aria-hidden="true" />
        </span>
      </div>

      <p className="mt-3 text-sm text-slate-600">{description}</p>
    </div>
  );
}

function LoadingState() {
  return (
    <div
      className="flex min-h-72 items-center justify-center rounded-2xl border border-slate-200 bg-white"
      role="status"
      aria-live="polite"
    >
      <div className="text-center">
        <LoaderCircle
          className="mx-auto h-8 w-8 animate-spin text-brand-700"
          aria-hidden="true"
        />

        <p className="mt-4 font-bold text-slate-800">
          Loading administrator accounts
        </p>

        <p className="mt-1 text-sm text-slate-500">
          Retrieving protected account information.
        </p>
      </div>
    </div>
  );
}

interface ErrorStateProps {
  message: string;
  onRetry: () => void;
}

function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-8 text-center">
      <CircleAlert
        className="mx-auto h-8 w-8 text-red-700"
        aria-hidden="true"
      />

      <h2 className="mt-4 font-display text-xl font-extrabold text-red-950">
        Unable to load administrators
      </h2>

      <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-red-800">
        {message}
      </p>

      <button
        type="button"
        onClick={onRetry}
        className="focus-ring mt-5 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-red-800 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-900"
      >
        <RefreshCw className="h-4 w-4" aria-hidden="true" />
        Try again
      </button>
    </div>
  );
}

function filterAdministrators(
  administrators: AdminUser[],
  filters: AdminUserSearchFilters,
): AdminUser[] {
  const normalizedKeyword = filters.keyword.trim().toLowerCase();

  return administrators.filter((administrator) => {
    const matchesKeyword =
      !normalizedKeyword ||
      [
        administrator.fullName,
        administrator.firstName,
        administrator.lastName,
        administrator.email,
        administrator.jobTitle ?? "",
      ].some((value) => value.toLowerCase().includes(normalizedKeyword));

    const matchesRole = !filters.role || administrator.role === filters.role;

    const matchesStatus =
      !filters.status || administrator.status === filters.status;

    return matchesKeyword && matchesRole && matchesStatus;
  });
}

function resolveErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return "The administrator accounts could not be retrieved. Check the backend connection and try again.";
}
