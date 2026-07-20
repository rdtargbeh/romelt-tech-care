/**
 * ================================================================
 * ROMELT TECHCARE — BRAND LOGO
 * ================================================================
 *
 * Purpose:
 * Provides a temporary text-and-icon brand mark for the website.
 *
 * Responsibilities:
 * - Displays the Romelt TechCare name consistently.
 * - Supports light and dark backgrounds.
 * - Links to the homepage when rendered in navigational areas.
 *
 * Real-data integration:
 * This temporary mark should be replaced with finalized professional
 * logo assets without changing the consuming layout components.
 * ================================================================
 */

import { HeartHandshake } from "lucide-react";
import { Link } from "react-router";

interface BrandLogoProps {
  variant?: "default" | "light";
  showTagline?: boolean;
}

export function BrandLogo({
  variant = "default",
  showTagline = false,
}: BrandLogoProps) {
  const primaryText = variant === "light" ? "text-white" : "text-navy-950";

  const secondaryText =
    variant === "light" ? "text-brand-200" : "text-brand-700";

  return (
    <Link
      to="/"
      aria-label="Romelt TechCare home"
      className="focus-ring inline-flex items-center gap-3 rounded-xl"
    >
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-brand-500 text-white shadow-lg shadow-brand-950/15">
        <HeartHandshake aria-hidden="true" className="h-6 w-6" />
      </span>

      <span className="flex flex-col">
        <span
          className={`font-display text-lg font-extrabold tracking-[-0.035em] ${primaryText}`}
        >
          Romelt <span className={secondaryText}>TechCare</span>
        </span>

        {showTagline ? (
          <span
            className={`text-[0.65rem] font-semibold tracking-wide ${
              variant === "light" ? "text-slate-300" : "text-slate-500"
            }`}
          >
            Hassle-Free Technology
          </span>
        ) : null}
      </span>
    </Link>
  );
}
