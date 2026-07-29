/**
 * ================================================================
 * ROMELT TECHCARE — ROUTER STATE TYPES
 * ================================================================
 *
 * Purpose:
 * Defines reusable React Router navigation-state structures.
 *
 * Responsibilities:
 * - Types the route saved before an authentication redirect.
 * - Supports safe post-login navigation.
 * - Prevents pages from using untyped location state.
 *
 * Real-data integration:
 * Used by administrator login and protected-route components.
 * ================================================================
 */

export interface RedirectLocationState {
  from?: {
    pathname?: string;
    search?: string;
    hash?: string;
  };
}
