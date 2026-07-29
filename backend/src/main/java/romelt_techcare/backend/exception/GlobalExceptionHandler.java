package romelt_techcare.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import romelt_techcare.backend.api.ApiErrorResponse;
import romelt_techcare.backend.api.ApiFieldError;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — GLOBAL EXCEPTION HANDLER
 * ================================================================
 *
 * Purpose:
 * Converts controller and service exceptions into predictable JSON
 * responses that the React frontend can process consistently.
 *
 * Responsibilities:
 * - Handles Jakarta Bean Validation failures.
 * - Handles request-parameter constraint violations.
 * - Handles malformed JSON.
 * - Handles controlled public request rejections.
 * - Handles controlled administrator authentication failures.
 * - Handles authentication and authorization failures.
 * - Handles missing routes and unsupported methods.
 * - Handles database-integrity conflicts safely.
 * - Prevents internal exception details from reaching API clients.
 *
 * Security:
 * - Stack traces are never returned to clients.
 * - SQL and infrastructure details are never returned.
 * - Passwords, tokens, secrets, payment data, and sensitive rejected
 *   values are never included in responses.
 * ================================================================
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toApiFieldError)
                .toList();

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (ApiFieldError fieldError : fieldErrors) {
            validationErrors.putIfAbsent(
                    fieldError.field(),
                    fieldError.message()
            );
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Some submitted information is invalid.",
                request,
                fieldErrors,
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fieldErrors = exception
                .getConstraintViolations()
                .stream()
                .map(this::toApiFieldError)
                .toList();

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (ApiFieldError fieldError : fieldErrors) {
            validationErrors.putIfAbsent(
                    fieldError.field(),
                    fieldError.message()
            );
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Some submitted information is invalid.",
                request,
                fieldErrors,
                validationErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "The request body is missing or contains invalid JSON.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        ApiFieldError fieldError = new ApiFieldError(
                exception.getParameterName(),
                "The request parameter is required.",
                null
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "A required request parameter is missing.",
                request,
                List.of(fieldError),
                Map.of(
                        exception.getParameterName(),
                        "The request parameter is required."
                )
        );
    }

    @ExceptionHandler(PublicRequestRejectedException.class)
    public ResponseEntity<ApiErrorResponse>
    handlePublicRequestRejected(
            PublicRequestRejectedException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                exception.getStatus() == null
                        ? HttpStatus.BAD_REQUEST
                        : exception.getStatus();

        return buildResponse(
                status,
                safeMessage(
                        exception.getMessage(),
                        "The submitted request could not be accepted."
                ),
                request
        );
    }

    @ExceptionHandler(AdminAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleAdminAuthenticationException(
            AdminAuthenticationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                exception.getStatus() == null
                        ? HttpStatus.UNAUTHORIZED
                        : exception.getStatus();

        Map<String, String> details = Map.of(
                "errorCode",
                safeErrorCode(exception.getErrorCode())
        );

        if (status == HttpStatus.UNAUTHORIZED
                || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.LOCKED) {

            log.warn(
                    "Administrator authentication request rejected. "
                            + "Method={}, path={}, status={}, code={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    safeErrorCode(exception.getErrorCode())
            );
        }

        return buildResponse(
                status,
                safeMessage(
                        exception.getMessage(),
                        "Administrator authentication failed."
                ),
                request,
                List.of(),
                details
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Authentication failed for {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse>
    handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Access denied for {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this operation.",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse>
    handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "The requested HTTP method is not supported for this endpoint.",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "The requested resource was not found.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Database integrity conflict while processing {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with existing information.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The service encountered an unexpected problem. Please try again later.",
                request
        );
    }

    private ApiFieldError toApiFieldError(
            FieldError fieldError
    ) {
        return new ApiFieldError(
                fieldError.getField(),
                safeMessage(
                        fieldError.getDefaultMessage(),
                        "The submitted value is invalid."
                ),
                sanitizeRejectedValue(
                        fieldError.getField(),
                        fieldError.getRejectedValue()
                )
        );
    }

    private ApiFieldError toApiFieldError(
            ConstraintViolation<?> violation
    ) {
        String propertyPath =
                violation.getPropertyPath() == null
                        ? "request"
                        : violation.getPropertyPath().toString();

        String fieldName = propertyPath;

        int separatorIndex =
                propertyPath.lastIndexOf('.');

        if (separatorIndex >= 0
                && separatorIndex
                < propertyPath.length() - 1) {

            fieldName = propertyPath.substring(
                    separatorIndex + 1
            );
        }

        return new ApiFieldError(
                fieldName,
                safeMessage(
                        violation.getMessage(),
                        "The submitted value is invalid."
                ),
                sanitizeRejectedValue(
                        fieldName,
                        violation.getInvalidValue()
                )
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(
                status,
                message,
                request,
                List.of(),
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ApiFieldError> fieldErrors,
            Map<String, String> validationErrors
    ) {
        HttpStatus resolvedStatus =
                status == null
                        ? HttpStatus.INTERNAL_SERVER_ERROR
                        : status;

        String path =
                request == null
                        ? "/"
                        : request.getRequestURI();

        ApiErrorResponse response =
                new ApiErrorResponse(
                        false,
                        resolvedStatus.value(),
                        resolvedStatus.getReasonPhrase(),
                        safeMessage(
                                message,
                                "The request could not be completed."
                        ),
                        path,
                        Instant.now(),
                        fieldErrors == null
                                ? List.of()
                                : List.copyOf(fieldErrors),
                        validationErrors == null
                                ? Map.of()
                                : Map.copyOf(validationErrors)
                );

        return ResponseEntity
                .status(resolvedStatus)
                .body(response);
    }

    private Object sanitizeRejectedValue(
            String fieldName,
            Object rejectedValue
    ) {
        String normalizedFieldName =
                fieldName == null
                        ? ""
                        : fieldName
                        .replace("_", "")
                        .replace("-", "")
                        .replace(".", "")
                        .toLowerCase();

        if (normalizedFieldName.contains("password")
                || normalizedFieldName.contains("secret")
                || normalizedFieldName.contains("token")
                || normalizedFieldName.contains("authorization")
                || normalizedFieldName.contains("apikey")
                || normalizedFieldName.contains("card")
                || normalizedFieldName.contains("cvv")
                || normalizedFieldName.contains("cvc")
                || normalizedFieldName.contains("bank")
                || normalizedFieldName.contains("routing")
                || normalizedFieldName.contains("socialsecurity")
                || normalizedFieldName.contains("ssn")) {

            return null;
        }

        return rejectedValue;
    }

    private String safeMessage(
            String message,
            String fallback
    ) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        return message.trim();
    }

    private String safeErrorCode(
            String errorCode
    ) {
        if (errorCode == null || errorCode.isBlank()) {
            return "ADMIN_AUTHENTICATION_FAILED";
        }

        return errorCode.trim();
    }
}