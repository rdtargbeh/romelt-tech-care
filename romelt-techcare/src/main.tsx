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
 * - Provides the administrator authentication context.
 * - Restores and verifies stored administrator sessions.
 * - Renders the root App component.
 *
 * Real-data integration:
 * AdminAuthProvider manages administrator authentication through:
 *
 * POST /api/v1/admin/auth/login
 * GET  /api/v1/admin/auth/me
 * POST /api/v1/admin/auth/change-password
 *
 * Additional global providers for API caching, analytics, customer
 * sessions, and error monitoring can be added here later.
 * ================================================================
 */

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import App from "./App";
import { AdminAuthProvider } from "./context/AdminAuthContext";
import "./index.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error(
    "Romelt TechCare could not start because the root HTML element was not found.",
  );
}

createRoot(rootElement).render(
  <StrictMode>
    <AdminAuthProvider>
      <App />
    </AdminAuthProvider>
  </StrictMode>,
);
