package com.romelttechcare.backend.common.exception;

import com.romelttechcare.backend.common.api.ApiErrorResponse;
import com.romelttechcare.backend.common.api.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
 * Converts backend exceptions into predictable JSON responses that
 * the React frontend can process consistently.
 *
 * Responsibilities:
 * - Handles Jakarta Bean Validation failures.
 * - Handles malformed JSON requests.
 * - Handles application business exceptions.
 * - Prevents internal exception details from reaching public clients.
 * - Logs unexpected failures for server-side investigation.
 *
 * Security:
 * Stack traces, SQL messages, infrastructure details, and internal
 * class names are never returned to clients.
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

        ApiErrorResponse response = new ApiErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Some submitted information is invalid.",
                request.getRequestURI(),
                Instant.now(),
                fieldErrors,
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "The request body is missing or contains invalid JSON.",
                request.getRequestURI(),
                Instant.now(),
                List.of(),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(PublicRequestRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handlePublicRequestRejected(
            PublicRequestRejectedException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                false,
                exception.getStatus().value(),
                exception.getStatus().getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now(),
                List.of(),
                Map.of()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ApiErrorResponse response = new ApiErrorResponse(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "The service encountered an unexpected problem. Please try again later.",
                request.getRequestURI(),
                Instant.now(),
                List.of(),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ApiFieldError toApiFieldError(
            FieldError fieldError
    ) {
        return new ApiFieldError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                sanitizeRejectedValue(
                        fieldError.getField(),
                        fieldError.getRejectedValue()
                )
        );
    }

    private Object sanitizeRejectedValue(
            String fieldName,
            Object rejectedValue
    ) {
        String normalizedFieldName =
                fieldName == null
                        ? ""
                        : fieldName.toLowerCase();

        if (
                normalizedFieldName.contains("password")
                        || normalizedFieldName.contains("secret")
                        || normalizedFieldName.contains("token")
                        || normalizedFieldName.contains("card")
                        || normalizedFieldName.contains("bank")
                        || normalizedFieldName.contains("socialsecurity")
        ) {
            return null;
        }

        return rejectedValue;
    }
}