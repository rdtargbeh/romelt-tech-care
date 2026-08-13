/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER DETAIL PAGE
 * ================================================================
 *
 * Purpose:
 * Displays the complete reusable Romelt TechCare customer profile
 * for an authenticated administrator.
 *
 * Responsibilities:
 * - Loads one customer from the Spring Boot backend.
 * - Displays identity and reusable contact information.
 * - Displays customer address information.
 * - Displays communication preferences and restrictions.
 * - Displays email and phone verification status.
 * - Displays private communication and administrator notes.
 * - Displays customer activity timestamps.
 * - Displays customer lifecycle, archive, delete, and merge metadata.
 * - Allows administrators to archive or restore a customer.
 * - Allows administrators to soft-delete a customer.
 * - Provides navigation back to the customer directory.
 *
 * Real-data integration:
 *
 * GET    /api/v1/admin/customers/{customerId}
 * POST   /api/v1/admin/customers/{customerId}/archive
 * POST   /api/v1/admin/customers/{customerId}/restore
 * DELETE /api/v1/admin/customers/{customerId}
 *
 * Navigation:
 *
 * Back:
 * /admin/customers
 *
 * Current page:
 * /admin/customers/{customerId}
 *
 * Important:
 * This page displays the reusable Customer profile.
 *
 * Historical BookingRequest records retain their own customer/contact
 * snapshots and are not rewritten when this reusable profile changes.
 *
 * Customer editing and customer merging are separate administrative
 * workflows and should not be implemented as uncontrolled mutations
 * directly inside this read-focused profile page.
 * ================================================================
 */

import {
  Activity,
  Archive,
  ArrowLeft,
  BadgeCheck,
  Ban,
  CalendarClock,
  CalendarDays,
  Check,
  CircleAlert,
  Clock3,
  Database,
  FileText,
  Fingerprint,
  History,
  LoaderCircle,
  Mail,
  MapPin,
  MessageSquareText,
  Phone,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  ShieldX,
  Trash2,
  UserRound,
  UsersRound,
  Wrench,
  X,
} from "lucide-react";

import { useCallback, useEffect, useState, type ReactNode } from "react";

import { Link, useNavigate, useParams } from "react-router-dom";

import {
  archiveAdminCustomer,
  deleteAdminCustomer,
  getAdminCustomer,
  restoreAdminCustomer,
} from "@/services/admin-customer.service";

import type {
  AdminCustomer,
  CustomerSource,
  CustomerStatus,
} from "@/types/admin-customer.types";

// =====================================================================
// ACTION TYPE
// =====================================================================

