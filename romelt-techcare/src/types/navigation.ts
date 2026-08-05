/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION TYPES
 * ================================================================
 *
 * Purpose:
 * Defines the TypeScript shape used by public navigation links.
 *
 * Responsibilities:
 * - Keeps desktop, mobile, and footer navigation consistent.
 * - Prevents invalid or incomplete navigation objects.
 *
 * Real-data integration:
 * Public navigation is currently static. A future administration
 * system could provide configurable links through an API.
 * ================================================================
 */

export interface NavigationItem {
  label: string;
  path: string;
  exact?: boolean;
}
