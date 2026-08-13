/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING DETAIL PAGE
 * ================================================================
 *
 * Purpose:
 * Displays the complete booking record and allows authenticated
 * administrators to move a booking through its supported lifecycle.
 *
 * Responsibilities:
 * - Displays customer and notification information.
 * - Displays personal/business booking information.
 * - Displays requested service and location information.
 * - Displays requested and confirmed scheduling information.
 * - Displays lifecycle timestamps and outcome information.
 * - Restricts status choices to backend-supported transitions.
 * - Collects status-specific information before lifecycle updates.
 * - Requires confirmed appointment schedule when confirming.
 * - Requires completion summary when completing.
 * - Requires cancellation, decline, or expiration reasons when needed.
 * - Supports review eligibility information.
 * - Supports administrator notes and status-change reasons.
 *
 * Real-data integration:
 *
 * GET
 * /api/v1/admin/booking-requests/{bookingRequestId}
 *
 * PATCH
 * /api/v1/admin/booking-requests/{bookingRequestId}/status
 *
 * Backend lifecycle:
 *
 * PENDING
 *   -> UNDER_REVIEW
 *   -> CONFIRMED
 *   -> CANCELLED
 *   -> DECLINED
 *   -> EXPIRED
 *
 * UNDER_REVIEW
 *   -> CONFIRMED
 *   -> CANCELLED
 *   -> DECLINED
 *   -> EXPIRED
 *
 * CONFIRMED
 *   -> COMPLETED
 *   -> CANCELLED
 *
 * COMPLETED / CANCELLED / DECLINED / EXPIRED
 *   -> terminal
 * ================================================================
 */

import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ArrowLeft,
  Building2,
  CalendarCheck2,
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  Clock3,
  FileText,
  LoaderCircle,
  Mail,
  MapPin,
  Phone,
  Save,
  ShieldCheck,
  UserRound,
  UserRoundCog,
  Wrench,
  XCircle,
} from "lucide-react";

import { Link, useParams } from "react-router-dom";

import {
  getAdminBookingRequest,
  updateAdminBookingStatus,
} from "@/services/admin-customer-request.service";

import type {
  AdminBookingRequest,
  AdminBookingStatusUpdatePayload,
  BookingRequestStatus,
} from "@/types/admin-customer-request.types";

// =====================================================================
// STATUS FORM
// =====================================================================

interface StatusUpdateFormState {
  status: BookingRequestStatus | "";

  scheduledStartAt: string;
  scheduledEndAt: string;
  scheduledTimezone: string;

  cancellationReason: string;
  declineReason: string;
  expirationReason: string;

  completionSummary: string;
  completionNotes: string;

  reviewEligible: boolean;
  reviewEligibilityNotes: string;

  adminNotes: string;

  changeReason: string;
}

// =====================================================================
// PAGE
// =====================================================================