type CustomerAction = "archive" | "restore" | "delete" | null;

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCustomerDetailPage() {
  const navigate = useNavigate();

  const { customerId = "" } = useParams<{ customerId: string }>();

  const [customer, setCustomer] = useState<AdminCustomer | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [activeAction, setActiveAction] = useState<CustomerAction>(null);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // ===================================================================
  // LOAD CUSTOMER
  // ===================================================================

  const loadCustomer = useCallback(
    async (signal?: AbortSignal) => {
      if (!customerId.trim()) {
        setCustomer(null);

        setErrorMessage("A customer ID was not provided.");

        setIsLoading(false);

        return;
      }

      setIsLoading(true);

      setErrorMessage(null);

      try {
        const response = await getAdminCustomer(customerId, signal);

        setCustomer(response);
      } catch (error) {
        if (!signal?.aborted) {
          setCustomer(null);

          setErrorMessage(
            error instanceof Error
              ? error.message
              : "The customer could not be loaded.",
          );
        }
      } finally {
        if (!signal?.aborted) {
          setIsLoading(false);
        }
      }
    },
    [customerId],
  );

  useEffect(() => {
    const controller = new AbortController();

    void loadCustomer(controller.signal);

    return () => {
      controller.abort();
    };
  }, [loadCustomer]);

  // ===================================================================
  // REFRESH
  // ===================================================================

  async function handleRefresh() {
    setActionMessage(null);

    await loadCustomer();
  }

  // ===================================================================
  // ARCHIVE
  // ===================================================================

  async function handleArchive() {
    if (!customer) {
      return;
    }

    const confirmed = window.confirm(
      `Archive ${customer.displayName}?\n\n` +
        "The customer will remain in the system but will be marked as archived.",
    );

    if (!confirmed) {
      return;
    }

    setActiveAction("archive");

    setErrorMessage(null);

    setActionMessage(null);

    try {
      const updatedCustomer = await archiveAdminCustomer(customer.customerId);

      setCustomer(updatedCustomer);

      setActionMessage("Customer archived successfully.");
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The customer could not be archived.",
      );

      scrollToTop();
    } finally {
      setActiveAction(null);
    }
  }

  // ===================================================================
  // RESTORE
  // ===================================================================

  async function handleRestore() {
    if (!customer) {
      return;
    }

    const confirmed = window.confirm(
      `Restore ${customer.displayName}?\n\n` +
        "The customer will be returned to an active operational state if permitted by the backend.",
    );

    if (!confirmed) {
      return;
    }

    setActiveAction("restore");

    setErrorMessage(null);

    setActionMessage(null);

    try {
      const updatedCustomer = await restoreAdminCustomer(customer.customerId);

      setCustomer(updatedCustomer);

      setActionMessage("Customer restored successfully.");
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The customer could not be restored.",
      );

      scrollToTop();
    } finally {
      setActiveAction(null);
    }
  }

  // ===================================================================
  // DELETE
  // ===================================================================

  async function handleDelete() {
    if (!customer) {
      return;
    }

    const confirmed = window.confirm(
      `Delete ${customer.displayName}?\n\n` +
        "This performs the customer deletion operation supported by the backend. " +
        "Historical business records may remain separately preserved.",
    );

    if (!confirmed) {
      return;
    }

    setActiveAction("delete");

    setErrorMessage(null);

    setActionMessage(null);

    try {
      await deleteAdminCustomer(customer.customerId);

      navigate("/admin/customers", {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The customer could not be deleted.",
      );

      scrollToTop();

      setActiveAction(null);
    }
  }

  // ===================================================================
  // LOADING
  // ===================================================================

  if (isLoading && !customer) {
    return (
      <section className="space-y-5">
        <BackToCustomers />

        <div className="flex min-h-80 items-center justify-center rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="text-center">
            <LoaderCircle
              className="mx-auto h-8 w-8 animate-spin text-brand-700"
              aria-hidden="true"
            />

            <p className="mt-3 text-sm font-semibold text-slate-600">
              Loading customer profile...
            </p>
          </div>
        </div>
      </section>
    );
  }

  // ===================================================================
  // LOAD ERROR
  // ===================================================================

  if (!customer) {
    return (
      <section className="space-y-5">
        <BackToCustomers />

        <div className="rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900">
          <div className="flex items-start gap-3">
            <CircleAlert
              className="mt-0.5 h-5 w-5 shrink-0"
              aria-hidden="true"
            />

            <div>
              <h1 className="font-display text-lg font-extrabold">
                Customer could not be loaded
              </h1>

              <p className="mt-1 text-sm leading-6">
                {errorMessage ||
                  "The requested customer record is unavailable."}
              </p>

              <button
                type="button"
                onClick={() => void handleRefresh()}
                className="focus-ring mt-4 inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-red-300 bg-white px-4 text-sm font-bold text-red-800 transition hover:bg-red-100"
              >
                <RefreshCw className="h-4 w-4" />
                Try again
              </button>
            </div>
          </div>
        </div>
      </section>
    );
  }

  // ===================================================================
  // DERIVED VALUES
  // ===================================================================

  const address = formatAddress(customer);

  const isArchived = customer.customerStatus === "ARCHIVED";

  const isDeleted = customer.customerStatus === "DELETED";

  const isMerged = customer.customerStatus === "MERGED";

  const canArchive = !isArchived && !isDeleted && !isMerged;

  const canRestore = isArchived;

  const canDelete = !isDeleted && !isMerged;

  const isActionRunning = activeAction !== null;

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <section className="space-y-5">
      {/* =============================================================
       * BACK
       * ============================================================= */}

      <BackToCustomers />

      {/* =============================================================
       * PAGE HEADER
       * ============================================================= */}

      <header className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex min-w-0 items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
              <UserRound className="h-7 w-7" />
            </div>

            <div className="min-w-0">
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-brand-700">
                Customer profile
              </p>

              <div className="mt-1 flex flex-wrap items-center gap-2">
                <h1 className="break-words font-display text-2xl font-black text-navy-950 sm:text-3xl">
                  {customer.displayName}
                </h1>

                <CustomerStatusBadge status={customer.customerStatus} />

                <CustomerSourceBadge source={customer.customerSource} />
              </div>

              <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1">
                <p className="font-mono text-xs font-bold text-slate-500">
                  {customer.customerNumber}
                </p>

                {customer.preferredName ? (
                  <p className="text-xs text-slate-500">
                    Preferred name:{" "}
                    <strong className="font-semibold text-slate-700">
                      {customer.preferredName}
                    </strong>
                  </p>
                ) : null}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void handleRefresh()}
              disabled={isLoading || isActionRunning}
              className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw
                className={["h-4 w-4", isLoading ? "animate-spin" : ""].join(
                  " ",
                )}
              />
              Refresh
            </button>

            {canRestore ? (
              <button
                type="button"
                onClick={() => void handleRestore()}
                disabled={isActionRunning}
                className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-emerald-300 bg-emerald-50 px-3 text-sm font-bold text-emerald-800 transition hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {activeAction === "restore" ? (
                  <LoaderCircle className="h-4 w-4 animate-spin" />
                ) : (
                  <RotateCcw className="h-4 w-4" />
                )}
                Restore
              </button>
            ) : null}

            {canArchive ? (
              <button
                type="button"
                onClick={() => void handleArchive()}
                disabled={isActionRunning}
                className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-amber-300 bg-amber-50 px-3 text-sm font-bold text-amber-800 transition hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {activeAction === "archive" ? (
                  <LoaderCircle className="h-4 w-4 animate-spin" />
                ) : (
                  <Archive className="h-4 w-4" />
                )}
                Archive
              </button>
            ) : null}

            {canDelete ? (
              <button
                type="button"
                onClick={() => void handleDelete()}
                disabled={isActionRunning}
                className="focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-red-300 bg-red-50 px-3 text-sm font-bold text-red-800 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {activeAction === "delete" ? (
                  <LoaderCircle className="h-4 w-4 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4" />
                )}
                Delete
              </button>
            ) : null}
          </div>
        </div>
      </header>

      {/* =============================================================
       * ACTION SUCCESS
       * ============================================================= */}

      {actionMessage ? (
        <div
          role="status"
          className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-emerald-900"
        >
          <Check className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />

          <div className="flex min-w-0 flex-1 items-start justify-between gap-3">
            <p className="text-sm font-semibold">{actionMessage}</p>

            <button
              type="button"
              onClick={() => setActionMessage(null)}
              className="focus-ring rounded-lg p-1 text-emerald-700 hover:bg-emerald-100"
              aria-label="Dismiss message"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * ACTION ERROR
       * ============================================================= */}

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />

          <div className="flex min-w-0 flex-1 items-start justify-between gap-3">
            <div>
              <p className="text-sm font-bold">Customer action failed</p>

              <p className="mt-1 text-sm leading-6">{errorMessage}</p>
            </div>

            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="focus-ring rounded-lg p-1 text-red-700 hover:bg-red-100"
              aria-label="Dismiss error"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * MERGED NOTICE
       * ============================================================= */}

      {isMerged ? (
        <div className="rounded-2xl border border-violet-200 bg-violet-50 p-4 text-violet-950">
          <div className="flex items-start gap-3">
            <UsersRound className="mt-0.5 h-5 w-5 shrink-0" />

            <div>
              <p className="font-bold">This customer profile has been merged</p>

              <p className="mt-1 text-sm leading-6 text-violet-800">
                This record is no longer the surviving reusable customer
                profile.
              </p>

              {customer.mergedIntoCustomerId ? (
                <Link
                  to={`/admin/customers/${customer.mergedIntoCustomerId}`}
                  className="focus-ring mt-3 inline-flex rounded-lg text-sm font-bold text-violet-900 underline decoration-violet-300 underline-offset-4 hover:text-violet-700"
                >
                  Open surviving customer
                </Link>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}

      {/* =============================================================
       * QUICK SUMMARY
       * ============================================================= */}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          icon={<Mail />}
          label="Email"
          value={customer.primaryEmail || "Not provided"}
        />

        <SummaryCard
          icon={<Phone />}
          label="Phone"
          value={customer.primaryPhone || "Not provided"}
        />

        <SummaryCard
          icon={<CalendarDays />}
          label="Last booking"
          value={formatDateTime(customer.lastBookingAt)}
        />

        <SummaryCard
          icon={<Wrench />}
          label="Last service"
          value={formatDateTime(customer.lastServiceCompletedAt)}
        />
      </div>

      {/* =============================================================
       * MAIN INFORMATION
       * ============================================================= */}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.6fr)_minmax(320px,0.8fr)]">
        <div className="space-y-5">
          {/* =========================================================
           * IDENTITY
           * ========================================================= */}

          <DetailSection
            title="Customer identity"
            description="Reusable identifying information for this customer."
            icon={<UserRound />}
          >
            <DetailGrid>
              <DetailItem label="First name" value={customer.firstName} />

              <DetailItem label="Last name" value={customer.lastName} />

              <DetailItem
                label="Preferred name"
                value={customer.preferredName}
              />

              <DetailItem label="Display name" value={customer.displayName} />

              <DetailItem
                label="Customer number"
                value={customer.customerNumber}
                mono
              />

              <DetailItem
                label="Customer source"
                value={formatEnumLabel(customer.customerSource)}
              />
            </DetailGrid>
          </DetailSection>

          {/* =========================================================
           * CONTACT
           * ========================================================= */}

          <DetailSection
            title="Contact information"
            description="Current reusable contact information and communication preference."
            icon={<Phone />}
          >
            <DetailGrid>
              <DetailItem
                label="Email address"
                value={customer.primaryEmail}
                icon={<Mail />}
              />

              <DetailItem
                label="Phone number"
                value={customer.primaryPhone}
                icon={<Phone />}
              />

              <DetailItem
                label="Preferred contact method"
                value={
                  customer.preferredContactMethod
                    ? formatEnumLabel(customer.preferredContactMethod)
                    : null
                }
              />

              <DetailItem
                label="Email verification"
                value={customer.emailVerified ? "Verified" : "Not verified"}
                detail={
                  customer.emailVerifiedAt
                    ? formatDateTime(customer.emailVerifiedAt)
                    : null
                }
                icon={customer.emailVerified ? <BadgeCheck /> : <ShieldX />}
              />

              <DetailItem
                label="Phone verification"
                value={customer.phoneVerified ? "Verified" : "Not verified"}
                detail={
                  customer.phoneVerifiedAt
                    ? formatDateTime(customer.phoneVerifiedAt)
                    : null
                }
                icon={customer.phoneVerified ? <BadgeCheck /> : <ShieldX />}
              />
            </DetailGrid>
          </DetailSection>

          {/* =========================================================
           * ADDRESS
           * ========================================================= */}

          <DetailSection
            title="Address"
            description="Current reusable customer address."
            icon={<MapPin />}
          >
            {address ? (
              <div className="rounded-xl bg-slate-50 p-4">
                <div className="flex items-start gap-3">
                  <MapPin className="mt-0.5 h-5 w-5 shrink-0 text-slate-400" />

                  <p className="whitespace-pre-line text-sm font-semibold leading-6 text-slate-800">
                    {address}
                  </p>
                </div>
              </div>
            ) : (
              <EmptyValue>No customer address has been recorded.</EmptyValue>
            )}
          </DetailSection>

          {/* =========================================================
           * COMMUNICATION
           * ========================================================= */}

          <DetailSection
            title="Communication preferences"
            description="Consent status and communication restrictions recorded for this customer."
            icon={<ShieldCheck />}
          >
            <div className="grid gap-3 sm:grid-cols-2">
              <PreferenceCard
                title="Marketing consent"
                enabled={customer.marketingConsent}
                enabledLabel="Consent recorded"
                disabledLabel="No consent recorded"
                detail={
                  customer.marketingConsentAt
                    ? `Recorded ${formatDateTime(customer.marketingConsentAt)}`
                    : null
                }
              />

              <PreferenceCard
                title="Do not email"
                enabled={customer.doNotEmail}
                enabledLabel="Email restricted"
                disabledLabel="Email allowed"
              />

              <PreferenceCard
                title="Do not call"
                enabled={customer.doNotCall}
                enabledLabel="Calls restricted"
                disabledLabel="Calls allowed"
              />

              <PreferenceCard
                title="Do not text"
                enabled={customer.doNotText}
                enabledLabel="Texting restricted"
                disabledLabel="Texting allowed"
              />
            </div>

            {customer.marketingConsentSource ? (
              <div className="mt-4">
                <DetailItem
                  label="Marketing consent source"
                  value={customer.marketingConsentSource}
                />
              </div>
            ) : null}
          </DetailSection>

          {/* =========================================================
           * NOTES
           * ========================================================= */}

          <DetailSection
            title="Customer notes"
            description="Operational and private administrator notes stored on the reusable customer profile."
            icon={<MessageSquareText />}
          >
            <div className="grid gap-4 xl:grid-cols-2">
              <NoteCard
                title="Communication notes"
                value={customer.communicationNotes}
                emptyMessage="No communication notes recorded."
              />

              <NoteCard
                title="Internal notes"
                value={customer.internalNotes}
                emptyMessage="No internal administrator notes recorded."
                privateNote
              />
            </div>
          </DetailSection>
        </div>

        {/* ===========================================================
         * SIDEBAR
         * =========================================================== */}

        <div className="space-y-5">
          {/* =========================================================
           * STATUS
           * ========================================================= */}

          <DetailSection
            title="Customer status"
            description="Current operational state of this customer profile."
            icon={<Activity />}
          >
            <div className="space-y-3">
              <DetailItem
                label="Status"
                value={formatEnumLabel(customer.customerStatus)}
              />

              <DetailItem
                label="Source"
                value={formatEnumLabel(customer.customerSource)}
              />

              <DetailItem
                label="Record version"
                value={String(customer.rowVersion)}
              />
            </div>
          </DetailSection>

          {/* =========================================================
           * ACTIVITY
           * ========================================================= */}

          <DetailSection
            title="Customer activity"
            description="Important customer interaction timestamps."
            icon={<History />}
          >
            <TimelineList>
              <TimelineItem
                icon={<CalendarClock />}
                label="First contact"
                value={formatDateTime(customer.firstContactAt)}
              />

              <TimelineItem
                icon={<Phone />}
                label="Last contacted"
                value={formatDateTime(customer.lastContactedAt)}
              />

              <TimelineItem
                icon={<CalendarDays />}
                label="Last booking"
                value={formatDateTime(customer.lastBookingAt)}
              />

              <TimelineItem
                icon={<Wrench />}
                label="Last service completed"
                value={formatDateTime(customer.lastServiceCompletedAt)}
              />

              <TimelineItem
                icon={<Activity />}
                label="Last activity"
                value={formatDateTime(customer.lastActivityAt)}
              />
            </TimelineList>
          </DetailSection>

          {/* =========================================================
           * RECORD HISTORY
           * ========================================================= */}

          <DetailSection
            title="Record history"
            description="Creation and modification timestamps for this customer record."
            icon={<Database />}
          >
            <TimelineList>
              <TimelineItem
                icon={<CalendarDays />}
                label="Created"
                value={formatDateTime(customer.createdAt)}
              />

              <TimelineItem
                icon={<Clock3 />}
                label="Last updated"
                value={formatDateTime(customer.updatedAt)}
              />

              {customer.archivedAt ? (
                <TimelineItem
                  icon={<Archive />}
                  label="Archived"
                  value={formatDateTime(customer.archivedAt)}
                />
              ) : null}

              {customer.deletedAt ? (
                <TimelineItem
                  icon={<Trash2 />}
                  label="Deleted"
                  value={formatDateTime(customer.deletedAt)}
                />
              ) : null}

              {customer.mergedAt ? (
                <TimelineItem
                  icon={<UsersRound />}
                  label="Merged"
                  value={formatDateTime(customer.mergedAt)}
                />
              ) : null}
            </TimelineList>
          </DetailSection>

          {/* =========================================================
           * AUDIT IDS
           * ========================================================= */}

          <DetailSection
            title="Administrative audit"
            description="Administrator identifiers associated with customer record changes."
            icon={<Fingerprint />}
          >
            <div className="space-y-3">
              <DetailItem
                label="Created by"
                value={customer.createdByAdminUserId}
                mono
              />

              <DetailItem
                label="Updated by"
                value={customer.updatedByAdminUserId}
                mono
              />

              {customer.archivedByAdminUserId ? (
                <DetailItem
                  label="Archived by"
                  value={customer.archivedByAdminUserId}
                  mono
                />
              ) : null}

              {customer.deletedByAdminUserId ? (
                <DetailItem
                  label="Deleted by"
                  value={customer.deletedByAdminUserId}
                  mono
                />
              ) : null}

              {customer.mergedByAdminUserId ? (
                <DetailItem
                  label="Merged by"
                  value={customer.mergedByAdminUserId}
                  mono
                />
              ) : null}
            </div>
          </DetailSection>

          {/* =========================================================
           * MERGE INFORMATION
           * ========================================================= */}

          {customer.mergedIntoCustomerId ||
          customer.mergedAt ||
          customer.mergedByAdminUserId ? (
            <DetailSection
              title="Merge information"
              description="Information about this customer's merge lifecycle."
              icon={<UsersRound />}
            >
              <div className="space-y-3">
                {customer.mergedIntoCustomerId ? (
                  <div className="rounded-xl bg-violet-50 p-3">
                    <p className="text-xs font-bold uppercase tracking-wide text-violet-700">
                      Surviving customer
                    </p>

                    <Link
                      to={`/admin/customers/${customer.mergedIntoCustomerId}`}
                      className="focus-ring mt-1 block break-all rounded-md font-mono text-sm font-bold text-violet-900 hover:text-violet-700"
                    >
                      {customer.mergedIntoCustomerId}
                    </Link>
                  </div>
                ) : null}

                <DetailItem
                  label="Merged at"
                  value={
                    customer.mergedAt ? formatDateTime(customer.mergedAt) : null
                  }
                />

                <DetailItem
                  label="Merged by"
                  value={customer.mergedByAdminUserId}
                  mono
                />
              </div>
            </DetailSection>
          ) : null}
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// BACK LINK
// =====================================================================

function BackToCustomers() {
  return (
    <Link
      to="/admin/customers"
      className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-bold text-slate-600 transition hover:text-brand-700"
    >
      <ArrowLeft className="h-4 w-4" />
      Back to customers
    </Link>
  );
}

// =====================================================================
// DETAIL SECTION
// =====================================================================

interface DetailSectionProps {
  title: string;

  description: string;

  icon: ReactNode;

  children: ReactNode;
}

function DetailSection({
  title,
  description,
  icon,
  children,
}: DetailSectionProps) {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <header className="flex items-start gap-3 border-b border-slate-100 px-4 py-4 sm:px-5">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <div className="min-w-0">
          <h2 className="font-display text-lg font-extrabold text-navy-950">
            {title}
          </h2>

          <p className="mt-1 text-sm leading-5 text-slate-500">{description}</p>
        </div>
      </header>

      <div className="p-4 sm:p-5">{children}</div>
    </section>
  );
}

