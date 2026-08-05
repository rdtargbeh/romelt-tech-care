/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PASSWORD-CHANGE ROUTE
 * ================================================================
 *
 * Purpose:
 * Controls access to the administrator password-change page.
 *
 * Responsibilities:
 * - Requires an authenticated administrator.
 * - Waits for session restoration before making routing decisions.
 * - Allows forced password changes.
 * - Allows authenticated administrators to voluntarily change their
 *   password later.
 * - Redirects unauthenticated users to the login page.
 *
 * Real-data integration:
 * Uses authentication state from AdminAuthContext.
 * ================================================================
 */

import { Navigate, Outlet, useLocation } from "react-router-dom";

import { AdminAuthLoadingScreen } from "@/components/admin/AdminAuthLoadingScreen";
import { useAdminAuth } from "@/hooks/useAdminAuth";

export function AdminPasswordChangeRoute() {
  const location = useLocation();

  const { isAuthenticated, isInitializing } = useAdminAuth();

  if (isInitializing) {
    return <AdminAuthLoadingScreen />;
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/admin/login"
        replace
        state={{
          from: location,
        }}
      />
    );
  }

  return <Outlet />;
}
