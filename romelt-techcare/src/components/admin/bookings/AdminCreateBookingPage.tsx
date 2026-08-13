/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CREATE CLIENT BOOKING PAGE
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to create a booking on behalf
 * of a customer who contacted Romelt TechCare by phone, email,
 * walk-in, or another supported channel.
 *
 * Six-step workflow:
 * 1. Customer
 * 2. Notifications & Business
 * 3. Service
 * 4. Schedule
 * 5. Location & Notes
 * 6. Review & Submit
 *
 * Responsibilities:
 * - Supports PERSONAL and BUSINESS bookings.
 * - Captures customer/contact-person information.
 * - Supports dedicated notification email and phone destinations.
 * - Captures business snapshot information when applicable.
 * - Captures requested service information.
 * - Captures scheduling preferences.
 * - Requires a complete service address for ON_SITE requests.
 * - Records the administrator booking source.
 * - Supports private administrator notes.
 * - Validates each step before continuing.
 * - Sends the exact AdminBookingRequestCreateRequest contract.
 *
 * Real-data integration:
 * POST /api/v1/admin/booking-requests
 *
 * Existing-customer integration:
 * - Loads the reusable customer directory from GET /api/v1/admin/customers.
 * - Supports keyword search by customer name, number, email, or phone.
 * - Selecting an existing customer loads the complete customer profile.
 * - Customer contact fields are automatically filled from that profile.
 * - The selected customerId is submitted with the booking.
 *
 * serviceId and assignedAdminUserId remain null until their dedicated
 * selectors are connected. Raw UUID entry is intentionally not exposed.
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
  ArrowRight,
  Building2,
  CalendarDays,
  CalendarPlus,
  Check,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  Clock3,
  LoaderCircle,
  MapPin,
  Phone,
  Save,
  Search,
  UserRound,
  Wrench,
} from "lucide-react";

import { Link, useNavigate } from "react-router-dom";

import {
  getAdminCustomer,
  getAdminCustomers,
} from "@/services/admin-customer.service";
import { createAdminBookingRequest } from "@/services/admin-customer-request.service";

import type { AdminCustomerSummary } from "@/types/admin-customer.types";

import type {
  AdminBookingRequestCreatePayload,
  AdminBookingSource,
  BookingFor,
  ContactMethod,
  PreferredServiceTime,
  ServiceMethod,
} from "@/types/admin-customer-request.types";

// =====================================================================
// FORM STATE
// =====================================================================

interface BookingFormState {
  bookingFor: BookingFor;

  fullName: string;
  email: string;
  phone: string;
  preferredContactMethod: ContactMethod;

  notificationEmail: string;
  notificationPhone: string;

  businessName: string;
  businessEmail: string;
  businessPhone: string;
  businessStreetAddress: string;
  businessCity: string;
  businessState: string;
  businessPostalCode: string;
  businessCountryCode: string;
  businessContactRole: string;

  serviceType: string;
  serviceMethod: ServiceMethod;

  preferredDate: string;
  preferredTime: PreferredServiceTime;
  alternateDate: string;

  deviceType: string;
  problemDescription: string;

  streetAddress: string;
  addressLine2: string;
  city: string;
  stateRegion: string;
  postalCode: string;
  countryCode: string;

  bookingSource: AdminBookingSource;

  adminNotes: string;
}

// =====================================================================
// PROGRESS
// =====================================================================

interface ProgressStep {
  number: number;
  title: string;
  description: string;
  icon: ReactNode;
}

const TOTAL_STEPS = 6;

const PROGRESS_STEPS: ProgressStep[] = [
  {
    number: 1,
    title: "Customer",
    description: "Primary contact",
    icon: <UserRound />,
  },
  {
    number: 2,
    title: "Business",
    description: "Business details when applicable",
    icon: <Building2 />,
  },
  {
    number: 3,
    title: "Service",
    description: "Requested support",
    icon: <Wrench />,
  },
  {
    number: 4,
    title: "Schedule",
    description: "Date and time",
    icon: <Clock3 />,
  },
  {
    number: 5,
    title: "Location",
    description: "Address and notes",
    icon: <MapPin />,
  },
  {
    number: 6,
    title: "Review",
    description: "Confirm and submit",
    icon: <ClipboardCheck />,
  },
];

// =====================================================================
// INITIAL FORM
// =====================================================================

const INITIAL_FORM: BookingFormState = {
  bookingFor: "PERSONAL",

  fullName: "",
  email: "",
  phone: "",
  preferredContactMethod: "PHONE",

  notificationEmail: "",
  notificationPhone: "",

  businessName: "",
  businessEmail: "",
  businessPhone: "",
  businessStreetAddress: "",
  businessCity: "",
  businessState: "Iowa",
  businessPostalCode: "",
  businessCountryCode: "US",
  businessContactRole: "",

  serviceType: "",
  serviceMethod: "REMOTE",

  preferredDate: "",
  preferredTime: "FLEXIBLE",
  alternateDate: "",

  deviceType: "",
  problemDescription: "",

  streetAddress: "",
  addressLine2: "",
  city: "",
  stateRegion: "Iowa",
  postalCode: "",
  countryCode: "US",

  bookingSource: "PHONE",

  adminNotes: "",
};

// =====================================================================
// PAGE
// =====================================================================

