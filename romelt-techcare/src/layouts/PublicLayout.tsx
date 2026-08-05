/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE LAYOUT
 * ================================================================
 *
 * Purpose:
 * Provides the shared structure used by all public website pages.
 *
 * Responsibilities:
 * - Displays the website header.
 * - Renders the active public page.
 * - Displays the website footer.
 * - Maintains consistent spacing and navigation.
 *
 * Real-data integration:
 * Business contact information, operating hours, service areas,
 * promotions, and navigation links can later come from the backend.
 * ================================================================
 */

import { Outlet } from "react-router";

import { Footer } from "../components/layout/Footer";
import { Header } from "../components/layout/Header";

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <Header />

      <main>
        <Outlet />
      </main>

      <Footer />
    </div>
  );
}
