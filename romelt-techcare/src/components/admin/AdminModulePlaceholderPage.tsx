/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN MODULE PLACEHOLDER PAGE
 * ================================================================
 *
 * Purpose:
 * Provides a temporary but functional destination for administrator
 * navigation modules whose full management interfaces are still
 * being implemented.
 *
 * Responsibilities:
 * - Keeps registered sidebar navigation links active and usable.
 * - Displays the selected module name and implementation status.
 * - Prevents unfinished modules from opening a blank page.
 *
 * Removal:
 * Replace this component route-by-route as the Bookings, Contact
 * Inquiries, Services, and Settings management pages are completed.
 * ================================================================
 */

import { ArrowLeft, Construction } from "lucide-react";
import { Link } from "react-router-dom";

interface AdminModulePlaceholderPageProps {
  title: string;
  description: string;
}

export default function AdminModulePlaceholderPage({
  title,
  description,
}: AdminModulePlaceholderPageProps) {
  return (
    <section className="mx-auto w-full max-w-5xl">
      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8 lg:p-10">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
          <Construction className="h-7 w-7" aria-hidden="true" />
        </div>

        <p className="mt-6 text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
          Administrator module
        </p>

        <h2 className="mt-2 text-2xl font-extrabold text-slate-950 sm:text-3xl">
          {title}
        </h2>

        <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 sm:text-base">
          {description}
        </p>

        <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <p className="text-sm font-semibold text-amber-900">
            This navigation link is now active. The complete management workflow
            will be added during this module’s implementation.
          </p>
        </div>

        <Link
          to="/admin"
          className="mt-8 inline-flex min-h-11 items-center gap-2 rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-950/30"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Return to dashboard
        </Link>
      </div>
    </section>
  );
}
