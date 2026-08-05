/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides frontend API operations for administrator authentication.
 *
 * Responsibilities:
 * - Authenticates an administrator.
 * - Saves the returned JWT session.
 * - Retrieves the current administrator profile.
 * - Changes the administrator password.
 * - Updates the stored profile after password changes.
 * - Clears the browser session during logout.
 *
 * Real-data integration:
 * Uses:
 *
 * POST /api/v1/admin/auth/login
 * GET  /api/v1/admin/auth/me
 * POST /api/v1/admin/auth/change-password
 *
 * Security:
 * - Passwords are sent only in HTTPS request bodies.
 * - Passwords are never logged or stored.
 * - Protected requests require the stored bearer token.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";
import {
  clearAdminSession,
  saveAdminLoginResponse,
  updateStoredAdministrator,
} from "@/services/admin-session";
import type {
  AdminChangePasswordRequest,
  AdminLoginRequest,
  AdminLoginResponse,
  AdminProfile,
  AdminSession,
} from "@/types/admin-auth.types";

const ADMIN_AUTH_BASE_ENDPOINT = "/admin/auth";

export async function loginAdministrator(
  request: AdminLoginRequest,
  signal?: AbortSignal,
): Promise<AdminSession> {
  const response = await apiClient.post<AdminLoginResponse>(
    `${ADMIN_AUTH_BASE_ENDPOINT}/login`,
    request,
    {
      signal,
      requireAuthentication: false,
    },
  );

  return saveAdminLoginResponse(response);
}

export async function getCurrentAdministrator(
  signal?: AbortSignal,
): Promise<AdminProfile> {
  const administrator = await apiClient.get<AdminProfile>(
    `${ADMIN_AUTH_BASE_ENDPOINT}/me`,
    {
      signal,
      requireAuthentication: true,
    },
  );

  updateStoredAdministrator(administrator);

  return administrator;
}

export async function changeAdministratorPassword(
  request: AdminChangePasswordRequest,
  signal?: AbortSignal,
): Promise<AdminProfile> {
  const administrator = await apiClient.post<AdminProfile>(
    `${ADMIN_AUTH_BASE_ENDPOINT}/change-password`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );

  updateStoredAdministrator(administrator);

  return administrator;
}

export function logoutAdministrator(): void {
  clearAdminSession();
}
