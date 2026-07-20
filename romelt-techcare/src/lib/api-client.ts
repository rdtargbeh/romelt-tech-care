/**
 * ================================================================
 * ROMELT TECHCARE — API CLIENT
 * ================================================================
 *
 * Purpose:
 * Provides the shared HTTP client used by all frontend services to
 * communicate with the Spring Boot backend.
 *
 * Responsibilities:
 * - Builds requests using the configured API base URL.
 * - Sends and receives JSON.
 * - Applies request timeouts.
 * - Handles empty and non-JSON responses safely.
 * - Converts failed responses into ApiError instances.
 * - Supports authenticated and public requests.
 * - Prevents repeated low-level fetch logic across the application.
 *
 * Real-data integration:
 * Authentication-token retrieval should be connected when customer
 * and administrative authentication are implemented.
 * ================================================================
 */

import { environmentConfig } from "@/config/environment.config";
import { ApiError } from "@/lib/api-error";
import { logger } from "@/lib/logger";
import type {
  ApiErrorResponse,
  ApiFieldError,
  ApiRequestOptions,
  ApiResponse,
} from "@/types/api.types";

const DEFAULT_REQUEST_TIMEOUT_MS = 20_000;

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

class ApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = removeTrailingSlash(baseUrl);
  }

  get<TResponse>(
    path: string,
    options?: ApiRequestOptions,
  ): Promise<TResponse> {
    return this.request<TResponse>("GET", path, options);
  }

  post<TResponse>(
    path: string,
    body?: unknown,
    options?: ApiRequestOptions,
  ): Promise<TResponse> {
    return this.request<TResponse>("POST", path, {
      ...options,
      body,
    });
  }

  put<TResponse>(
    path: string,
    body?: unknown,
    options?: ApiRequestOptions,
  ): Promise<TResponse> {
    return this.request<TResponse>("PUT", path, {
      ...options,
      body,
    });
  }

  patch<TResponse>(
    path: string,
    body?: unknown,
    options?: ApiRequestOptions,
  ): Promise<TResponse> {
    return this.request<TResponse>("PATCH", path, {
      ...options,
      body,
    });
  }

  delete<TResponse>(
    path: string,
    options?: ApiRequestOptions,
  ): Promise<TResponse> {
    return this.request<TResponse>("DELETE", path, options);
  }

  private async request<TResponse>(
    method: HttpMethod,
    path: string,
    options: ApiRequestOptions = {},
  ): Promise<TResponse> {
    const {
      body,
      headers,
      timeoutMs = DEFAULT_REQUEST_TIMEOUT_MS,
      requireAuthentication = false,
      signal: externalSignal,
      ...requestInit
    } = options;

    const requestUrl = buildRequestUrl(this.baseUrl, path);

    const abortController = new AbortController();

    const timeoutId = window.setTimeout(() => {
      abortController.abort();
    }, timeoutMs);

    const cancelFromExternalSignal = () => {
      abortController.abort();
    };

    externalSignal?.addEventListener("abort", cancelFromExternalSignal, {
      once: true,
    });

    try {
      const requestHeaders = new Headers(headers);

      requestHeaders.set("Accept", "application/json");

      if (body !== undefined && body !== null && !(body instanceof FormData)) {
        requestHeaders.set("Content-Type", "application/json");
      }

      if (requireAuthentication) {
        const accessToken = getAccessToken();

        if (!accessToken) {
          throw new ApiError({
            status: 401,
            statusText: "Unauthorized",
            message: "Authentication is required to complete this request.",
            fieldErrors: [],
            validationErrors: {},
          });
        }

        requestHeaders.set("Authorization", `Bearer ${accessToken}`);
      }

      logger.debug(`${method} ${requestUrl}`);

      const response = await fetch(requestUrl, {
        ...requestInit,
        method,
        headers: requestHeaders,
        body: serializeRequestBody(body),
        signal: abortController.signal,
      });

      const responseBody = await parseResponseBody(response);

      if (!response.ok) {
        throw createApiError(response, responseBody);
      }

      return unwrapSuccessfulResponse<TResponse>(responseBody);
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }

      if (error instanceof DOMException && error.name === "AbortError") {
        const wasExternallyCancelled = externalSignal?.aborted === true;

        throw new ApiError({
          status: 0,
          statusText: wasExternallyCancelled
            ? "Request Cancelled"
            : "Request Timeout",
          message: wasExternallyCancelled
            ? "The request was cancelled."
            : "The request took too long. Please try again.",
          fieldErrors: [],
          validationErrors: {},
        });
      }

      logger.error("Unable to connect to the API.", error);

      throw new ApiError({
        status: 0,
        statusText: "Network Error",
        message:
          "We could not connect to the service. Check your internet connection and try again.",
        fieldErrors: [],
        validationErrors: {},
        responseBody: error,
      });
    } finally {
      window.clearTimeout(timeoutId);

      externalSignal?.removeEventListener("abort", cancelFromExternalSignal);
    }
  }
}

