/**
 * ================================================================
 * ROMELT TECHCARE — NOT FOUND PAGE
 * ================================================================
 *
 * Purpose:
 * Handles website routes that do not match an existing page.
 *
 * Responsibilities:
 * - Informs visitors that the page could not be found.
 * - Provides a safe path back to the homepage.
 * ================================================================
 */

import { ArrowLeft } from "lucide-react";
import { Link } from "react-router";

export function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-16">
      <div className="max-w-xl text-center">
        <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
          Error 404
        </p>

        <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">
          Page not found
        </h1>

        <p className="mt-5 text-lg leading-8 text-slate-600">
          The page you requested may have been moved, renamed, or does not
          exist.
        </p>

        <Link
          to="/"
          className="mt-8 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-blue-700 px-6 py-3 font-bold text-white transition hover:bg-blue-800"
        >
          <ArrowLeft className="size-5" aria-hidden="true" />
          Return Home
        </Link>
      </div>
    </main>
  );
}
