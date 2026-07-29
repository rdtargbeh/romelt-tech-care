/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR PASSWORD RESET DIALOG
 * ================================================================
 *
 * Purpose:
 * Provides the protected workflow for assigning a temporary password
 * to an existing administrator account.
 *
 * Responsibilities:
 * - Collects and confirms a temporary password.
 * - Validates password strength before submission.
 * - Displays backend and client-side validation errors.
 * - Prevents duplicate submissions.
 * - Requires the administrator to change the password after login.
 * - Clears all password values when the dialog closes.
 *
 * Real-data integration:
 * Uses:
 *
 * POST /api/v1/admin/users/{adminUserId}/reset-password
 *
 * Security:
 * - Password values remain only in local component state.
 * - Password values are never stored in browser storage.
 * - Password fields are cleared when the dialog closes.
 * ================================================================
 */

import {
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  LoaderCircle,
  ShieldAlert,
  X,
} from "lucide-react";
import {
  type ChangeEvent,
  type FormEvent,
  useEffect,
  useMemo,
  useState,
} from "react";

import { ApiError } from "@/lib/api-error";
import { resetAdminUserPassword } from "@/services/admin-user.service";
import type {
  AdminPasswordResetFormErrors,
  AdminPasswordResetFormValues,
  AdminUser,
} from "@/types/admin-user.types";

interface AdminResetPasswordDialogProps {
  administrator: AdminUser;
  open: boolean;
  onClose: () => void;
  onPasswordReset: (administrator: AdminUser) => void;
}

const INITIAL_VALUES: AdminPasswordResetFormValues = {
  temporaryPassword: "",
  confirmTemporaryPassword: "",
};

