/**
 * ================================================================
 * ROMELT TECHCARE — USE ADMIN AUTH HOOK
 * ================================================================
 *
 * Purpose:
 * Provides a safe reusable hook for accessing administrator
 * authentication state and operations.
 *
 * Responsibilities:
 * - Reads the AdminAuthContext.
 * - Prevents usage outside AdminAuthProvider.
 * - Gives pages and route guards one consistent authentication API.
 *
 * Real-data integration:
 * The returned operations ultimately communicate with the Spring
 * Boot administrator authentication endpoints.
 * ================================================================
 */

import { useContext } from "react";

import {
  AdminAuthContext,
  type AdminAuthContextValue,
} from "@/context/AdminAuthContext";

export function useAdminAuth(): AdminAuthContextValue {
  const context = useContext(AdminAuthContext);

  if (!context) {
    throw new Error("useAdminAuth must be used inside AdminAuthProvider.");
  }

  return context;
}
