/**
 * ================================================================
 * ROMELT TECHCARE — APPLICATION ROUTES
 * ================================================================
 *
 * Purpose:
 * Defines the complete top-level navigation structure for the
 * Romelt TechCare public website and administrator portal.
 *
 * Responsibilities:
 * - Connects public browser URLs to public website pages.
 * - Wraps public pages in the shared PublicLayout.
 * - Delegates every /admin/* URL to AdminRoutes.
 * - Keeps administrator pages outside the public website layout.
 * - Registers legal and accessibility pages.
 * - Provides a fallback page for unknown public routes.
 *
 * Routing structure:
 *
 * Public website:
 * /
 * /services
 * /pricing
 * /about
 * /book
 * /contact
 * /privacy
 * /terms
 * /accessibility
 *
 * Administrator portal:
 * /admin/*
 *
 * Real-data integration:
 * - BookingPage connects to the public booking API.
 * - ContactPage connects to the public contact-inquiry API.
 * - Administrator authentication and protected routes are managed
 *   by AdminRoutes and AdminAuthContext.
 * - Administrator Bookings, Contact Inquiries, Services, Settings,
 *   and Administrator Users must be registered inside AdminRoutes.
 * ================================================================
 */

import { Route, Routes } from "react-router-dom";

import { PublicLayout } from "@/layouts/PublicLayout";

import { AboutPage } from "@/pages/AboutPage";
import { AccessibilityPage } from "@/pages/AccessibilityPage";
import { BookingPage } from "@/pages/BookingPage";
import { ContactPage } from "@/pages/ContactPage";
import { HomePage } from "@/pages/HomePage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { PricingPage } from "@/pages/PricingPage";
import { PrivacyPage } from "@/pages/PrivacyPage";
import { ServicesPage } from "@/pages/ServicesPage";
import { TermsPage } from "@/pages/TermsPage";

import AdminRoutes from "@/routes/AdminRoutes";

export function AppRoutes() {
  return (
    <Routes>
      {/* =========================================================
       * PUBLIC WEBSITE
       * ========================================================= */}
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />

        <Route path="services" element={<ServicesPage />} />

        <Route path="pricing" element={<PricingPage />} />

        <Route path="about" element={<AboutPage />} />

        <Route path="book" element={<BookingPage />} />

        <Route path="contact" element={<ContactPage />} />

        <Route path="privacy" element={<PrivacyPage />} />

        <Route path="terms" element={<TermsPage />} />

        <Route path="accessibility" element={<AccessibilityPage />} />
      </Route>

      {/* =========================================================
       * ADMINISTRATOR PORTAL
       *
       * AdminRoutes controls:
       * /admin
       * /admin/users
       * /admin/bookings
       * /admin/contact-inquiries
       * /admin/services
       * /admin/settings
       * ========================================================= */}
      <Route path="admin/*" element={<AdminRoutes />} />

      {/* =========================================================
       * UNKNOWN PUBLIC ROUTES
       * ========================================================= */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
