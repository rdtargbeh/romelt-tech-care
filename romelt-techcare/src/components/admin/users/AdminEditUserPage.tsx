/**
 * ================================================================
 * ROMELT TECHCARE — EDIT ADMINISTRATOR USER PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the protected workflow for updating an existing Romelt
 * TechCare administrator account.
 *
 * Responsibilities:
 * - Loads the requested administrator account.
 * - Populates the shared administrator form.
 * - Updates identity, email, job title, and role.
 * - Performs client-side validation.
 * - Displays backend field and general errors.
 * - Handles missing or invalid administrator IDs.
 * - Redirects to the administrator details page after success.
 *
 * Real-data integration:
 * Uses:
 *
 * GET /api/v1/admin/users/{adminUserId}
 * PUT /api/v1/admin/users/{adminUserId}
 *
 * Authorization:
 * The backend remains the final authority for SUPER_ADMIN access.
 * ================================================================
 */

import {
  ArrowLeft,
  CircleAlert,
  LoaderCircle,
  RefreshCw,
  UserCog,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { AdminUserForm } from "@/components/admin/users/AdminUserForm";
import { ApiError } from "@/lib/api-error";
import {
  getAdminUserById,
  updateAdminUser,
} from "@/services/admin-user.service";
import {
  INITIAL_ADMIN_USER_FORM_VALUES,
  type AdminUpdateUserRequest,
  type AdminUser,
  type AdminUserFormErrors,
  type AdminUserFormValues,
} from "@/types/admin-user.types";

export default function AdminEditUserPage() {
  const navigate = useNavigate();
  const { adminUserId } = useParams<{
    adminUserId: string;
  }>();

  const [administrator, setAdministrator] = useState<AdminUser | null>(null);

  const [values, setValues] = useState<AdminUserFormValues>({
    ...INITIAL_ADMIN_USER_FORM_VALUES,
  });

  const [errors, setErrors] = useState<AdminUserFormErrors>({});

  const [loadError, setLoadError] = useState<string | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!adminUserId) {
      setLoadError("Administrator user ID is missing.");
      setIsLoading(false);
      return;
    }

    const controller = new AbortController();

    void loadAdministrator(adminUserId, controller.signal);

    return () => {
      controller.abort();
    };
  }, [adminUserId]);

  async function loadAdministrator(
    requestedAdminUserId: string,
    signal?: AbortSignal,
  ) {
    setIsLoading(true);
    setLoadError(null);

    try {
      const response = await getAdminUserById(requestedAdminUserId, signal);

      setAdministrator(response);

      setValues({
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        jobTitle: response.jobTitle ?? "",
        role: response.role,
        temporaryPassword: "",
        confirmTemporaryPassword: "",
      });
    } catch (error) {
      if (signal?.aborted) {
        return;
      }

      setLoadError(resolveLoadError(error));
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false);
      }
    }
  }

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
      general: undefined,
    }));
  }

  async function handleSubmit(formValues: AdminUserFormValues) {
    if (!adminUserId) {
      setErrors({
        general: "Administrator user ID is missing.",
      });
      return;
    }

    const validationErrors = validateEditForm(formValues);

    if (hasFormErrors(validationErrors)) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    setIsSubmitting(true);

    try {
      const request: AdminUpdateUserRequest = {
        email: formValues.email.trim().toLowerCase(),
        firstName: formValues.firstName.trim(),
        lastName: formValues.lastName.trim(),
        jobTitle: formValues.jobTitle.trim() || null,
        role: formValues.role,
      };

      const updatedAdministrator = await updateAdminUser(adminUserId, request);

      navigate(`/admin/users/${updatedAdministrator.adminUserId}`, {
        replace: true,
        state: {
          successMessage: `${updatedAdministrator.fullName} was updated successfully.`,
        },
      });
    } catch (error) {
      setErrors(resolveUpdateErrors(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return <AdminUserEditLoadingState />;
  }

  if (loadError || !administrator) {
    return (
      <AdminUserEditErrorState
        message={
          loadError ?? "The requested administrator account is unavailable."
        }
        onRetry={() => {
          if (adminUserId) {
            void loadAdministrator(adminUserId);
          }
        }}
      />
    );
  }

  return (
    <div className="mx-auto max-w-5xl">
      <header className="mb-6">
        <Link
          to={`/admin/users/${administrator.adminUserId}`}
          className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          {administrator.fullName}
        </Link>

        <div className="mt-5 flex items-start gap-4">
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-navy-950 text-white">
            <UserCog className="h-7 w-7" aria-hidden="true" />
          </span>

          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-brand-700">
              Access management
            </p>

            <h1 className="mt-2 font-display text-3xl font-extrabold text-navy-950">
              Edit administrator
            </h1>

            <p className="mt-3 max-w-3xl leading-7 text-slate-600">
              Update the profile and access role for{" "}
              <span className="font-bold text-slate-900">
                {administrator.fullName}
              </span>
              .
            </p>
          </div>
        </div>
      </header>

      <AdminUserForm
        mode="edit"
        values={values}
        errors={errors}
        isSubmitting={isSubmitting}
        onChange={handleValuesChange}
        onSubmit={handleSubmit}
        onCancel={() => navigate(`/admin/users/${administrator.adminUserId}`)}
      />
    </div>
  );
}

function AdminUserEditLoadingState() {
  return (
    <div
      className="mx-auto flex min-h-[60vh] max-w-5xl items-center justify-center"
      role="status"
      aria-live="polite"
    >
      <div className="text-center">
        <LoaderCircle
          className="mx-auto h-9 w-9 animate-spin text-brand-700"
          aria-hidden="true"
        />

        <h1 className="mt-5 font-display text-xl font-extrabold text-navy-950">
          Loading administrator
        </h1>

        <p className="mt-2 text-sm text-slate-600">
          Retrieving protected administrator information.
        </p>
      </div>
    </div>
  );
}

interface AdminUserEditErrorStateProps {
  message: string;
  onRetry: () => void;
}

function AdminUserEditErrorState({
  message,
  onRetry,
}: AdminUserEditErrorStateProps) {
  return (
    <div className="mx-auto max-w-3xl rounded-3xl border border-red-200 bg-red-50 px-5 py-10 text-center sm:px-8">
      <CircleAlert
        className="mx-auto h-10 w-10 text-red-700"
        aria-hidden="true"
      />

      <h1 className="mt-5 font-display text-2xl font-extrabold text-red-950">
        Unable to load administrator
      </h1>

      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-red-800">
        {message}
      </p>

      <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
        <button
          type="button"
          onClick={onRetry}
          className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-red-800 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-900"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Try again
        </button>

        <Link
          to="/admin/users"
          className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-red-300 bg-white px-4 py-2.5 text-sm font-bold text-red-800 transition hover:bg-red-100"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Administrator users
        </Link>
      </div>
    </div>
  );
}

function validateEditForm(values: AdminUserFormValues): AdminUserFormErrors {
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

  return errors;
}

function resolveLoadError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return "The administrator account could not be retrieved. Check the backend connection and try again.";
}

function resolveUpdateErrors(error: unknown): AdminUserFormErrors {
  if (!(error instanceof ApiError)) {
    return {
      general:
        "Unable to update the administrator. Check the backend connection and try again.",
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

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
