/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER MANAGEMENT TYPES
 * ================================================================
 *
 * Purpose:
 * Defines frontend types used by the protected Administrator User
 * Management module.
 *
 * Responsibilities:
 * - Mirrors administrator-management backend DTOs.
 * - Defines create, update, status, and password-reset requests.
 * - Defines administrator list and detail response data.
 * - Defines frontend filtering and form models.
 *
 * Real-data integration:
 * Supports:
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
 * - Temporary passwords appear only in request payloads.
 * - Passwords must never be stored in frontend state longer than
 *   required to submit a form.
 * ================================================================
 */

import type { AdminRole, AdminStatus } from "@/types/admin-auth.types";

export interface AdminUser {
  adminUserId: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  jobTitle: string | null;
  role: AdminRole;
  status: AdminStatus;
  mustChangePassword: boolean;

  /**
   * Management responses may expose these security fields.
   * They remain optional so the frontend is also compatible with
   * profile-shaped responses that intentionally omit them.
   */
  failedLoginAttempts?: number;
  lockedUntil?: string | null;

  lastLoginAt: string | null;
  passwordChangedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  jobTitle?: string | null;
  role: AdminRole;
  temporaryPassword: string;
  confirmTemporaryPassword: string;
}

export interface AdminUpdateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  jobTitle?: string | null;
  role: AdminRole;
}

export interface AdminUserStatusRequest {
  status: AdminStatus;
}

export interface AdminResetPasswordRequest {
  temporaryPassword: string;
  confirmTemporaryPassword: string;
}

export interface AdminUserSearchFilters {
  keyword: string;
  role: AdminRole | "";
  status: AdminStatus | "";
}

export interface AdminUserFormValues {
  email: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  role: AdminRole;
  temporaryPassword: string;
  confirmTemporaryPassword: string;
}

export interface AdminUserFormErrors {
  email?: string;
  firstName?: string;
  lastName?: string;
  jobTitle?: string;
  role?: string;
  temporaryPassword?: string;
  confirmTemporaryPassword?: string;
  general?: string;
}

export interface AdminPasswordResetFormValues {
  temporaryPassword: string;
  confirmTemporaryPassword: string;
}

export interface AdminPasswordResetFormErrors {
  temporaryPassword?: string;
  confirmTemporaryPassword?: string;
  general?: string;
}

export interface AdminUsersLocationState {
  successMessage?: string;
}

export const ADMIN_ROLE_OPTIONS: ReadonlyArray<{
  value: AdminRole;
  label: string;
  description: string;
}> = [
  {
    value: "SUPER_ADMIN",
    label: "Super Administrator",
    description:
      "Full administrative control, including administrator account management.",
  },
  {
    value: "ADMIN",
    label: "Administrator",
    description: "Administrative access for managing business operations.",
  },
  {
    value: "STAFF",
    label: "Staff",
    description:
      "Operational access limited by the backend authorization policy.",
  },
];

export const ADMIN_STATUS_OPTIONS: ReadonlyArray<{
  value: AdminStatus;
  label: string;
  description: string;
}> = [
  {
    value: "ACTIVE",
    label: "Active",
    description: "The administrator can sign in and use authorized features.",
  },
  {
    value: "INACTIVE",
    label: "Inactive",
    description: "The administrator cannot sign in until reactivated.",
  },
  {
    value: "LOCKED",
    label: "Locked",
    description: "The account remains blocked until it is manually unlocked.",
  },
];

export const INITIAL_ADMIN_USER_FORM_VALUES: AdminUserFormValues = {
  email: "",
  firstName: "",
  lastName: "",
  jobTitle: "",
  role: "STAFF",
  temporaryPassword: "",
  confirmTemporaryPassword: "",
};

export const INITIAL_ADMIN_USER_FILTERS: AdminUserSearchFilters = {
  keyword: "",
  role: "",
  status: "",
};
