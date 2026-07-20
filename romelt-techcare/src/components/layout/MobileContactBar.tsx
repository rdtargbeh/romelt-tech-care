/**
 * ================================================================
 * ROMELT TECHCARE — MOBILE CONTACT BAR
 * ================================================================
 *
 * Purpose:
 * Keeps the two most important customer actions visible on phones.
 *
 * Responsibilities:
 * - Provides one-tap calling.
 * - Provides one-tap access to the booking page.
 * - Appears only on smaller screens.
 *
 * Real-data integration:
 * The placeholder telephone number must be replaced by the business
 * number from centralized company settings before launch.
 * ================================================================
 */

import { CalendarCheck, Phone } from "lucide-react";
import { Link } from "react-router";

export function MobileContactBar() {
  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 p-3 shadow-[0_-10px_35px_-20px_rgba(16,39,56,0.35)] backdrop-blur-xl sm:hidden">
      <div className="grid grid-cols-2 gap-3">
        <a
          href="tel:+10000000000"
          className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-brand-50 px-3 text-sm font-extrabold text-brand-800"
        >
          <Phone aria-hidden="true" className="h-4 w-4" />
          Call
        </a>

        <Link
          to="/book-service"
          className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-brand-600 px-3 text-sm font-extrabold text-white"
        >
          <CalendarCheck aria-hidden="true" className="h-4 w-4" />
          Book service
        </Link>
      </div>
    </div>
  );
}
