/**
 * ================================================================
 * ROMELT TECHCARE — ENVIRONMENT CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Provides one centralized and validated source for all frontend
 * environment variables used by Romelt TechCare.
 *
 * Responsibilities:
 * - Reads Vite environment variables.
 * - Provides safe local-development defaults.
 * - Normalizes website and backend API URLs.
 * - Converts string feature flags into booleans.
 * - Detects development, test, staging, and production modes.
 * - Enables real backend integration by default in development.
 * - Prevents application files from reading import.meta.env directly.
 *
 * Real-data integration:
 * Local development connects to:
 * http://localhost:8080/api/v1
 *
 * Production values must be supplied through `.env.production`
 * or the production hosting platform.
 *
 * Security:
 * Every variable beginning with VITE_ is exposed to the browser.
 * Never store passwords, private keys, database credentials,
 * access tokens, or other secrets in VITE_ variables.
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

const DEFAULT_DEVELOPMENT_SITE_URL = "http://localhost:5173";

const DEFAULT_DEVELOPMENT_API_BASE_URL = "http://localhost:8080/api/v1";

const DEFAULT_PRODUCTION_SITE_URL = "https://www.romelttechcare.com";

const DEFAULT_PRODUCTION_API_BASE_URL = "https://api.romelttechcare.com/api/v1";

const appEnvironment = parseApplicationEnvironment(
  import.meta.env.VITE_APP_ENV,
);

const isDevelopment = appEnvironment === "development";
const isTest = appEnvironment === "test";
const isStaging = appEnvironment === "staging";
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

/*
 * Real public API integration is enabled by default outside tests.
 *
 * Environment files may still explicitly disable either API by
 * setting its value to false.
 */
const defaultPublicApiEnabled = !isTest;

const enableDebugLogging = parseBoolean(
  import.meta.env.VITE_ENABLE_DEBUG_LOGGING,
  !isProduction,
  "VITE_ENABLE_DEBUG_LOGGING",
);

const enableContactApi = parseBoolean(
  import.meta.env.VITE_ENABLE_CONTACT_API,
  defaultPublicApiEnabled,
  "VITE_ENABLE_CONTACT_API",
);

const enableBookingApi = parseBoolean(
  import.meta.env.VITE_ENABLE_BOOKING_API,
  defaultPublicApiEnabled,
  "VITE_ENABLE_BOOKING_API",
);

export const environmentConfig: Readonly<EnvironmentConfig> = Object.freeze({
  appEnvironment,

  siteUrl,
  apiBaseUrl,

  enableDebugLogging,
  enableContactApi,
  enableBookingApi,

  isDevelopment,
  isTest,
  isStaging,
  isProduction,
});

if (enableDebugLogging) {
  console.info("[Romelt TechCare] Environment configuration loaded.", {
    appEnvironment,
    siteUrl,
    apiBaseUrl,
    enableContactApi,
    enableBookingApi,
  });
}

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
    `[Environment] ${variableName} contains invalid boolean value ` +
      `"${value}". Using fallback value "${fallback}".`,
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
      throw new Error("Only HTTP and HTTPS URLs are supported.");
    }

    return normalizedValue;
  } catch {
    console.warn(
      `[Environment] ${variableName} contains invalid URL ` +
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
