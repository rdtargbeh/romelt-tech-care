/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER DETAILS PAGE
 * ================================================================
 *
 * Purpose:
 * Provides the complete profile, security, and account-management
 * workspace for one administrator.
 *
 * Responsibilities:
 * - Loads one administrator by ID.
 * - Displays profile, security, and activity sections.
 * - Updates account status.
 * - Supports password reset.
 * - Supports administrator deletion.
 * - Provides navigation to the edit workflow.
 * - Displays success, loading, and error states.
 *
 * Real-data integration:
 * Uses:
 *
 * GET    /api/v1/admin/users/{adminUserId}
 * PATCH  /api/v1/admin/users/{adminUserId}/status
 * POST   /api/v1/admin/users/{adminUserId}/reset-password
 * DELETE /api/v1/admin/users/{adminUserId}
 *
 * Authorization:
 * The Spring Boot backend remains the final authority for all
 * administrator-management actions.
 * ================================================================
 */

import {
  Activity,
  ArrowLeft,
  Ban,
  CalendarClock,
  CheckCircle2,
  CircleAlert,
  Edit3,
  KeyRound,
  LoaderCircle,
  LockKeyhole,
  Mail,
  RefreshCw,
  ShieldCheck,
  Trash2,
  UserRound,
} from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";

import { AdminDeleteUserDialog } from "@/components/admin/users/AdminDeleteUserDialog";
import { AdminResetPasswordDialog } from "@/components/admin/users/AdminResetPasswordDialog";
import { AdminStatusBadge } from "@/components/admin/users/AdminStatusBadge";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { ApiError } from "@/lib/api-error";
import {
  changeAdminUserStatus,
  getAdminUserById,
} from "@/services/admin-user.service";
import type { AdminStatus } from "@/types/admin-auth.types";
import type {
  AdminUser,
  AdminUsersLocationState,
} from "@/types/admin-user.types";

type DetailsTab = "profile" | "security" | "activity";

