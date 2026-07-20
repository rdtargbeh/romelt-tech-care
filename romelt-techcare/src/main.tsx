/**
 * ================================================================
 * ROMELT TECHCARE — APPLICATION ENTRY POINT
 * ================================================================
 *
 * Purpose:
 * Starts the React application and mounts it into the browser DOM.
 *
 * Responsibilities:
 * - Enables React StrictMode for development safeguards.
 * - Loads the global Tailwind and brand styles.
 * - Renders the root App component.
 *
 * Real-data integration:
 * Global providers for API caching, authentication, analytics,
 * customer sessions, and error monitoring can be added here later.
 * ================================================================
 */

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import App from "./App";
import "./index.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error(
    "Romelt TechCare could not start because the root HTML element was not found.",
  );
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
