/**
 * ================================================================
 * ROMELT TECHCARE — APPLICATION ROUTES
 * ================================================================
 *
 * Purpose:
 * Defines all public-facing navigation routes for the website.
 *
 * Responsibilities:
 * - Connects browser URLs to website pages.
 * - Wraps public pages in the shared public layout.
 * - Registers legal and accessibility pages.
 * - Provides a fallback page for unknown routes.
 *
 * Real-data integration:
 * Customer, technician, employee, administrator, authentication,
 * booking-management, and service-request routes will be added as
 * the corresponding backend features are implemented.
 * ================================================================
 */

import { Route, Routes } from "react-router";

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

export function AppRoutes() {
  return (
    <Routes>
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

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
