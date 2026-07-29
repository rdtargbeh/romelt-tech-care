/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR CHANGE PASSWORD PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the secure password-change workflow for authenticated
 * Romelt TechCare administrators.
 *
 * Responsibilities:
 * - Supports mandatory first-login password changes.
 * - Supports voluntary password changes for signed-in administrators.
 * - Collects the current, new, and confirmed passwords.
 * - Performs client-side password validation.
 * - Submits the password-change request through AdminAuthContext.
 * - Displays backend validation and authentication errors.
 * - Redirects the administrator to the dashboard after success.
 * - Prevents duplicate submissions.
 *
 * Real-data integration:
 * Uses:
 *
 * POST /api/v1/admin/auth/change-password
 *
 * Security:
 * - Passwords are never stored in browser storage.
 * - Password values are cleared after successful submission.
 * - Password visibility is controlled independently for each field.
 * - Backend error messages are displayed without exposing tokens or
 *   internal stack traces.
 * ================================================================
 */

import {
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  LoaderCircle,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";
import { type ChangeEvent, type FormEvent, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { useAdminAuth } from "@/hooks/useAdminAuth";
import { ApiError } from "@/lib/api-error";

interface ChangePasswordFormState {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

interface ChangePasswordFormErrors {
  currentPassword?: string;
  newPassword?: string;
  confirmNewPassword?: string;
  general?: string;
}

type PasswordFieldName =
  | "currentPassword"
  | "newPassword"
  | "confirmNewPassword";

const INITIAL_FORM_STATE: ChangePasswordFormState = {
  currentPassword: "",
  newPassword: "",
  confirmNewPassword: "",
};

export default function AdminChangePasswordPage() {
  const navigate = useNavigate();

  const {
    administrator,
    changePassword,
    isLoading,
    mustChangePassword,
    logout,
  } = useAdminAuth();

  const [form, setForm] = useState<ChangePasswordFormState>(INITIAL_FORM_STATE);

  const [errors, setErrors] = useState<ChangePasswordFormErrors>({});

  const [visibleFields, setVisibleFields] = useState<
    Record<PasswordFieldName, boolean>
  >({
    currentPassword: false,
    newPassword: false,
    confirmNewPassword: false,
  });

  const passwordRequirements = useMemo(
    () => evaluatePasswordRequirements(form.newPassword),
    [form.newPassword],
  );

  function handleFieldChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target as HTMLInputElement & {
      name: PasswordFieldName;
    };

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

  function togglePasswordVisibility(fieldName: PasswordFieldName) {
    setVisibleFields((current) => ({
      ...current,
      [fieldName]: !current[fieldName],
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const validationErrors = validateChangePasswordForm(form);

    if (
      validationErrors.currentPassword ||
      validationErrors.newPassword ||
      validationErrors.confirmNewPassword
    ) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});

    try {
      await changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
        confirmNewPassword: form.confirmNewPassword,
      });

      setForm(INITIAL_FORM_STATE);

      navigate("/admin", {
        replace: true,
        state: {
          passwordChanged: true,
        },
      });
    } catch (error) {
      setErrors(resolveChangePasswordErrors(error));
    }
  }

  function handleCancel() {
    if (mustChangePassword) {
      logout();

      navigate("/admin/login", {
        replace: true,
      });

      return;
    }

    navigate("/admin", {
      replace: true,
    });
  }

  return (
    <main className="min-h-screen bg-slate-50">
      <div className="grid min-h-screen lg:grid-cols-[minmax(0,0.78fr)_minmax(520px,1fr)]">
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
              Account security
            </p>

            <h1 className="mt-5 font-display text-4xl font-extrabold leading-tight xl:text-5xl">
              Protect administrative access with a strong, private password.
            </h1>

            <p className="mt-6 max-w-lg text-lg leading-8 text-slate-300">
              Use a unique password that is not shared with another website,
              service, or employee account.
            </p>
          </div>

          <p className="text-sm text-slate-400">
            Authorized administrators only.
          </p>
        </section>

        <section className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-8 lg:px-12">
          <div className="w-full max-w-xl">
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
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
                  <KeyRound className="h-6 w-6" aria-hidden="true" />
                </div>

                <div>
                  <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
                    Account security
                  </p>

                  <h1 className="mt-2 font-display text-3xl font-extrabold text-navy-950">
                    Change your password
                  </h1>
                </div>
              </div>

              <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                {mustChangePassword ? (
                  <p className="text-sm leading-6 text-slate-700">
                    You must create a new password before accessing the
                    administrator dashboard.
                  </p>
                ) : (
                  <p className="text-sm leading-6 text-slate-700">
                    Signed in as{" "}
                    <span className="font-bold text-navy-950">
                      {administrator?.email ?? "administrator"}
                    </span>
                    .
                  </p>
                )}
              </div>

              <form
                className="mt-7 space-y-5"
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

                <PasswordInput
                  id="current-password"
                  name="currentPassword"
                  label="Current password"
                  value={form.currentPassword}
                  error={errors.currentPassword}
                  visible={visibleFields.currentPassword}
                  autoComplete="current-password"
                  placeholder="Enter your current password"
                  disabled={isLoading}
                  onChange={handleFieldChange}
                  onToggleVisibility={() =>
                    togglePasswordVisibility("currentPassword")
                  }
                />

                <PasswordInput
                  id="new-password"
                  name="newPassword"
                  label="New password"
                  value={form.newPassword}
                  error={errors.newPassword}
                  visible={visibleFields.newPassword}
                  autoComplete="new-password"
                  placeholder="Create a new password"
                  disabled={isLoading}
                  onChange={handleFieldChange}
                  onToggleVisibility={() =>
                    togglePasswordVisibility("newPassword")
                  }
                />

                <PasswordRequirements requirements={passwordRequirements} />

                <PasswordInput
                  id="confirm-new-password"
                  name="confirmNewPassword"
                  label="Confirm new password"
                  value={form.confirmNewPassword}
                  error={errors.confirmNewPassword}
                  visible={visibleFields.confirmNewPassword}
                  autoComplete="new-password"
                  placeholder="Re-enter your new password"
                  disabled={isLoading}
                  onChange={handleFieldChange}
                  onToggleVisibility={() =>
                    togglePasswordVisibility("confirmNewPassword")
                  }
                />

                <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={isLoading}
                    className="focus-ring inline-flex min-h-12 items-center justify-center rounded-xl border border-slate-300 bg-white px-5 py-3 font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {mustChangePassword ? "Sign out" : "Cancel"}
                  </button>

                  <button
                    type="submit"
                    disabled={isLoading}
                    className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 py-3 font-bold text-white transition hover:bg-navy-900 disabled:cursor-not-allowed disabled:opacity-65"
                  >
                    {isLoading ? (
                      <>
                        <LoaderCircle
                          className="h-5 w-5 animate-spin"
                          aria-hidden="true"
                        />
                        Updating password...
                      </>
                    ) : (
                      <>
                        <LockKeyhole className="h-5 w-5" aria-hidden="true" />
                        Update password
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>

            <p className="mt-6 text-center text-sm text-slate-500">
              Need assistance? Contact the Romelt TechCare system administrator.
            </p>
          </div>
        </section>
      </div>
    </main>
  );
}

interface PasswordInputProps {
  id: string;
  name: PasswordFieldName;
  label: string;
  value: string;
  error?: string;
  visible: boolean;
  autoComplete: string;
  placeholder: string;
  disabled: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onToggleVisibility: () => void;
}

function PasswordInput({
  id,
  name,
  label,
  value,
  error,
  visible,
  autoComplete,
  placeholder,
  disabled,
  onChange,
  onToggleVisibility,
}: PasswordInputProps) {
  const errorId = `${id}-error`;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-bold text-slate-800">
        {label}
      </label>

      <div className="relative mt-2">
        <LockKeyhole
          className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
          aria-hidden="true"
        />

        <input
          id={id}
          name={name}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          value={value}
          onChange={onChange}
          disabled={disabled}
          aria-invalid={error ? "true" : "false"}
          aria-describedby={error ? errorId : undefined}
          placeholder={placeholder}
          className="focus-ring min-h-12 w-full rounded-xl border border-slate-300 bg-white py-3 pl-12 pr-12 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
        />

        <button
          type="button"
          onClick={onToggleVisibility}
          disabled={disabled}
          className="focus-ring absolute right-2 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          aria-label={
            visible
              ? `Hide ${label.toLowerCase()}`
              : `Show ${label.toLowerCase()}`
          }
        >
          {visible ? (
            <EyeOff className="h-5 w-5" aria-hidden="true" />
          ) : (
            <Eye className="h-5 w-5" aria-hidden="true" />
          )}
        </button>
      </div>

      {error && (
        <p id={errorId} className="mt-2 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}

interface PasswordRequirementState {
  minimumLength: boolean;
  uppercase: boolean;
  lowercase: boolean;
  number: boolean;
  specialCharacter: boolean;
}

interface PasswordRequirementsProps {
  requirements: PasswordRequirementState;
}

function PasswordRequirements({ requirements }: PasswordRequirementsProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-sm font-bold text-slate-800">Password requirements</p>

      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        <RequirementItem
          satisfied={requirements.minimumLength}
          label="At least 8 characters"
        />

        <RequirementItem
          satisfied={requirements.uppercase}
          label="One uppercase letter"
        />

        <RequirementItem
          satisfied={requirements.lowercase}
          label="One lowercase letter"
        />

        <RequirementItem satisfied={requirements.number} label="One number" />

        <RequirementItem
          satisfied={requirements.specialCharacter}
          label="One special character"
        />
      </div>
    </div>
  );
}

interface RequirementItemProps {
  satisfied: boolean;
  label: string;
}

function RequirementItem({ satisfied, label }: RequirementItemProps) {
  return (
    <div
      className={`flex items-center gap-2 text-sm ${
        satisfied ? "text-emerald-700" : "text-slate-500"
      }`}
    >
      <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />

      <span>{label}</span>
    </div>
  );
}

function validateChangePasswordForm(
  form: ChangePasswordFormState,
): ChangePasswordFormErrors {
  const errors: ChangePasswordFormErrors = {};

  if (!form.currentPassword) {
    errors.currentPassword = "Enter your current password.";
  }

  if (!form.newPassword) {
    errors.newPassword = "Enter a new password.";
  } else {
    const requirementState = evaluatePasswordRequirements(form.newPassword);

    const allRequirementsSatisfied =
      Object.values(requirementState).every(Boolean);

    if (!allRequirementsSatisfied) {
      errors.newPassword =
        "Your new password does not meet all password requirements.";
    } else if (form.newPassword === form.currentPassword) {
      errors.newPassword =
        "Your new password must be different from your current password.";
    }
  }

  if (!form.confirmNewPassword) {
    errors.confirmNewPassword = "Confirm your new password.";
  } else if (form.confirmNewPassword !== form.newPassword) {
    errors.confirmNewPassword = "The password confirmation does not match.";
  }

  return errors;
}

function evaluatePasswordRequirements(
  password: string,
): PasswordRequirementState {
  return {
    minimumLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /\d/.test(password),
    specialCharacter: /[^A-Za-z0-9]/.test(password),
  };
}

function resolveChangePasswordErrors(error: unknown): ChangePasswordFormErrors {
  if (!(error instanceof ApiError)) {
    return {
      general: "Unable to update your password. Please try again.",
    };
  }

  const resolvedErrors: ChangePasswordFormErrors = {
    general: error.message,
  };

  const currentPasswordError = findFieldError(error, "currentPassword");

  const newPasswordError = findFieldError(error, "newPassword");

  const confirmPasswordError = findFieldError(error, "confirmNewPassword");

  if (currentPasswordError) {
    resolvedErrors.currentPassword = currentPasswordError;
  }

  if (newPasswordError) {
    resolvedErrors.newPassword = newPasswordError;
  }

  if (confirmPasswordError) {
    resolvedErrors.confirmNewPassword = confirmPasswordError;
  }

  if (currentPasswordError || newPasswordError || confirmPasswordError) {
    resolvedErrors.general = undefined;
  }

  return resolvedErrors;
}

function findFieldError(
  error: ApiError,
  fieldName: string,
): string | undefined {
  return (
    error.validationErrors[fieldName] ??
    error.fieldErrors.find((fieldError) => fieldError.field === fieldName)
      ?.message
  );
}
