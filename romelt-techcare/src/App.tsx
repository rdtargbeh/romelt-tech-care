/**
 * ================================================================
 * ROMELT TECHCARE — ROOT APPLICATION COMPONENT
 * ================================================================
 *
 * Purpose:
 * Initializes the website router and shared application-level
 * services used across all public routes.
 *
 * Responsibilities:
 * - Provides BrowserRouter context.
 * - Activates route-based SEO metadata.
 * - Adds LocalBusiness structured data.
 * - Provides global route accessibility management.
 * - Restores page position when routes change.
 * - Provides the reusable floating Scroll to Top button.
 * - Renders all public application routes.
 *
 * Real-data integration:
 * Authentication providers, API-query providers, notification
 * providers, customer sessions, and backend data services may be
 * added here as application functionality is implemented.
 * ================================================================
 */

import { BrowserRouter } from "react-router";

import { AccessibilityManager } from "@/components/accessibility/AccessibilityManager";
import { ScrollToTop } from "@/components/common/ScrollToTop";
import { SeoManager } from "@/components/seo/SeoManager";
import { StructuredData } from "@/components/seo/StructuredData";
import { AppRoutes } from "@/routes/AppRoutes";

export default function App() {
  return (
    <BrowserRouter>
      <SeoManager />
      <StructuredData />
      <AccessibilityManager />
      <ScrollToTop />

      <AppRoutes />
    </BrowserRouter>
  );
}
