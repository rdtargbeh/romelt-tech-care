/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN GUEST ROUTE
 * ================================================================
 *
 * Purpose:
 * Prevents authenticated administrators from returning to public
 * authentication pages such as the admin login page.
 *
 * Responsibilities:
 * - Waits for administrator session initialization.
 * - Allows unauthenticated users to access guest-only routes.
 * - Redirects authenticated administrators to the dashboard.
 * - Redirects administrators requiring a password change to the
 *   password-change page.
 *
 * Real-data integration:
 * Authentication state is supplied by AdminAuthContext.
 * ================================================================
 */

import { Navigate, Outlet } from "react-router-dom";

import { AdminAuthLoadingScreen } from "@/components/admin/AdminAuthLoadingScreen";
import { useAdminAuth } from "@/hooks/useAdminAuth";

export function AdminGuestRoute() {
  const { isAuthenticated, isInitializing, mustChangePassword } =
    useAdminAuth();

  if (isInitializing) {
    return <AdminAuthLoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Outlet />;
  }

  if (mustChangePassword) {
    return <Navigate to="/admin/change-password" replace />;
  }

  return <Navigate to="/admin" replace />;
}
