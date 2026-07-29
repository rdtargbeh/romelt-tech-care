/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR ROUTES
 * ================================================================
 *
 * Purpose:
 * Defines authentication and protected workspace routes for the
 * Romelt TechCare administrator portal.
 *
 * Responsibilities:
 * - Registers administrator authentication routes.
 * - Protects authenticated workspace routes.
 * - Registers booking list, create, and detail pages.
 * - Registers contact-inquiry list and detail pages.
 * - Registers administrator-user management routes.
 *
 * Parent route:
 * This route collection is mounted beneath /admin/*.
 * ================================================================
 */

import { Route, Routes } from "react-router-dom";

import AdminChangePasswordPage from "@/components/admin/AdminChangePasswordPage";
import AdminDashboardPage from "@/components/admin/AdminDashboardPage";
import { AdminGuestRoute } from "@/components/admin/AdminGuestRoute";
import AdminLayout from "@/components/admin/AdminLayout";
import AdminLoginPage from "@/components/admin/AdminLoginPage";
import AdminModulePlaceholderPage from "@/components/admin/AdminModulePlaceholderPage";
import AdminNotFoundPage from "@/components/admin/AdminNotFoundPage";
import { AdminPasswordChangeRoute } from "@/components/admin/AdminPasswordChangeRoute";
import { AdminProtectedRoute } from "@/components/admin/AdminProtectedRoute";

import AdminBookingDetailsPage from "@/components/admin/bookings/AdminBookingDetailsPage";
import AdminBookingsPage from "@/components/admin/bookings/AdminBookingsPage";
import AdminCreateBookingPage from "@/components/admin/bookings/AdminCreateBookingPage";

import AdminContactInquiriesPage from "@/components/admin/contact-inquiries/AdminContactInquiriesPage";
import AdminContactInquiryDetailsPage from "@/components/admin/contact-inquiries/AdminContactInquiryDetailsPage";

import AdminCreateUserPage from "@/components/admin/users/AdminCreateUserPage";
import AdminEditUserPage from "@/components/admin/users/AdminEditUserPage";
import AdminUserDetailsPage from "@/components/admin/users/AdminUserDetailsPage";
import AdminUsersPage from "@/components/admin/users/AdminUsersPage";

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminGuestRoute />}>
        <Route path="login" element={<AdminLoginPage />} />
      </Route>

      <Route element={<AdminPasswordChangeRoute />}>
        <Route path="change-password" element={<AdminChangePasswordPage />} />
      </Route>

      <Route element={<AdminProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route index element={<AdminDashboardPage />} />

          <Route path="users">
            <Route index element={<AdminUsersPage />} />
            <Route path="new" element={<AdminCreateUserPage />} />

            <Route path=":adminUserId" element={<AdminUserDetailsPage />} />

            <Route path=":adminUserId/edit" element={<AdminEditUserPage />} />
          </Route>

          <Route path="bookings">
            <Route index element={<AdminBookingsPage />} />

            <Route path="new" element={<AdminCreateBookingPage />} />

            <Route
              path=":bookingRequestId"
              element={<AdminBookingDetailsPage />}
            />
          </Route>

          <Route path="contact-inquiries">
            <Route index element={<AdminContactInquiriesPage />} />

            <Route
              path=":contactInquiryId"
              element={<AdminContactInquiryDetailsPage />}
            />
          </Route>

          <Route
            path="services"
            element={
              <AdminModulePlaceholderPage
                title="Service management"
                description="Manage the Romelt TechCare service catalog and customer-facing service options."
              />
            }
          />

          <Route
            path="settings"
            element={
              <AdminModulePlaceholderPage
                title="Administrator settings"
                description="Manage business information, operating hours, service areas, branding, notifications, and integrations."
              />
            }
          />

          <Route path="*" element={<AdminNotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
