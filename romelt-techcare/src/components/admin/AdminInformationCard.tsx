/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN INFORMATION CARD
 * ================================================================
 *
 * Purpose:
 * Provides a reusable information card for administrator dashboard
 * modules and operational summaries.
 *
 * Responsibilities:
 * - Displays an icon, title, and description.
 * - Keeps dashboard card presentation separate from AdminLayout.
 * - Supports reuse across administrator pages.
 * ================================================================
 */

import type { ReactNode } from "react";

interface AdminInformationCardProps {
  icon: ReactNode;
  title: string;
  description: string;
}

export default function AdminInformationCard({
  icon,
  title,
  description,
}: AdminInformationCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700">
        {icon}
      </div>

      <h2 className="mt-4 text-lg font-extrabold text-slate-950">{title}</h2>

      <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
    </div>
  );
}
