/**
 * ================================================================
 * ROMELT TECHCARE — ACCESSIBILITY MANAGER
 * ================================================================
 *
 * Purpose:
 * Provides website-wide keyboard and screen-reader accessibility
 * behavior as users navigate between public routes.
 *
 * Responsibilities:
 * - Adds a keyboard-accessible “Skip to main content” link.
 * - Finds the primary <main> element rendered by the active route.
 * - Assigns a stable main-content target when one is not present.
 * - Moves keyboard focus to the main content after route changes.
 * - Announces newly loaded pages to screen-reader users.
 * - Avoids intrusive visible focus changes for pointer navigation.
 *
 * Real-data integration:
 * This component is a frontend accessibility utility and does not
 * require backend integration.
 * ================================================================
 */

import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router";

const MAIN_CONTENT_ID = "main-content";

export function AccessibilityManager() {
  const location = useLocation();

  const [announcement, setAnnouncement] = useState("");
  const isInitialRender = useRef(true);

  /**
   * Ensures the active page has one stable skip-link destination.
   */
  useEffect(() => {
    const animationFrame = window.requestAnimationFrame(() => {
      const mainElement = document.querySelector<HTMLElement>("main");

      if (!mainElement) {
        return;
      }

      if (!mainElement.id) {
        mainElement.id = MAIN_CONTENT_ID;
      }

      if (!mainElement.hasAttribute("tabindex")) {
        mainElement.setAttribute("tabindex", "-1");
      }

      /**
       * Do not automatically move focus during the initial page load.
       *
       * On client-side route changes, focus is moved to the primary
       * content so keyboard and screen-reader users know that a new
       * page has loaded.
       */
      if (isInitialRender.current) {
        isInitialRender.current = false;
      } else {
        mainElement.focus({
          preventScroll: true,
        });

        mainElement.scrollIntoView({
          block: "start",
          behavior: "auto",
        });
      }

      setAnnouncement(getPageAnnouncement());
    });

    return () => {
      window.cancelAnimationFrame(animationFrame);
    };
  }, [location.pathname]);

  return (
    <>
      <a
        href={`#${MAIN_CONTENT_ID}`}
        className={[
          "fixed left-4 top-4 z-[100]",
          "-translate-y-24 rounded-xl",
          "bg-navy-950 px-5 py-3",
          "font-bold text-white shadow-xl",
          "transition-transform duration-150",
          "focus:translate-y-0",
          "focus:outline-none",
          "focus-visible:ring-4",
          "focus-visible:ring-brand-300",
          "focus-visible:ring-offset-2",
        ].join(" ")}
      >
        Skip to main content
      </a>

      <div aria-live="polite" aria-atomic="true" className="sr-only">
        {announcement}
      </div>
    </>
  );
}

function getPageAnnouncement(): string {
  const pageTitle = document.title.trim() || "Romelt TechCare";

  return `${pageTitle} page loaded`;
}
