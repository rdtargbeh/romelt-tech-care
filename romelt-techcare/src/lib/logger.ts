/**
 * ================================================================
 * ROMELT TECHCARE — FRONTEND LOGGER
 * ================================================================
 *
 * Purpose:
 * Provides centralized browser logging that can be disabled in
 * production while preserving warnings and errors.
 *
 * Responsibilities:
 * - Allows debug and informational logs during development.
 * - Suppresses development-only logs in production.
 * - Keeps warning and error reporting available.
 * - Prevents application files from using console methods directly.
 *
 * Real-data integration:
 * A production monitoring provider may later replace or extend this
 * logger for error tracking and application diagnostics.
 * ================================================================
 */

import { environmentConfig } from "@/config/environment.config";

type LogArgument = unknown;

export const logger = {
  debug(...arguments_: LogArgument[]): void {
    if (environmentConfig.enableDebugLogging) {
      console.debug("[Romelt TechCare]", ...arguments_);
    }
  },

  info(...arguments_: LogArgument[]): void {
    if (environmentConfig.enableDebugLogging) {
      console.info("[Romelt TechCare]", ...arguments_);
    }
  },

  warn(...arguments_: LogArgument[]): void {
    console.warn("[Romelt TechCare]", ...arguments_);
  },

  error(...arguments_: LogArgument[]): void {
    console.error("[Romelt TechCare]", ...arguments_);
  },
};