export default function AdminCreateBookingPage() {
  const navigate = useNavigate();

  const [currentStep, setCurrentStep] = useState(1);

  const [highestCompletedStep, setHighestCompletedStep] = useState(0);

  const [form, setForm] = useState<BookingFormState>(INITIAL_FORM);

  const [customerSearch, setCustomerSearch] = useState("");

  const [customerOptions, setCustomerOptions] = useState<
    AdminCustomerSummary[]
  >([]);

  const [selectedCustomerId, setSelectedCustomerId] = useState("");

  const [selectedCustomerSummary, setSelectedCustomerSummary] =
    useState<AdminCustomerSummary | null>(null);

  const [isLoadingCustomers, setIsLoadingCustomers] = useState(false);

  const [isLoadingSelectedCustomer, setIsLoadingSelectedCustomer] =
    useState(false);

  const [customerSearchError, setCustomerSearchError] = useState<string | null>(
    null,
  );

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const minimumDate = useMemo(() => {
    const tomorrow = new Date();

    tomorrow.setDate(tomorrow.getDate() + 1);

    return formatDateInput(tomorrow);
  }, []);

  const requiresAddress = form.serviceMethod === "ON_SITE";

  const isBusinessBooking = form.bookingFor === "BUSINESS";

  useEffect(() => {
    const controller = new AbortController();

    const timer = window.setTimeout(() => {
      setIsLoadingCustomers(true);
      setCustomerSearchError(null);

      void getAdminCustomers({
        keyword: customerSearch.trim() || undefined,
        customerStatus: "ACTIVE",
        page: 0,
        size: 20,
        signal: controller.signal,
      })
        .then((response) => {
          setCustomerOptions(response.content ?? []);
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) {
            return;
          }

          setCustomerOptions([]);
          setCustomerSearchError(
            error instanceof Error
              ? error.message
              : "Customers could not be loaded.",
          );
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setIsLoadingCustomers(false);
          }
        });
    }, 300);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [customerSearch]);

  async function handleCustomerSelection(customerId: string) {
    setSelectedCustomerId(customerId);
    setCustomerSearchError(null);
    setErrorMessage(null);

    if (!customerId) {
      setSelectedCustomerSummary(null);

      setForm((current) => ({
        ...current,
        fullName: "",
        email: "",
        phone: "",
        preferredContactMethod: "PHONE",
        notificationEmail: "",
        notificationPhone: "",
        streetAddress: "",
        addressLine2: "",
        city: "",
        stateRegion: "Iowa",
        postalCode: "",
        countryCode: "US",
      }));

      return;
    }

    const summary =
      customerOptions.find((customer) => customer.customerId === customerId) ??
      null;

    setSelectedCustomerSummary(summary);
    setIsLoadingSelectedCustomer(true);

    try {
      const customer = await getAdminCustomer(customerId);

      setSelectedCustomerSummary(
        (current) =>
          current ?? {
            customerId: customer.customerId,
            customerNumber: customer.customerNumber,
            displayName: customer.displayName,
            preferredName: customer.preferredName,
            primaryEmail: customer.primaryEmail,
            primaryPhone: customer.primaryPhone,
            preferredContactMethod: customer.preferredContactMethod,
            customerStatus: customer.customerStatus,
            customerSource: customer.customerSource,
            lastBookingAt: customer.lastBookingAt,
            lastServiceCompletedAt: customer.lastServiceCompletedAt,
            lastActivityAt: customer.lastActivityAt,
          },
      );

      setForm((current) => ({
        ...current,
        fullName: customer.displayName ?? "",
        email: customer.primaryEmail ?? "",
        phone: customer.primaryPhone ?? "",
        preferredContactMethod:
          customer.preferredContactMethod ?? current.preferredContactMethod,
        notificationEmail: "",
        notificationPhone: "",
        streetAddress: customer.streetAddress ?? "",
        addressLine2: customer.addressLine2 ?? "",
        city: customer.city ?? "",
        stateRegion: customer.stateRegion ?? "Iowa",
        postalCode: customer.postalCode ?? "",
        countryCode: customer.countryCode ?? "US",
      }));
    } catch (error) {
      setSelectedCustomerId("");
      setSelectedCustomerSummary(null);

      setCustomerSearchError(
        error instanceof Error
          ? error.message
          : "The selected customer could not be loaded.",
      );
    } finally {
      setIsLoadingSelectedCustomer(false);
    }
  }

  function handleChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) {
    const { name, value } = event.target;

    setForm((current) => {
      const next = {
        ...current,
        [name]: value,
      };

      /*
       * Personal bookings must not send stale business information
       * left behind after switching from BUSINESS to PERSONAL.
       */
      if (name === "bookingFor" && value === "PERSONAL") {
        return {
          ...next,

          businessName: "",
          businessEmail: "",
          businessPhone: "",
          businessStreetAddress: "",
          businessCity: "",
          businessState: "Iowa",
          businessPostalCode: "",
          businessCountryCode: "US",
          businessContactRole: "",
        };
      }

      return next;
    });

    setErrorMessage(null);
  }

  function handleNextStep() {
    const validationMessage = validateStep(currentStep, form);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      scrollToTop();

      return;
    }

    setErrorMessage(null);

    setHighestCompletedStep((current) => Math.max(current, currentStep));

    setCurrentStep((step) => Math.min(step + 1, TOTAL_STEPS));

    scrollToTop();
  }

  function handlePreviousStep() {
    setErrorMessage(null);

    setCurrentStep((step) => Math.max(step - 1, 1));

    scrollToTop();
  }

  function handleStepSelection(stepNumber: number) {
    if (stepNumber === currentStep) {
      return;
    }

    if (stepNumber > highestCompletedStep + 1) {
      return;
    }

    if (stepNumber > currentStep) {
      const validationMessage = validateStep(currentStep, form);

      if (validationMessage) {
        setErrorMessage(validationMessage);

        scrollToTop();

        return;
      }

      setHighestCompletedStep((current) => Math.max(current, currentStep));
    }

    setErrorMessage(null);

    setCurrentStep(stepNumber);

    scrollToTop();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    if (currentStep < TOTAL_STEPS) {
      handleNextStep();

      return;
    }

    setErrorMessage(null);

    const validationMessage = validateEntireForm(form);

    if (validationMessage) {
      setErrorMessage(validationMessage);

      scrollToTop();

      return;
    }

    const isBusiness = form.bookingFor === "BUSINESS";

    const payload: AdminBookingRequestCreatePayload = {
      customerId: selectedCustomerId || null,

      bookingFor: form.bookingFor,

      fullName: form.fullName.trim(),

      email: form.email.trim().toLowerCase(),

      phone: form.phone.trim(),

      preferredContactMethod: form.preferredContactMethod,

      notificationEmail: toNullableLowercase(form.notificationEmail),

      notificationPhone: toNullable(form.notificationPhone),

      // ============================================================
      // BUSINESS SNAPSHOT
      // ============================================================

      businessName: isBusiness ? toNullable(form.businessName) : null,

      businessEmail: isBusiness
        ? toNullableLowercase(form.businessEmail)
        : null,

      businessPhone: isBusiness ? toNullable(form.businessPhone) : null,

      businessStreetAddress: isBusiness
        ? toNullable(form.businessStreetAddress)
        : null,

      businessCity: isBusiness ? toNullable(form.businessCity) : null,

      businessState: isBusiness ? toNullable(form.businessState) : null,

      businessPostalCode: isBusiness
        ? toNullable(form.businessPostalCode)
        : null,

      businessCountryCode: isBusiness
        ? toNullableUppercase(form.businessCountryCode)
        : null,

      businessContactRole: isBusiness
        ? toNullable(form.businessContactRole)
        : null,

      /*
       * A service selector will populate serviceId after the
       * administrator service-catalog API is connected.
       */
      serviceId: null,

      serviceType: form.serviceType.trim(),

      serviceMethod: form.serviceMethod,

      preferredDate: form.preferredDate,

      preferredTime: form.preferredTime,

      alternateDate: toNullable(form.alternateDate),

      deviceType: toNullable(form.deviceType),

      problemDescription: form.problemDescription.trim(),

      streetAddress: toNullable(form.streetAddress),

      addressLine2: toNullable(form.addressLine2),

      city: toNullable(form.city),

      stateRegion: toNullable(form.stateRegion),

      postalCode: toNullable(form.postalCode),

      countryCode: toNullableUppercase(form.countryCode),

      bookingSource: form.bookingSource,

      /*
       * Assignment will be selected through an administrator picker
       * when that API is connected.
       */
      assignedAdminUserId: null,

      adminNotes: toNullable(form.adminNotes),
    };

    setIsSubmitting(true);

    try {
      const createdBooking = await createAdminBookingRequest(payload);

      navigate(`/admin/bookings/${createdBooking.bookingRequestId}`, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "The client booking could not be created.",
      );

      scrollToTop();
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto w-full max-w-6xl space-y-5">
      <Link
        to="/admin/bookings"
        className="focus-ring inline-flex items-center gap-2 rounded-lg font-bold text-slate-600 transition hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to bookings
      </Link>

      <header>
        <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-brand-700">
          Administrator booking
        </p>

        <h1 className="mt-2 font-display text-3xl font-black text-navy-950">
          Book Service for a Client
        </h1>

        <p className="mt-2 max-w-3xl leading-7 text-slate-600">
          Create a booking for a customer who contacted Romelt TechCare
          directly. The customer will receive booking notifications using the
          contact information recorded here.
        </p>
      </header>

      <ProgressIndicator
        currentStep={currentStep}
        highestCompletedStep={highestCompletedStep}
        onStepSelection={handleStepSelection}
      />

      {errorMessage ? (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-900"
        >
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />

          <div>
            <p className="font-bold">
              {currentStep === TOTAL_STEPS
                ? "Booking could not be created"
                : "Complete this step"}
            </p>

            <p className="mt-1 text-sm">{errorMessage}</p>
          </div>
        </div>
      ) : null}

      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {currentStep === 1 ? (
          <CustomerStep
            form={form}
            onChange={handleChange}
            customerSearch={customerSearch}
            onCustomerSearchChange={setCustomerSearch}
            customerOptions={customerOptions}
            selectedCustomerId={selectedCustomerId}
            selectedCustomerSummary={selectedCustomerSummary}
            isLoadingCustomers={isLoadingCustomers}
            isLoadingSelectedCustomer={isLoadingSelectedCustomer}
            customerSearchError={customerSearchError}
            onCustomerSelection={handleCustomerSelection}
          />
        ) : null}

        {currentStep === 2 ? (
          <NotificationsAndBusinessStep form={form} onChange={handleChange} />
        ) : null}

        {currentStep === 3 ? (
          <ServiceStep form={form} onChange={handleChange} />
        ) : null}

        {currentStep === 4 ? (
          <ScheduleStep
            form={form}
            minimumDate={minimumDate}
            onChange={handleChange}
          />
        ) : null}

        {currentStep === 5 ? (
          <LocationAndNotesStep
            form={form}
            requiresAddress={requiresAddress}
            onChange={handleChange}
          />
        ) : null}

        {currentStep === 6 ? (
          <ReviewStep
            form={form}
            requiresAddress={requiresAddress}
            selectedCustomerSummary={selectedCustomerSummary}
          />
        ) : null}

        <FormNavigation
          currentStep={currentStep}
          isSubmitting={isSubmitting}
          onPrevious={handlePreviousStep}
        />
      </form>

      <div className="rounded-2xl border border-brand-200 bg-brand-50 p-5">
        <div className="flex items-start gap-3">
          <CalendarPlus className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />

          <p className="text-sm leading-6 text-brand-950">
            Creating this record means Romelt TechCare received and recorded the
            customer's service request. The booking begins in the pending
            lifecycle state and is not a confirmed appointment yet.
          </p>
        </div>
      </div>
    </section>
  );
}