export function AdminResetPasswordDialog({
  administrator,
  open,
  onClose,
  onPasswordReset,
}: AdminResetPasswordDialogProps) {
  const [values, setValues] =
    useState<AdminPasswordResetFormValues>(INITIAL_VALUES);

  const [errors, setErrors] = useState<AdminPasswordResetFormErrors>({});

  const [showTemporaryPassword, setShowTemporaryPassword] = useState(false);

  const [showConfirmTemporaryPassword, setShowConfirmTemporaryPassword] =
    useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordRequirements = useMemo(
    () => evaluatePasswordRequirements(values.temporaryPassword),
    [values.temporaryPassword],
  );

  useEffect(() => {
    if (!open) {
      resetDialog();
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && !isSubmitting) {
        handleClose();
      }
    }

    const previousOverflow = document.body.style.overflow;

    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open, isSubmitting]);

  if (!open) {
    return null;
  }

  function resetDialog() {
    setValues(INITIAL_VALUES);
    setErrors({});
    setShowTemporaryPassword(false);
    setShowConfirmTemporaryPassword(false);
    setIsSubmitting(false);
  }

  function handleClose() {
    if (isSubmitting) {
      return;
    }

    resetDialog();
    onClose();
  }

  function handleFieldChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setValues((current) => ({
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

    const validationErrors = validatePasswordReset(values);

    if (Object.values(validationErrors).some(Boolean)) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    setIsSubmitting(true);

    try {
      const updatedAdministrator = await resetAdminUserPassword(
        administrator.adminUserId,
        {
          temporaryPassword: values.temporaryPassword,
          confirmTemporaryPassword: values.confirmTemporaryPassword,
        },
      );

      resetDialog();
      onPasswordReset(updatedAdministrator);
      onClose();
    } catch (error) {
      setErrors(resolvePasswordResetErrors(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[70] flex items-end justify-center p-0 sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="reset-password-dialog-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/65"
        onClick={handleClose}
        disabled={isSubmitting}
        aria-label="Close password reset dialog"
      />

      <div className="relative z-10 max-h-[95vh] w-full overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:max-w-xl sm:rounded-3xl">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-5 sm:px-6">
          <div className="flex items-start gap-4">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
              <KeyRound className="h-6 w-6" aria-hidden="true" />
            </span>

            <div>
              <p className="text-sm font-bold uppercase tracking-[0.16em] text-amber-700">
                Account security
              </p>

              <h2
                id="reset-password-dialog-title"
                className="mt-1 font-display text-2xl font-extrabold text-navy-950"
              >
                Reset administrator password
              </h2>
            </div>
          </div>

          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            className="focus-ring flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="px-5 py-6 sm:px-6">
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4">
              <div className="flex items-start gap-3">
                <ShieldAlert
                  className="mt-0.5 h-5 w-5 shrink-0 text-amber-700"
                  aria-hidden="true"
                />

                <p className="text-sm leading-6 text-amber-900">
                  You are assigning a temporary password to{" "}
                  <span className="font-bold">{administrator.fullName}</span>.
                  The account will be required to change this password after the
                  next successful login.
                </p>
              </div>
            </div>

            {errors.general && (
              <div
                className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-800"
                role="alert"
                aria-live="assertive"
              >
                {errors.general}
              </div>
            )}

            <div className="mt-5 space-y-5">
              <PasswordField
                id="reset-temporary-password"
                name="temporaryPassword"
                label="Temporary password"
                value={values.temporaryPassword}
                visible={showTemporaryPassword}
                error={errors.temporaryPassword}
                disabled={isSubmitting}
                placeholder="Create a temporary password"
                onChange={handleFieldChange}
                onToggleVisibility={() =>
                  setShowTemporaryPassword((current) => !current)
                }
              />

              <PasswordField
                id="reset-confirm-temporary-password"
                name="confirmTemporaryPassword"
                label="Confirm temporary password"
                value={values.confirmTemporaryPassword}
                visible={showConfirmTemporaryPassword}
                error={errors.confirmTemporaryPassword}
                disabled={isSubmitting}
                placeholder="Re-enter the temporary password"
                onChange={handleFieldChange}
                onToggleVisibility={() =>
                  setShowConfirmTemporaryPassword((current) => !current)
                }
              />

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-bold text-slate-800">
                  Password requirements
                </p>

                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  <RequirementItem
                    satisfied={passwordRequirements.minimumLength}
                    label="At least 8 characters"
                  />

                  <RequirementItem
                    satisfied={passwordRequirements.uppercase}
                    label="One uppercase letter"
                  />

                  <RequirementItem
                    satisfied={passwordRequirements.lowercase}
                    label="One lowercase letter"
                  />

                  <RequirementItem
                    satisfied={passwordRequirements.number}
                    label="One number"
                  />

                  <RequirementItem
                    satisfied={passwordRequirements.specialCharacter}
                    label="One special character"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50 px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
            <button
              type="button"
              onClick={handleClose}
              disabled={isSubmitting}
              className="focus-ring inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-amber-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-amber-800 disabled:cursor-not-allowed disabled:opacity-65"
            >
              {isSubmitting ? (
                <>
                  <LoaderCircle
                    className="h-4 w-4 animate-spin"
                    aria-hidden="true"
                  />
                  Resetting password...
                </>
              ) : (
                <>
                  <KeyRound className="h-4 w-4" aria-hidden="true" />
                  Reset password
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface PasswordFieldProps {
  id: string;
  name: keyof AdminPasswordResetFormValues;
  label: string;
  value: string;
  visible: boolean;
  error?: string;
  disabled: boolean;
  placeholder: string;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onToggleVisibility: () => void;
}

function PasswordField({
  id,
  name,
  label,
  value,
  visible,
  error,
  disabled,
  placeholder,
  onChange,
  onToggleVisibility,
}: PasswordFieldProps) {
  const errorId = `${id}-error`;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-bold text-slate-800">
        {label}
        <span className="ml-1 text-red-700" aria-hidden="true">
          *
        </span>
      </label>

      <div className="relative mt-2">
        <KeyRound
          className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
          aria-hidden="true"
        />

        <input
          id={id}
          name={name}
          type={visible ? "text" : "password"}
          autoComplete="new-password"
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
          className="focus-ring absolute right-2 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
          aria-label={visible ? `Hide ${label}` : `Show ${label}`}
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

interface RequirementItemProps {
  satisfied: boolean;
  label: string;
}

function RequirementItem({ satisfied, label }: RequirementItemProps) {
  return (
    <div
      className={[
        "flex items-center gap-2 text-sm",
        satisfied ? "text-emerald-700" : "text-slate-500",
      ].join(" ")}
    >
      <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
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

function validatePasswordReset(
  values: AdminPasswordResetFormValues,
): AdminPasswordResetFormErrors {
  const errors: AdminPasswordResetFormErrors = {};

  if (!values.temporaryPassword) {
    errors.temporaryPassword = "Enter a temporary password.";
  } else {
    const requirements = evaluatePasswordRequirements(values.temporaryPassword);

    if (!Object.values(requirements).every(Boolean)) {
      errors.temporaryPassword =
        "The temporary password does not meet all password requirements.";
    }
  }

  if (!values.confirmTemporaryPassword) {
    errors.confirmTemporaryPassword = "Confirm the temporary password.";
  } else if (values.confirmTemporaryPassword !== values.temporaryPassword) {
    errors.confirmTemporaryPassword =
      "The temporary password confirmation does not match.";
  }

  return errors;
}

function resolvePasswordResetErrors(
  error: unknown,
): AdminPasswordResetFormErrors {
  if (!(error instanceof ApiError)) {
    return {
      general: "Unable to reset the administrator password. Please try again.",
    };
  }

  const errors: AdminPasswordResetFormErrors = {
    general: error.message,
  };

  const temporaryPasswordError = findFieldError(error, "temporaryPassword");

  const confirmationError = findFieldError(error, "confirmTemporaryPassword");

  if (temporaryPasswordError) {
    errors.temporaryPassword = temporaryPasswordError;
  }

  if (confirmationError) {
    errors.confirmTemporaryPassword = confirmationError;
  }

  if (temporaryPasswordError || confirmationError) {
    errors.general = undefined;
  }

  return errors;
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
