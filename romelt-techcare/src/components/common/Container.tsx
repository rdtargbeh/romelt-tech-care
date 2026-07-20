/**
 * ================================================================
 * ROMELT TECHCARE — RESPONSIVE CONTAINER
 * ================================================================
 *
 * Purpose:
 * Provides consistent horizontal spacing and maximum page width.
 *
 * Responsibilities:
 * - Keeps content readable across phones, tablets, and desktops.
 * - Prevents duplicated page-width classes throughout the website.
 *
 * Real-data integration:
 * This presentation component does not load or modify application
 * data.
 * ================================================================
 */

import type { HTMLAttributes, ReactNode } from "react";

interface ContainerProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Container({
  children,
  className = "",
  ...rest
}: ContainerProps) {
  return (
    <div
      className={`mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
