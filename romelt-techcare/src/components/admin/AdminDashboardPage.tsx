/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR DASHBOARD PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the authenticated landing page for the Romelt TechCare
 * administration portal.
 *
 * Responsibilities:
 * - Welcomes the authenticated administrator.
 * - Displays account and session information from real auth data.
 * - Clearly identifies operational modules awaiting backend APIs.
 * - Provides access to available account-security actions.
 * - Avoids displaying fabricated booking, inquiry, or revenue totals.
 *
 * Real-data integration:
 * Current administrator information is supplied by AdminAuthContext
 * and originates from:
 *
 * GET /api/v1/admin/auth/me
 *
 * Production note:
 * Operational statistics are intentionally not mocked. Dashboard
 * totals should be added only after administrator booking, inquiry,
 * customer, and reporting endpoints exist.
 * ================================================================
 */

import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  KeyRound,
  Mail,
  MessageSquareText,
  ShieldCheck,
  UserRound,
  Wrench,
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import AdminInformationCard from "@/components/admin/AdminInformationCard";
import { useAdminAuth } from "@/hooks/useAdminAuth";

interface DashboardLocationState {
  passwordChanged?: boolean;
}

export default function AdminDashboardPage() {
  const location = useLocation();

  const { administrator } = useAdminAuth();

  const locationState = location.state as DashboardLocationState | null;

  const administratorName =
    administrator?.fullName?.trim() ||
    administrator?.firstName ||
    "Administrator";

  return (
    <div className="mx-auto max-w-7xl">
      {locationState?.passwordChanged && (
        <div
          className="mb-6 flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-emerald-900"
          role="status"
          aria-live="polite"
        >
          <CheckCircle2
            className="mt-0.5 h-5 w-5 shrink-0"
            aria-hidden="true"
          />

          <div>
            <p className="font-bold">Password updated successfully</p>

            <p className="mt-1 text-sm leading-6 text-emerald-800">
              Your administrator account is now protected by the new password.
            </p>
          </div>
        </div>
      )}

      <section className="overflow-hidden rounded-3xl bg-navy-950 px-5 py-7 text-white shadow-sm sm:px-8 sm:py-9">
        <div className="flex flex-col gap-7 lg:flex-row lg:items-center lg:justify-between">
          <div className="max-w-3xl">
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-brand-300">
              Administration dashboard
            </p>

            <h1 className="mt-3 font-display text-3xl font-extrabold sm:text-4xl">
              Welcome, {administratorName}
            </h1>

            <p className="mt-4 max-w-2xl leading-7 text-slate-300">
              Your secure administrator workspace is active. Customer request
              management modules will be connected as their protected backend
              endpoints are implemented.
            </p>
          </div>

          <div className="flex shrink-0">
            <Link
              to="/admin/change-password"
              className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-white px-5 py-3 font-bold text-navy-950 transition hover:bg-brand-50"
            >
              <KeyRound className="h-5 w-5" aria-hidden="true" />
              Account security
            </Link>
          </div>
        </div>
      </section>

      <section
        className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
        aria-label="Administrator account summary"
      >
        <AccountSummaryCard
          icon={<UserRound className="h-5 w-5" aria-hidden="true" />}
          label="Administrator"
          value={administratorName}
        />

        <AccountSummaryCard
          icon={<ShieldCheck className="h-5 w-5" aria-hidden="true" />}
          label="Role"
          value={formatRole(administrator?.role)}
        />

        <AccountSummaryCard
          icon={<Mail className="h-5 w-5" aria-hidden="true" />}
          label="Email"
          value={administrator?.email ?? "Not available"}
          breakValue
        />

        <AccountSummaryCard
          icon={<Clock3 className="h-5 w-5" aria-hidden="true" />}
          label="Last login"
          value={formatDateTime(administrator?.lastLoginAt)}
        />
      </section>

      <section className="mt-8">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
            Operations
          </p>

          <h2 className="mt-2 font-display text-2xl font-extrabold text-navy-950">
            Management modules
          </h2>

          <p className="mt-2 max-w-3xl leading-7 text-slate-600">
            These workspaces will become active after their secured
            administrator APIs are added to the Spring Boot backend.
          </p>
        </div>

        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <AdminInformationCard
            icon={<CalendarDays className="h-5 w-5" aria-hidden="true" />}
            title="Booking management"
            description="Review service requests, update appointment status, assign technicians, and coordinate scheduling."
          />

          <AdminInformationCard
            icon={<MessageSquareText className="h-5 w-5" aria-hidden="true" />}
            title="Contact inquiries"
            description="Review customer messages, track responses, record follow-up activity, and close resolved inquiries."
          />

          <AdminInformationCard
            icon={<Wrench className="h-5 w-5" aria-hidden="true" />}
            title="Service management"
            description="Maintain the service catalog, pricing guidance, availability, and public service descriptions."
          />
        </div>
      </section>

      <section className="mt-8 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
              Account status
            </p>

            <h2 className="mt-2 font-display text-2xl font-extrabold text-navy-950">
              Secure administrator access
            </h2>
          </div>

          <span className="inline-flex w-fit items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-sm font-bold text-emerald-800">
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />

            {formatStatus(administrator?.status)}
          </span>
        </div>

        <dl className="mt-6 grid gap-5 border-t border-slate-200 pt-6 sm:grid-cols-2 xl:grid-cols-4">
          <AccountDetail
            term="Job title"
            description={administrator?.jobTitle?.trim() || "Not specified"}
          />

          <AccountDetail
            term="Password changed"
            description={formatDateTime(administrator?.passwordChangedAt)}
          />

          <AccountDetail
            term="Account created"
            description={formatDateTime(administrator?.createdAt)}
          />

          <AccountDetail
            term="Password action"
            description={
              administrator?.mustChangePassword
                ? "Change required"
                : "No action required"
            }
          />
        </dl>
      </section>
    </div>
  );
}

interface AccountSummaryCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  breakValue?: boolean;
}

function AccountSummaryCard({
  icon,
  label,
  value,
  breakValue = false,
}: AccountSummaryCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
          {icon}
        </span>

        <p className="text-sm font-semibold text-slate-500">{label}</p>
      </div>

      <p
        className={`mt-4 font-display text-lg font-extrabold text-navy-950 ${
          breakValue ? "break-all" : ""
        }`}
      >
        {value}
      </p>
    </div>
  );
}

interface AccountDetailProps {
  term: string;
  description: string;
}

function AccountDetail({ term, description }: AccountDetailProps) {
  return (
    <div>
      <dt className="text-sm font-semibold text-slate-500">{term}</dt>

      <dd className="mt-1 text-sm font-bold leading-6 text-slate-900">
        {description}
      </dd>
    </div>
  );
}

function formatRole(role: string | undefined): string {
  if (!role) {
    return "Administrator";
  }

  return role.toLowerCase().split("_").map(capitalize).join(" ");
}

function formatStatus(status: string | undefined): string {
  if (!status) {
    return "Active";
  }

  return status.toLowerCase().split("_").map(capitalize).join(" ");
}

function capitalize(value: string): string {
  if (!value) {
    return value;
  }

  return `${value.charAt(0).toUpperCase()}${value.slice(1)}`;
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "Not available";
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