export default function AdminBookingDetailsPage() {
  const { bookingRequestId } = useParams();

  const [booking, setBooking] = useState<AdminBookingRequest | null>(null);

  const [statusForm, setStatusForm] = useState<StatusUpdateFormState | null>(
    null,
  );

  const [isLoading, setIsLoading] = useState(true);

  const [isUpdating, setIsUpdating] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // ===================================================================
  // LOAD BOOKING
  // ===================================================================

  useEffect(() => {
    const controller = new AbortController();

    async function loadBooking() {
      if (!bookingRequestId) {
        setErrorMessage("Booking request ID is missing.");

        setIsLoading(false);

        return;
      }

      setIsLoading(true);
      setErrorMessage(null);

      try {
        const response = await getAdminBookingRequest(
          bookingRequestId,
          controller.signal,
        );

        if (controller.signal.aborted) {
          return;
        }

        setBooking(response);

        setStatusForm(createStatusForm(response));
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        setErrorMessage(
          error instanceof Error
            ? error.message
            : "Booking request could not be loaded.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadBooking();

    return () => controller.abort();
  }, [bookingRequestId]);

  // ===================================================================
  // DERIVED DATA
  // ===================================================================

  const allowedStatuses = useMemo(
    () => (booking ? getAllowedStatusTransitions(booking.status) : []),
    [booking],
  );

  const isTerminal = booking ? isTerminalStatus(booking.status) : false;

  const requiresConfirmationSchedule = statusForm?.status === "CONFIRMED";

  const requiresCompletion = statusForm?.status === "COMPLETED";

  const requiresCancellationReason = statusForm?.status === "CANCELLED";

  const requiresDeclineReason = statusForm?.status === "DECLINED";

  const requiresExpirationReason = statusForm?.status === "EXPIRED";

  // ===================================================================
  // FORM CHANGE
  // ===================================================================

  function updateStatusField<K extends keyof StatusUpdateFormState>(
    field: K,
    value: StatusUpdateFormState[K],
  ) {
    setStatusForm((current) =>
      current
        ? {
            ...current,
            [field]: value,
          }
        : current,
    );

    setErrorMessage(null);
    setSuccessMessage(null);
  }

  function handleTextChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) {
    const { name, value } = event.target;

    updateStatusField(name as keyof StatusUpdateFormState, value as never);
  }

  function handleStatusSelection(event: ChangeEvent<HTMLSelectElement>) {
    const status = event.target.value as BookingRequestStatus;

    setStatusForm((current) => {
      if (!current) {
        return current;
      }

      return {
        ...current,

        status,

        /*
         * Keep existing values where they may be relevant, but clear
         * status-specific reason fields that belong to a different
         * transition.
         */
        cancellationReason:
          status === "CANCELLED" ? current.cancellationReason : "",

        declineReason: status === "DECLINED" ? current.declineReason : "",

        expirationReason: status === "EXPIRED" ? current.expirationReason : "",

        completionSummary:
          status === "COMPLETED" ? current.completionSummary : "",

        completionNotes: status === "COMPLETED" ? current.completionNotes : "",
      };
    });

    setErrorMessage(null);
    setSuccessMessage(null);
  }

  // ===================================================================
  // UPDATE STATUS
  // ===================================================================

  async function handleStatusUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!bookingRequestId || !booking || !statusForm) {
      return;
    }

    if (isUpdating) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);

    const validationMessage = validateStatusUpdate(booking, statusForm);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      return;
    }

    const payload = createStatusUpdatePayload(booking, statusForm);

    setIsUpdating(true);

    try {
      const updatedBooking = await updateAdminBookingStatus(
        bookingRequestId,
        payload,
      );

      setBooking(updatedBooking);

      setStatusForm(createStatusForm(updatedBooking));

      setSuccessMessage(
        `Booking updated successfully to ${formatLabel(
          updatedBooking.status,
        )}.`,
      );
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "Booking status could not be updated.",
      );
    } finally {
      setIsUpdating(false);
    }
  }

  // ===================================================================
  // LOADING / ERROR
  // ===================================================================

  if (isLoading) {
    return (
      <div
        role="status"
        className="flex min-h-72 items-center justify-center gap-3 text-slate-600"
      >
        <LoaderCircle className="h-6 w-6 animate-spin" />
        Loading booking request...
      </div>
    );
  }

  if (errorMessage && !booking) {
    return (
      <section className="space-y-6">
        <BackLink />

        <ErrorMessage message={errorMessage} />
      </section>
    );
  }

  if (!booking || !statusForm) {
    return null;
  }

  // ===================================================================
  // ADDRESSES
  // ===================================================================

  const serviceAddress = formatAddress([
    booking.streetAddress,
    booking.addressLine2,
    booking.city,
    booking.stateRegion,
    booking.postalCode,
    booking.countryCode,
  ]);

  const businessAddress = formatAddress([
    booking.businessStreetAddress,
    booking.businessCity,
    booking.businessState,
    booking.businessPostalCode,
    booking.businessCountryCode,
  ]);

  return (
    <section className="space-y-6">
      <BackLink />

      {/* =============================================================
          HEADER
          ============================================================= */}

      <header className="rounded-3xl bg-navy-950 p-6 text-white shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-300">
              Booking request
            </p>

            <h1 className="mt-3 font-display text-3xl font-black">
              {booking.fullName}
            </h1>

            {booking.bookingFor === "BUSINESS" && booking.businessName ? (
              <p className="mt-1 font-semibold text-slate-200">
                {booking.businessName}
              </p>
            ) : null}

            <p className="mt-2 text-slate-300">{booking.referenceNumber}</p>

            <div className="mt-4 flex flex-wrap gap-2">
              <HeaderBadge>{formatLabel(booking.bookingFor)}</HeaderBadge>

              <HeaderBadge>
                Source: {formatLabel(booking.bookingSource)}
              </HeaderBadge>
            </div>
          </div>

          <StatusBadge status={booking.status} />
        </div>
      </header>

      {/* =============================================================
          ALERTS
          ============================================================= */}

      {errorMessage ? <ErrorMessage message={errorMessage} /> : null}

      {successMessage ? (
        <div
          role="status"
          className="flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-950"
        >
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

          <p className="font-semibold">{successMessage}</p>
        </div>
      ) : null}

      {/* =============================================================
          MAIN GRID
          ============================================================= */}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        {/* ===========================================================
            LEFT SIDE
            =========================================================== */}

        <div className="space-y-6">
          {/* =========================================================
              CUSTOMER
              ========================================================= */}

          <DetailCard title="Customer" icon={<UserRound />}>
            <DetailRow
              label="Booking for"
              value={formatLabel(booking.bookingFor)}
            />

            <DetailRow label="Customer" value={booking.fullName} />

            <DetailRow
              label="Customer ID"
              value={booking.customerId || "Not linked"}
            />

            <DetailRow
              label="Preferred contact"
              value={formatLabel(booking.preferredContactMethod)}
            />

            <ContactLink
              icon={<Mail />}
              href={`mailto:${booking.email}`}
              value={booking.email}
            />

            <ContactLink
              icon={<Phone />}
              href={`tel:${booking.phone}`}
              value={booking.phone}
            />
          </DetailCard>

          {/* =========================================================
              NOTIFICATIONS
              ========================================================= */}

          <DetailCard title="Notification Delivery" icon={<Mail />}>
            <DetailRow
              label="Notification email"
              value={booking.notificationEmail || booking.email}
            />

            <DetailRow
              label="Notification phone"
              value={booking.notificationPhone || booking.phone}
            />

            <div className="mt-4 rounded-xl border border-brand-200 bg-brand-50 p-4">
              <p className="text-sm leading-6 text-brand-950">
                Booking lifecycle updates are sent using these notification
                destinations according to the backend notification workflow.
              </p>
            </div>
          </DetailCard>

          {/* =========================================================
              BUSINESS
              ========================================================= */}

          {booking.bookingFor === "BUSINESS" ? (
            <DetailCard title="Business" icon={<Building2 />}>
              <DetailRow
                label="Business name"
                value={booking.businessName || "Not provided"}
              />

              <DetailRow
                label="Contact role"
                value={booking.businessContactRole || "Not provided"}
              />

              <DetailRow
                label="Business email"
                value={booking.businessEmail || "Not provided"}
              />

              <DetailRow
                label="Business phone"
                value={booking.businessPhone || "Not provided"}
              />

              <DetailRow
                label="Business address"
                value={businessAddress || "Not provided"}
              />
            </DetailCard>
          ) : null}

          {/* =========================================================
              SERVICE
              ========================================================= */}

          <DetailCard title="Service Request" icon={<Wrench />}>
            <DetailRow label="Service" value={booking.serviceType} />

            <DetailRow
              label="Service ID"
              value={booking.serviceId || "Not linked to catalog"}
            />

            <DetailRow
              label="Service method"
              value={formatLabel(booking.serviceMethod)}
            />

            <DetailRow
              label="Device / equipment"
              value={booking.deviceType || "Not provided"}
            />

            <LongText
              label="Problem or requested service"
              value={booking.problemDescription}
            />
          </DetailCard>

          {/* =========================================================
              REQUESTED SCHEDULE
              ========================================================= */}

          <DetailCard title="Requested Schedule" icon={<CalendarDays />}>
            <DetailRow
              label="Preferred date"
              value={formatDate(booking.preferredDate)}
            />

            <DetailRow
              label="Preferred time"
              value={formatLabel(booking.preferredTime)}
            />

            <DetailRow
              label="Alternate date"
              value={
                booking.alternateDate
                  ? formatDate(booking.alternateDate)
                  : "Not provided"
              }
            />
          </DetailCard>

          {/* =========================================================
              CONFIRMED SCHEDULE
              ========================================================= */}

          <DetailCard title="Confirmed Appointment" icon={<CalendarCheck2 />}>
            {booking.scheduledStartAt ? (
              <>
                <DetailRow
                  label="Starts"
                  value={formatDateTime(booking.scheduledStartAt)}
                />

                <DetailRow
                  label="Ends"
                  value={
                    booking.scheduledEndAt
                      ? formatDateTime(booking.scheduledEndAt)
                      : "Not provided"
                  }
                />

                <DetailRow
                  label="Timezone"
                  value={booking.scheduledTimezone || "Not provided"}
                />

                <DetailRow
                  label="Confirmed"
                  value={
                    booking.confirmedAt
                      ? formatDateTime(booking.confirmedAt)
                      : "Not recorded"
                  }
                />
              </>
            ) : (
              <EmptyState>No appointment has been confirmed yet.</EmptyState>
            )}
          </DetailCard>

          {/* =========================================================
              LOCATION
              ========================================================= */}

          <DetailCard title="Service Location" icon={<MapPin />}>
            <DetailRow
              label="Address"
              value={serviceAddress || "No service address provided"}
            />

            <DetailRow
              label="Country"
              value={booking.countryCode || "Not provided"}
            />
          </DetailCard>

          {/* =========================================================
              OUTCOME
              ========================================================= */}

          <DetailCard title="Lifecycle Outcome" icon={<FileText />}>
            <LifecycleOutcome booking={booking} />
          </DetailCard>

          {/* =========================================================
              ADMIN NOTES
              ========================================================= */}

          <DetailCard title="Administrator Notes" icon={<UserRoundCog />}>
            <LongText
              value={booking.adminNotes || "No administrator notes recorded."}
            />
          </DetailCard>
        </div>

        {/* ===========================================================
            RIGHT SIDE
            =========================================================== */}

        <div className="space-y-6">
          {/* =========================================================
              STATUS UPDATE
              ========================================================= */}

          <DetailCard title="Update Booking" icon={<Save />}>
            {isTerminal ? (
              <TerminalStatusCard status={booking.status} />
            ) : (
              <form onSubmit={handleStatusUpdate} className="space-y-5">
                <div>
                  <label
                    htmlFor="status"
                    className="text-sm font-bold text-slate-700"
                  >
                    New booking status
                    <span className="text-red-600"> *</span>
                  </label>

                  <select
                    id="status"
                    name="status"
                    value={statusForm.status}
                    onChange={handleStatusSelection}
                    required
                    className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2"
                  >
                    <option value="">Select next status</option>

                    {allowedStatuses.map((status) => (
                      <option key={status} value={status}>
                        {formatLabel(status)}
                      </option>
                    ))}
                  </select>

                  <p className="mt-2 text-xs leading-5 text-slate-500">
                    Only lifecycle transitions allowed by the backend are shown.
                  </p>
                </div>

                {/* ===================================================
                    CONFIRMED
                    =================================================== */}

                {requiresConfirmationSchedule ? (
                  <ConfirmedScheduleFields
                    form={statusForm}
                    onChange={handleTextChange}
                  />
                ) : null}

                {/* ===================================================
                    COMPLETED
                    =================================================== */}

                {requiresCompletion ? (
                  <CompletionFields
                    form={statusForm}
                    onChange={handleTextChange}
                    onReviewEligibleChange={(value) =>
                      updateStatusField("reviewEligible", value)
                    }
                  />
                ) : null}

                {/* ===================================================
                    CANCELLED
                    =================================================== */}

                {requiresCancellationReason ? (
                  <StatusReasonField
                    label="Cancellation reason"
                    name="cancellationReason"
                    value={statusForm.cancellationReason}
                    onChange={handleTextChange}
                    placeholder="Explain why this booking is being cancelled."
                  />
                ) : null}

                {/* ===================================================
                    DECLINED
                    =================================================== */}

                {requiresDeclineReason ? (
                  <StatusReasonField
                    label="Decline reason"
                    name="declineReason"
                    value={statusForm.declineReason}
                    onChange={handleTextChange}
                    placeholder="Explain why Romelt TechCare cannot accept this booking."
                  />
                ) : null}

                {/* ===================================================
                    EXPIRED
                    =================================================== */}

                {requiresExpirationReason ? (
                  <StatusReasonField
                    label="Expiration reason"
                    name="expirationReason"
                    value={statusForm.expirationReason}
                    onChange={handleTextChange}
                    placeholder="Explain why this request is being expired."
                  />
                ) : null}

                {/* ===================================================
                    CHANGE REASON
                    =================================================== */}

                {statusForm.status ? (
                  <label className="block">
                    <span className="text-sm font-bold text-slate-700">
                      Status-change reason
                    </span>

                    <textarea
                      name="changeReason"
                      value={statusForm.changeReason}
                      onChange={handleTextChange}
                      rows={3}
                      maxLength={500}
                      placeholder="Optional operational reason for this lifecycle transition."
                      className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
                    />

                    <CharacterCount
                      current={statusForm.changeReason.length}
                      maximum={500}
                    />
                  </label>
                ) : null}

                {/* ===================================================
                    ADMIN NOTES
                    =================================================== */}

                {statusForm.status ? (
                  <label className="block">
                    <span className="text-sm font-bold text-slate-700">
                      Administrator notes
                    </span>

                    <textarea
                      name="adminNotes"
                      value={statusForm.adminNotes}
                      onChange={handleTextChange}
                      rows={5}
                      maxLength={10000}
                      placeholder="Internal notes visible only to administrators."
                      className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
                    />

                    <CharacterCount
                      current={statusForm.adminNotes.length}
                      maximum={10000}
                    />
                  </label>
                ) : null}

                <button
                  type="submit"
                  disabled={isUpdating || !statusForm.status}
                  className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 py-2 font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isUpdating ? (
                    <LoaderCircle className="h-5 w-5 animate-spin" />
                  ) : (
                    <Save className="h-5 w-5" />
                  )}

                  {isUpdating ? "Updating booking..." : "Update Booking"}
                </button>
              </form>
            )}
          </DetailCard>

          {/* =========================================================
              ASSIGNMENT
              ========================================================= */}

          <DetailCard title="Assignment" icon={<UserRoundCog />}>
            <DetailRow
              label="Assigned administrator"
              value={booking.assignedAdminUserId || "Unassigned"}
            />

            <DetailRow
              label="Created by"
              value={
                booking.createdByAdminName ||
                (booking.bookingSource === "WEBSITE"
                  ? "Customer website"
                  : "Administrator")
              }
            />

            <DetailRow
              label="Last updated by"
              value={booking.updatedByAdminUserId || "Not recorded"}
            />

            <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-xs leading-5 text-slate-600">
                Administrator assignment will use a proper user selector when
                the admin-user lookup is connected. Raw UUID entry is not
                exposed.
              </p>
            </div>
          </DetailCard>

          {/* =========================================================
              RECORD
              ========================================================= */}

          <DetailCard title="Request Record" icon={<Clock3 />}>
            <DetailRow label="Status" value={formatLabel(booking.status)} />

            <DetailRow
              label="Booking source"
              value={formatLabel(booking.bookingSource)}
            />

            <DetailRow
              label="Submitted"
              value={formatDateTime(booking.submittedAt)}
            />

            <DetailRow
              label="Created"
              value={formatDateTime(booking.createdAt)}
            />

            <DetailRow
              label="Last updated"
              value={formatDateTime(booking.updatedAt)}
            />

            <DetailRow label="Row version" value={String(booking.rowVersion)} />
          </DetailCard>

          {/* =========================================================
              CONSENT
              ========================================================= */}

          <DetailCard title="Consent" icon={<ShieldCheck />}>
            <DetailRow
              label="Consent accepted"
              value={
                booking.consentAccepted
                  ? "Yes"
                  : booking.bookingSource === "WEBSITE"
                    ? "No"
                    : "Not applicable"
              }
            />

            <DetailRow
              label="Consent time"
              value={
                booking.consentAcceptedAt
                  ? formatDateTime(booking.consentAcceptedAt)
                  : "Not recorded"
              }
            />

            <DetailRow
              label="Consent version"
              value={booking.consentVersion || "Not recorded"}
            />
          </DetailCard>
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// STATUS-SPECIFIC COMPONENTS
// =====================================================================

