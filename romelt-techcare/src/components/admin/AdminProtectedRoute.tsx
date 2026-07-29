/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PROTECTED ROUTE
 * ================================================================
 *
 * Purpose:
 * Protects administrator-only routes from unauthenticated access.
 *
 * Responsibilities:
 * - Waits for administrator session initialization.
 * - Redirects unauthenticated users to the admin login page.
 * - Preserves the originally requested route for post-login return.
 * - Redirects administrators who must change their password.
 * - Renders protected child routes through React Router Outlet.
 *
 * Real-data integration:
 * Authentication state comes from AdminAuthContext, which verifies
 * stored JWT sessions against:
 *
 * GET /api/v1/admin/auth/me
 *
 * Routing behavior:
 * - Unauthenticated users are redirected to /admin/login.
 * - Administrators with mustChangePassword=true are redirected to
 *   /admin/change-password.
 * - Authenticated administrators may access protected admin routes.
 * ================================================================
 */

import { Navigate, Outlet, useLocation } from "react-router-dom";

import { AdminAuthLoadingScreen } from "@/components/admin/AdminAuthLoadingScreen";
import { useAdminAuth } from "@/hooks/useAdminAuth";

export function AdminProtectedRoute() {
  const location = useLocation();

  const { isAuthenticated, isInitializing, mustChangePassword } =
    useAdminAuth();

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

  if (mustChangePassword && location.pathname !== "/admin/change-password") {
    return <Navigate to="/admin/change-password" replace />;
  }

  return <Outlet />;
}
