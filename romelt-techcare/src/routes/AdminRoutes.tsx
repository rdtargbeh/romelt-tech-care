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
 * - Registers administrator-user management routes.
 * - Registers booking list, create, and detail pages.
 * - Registers contact-inquiry list and detail pages.
 * - Registers customer list, create, and detail pages.
 * - Registers customer-review list, create, and detail pages.
 * - Registers remaining administrator workspace modules.
 *
 * Parent route:
 * This route collection is mounted beneath /admin/*.
 *
 * Customer routes:
 * /admin/customers
 * /admin/customers/new
 * /admin/customers/:customerId
 *
 * Customer review routes:
 * /admin/customers/reviews
 * /admin/customers/reviews/new
 * /admin/customers/reviews/:customerReviewId
 *
 * Important routing rule:
 * Fixed routes such as "reviews" and "new" must appear before
 * dynamic identifier routes such as ":customerId".
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

import AdminCreateCustomerPage from "@/components/admin/customer/AdminCreateCustomerPage";
import AdminCustomerDetailPage from "@/components/admin/customer/AdminCustomerDetailPage";
import AdminCustomersPage from "@/components/admin/customer/AdminCustomersPage";

import AdminCreateCustomerReviewPage from "@/components/admin/customer/AdminCreateCustomerReviewPage";
import AdminCustomerReviewDetailsPage from "@/components/admin/customer/AdminCustomerReviewDetailsPage";
import AdminCustomerReviewsPage from "@/components/admin/customer/AdminCustomerReviewsPage";

import AdminCreateUserPage from "@/components/admin/users/AdminCreateUserPage";
import AdminEditUserPage from "@/components/admin/users/AdminEditUserPage";
import AdminUserDetailsPage from "@/components/admin/users/AdminUserDetailsPage";
import AdminUsersPage from "@/components/admin/users/AdminUsersPage";

export default function AdminRoutes() {
  return (
    <Routes>
      {/* =============================================================
       * PUBLIC / GUEST ADMIN ROUTES
       * ============================================================= */}

      <Route element={<AdminGuestRoute />}>
        <Route path="login" element={<AdminLoginPage />} />
      </Route>

      {/* =============================================================
       * PASSWORD CHANGE ROUTE
       * ============================================================= */}

      <Route element={<AdminPasswordChangeRoute />}>
        <Route path="change-password" element={<AdminChangePasswordPage />} />
      </Route>

      {/* =============================================================
       * PROTECTED ADMIN WORKSPACE
       * ============================================================= */}

      <Route element={<AdminProtectedRoute />}>
        <Route element={<AdminLayout />}>
          {/* =========================================================
           * DASHBOARD
           * ========================================================= */}

          <Route index element={<AdminDashboardPage />} />

          {/* =========================================================
           * ADMINISTRATOR USERS
           * ========================================================= */}

          <Route path="users">
            <Route index element={<AdminUsersPage />} />

            <Route path="new" element={<AdminCreateUserPage />} />

            <Route path=":adminUserId" element={<AdminUserDetailsPage />} />

            <Route path=":adminUserId/edit" element={<AdminEditUserPage />} />
          </Route>

          {/* =========================================================
           * CUSTOMERS
           *
           * Important:
           * Fixed review routes must remain before :customerId.
           * ========================================================= */}

          <Route path="customers">
            {/* =====================================================
             * CUSTOMER LIST
             * /admin/customers
             * ===================================================== */}

            <Route index element={<AdminCustomersPage />} />

            {/* =====================================================
             * CREATE CUSTOMER
             * /admin/customers/new
             * ===================================================== */}

            <Route path="new" element={<AdminCreateCustomerPage />} />

            {/* =====================================================
             * CUSTOMER REVIEWS
             *
             * /admin/customers/reviews
             * /admin/customers/reviews/new
             * /admin/customers/reviews/:customerReviewId
             *
             * This block must remain before :customerId.
             * ===================================================== */}

            <Route path="reviews">
              <Route index element={<AdminCustomerReviewsPage />} />

              <Route path="new" element={<AdminCreateCustomerReviewPage />} />

              <Route
                path=":customerReviewId"
                element={<AdminCustomerReviewDetailsPage />}
              />
            </Route>

            {/* =====================================================
             * CUSTOMER DETAIL
             * /admin/customers/:customerId
             *
             * Dynamic route intentionally comes last.
             * ===================================================== */}

            <Route path=":customerId" element={<AdminCustomerDetailPage />} />
          </Route>

          {/* =========================================================
           * BOOKINGS
           * ========================================================= */}

          <Route path="bookings">
            <Route index element={<AdminBookingsPage />} />

            <Route path="new" element={<AdminCreateBookingPage />} />

            <Route
              path=":bookingRequestId"
              element={<AdminBookingDetailsPage />}
            />
          </Route>

          {/* =========================================================
           * CONTACT INQUIRIES
           * ========================================================= */}

          <Route path="contact-inquiries">
            <Route index element={<AdminContactInquiriesPage />} />

            <Route
              path=":contactInquiryId"
              element={<AdminContactInquiryDetailsPage />}
            />
          </Route>

          {/* =========================================================
           * SERVICES
           * ========================================================= */}

          <Route
            path="services"
            element={
              <AdminModulePlaceholderPage
                title="Service management"
                description="Manage the Romelt TechCare service catalog and customer-facing service options."
              />
            }
          />

          {/* =========================================================
           * SETTINGS
           * ========================================================= */}

          <Route
            path="settings"
            element={
              <AdminModulePlaceholderPage
                title="Administrator settings"
                description="Manage business information, operating hours, service areas, branding, notifications, and integrations."
              />
            }
          />

          {/* =========================================================
           * ADMIN NOT FOUND
           * ========================================================= */}

          <Route path="*" element={<AdminNotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