function ConfirmedScheduleFields({
  form,
  onChange,
}: {
  form: StatusUpdateFormState;
  onChange: TextChangeHandler;
}) {
  return (
    <div className="space-y-4 rounded-xl border border-brand-200 bg-brand-50 p-4">
      <div className="flex items-start gap-3">
        <CalendarCheck2 className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

        <div>
          <p className="font-bold text-brand-950">Confirmed appointment</p>

          <p className="mt-1 text-xs leading-5 text-brand-900">
            A confirmed booking requires a start time, end time, and timezone.
          </p>
        </div>
      </div>

      <label className="block">
        <span className="text-sm font-bold text-slate-700">
          Appointment starts
          <span className="text-red-600"> *</span>
        </span>

        <input
          type="datetime-local"
          name="scheduledStartAt"
          value={form.scheduledStartAt}
          onChange={onChange}
          required
          className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2"
        />
      </label>

      <label className="block">
        <span className="text-sm font-bold text-slate-700">
          Appointment ends
          <span className="text-red-600"> *</span>
        </span>

        <input
          type="datetime-local"
          name="scheduledEndAt"
          value={form.scheduledEndAt}
          onChange={onChange}
          required
          className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2"
        />
      </label>

      <label className="block">
        <span className="text-sm font-bold text-slate-700">
          Appointment timezone
          <span className="text-red-600"> *</span>
        </span>

        <input
          type="text"
          name="scheduledTimezone"
          value={form.scheduledTimezone}
          readOnly
          maxLength={80}
          className="mt-2 min-h-11 w-full rounded-xl border border-slate-200 bg-slate-100 px-3 py-2 text-slate-700"
        />

        <p className="mt-2 text-xs leading-5 text-slate-500">
          Appointment date and time are interpreted using the administrator
          browser's current timezone.
        </p>
      </label>
    </div>
  );
}