export default function AdminUserDetailsPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const { adminUserId } = useParams<{
    adminUserId: string;
  }>();

  const { administrator: currentAdministrator } = useAdminAuth();

  const [administrator, setAdministrator] = useState<AdminUser | null>(null);

  const [activeTab, setActiveTab] = useState<DetailsTab>("profile");

  const [isLoading, setIsLoading] = useState(true);

  const [isChangingStatus, setIsChangingStatus] = useState(false);

  const [loadError, setLoadError] = useState<string | null>(null);

  const [actionError, setActionError] = useState<string | null>(null);

  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const locationState = location.state as AdminUsersLocationState | null;

  const isCurrentAccount =
    administrator?.adminUserId === currentAdministrator?.adminUserId;

  useEffect(() => {
    if (locationState?.successMessage) {
      setSuccessMessage(locationState.successMessage);

      navigate(location.pathname, {
        replace: true,
        state: null,
      });
    }
  }, [location.pathname, locationState, navigate]);

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
    } catch (error) {
      if (signal?.aborted) {
        return;
      }

      setLoadError(resolveErrorMessage(error));
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false);
      }
    }
  }

  async function handleStatusChange(status: AdminStatus) {
    if (!administrator || isChangingStatus) {
      return;
    }

    const confirmed = window.confirm(
      createStatusConfirmationMessage(administrator, status),
    );

    if (!confirmed) {
      return;
    }

    setActionError(null);
    setSuccessMessage(null);
    setIsChangingStatus(true);

    try {
      const updatedAdministrator = await changeAdminUserStatus(
        administrator.adminUserId,
        {
          status,
        },
      );

      setAdministrator(updatedAdministrator);

      setSuccessMessage(
        `${updatedAdministrator.fullName}'s account is now ${formatEnumValue(
          status,
        ).toLowerCase()}.`,
      );
    } catch (error) {
      setActionError(resolveErrorMessage(error));
    } finally {
      setIsChangingStatus(false);
    }
  }

  function handlePasswordReset(updatedAdministrator: AdminUser) {
    setAdministrator(updatedAdministrator);

    setSuccessMessage(
      `A temporary password was assigned to ${updatedAdministrator.fullName}. The administrator must change it after signing in.`,
    );

    setActionError(null);
  }

  function handleDeleted(deletedAdministrator: AdminUser) {
    navigate("/admin/users", {
      replace: true,
      state: {
        successMessage: `${deletedAdministrator.fullName} was deleted successfully.`,
      },
    });
  }

  if (isLoading) {
    return <AdminUserDetailsLoadingState />;
  }

  if (loadError || !administrator) {
    return (
      <AdminUserDetailsErrorState
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
    <div className="mx-auto max-w-7xl">
      {successMessage && (
        <div
          className="mb-6 flex items-start justify-between gap-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-emerald-900"
          role="status"
          aria-live="polite"
        >
          <div className="flex items-start gap-3">
            <CheckCircle2
              className="mt-0.5 h-5 w-5 shrink-0"
              aria-hidden="true"
            />

            <p className="text-sm font-semibold leading-6">{successMessage}</p>
          </div>

          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="focus-ring rounded-lg px-2 py-1 text-sm font-bold text-emerald-800 transition hover:bg-emerald-100"
          >
            Dismiss
          </button>
        </div>
      )}

      {actionError && (
        <div
          className="mb-6 flex items-start justify-between gap-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-red-900"
          role="alert"
          aria-live="assertive"
        >
          <div className="flex items-start gap-3">
            <CircleAlert
              className="mt-0.5 h-5 w-5 shrink-0"
              aria-hidden="true"
            />

            <p className="text-sm font-semibold leading-6">{actionError}</p>
          </div>

          <button
            type="button"
            onClick={() => setActionError(null)}
            className="focus-ring rounded-lg px-2 py-1 text-sm font-bold text-red-800 transition hover:bg-red-100"
          >
            Dismiss
          </button>
        </div>
      )}

      <Link
        to="/admin/users"
        className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Administrator users
      </Link>

      <section className="mt-5 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="bg-navy-950 px-5 py-7 text-white sm:px-7">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex min-w-0 items-start gap-4">
              <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white text-xl font-extrabold text-navy-950">
                {createInitials(administrator.fullName)}
              </span>

              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="break-words font-display text-3xl font-extrabold">
                    {administrator.fullName}
                  </h1>

                  <AdminStatusBadge status={administrator.status} />
                </div>

                <p className="mt-2 break-all text-sm text-slate-300">
                  {administrator.email}
                </p>

                <p className="mt-2 text-sm font-semibold text-brand-300">
                  {formatEnumValue(administrator.role)}
                  {administrator.jobTitle ? ` • ${administrator.jobTitle}` : ""}
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link
                to={`/admin/users/${administrator.adminUserId}/edit`}
                className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-bold text-navy-950 transition hover:bg-brand-50"
              >
                <Edit3 className="h-4 w-4" aria-hidden="true" />
                Edit administrator
              </Link>

              <button
                type="button"
                onClick={() => setResetPasswordDialogOpen(true)}
                className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-white/20"
              >
                <KeyRound className="h-4 w-4" aria-hidden="true" />
                Reset password
              </button>
            </div>
          </div>
        </div>

        <div className="grid gap-4 border-b border-slate-200 p-5 sm:grid-cols-2 lg:grid-cols-4 sm:p-7">
          <SummaryItem
            icon={<ShieldCheck className="h-5 w-5" aria-hidden="true" />}
            label="Role"
            value={formatEnumValue(administrator.role)}
          />

          <SummaryItem
            icon={<CalendarClock className="h-5 w-5" aria-hidden="true" />}
            label="Last login"
            value={formatDateTime(administrator.lastLoginAt)}
          />

          <SummaryItem
            icon={<KeyRound className="h-5 w-5" aria-hidden="true" />}
            label="Password"
            value={
              administrator.mustChangePassword ? "Change required" : "Current"
            }
          />

          <SummaryItem
            icon={<UserRound className="h-5 w-5" aria-hidden="true" />}
            label="Account"
            value={isCurrentAccount ? "Your account" : "Administrator"}
          />
        </div>

        <div className="overflow-x-auto border-b border-slate-200 px-5 sm:px-7">
          <div
            className="flex min-w-max gap-1"
            role="tablist"
            aria-label="Administrator details"
          >
            <DetailsTabButton
              active={activeTab === "profile"}
              icon={<UserRound className="h-4 w-4" aria-hidden="true" />}
              label="Profile"
              onClick={() => setActiveTab("profile")}
            />

            <DetailsTabButton
              active={activeTab === "security"}
              icon={<ShieldCheck className="h-4 w-4" aria-hidden="true" />}
              label="Security"
              onClick={() => setActiveTab("security")}
            />

            <DetailsTabButton
              active={activeTab === "activity"}
              icon={<Activity className="h-4 w-4" aria-hidden="true" />}
              label="Activity"
              onClick={() => setActiveTab("activity")}
            />
          </div>
        </div>

        <div className="p-5 sm:p-7">
          {activeTab === "profile" && (
            <ProfileTab administrator={administrator} />
          )}

          {activeTab === "security" && (
            <SecurityTab
              administrator={administrator}
              isCurrentAccount={isCurrentAccount}
              isChangingStatus={isChangingStatus}
              onStatusChange={handleStatusChange}
              onResetPassword={() => setResetPasswordDialogOpen(true)}
              onDelete={() => setDeleteDialogOpen(true)}
            />
          )}

          {activeTab === "activity" && (
            <ActivityTab administrator={administrator} />
          )}
        </div>
      </section>

      <AdminResetPasswordDialog
        administrator={administrator}
        open={resetPasswordDialogOpen}
        onClose={() => setResetPasswordDialogOpen(false)}
        onPasswordReset={handlePasswordReset}
      />

      <AdminDeleteUserDialog
        administrator={administrator}
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        onDeleted={handleDeleted}
      />
    </div>
  );
}

