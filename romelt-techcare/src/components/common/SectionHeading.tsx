/**
 * ================================================================
 * ROMELT TECHCARE — SECTION HEADING
 * ================================================================
 *
 * Purpose:
 * Renders consistent introductory content for major page sections.
 *
 * Responsibilities:
 * - Displays an optional eyebrow, title, and supporting description.
 * - Supports left-aligned and centered section layouts.
 *
 * Real-data integration:
 * Text is supplied by parent pages and can later originate from
 * backend-managed website content.
 * ================================================================
 */

interface SectionHeadingProps {
  eyebrow?: string;
  title: string;
  description?: string;
  align?: "left" | "center";
  className?: string;
}

export function SectionHeading({
  eyebrow,
  title,
  description,
  align = "left",
  className = "",
}: SectionHeadingProps) {
  const alignmentClass =
    align === "center"
      ? "mx-auto items-center text-center"
      : "items-start text-left";

  return (
    <div
      className={`flex max-w-3xl flex-col gap-4 ${alignmentClass} ${className}`}
    >
      {eyebrow ? <span className="site-eyebrow">{eyebrow}</span> : null}

      <h2 className="text-balance font-display text-3xl font-extrabold tracking-[-0.035em] text-navy-950 sm:text-4xl lg:text-5xl">
        {title}
      </h2>

      {description ? (
        <p className="text-pretty text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
          {description}
        </p>
      ) : null}
    </div>
  );
}