function CompletionFields({
  form,
  onChange,
  onReviewEligibleChange,
}: {
  form: StatusUpdateFormState;
  onChange: TextChangeHandler;
  onReviewEligibleChange: (value: boolean) => void;
}) {
  return (
    <div className="space-y-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
      <div className="flex items-start gap-3">
        <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

        <div>
          <p className="font-bold text-emerald-950">Complete service</p>

          <p className="mt-1 text-xs leading-5 text-emerald-900">
            Record what was completed before closing the booking.
          </p>
        </div>
      </div>

      <label className="block">
        <span className="text-sm font-bold text-slate-700">
          Completion summary
          <span className="text-red-600"> *</span>
        </span>

        <textarea
          name="completionSummary"
          value={form.completionSummary}
          onChange={onChange}
          rows={5}
          maxLength={10000}
          required
          placeholder="Summarize the service completed, work performed, and final outcome."
          className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
        />

        <CharacterCount
          current={form.completionSummary.length}
          maximum={10000}
        />
      </label>

      <label className="block">
        <span className="text-sm font-bold text-slate-700">
          Completion notes
        </span>

        <textarea
          name="completionNotes"
          value={form.completionNotes}
          onChange={onChange}
          rows={5}
          maxLength={20000}
          placeholder="Optional technical notes, recommendations, parts used, follow-up information, or internal details."
          className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
        />

        <CharacterCount current={form.completionNotes.length} maximum={20000} />
      </label>

      <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <input
          type="checkbox"
          checked={form.reviewEligible}
          onChange={(event) => onReviewEligibleChange(event.target.checked)}
          className="mt-1 h-4 w-4"
        />

        <span>
          <span className="block text-sm font-bold text-slate-800">
            Customer is eligible for a service review
          </span>

          <span className="mt-1 block text-xs leading-5 text-slate-500">
            Disable this only when there is an operational reason the customer
            should not receive a review request.
          </span>
        </span>
      </label>

      {!form.reviewEligible ? (
        <label className="block">
          <span className="text-sm font-bold text-slate-700">
            Review eligibility notes
            <span className="text-red-600"> *</span>
          </span>

          <textarea
            name="reviewEligibilityNotes"
            value={form.reviewEligibilityNotes}
            onChange={onChange}
            rows={3}
            maxLength={500}
            required
            placeholder="Explain why this booking should not receive a review request."
            className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3"
          />

          <CharacterCount
            current={form.reviewEligibilityNotes.length}
            maximum={500}
          />
        </label>
      ) : null}
    </div>
  );
}

