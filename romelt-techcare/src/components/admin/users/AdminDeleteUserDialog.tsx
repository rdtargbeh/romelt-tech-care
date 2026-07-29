/**
 * ================================================================
 * ROMELT TECHCARE — DELETE ADMINISTRATOR DIALOG
 * ================================================================
 *
 * Purpose:
 * Provides explicit confirmation before permanently deleting an
 * administrator account.
 *
 * Responsibilities:
 * - Clearly identifies the account being deleted.
 * - Requires the administrator's email address as confirmation.
 * - Displays backend deletion restrictions and errors.
 * - Prevents accidental duplicate deletion requests.
 * - Prevents dismissal while a deletion is in progress.
 *
 * Real-data integration:
 * Uses:
 *
 * DELETE /api/v1/admin/users/{adminUserId}
 *
 * Backend safeguards:
 * The backend remains responsible for preventing deletion of the
 * current account or the final SUPER_ADMIN account.
 * ================================================================
 */

import { LoaderCircle, ShieldAlert, Trash2, X } from "lucide-react";
import { type ChangeEvent, type FormEvent, useEffect, useState } from "react";

import { ApiError } from "@/lib/api-error";
import { deleteAdminUser } from "@/services/admin-user.service";
import type { AdminUser } from "@/types/admin-user.types";

interface AdminDeleteUserDialogProps {
  administrator: AdminUser;
  open: boolean;
  onClose: () => void;
  onDeleted: (administrator: AdminUser) => void;
}

export function AdminDeleteUserDialog({
  administrator,
  open,
  onClose,
  onDeleted,
}: AdminDeleteUserDialogProps) {
  const [confirmation, setConfirmation] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const confirmationMatches =
    confirmation.trim().toLowerCase() ===
    administrator.email.trim().toLowerCase();

  useEffect(() => {
    if (!open) {
      resetDialog();
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && !isDeleting) {
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
  }, [open, isDeleting]);

  if (!open) {
    return null;
  }

  function resetDialog() {
    setConfirmation("");
    setErrorMessage(null);
    setIsDeleting(false);
  }

  function handleClose() {
    if (isDeleting) {
      return;
    }

    resetDialog();
    onClose();
  }

  function handleConfirmationChange(event: ChangeEvent<HTMLInputElement>) {
    setConfirmation(event.target.value);
    setErrorMessage(null);
  }

  async function handleDelete(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!confirmationMatches) {
      setErrorMessage(
        "Enter the administrator's complete email address to confirm deletion.",
      );
      return;
    }

    setErrorMessage(null);
    setIsDeleting(true);

    try {
      await deleteAdminUser(administrator.adminUserId);

      resetDialog();
      onDeleted(administrator);
      onClose();
    } catch (error) {
      setErrorMessage(resolveDeleteError(error));
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[70] flex items-end justify-center p-0 sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-admin-dialog-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/65"
        onClick={handleClose}
        disabled={isDeleting}
        aria-label="Close delete administrator dialog"
      />

      <div className="relative z-10 w-full rounded-t-3xl bg-white shadow-2xl sm:max-w-lg sm:rounded-3xl">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-5 sm:px-6">
          <div className="flex items-start gap-4">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-red-50 text-red-700">
              <Trash2 className="h-6 w-6" aria-hidden="true" />
            </span>

            <div>
              <p className="text-sm font-bold uppercase tracking-[0.16em] text-red-700">
                Permanent action
              </p>

              <h2
                id="delete-admin-dialog-title"
                className="mt-1 font-display text-2xl font-extrabold text-navy-950"
              >
                Delete administrator
              </h2>
            </div>
          </div>

          <button
            type="button"
            onClick={handleClose}
            disabled={isDeleting}
            className="focus-ring flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <form onSubmit={handleDelete} noValidate>
          <div className="px-5 py-6 sm:px-6">
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4">
              <div className="flex items-start gap-3">
                <ShieldAlert
                  className="mt-0.5 h-5 w-5 shrink-0 text-red-700"
                  aria-hidden="true"
                />

                <div className="text-sm leading-6 text-red-900">
                  <p className="font-bold">This action cannot be undone.</p>

                  <p className="mt-1">
                    The administrator account for{" "}
                    <span className="font-bold">{administrator.fullName}</span>{" "}
                    will be permanently removed.
                  </p>
                </div>
              </div>
            </div>

            <div className="mt-5">
              <label
                htmlFor="delete-admin-confirmation"
                className="block text-sm font-bold text-slate-800"
              >
                Enter{" "}
                <span className="break-all text-red-700">
                  {administrator.email}
                </span>{" "}
                to confirm
              </label>

              <input
                id="delete-admin-confirmation"
                type="email"
                value={confirmation}
                onChange={handleConfirmationChange}
                disabled={isDeleting}
                autoComplete="off"
                placeholder={administrator.email}
                className="focus-ring mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-red-500 disabled:cursor-not-allowed disabled:bg-slate-100"
              />

              {errorMessage && (
                <p className="mt-2 text-sm leading-6 text-red-700" role="alert">
                  {errorMessage}
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50 px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
            <button
              type="button"
              onClick={handleClose}
              disabled={isDeleting}
              className="focus-ring inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={isDeleting || !confirmationMatches}
              className="focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-red-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isDeleting ? (
                <>
                  <LoaderCircle
                    className="h-4 w-4 animate-spin"
                    aria-hidden="true"
                  />
                  Deleting administrator...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                  Delete permanently
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function resolveDeleteError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return "Unable to delete the administrator account. Please try again.";
}