export const apiClient = new ApiClient(environmentConfig.apiBaseUrl);

function getAccessToken(): string | null {
  /*
   * Authentication has not yet been implemented.
   *
   * Replace this function with the centralized authentication-session
   * service when customer and administrative portals are added.
   */
  return null;
}

function serializeRequestBody(body: unknown): BodyInit | undefined {
  if (body === undefined || body === null) {
    return undefined;
  }

  if (
    body instanceof FormData ||
    body instanceof URLSearchParams ||
    typeof body === "string" ||
    body instanceof Blob ||
    body instanceof ArrayBuffer
  ) {
    return body;
  }

  return JSON.stringify(body);
}

async function parseResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204 || response.status === 205) {
    return undefined;
  }

  const responseText = await response.text();

  if (!responseText.trim()) {
    return undefined;
  }

  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(responseText) as unknown;
    } catch {
      return responseText;
    }
  }

  return responseText;
}

function unwrapSuccessfulResponse<TResponse>(responseBody: unknown): TResponse {
  if (isApiResponse<TResponse>(responseBody)) {
    return responseBody.data;
  }

  return responseBody as TResponse;
}

function createApiError(response: Response, responseBody: unknown): ApiError {
  const backendError = isObject(responseBody)
    ? (responseBody as ApiErrorResponse)
    : undefined;

  const message =
    backendError?.message?.trim() ||
    backendError?.error?.trim() ||
    (typeof responseBody === "string" ? responseBody.trim() : "") ||
    getDefaultErrorMessage(response.status);

  return new ApiError({
    status: response.status,
    statusText: response.statusText,
    message,
    path: backendError?.path,
    fieldErrors: normalizeFieldErrors(backendError?.fieldErrors),
    validationErrors: normalizeValidationErrors(backendError?.validationErrors),
    responseBody,
  });
}

function normalizeFieldErrors(
  value: ApiFieldError[] | undefined,
): ApiFieldError[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((fieldError): fieldError is ApiFieldError =>
    Boolean(
      fieldError &&
      typeof fieldError.field === "string" &&
      typeof fieldError.message === "string",
    ),
  );
}

function normalizeValidationErrors(
  value: Record<string, string> | undefined,
): Record<string, string> {
  if (!value || !isObject(value)) {
    return {};
  }

  return Object.entries(value).reduce<Record<string, string>>(
    (result, [field, message]) => {
      if (typeof message === "string") {
        result[field] = message;
      }

      return result;
    },
    {},
  );
}

function getDefaultErrorMessage(status: number): string {
  switch (status) {
    case 400:
      return "The submitted information is invalid.";

    case 401:
      return "You must sign in to complete this request.";

    case 403:
      return "You do not have permission to complete this request.";

    case 404:
      return "The requested resource could not be found.";

    case 409:
      return "The request conflicts with an existing record.";

    case 422:
      return "Some submitted information could not be processed.";

    case 429:
      return "Too many requests were submitted. Please try again later.";

    default:
      return status >= 500
        ? "The service encountered an unexpected problem. Please try again later."
        : "The request could not be completed.";
  }
}

function buildRequestUrl(baseUrl: string, path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  return `${baseUrl}${normalizedPath}`;
}

function removeTrailingSlash(value: string): string {
  return value.replace(/\/+$/, "");
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isApiResponse<TData>(value: unknown): value is ApiResponse<TData> {
  if (!isObject(value)) {
    return false;
  }

  return (
    typeof value.success === "boolean" &&
    typeof value.message === "string" &&
    "data" in value
  );
}
