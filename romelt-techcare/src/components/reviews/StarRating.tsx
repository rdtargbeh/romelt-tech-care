/**
 * ================================================================
 * ROMELT TECHCARE — STAR RATING
 * ================================================================
 *
 * Purpose:
 * Displays an accessible one-to-five-star customer rating.
 *
 * Responsibilities:
 * - Supports display-only and interactive modes.
 * - Supports whole and fractional aggregate ratings.
 * - Provides an accessible text label.
 * - Preserves mobile-friendly touch targets when interactive.
 * ================================================================
 */

import { Star } from "lucide-react";

interface StarRatingProps {
  value: number;
  maximum?: number;
  size?: "sm" | "md" | "lg";
  interactive?: boolean;
  disabled?: boolean;
  showValue?: boolean;
  label?: string;
  onChange?: (rating: number) => void;
}

const sizeClasses = {
  sm: "size-4",
  md: "size-5",
  lg: "size-7",
} as const;

export function StarRating({
  value,
  maximum = 5,
  size = "md",
  interactive = false,
  disabled = false,
  showValue = false,
  label,
  onChange,
}: StarRatingProps) {
  const safeMaximum = Math.max(1, Math.floor(maximum));
  const safeValue = Math.min(safeMaximum, Math.max(0, value));

  const accessibleLabel =
    label ??
    `${safeValue.toFixed(Number.isInteger(safeValue) ? 0 : 1)} out of ${
      safeMaximum
    } stars`;

  return (
    <div
      className="inline-flex items-center gap-2"
      aria-label={accessibleLabel}
    >
      <div className="inline-flex items-center gap-1" role="img">
        {Array.from({ length: safeMaximum }, (_, index) => {
          const starNumber = index + 1;
          const isFilled = safeValue >= starNumber;
          const isPartiallyFilled =
            !isFilled && safeValue > index && safeValue < starNumber;

          if (interactive) {
            return (
              <button
                key={starNumber}
                type="button"
                disabled={disabled}
                onClick={() => onChange?.(starNumber)}
                aria-label={`Rate ${starNumber} out of ${safeMaximum} stars`}
                className="focus-ring inline-flex min-h-10 min-w-10 items-center justify-center rounded-lg transition hover:scale-110 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Star
                  className={[
                    sizeClasses[size],
                    starNumber <= safeValue
                      ? "fill-[#D4AF37] text-[#D4AF37]"
                      : "text-slate-300",
                  ].join(" ")}
                  aria-hidden="true"
                />
              </button>
            );
          }

          return (
            <span key={starNumber} className="relative inline-flex">
              <Star
                className={`${sizeClasses[size]} text-slate-300`}
                aria-hidden="true"
              />

              {(isFilled || isPartiallyFilled) && (
                <span
                  aria-hidden="true"
                  className="absolute inset-0 overflow-hidden"
                  style={{
                    width: isFilled
                      ? "100%"
                      : `${Math.round((safeValue - index) * 100)}%`,
                  }}
                >
                  <Star
                    className={`${sizeClasses[size]} fill-[#D4AF37] text-[#D4AF37]`}
                  />
                </span>
              )}
            </span>
          );
        })}
      </div>

      {showValue && (
        <span className="font-extrabold text-[#0B2545]">
          {safeValue.toFixed(1)}
        </span>
      )}
    </div>
  );
}