// =====================================================================
// STEP 1 — CUSTOMER
// =====================================================================

function CustomerStep({
  form,
  onChange,
  customerSearch,
  onCustomerSearchChange,
  customerOptions,
  selectedCustomerId,
  selectedCustomerSummary,
  isLoadingCustomers,
  isLoadingSelectedCustomer,
  customerSearchError,
  onCustomerSelection,
}: {
  form: BookingFormState;
  onChange: ChangeEventHandler;
  customerSearch: string;
  onCustomerSearchChange: (value: string) => void;
  customerOptions: AdminCustomerSummary[];
  selectedCustomerId: string;
  selectedCustomerSummary: AdminCustomerSummary | null;
  isLoadingCustomers: boolean;
  isLoadingSelectedCustomer: boolean;
  customerSearchError: string | null;
  onCustomerSelection: (customerId: string) => Promise<void>;
}) {
  const visibleCustomers = selectedCustomerSummary
    ? [
        selectedCustomerSummary,
        ...customerOptions.filter(
          (customer) =>
            customer.customerId !== selectedCustomerSummary.customerId,
        ),
      ]
    : customerOptions;

  const hasExistingCustomer = Boolean(selectedCustomerId);

  return (
    <div className="space-y-5">
      <FormSection
        stepNumber={1}
        title="Customer Information"
        description="Select an existing customer to fill their saved information automatically, or leave the customer selection empty to enter a new customer manually."
        icon={<UserRound />}
      >
        <div className="rounded-2xl border border-brand-200 bg-brand-50/50 p-4">
          <div className="flex items-start gap-3">
            <div className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-brand-700 shadow-sm">
              <Search className="h-5 w-5" />
            </div>

            <div className="min-w-0 flex-1">
              <p className="font-bold text-navy-950">Existing customer</p>

              <p className="mt-1 text-sm leading-6 text-slate-600">
                Search by customer name, customer number, email, or telephone
                number. Selecting a customer fills the saved contact and address
                information below.
              </p>
            </div>
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            <label className="block">
              <span className="text-sm font-bold text-slate-700">
                Search customers
              </span>

              <div className="relative mt-2">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />

                <input
                  type="search"
                  value={customerSearch}
                  onChange={(event) =>
                    onCustomerSearchChange(event.target.value)
                  }
                  placeholder="Name, customer number, email, or phone"
                  autoComplete="off"
                  className="focus-ring min-h-11 w-full rounded-xl border border-slate-300 bg-white py-2 pl-10 pr-10 text-slate-900 placeholder:text-slate-400"
                />

                {isLoadingCustomers ? (
                  <LoaderCircle className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-brand-700" />
                ) : null}
              </div>
            </label>

            <label className="block">
              <span className="text-sm font-bold text-slate-700">Customer</span>

              <select
                value={selectedCustomerId}
                onChange={(event) =>
                  void onCustomerSelection(event.target.value)
                }
                disabled={isLoadingSelectedCustomer}
                className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-900 disabled:cursor-wait disabled:bg-slate-100"
              >
                <option value="">New / unlisted customer</option>

                {visibleCustomers.map((customer) => (
                  <option key={customer.customerId} value={customer.customerId}>
                    {formatCustomerOption(customer)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {isLoadingSelectedCustomer ? (
            <div className="mt-3 flex items-center gap-2 text-sm font-semibold text-brand-800">
              <LoaderCircle className="h-4 w-4 animate-spin" />
              Loading customer profile...
            </div>
          ) : null}

          {customerSearchError ? (
            <div className="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-800">
              {customerSearchError}
            </div>
          ) : null}

          {!isLoadingCustomers &&
          !customerSearchError &&
          customerSearch.trim() &&
          customerOptions.length === 0 ? (
            <p className="mt-3 text-sm font-semibold text-slate-500">
              No active customers matched this search. Choose New / unlisted
              customer to enter the information manually.
            </p>
          ) : null}

          {selectedCustomerSummary ? (
            <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
              <div className="flex items-start gap-3">
                <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

                <div>
                  <p className="font-bold text-emerald-950">
                    {selectedCustomerSummary.displayName}
                  </p>

                  <p className="mt-1 text-sm text-emerald-900">
                    {selectedCustomerSummary.customerNumber}
                    {selectedCustomerSummary.primaryEmail
                      ? ` • ${selectedCustomerSummary.primaryEmail}`
                      : ""}
                    {selectedCustomerSummary.primaryPhone
                      ? ` • ${selectedCustomerSummary.primaryPhone}`
                      : ""}
                  </p>

                  <p className="mt-2 text-xs font-semibold leading-5 text-emerald-800">
                    Contact fields below are filled from the reusable customer
                    profile. To change the customer's permanent contact details,
                    update the customer profile rather than this booking.
                  </p>
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <FormGrid>
          <SelectField
            label="Booking for"
            name="bookingFor"
            value={form.bookingFor}
            onChange={onChange}
            options={[
              ["PERSONAL", "Personal"],
              ["BUSINESS", "Business"],
            ]}
          />

          <SelectField
            label="Booking source"
            name="bookingSource"
            value={form.bookingSource}
            onChange={onChange}
            options={[
              ["PHONE", "Telephone"],
              ["EMAIL", "Email"],
              ["WALK_IN", "Walk-in"],
              ["OTHER", "Other"],
            ]}
          />

          <TextField
            label="Full name"
            name="fullName"
            value={form.fullName}
            onChange={onChange}
            maxLength={120}
            autoComplete="name"
            disabled={hasExistingCustomer}
            required
          />

          <TextField
            label="Email address"
            name="email"
            type="email"
            value={form.email}
            onChange={onChange}
            maxLength={254}
            autoComplete="email"
            disabled={hasExistingCustomer}
            required
          />

          <TextField
            label="Telephone number"
            name="phone"
            type="tel"
            value={form.phone}
            onChange={onChange}
            maxLength={40}
            autoComplete="tel"
            disabled={hasExistingCustomer}
            required
          />

          <SelectField
            label="Preferred contact method"
            name="preferredContactMethod"
            value={form.preferredContactMethod}
            onChange={onChange}
            options={[
              ["PHONE", "Phone"],
              ["TEXT", "Text message"],
              ["EMAIL", "Email"],
            ]}
          />
        </FormGrid>
      </FormSection>

      <div className="grid gap-4 lg:grid-cols-3">
        <QuickInfoCard
          icon={<UserRound />}
          title="Existing or new"
          description="Select a saved customer to avoid retyping, or leave the selector on New / unlisted customer."
        />

        <QuickInfoCard
          icon={<Phone />}
          title="Current customer data"
          description="Existing-customer contact fields come from the reusable customer profile."
        />

        <QuickInfoCard
          icon={<Building2 />}
          title="Business support"
          description="Business-specific fields remain booking snapshots and appear in the next step."
        />
      </div>
    </div>
  );
}

// =====================================================================
// STEP 2 — BUSINESS
// =====================================================================

function NotificationsAndBusinessStep({
  form,
  onChange,
}: {
  form: BookingFormState;
  onChange: ChangeEventHandler;
}) {
  const isBusiness = form.bookingFor === "BUSINESS";

  return (
    <div className="space-y-5">
      {isBusiness ? (
        <FormSection
          stepNumber={2}
          title="Business Information"
          description="Business bookings require the organization's contact and address information."
          icon={<Building2 />}
        >
          <FormGrid>
            <TextField
              label="Business name"
              name="businessName"
              value={form.businessName}
              onChange={onChange}
              maxLength={180}
              required
            />

            <TextField
              label="Business contact role"
              name="businessContactRole"
              value={form.businessContactRole}
              onChange={onChange}
              placeholder="Owner, Manager, Office Administrator..."
              maxLength={120}
            />

            <TextField
              label="Business email"
              name="businessEmail"
              type="email"
              value={form.businessEmail}
              onChange={onChange}
              maxLength={254}
              required
            />

            <TextField
              label="Business phone"
              name="businessPhone"
              type="tel"
              value={form.businessPhone}
              onChange={onChange}
              maxLength={40}
              required
            />

            <TextField
              label="Business street address"
              name="businessStreetAddress"
              value={form.businessStreetAddress}
              onChange={onChange}
              maxLength={180}
              required
            />

            <TextField
              label="Business city"
              name="businessCity"
              value={form.businessCity}
              onChange={onChange}
              maxLength={100}
              required
            />

            <TextField
              label="Business state / region"
              name="businessState"
              value={form.businessState}
              onChange={onChange}
              maxLength={100}
              required
            />

            <TextField
              label="Business postal code"
              name="businessPostalCode"
              value={form.businessPostalCode}
              onChange={onChange}
              maxLength={30}
              required
            />

            <TextField
              label="Business country code"
              name="businessCountryCode"
              value={form.businessCountryCode}
              onChange={onChange}
              maxLength={2}
              placeholder="US"
              required
            />
          </FormGrid>
        </FormSection>
      ) : (
        <FormSection
          stepNumber={2}
          title="Business Information"
          description="This booking is marked as Personal, so business fields are not required."
          icon={<Building2 />}
        >
          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-5 text-sm leading-6 text-slate-600">
            Switch Booking for to Business on Step 1 if this service request
            should be attached to a business snapshot.
          </div>
        </FormSection>
      )}
    </div>
  );
}

// =====================================================================
// STEP 3 — SERVICE
// =====================================================================

function ServiceStep({
  form,
  onChange,
}: {
  form: BookingFormState;
  onChange: ChangeEventHandler;
}) {
  return (
    <FormSection
      stepNumber={3}
      title="Service Request"
      description="Record the requested service and enough technical information for the request to be reviewed."
      icon={<Wrench />}
    >
      <FormGrid>
        <TextField
          label="Service type"
          name="serviceType"
          value={form.serviceType}
          onChange={onChange}
          placeholder="Example: Computer repair"
          maxLength={120}
          required
        />

        <SelectField
          label="Service method"
          name="serviceMethod"
          value={form.serviceMethod}
          onChange={onChange}
          options={[
            ["REMOTE", "Remote support"],
            ["ON_SITE", "On-site service"],
            ["DROP_OFF", "Device drop-off"],
            ["NOT_SURE", "Not sure"],
          ]}
        />

        <TextField
          label="Device or equipment"
          name="deviceType"
          value={form.deviceType}
          onChange={onChange}
          placeholder="Example: Dell laptop"
          maxLength={120}
        />
      </FormGrid>

      <TextAreaField
        label="Problem or requested service"
        name="problemDescription"
        value={form.problemDescription}
        onChange={onChange}
        placeholder="Describe the issue, symptoms, requested work, error messages, and any other useful information."
        minLength={20}
        maxLength={2000}
        rows={7}
        required
      />
    </FormSection>
  );
}

// =====================================================================
// STEP 4 — SCHEDULE
// =====================================================================

function ScheduleStep({
  form,
  minimumDate,
  onChange,
}: {
  form: BookingFormState;
  minimumDate: string;
  onChange: ChangeEventHandler;
}) {
  return (
    <div className="space-y-5">
      <FormSection
        stepNumber={4}
        title="Requested Schedule"
        description="Record the customer's preferred date and time. This does not confirm the appointment."
        icon={<Clock3 />}
      >
        <FormGrid>
          <TextField
            label="Preferred date"
            name="preferredDate"
            type="date"
            value={form.preferredDate}
            onChange={onChange}
            min={minimumDate}
            required
          />

          <SelectField
            label="Preferred time"
            name="preferredTime"
            value={form.preferredTime}
            onChange={onChange}
            options={[
              ["MORNING", "Morning"],
              ["AFTERNOON", "Afternoon"],
              ["EVENING", "Evening"],
              ["FLEXIBLE", "Flexible"],
            ]}
          />

          <TextField
            label="Alternate date"
            name="alternateDate"
            type="date"
            value={form.alternateDate}
            onChange={onChange}
            min={minimumDate}
          />
        </FormGrid>
      </FormSection>

      <div className="grid gap-4 lg:grid-cols-3">
        <QuickInfoCard
          icon={<CalendarDays />}
          title="Preferred date"
          description="Use the customer's requested day, not the confirmed appointment date."
        />

        <QuickInfoCard
          icon={<Clock3 />}
          title="Time preference"
          description="Morning, afternoon, evening, or flexible helps scheduling follow-up."
        />

        <QuickInfoCard
          icon={<CalendarPlus />}
          title="Alternate option"
          description="An alternate date gives operations another scheduling option."
        />
      </div>
    </div>
  );
}

// =====================================================================
// STEP 5 — LOCATION & NOTES
// =====================================================================

function LocationAndNotesStep({
  form,
  requiresAddress,
  onChange,
}: {
  form: BookingFormState;
  requiresAddress: boolean;
  onChange: ChangeEventHandler;
}) {
  return (
    <div className="space-y-5">
      <FormSection
        stepNumber={5}
        title="Service Location"
        description={
          requiresAddress
            ? "A complete service address is required because on-site service was selected."
            : "Address information is optional unless the customer requests on-site service."
        }
        icon={<MapPin />}
      >
        {requiresAddress ? (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold leading-6 text-amber-900">
            On-site service requires the street address, city, state or region,
            postal code, and country code.
          </div>
        ) : null}

        <FormGrid>
          <TextField
            label="Street address"
            name="streetAddress"
            value={form.streetAddress}
            onChange={onChange}
            maxLength={180}
            autoComplete="address-line1"
            required={requiresAddress}
          />

          <TextField
            label="Address line 2"
            name="addressLine2"
            value={form.addressLine2}
            onChange={onChange}
            maxLength={180}
            autoComplete="address-line2"
            placeholder="Apartment, suite, unit..."
          />

          <TextField
            label="City"
            name="city"
            value={form.city}
            onChange={onChange}
            maxLength={100}
            autoComplete="address-level2"
            required={requiresAddress}
          />

          <TextField
            label="State / region"
            name="stateRegion"
            value={form.stateRegion}
            onChange={onChange}
            maxLength={100}
            autoComplete="address-level1"
            required={requiresAddress}
          />

          <TextField
            label="Postal code"
            name="postalCode"
            value={form.postalCode}
            onChange={onChange}
            maxLength={30}
            autoComplete="postal-code"
            required={requiresAddress}
          />

          <TextField
            label="Country code"
            name="countryCode"
            value={form.countryCode}
            onChange={onChange}
            maxLength={2}
            placeholder="US"
            required={requiresAddress}
          />
        </FormGrid>
      </FormSection>

      <FormSection
        title="Internal Notes"
        description="Add information that should remain visible only to authenticated administrators."
        icon={<ClipboardCheck />}
      >
        <TextAreaField
          label="Administrator notes"
          name="adminNotes"
          value={form.adminNotes}
          onChange={onChange}
          placeholder="Example: Customer requested a callback after 5:00 PM."
          maxLength={10000}
          rows={5}
        />
      </FormSection>
    </div>
  );
}

// =====================================================================
// STEP 6 — REVIEW
// =====================================================================

function ReviewStep({
  form,
  requiresAddress,
  selectedCustomerSummary,
}: {
  form: BookingFormState;
  requiresAddress: boolean;
  selectedCustomerSummary: AdminCustomerSummary | null;
}) {
  const serviceAddress = [
    form.streetAddress,
    form.addressLine2,
    form.city,
    form.stateRegion,
    form.postalCode,
    form.countryCode,
  ]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  const businessAddress = [
    form.businessStreetAddress,
    form.businessCity,
    form.businessState,
    form.businessPostalCode,
    form.businessCountryCode,
  ]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(", ");

  return (
    <FormSection
      stepNumber={6}
      title="Review and Submit"
      description="Review the booking carefully before creating the customer request. This final step is read-only."
      icon={<ClipboardCheck />}
    >
      <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
        <div className="flex items-start gap-3">
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700" />

          <div>
            <p className="font-bold text-emerald-950">Ready for final review</p>

            <p className="mt-1 text-sm leading-6 text-emerald-900">
              Confirm the customer, service, scheduling, and notification
              information before creating the booking.
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-2">
        <ReviewCard title="Customer" icon={<UserRound />}>
          <ReviewRow
            label="Customer record"
            value={
              selectedCustomerSummary
                ? `${selectedCustomerSummary.customerNumber} — ${selectedCustomerSummary.displayName}`
                : "New / resolved from booking"
            }
          />

          <ReviewRow label="Booking for" value={formatLabel(form.bookingFor)} />

          <ReviewRow label="Full name" value={form.fullName} />

          <ReviewRow label="Email" value={form.email} />

          <ReviewRow label="Telephone" value={form.phone} />

          <ReviewRow
            label="Preferred contact"
            value={formatLabel(form.preferredContactMethod)}
          />

          <ReviewRow
            label="Booking source"
            value={formatLabel(form.bookingSource)}
          />

          <ReviewRow
            label="Notification email"
            value={form.notificationEmail || "Use customer email"}
          />

          <ReviewRow
            label="Notification phone"
            value={form.notificationPhone || "Use customer phone"}
          />
        </ReviewCard>

        {form.bookingFor === "BUSINESS" ? (
          <ReviewCard title="Business" icon={<Building2 />}>
            <ReviewRow label="Business name" value={form.businessName} />

            <ReviewRow
              label="Contact role"
              value={form.businessContactRole || "Not provided"}
            />

            <ReviewRow label="Business email" value={form.businessEmail} />

            <ReviewRow label="Business phone" value={form.businessPhone} />

            <ReviewRow label="Business address" value={businessAddress} />
          </ReviewCard>
        ) : null}

        <ReviewCard title="Service" icon={<Wrench />}>
          <ReviewRow label="Service type" value={form.serviceType} />

          <ReviewRow
            label="Service method"
            value={formatLabel(form.serviceMethod)}
          />

          <ReviewRow
            label="Device or equipment"
            value={form.deviceType || "Not provided"}
          />

          <div className="mt-4">
            <p className="text-sm font-semibold text-slate-500">
              Problem or requested service
            </p>

            <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-800">
              {form.problemDescription}
            </p>
          </div>
        </ReviewCard>

        <ReviewCard title="Schedule" icon={<CalendarDays />}>
          <ReviewRow
            label="Preferred date"
            value={formatDate(form.preferredDate)}
          />

          <ReviewRow
            label="Preferred time"
            value={formatLabel(form.preferredTime)}
          />

          <ReviewRow
            label="Alternate date"
            value={
              form.alternateDate
                ? formatDate(form.alternateDate)
                : "Not provided"
            }
          />
        </ReviewCard>

        <ReviewCard title="Location and Notes" icon={<MapPin />}>
          <ReviewRow
            label="Service location"
            value={
              serviceAddress ||
              (requiresAddress
                ? "Required address missing"
                : "No service address provided")
            }
          />

          <div className="mt-4">
            <p className="text-sm font-semibold text-slate-500">
              Administrator notes
            </p>

            <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-800">
              {form.adminNotes || "No administrator notes provided."}
            </p>
          </div>
        </ReviewCard>
      </div>
    </FormSection>
  );
}

// =====================================================================
// PROGRESS INDICATOR
// =====================================================================

function ProgressIndicator({
  currentStep,
  highestCompletedStep,
  onStepSelection,
}: {
  currentStep: number;
  highestCompletedStep: number;
  onStepSelection: (stepNumber: number) => void;
}) {
  return (
    <nav
      aria-label="Booking creation progress"
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
    >
      <div className="h-1.5 bg-slate-100">
        <div
          className="h-full bg-brand-700 transition-all duration-300"
          style={{
            width: `${(currentStep / TOTAL_STEPS) * 100}%`,
          }}
        />
      </div>

      <div className="hidden lg:grid lg:grid-cols-6">
        {PROGRESS_STEPS.map((step) => {
          const isActive = step.number === currentStep;
          const isCompleted =
            step.number < currentStep || step.number <= highestCompletedStep;
          const isAccessible = step.number <= highestCompletedStep + 1;

          return (
            <button
              key={step.number}
              type="button"
              onClick={() => onStepSelection(step.number)}
              disabled={!isAccessible}
              aria-current={isActive ? "step" : undefined}
              className={[
                "flex min-h-24 items-start gap-3 border-r border-slate-100 px-4 py-4 text-left transition last:border-r-0",
                isActive
                  ? "bg-brand-50/70"
                  : isAccessible
                    ? "bg-white hover:bg-slate-50"
                    : "cursor-not-allowed bg-slate-50/60 opacity-60",
              ].join(" ")}
            >
              <span
                className={[
                  "flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-extrabold [&>svg]:h-4 [&>svg]:w-4",
                  isActive
                    ? "bg-brand-700 text-white"
                    : isCompleted
                      ? "bg-emerald-600 text-white"
                      : "bg-slate-200 text-slate-500",
                ].join(" ")}
              >
                {isCompleted && !isActive ? <Check /> : step.number}
              </span>

              <span className="min-w-0">
                <span
                  className={[
                    "block text-[11px] font-extrabold uppercase tracking-wide",
                    isActive
                      ? "text-brand-700"
                      : isCompleted
                        ? "text-emerald-700"
                        : "text-slate-500",
                  ].join(" ")}
                >
                  Step {step.number}
                </span>

                <span className="mt-1 block font-bold text-navy-950">
                  {step.title}
                </span>

                <span className="mt-1 block text-xs text-slate-500">
                  {step.description}
                </span>
              </span>
            </button>
          );
        })}
      </div>

      <div className="p-4 lg:hidden">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-700 text-sm font-extrabold text-white">
            {currentStep}
          </span>

          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-brand-700">
              Step {currentStep} of {TOTAL_STEPS}
            </p>
            <p className="mt-0.5 font-display text-lg font-extrabold text-navy-950">
              {PROGRESS_STEPS[currentStep - 1]?.title}
            </p>
            <p className="mt-0.5 text-xs text-slate-500">
              {PROGRESS_STEPS[currentStep - 1]?.description}
            </p>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-3 gap-2">
          {PROGRESS_STEPS.map((step) => {
            const isActive = step.number === currentStep;
            const isCompleted =
              step.number < currentStep || step.number <= highestCompletedStep;
            const isAccessible = step.number <= highestCompletedStep + 1;

            return (
              <button
                key={step.number}
                type="button"
                onClick={() => onStepSelection(step.number)}
                disabled={!isAccessible}
                className={[
                  "rounded-xl border px-3 py-2 text-left transition",
                  isActive
                    ? "border-brand-500 bg-brand-50"
                    : isCompleted
                      ? "border-emerald-200 bg-emerald-50"
                      : "border-slate-200 bg-slate-50",
                ].join(" ")}
              >
                <div className="text-[11px] font-extrabold uppercase tracking-wide text-slate-500">
                  Step {step.number}
                </div>
                <div className="mt-1 text-sm font-bold text-navy-950">
                  {step.title}
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </nav>
  );
}

// =====================================================================
// NAVIGATION
// =====================================================================

function FormNavigation({
  currentStep,
  isSubmitting,
  onPrevious,
}: {
  currentStep: number;
  isSubmitting: boolean;
  onPrevious: () => void;
}) {
  const isFirstStep = currentStep === 1;

  const isFinalStep = currentStep === TOTAL_STEPS;

  return (
    <div className="sticky bottom-3 z-10 rounded-2xl border border-slate-200 bg-white/95 p-3.5 shadow-lg backdrop-blur sm:p-4">
      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          {isFirstStep ? (
            <Link
              to="/admin/bookings"
              className="focus-ring inline-flex min-h-11 w-full items-center justify-center rounded-xl border border-slate-300 bg-white px-5 py-2 font-bold text-slate-700 transition hover:border-slate-400 sm:w-auto"
            >
              Cancel
            </Link>
          ) : (
            <button
              type="button"
              onClick={onPrevious}
              disabled={isSubmitting}
              className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-2 font-bold text-slate-700 transition hover:border-brand-400 hover:text-brand-700 disabled:opacity-50 sm:w-auto"
            >
              <ArrowLeft className="h-4 w-4" />
              Previous
            </button>
          )}
        </div>

        <div className="hidden text-xs font-semibold text-slate-400 md:block">
          {isFinalStep
            ? "Ready to create booking"
            : `${TOTAL_STEPS - currentStep} ${TOTAL_STEPS - currentStep === 1 ? "step" : "steps"} remaining`}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="focus-ring inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-6 py-2 font-extrabold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
        >
          {isFinalStep ? (
            <Save className="h-5 w-5" />
          ) : (
            <ArrowRight className="h-5 w-5" />
          )}

          {isSubmitting
            ? "Creating booking..."
            : isFinalStep
              ? "Create Client Booking"
              : "Continue"}
        </button>
      </div>
    </div>
  );
}

// =====================================================================
// FORM COMPONENTS
// =====================================================================

function FormSection({
  stepNumber,
  title,
  description,
  icon,
  children,
}: {
  stepNumber?: number;
  title: string;
  description: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <div className="flex items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-6 [&>svg]:w-6">
          {icon}
        </div>

        <div>
          {stepNumber ? (
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-brand-700">
              Step {stepNumber}
            </p>
          ) : null}

          <h2 className="mt-1 font-display text-xl font-extrabold text-navy-950">
            {title}
          </h2>

          <p className="mt-1 text-sm leading-6 text-slate-600">{description}</p>
        </div>
      </div>

      <div className="mt-5 space-y-4">{children}</div>
    </section>
  );
}

function FormGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-4 md:grid-cols-2">{children}</div>;
}

function QuickInfoCard({
  icon,
  title,
  description,
}: {
  icon: ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-brand-700 shadow-sm [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <div>
          <p className="font-bold text-navy-950">{title}</p>
          <p className="mt-1 text-sm leading-6 text-slate-600">{description}</p>
        </div>
      </div>
    </div>
  );
}

function TextField({
  label,
  name,
  value,
  onChange,
  type = "text",
  required = false,
  disabled = false,
  ...inputProps
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  type?: string;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  min?: string;
  minLength?: number;
  maxLength?: number;
  autoComplete?: string;
}) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-700">
        {label}

        {required ? <span className="text-red-600"> *</span> : null}
      </span>

      <input
        {...inputProps}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-900 placeholder:text-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-600"
      />
    </label>
  );
}

function SelectField({
  label,
  name,
  value,
  onChange,
  options,
  disabled = false,
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  options: Array<[string, string]>;
  disabled?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-700">
        {label} <span className="text-red-600">*</span>
      </span>

      <select
        name={name}
        value={value}
        onChange={onChange}
        required
        disabled={disabled}
        className="focus-ring mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-600"
      >
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextAreaField({
  label,
  name,
  value,
  onChange,
  required = false,
  ...textAreaProps
}: {
  label: string;
  name: string;
  value: string;
  onChange: ChangeEventHandler;
  required?: boolean;
  placeholder?: string;
  minLength?: number;
  maxLength?: number;
  rows?: number;
}) {
  return (
    <label className="block">
      <span className="text-sm font-bold text-slate-700">
        {label}

        {required ? <span className="text-red-600"> *</span> : null}
      </span>

      <textarea
        {...textAreaProps}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="focus-ring mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-slate-900 placeholder:text-slate-400"
      />

      {textAreaProps.maxLength ? (
        <span className="mt-1 block text-right text-xs text-slate-500">
          {value.length}/{textAreaProps.maxLength}
        </span>
      ) : null}
    </label>
  );
}

// =====================================================================
// REVIEW COMPONENTS
// =====================================================================

function ReviewCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <article className="rounded-xl border border-slate-200 p-4">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700 [&>svg]:h-5 [&>svg]:w-5">
          {icon}
        </div>

        <h3 className="font-display text-lg font-extrabold text-navy-950">
          {title}
        </h3>
      </div>

      <div className="mt-5">{children}</div>
    </article>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 border-b border-slate-100 py-3 first:pt-0 last:border-0 last:pb-0 sm:flex-row sm:justify-between sm:gap-5">
      <dt className="text-sm font-semibold text-slate-500">{label}</dt>

      <dd className="break-words font-bold text-slate-800 sm:text-right">
        {value || "Not provided"}
      </dd>
    </div>
  );
}

// =====================================================================
// VALIDATION
// =====================================================================

type ChangeEventHandler = (
  event: ChangeEvent<
    HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  >,
) => void;

function validateStep(step: number, form: BookingFormState): string | null {
  switch (step) {
    case 1:
      return validatePrimaryCustomerStep(form);

    case 2:
      return validateNotificationAndBusinessStep(form);

    case 3:
      return validateServiceStep(form);

    case 4:
      return validateScheduleStep(form);

    case 5:
      return validateLocationAndNotesStep(form);

    case 6:
      return validateEntireForm(form);

    default:
      return "The selected booking step is invalid.";
  }
}

function validatePrimaryCustomerStep(form: BookingFormState): string | null {
  if (!form.bookingFor) {
    return "Select whether the booking is personal or business.";
  }

  if (form.fullName.trim().length < 2) {
    return "Enter the customer's full name.";
  }

  if (!isValidEmail(form.email)) {
    return "Enter a valid customer email address.";
  }

  if (!isValidPhone(form.phone)) {
    return "Enter a valid customer telephone number.";
  }

  return null;
}

function validateNotificationAndBusinessStep(
  form: BookingFormState,
): string | null {
  if (form.notificationEmail.trim() && !isValidEmail(form.notificationEmail)) {
    return "Enter a valid notification email address.";
  }

  if (form.notificationPhone.trim() && !isValidPhone(form.notificationPhone)) {
    return "Enter a valid notification telephone number.";
  }

  if (form.bookingFor === "BUSINESS") {
    if (!form.businessName.trim()) {
      return "Enter the business name.";
    }

    if (!isValidEmail(form.businessEmail)) {
      return "Enter a valid business email address.";
    }

    if (!isValidPhone(form.businessPhone)) {
      return "Enter a valid business telephone number.";
    }

    if (!form.businessStreetAddress.trim()) {
      return "Enter the business street address.";
    }

    if (!form.businessCity.trim()) {
      return "Enter the business city.";
    }

    if (!form.businessState.trim()) {
      return "Enter the business state or region.";
    }

    if (!form.businessPostalCode.trim()) {
      return "Enter the business postal code.";
    }

    if (!isValidCountryCode(form.businessCountryCode)) {
      return "Business country code must contain exactly two letters.";
    }
  }

  return null;
}

function validateServiceStep(form: BookingFormState): string | null {
  if (!form.serviceType.trim()) {
    return "Enter the service the customer needs.";
  }

  if (!form.serviceMethod) {
    return "Select the requested service method.";
  }

  const description = form.problemDescription.trim();

  if (description.length < 20) {
    return "The problem description must contain at least 20 characters.";
  }

  if (description.length > 2000) {
    return "The problem description cannot exceed 2,000 characters.";
  }

  return null;
}

function validateScheduleStep(form: BookingFormState): string | null {
  if (!form.preferredDate) {
    return "Select the customer's preferred service date.";
  }

  if (isBeforeTomorrow(form.preferredDate)) {
    return "Preferred date must be a future date.";
  }

  if (!form.preferredTime) {
    return "Select the customer's preferred service time.";
  }

  if (form.alternateDate && isBeforeTomorrow(form.alternateDate)) {
    return "Alternate date must be a future date.";
  }

  if (form.alternateDate && form.alternateDate === form.preferredDate) {
    return "Alternate date must be different from the preferred date.";
  }

  return null;
}

function validateLocationAndNotesStep(form: BookingFormState): string | null {
  if (form.serviceMethod === "ON_SITE") {
    if (!form.streetAddress.trim()) {
      return "Street address is required for on-site service.";
    }

    if (!form.city.trim()) {
      return "City is required for on-site service.";
    }

    if (!form.stateRegion.trim()) {
      return "State or region is required for on-site service.";
    }

    if (!form.postalCode.trim()) {
      return "Postal code is required for on-site service.";
    }

    if (!isValidCountryCode(form.countryCode)) {
      return "Country code must contain exactly two letters.";
    }
  } else if (form.countryCode.trim() && !isValidCountryCode(form.countryCode)) {
    return "Country code must contain exactly two letters.";
  }

  return null;
}

function validateEntireForm(form: BookingFormState): string | null {
  return (
    validatePrimaryCustomerStep(form) ??
    validateNotificationAndBusinessStep(form) ??
    validateServiceStep(form) ??
    validateScheduleStep(form) ??
    validateLocationAndNotesStep(form)
  );
}

// =====================================================================
// HELPERS
// =====================================================================

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function isValidPhone(value: string): boolean {
  if (!/^[0-9+()\-\.\s]{10,40}$/.test(value.trim())) {
    return false;
  }

  const digitCount = value.replace(/\D/g, "").length;

  return digitCount >= 10 && digitCount <= 20;
}

function isValidCountryCode(value: string): boolean {
  return /^[A-Za-z]{2}$/.test(value.trim());
}

function toNullable(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized : null;
}

function toNullableLowercase(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized.toLowerCase() : null;
}

function toNullableUppercase(value: string): string | null {
  const normalized = value.trim();

  return normalized ? normalized.toUpperCase() : null;
}

function formatCustomerOption(customer: AdminCustomerSummary): string {
  const contact =
    customer.primaryEmail || customer.primaryPhone || "No contact";

  return `${customer.displayName} — ${customer.customerNumber} — ${contact}`;
}

function formatLabel(value: string): string {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatDate(value: string): string {
  if (!value) {
    return "Not provided";
  }

  const date = new Date(`${value}T00:00:00`);

  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-US", {
        dateStyle: "long",
      }).format(date);
}

function formatDateInput(date: Date): string {
  const year = date.getFullYear();

  const month = String(date.getMonth() + 1).padStart(2, "0");

  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function isBeforeTomorrow(value: string): boolean {
  const tomorrow = new Date();

  tomorrow.setDate(tomorrow.getDate() + 1);

  return value < formatDateInput(tomorrow);
}

function scrollToTop() {
  window.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}