function StatusReasonField({
  label,
  name,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  name: "cancellationReason" | "declineReason" | "expirationReason";
  value: string;
  onChange: TextChangeHandler;
  placeholder: string;
}) {
  return (
    <label className="block rounded-xl border border-amber-200 bg-amber-50 p-4">
      <span className="text-sm font-bold text-amber-950">
        {label}
        <span className="text-red-600"> *</span>
      </span>

      <textarea
        name={name}
        value={value}
        onChange={onChange}
        rows={4}
        maxLength={500}
        required
        placeholder={placeholder}
        className="focus-ring mt-2 w-full rounded-xl border border-amber-300 bg-white px-3 py-3"
      />

      <CharacterCount current={value.length} maximum={500} />
    </label>
  );
}

// =====================================================================
// LIFECYCLE DISPLAY
// =====================================================================

function LifecycleOutcome({ booking }: { booking: AdminBookingRequest }) {
  const hasOutcome =
    booking.completedAt ||
    booking.cancelledAt ||
    booking.declinedAt ||
    booking.expiredAt ||
    booking.completionSummary ||
    booking.cancellationReason ||
    booking.declineReason ||
    booking.expirationReason;

  if (!hasOutcome) {
    return (
      <EmptyState>
        This booking has no terminal lifecycle outcome yet.
      </EmptyState>
    );
  }

  return (
    <>
      {booking.completedAt ? (
        <>
          <DetailRow
            label="Completed"
            value={formatDateTime(booking.completedAt)}
          />

          <DetailRow
            label="Review eligible"
            value={booking.reviewEligible ? "Yes" : "No"}
          />

          <LongText
            label="Completion summary"
            value={booking.completionSummary || "Not recorded"}
          />

          {booking.completionNotes ? (
            <LongText
              label="Completion notes"
              value={booking.completionNotes}
            />
          ) : null}

          {!booking.reviewEligible && booking.reviewEligibilityNotes ? (
            <LongText
              label="Review eligibility notes"
              value={booking.reviewEligibilityNotes}
            />
          ) : null}
        </>
      ) : null}

      {booking.cancelledAt ? (
        <>
          <DetailRow
            label="Cancelled"
            value={formatDateTime(booking.cancelledAt)}
          />

          <LongText
            label="Cancellation reason"
            value={booking.cancellationReason || "Not recorded"}
          />
        </>
      ) : null}

      {booking.declinedAt ? (
        <>
          <DetailRow
            label="Declined"
            value={formatDateTime(booking.declinedAt)}
          />

          <LongText
            label="Decline reason"
            value={booking.declineReason || "Not recorded"}
          />
        </>
      ) : null}

      {booking.expiredAt ? (
        <>
          <DetailRow
            label="Expired"
            value={formatDateTime(booking.expiredAt)}
          />

          <LongText
            label="Expiration reason"
            value={booking.expirationReason || "Not recorded"}
          />
        </>
      ) : null}
    </>
  );
}

