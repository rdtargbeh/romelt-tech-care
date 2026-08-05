/**
 * ================================================================
 * ROMELT TECHCARE — VITE ENVIRONMENT TYPES
 * ================================================================
 *
 * Purpose:
 * Defines TypeScript types for all supported Vite environment
 * variables used by the frontend.
 *
 * Responsibilities:
 * - Provides autocomplete for import.meta.env values.
 * - Prevents accidental use of undocumented variables.
 * - Documents the expected public runtime configuration.
 *
 * Security:
 * Values beginning with VITE_ are exposed to the browser and must
 * never contain secrets or private credentials.
 * ================================================================
 */

/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SITE_URL: string;
  readonly VITE_API_BASE_URL: string;

  readonly VITE_APP_ENV: "development" | "test" | "staging" | "production";

  readonly VITE_ENABLE_DEBUG_LOGGING: "true" | "false";

  readonly VITE_ENABLE_CONTACT_API: "true" | "false";

  readonly VITE_ENABLE_BOOKING_API: "true" | "false";
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
