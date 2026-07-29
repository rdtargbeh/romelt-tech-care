/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR LOGIN PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the secure sign-in interface for Romelt TechCare
 * administrators.
 *
 * Responsibilities:
 * - Collects administrator email and password.
 * - Validates required fields before submission.
 * - Authenticates through AdminAuthContext.
 * - Displays backend and network errors safely.
 * - Redirects authenticated administrators to their requested page.
 * - Redirects administrators requiring a password change.
 * - Prevents duplicate form submissions.
 *
 * Real-data integration:
 * Authentication is performed through:
 *
 * POST /api/v1/admin/auth/login
 *
 * Security:
 * - Passwords are never stored.
 * - The password field supports intentional visibility toggling.
 * - Backend error details are displayed without exposing tokens or
 *   sensitive internal information.
 * ================================================================
 */

import {
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from "lucide-react";
import { type ChangeEvent, type FormEvent, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { useAdminAuth } from "@/hooks/useAdminAuth";
import { ApiError } from "@/lib/api-error";
import type { RedirectLocationState } from "@/types/router-state.types";
import { resolveAdminRedirectPath } from "@/utils/admin-navigation";

interface LoginFormState {
  email: string;
  password: string;
}

interface LoginFormErrors {
  email?: string;
  password?: string;
  general?: string;
}

const INITIAL_FORM_STATE: LoginFormState = {
  email: "",
  password: "",
};

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const { login, isLoading } = useAdminAuth();

  const [form, setForm] = useState<LoginFormState>(INITIAL_FORM_STATE);

  const [errors, setErrors] = useState<LoginFormErrors>({});

  const [showPassword, setShowPassword] = useState(false);

  const redirectPath = useMemo(
    () =>
      resolveAdminRedirectPath(location.state as RedirectLocationState | null),
    [location.state],
  );

  function handleFieldChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setErrors((current) => ({
      ...current,
      [name]: undefined,
      general: undefined,
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const validationErrors = validateLoginForm(form);

    if (validationErrors.email || validationErrors.password) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});

    try {
      const session = await login({
        email: form.email.trim().toLowerCase(),
        password: form.password,
      });

      setForm(INITIAL_FORM_STATE);

      if (session.administrator.mustChangePassword) {
        navigate("/admin/change-password", {
          replace: true,
        });

        return;
      }

      navigate(redirectPath, {
        replace: true,
      });
    } catch (error) {
      setErrors(resolveLoginErrors(error));
    }
  }

  return (
    <main className="min-h-screen bg-slate-50">
      <div className="grid min-h-screen lg:grid-cols-[minmax(0,1fr)_minmax(460px,0.72fr)]">
        <section className="hidden bg-navy-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
          <Link
            to="/"
            className="focus-ring inline-flex w-fit items-center gap-3 rounded-xl"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white text-navy-950">
              <ShieldCheck className="h-7 w-7" aria-hidden="true" />
            </div>

            <div>
              <p className="font-display text-xl font-extrabold">
                Romelt TechCare
              </p>

              <p className="text-sm text-slate-300">Administration Portal</p>
            </div>
          </Link>

          <div className="max-w-xl">
            <p className="text-sm font-bold uppercase tracking-[0.22em] text-brand-300">
              Secure administration
            </p>

            <h1 className="mt-5 font-display text-4xl font-extrabold leading-tight xl:text-5xl">
              Manage customer service operations from one protected workspace.
            </h1>

            <p className="mt-6 max-w-lg text-lg leading-8 text-slate-300">
              Review service requests, communicate with customers, coordinate
              appointments, and oversee Romelt TechCare operations.
            </p>
          </div>

          <p className="text-sm text-slate-400">
            Authorized administrators only.
          </p>
        </section>

        <section className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-8 lg:px-12">
          <div className="w-full max-w-md">
            <div className="mb-8 lg:hidden">
              <Link
                to="/"
                className="focus-ring inline-flex items-center gap-3 rounded-xl"
              >
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-navy-950 text-white">
                  <ShieldCheck className="h-6 w-6" aria-hidden="true" />
                </div>

                <div>
                  <p className="font-display text-lg font-extrabold text-navy-950">
                    Romelt TechCare
                  </p>

                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Admin Portal
                  </p>
                </div>
              </Link>
            </div>

            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
              <div>
                <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
                  Administrator access
                </p>

                <h1 className="mt-3 font-display text-3xl font-extrabold text-navy-950">
                  Sign in
                </h1>

                <p className="mt-3 leading-7 text-slate-600">
                  Enter your administrator credentials to continue.
                </p>
              </div>

              <form
                className="mt-8 space-y-5"
                onSubmit={handleSubmit}
                noValidate
              >
                {errors.general && (
                  <div
                    className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-800"
                    role="alert"
                    aria-live="assertive"
                  >
                    {errors.general}
                  </div>
                )}

                <div>
                  <label
                    htmlFor="admin-email"
                    className="block text-sm font-bold text-slate-800"
                  >
                    Email address
                  </label>

                  <div className="relative mt-2">
                    <Mail
                      className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                      aria-hidden="true"
                    />

                    <input
                      id="admin-email"
                      name="email"
                      type="email"
                      inputMode="email"
                      autoComplete="username"
                      value={form.email}
                      onChange={handleFieldChange}
                      disabled={isLoading}
                      aria-invalid={errors.email ? "true" : "false"}
                      aria-describedby={
                        errors.email ? "admin-email-error" : undefined
                      }
                      placeholder="admin@romelttechcare.com"
                      className="focus-ring min-h-12 w-full rounded-xl border border-slate-300 bg-white py-3 pl-12 pr-4 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
                    />
                  </div>

                  {errors.email && (
                    <p
                      id="admin-email-error"
                      className="mt-2 text-sm text-red-700"
                    >
                      {errors.email}
                    </p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="admin-password"
                    className="block text-sm font-bold text-slate-800"
                  >
                    Password
                  </label>

                  <div className="relative mt-2">
                    <LockKeyhole
                      className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                      aria-hidden="true"
                    />

                    <input
                      id="admin-password"
                      name="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      value={form.password}
                      onChange={handleFieldChange}
                      disabled={isLoading}
                      aria-invalid={errors.password ? "true" : "false"}
                      aria-describedby={
                        errors.password ? "admin-password-error" : undefined
                      }
                      placeholder="Enter your password"
                      className="focus-ring min-h-12 w-full rounded-xl border border-slate-300 bg-white py-3 pl-12 pr-12 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
                    />

                    <button
                      type="button"
                      onClick={() => setShowPassword((current) => !current)}
                      disabled={isLoading}
                      className="focus-ring absolute right-2 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                      aria-label={
                        showPassword ? "Hide password" : "Show password"
                      }
                    >
                      {showPassword ? (
                        <EyeOff className="h-5 w-5" aria-hidden="true" />
                      ) : (
                        <Eye className="h-5 w-5" aria-hidden="true" />
                      )}
                    </button>
                  </div>

                  {errors.password && (
                    <p
                      id="admin-password-error"
                      className="mt-2 text-sm text-red-700"
                    >
                      {errors.password}
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="focus-ring inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 py-3 font-bold text-white transition hover:bg-navy-900 disabled:cursor-not-allowed disabled:opacity-65"
                >
                  {isLoading ? (
                    <>
                      <LoaderCircle
                        className="h-5 w-5 animate-spin"
                        aria-hidden="true"
                      />
                      Signing in...
                    </>
                  ) : (
                    <>
                      <ShieldCheck className="h-5 w-5" aria-hidden="true" />
                      Sign in securely
                    </>
                  )}
                </button>
              </form>

              <div className="mt-7 border-t border-slate-200 pt-6">
                <p className="text-center text-sm leading-6 text-slate-600">
                  This portal is restricted to authorized Romelt TechCare
                  personnel.
                </p>
              </div>
            </div>

            <p className="mt-6 text-center text-sm text-slate-500">
              <Link
                to="/"
                className="focus-ring rounded-md font-bold text-navy-950 transition hover:text-brand-700"
              >
                Return to the public website
              </Link>
            </p>
          </div>
        </section>
      </div>
    </main>
  );
}

function validateLoginForm(form: LoginFormState): LoginFormErrors {
  const errors: LoginFormErrors = {};

  const email = form.email.trim();

  if (!email) {
    errors.email = "Enter your administrator email address.";
  } else if (!isValidEmail(email)) {
    errors.email = "Enter a valid email address.";
  }

  if (!form.password) {
    errors.password = "Enter your administrator password.";
  }

  return errors;
}

function resolveLoginErrors(error: unknown): LoginFormErrors {
  if (!(error instanceof ApiError)) {
    return {
      general: "Unable to sign in. Please try again.",
    };
  }

  const errors: LoginFormErrors = {
    general: error.message,
  };

  const emailError =
    error.validationErrors.email ??
    error.fieldErrors.find((fieldError) => fieldError.field === "email")
      ?.message;

  const passwordError =
    error.validationErrors.password ??
    error.fieldErrors.find((fieldError) => fieldError.field === "password")
      ?.message;

  if (emailError) {
    errors.email = emailError;
  }

  if (passwordError) {
    errors.password = passwordError;
  }

  if (emailError || passwordError) {
    errors.general = undefined;
  }

  return errors;
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
