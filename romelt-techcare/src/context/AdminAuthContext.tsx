/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION CONTEXT
 * ================================================================
 *
 * Purpose:
 * Provides centralized administrator authentication state throughout
 * the React application.
 *
 * Responsibilities:
 * - Restores the administrator session when the application starts.
 * - Verifies the stored session against the backend /me endpoint.
 * - Exposes the authenticated administrator profile.
 * - Exposes login, logout, refresh, and password-change operations.
 * - Reacts to administrator session changes from browser storage.
 * - Clears invalid or expired authentication sessions.
 *
 * Real-data integration:
 * Uses:
 *
 * POST /api/v1/admin/auth/login
 * GET  /api/v1/admin/auth/me
 * POST /api/v1/admin/auth/change-password
 *
 * Security:
 * - Passwords are never stored in React state after a request ends.
 * - Invalid HTTP 401 sessions are cleared by the shared API client.
 * - Protected routes should rely on this context rather than reading
 *   browser storage directly.
 * ================================================================
 */

import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";

import {
  changeAdministratorPassword,
  getCurrentAdministrator,
  loginAdministrator,
  logoutAdministrator,
} from "@/services/admin-auth.service";

import {
  ADMIN_SESSION_CHANGED_EVENT,
  clearAdminSession,
  getAdminSession,
} from "@/services/admin-session";

import type {
  AdminChangePasswordRequest,
  AdminLoginRequest,
  AdminProfile,
  AdminSession,
} from "@/types/admin-auth.types";

export interface AdminAuthContextValue {
  session: AdminSession | null;
  administrator: AdminProfile | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  isLoading: boolean;
  mustChangePassword: boolean;
  login: (request: AdminLoginRequest) => Promise<AdminSession>;
  logout: () => void;
  refreshAdministrator: () => Promise<AdminProfile | null>;
  changePassword: (
    request: AdminChangePasswordRequest,
  ) => Promise<AdminProfile>;
}

export const AdminAuthContext = createContext<AdminAuthContextValue | null>(
  null,
);

export function AdminAuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AdminSession | null>(null);

  const [isInitializing, setIsInitializing] = useState(true);

  const [isLoading, setIsLoading] = useState(false);

  const synchronizeSession = useCallback(() => {
    setSession(getAdminSession());
  }, []);

  const refreshAdministrator =
    useCallback(async (): Promise<AdminProfile | null> => {
      const currentSession = getAdminSession();

      if (!currentSession) {
        setSession(null);
        return null;
      }

      try {
        const administrator = await getCurrentAdministrator();

        const updatedSession = getAdminSession();

        setSession(updatedSession);

        return administrator;
      } catch {
        clearAdminSession();
        setSession(null);

        return null;
      }
    }, []);

  useEffect(() => {
    let isMounted = true;

    async function initializeAuthentication() {
      const storedSession = getAdminSession();

      if (!storedSession) {
        if (isMounted) {
          setSession(null);
          setIsInitializing(false);
        }

        return;
      }

      if (isMounted) {
        setSession(storedSession);
      }

      try {
        await getCurrentAdministrator();

        if (isMounted) {
          setSession(getAdminSession());
        }
      } catch {
        clearAdminSession();

        if (isMounted) {
          setSession(null);
        }
      } finally {
        if (isMounted) {
          setIsInitializing(false);
        }
      }
    }

    void initializeAuthentication();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    window.addEventListener(ADMIN_SESSION_CHANGED_EVENT, synchronizeSession);

    window.addEventListener("storage", synchronizeSession);

    return () => {
      window.removeEventListener(
        ADMIN_SESSION_CHANGED_EVENT,
        synchronizeSession,
      );

      window.removeEventListener("storage", synchronizeSession);
    };
  }, [synchronizeSession]);

  const login = useCallback(
    async (request: AdminLoginRequest): Promise<AdminSession> => {
      setIsLoading(true);

      try {
        const authenticatedSession = await loginAdministrator(request);

        setSession(authenticatedSession);

        return authenticatedSession;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const logout = useCallback(() => {
    logoutAdministrator();
    setSession(null);
  }, []);

  const changePassword = useCallback(
    async (request: AdminChangePasswordRequest): Promise<AdminProfile> => {
      setIsLoading(true);

      try {
        const administrator = await changeAdministratorPassword(request);

        setSession(getAdminSession());

        return administrator;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const value = useMemo<AdminAuthContextValue>(
    () => ({
      session,
      administrator: session?.administrator ?? null,
      isAuthenticated: session !== null,
      isInitializing,
      isLoading,
      mustChangePassword: session?.administrator.mustChangePassword ?? false,
      login,
      logout,
      refreshAdministrator,
      changePassword,
    }),
    [
      session,
      isInitializing,
      isLoading,
      login,
      logout,
      refreshAdministrator,
      changePassword,
    ],
  );

  return (
    <AdminAuthContext.Provider value={value}>
      {children}
    </AdminAuthContext.Provider>
  );
}