interface ProfileTabProps {
  administrator: AdminUser;
}

function ProfileTab({ administrator }: ProfileTabProps) {
  return (
    <section aria-labelledby="administrator-profile-heading">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.16em] text-brand-700">
          Administrator profile
        </p>

        <h2
          id="administrator-profile-heading"
          className="mt-2 font-display text-2xl font-extrabold text-navy-950"
        >
          Identity and employment information
        </h2>
      </div>

      <dl className="mt-6 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
        <DetailItem term="First name" description={administrator.firstName} />

        <DetailItem term="Last name" description={administrator.lastName} />

        <DetailItem term="Full name" description={administrator.fullName} />

        <DetailItem
          term="Email address"
          description={administrator.email}
          breakText
        />

        <DetailItem
          term="Job title"
          description={administrator.jobTitle || "Not specified"}
        />

        <DetailItem
          term="Administrator role"
          description={formatEnumValue(administrator.role)}
        />

        <DetailItem
          term="Account status"
          description={formatEnumValue(administrator.status)}
        />

        <DetailItem
          term="Account created"
          description={formatDateTime(administrator.createdAt)}
        />

        <DetailItem
          term="Last updated"
          description={formatDateTime(administrator.updatedAt)}
        />
      </dl>
    </section>
  );
}

interface SecurityTabProps {
  administrator: AdminUser;
  isCurrentAccount: boolean;
  isChangingStatus: boolean;
  onStatusChange: (status: AdminStatus) => void;
  onResetPassword: () => void;
  onDelete: () => void;
}