// =====================================================================
// TERMINAL STATUS
// =====================================================================

function TerminalStatusCard({ status }: { status: BookingRequestStatus }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-5">
      <div className="flex items-start gap-3">
        <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-slate-500" />

        <div>
          <p className="font-bold text-slate-900">Booking lifecycle complete</p>

          <p className="mt-1 text-sm leading-6 text-slate-600">
            This booking is <strong>{formatLabel(status)}</strong>. The backend
            treats this as a terminal lifecycle status, so no additional status
            transition is available.
          </p>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
// PAYLOAD
// =====================================================================

function createStatusUpdatePayload(
  booking: AdminBookingRequest,
  form: StatusUpdateFormState,
): AdminBookingStatusUpdatePayload {
  if (!form.status) {
    throw new Error("New booking status is required.");
  }

  const status = form.status;

  return {
    status,

    /*
     * Null means "do not change assignment" in the current backend
     * implementation.
     */
    assignedAdminUserId: null,

    /*
     * A confirmation requires the newly entered confirmed schedule.
     *
     * For completion, an existing confirmed schedule already exists on
     * the booking and therefore does not need to be replaced.
     */
    scheduledStartAt:
      status === "CONFIRMED"
        ? localDateTimeToInstant(form.scheduledStartAt)
        : null,

    scheduledEndAt:
      status === "CONFIRMED"
        ? localDateTimeToInstant(form.scheduledEndAt)
        : null,

    scheduledTimezone:
      status === "CONFIRMED" ? toNullable(form.scheduledTimezone) : null,

    cancellationReason:
      status === "CANCELLED" ? toNullable(form.cancellationReason) : null,

    declineReason:
      status === "DECLINED" ? toNullable(form.declineReason) : null,

    expirationReason:
      status === "EXPIRED" ? toNullable(form.expirationReason) : null,

    completionSummary:
      status === "COMPLETED" ? toNullable(form.completionSummary) : null,

    completionNotes:
      status === "COMPLETED" ? toNullable(form.completionNotes) : null,

    reviewEligible: status === "COMPLETED" ? form.reviewEligible : null,

    reviewEligibilityNotes:
      status === "COMPLETED" && !form.reviewEligible
        ? toNullable(form.reviewEligibilityNotes)
        : null,

    adminNotes: toNullable(form.adminNotes),

    changeReason: resolveChangeReason(status, form),
  };
}

// =====================================================================
// STATUS FORM CREATION
// =====================================================================

function createStatusForm(booking: AdminBookingRequest): StatusUpdateFormState {
  return {
    status: "",

    scheduledStartAt: booking.scheduledStartAt
      ? instantToLocalInput(booking.scheduledStartAt)
      : "",

    scheduledEndAt: booking.scheduledEndAt
      ? instantToLocalInput(booking.scheduledEndAt)
      : "",

    scheduledTimezone: booking.scheduledTimezone || getBrowserTimezone(),

    cancellationReason: booking.cancellationReason || "",

    declineReason: booking.declineReason || "",

    expirationReason: booking.expirationReason || "",

    completionSummary: booking.completionSummary || "",

    completionNotes: booking.completionNotes || "",

    reviewEligible: booking.reviewEligible,

    reviewEligibilityNotes: booking.reviewEligibilityNotes || "",

    adminNotes: booking.adminNotes || "",

    changeReason: "",
  };
}

// =====================================================================
// VALIDATION
// =====================================================================

function validateStatusUpdate(
  booking: AdminBookingRequest,
  form: StatusUpdateFormState,
): string | null {
  if (!form.status) {
    return "Select the new booking status.";
  }

  const allowedStatuses = getAllowedStatusTransitions(booking.status);

  if (!allowedStatuses.includes(form.status)) {
    return `Booking status cannot change from ${formatLabel(
      booking.status,
    )} to ${formatLabel(form.status)}.`;
  }

  // ===================================================================
  // CONFIRMED
  // ===================================================================

  if (form.status === "CONFIRMED") {
    if (!form.scheduledStartAt) {
      return "Enter the confirmed appointment start time.";
    }

    if (!form.scheduledEndAt) {
      return "Enter the confirmed appointment end time.";
    }

    if (!form.scheduledTimezone.trim()) {
      return "Appointment timezone is required.";
    }

    const start = new Date(form.scheduledStartAt);

    const end = new Date(form.scheduledEndAt);

    if (Number.isNaN(start.getTime())) {
      return "Enter a valid appointment start time.";
    }

    if (Number.isNaN(end.getTime())) {
      return "Enter a valid appointment end time.";
    }

    if (end.getTime() <= start.getTime()) {
      return "Appointment end time must be after the start time.";
    }
  }

  // ===================================================================
  // COMPLETED
  // ===================================================================

  if (form.status === "COMPLETED") {
    if (
      !booking.scheduledStartAt ||
      !booking.scheduledEndAt ||
      !booking.scheduledTimezone
    ) {
      return "This booking does not have a complete confirmed appointment schedule and cannot be completed.";
    }

    if (!form.completionSummary.trim()) {
      return "Completion summary is required when completing a booking.";
    }

    if (form.completionSummary.trim().length > 10000) {
      return "Completion summary cannot exceed 10,000 characters.";
    }

    if (form.completionNotes.trim().length > 20000) {
      return "Completion notes cannot exceed 20,000 characters.";
    }

    if (!form.reviewEligible && !form.reviewEligibilityNotes.trim()) {
      return "Review eligibility notes are required when the booking is not review eligible.";
    }
  }

  // ===================================================================
  // CANCELLED
  // ===================================================================

  if (form.status === "CANCELLED" && !form.cancellationReason.trim()) {
    return "Cancellation reason is required when cancelling a booking.";
  }

  // ===================================================================
  // DECLINED
  // ===================================================================

  if (form.status === "DECLINED" && !form.declineReason.trim()) {
    return "Decline reason is required when declining a booking.";
  }

  // ===================================================================
  // EXPIRED
  // ===================================================================

  if (form.status === "EXPIRED" && !form.expirationReason.trim()) {
    return "Expiration reason is required when expiring a booking.";
  }

  if (form.changeReason.trim().length > 500) {
    return "Status-change reason cannot exceed 500 characters.";
  }

  if (form.adminNotes.trim().length > 10000) {
    return "Administrator notes cannot exceed 10,000 characters.";
  }

  return null;
}

// =====================================================================
// STATUS TRANSITIONS
// =====================================================================

function getAllowedStatusTransitions(
  status: BookingRequestStatus,
): BookingRequestStatus[] {
  switch (status) {
    case "PENDING":
      return ["UNDER_REVIEW", "CONFIRMED", "CANCELLED", "DECLINED", "EXPIRED"];

    case "UNDER_REVIEW":
      return ["CONFIRMED", "CANCELLED", "DECLINED", "EXPIRED"];

    case "CONFIRMED":
      return ["COMPLETED", "CANCELLED"];

    case "COMPLETED":
    case "CANCELLED":
    case "DECLINED":
    case "EXPIRED":
      return [];
  }
}

function isTerminalStatus(status: BookingRequestStatus): boolean {
  return (
    status === "COMPLETED" ||
    status === "CANCELLED" ||
    status === "DECLINED" ||
    status === "EXPIRED"
  );
}

// =====================================================================
// CHANGE REASON
// =====================================================================

function resolveChangeReason(
  status: BookingRequestStatus,
  form: StatusUpdateFormState,
): string | null {
  const explicitReason = toNullable(form.changeReason);

  if (explicitReason) {
    return explicitReason;
  }

  switch (status) {
    case "CANCELLED":
      return toNullable(form.cancellationReason);

    case "DECLINED":
      return toNullable(form.declineReason);

    case "EXPIRED":
      return toNullable(form.expirationReason);

    case "COMPLETED":
      return "Service completed.";

    case "CONFIRMED":
      return "Appointment confirmed.";

    case "UNDER_REVIEW":
      return "Booking moved under review.";

    case "PENDING":
      return null;
  }
}

// =====================================================================
// SHARED DISPLAY COMPONENTS
// =====================================================================

function BackLink() {
  return (
    <Link
      to="/admin/bookings"
      className="focus-ring inline-flex items-center gap-2 rounded-lg font-bold text-slate-600 transition hover:text-brand-700"
    >
      <ArrowLeft className="h-4 w-4" />
      Back to bookings
    </Link>
  );
}

function HeaderBadge({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full bg-white/10 px-3 py-1.5 text-xs font-bold text-slate-100">
      {children}
    </span>
  );
}

function StatusBadge({ status }: { status: BookingRequestStatus }) {
  return (
    <span className="w-fit rounded-full bg-white/10 px-4 py-2 text-sm font-extrabold">
      {formatLabel(status)}
    </span>
  );
}

function ErrorMessage({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
    >
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

      <div>
        <h2 className="font-bold">Booking operation failed</h2>

        <p className="mt-1 text-sm leading-6">{message}</p>
      </div>
    </div>
  );
}

function DetailCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <h2 className="font-display text-xl font-extrabold text-navy-950">
          {title}
        </h2>
      </div>

      <div className="mt-6">{children}</div>
    </article>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 border-b border-slate-100 py-3 first:pt-0 last:border-0 last:pb-0 sm:flex-row sm:justify-between sm:gap-6">
      <dt className="font-semibold text-slate-500">{label}</dt>

      <dd className="break-words font-bold text-slate-800 sm:max-w-[60%] sm:text-right">
        {value}
      </dd>
    </div>
  );
}

