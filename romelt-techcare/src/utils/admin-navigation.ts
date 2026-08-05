/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NAVIGATION UTILITIES
 * ================================================================
 *
 * Purpose:
 * Provides safe administrator redirect-path resolution after login.
 *
 * Responsibilities:
 * - Reads the originally requested route from React Router state.
 * - Rejects external and malformed redirect destinations.
 * - Prevents open-redirect vulnerabilities.
 * - Returns the admin dashboard when no safe destination exists.
 *
 * Security:
 * Only internal paths beginning with /admin are accepted.
 * Protocol-relative and absolute URLs are rejected.
 * ================================================================
 */

import type { RedirectLocationState } from "@/types/router-state.types";

const DEFAULT_ADMIN_ROUTE = "/admin";

export function resolveAdminRedirectPath(
  state: RedirectLocationState | null | undefined,
): string {
  const pathname = state?.from?.pathname;

  if (!isSafeAdminPath(pathname)) {
    return DEFAULT_ADMIN_ROUTE;
  }

  const search = normalizePathSuffix(state?.from?.search, "?");

  const hash = normalizePathSuffix(state?.from?.hash, "#");

  return `${pathname}${search}${hash}`;
}

function isSafeAdminPath(pathname: string | undefined): pathname is string {
  if (!pathname) {
    return false;
  }

  if (!pathname.startsWith("/admin")) {
    return false;
  }

  if (pathname.startsWith("//") || pathname.includes("://")) {
    return false;
  }

  return true;
}

function normalizePathSuffix(
  value: string | undefined,
  expectedPrefix: "?" | "#",
): string {
  if (!value || !value.startsWith(expectedPrefix)) {
    return "";
  }

  return value;
}
