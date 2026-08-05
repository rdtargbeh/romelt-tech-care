/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR SESSION STORAGE
 * ================================================================
 *
 * Purpose:
 * Provides one secure and centralized browser-storage interface for
 * the authenticated administrator session.
 *
 * Responsibilities:
 * - Saves the administrator JWT and profile after login.
 * - Reads the current administrator session.
 * - Removes expired or malformed sessions.
 * - Provides the access token to the shared API client.
 * - Prevents authentication storage logic from being repeated.
 * - Dispatches session-change events for React authentication state.
 *
 * Storage decision:
 * sessionStorage is used instead of localStorage so the administrator
 * session ends when the browser tab or window session is closed.
 *
 * Security:
 * - Passwords are never stored.
 * - Only the JWT, expiration time, token type, and safe profile are
 *   persisted.
 * - Browser storage cannot completely prevent token theft if the
 *   application contains an XSS vulnerability. Strong CSP and safe
 *   rendering practices remain required.
 * ================================================================
 */

import type {
  AdminLoginResponse,
  AdminProfile,
  AdminSession,
} from "@/types/admin-auth.types";

const ADMIN_SESSION_STORAGE_KEY = "romelt-techcare.admin-session";

export const ADMIN_SESSION_CHANGED_EVENT =
  "romelt-techcare:admin-session-changed";

export function createAdminSession(
  loginResponse: AdminLoginResponse,
): AdminSession {
  if (!loginResponse.accessToken?.trim()) {
    throw new Error("The administrator access token is missing.");
  }

  if (!loginResponse.expiresAt?.trim()) {
    throw new Error("The administrator token expiration is missing.");
  }

  if (!loginResponse.administrator?.adminUserId) {
    throw new Error("The administrator profile is missing.");
  }

  return {
    accessToken: loginResponse.accessToken.trim(),
    tokenType: loginResponse.tokenType?.trim() || "Bearer",
    expiresAt: loginResponse.expiresAt,
    administrator: loginResponse.administrator,
  };
}

export function saveAdminSession(session: AdminSession): void {
  if (!isBrowserAvailable()) {
    return;
  }

  if (!isValidAdminSession(session)) {
    throw new Error("The administrator session is invalid.");
  }

  sessionStorage.setItem(ADMIN_SESSION_STORAGE_KEY, JSON.stringify(session));

  dispatchAdminSessionChanged();
}

export function saveAdminLoginResponse(
  loginResponse: AdminLoginResponse,
): AdminSession {
  const session = createAdminSession(loginResponse);

  saveAdminSession(session);

  return session;
}

export function getAdminSession(): AdminSession | null {
  if (!isBrowserAvailable()) {
    return null;
  }

  const storedValue = sessionStorage.getItem(ADMIN_SESSION_STORAGE_KEY);

  if (!storedValue) {
    return null;
  }

  try {
    const parsedValue = JSON.parse(storedValue) as unknown;

    if (!isValidAdminSession(parsedValue)) {
      clearAdminSession();
      return null;
    }

    if (isAdminSessionExpired(parsedValue)) {
      clearAdminSession();
      return null;
    }

    return parsedValue;
  } catch {
    clearAdminSession();
    return null;
  }
}

export function getAdminAccessToken(): string | null {
  return getAdminSession()?.accessToken ?? null;
}

export function getAuthenticatedAdministrator(): AdminProfile | null {
  return getAdminSession()?.administrator ?? null;
}

export function isAdminAuthenticated(): boolean {
  return getAdminSession() !== null;
}

export function updateStoredAdministrator(
  administrator: AdminProfile,
): AdminSession | null {
  const currentSession = getAdminSession();

  if (!currentSession) {
    return null;
  }

  const updatedSession: AdminSession = {
    ...currentSession,
    administrator,
  };

  saveAdminSession(updatedSession);

  return updatedSession;
}

export function clearAdminSession(): void {
  if (!isBrowserAvailable()) {
    return;
  }

  sessionStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);

  dispatchAdminSessionChanged();
}

export function isAdminSessionExpired(
  session: Pick<AdminSession, "expiresAt">,
): boolean {
  const expirationTime = Date.parse(session.expiresAt);

  if (!Number.isFinite(expirationTime)) {
    return true;
  }

  return expirationTime <= Date.now();
}

function isValidAdminSession(value: unknown): value is AdminSession {
  if (!isObject(value)) {
    return false;
  }

  if (
    typeof value.accessToken !== "string" ||
    value.accessToken.trim() === ""
  ) {
    return false;
  }

  if (typeof value.tokenType !== "string" || value.tokenType.trim() === "") {
    return false;
  }

  if (typeof value.expiresAt !== "string" || value.expiresAt.trim() === "") {
    return false;
  }

  return isValidAdminProfile(value.administrator);
}

function isValidAdminProfile(value: unknown): value is AdminProfile {
  if (!isObject(value)) {
    return false;
  }

  return (
    typeof value.adminUserId === "string" &&
    value.adminUserId.trim() !== "" &&
    typeof value.email === "string" &&
    value.email.trim() !== "" &&
    typeof value.firstName === "string" &&
    typeof value.lastName === "string" &&
    typeof value.fullName === "string" &&
    typeof value.role === "string" &&
    typeof value.status === "string" &&
    typeof value.mustChangePassword === "boolean"
  );
}

function dispatchAdminSessionChanged(): void {
  if (!isBrowserAvailable()) {
    return;
  }

  window.dispatchEvent(new CustomEvent(ADMIN_SESSION_CHANGED_EVENT));
}

function isBrowserAvailable(): boolean {
  return (
    typeof window !== "undefined" &&
    typeof window.sessionStorage !== "undefined"
  );
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
