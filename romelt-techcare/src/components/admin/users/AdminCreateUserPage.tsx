/**
 * ================================================================
 * ROMELT TECHCARE — CREATE ADMINISTRATOR USER PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the protected workflow for creating a new Romelt
 * TechCare administrator account.
 *
 * Responsibilities:
 * - Collects administrator identity, role, and temporary password.
 * - Performs client-side form validation.
 * - Creates the administrator through the protected backend API.
 * - Displays backend field and general errors.
 * - Redirects to the administrator details page after success.
 * - Prevents duplicate submissions.
 *
 * Real-data integration:
 * Uses:
 *
 * POST /api/v1/admin/users
 *
 * Authorization:
 * The backend remains the final authority for SUPER_ADMIN access.
 * ================================================================
 */

import { ArrowLeft, ShieldPlus } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { AdminUserForm } from "@/components/admin/users/AdminUserForm";
import { ApiError } from "@/lib/api-error";
import { createAdminUser } from "@/services/admin-user.service";
import {
  INITIAL_ADMIN_USER_FORM_VALUES,
  type AdminCreateUserRequest,
  type AdminUserFormErrors,
  type AdminUserFormValues,
} from "@/types/admin-user.types";

export default function AdminCreateUserPage() {
  const navigate = useNavigate();

  const [values, setValues] = useState<AdminUserFormValues>({
    ...INITIAL_ADMIN_USER_FORM_VALUES,
  });

  const [errors, setErrors] = useState<AdminUserFormErrors>({});

  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleValuesChange(nextValues: AdminUserFormValues) {
    setValues(nextValues);

    setErrors((current) => ({
      ...current,
      email: nextValues.email !== values.email ? undefined : current.email,
      firstName:
        nextValues.firstName !== values.firstName
          ? undefined
          : current.firstName,
      lastName:
        nextValues.lastName !== values.lastName ? undefined : current.lastName,
      jobTitle:
        nextValues.jobTitle !== values.jobTitle ? undefined : current.jobTitle,
      role: nextValues.role !== values.role ? undefined : current.role,
      temporaryPassword:
        nextValues.temporaryPassword !== values.temporaryPassword
          ? undefined
          : current.temporaryPassword,
      confirmTemporaryPassword:
        nextValues.confirmTemporaryPassword !== values.confirmTemporaryPassword
          ? undefined
          : current.confirmTemporaryPassword,
      general: undefined,
    }));
  }

  async function handleSubmit(formValues: AdminUserFormValues) {
    const validationErrors = validateCreateForm(formValues);

    if (hasFormErrors(validationErrors)) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    setIsSubmitting(true);

    try {
      const request: AdminCreateUserRequest = {
        email: formValues.email.trim().toLowerCase(),
        firstName: formValues.firstName.trim(),
        lastName: formValues.lastName.trim(),
        jobTitle: formValues.jobTitle.trim() || null,
        role: formValues.role,
        temporaryPassword: formValues.temporaryPassword,
        confirmTemporaryPassword: formValues.confirmTemporaryPassword,
      };

      const administrator = await createAdminUser(request);

      setValues({
        ...INITIAL_ADMIN_USER_FORM_VALUES,
      });

      navigate(`/admin/users/${administrator.adminUserId}`, {
        replace: true,
        state: {
          successMessage: `${administrator.fullName} was created successfully.`,
        },
      });
    } catch (error) {
      setErrors(resolveCreateErrors(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl">
      <header className="mb-6">
        <Link
          to="/admin/users"
          className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Administrator users
        </Link>

        <div className="mt-5 flex items-start gap-4">
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-navy-950 text-white">
            <ShieldPlus className="h-7 w-7" aria-hidden="true" />
          </span>

          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
              Access management
            </p>

            <h1 className="mt-2 font-display text-3xl font-extrabold text-navy-950">
              Create administrator
            </h1>

            <p className="mt-3 max-w-3xl leading-7 text-slate-600">
              Create a protected account for an authorized Romelt TechCare
              administrator or staff member.
            </p>
          </div>
        </div>
      </header>

      <AdminUserForm
        mode="create"
        values={values}
        errors={errors}
        isSubmitting={isSubmitting}
        onChange={handleValuesChange}
        onSubmit={handleSubmit}
        onCancel={() => navigate("/admin/users")}
      />
    </div>
  );
}

function validateCreateForm(values: AdminUserFormValues): AdminUserFormErrors {
  const errors: AdminUserFormErrors = {};

  const email = values.email.trim();
  const firstName = values.firstName.trim();
  const lastName = values.lastName.trim();
  const jobTitle = values.jobTitle.trim();

  if (!firstName) {
    errors.firstName = "Enter the administrator's first name.";
  } else if (firstName.length > 100) {
    errors.firstName = "The first name cannot exceed 100 characters.";
  }

  if (!lastName) {
    errors.lastName = "Enter the administrator's last name.";
  } else if (lastName.length > 100) {
    errors.lastName = "The last name cannot exceed 100 characters.";
  }

  if (!email) {
    errors.email = "Enter the administrator's email address.";
  } else if (!isValidEmail(email)) {
    errors.email = "Enter a valid email address.";
  } else if (email.length > 255) {
    errors.email = "The email address cannot exceed 255 characters.";
  }

  if (jobTitle.length > 150) {
    errors.jobTitle = "The job title cannot exceed 150 characters.";
  }

  if (!values.role) {
    errors.role = "Select an administrator role.";
  }

  if (!values.temporaryPassword) {
    errors.temporaryPassword = "Create a temporary administrator password.";
  } else if (!meetsPasswordRequirements(values.temporaryPassword)) {
    errors.temporaryPassword =
      "The temporary password does not meet all password requirements.";
  }

  if (!values.confirmTemporaryPassword) {
    errors.confirmTemporaryPassword = "Confirm the temporary password.";
  } else if (values.confirmTemporaryPassword !== values.temporaryPassword) {
    errors.confirmTemporaryPassword =
      "The temporary password confirmation does not match.";
  }

  return errors;
}

function resolveCreateErrors(error: unknown): AdminUserFormErrors {
  if (!(error instanceof ApiError)) {
    return {
      general:
        "Unable to create the administrator. Check the backend connection and try again.",
    };
  }

  const resolvedErrors: AdminUserFormErrors = {
    general: error.message,
  };

  const supportedFields: Array<keyof AdminUserFormErrors> = [
    "email",
    "firstName",
    "lastName",
    "jobTitle",
    "role",
    "temporaryPassword",
    "confirmTemporaryPassword",
  ];

  for (const field of supportedFields) {
    const fieldError = findApiFieldError(error, field);

    if (fieldError) {
      resolvedErrors[field] = fieldError;
    }
  }

  if (supportedFields.some((field) => Boolean(resolvedErrors[field]))) {
    resolvedErrors.general = undefined;
  }

  return resolvedErrors;
}

function findApiFieldError(
  error: ApiError,
  fieldName: string,
): string | undefined {
  return (
    error.validationErrors[fieldName] ??
    error.fieldErrors.find((fieldError) => fieldError.field === fieldName)
      ?.message
  );
}

function hasFormErrors(errors: AdminUserFormErrors): boolean {
  return Object.values(errors).some(Boolean);
}

function meetsPasswordRequirements(password: string): boolean {
  return (
    password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password)
  );
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
