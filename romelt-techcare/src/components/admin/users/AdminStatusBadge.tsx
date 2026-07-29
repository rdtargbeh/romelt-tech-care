/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR STATUS BADGE
 * ================================================================
 *
 * Purpose:
 * Displays a consistent visual label for administrator account
 * statuses.
 *
 * Responsibilities:
 * - Distinguishes ACTIVE, INACTIVE, and LOCKED accounts.
 * - Provides accessible text in addition to visual styling.
 * - Supports compact use in tables, cards, and detail pages.
 *
 * Real-data integration:
 * Status values originate from administrator-management backend
 * responses.
 * ================================================================
 */

import { Ban, CheckCircle2, LockKeyhole } from "lucide-react";

import type { AdminStatus } from "@/types/admin-auth.types";

interface AdminStatusBadgeProps {
  status: AdminStatus;
}

export function AdminStatusBadge({ status }: AdminStatusBadgeProps) {
  const configuration = resolveStatusConfiguration(status);

  const Icon = configuration.icon;

  return (
    <span
      className={[
        "inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1",
        "text-xs font-bold",
        configuration.className,
      ].join(" ")}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />

      {configuration.label}
    </span>
  );
}

function resolveStatusConfiguration(status: AdminStatus) {
  switch (status) {
    case "ACTIVE":
      return {
        label: "Active",
        icon: CheckCircle2,
        className: "bg-emerald-50 text-emerald-800",
      };

    case "INACTIVE":
      return {
        label: "Inactive",
        icon: Ban,
        className: "bg-slate-100 text-slate-700",
      };

    case "LOCKED":
      return {
        label: "Locked",
        icon: LockKeyhole,
        className: "bg-red-50 text-red-800",
      };
  }
}
