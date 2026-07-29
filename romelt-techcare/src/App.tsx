/**
 * ================================================================
 * ROMELT TECHCARE — ROOT APPLICATION COMPONENT
 * ================================================================
 *
 * Purpose:
 * Initializes the Romelt TechCare application router and shared
 * application-level services used across public and administrator
 * routes.
 *
 * Responsibilities:
 * - Provides BrowserRouter context to the entire application.
 * - Activates route-based SEO metadata.
 * - Adds LocalBusiness structured data.
 * - Provides global route accessibility management.
 * - Restores page position when routes change.
 * - Provides the reusable floating Scroll to Top button.
 * - Renders all public and administrator application routes.
 *
 * Real-data integration:
 * Administrator authentication is provided globally through
 * AdminAuthProvider in src/main.tsx.
 *
 * Additional API-query, notification, analytics, customer-session,
 * and error-monitoring providers may be added as new application
 * functionality is implemented.
 * ================================================================
 */

import { BrowserRouter } from "react-router-dom";

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
