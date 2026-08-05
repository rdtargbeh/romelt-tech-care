/**
 * ================================================================
 * ROMELT TECHCARE — BRAND LOGO
 * ================================================================
 *
 * Purpose:
 * Displays the temporary image-based business logo throughout the
 * Romelt TechCare public website.
 *
 * Responsibilities:
 * - Displays the temporary logo consistently.
 * - Links the logo to the public homepage.
 * - Supports light and default background variants.
 * - Optionally displays the Romelt TechCare tagline.
 * - Maintains accessible alternative text.
 *
 * Temporary logo asset:
 * public/image/logo-1.png
 *
 * Real-data integration:
 * This temporary image should later be replaced with the finalized
 * Romelt TechCare logo without changing consuming layout components.
 * ================================================================
 */

import { Link } from "react-router";

interface BrandLogoProps {
  variant?: "default" | "light";
  showTagline?: boolean;
}

export function BrandLogo({
  variant = "default",
  showTagline = false,
}: BrandLogoProps) {
  const taglineColor = variant === "light" ? "text-white" : "text-[#0B2545]";

  return (
    <Link
      to="/"
      aria-label="Romelt TechCare home"
      className="focus-ring inline-flex flex-col items-start rounded-xl"
    >
      <img
        src="/image/logo-1.png"
        alt="Romelt TechCare"
        className="h-auto w-44 object-contain sm:w-52"
      />

      {showTagline ? (
        <span
          className={`mt-1 pl-1 text-[0.65rem] font-bold tracking-wide ${taglineColor}`}
        >
          Hassle-Free Technology. Honest Service.
        </span>
      ) : null}
    </Link>
  );
}