function SecurityTab({
  administrator,
  isCurrentAccount,
  isChangingStatus,
  onStatusChange,
  onResetPassword,
  onDelete,
}: SecurityTabProps) {
  return (
    <section aria-labelledby="administrator-security-heading">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.16em] text-brand-700">
          Account security
        </p>

        <h2
          id="administrator-security-heading"
          className="mt-2 font-display text-2xl font-extrabold text-navy-950"
        >
          Access and authentication controls
        </h2>
      </div>

      <dl className="mt-6 grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
        <DetailItem
          term="Current status"
          description={formatEnumValue(administrator.status)}
        />

        <DetailItem
          term="Last login"
          description={formatDateTime(administrator.lastLoginAt)}
        />

        <DetailItem
          term="Password changed"
          description={formatDateTime(administrator.passwordChangedAt)}
        />

        <DetailItem
          term="Password action"
          description={
            administrator.mustChangePassword
              ? "Change required"
              : "No action required"
          }
        />

        <DetailItem
          term="Failed login attempts"
          description={String(administrator.failedLoginAttempts ?? 0)}
        />

        <DetailItem
          term="Locked until"
          description={formatDateTime(administrator.lockedUntil)}
        />
      </dl>

      <div className="mt-8 border-t border-slate-200 pt-7">
        <h3 className="font-display text-xl font-extrabold text-navy-950">
          Security actions
        </h3>

        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
          Account restrictions take effect according to the backend
          authentication policy.
        </p>

        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <SecurityActionCard
            icon={<KeyRound className="h-5 w-5" aria-hidden="true" />}
            title="Reset password"
            description="Assign a temporary password and require the administrator to replace it after login."
          >
            <button
              type="button"
              onClick={onResetPassword}
              className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-navy-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-navy-900"
            >
              <KeyRound className="h-4 w-4" aria-hidden="true" />
              Reset password
            </button>
          </SecurityActionCard>

          <SecurityActionCard
            icon={<ShieldCheck className="h-5 w-5" aria-hidden="true" />}
            title="Account status"
            description="Activate, deactivate, lock, or unlock access to the administrator portal."
          >
            <div className="grid gap-2">
              {administrator.status !== "ACTIVE" && (
                <StatusActionButton
                  label="Activate account"
                  icon={<CheckCircle2 className="h-4 w-4" />}
                  disabled={isChangingStatus}
                  onClick={() => onStatusChange("ACTIVE")}
                />
              )}

              {administrator.status !== "INACTIVE" && (
                <StatusActionButton
                  label="Deactivate account"
                  icon={<Ban className="h-4 w-4" />}
                  disabled={isChangingStatus || isCurrentAccount}
                  onClick={() => onStatusChange("INACTIVE")}
                />
              )}

              {administrator.status !== "LOCKED" && (
                <StatusActionButton
                  label="Lock account"
                  icon={<LockKeyhole className="h-4 w-4" />}
                  disabled={isChangingStatus || isCurrentAccount}
                  onClick={() => onStatusChange("LOCKED")}
                />
              )}

              {isChangingStatus && (
                <p className="flex items-center justify-center gap-2 py-2 text-sm font-semibold text-slate-500">
                  <LoaderCircle
                    className="h-4 w-4 animate-spin"
                    aria-hidden="true"
                  />
                  Updating status...
                </p>
              )}

              {isCurrentAccount && (
                <p className="text-xs leading-5 text-slate-500">
                  You cannot deactivate or lock the account currently being
                  used.
                </p>
              )}
            </div>
          </SecurityActionCard>

          <SecurityActionCard
            danger
            icon={<Trash2 className="h-5 w-5" aria-hidden="true" />}
            title="Delete administrator"
            description="Permanently remove this administrator account from Romelt TechCare."
          >
            <button
              type="button"
              onClick={onDelete}
              disabled={isCurrentAccount}
              className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-red-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 className="h-4 w-4" aria-hidden="true" />
              Delete administrator
            </button>

            {isCurrentAccount && (
              <p className="mt-2 text-xs leading-5 text-red-700">
                You cannot delete the account currently being used.
              </p>
            )}
          </SecurityActionCard>
        </div>
      </div>
    </section>
  );
}

interface ActivityTabProps {
  administrator: AdminUser;
}

