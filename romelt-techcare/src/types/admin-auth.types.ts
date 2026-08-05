/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION TYPES
 * ================================================================
 *
 * Purpose:
 * Defines the request, response, profile, role, status, and session
 * types used by Romelt TechCare administrator authentication.
 *
 * Responsibilities:
 * - Mirrors the Spring Boot administrator authentication DTOs.
 * - Defines supported administrator roles and account statuses.
 * - Provides strongly typed login and password-change payloads.
 * - Defines the authenticated administrator profile.
 * - Defines the browser-stored administrator session.
 *
 * Real-data integration:
 * These types support:
 *
 * POST /api/v1/admin/auth/login
 * GET  /api/v1/admin/auth/me
 * POST /api/v1/admin/auth/change-password
 *
 * Security:
 * - Passwords appear only in outbound request payloads.
 * - Passwords must never be stored in browser storage.
 * - JWT session storage remains managed by admin-session.ts.
 * ================================================================
 */

export type AdminRole = "SUPER_ADMIN" | "ADMIN" | "STAFF";

export type AdminStatus = "ACTIVE" | "INACTIVE" | "LOCKED";

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AdminChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface AdminProfile {
  adminUserId: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  jobTitle: string | null;
  role: AdminRole;
  status: AdminStatus;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  passwordChangedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminLoginResponse {
  accessToken: string;
  tokenType: "Bearer" | string;
  expiresAt: string;
  expiresInSeconds: number;
  mustChangePassword: boolean;
  administrator: AdminProfile;
}

export interface AdminSession {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  administrator: AdminProfile;
}

export interface AdminAuthenticationState {
  session: AdminSession | null;
  administrator: AdminProfile | null;
  isAuthenticated: boolean;
  mustChangePassword: boolean;
}
