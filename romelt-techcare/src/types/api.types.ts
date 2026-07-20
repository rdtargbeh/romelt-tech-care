/**
 * ================================================================
 * ROMELT TECHCARE — API TYPES
 * ================================================================
 *
 * Purpose:
 * Defines shared frontend types for communication with the Spring
 * Boot backend.
 *
 * Responsibilities:
 * - Defines the standard backend response structure.
 * - Defines field-level validation errors.
 * - Defines frontend request options.
 * - Provides a consistent error shape across all API services.
 *
 * Real-data integration:
 * The Spring Boot backend should return responses that follow the
 * ApiResponse structure defined here.
 * ================================================================
 */

export interface ApiResponse<TData> {
  success: boolean;
  message: string;
  data: TData;
  timestamp?: string;
  path?: string;
}

export interface ApiFieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ApiErrorResponse {
  success?: false;
  message?: string;
  error?: string;
  status?: number;
  timestamp?: string;
  path?: string;
  fieldErrors?: ApiFieldError[];
  validationErrors?: Record<string, string>;
}

export interface ApiRequestOptions extends Omit<
  RequestInit,
  "body" | "headers"
> {
  body?: unknown;
  headers?: HeadersInit;
  timeoutMs?: number;
  requireAuthentication?: boolean;
}

export interface ApiErrorDetails {
  status: number;
  statusText: string;
  message: string;
  path?: string;
  fieldErrors: ApiFieldError[];
  validationErrors: Record<string, string>;
  responseBody?: unknown;
}
