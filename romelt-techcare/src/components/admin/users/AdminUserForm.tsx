/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER FORM
 * ================================================================
 *
 * Purpose:
 * Provides the reusable form used to create and edit Romelt TechCare
 * administrator accounts.
 *
 * Responsibilities:
 * - Collects administrator identity and employment information.
 * - Supports administrator role selection.
 * - Collects temporary passwords during account creation.
 * - Validates required fields before submission.
 * - Displays frontend and backend validation errors.
 * - Prevents duplicate submissions.
 * - Supports cancel navigation.
 *
 * Real-data integration:
 * This form is used by:
 *
 * POST /api/v1/admin/users
 * PUT  /api/v1/admin/users/{adminUserId}
 *
 * Security:
 * - Temporary passwords are rendered only during account creation.
 * - Password values are never persisted.
 * - Password visibility is intentionally controlled by the user.
 * - Backend errors are displayed without exposing sensitive data.
 * ================================================================
 */

import {
  CheckCircle2,
  Eye,
  EyeOff,
  LoaderCircle,
  Mail,
  Save,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { type ChangeEvent, type FormEvent, useMemo, useState } from "react";

import {
  ADMIN_ROLE_OPTIONS,
  type AdminUserFormErrors,
  type AdminUserFormValues,
} from "@/types/admin-user.types";

export type AdminUserFormMode = "create" | "edit";

interface AdminUserFormProps {
  mode: AdminUserFormMode;
  values: AdminUserFormValues;
  errors: AdminUserFormErrors;
  isSubmitting: boolean;
  onChange: (values: AdminUserFormValues) => void;
  onSubmit: (values: AdminUserFormValues) => void | Promise<void>;
  onCancel: () => void;
}

export function AdminUserForm({
  mode,
  values,
  errors,
  isSubmitting,
  onChange,
  onSubmit,
  onCancel,
}: AdminUserFormProps) {
  const [showTemporaryPassword, setShowTemporaryPassword] = useState(false);

  const [showConfirmTemporaryPassword, setShowConfirmTemporaryPassword] =
    useState(false);

  const passwordRequirements = useMemo(
    () => evaluatePasswordRequirements(values.temporaryPassword),
    [values.temporaryPassword],
  );

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = event.target;

    onChange({
      ...values,
      [name]: value,
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    await onSubmit(values);
  }

  const isCreateMode = mode === "create";

  return (
    <form onSubmit={handleSubmit} noValidate>
      {errors.general && (
        <div
          className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm leading-6 text-red-800"
          role="alert"
          aria-live="assertive"
        >
          {errors.general}
        </div>
      )}

      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
        <div className="flex items-start gap-4">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
            <UserRound className="h-6 w-6" aria-hidden="true" />
          </span>

          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
              Administrator profile
            </p>

            <h2 className="mt-2 font-display text-2xl font-extrabold text-navy-950">
              Personal and account information
            </h2>

            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
              Enter the administrator&apos;s legal name, work email, job title,
              and assigned access role.
            </p>
          </div>
        </div>

        <div className="mt-7 grid gap-5 sm:grid-cols-2">
          <FormField
            id="admin-first-name"
            name="firstName"
            label="First name"
            value={values.firstName}
            error={errors.firstName}
            placeholder="Enter first name"
            autoComplete="given-name"
            disabled={isSubmitting}
            required
            onChange={handleFieldChange}
          />

          <FormField
            id="admin-last-name"
            name="lastName"
            label="Last name"
            value={values.lastName}
            error={errors.lastName}
            placeholder="Enter last name"
            autoComplete="family-name"
            disabled={isSubmitting}
            required
            onChange={handleFieldChange}
          />

          <FormField
            id="admin-user-email"
            name="email"
            label="Email address"
            value={values.email}
            error={errors.email}
            placeholder="administrator@romelttechcare.com"
            autoComplete="email"
            inputMode="email"
            type="email"
            icon={<Mail className="h-5 w-5" aria-hidden="true" />}
            disabled={isSubmitting}
            required
            onChange={handleFieldChange}
          />

          <FormField
            id="admin-job-title"
            name="jobTitle"
            label="Job title"
            value={values.jobTitle}
            error={errors.jobTitle}
            placeholder="Example: Operations Administrator"
            autoComplete="organization-title"
            disabled={isSubmitting}
            onChange={handleFieldChange}
          />
        </div>

        <div className="mt-5">
          <label
            htmlFor="admin-user-role"
            className="block text-sm font-bold text-slate-800"
          >
            Administrator role
            <span className="ml-1 text-red-700" aria-hidden="true">
              *
            </span>
          </label>

          <select
            id="admin-user-role"
            name="role"
            value={values.role}
            onChange={handleFieldChange}
            disabled={isSubmitting}
            aria-invalid={errors.role ? "true" : "false"}
            aria-describedby={
              errors.role ? "admin-user-role-error" : "admin-role-help"
            }
            className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100"
          >
            {ADMIN_ROLE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          {errors.role ? (
            <p id="admin-user-role-error" className="mt-2 text-sm text-red-700">
              {errors.role}
            </p>
          ) : (
            <p
              id="admin-role-help"
              className="mt-2 text-sm leading-6 text-slate-500"
            >
              {
                ADMIN_ROLE_OPTIONS.find(
                  (option) => option.value === values.role,
                )?.description
              }
            </p>
          )}
        </div>
      </section>

      {isCreateMode && (
        <section className="mt-6 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
          <div className="flex items-start gap-4">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
              <ShieldCheck className="h-6 w-6" aria-hidden="true" />
            </span>

            <div>
              <p className="text-sm font-bold uppercase tracking-[0.18em] text-amber-700">
                Initial account security
              </p>

              <h2 className="mt-2 font-display text-2xl font-extrabold text-navy-950">
                Temporary password
              </h2>

              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
                Create a secure temporary password. The administrator will be
                required to replace it after signing in.
              </p>
            </div>
          </div>

          <div className="mt-7 grid gap-5 lg:grid-cols-2">
            <PasswordField
              id="admin-temporary-password"
              name="temporaryPassword"
              label="Temporary password"
              value={values.temporaryPassword}
              error={errors.temporaryPassword}
              visible={showTemporaryPassword}
              disabled={isSubmitting}
              placeholder="Create a temporary password"
              onChange={handleFieldChange}
              onToggleVisibility={() =>
                setShowTemporaryPassword((current) => !current)
              }
            />

            <PasswordField
              id="admin-confirm-temporary-password"
              name="confirmTemporaryPassword"
              label="Confirm temporary password"
              value={values.confirmTemporaryPassword}
              error={errors.confirmTemporaryPassword}
              visible={showConfirmTemporaryPassword}
              disabled={isSubmitting}
              placeholder="Re-enter the temporary password"
              onChange={handleFieldChange}
              onToggleVisibility={() =>
                setShowConfirmTemporaryPassword((current) => !current)
              }
            />
          </div>

          <PasswordRequirements requirements={passwordRequirements} />
        </section>
      )}

      <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
          className="focus-ring inline-flex min-h-12 items-center justify-center rounded-xl border border-slate-300 bg-white px-5 py-3 font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Cancel
        </button>

        <button
          type="submit"
          disabled={isSubmitting}
          className="focus-ring inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-navy-950 px-5 py-3 font-bold text-white transition hover:bg-navy-900 disabled:cursor-not-allowed disabled:opacity-65"
        >
          {isSubmitting ? (
            <>
              <LoaderCircle
                className="h-5 w-5 animate-spin"
                aria-hidden="true"
              />

              {isCreateMode ? "Creating administrator..." : "Saving changes..."}
            </>
          ) : (
            <>
              <Save className="h-5 w-5" aria-hidden="true" />

              {isCreateMode ? "Create administrator" : "Save administrator"}
            </>
          )}
        </button>
      </div>
    </form>
  );
}

interface FormFieldProps {
  id: string;
  name: keyof AdminUserFormValues;
  label: string;
  value: string;
  error?: string;
  placeholder: string;
  type?: string;
  inputMode?:
    | "none"
    | "text"
    | "tel"
    | "url"
    | "email"
    | "numeric"
    | "decimal"
    | "search";
  autoComplete?: string;
  icon?: React.ReactNode;
  disabled: boolean;
  required?: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

function FormField({
  id,
  name,
  label,
  value,
  error,
  placeholder,
  type = "text",
  inputMode,
  autoComplete,
  icon,
  disabled,
  required = false,
  onChange,
}: FormFieldProps) {
  const errorId = `${id}-error`;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-bold text-slate-800">
        {label}

        {required && (
          <span className="ml-1 text-red-700" aria-hidden="true">
            *
          </span>
        )}
      </label>

      <div className="relative mt-2">
        {icon && (
          <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
            {icon}
          </span>
        )}

        <input
          id={id}
          name={name}
          type={type}
          inputMode={inputMode}
          autoComplete={autoComplete}
          value={value}
          onChange={onChange}
          disabled={disabled}
          aria-invalid={error ? "true" : "false"}
          aria-describedby={error ? errorId : undefined}
          placeholder={placeholder}
          className={[
            "focus-ring min-h-12 w-full rounded-xl border border-slate-300 bg-white py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 disabled:cursor-not-allowed disabled:bg-slate-100",
            icon ? "pl-12 pr-4" : "px-4",
          ].join(" ")}
        />
      </div>

      {error && (
        <p id={errorId} className="mt-2 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}

interface PasswordFieldProps {
  id: string;
  name: "temporaryPassword" | "confirmTemporaryPassword";
  label: string;
  value: string;
  error?: string;
  visible: boolean;
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
  error,
  visible,
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
        <ShieldCheck
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
    <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-sm font-bold text-slate-800">
        Temporary password requirements
      </p>

      <div className="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
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