function ContactLink({
  icon,
  href,
  value,
}: {
  icon: ReactNode;
  href: string;
  value: string;
}) {
  return (
    <a
      href={href}
      className="focus-ring mt-3 flex items-center gap-3 rounded-xl border border-slate-200 p-4 transition hover:border-brand-400"
    >
      <span className="text-brand-700 [&>svg]:h-5 [&>svg]:w-5">{icon}</span>

      <span className="break-all font-bold text-slate-800">{value}</span>
    </a>
  );
}

function LongText({ label, value }: { label?: string; value: string }) {
  return (
    <div className="mt-5">
      {label ? (
        <p className="text-sm font-bold text-slate-500">{label}</p>
      ) : null}

      <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-800">
        {value}
      </p>
    </div>
  );
}

function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-5 text-sm leading-6 text-slate-600">
      {children}
    </div>
  );
}

function CharacterCount({
  current,
  maximum,
}: {
  current: number;
  maximum: number;
}) {
  return (
    <p className="mt-1 text-right text-xs text-slate-500">
      {current.toLocaleString()}/{maximum.toLocaleString()}
    </p>
  );
}

// =====================================================================
// TYPES
// =====================================================================

type TextChangeHandler = (
  event: ChangeEvent<
    HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  >,
) => void;

// =====================================================================
// ADDRESS
// =====================================================================

function formatAddress(values: Array<string | null>): string {
  return values
    .map((value) => value?.trim() ?? "")
    .filter(Boolean)
    .join(", ");
}

// =====================================================================
// DATE / TIME
// =====================================================================

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "long",
      }).format(date);
}

function formatDateTime(value: string): string {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}

/**
 * Converts the browser-local datetime-local control value into the
 * UTC Instant expected by Spring Boot.
 */
function localDateTimeToInstant(value: string): string | null {
  if (!value.trim()) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return date.toISOString();
}

/**
 * Converts an API Instant into the browser-local format required by a
 * datetime-local input.
 */
function instantToLocalInput(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const year = date.getFullYear();

  const month = String(date.getMonth() + 1).padStart(2, "0");

  const day = String(date.getDate()).padStart(2, "0");

  const hours = String(date.getHours()).padStart(2, "0");

  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function getBrowserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || "America/Chicago";
}

// =====================================================================
// STRING HELPERS
// =====================================================================

function toNullable(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized : null;
}

function formatLabel(value: string): string {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}
