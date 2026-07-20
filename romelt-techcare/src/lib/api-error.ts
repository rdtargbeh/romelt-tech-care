/**
 * ================================================================
 * ROMELT TECHCARE — API ERROR
 * ================================================================
 *
 * Purpose:
 * Provides one consistent error class for failed frontend API
 * requests.
 *
 * Responsibilities:
 * - Stores the HTTP status and backend error message.
 * - Stores backend field-level validation errors.
 * - Distinguishes timeout, network, authorization, and server errors.
 * - Provides user-friendly messages for form and page components.
 *
 * Real-data integration:
 * Error properties should remain aligned with the Spring Boot global
 * exception-handler response structure.
 * ================================================================
 */

import type { ApiErrorDetails, ApiFieldError } from "@/types/api.types";

export class ApiError extends Error {
  readonly status: number;
  readonly statusText: string;
  readonly path?: string;
  readonly fieldErrors: ApiFieldError[];
  readonly validationErrors: Record<string, string>;
  readonly responseBody?: unknown;

  constructor(details: ApiErrorDetails) {
    super(details.message);

    this.name = "ApiError";
    this.status = details.status;
    this.statusText = details.statusText;
    this.path = details.path;
    this.fieldErrors = details.fieldErrors;
    this.validationErrors = details.validationErrors;
    this.responseBody = details.responseBody;

    Object.setPrototypeOf(this, ApiError.prototype);
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  get isValidationError(): boolean {
    return (
      this.status === 400 ||
      this.status === 422 ||
      this.fieldErrors.length > 0 ||
      Object.keys(this.validationErrors).length > 0
    );
  }

  get isServerError(): boolean {
    return this.status >= 500;
  }

  getFieldMessage(fieldName: string): string | undefined {
    const directValidationMessage = this.validationErrors[fieldName];

    if (directValidationMessage) {
      return directValidationMessage;
    }

    return this.fieldErrors.find((fieldError) => fieldError.field === fieldName)
      ?.message;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
