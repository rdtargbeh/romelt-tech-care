/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTH LOADING SCREEN
 * ================================================================
 *
 * Purpose:
 * Displays a full-page loading state while the frontend restores and
 * validates the administrator session.
 *
 * Responsibilities:
 * - Prevents protected admin content from flashing before session
 *   verification finishes.
 * - Provides an accessible loading status.
 * - Uses the Romelt TechCare visual identity.
 * ================================================================
 */

import { LoaderCircle } from "lucide-react";

export function AdminAuthLoadingScreen() {
  return (
    <div
      className="flex min-h-screen items-center justify-center bg-slate-50 px-4"
      role="status"
      aria-live="polite"
      aria-label="Verifying administrator session"
    >
      <div className="flex max-w-sm flex-col items-center text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-navy-950 text-white shadow-lg">
          <LoaderCircle className="h-8 w-8 animate-spin" aria-hidden="true" />
        </div>

        <h1 className="mt-5 font-display text-xl font-extrabold text-navy-950">
          Romelt TechCare
        </h1>

        <p className="mt-2 text-sm leading-6 text-slate-600">
          Verifying your administrator session.
        </p>
      </div>
    </div>
  );
}
