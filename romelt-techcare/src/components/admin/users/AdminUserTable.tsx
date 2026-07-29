/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER TABLE
 * ================================================================
 *
 * Purpose:
 * Displays administrator accounts in responsive desktop and mobile
 * presentations.
 *
 * Responsibilities:
 * - Displays administrator identity, role, status, and login data.
 * - Provides View and Edit navigation.
 * - Supports row double-click navigation to account details.
 * - Provides a mobile card presentation.
 * - Displays an empty result state.
 *
 * Real-data integration:
 * Administrator records are supplied by AdminUsersPage from:
 *
 * GET /api/v1/admin/users
 * ================================================================
 */

import { ArrowRight, Edit3, Eye, ShieldCheck, UserRound } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

import { AdminStatusBadge } from "@/components/admin/users/AdminStatusBadge";
import type { AdminUser } from "@/types/admin-user.types";

interface AdminUserTableProps {
  administrators: AdminUser[];
}

export function AdminUserTable({ administrators }: AdminUserTableProps) {
  const navigate = useNavigate();

  if (administrators.length === 0) {
    return <AdminUsersEmptyState />;
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[920px] border-collapse">
          <thead className="bg-slate-50">
            <tr className="border-b border-slate-200">
              <TableHeader>Administrator</TableHeader>
              <TableHeader>Job title</TableHeader>
              <TableHeader>Role</TableHeader>
              <TableHeader>Status</TableHeader>
              <TableHeader>Last login</TableHeader>
              <TableHeader alignRight>Actions</TableHeader>
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-200">
            {administrators.map((administrator) => (
              <tr
                key={administrator.adminUserId}
                onDoubleClick={() =>
                  navigate(`/admin/users/${administrator.adminUserId}`)
                }
                className="cursor-pointer transition hover:bg-slate-50"
                title="Double-click to view administrator details"
              >
                <td className="px-5 py-4">
                  <AdministratorIdentity administrator={administrator} />
                </td>

                <td className="px-5 py-4 text-sm text-slate-700">
                  {administrator.jobTitle?.trim() || "Not specified"}
                </td>

                <td className="px-5 py-4">
                  <span className="inline-flex rounded-full bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-800">
                    {formatEnumValue(administrator.role)}
                  </span>
                </td>

                <td className="px-5 py-4">
                  <AdminStatusBadge status={administrator.status} />
                </td>

                <td className="px-5 py-4 text-sm text-slate-600">
                  {formatDateTime(administrator.lastLoginAt)}
                </td>

                <td className="px-5 py-4">
                  <div className="flex justify-end gap-2">
                    <Link
                      to={`/admin/users/${administrator.adminUserId}`}
                      className="focus-ring inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-800"
                      aria-label={`View ${administrator.fullName}`}
                    >
                      <Eye className="h-4 w-4" aria-hidden="true" />
                    </Link>

                    <Link
                      to={`/admin/users/${administrator.adminUserId}/edit`}
                      className="focus-ring inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-800"
                      aria-label={`Edit ${administrator.fullName}`}
                    >
                      <Edit3 className="h-4 w-4" aria-hidden="true" />
                    </Link>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="divide-y divide-slate-200 md:hidden">
        {administrators.map((administrator) => (
          <article key={administrator.adminUserId} className="p-4">
            <div className="flex items-start justify-between gap-3">
              <AdministratorIdentity administrator={administrator} />

              <AdminStatusBadge status={administrator.status} />
            </div>

            <dl className="mt-4 grid grid-cols-2 gap-4 rounded-xl bg-slate-50 p-4">
              <MobileDetail
                term="Role"
                description={formatEnumValue(administrator.role)}
              />

              <MobileDetail
                term="Job title"
                description={administrator.jobTitle?.trim() || "Not specified"}
              />

              <MobileDetail
                term="Last login"
                description={formatDateTime(administrator.lastLoginAt)}
                wide
              />
            </dl>

            <div className="mt-4 grid grid-cols-2 gap-3">
              <Link
                to={`/admin/users/${administrator.adminUserId}`}
                className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50"
              >
                <Eye className="h-4 w-4" aria-hidden="true" />
                View
              </Link>

              <Link
                to={`/admin/users/${administrator.adminUserId}/edit`}
                className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-navy-950 px-3 py-2 text-sm font-bold text-white transition hover:bg-navy-900"
              >
                <Edit3 className="h-4 w-4" aria-hidden="true" />
                Edit
              </Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

interface TableHeaderProps {
  children: React.ReactNode;
  alignRight?: boolean;
}

function TableHeader({ children, alignRight = false }: TableHeaderProps) {
  return (
    <th
      scope="col"
      className={[
        "px-5 py-3 text-xs font-bold uppercase tracking-wider text-slate-500",
        alignRight ? "text-right" : "text-left",
      ].join(" ")}
    >
      {children}
    </th>
  );
}

interface AdministratorIdentityProps {
  administrator: AdminUser;
}

function AdministratorIdentity({ administrator }: AdministratorIdentityProps) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-navy-950 text-sm font-extrabold text-white">
        {createInitials(administrator.fullName)}
      </span>

      <div className="min-w-0">
        <p className="truncate text-sm font-bold text-slate-900">
          {administrator.fullName}
        </p>

        <p className="mt-0.5 truncate text-xs text-slate-500">
          {administrator.email}
        </p>

        {administrator.mustChangePassword && (
          <p className="mt-1 flex items-center gap-1 text-xs font-semibold text-amber-700">
            <ShieldCheck className="h-3.5 w-3.5" aria-hidden="true" />
            Password change required
          </p>
        )}
      </div>
    </div>
  );
}

interface MobileDetailProps {
  term: string;
  description: string;
  wide?: boolean;
}

function MobileDetail({ term, description, wide = false }: MobileDetailProps) {
  return (
    <div className={wide ? "col-span-2" : ""}>
      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {term}
      </dt>

      <dd className="mt-1 break-words text-sm font-bold text-slate-800">
        {description}
      </dd>
    </div>
  );
}

function AdminUsersEmptyState() {
  return (
    <section className="rounded-2xl border border-dashed border-slate-300 bg-white px-5 py-12 text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-600">
        <UserRound className="h-7 w-7" aria-hidden="true" />
      </div>

      <h2 className="mt-5 font-display text-xl font-extrabold text-navy-950">
        No administrators found
      </h2>

      <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-600">
        No administrator account matches the current search and filter
        selections.
      </p>

      <Link
        to="/admin/users/new"
        className="focus-ring mt-6 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-navy-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-navy-900"
      >
        Create administrator
        <ArrowRight className="h-4 w-4" aria-hidden="true" />
      </Link>
    </section>
  );
}

function createInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);

  if (words.length === 0) {
    return "AD";
  }

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return `${words[0][0]}${words.at(-1)?.[0] ?? ""}`.toUpperCase();
}

function formatEnumValue(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
    .join(" ");
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "Never";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
