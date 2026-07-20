/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING FORM DATA
 * ================================================================
 *
 * Purpose:
 * Provides reusable booking options for the public service-request
 * form.
 *
 * Responsibilities:
 * - Defines customer types.
 * - Defines available technology services.
 * - Defines supported service-delivery methods.
 * - Prevents booking options from being duplicated inside pages.
 *
 * Real-data integration:
 * Services, availability, prices, delivery methods, and active
 * status should later be loaded from the Spring Boot backend.
 * ================================================================
 */

export interface SelectOption {
  value: string;
  label: string;
}

export const customerTypeOptions: SelectOption[] = [
  {
    value: "individual",
    label: "Individual",
  },
  {
    value: "home-office",
    label: "Home office or remote worker",
  },
  {
    value: "small-business",
    label: "Small business",
  },
  {
    value: "church",
    label: "Church or ministry",
  },
  {
    value: "nonprofit",
    label: "Nonprofit organization",
  },
  {
    value: "other-organization",
    label: "Other organization",
  },
];

export const serviceTypeOptions: SelectOption[] = [
  {
    value: "computer-support",
    label: "Computer troubleshooting and support",
  },
  {
    value: "wifi-networking",
    label: "Wi-Fi and networking",
  },
  {
    value: "device-setup",
    label: "New device setup",
  },
  {
    value: "printer-support",
    label: "Printer and scanner support",
  },
  {
    value: "remote-support",
    label: "Remote technology support",
  },
  {
    value: "business-it",
    label: "Small-business IT support",
  },
  {
    value: "security-care",
    label: "Security and preventive maintenance",
  },
  {
    value: "data-storage",
    label: "Data transfer, backup, and storage support",
  },
  {
    value: "mobile-device",
    label: "Phone or tablet assistance",
  },
  {
    value: "other",
    label: "Other technology service",
  },
];

export const serviceMethodOptions: SelectOption[] = [
  {
    value: "remote",
    label: "Remote support",
  },
  {
    value: "onsite",
    label: "On-site support",
  },
  {
    value: "either",
    label: "Either remote or on-site",
  },
];

export const preferredContactOptions: SelectOption[] = [
  {
    value: "phone",
    label: "Phone call",
  },
  {
    value: "text",
    label: "Text message",
  },
  {
    value: "email",
    label: "Email",
  },
];

export const preferredTimeOptions: SelectOption[] = [
  {
    value: "weekday-evening",
    label: "Weekday evening",
  },
  {
    value: "saturday-morning",
    label: "Saturday morning",
  },
  {
    value: "saturday-afternoon",
    label: "Saturday afternoon",
  },
  {
    value: "flexible",
    label: "Flexible",
  },
];