function ActivityTab({ administrator }: ActivityTabProps) {
  const activityItems = [
    {
      title: "Account created",
      description: `${administrator.fullName}'s administrator account was created.`,
      timestamp: administrator.createdAt,
    },
    {
      title: "Profile last updated",
      description:
        "The administrator profile or access information was last updated.",
      timestamp: administrator.updatedAt,
    },
    {
      title: "Password last changed",
      description: administrator.passwordChangedAt
        ? "The administrator password was changed."
        : "No completed password change has been recorded.",
      timestamp: administrator.passwordChangedAt,
    },
    {
      title: "Last successful login",
      description: administrator.lastLoginAt
        ? "The administrator successfully authenticated."
        : "The administrator has not completed a successful login.",
      timestamp: administrator.lastLoginAt,
    },
  ];

  return (
    <section aria-labelledby="administrator-activity-heading">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.16em] text-brand-700">
          Account activity
        </p>

        <h2
          id="administrator-activity-heading"
          className="mt-2 font-display text-2xl font-extrabold text-navy-950"
        >
          Current account timeline
        </h2>

        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
          This timeline uses timestamps available in the current administrator
          response. A complete audit history can be connected when the protected
          audit-log API is implemented.
        </p>
      </div>

      <div className="mt-7 space-y-4">
        {activityItems.map((item) => (
          <article
            key={item.title}
            className="flex items-start gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-brand-700 shadow-sm">
              <Activity className="h-5 w-5" aria-hidden="true" />
            </span>

            <div>
              <h3 className="font-bold text-slate-900">{item.title}</h3>

              <p className="mt-1 text-sm leading-6 text-slate-600">
                {item.description}
              </p>

              <p className="mt-2 text-xs font-semibold text-slate-500">
                {formatDateTime(item.timestamp)}
              </p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

interface SummaryItemProps {
  icon: ReactNode;
  label: string;
  value: string;
}

function SummaryItem({ icon, label, value }: SummaryItemProps) {
  return (
    <div className="flex items-start gap-3">
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
        {icon}
      </span>

      <div>
        <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
          {label}
        </p>

        <p className="mt-1 text-sm font-bold text-slate-900">{value}</p>
      </div>
    </div>
  );
}

interface DetailsTabButtonProps {
  active: boolean;
  icon: ReactNode;
  label: string;
  onClick: () => void;
}

function DetailsTabButton({
  active,
  icon,
  label,
  onClick,
}: DetailsTabButtonProps) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={[
        "focus-ring inline-flex min-h-12 items-center gap-2 border-b-2 px-4 py-3 text-sm font-bold transition",
        active
          ? "border-brand-700 text-brand-800"
          : "border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-900",
      ].join(" ")}
    >
      {icon}
      {label}
    </button>
  );
}

interface DetailItemProps {
  term: string;
  description: string;
  breakText?: boolean;
}

function DetailItem({ term, description, breakText = false }: DetailItemProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <dt className="text-sm font-semibold text-slate-500">{term}</dt>

      <dd
        className={[
          "mt-2 text-sm font-bold leading-6 text-slate-900",
          breakText ? "break-all" : "",
        ].join(" ")}
      >
        {description}
      </dd>
    </div>
  );
}

interface SecurityActionCardProps {
  icon: ReactNode;
  title: string;
  description: string;
  danger?: boolean;
  children: ReactNode;
}

function SecurityActionCard({
  icon,
  title,
  description,
  danger = false,
  children,
}: SecurityActionCardProps) {
  return (
    <article
      className={[
        "rounded-2xl border p-5",
        danger ? "border-red-200 bg-red-50" : "border-slate-200 bg-slate-50",
      ].join(" ")}
    >
      <span
        className={[
          "flex h-11 w-11 items-center justify-center rounded-xl",
          danger ? "bg-white text-red-700" : "bg-white text-brand-700",
        ].join(" ")}
      >
        {icon}
      </span>

      <h3
        className={[
          "mt-4 font-display text-lg font-extrabold",
          danger ? "text-red-950" : "text-navy-950",
        ].join(" ")}
      >
        {title}
      </h3>

      <p
        className={[
          "mt-2 min-h-12 text-sm leading-6",
          danger ? "text-red-800" : "text-slate-600",
        ].join(" ")}
      >
        {description}
      </p>

      <div className="mt-5">{children}</div>
    </article>
  );
}

interface StatusActionButtonProps {
  label: string;
  icon: ReactNode;
  disabled: boolean;
  onClick: () => void;
}

function StatusActionButton({
  label,
  icon,
  disabled,
  onClick,
}: StatusActionButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
    >
      {icon}
      {label}
    </button>
  );
}

function AdminUserDetailsLoadingState() {
  return (
    <div
      className="mx-auto flex min-h-[65vh] max-w-7xl items-center justify-center"
      role="status"
      aria-live="polite"
    >
      <div className="text-center">
        <LoaderCircle
          className="mx-auto h-9 w-9 animate-spin text-brand-700"
          aria-hidden="true"
        />

        <h1 className="mt-5 font-display text-xl font-extrabold text-navy-950">
          Loading administrator details
        </h1>

        <p className="mt-2 text-sm text-slate-600">
          Retrieving protected administrator information.
        </p>
      </div>
    </div>
  );
}

interface AdminUserDetailsErrorStateProps {
  message: string;
  onRetry: () => void;
}

function AdminUserDetailsErrorState({
  message,
  onRetry,
}: AdminUserDetailsErrorStateProps) {
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

function createStatusConfirmationMessage(
  administrator: AdminUser,
  status: AdminStatus,
): string {
  switch (status) {
    case "ACTIVE":
      return `Activate ${administrator.fullName}'s administrator account?`;

    case "INACTIVE":
      return `Deactivate ${administrator.fullName}'s administrator account? The administrator will no longer be able to sign in.`;

    case "LOCKED":
      return `Lock ${administrator.fullName}'s administrator account? The administrator will remain blocked until the account is activated again.`;
  }
}

function createInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);

  if (words.length === 0) {
    return "AD";
  }

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return `${words[0][0]}${words.at(-1)?.[0] ?? ""}`.toUpperCase();
}

function formatEnumValue(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
    .join(" ");
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function resolveErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return "The requested administrator operation could not be completed. Check the backend connection and try again.";
}