// =====================================================================
// DETAIL GRID
// =====================================================================

function DetailGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-3 sm:grid-cols-2">{children}</div>;
}

// =====================================================================
// DETAIL ITEM
// =====================================================================

interface DetailItemProps {
  label: string;

  value: string | null | undefined;

  detail?: string | null;

  icon?: ReactNode;

  mono?: boolean;
}

function DetailItem({
  label,
  value,
  detail,
  icon,
  mono = false,
}: DetailItemProps) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-3">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-slate-500">
        {icon ? (
          <span className="[&>svg]:h-3.5 [&>svg]:w-3.5">{icon}</span>
        ) : null}

        {label}
      </div>

      <p
        className={[
          "mt-1.5 break-words text-sm font-semibold text-slate-800",
          mono ? "font-mono text-xs" : "",
        ].join(" ")}
      >
        {value || "Not recorded"}
      </p>

      {detail ? (
        <p className="mt-1 text-xs leading-5 text-slate-500">{detail}</p>
      ) : null}
    </div>
  );
}

// =====================================================================
// SUMMARY CARD
// =====================================================================

interface SummaryCardProps {
  icon: ReactNode;

  label: string;

  value: string;
}

function SummaryCard({ icon, label, value }: SummaryCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-1 break-words text-sm font-bold text-slate-900">
            {value}
          </p>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// PREFERENCE CARD
// =====================================================================

interface PreferenceCardProps {
  title: string;

  enabled: boolean;

  enabledLabel: string;

  disabledLabel: string;

  detail?: string | null;
}

function PreferenceCard({
  title,
  enabled,
  enabledLabel,
  disabledLabel,
  detail,
}: PreferenceCardProps) {
  return (
    <div
      className={[
        "rounded-xl border p-4",
        enabled
          ? "border-amber-200 bg-amber-50"
          : "border-slate-200 bg-slate-50",
      ].join(" ")}
    >
      <div className="flex items-start gap-3">
        <div
          className={[
            "flex h-8 w-8 shrink-0 items-center justify-center rounded-lg",
            enabled
              ? "bg-amber-100 text-amber-800"
              : "bg-emerald-100 text-emerald-700",
          ].join(" ")}
        >
          {enabled ? (
            <Ban className="h-4 w-4" />
          ) : (
            <Check className="h-4 w-4" />
          )}
        </div>

        <div>
          <p className="text-sm font-bold text-slate-900">{title}</p>

          <p
            className={[
              "mt-1 text-xs font-semibold",
              enabled ? "text-amber-800" : "text-emerald-700",
            ].join(" ")}
          >
            {enabled ? enabledLabel : disabledLabel}
          </p>

          {detail ? (
            <p className="mt-1 text-xs leading-5 text-slate-500">{detail}</p>
          ) : null}
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// NOTE CARD
// =====================================================================

interface NoteCardProps {
  title: string;

  value: string | null;

  emptyMessage: string;

  privateNote?: boolean;
}

function NoteCard({
  title,
  value,
  emptyMessage,
  privateNote = false,
}: NoteCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-center gap-2">
        {privateNote ? (
          <ShieldCheck className="h-4 w-4 text-amber-700" />
        ) : (
          <FileText className="h-4 w-4 text-slate-500" />
        )}

        <h3 className="text-sm font-bold text-slate-900">{title}</h3>
      </div>

      {privateNote ? (
        <p className="mt-1 text-xs font-semibold text-amber-700">
          Administrator only
        </p>
      ) : null}

      {value ? (
        <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">
          {value}
        </p>
      ) : (
        <p className="mt-3 text-sm italic text-slate-500">{emptyMessage}</p>
      )}
    </div>
  );
}

// =====================================================================
// TIMELINE
// =====================================================================

function TimelineList({ children }: { children: ReactNode }) {
  return <div className="divide-y divide-slate-100">{children}</div>;
}

interface TimelineItemProps {
  icon: ReactNode;

  label: string;

  value: string;
}

function TimelineItem({ icon, label, value }: TimelineItemProps) {
  return (
    <div className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
      <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600 [&>svg]:h-4 [&>svg]:w-4">
        {icon}
      </div>

      <div className="min-w-0">
        <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
          {label}
        </p>

        <p className="mt-1 text-sm font-semibold leading-5 text-slate-800">
          {value}
        </p>
      </div>
    </div>
  );
}

// =====================================================================
// EMPTY VALUE
// =====================================================================

function EmptyValue({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center">
      <p className="text-sm text-slate-500">{children}</p>
    </div>
  );
}

// =====================================================================
// STATUS BADGE
// =====================================================================

function CustomerStatusBadge({ status }: { status: CustomerStatus }) {
  return (
    <span
      className={[
        "inline-flex rounded-full px-2.5 py-1 text-[11px] font-extrabold uppercase tracking-wide",
        resolveStatusClasses(status),
      ].join(" ")}
    >
      {formatEnumLabel(status)}
    </span>
  );
}

// =====================================================================
// SOURCE BADGE
// =====================================================================

function CustomerSourceBadge({ source }: { source: CustomerSource }) {
  return (
    <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-600">
      {formatEnumLabel(source)}
    </span>
  );
}

// =====================================================================
// STATUS STYLES
// =====================================================================

function resolveStatusClasses(status: CustomerStatus): string {
  switch (status) {
    case "ACTIVE":
      return "bg-emerald-100 text-emerald-800";

    case "INACTIVE":
      return "bg-slate-200 text-slate-700";

    case "BLOCKED":
      return "bg-red-100 text-red-800";

    case "ARCHIVED":
      return "bg-amber-100 text-amber-800";

    case "MERGED":
      return "bg-violet-100 text-violet-800";

    case "DELETED":
      return "bg-rose-100 text-rose-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

// =====================================================================
// ADDRESS FORMATTER
// =====================================================================

function formatAddress(customer: AdminCustomer): string | null {
  const lines: string[] = [];

  const street = [customer.streetAddress, customer.addressLine2]
    .filter(Boolean)
    .join("\n");

  if (street) {
    lines.push(street);
  }

  const locality = [customer.city, customer.stateRegion, customer.postalCode]
    .filter(Boolean)
    .join(", ");

  if (locality) {
    lines.push(locality);
  }

  if (customer.countryCode) {
    lines.push(customer.countryCode);
  }

  return lines.length > 0 ? lines.join("\n") : null;
}

// =====================================================================
// ENUM FORMATTER
// =====================================================================

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

// =====================================================================
// DATE FORMATTER
// =====================================================================

function formatDateTime(value: string | null): string {
  if (!value) {
    return "No activity";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

// =====================================================================
// SCROLL
// =====================================================================

function scrollToTop() {
  window.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}
