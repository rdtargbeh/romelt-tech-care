/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER MANAGEMENT SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides protected frontend API operations for Administrator User
 * Management.
 *
 * Responsibilities:
 * - Retrieves administrator accounts.
 * - Retrieves one administrator by ID.
 * - Creates administrator accounts.
 * - Updates administrator profile and role information.
 * - Changes administrator account status.
 * - Resets administrator passwords.
 * - Deletes administrator accounts.
 *
 * Real-data integration:
 * Uses:
 *
 * GET    /api/v1/admin/users
 * GET    /api/v1/admin/users/{adminUserId}
 * POST   /api/v1/admin/users
 * PUT    /api/v1/admin/users/{adminUserId}
 * PATCH  /api/v1/admin/users/{adminUserId}/status
 * POST   /api/v1/admin/users/{adminUserId}/reset-password
 * DELETE /api/v1/admin/users/{adminUserId}
 *
 * Security:
 * - Every request requires administrator JWT authentication.
 * - Temporary passwords are sent only inside HTTPS request bodies.
 * - Password values are never logged or persisted by this service.
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";
import type {
  AdminCreateUserRequest,
  AdminResetPasswordRequest,
  AdminUpdateUserRequest,
  AdminUser,
  AdminUserStatusRequest,
} from "@/types/admin-user.types";

const ADMIN_USERS_BASE_ENDPOINT = "/admin/users";

export async function getAdminUsers(
  signal?: AbortSignal,
): Promise<AdminUser[]> {
  return apiClient.get<AdminUser[]>(ADMIN_USERS_BASE_ENDPOINT, {
    signal,
    requireAuthentication: true,
  });
}

export async function getAdminUserById(
  adminUserId: string,
  signal?: AbortSignal,
): Promise<AdminUser> {
  const normalizedAdminUserId = requireAdminUserId(adminUserId);

  return apiClient.get<AdminUser>(
    `${ADMIN_USERS_BASE_ENDPOINT}/${encodeURIComponent(normalizedAdminUserId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

export async function createAdminUser(
  request: AdminCreateUserRequest,
  signal?: AbortSignal,
): Promise<AdminUser> {
  return apiClient.post<AdminUser>(
    ADMIN_USERS_BASE_ENDPOINT,
    normalizeCreateRequest(request),
    {
      signal,
      requireAuthentication: true,
    },
  );
}

export async function updateAdminUser(
  adminUserId: string,
  request: AdminUpdateUserRequest,
  signal?: AbortSignal,
): Promise<AdminUser> {
  const normalizedAdminUserId = requireAdminUserId(adminUserId);

  return apiClient.put<AdminUser>(
    `${ADMIN_USERS_BASE_ENDPOINT}/${encodeURIComponent(normalizedAdminUserId)}`,
    normalizeUpdateRequest(request),
    {
      signal,
      requireAuthentication: true,
    },
  );
}

export async function changeAdminUserStatus(
  adminUserId: string,
  request: AdminUserStatusRequest,
  signal?: AbortSignal,
): Promise<AdminUser> {
  const normalizedAdminUserId = requireAdminUserId(adminUserId);

  return apiClient.patch<AdminUser>(
    `${ADMIN_USERS_BASE_ENDPOINT}/${encodeURIComponent(
      normalizedAdminUserId,
    )}/status`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

export async function resetAdminUserPassword(
  adminUserId: string,
  request: AdminResetPasswordRequest,
  signal?: AbortSignal,
): Promise<AdminUser> {
  const normalizedAdminUserId = requireAdminUserId(adminUserId);

  return apiClient.post<AdminUser>(
    `${ADMIN_USERS_BASE_ENDPOINT}/${encodeURIComponent(
      normalizedAdminUserId,
    )}/reset-password`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

export async function deleteAdminUser(
  adminUserId: string,
  signal?: AbortSignal,
): Promise<void> {
  const normalizedAdminUserId = requireAdminUserId(adminUserId);

  await apiClient.delete<void>(
    `${ADMIN_USERS_BASE_ENDPOINT}/${encodeURIComponent(normalizedAdminUserId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

function normalizeCreateRequest(
  request: AdminCreateUserRequest,
): AdminCreateUserRequest {
  return {
    email: request.email.trim().toLowerCase(),
    firstName: request.firstName.trim(),
    lastName: request.lastName.trim(),
    jobTitle: normalizeOptionalValue(request.jobTitle),
    role: request.role,
    temporaryPassword: request.temporaryPassword,
    confirmTemporaryPassword: request.confirmTemporaryPassword,
  };
}

function normalizeUpdateRequest(
  request: AdminUpdateUserRequest,
): AdminUpdateUserRequest {
  return {
    email: request.email.trim().toLowerCase(),
    firstName: request.firstName.trim(),
    lastName: request.lastName.trim(),
    jobTitle: normalizeOptionalValue(request.jobTitle),
    role: request.role,
  };
}

function normalizeOptionalValue(
  value: string | null | undefined,
): string | null {
  const normalizedValue = value?.trim();

  return normalizedValue ? normalizedValue : null;
}

function requireAdminUserId(adminUserId: string): string {
  const normalizedAdminUserId = adminUserId?.trim();

  if (!normalizedAdminUserId) {
    throw new Error("Administrator user ID is required.");
  }

  return normalizedAdminUserId;
}
