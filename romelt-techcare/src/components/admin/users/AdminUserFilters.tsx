/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER FILTERS
 * ================================================================
 *
 * Purpose:
 * Provides search, role, and account-status controls for the
 * Administrator Users list.
 *
 * Responsibilities:
 * - Searches administrators by identity or job title.
 * - Filters administrators by role.
 * - Filters administrators by account status.
 * - Clears all active filters.
 *
 * Real-data integration:
 * Filtering currently operates on administrator records returned by
 * GET /api/v1/admin/users.
 *
 * Future integration:
 * The same filter model can be sent to backend query parameters when
 * server-side filtering and pagination are introduced.
 * ================================================================
 */

import { RotateCcw, Search, SlidersHorizontal } from "lucide-react";
import type { ChangeEvent } from "react";

import {
  ADMIN_ROLE_OPTIONS,
  ADMIN_STATUS_OPTIONS,
  type AdminUserSearchFilters,
} from "@/types/admin-user.types";

interface AdminUserFiltersProps {
  filters: AdminUserSearchFilters;
  disabled?: boolean;
  onChange: (filters: AdminUserSearchFilters) => void;
  onReset: () => void;
}

export function AdminUserFilters({
  filters,
  disabled = false,
  onChange,
  onReset,
}: AdminUserFiltersProps) {
  function handleInputChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = event.target;

    onChange({
      ...filters,
      [name]: value,
    });
  }

  const hasActiveFilters =
    Boolean(filters.keyword.trim()) ||
    Boolean(filters.role) ||
    Boolean(filters.status);

  return (
    <section
      className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
      aria-label="Administrator user filters"
    >
      <div className="flex items-center gap-2">
        <SlidersHorizontal
          className="h-5 w-5 text-brand-700"
          aria-hidden="true"
        />

        <h2 className="font-display text-lg font-extrabold text-navy-950">
          Search and filter
        </h2>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(260px,1fr)_220px_220px_auto]">
        <div>
          <label
            htmlFor="admin-user-keyword"
            className="block text-sm font-bold text-slate-700"
          >
            Search administrators
          </label>

          <div className="relative mt-2">
            <Search
              className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />

            <input
              id="admin-user-keyword"
              name="keyword"
              type="search"
              value={filters.keyword}
              onChange={handleInputChange}
              disabled={disabled}
              placeholder="Name, email, or job title"
              className="focus-ring min-h-12 w-full rounded-xl border border-slate-300 bg-white py-3 pl-12 pr-4 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
            />
          </div>
        </div>

        <div>
          <label
            htmlFor="admin-user-role"
            className="block text-sm font-bold text-slate-700"
          >
            Role
          </label>

          <select
            id="admin-user-role"
            name="role"
            value={filters.role}
            onChange={handleInputChange}
            disabled={disabled}
            className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
          >
            <option value="">All roles</option>

            {ADMIN_ROLE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label
            htmlFor="admin-user-status"
            className="block text-sm font-bold text-slate-700"
          >
            Status
          </label>

          <select
            id="admin-user-status"
            name="status"
            value={filters.status}
            onChange={handleInputChange}
            disabled={disabled}
            className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
          >
            <option value="">All statuses</option>

            {ADMIN_STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-end">
          <button
            type="button"
            onClick={onReset}
            disabled={disabled || !hasActiveFilters}
            className="focus-ring inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 lg:w-auto"
          >
            <RotateCcw className="h-4 w-4" aria-hidden="true" />
            Reset
          </button>
        </div>
      </div>
    </section>
  );
}
