/**
 * ================================================================
 * ROMELT TECHCARE — SCROLL TO TOP
 * ================================================================
 *
 * Purpose:
 * Provides consistent page-position behavior when users navigate
 * between routes and while reading long website pages.
 *
 * Responsibilities:
 * - Scrolls the window to the top whenever the route changes.
 * - Displays a floating Scroll to Top button after the user scrolls.
 * - Supports keyboard and screen-reader users.
 * - Respects the user's reduced-motion preference.
 * - Avoids rendering the floating button near the top of the page.
 *
 * Real-data integration:
 * This component is a frontend navigation utility and does not
 * require backend integration.
 * ================================================================
 */

import { useEffect, useState } from "react";
import { ArrowUp } from "lucide-react";
import { useLocation } from "react-router";

const BUTTON_VISIBILITY_THRESHOLD = 500;

export function ScrollToTop() {
  const location = useLocation();

  const [isVisible, setIsVisible] = useState(false);

  /**
   * Reset the page position whenever the route changes.
   *
   * This prevents a newly opened page from retaining the previous
   * page's scroll position.
   */
  useEffect(() => {
    window.scrollTo({
      top: 0,
      left: 0,
      behavior: "auto",
    });
  }, [location.pathname]);

  /**
   * Show the floating button only after the user has scrolled far
   * enough down the current page.
   */
  useEffect(() => {
    const handleScroll = () => {
      setIsVisible(window.scrollY >= BUTTON_VISIBILITY_THRESHOLD);
    };

    handleScroll();

    window.addEventListener("scroll", handleScroll, {
      passive: true,
    });

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  const handleScrollToTop = () => {
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;

    window.scrollTo({
      top: 0,
      left: 0,
      behavior: prefersReducedMotion ? "auto" : "smooth",
    });
  };

  return (
    <button
      type="button"
      onClick={handleScrollToTop}
      aria-label="Scroll to the top of the page"
      title="Scroll to top"
      className={[
        "focus-ring fixed bottom-5 right-5 z-50",
        "flex h-12 w-12 items-center justify-center",
        "rounded-full border border-white/20",
        "bg-navy-950 text-white shadow-xl",
        "transition duration-200",
        "hover:-translate-y-1 hover:bg-brand-700",
        "focus-visible:outline-none",
        "sm:bottom-7 sm:right-7",
        isVisible
          ? "pointer-events-auto translate-y-0 opacity-100"
          : "pointer-events-none translate-y-4 opacity-0",
      ].join(" ")}
    >
      <ArrowUp className="h-5 w-5" aria-hidden="true" />
    </button>
  );
}
