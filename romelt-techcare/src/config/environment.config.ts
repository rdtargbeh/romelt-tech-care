/**
 * ================================================================
 * ROMELT TECHCARE — ENVIRONMENT CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Provides one safe and centralized source for all frontend
 * environment variables.
 *
 * Responsibilities:
 * - Reads Vite environment variables.
 * - Provides safe local-development defaults.
 * - Normalizes website and API URLs.
 * - Converts string feature flags into booleans.
 * - Detects development, test, staging, and production modes.
 * - Prevents the application from crashing when local environment
 *   files have not yet been created.
 * - Prevents application files from reading import.meta.env directly.
 *
 * Real-data integration:
 * Production deployment values should be supplied through the
 * production hosting environment or `.env.production`.
 *
 * Security:
 * Every variable beginning with VITE_ is exposed to the browser.
 * Never place passwords, private keys, database credentials, tokens,
 * or other secrets in VITE_ environment variables.
 * ================================================================
 */

export type ApplicationEnvironment =
  | "development"
  | "test"
  | "staging"
  | "production";

interface EnvironmentConfig {
  appEnvironment: ApplicationEnvironment;

  siteUrl: string;
  apiBaseUrl: string;

  enableDebugLogging: boolean;
  enableContactApi: boolean;
  enableBookingApi: boolean;

  isDevelopment: boolean;
  isTest: boolean;
  isStaging: boolean;
  isProduction: boolean;
}

/**
 * Safe frontend defaults.
 *
 * These values allow the application to start even when no `.env`
 * file exists.
 *
 * Production values should still be configured through the hosting
 * environment or `.env.production`.
 */
const DEFAULT_DEVELOPMENT_SITE_URL = "http://localhost:5173";

const DEFAULT_DEVELOPMENT_API_BASE_URL = "http://localhost:8080/api/v1";

const DEFAULT_PRODUCTION_SITE_URL = "https://www.romelttechcare.com";

const DEFAULT_PRODUCTION_API_BASE_URL = "https://api.romelttechcare.com/api/v1";

const appEnvironment = parseApplicationEnvironment(
  import.meta.env.VITE_APP_ENV,
);

const isProduction = appEnvironment === "production";

const siteUrl = normalizeUrl(
  import.meta.env.VITE_SITE_URL,
  isProduction
    ? DEFAULT_PRODUCTION_SITE_URL
    : (getCurrentBrowserOrigin() ?? DEFAULT_DEVELOPMENT_SITE_URL),
  "VITE_SITE_URL",
);

const apiBaseUrl = normalizeUrl(
  import.meta.env.VITE_API_BASE_URL,
  isProduction
    ? DEFAULT_PRODUCTION_API_BASE_URL
    : DEFAULT_DEVELOPMENT_API_BASE_URL,
  "VITE_API_BASE_URL",
);

export const environmentConfig: Readonly<EnvironmentConfig> = Object.freeze({
  appEnvironment,

  siteUrl,
  apiBaseUrl,

  enableDebugLogging: parseBoolean(
    import.meta.env.VITE_ENABLE_DEBUG_LOGGING,
    !isProduction,
    "VITE_ENABLE_DEBUG_LOGGING",
  ),

  enableContactApi: parseBoolean(
    import.meta.env.VITE_ENABLE_CONTACT_API,
    false,
    "VITE_ENABLE_CONTACT_API",
  ),

  enableBookingApi: parseBoolean(
    import.meta.env.VITE_ENABLE_BOOKING_API,
    false,
    "VITE_ENABLE_BOOKING_API",
  ),

  isDevelopment: appEnvironment === "development",

  isTest: appEnvironment === "test",

  isStaging: appEnvironment === "staging",

  isProduction,
});

function parseApplicationEnvironment(
  value: string | undefined,
): ApplicationEnvironment {
  const normalizedValue = value?.trim().toLowerCase();

  switch (normalizedValue) {
    case "development":
    case "test":
    case "staging":
    case "production":
      return normalizedValue;

    default:
      return import.meta.env.PROD ? "production" : "development";
  }
}

function parseBoolean(
  value: string | undefined,
  fallback: boolean,
  variableName: string,
): boolean {
  if (value === undefined || value.trim() === "") {
    return fallback;
  }

  const normalizedValue = value.trim().toLowerCase();

  if (
    normalizedValue === "true" ||
    normalizedValue === "1" ||
    normalizedValue === "yes" ||
    normalizedValue === "on"
  ) {
    return true;
  }

  if (
    normalizedValue === "false" ||
    normalizedValue === "0" ||
    normalizedValue === "no" ||
    normalizedValue === "off"
  ) {
    return false;
  }

  console.warn(
    `[Environment] ${variableName} contains an invalid boolean ` +
      `value "${value}". Using fallback value "${fallback}".`,
  );

  return fallback;
}

function normalizeUrl(
  value: string | undefined,
  fallback: string,
  variableName: string,
): string {
  const candidate = value?.trim() || fallback;

  const normalizedValue = candidate.replace(/\/+$/, "");

  try {
    const parsedUrl = new URL(normalizedValue);

    if (parsedUrl.protocol !== "http:" && parsedUrl.protocol !== "https:") {
      throw new Error("Only HTTP and HTTPS are supported.");
    }

    return normalizedValue;
  } catch {
    console.warn(
      `[Environment] ${variableName} contains an invalid URL ` +
        `"${candidate}". Using fallback URL "${fallback}".`,
    );

    return fallback.replace(/\/+$/, "");
  }
}

function getCurrentBrowserOrigin(): string | null {
  if (typeof window === "undefined" || !window.location?.origin) {
    return null;
  }

  const origin = window.location.origin.trim();

  if (!origin || origin === "null") {
    return null;
  }

  return origin;
}
