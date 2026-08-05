/**
 * ================================================================
 * ROMELT TECHCARE — BRAND MARK
 * ================================================================
 *
 * Purpose:
 * Provides the reusable Romelt TechCare visual icon used throughout
 * the public website and future customer-facing applications.
 *
 * Responsibilities:
 * - Displays the shield-shaped technology brand symbol.
 * - Supports light and dark presentation variants.
 * - Supports decorative and accessible usage.
 * - Provides consistent dimensions across headers, cards, and forms.
 *
 * Real-data integration:
 * This component is a permanent frontend brand asset and does not
 * require backend integration.
 * ================================================================
 */

interface BrandMarkProps {
  className?: string;
  variant?: "default" | "light" | "monochrome";
  decorative?: boolean;
  title?: string;
}

export function BrandMark({
  className = "h-11 w-11",
  variant = "default",
  decorative = true,
  title = "Romelt TechCare",
}: BrandMarkProps) {
  const gradientStart = variant === "light" ? "#60a5fa" : "#2563eb";

  const gradientEnd = variant === "light" ? "#2563eb" : "#0f3f91";

  const letterColor = variant === "monochrome" ? "currentColor" : "#ffffff";

  const shieldColor =
    variant === "monochrome" ? "currentColor" : "url(#romelt-brand-gradient)";

  const backgroundColor = variant === "light" ? "#ffffff" : "#07152e";

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      className={className}
      role={decorative ? undefined : "img"}
      aria-hidden={decorative ? "true" : undefined}
      aria-labelledby={decorative ? undefined : "romelt-brand-title"}
      focusable="false"
    >
      {!decorative ? <title id="romelt-brand-title">{title}</title> : null}

      <defs>
        <linearGradient
          id="romelt-brand-gradient"
          x1="8"
          y1="6"
          x2="56"
          y2="58"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor={gradientStart} />
          <stop offset="1" stopColor={gradientEnd} />
        </linearGradient>
      </defs>

      {variant !== "monochrome" ? (
        <rect width="64" height="64" rx="16" fill={backgroundColor} />
      ) : null}

      <path
        d="M32 7.5 52 15v14.2c0 13.3-8.1 22.7-20 27.3C20.1 51.9 12 42.5 12 29.2V15l20-7.5Z"
        fill={shieldColor}
      />

      <path
        d="M23 20.5h10.2c6.1 0 10.3 3.3 10.3 8.7 0 3.8-2.1 6.6-5.5 7.9l6.5 9.4h-7.2l-5.6-8.4H29v8.4h-6V20.5Zm6 5.3v7.1h4c2.8 0 4.4-1.2 4.4-3.6 0-2.3-1.6-3.5-4.4-3.5h-4Z"
        fill={letterColor}
      />

      {variant !== "monochrome" ? (
        <>
          <circle cx="47" cy="18" r="3" fill="#60a5fa" />
          <circle cx="50" cy="27" r="2" fill="#bfdbfe" />

          <path
            d="M43 20.5 47 18m-2 8 5 1"
            stroke="#dbeafe"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </>
      ) : null}
    </svg>
  );
}
