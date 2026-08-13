package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.dto.NotificationCreateRequest;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.service.AdminNotificationRecipientService;
import romelt_techcare.backend.service.BookingNotificationService;
import romelt_techcare.backend.service.NotificationService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING NOTIFICATION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Contains notification rules for booking submission and booking
 * lifecycle changes.
 *
 * Customer notifications:
 * - EMAIL when a customer email address exists.
 * - SMS when a customer telephone number exists.
 *
 * Administrator notifications:
 * - IN_APP notification for every active administrator.
 *
 * Delivery:
 * NotificationService stores and sends each notification through the
 * correct channel.
 *
 * Important:
 * This service should be called after the booking transaction commits.
 * It must not cause a valid booking transaction to roll back because
 * an email or SMS provider is unavailable.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationServiceImpl
        implements BookingNotificationService {

    private static final String DEFAULT_TIMEZONE =
            "America/Chicago";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d, yyyy 'at' h:mm a z",
                    Locale.US
            );

    private final NotificationService notificationService;

    private final AdminNotificationRecipientService
            adminNotificationRecipientService;

    /**
     * Sends notifications after a booking has been submitted.
     */
    @Override
    public void bookingSubmitted(
            BookingRequest bookingRequest
    ) {
        requirePersistedBooking(bookingRequest);

        String title =
                "Booking Request Received — "
                        + bookingRequest.getReferenceNumber();

        String customerMessage =
                buildBookingSubmittedMessage(
                        bookingRequest
                );

        String customerHtml =
                buildBookingSubmittedHtml(
                        bookingRequest
                );

        String smsMessage =
                buildBookingSubmittedSms(
                        bookingRequest
                );

        sendCustomerEmail(
                bookingRequest,
                title,
                customerMessage,
                customerHtml
        );

        sendCustomerSms(
                bookingRequest,
                smsMessage
        );

        notifyAdministrators(
                bookingRequest,
                "New Booking Request",
                buildAdminSubmittedMessage(
                        bookingRequest
                )
        );

        log.info(
                "Booking-submission notifications processed. bookingRequestId={}, referenceNumber={}",
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber()
        );
    }

    /**
     * Sends notifications after a successful booking status change.
     */
    @Override
    public void bookingStatusChanged(
            BookingRequest bookingRequest,
            BookingRequestStatus previousStatus
    ) {
        requirePersistedBooking(bookingRequest);

        if (previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous booking status is required."
            );
        }

        BookingRequestStatus currentStatus =
                bookingRequest.getStatus();

        if (currentStatus == null) {
            throw new IllegalArgumentException(
                    "Current booking status is required."
            );
        }

        if (previousStatus == currentStatus) {
            return;
        }

        String title =
                resolveStatusEmailTitle(
                        bookingRequest,
                        currentStatus
                );

        String customerMessage =
                buildStatusMessage(
                        bookingRequest,
                        currentStatus
                );

        String customerHtml =
                buildStatusHtml(
                        bookingRequest,
                        currentStatus,
                        customerMessage
                );

        String smsMessage =
                buildStatusSms(
                        bookingRequest,
                        currentStatus
                );

        sendCustomerEmail(
                bookingRequest,
                title,
                customerMessage,
                customerHtml
        );

        if (shouldSendSmsForStatus(currentStatus)) {
            sendCustomerSms(
                    bookingRequest,
                    smsMessage
            );
        }

        notifyAdministrators(
                bookingRequest,
                "Booking Status Updated",
                "Booking "
                        + bookingRequest.getReferenceNumber()
                        + " changed from "
                        + createStatusDisplayName(previousStatus)
                        + " to "
                        + createStatusDisplayName(currentStatus)
                        + "."
        );

        log.info(
                "Booking-status notifications processed. bookingRequestId={}, referenceNumber={}, previousStatus={}, currentStatus={}",
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                previousStatus,
                currentStatus
        );
    }

    private void sendCustomerEmail(
            BookingRequest bookingRequest,
            String title,
            String messageText,
            String messageHtml
    ) {
        String email =
                normalizeOptional(
                        bookingRequest.getEmail()
                );

        if (email == null) {
            log.debug(
                    "Booking email skipped because no email exists. bookingRequestId={}",
                    bookingRequest.getBookingRequestId()
            );

            return;
        }

        NotificationCreateRequest request =
                new NotificationCreateRequest(
                        NotificationChannel.EMAIL,
                        NotificationRecipientType.CUSTOMER,
                        bookingRequest.getCustomerId(),
                        null,
                        NotificationResourceType.BOOKING_REQUEST,
                        bookingRequest.getBookingRequestId(),
                        email,
                        normalizeOptional(
                                bookingRequest.getFullName()
                        ),
                        title,
                        messageText,
                        messageHtml,
                        null,
                        null,
                        null
                );

        notificationService.sendEmail(request);
    }

    private void sendCustomerSms(
            BookingRequest bookingRequest,
            String message
    ) {
        String phone =
                normalizeOptional(
                        bookingRequest.getPhone()
                );

        if (phone == null) {
            log.debug(
                    "Booking SMS skipped because no phone exists. bookingRequestId={}",
                    bookingRequest.getBookingRequestId()
            );

            return;
        }

        NotificationCreateRequest request =
                new NotificationCreateRequest(
                        NotificationChannel.SMS,
                        NotificationRecipientType.CUSTOMER,
                        bookingRequest.getCustomerId(),
                        null,
                        NotificationResourceType.BOOKING_REQUEST,
                        bookingRequest.getBookingRequestId(),
                        phone,
                        normalizeOptional(
                                bookingRequest.getFullName()
                        ),
                        "Booking Update",
                        message,
                        null,
                        null,
                        null,
                        null
                );

        notificationService.sendSms(request);
    }

    private void notifyAdministrators(
            BookingRequest bookingRequest,
            String title,
            String message
    ) {
        for (
                AdminNotificationRecipientService
                        .AdminNotificationRecipient recipient
                : adminNotificationRecipientService
                .getActiveRecipients()
        ) {
            NotificationCreateRequest request =
                    new NotificationCreateRequest(
                            NotificationChannel.IN_APP,
                            NotificationRecipientType.ADMIN,
                            null,
                            recipient.adminUserId(),
                            NotificationResourceType.BOOKING_REQUEST,
                            bookingRequest.getBookingRequestId(),
                            null,
                            recipient.displayName(),
                            title,
                            message,
                            null,
                            null,
                            null,
                            null
                    );

            notificationService.createInApp(request);
        }
    }

    private String buildBookingSubmittedMessage(
            BookingRequest bookingRequest
    ) {
        return """
                Hello %s,

                We received your Romelt TechCare booking request.

                Reference number: %s
                Service: %s
                Preferred service method: %s
                Preferred date: %s

                What happens next:
                Our team will review your service request and contact you with availability and scheduling information.

                Please keep your reference number for future communication.

                Romelt TechCare
                """.formatted(
                resolveCustomerName(bookingRequest),
                bookingRequest.getReferenceNumber(),
                resolveServiceType(bookingRequest),
                resolveServiceMethod(bookingRequest),
                resolvePreferredDate(bookingRequest)
        );
    }

    private String buildBookingSubmittedHtml(
            BookingRequest bookingRequest
    ) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <p>Hello %s,</p>

                    <p>We received your Romelt TechCare booking request.</p>

                    <p>
                        <strong>Reference number:</strong> %s<br>
                        <strong>Service:</strong> %s<br>
                        <strong>Preferred service method:</strong> %s<br>
                        <strong>Preferred date:</strong> %s
                    </p>

                    <h3>What happens next</h3>

                    <p>
                        Our team will review your service request and contact
                        you with availability and scheduling information.
                    </p>

                    <p>Please keep your reference number for future communication.</p>

                    <p>Romelt TechCare</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        resolveCustomerName(bookingRequest)
                ),
                escapeHtml(
                        bookingRequest.getReferenceNumber()
                ),
                escapeHtml(
                        resolveServiceType(bookingRequest)
                ),
                escapeHtml(
                        resolveServiceMethod(bookingRequest)
                ),
                escapeHtml(
                        resolvePreferredDate(bookingRequest)
                )
        );
    }

    private String buildBookingSubmittedSms(
            BookingRequest bookingRequest
    ) {
        return "Romelt TechCare received your booking request "
                + bookingRequest.getReferenceNumber()
                + ". Our team will review it and contact you with the next step.";
    }

    private String buildAdminSubmittedMessage(
            BookingRequest bookingRequest
    ) {
        return "New booking "
                + bookingRequest.getReferenceNumber()
                + " from "
                + resolveCustomerName(bookingRequest)
                + " for "
                + resolveServiceType(bookingRequest)
                + " requires review.";
    }

    private String resolveStatusEmailTitle(
            BookingRequest bookingRequest,
            BookingRequestStatus status
    ) {
        return switch (status) {
            case PENDING ->
                    "Booking Pending — "
                            + bookingRequest.getReferenceNumber();

            case UNDER_REVIEW ->
                    "Booking Under Review — "
                            + bookingRequest.getReferenceNumber();

            case CONFIRMED ->
                    "Booking Confirmed — "
                            + bookingRequest.getReferenceNumber();

            case COMPLETED ->
                    "Service Completed — "
                            + bookingRequest.getReferenceNumber();

            case CANCELLED ->
                    "Booking Cancelled — "
                            + bookingRequest.getReferenceNumber();

            case DECLINED ->
                    "Booking Update — "
                            + bookingRequest.getReferenceNumber();

            case EXPIRED ->
                    "Booking Expired — "
                            + bookingRequest.getReferenceNumber();
        };
    }

    private String buildStatusMessage(
            BookingRequest bookingRequest,
            BookingRequestStatus status
    ) {
        String customerName =
                resolveCustomerName(bookingRequest);

        String referenceNumber =
                bookingRequest.getReferenceNumber();

        return switch (status) {
            case PENDING ->
                    """
                    Hello %s,

                    Your booking request %s is pending review.

                    Our team will review your request and contact you with availability.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber
                    );

            case UNDER_REVIEW ->
                    """
                    Hello %s,

                    Your booking request %s is now under review.

                    Our team is reviewing your service requirements. No action is required unless we contact you for additional information.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber
                    );

            case CONFIRMED ->
                    """
                    Hello %s,

                    Your Romelt TechCare service appointment has been confirmed.

                    Reference number: %s
                    Scheduled time: %s

                    Please be available at the confirmed appointment time. Contact us promptly if the schedule must change.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveScheduledTime(
                                    bookingRequest
                            )
                    );

            case COMPLETED ->
                    """
                    Hello %s,

                    The service associated with booking %s has been completed.

                    Completion summary:
                    %s

                    Contact Romelt TechCare if you have questions or need additional assistance.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    bookingRequest
                                            .getCompletionSummary(),
                                    "Your requested service was completed."
                            )
                    );

            case CANCELLED ->
                    """
                    Hello %s,

                    Your booking %s has been cancelled.

                    Reason:
                    %s

                    You may submit another booking request when you are ready to reschedule.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    bookingRequest
                                            .getCancellationReason(),
                                    "The booking was cancelled."
                            )
                    );

            case DECLINED ->
                    """
                    Hello %s,

                    We are unable to accept booking request %s at this time.

                    Reason:
                    %s

                    Contact Romelt TechCare to discuss another service option.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    bookingRequest
                                            .getDeclineReason(),
                                    "We are unable to provide the requested service at this time."
                            )
                    );

            case EXPIRED ->
                    """
                    Hello %s,

                    Your booking request %s expired before it was confirmed.

                    Reason:
                    %s

                    Submit a new booking request if you still need the service.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    bookingRequest
                                            .getExpirationReason(),
                                    "The booking request was not confirmed before it expired."
                            )
                    );
        };
    }

    private String buildStatusHtml(
            BookingRequest bookingRequest,
            BookingRequestStatus status,
            String textMessage
    ) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <h2>%s</h2>
                    <p><strong>Reference number:</strong> %s</p>
                    <p>%s</p>
                    <p>Romelt TechCare</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        createStatusDisplayName(status)
                ),
                escapeHtml(
                        bookingRequest.getReferenceNumber()
                ),
                escapeHtml(textMessage)
                        .replace(
                                "\n",
                                "<br>"
                        )
        );
    }

    private String buildStatusSms(
            BookingRequest bookingRequest,
            BookingRequestStatus status
    ) {
        String reference =
                bookingRequest.getReferenceNumber();

        return switch (status) {
            case PENDING ->
                    "Romelt TechCare: Booking "
                            + reference
                            + " is pending review. We will contact you with the next step.";

            case UNDER_REVIEW ->
                    "Romelt TechCare: Booking "
                            + reference
                            + " is now under review.";

            case CONFIRMED ->
                    "Romelt TechCare: Booking "
                            + reference
                            + " is confirmed for "
                            + resolveScheduledTime(bookingRequest)
                            + ".";

            case COMPLETED ->
                    "Romelt TechCare: Service for booking "
                            + reference
                            + " has been completed.";

            case CANCELLED ->
                    "Romelt TechCare: Booking "
                            + reference
                            + " has been cancelled. Check your email for details.";

            case DECLINED ->
                    "Romelt TechCare: We are unable to accept booking "
                            + reference
                            + ". Check your email for details.";

            case EXPIRED ->
                    "Romelt TechCare: Booking "
                            + reference
                            + " has expired. Submit a new request if service is still needed.";
        };
    }

    private boolean shouldSendSmsForStatus(
            BookingRequestStatus status
    ) {
        return switch (status) {
            case PENDING,
                 UNDER_REVIEW,
                 CONFIRMED,
                 COMPLETED,
                 CANCELLED,
                 DECLINED,
                 EXPIRED -> true;
        };
    }

    private String resolveScheduledTime(
            BookingRequest bookingRequest
    ) {
        Instant scheduledStart =
                bookingRequest.getScheduledStartAt();

        if (scheduledStart == null) {
            return "the scheduled time provided by our team";
        }

        String timezone =
                normalizeOptional(
                        bookingRequest.getScheduledTimezone()
                );

        ZoneId zoneId;

        try {
            zoneId =
                    ZoneId.of(
                            timezone == null
                                    ? DEFAULT_TIMEZONE
                                    : timezone
                    );
        } catch (Exception ignored) {
            zoneId =
                    ZoneId.of(DEFAULT_TIMEZONE);
        }

        return DATE_TIME_FORMATTER.format(
                scheduledStart.atZone(zoneId)
        );
    }

    private String resolveCustomerName(
            BookingRequest bookingRequest
    ) {
        return resolveOptionalValue(
                bookingRequest.getFullName(),
                "Customer"
        );
    }

    private String resolveServiceType(
            BookingRequest bookingRequest
    ) {
        return resolveOptionalValue(
                String.valueOf(
                        bookingRequest.getServiceType()
                ),
                "Technology service"
        );
    }

    private String resolveServiceMethod(
            BookingRequest bookingRequest
    ) {
        Object serviceMethod =
                bookingRequest.getServiceMethod();

        if (serviceMethod == null) {
            return "To be confirmed";
        }

        return createDisplayName(
                serviceMethod.toString()
        );
    }

    private String resolvePreferredDate(
            BookingRequest bookingRequest
    ) {
        return bookingRequest.getPreferredDate() == null
                ? "To be confirmed"
                : bookingRequest
                .getPreferredDate()
                .toString();
    }

    private String createStatusDisplayName(
            BookingRequestStatus status
    ) {
        if (status == null) {
            return "Booking Update";
        }

        return createDisplayName(
                status.name()
        );
    }

    private String createDisplayName(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return "Not specified";
        }

        String[] words =
                normalized
                        .toLowerCase(Locale.ROOT)
                        .replace('_', ' ')
                        .split("\\s+");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            if (word.length() > 1) {
                result.append(
                        word.substring(1)
                );
            }
        }

        return result.toString();
    }

    private String resolveOptionalValue(
            String value,
            String fallback
    ) {
        String normalized =
                normalizeOptional(value);

        return normalized == null
                ? fallback
                : normalized;
    }

    private void requirePersistedBooking(
            BookingRequest bookingRequest
    ) {
        if (
                bookingRequest == null
                        || bookingRequest.getBookingRequestId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request is required."
            );
        }

        if (
                normalizeOptional(
                        bookingRequest.getReferenceNumber()
                ) == null
        ) {
            throw new IllegalArgumentException(
                    "Booking reference number is required."
            );
        }

        if (bookingRequest.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Booking status is required."
            );
        }
    }

    private String escapeHtml(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}