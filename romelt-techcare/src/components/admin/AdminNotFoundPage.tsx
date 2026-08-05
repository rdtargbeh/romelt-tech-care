/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR NOT FOUND PAGE
 * ================================================================
 *
 * Purpose:
 * Provides a secure administrator-specific fallback page when an
 * authenticated user visits an unknown administration route.
 *
 * Responsibilities:
 * - Keeps unknown admin URLs inside the administrator experience.
 * - Prevents an admin route error from rendering the public 404 page.
 * - Provides navigation back to the administrator dashboard.
 *
 * Real-data integration:
 * No backend request is required. Access to this page remains
 * controlled by the administrator route guards.
 * ================================================================
 */

import { ArrowLeft, LayoutDashboard, ShieldAlert } from "lucide-react";
import { Link } from "react-router-dom";

export default function AdminNotFoundPage() {
  return (
    <section className="mx-auto flex min-h-[65vh] max-w-3xl items-center justify-center">
      <div className="w-full rounded-3xl border border-slate-200 bg-white px-6 py-10 text-center shadow-sm sm:px-10 sm:py-14">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
          <ShieldAlert className="h-8 w-8" aria-hidden="true" />
        </div>

        <p className="mt-6 text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
          Administrator portal
        </p>

        <h1 className="mt-3 font-display text-3xl font-extrabold text-navy-950 sm:text-4xl">
          Page not found
        </h1>

        <p className="mx-auto mt-4 max-w-xl leading-7 text-slate-600">
          The administrator page you requested does not exist or is not
          currently available.
        </p>

        <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <Link
            to="/admin"
            className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 py-3 font-bold text-white transition hover:bg-navy-900"
          >
            <LayoutDashboard className="h-5 w-5" aria-hidden="true" />
            Return to dashboard
          </Link>

          <Link
            to="/"
            className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 font-bold text-slate-700 transition hover:bg-slate-50"
          >
            <ArrowLeft className="h-5 w-5" aria-hidden="true" />
            Public website
          </Link>
        </div>
      </div>
    </section>
  );
}
