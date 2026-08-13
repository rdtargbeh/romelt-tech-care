/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER SERVICE
 * ================================================================
 *
 * Purpose:
 * Provides authenticated API operations for the reusable Romelt
 * TechCare customer directory.
 *
 * Responsibilities:
 * - Searches and retrieves customer summaries.
 * - Retrieves one complete customer profile.
 * - Creates administrator-entered customers.
 * - Updates reusable customer information.
 * - Archives customers.
 * - Restores archived customers.
 * - Soft-deletes customers.
 * - Merges duplicate customer profiles.
 *
 * Authentication:
 * Every operation requires the administrator JWT.
 *
 * Real-data integration:
 *
 * POST   /api/v1/admin/customers
 * GET    /api/v1/admin/customers
 * GET    /api/v1/admin/customers/{customerId}
 * PUT    /api/v1/admin/customers/{customerId}
 * POST   /api/v1/admin/customers/{customerId}/archive
 * POST   /api/v1/admin/customers/{customerId}/restore
 * DELETE /api/v1/admin/customers/{customerId}
 * POST   /api/v1/admin/customers/{customerId}/merge
 * ================================================================
 */

import { apiClient } from "@/lib/api-client";

import type {
  AdminCustomer,
  AdminCustomerCreatePayload,
  AdminCustomerMergePayload,
  AdminCustomerSummary,
  AdminCustomerUpdatePayload,
  GetAdminCustomersOptions,
} from "@/types/admin-customer.types";

import type { PageResponse } from "@/types/admin-customer-request.types";

// =====================================================================
// ENDPOINT
// =====================================================================

const ADMIN_CUSTOMERS_ENDPOINT = "/admin/customers";

// =====================================================================
// LIST / SEARCH
// =====================================================================

export function getAdminCustomers(
  options: GetAdminCustomersOptions = {},
): Promise<PageResponse<AdminCustomerSummary>> {
  const { keyword, customerStatus, page = 0, size = 10, signal } = options;

  const searchParams = new URLSearchParams();

  searchParams.set("page", String(Math.max(0, page)));

  searchParams.set("size", String(Math.min(Math.max(1, size), 50)));

  const normalizedKeyword = normalizeOptional(keyword);

  if (normalizedKeyword) {
    searchParams.set("keyword", normalizedKeyword);
  }

  if (customerStatus) {
    searchParams.set("customerStatus", customerStatus);
  }

  return apiClient.get<PageResponse<AdminCustomerSummary>>(
    `${ADMIN_CUSTOMERS_ENDPOINT}?${searchParams.toString()}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// GET ONE
// =====================================================================

export function getAdminCustomer(
  customerId: string,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  const normalizedId = requireIdentifier(customerId, "Customer ID");

  return apiClient.get<AdminCustomer>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(normalizedId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// CREATE
// =====================================================================

export function createAdminCustomer(
  request: AdminCustomerCreatePayload,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  if (!request) {
    throw new Error("Customer information is required.");
  }

  return apiClient.post<AdminCustomer>(ADMIN_CUSTOMERS_ENDPOINT, request, {
    signal,
    requireAuthentication: true,
  });
}

// =====================================================================
// UPDATE
// =====================================================================

export function updateAdminCustomer(
  customerId: string,
  request: AdminCustomerUpdatePayload,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  const normalizedId = requireIdentifier(customerId, "Customer ID");

  if (!request) {
    throw new Error("Customer update information is required.");
  }

  return apiClient.put<AdminCustomer>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(normalizedId)}`,
    request,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// ARCHIVE
// =====================================================================

export function archiveAdminCustomer(
  customerId: string,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  const normalizedId = requireIdentifier(customerId, "Customer ID");

  return apiClient.post<AdminCustomer>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(normalizedId)}/archive`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// RESTORE
// =====================================================================

export function restoreAdminCustomer(
  customerId: string,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  const normalizedId = requireIdentifier(customerId, "Customer ID");

  return apiClient.post<AdminCustomer>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(normalizedId)}/restore`,
    undefined,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// DELETE
// =====================================================================

export async function deleteAdminCustomer(
  customerId: string,
  signal?: AbortSignal,
): Promise<void> {
  const normalizedId = requireIdentifier(customerId, "Customer ID");

  await apiClient.delete<void>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(normalizedId)}`,
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// MERGE
// =====================================================================

export function mergeAdminCustomer(
  duplicateCustomerId: string,
  request: AdminCustomerMergePayload,
  signal?: AbortSignal,
): Promise<AdminCustomer> {
  const normalizedDuplicateId = requireIdentifier(
    duplicateCustomerId,
    "Duplicate customer ID",
  );

  if (!request) {
    throw new Error("Customer merge information is required.");
  }

  const survivingCustomerId = requireIdentifier(
    request.survivingCustomerId,
    "Surviving customer ID",
  );

  return apiClient.post<AdminCustomer>(
    `${ADMIN_CUSTOMERS_ENDPOINT}/${encodeURIComponent(
      normalizedDuplicateId,
    )}/merge`,
    {
      ...request,
      survivingCustomerId,
    },
    {
      signal,
      requireAuthentication: true,
    },
  );
}

// =====================================================================
// HELPERS
// =====================================================================

function requireIdentifier(value: string, label: string): string {
  const normalized = value?.trim();

  if (!normalized) {
    throw new Error(`${label} is required.`);
  }

  return normalized;
}

function normalizeOptional(value: string | null | undefined): string | null {
  if (value == null) {
    return null;
  }

  const normalized = value.trim();

  return normalized.length > 0 ? normalized : null;
}
