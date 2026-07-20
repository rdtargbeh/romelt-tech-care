/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PAGE LAYOUT
 * ================================================================
 *
 * Purpose:
 * Provides the shared visual structure for every public website page.
 *
 * Responsibilities:
 * - Displays the global header and footer.
 * - Renders the currently matched child route.
 * - Provides the mobile contact action bar.
 * - Restores the page to the top when navigation occurs.
 *
 * Real-data integration:
 * Public business settings may later be loaded at this layout level
 * and shared with navigation, footer, and child pages.
 * ================================================================
 */

import { useEffect } from "react";
import { Outlet, useLocation } from "react-router";

import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { MobileContactBar } from "@/components/layout/MobileContactBar";

export function PublicLayout() {
  const location = useLocation();

  useEffect(() => {
    window.scrollTo({
      top: 0,
      behavior: "instant",
    });
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="flex-1 pb-20 sm:pb-0">
        <Outlet />
      </main>

      <Footer />
      <MobileContactBar />
    </div>
  );
}
