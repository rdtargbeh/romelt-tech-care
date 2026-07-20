/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC NAVIGATION DATA
 * ================================================================
 *
 * Purpose:
 * Provides the primary navigation links used throughout the website.
 *
 * Responsibilities:
 * - Keeps header and mobile navigation synchronized.
 * - Defines the public page order and route destinations.
 *
 * Real-data integration:
 * These links are intentionally static for the launch website.
 * Customer-specific portal navigation will be permission-driven and
 * loaded from authenticated backend data in a later phase.
 * ================================================================
 */

import type { NavigationItem } from "@/types/navigation";

export const primaryNavigation: NavigationItem[] = [
  {
    label: "Home",
    path: "/",
    exact: true,
  },
  {
    label: "Services",
    path: "/services",
  },
  {
    label: "Pricing",
    path: "/pricing",
  },
  {
    label: "About",
    path: "/about",
  },
  {
    label: "Contact",
    path: "/contact",
  },
];
