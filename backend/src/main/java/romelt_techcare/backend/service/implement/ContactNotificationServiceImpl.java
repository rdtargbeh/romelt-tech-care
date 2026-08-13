package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.dto.NotificationCreateRequest;
import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.service.AdminNotificationRecipientService;
import romelt_techcare.backend.service.ContactNotificationService;
import romelt_techcare.backend.service.NotificationService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT NOTIFICATION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Contains notification rules for contact-inquiry submission and
 * lifecycle updates.
 *
 * Customer notification rules:
 *
 * NEW:
 * - EMAIL acknowledgment when an email address exists.
 * - SMS acknowledgment when a telephone number exists.
 *
 * IN_PROGRESS:
 * - EMAIL status update.
 * - SMS status update when a telephone number exists.
 *
 * RESPONDED:
 * - EMAIL response notification.
 * - SMS response alert when a telephone number exists.
 *
 * CLOSED:
 * - EMAIL closure notification.
 * - SMS closure notification when a telephone number exists.
 *
 * SPAM:
 * - No customer EMAIL.
 * - No customer SMS.
 * - Administrator IN_APP notification only.
 *
 * Administrator rules:
 * - Every active administrator receives an IN_APP notification when
 *   an inquiry is submitted or its status changes.
 *
 * Delivery:
 * NotificationService creates the database notification record and
 * delivers it through the selected channel.
 *
 * Important:
 * This service should run after the contact-inquiry transaction
 * commits so provider failures do not roll back a valid inquiry.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactNotificationServiceImpl  implements ContactNotificationService {

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
     * Sends customer and administrator notifications after a contact
     * inquiry is submitted.
     */
    @Override
    public void inquirySubmitted(
            ContactInquiry contactInquiry
    ) {
        requirePersistedInquiry(contactInquiry);

        String emailTitle =
                "We Received Your Inquiry — "
                        + contactInquiry.getReferenceNumber();

        String emailText =
                buildInquirySubmittedText(
                        contactInquiry
                );

        String emailHtml =
                buildInquirySubmittedHtml(
                        contactInquiry
                );

        String smsText =
                buildInquirySubmittedSms(
                        contactInquiry
                );

        sendCustomerEmail(
                contactInquiry,
                emailTitle,
                emailText,
                emailHtml
        );

        sendCustomerSms(
                contactInquiry,
                smsText
        );

        notifyAdministrators(
                contactInquiry,
                "New Contact Inquiry",
                buildAdministratorSubmittedMessage(
                        contactInquiry
                )
        );

        log.info(
                "Contact-inquiry submission notifications processed. contactInquiryId={}, referenceNumber={}",
                contactInquiry.getContactInquiryId(),
                contactInquiry.getReferenceNumber()
        );
    }

    /**
     * Sends customer and administrator notifications after an inquiry
     * status change.
     */
    @Override
    public void inquiryStatusChanged(
            ContactInquiry contactInquiry,
            ContactInquiryStatus previousStatus,
            String customerVisibleMessage
    ) {
        requirePersistedInquiry(contactInquiry);

        if (previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous contact inquiry status is required."
            );
        }

        ContactInquiryStatus currentStatus =
                contactInquiry.getStatus();

        if (currentStatus == null) {
            throw new IllegalArgumentException(
                    "Current contact inquiry status is required."
            );
        }

        if (previousStatus == currentStatus) {
            return;
        }

        /*
         * SPAM is an internal classification.
         * Do not inform the sender that the inquiry was marked spam.
         */
        if (currentStatus != ContactInquiryStatus.SPAM) {
            String emailTitle =
                    resolveStatusEmailTitle(
                            contactInquiry,
                            currentStatus
                    );

            String emailText =
                    buildStatusText(
                            contactInquiry,
                            currentStatus,
                            customerVisibleMessage
                    );

            String emailHtml =
                    buildStatusHtml(
                            contactInquiry,
                            currentStatus,
                            customerVisibleMessage
                    );

            String smsText =
                    buildStatusSms(
                            contactInquiry,
                            currentStatus
                    );

            sendCustomerEmail(
                    contactInquiry,
                    emailTitle,
                    emailText,
                    emailHtml
            );

            sendCustomerSms(
                    contactInquiry,
                    smsText
            );
        }

        notifyAdministrators(
                contactInquiry,
                currentStatus == ContactInquiryStatus.SPAM
                        ? "Contact Inquiry Marked as Spam"
                        : "Contact Inquiry Updated",
                buildAdministratorStatusMessage(
                        contactInquiry,
                        previousStatus,
                        currentStatus
                )
        );

        log.info(
                "Contact-inquiry status notifications processed. contactInquiryId={}, referenceNumber={}, previousStatus={}, currentStatus={}",
                contactInquiry.getContactInquiryId(),
                contactInquiry.getReferenceNumber(),
                previousStatus,
                currentStatus
        );
    }

    // ================================================================
    // CUSTOMER EMAIL
    // ================================================================

    private void sendCustomerEmail(
            ContactInquiry contactInquiry,
            String title,
            String messageText,
            String messageHtml
    ) {
        String email =
                normalizeOptional(
                        contactInquiry.getEmail()
                );

        if (email == null) {
            log.debug(
                    "Contact-inquiry email skipped because no email exists. contactInquiryId={}",
                    contactInquiry.getContactInquiryId()
            );

            return;
        }

        NotificationCreateRequest request =
                new NotificationCreateRequest(
                        NotificationChannel.EMAIL,
                        NotificationRecipientType.CUSTOMER,
                        resolveCustomerId(contactInquiry),
                        null,
                        NotificationResourceType.CONTACT_INQUIRY,
                        contactInquiry.getContactInquiryId(),
                        email,
                        normalizeOptional(
                                contactInquiry.getFullName()
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

    // ================================================================
    // CUSTOMER SMS
    // ================================================================

    private void sendCustomerSms(
            ContactInquiry contactInquiry,
            String message
    ) {
        String phone =
                normalizeOptional(
                        contactInquiry.getPhone()
                );

        if (phone == null) {
            log.debug(
                    "Contact-inquiry SMS skipped because no phone exists. contactInquiryId={}",
                    contactInquiry.getContactInquiryId()
            );

            return;
        }

        NotificationCreateRequest request =
                new NotificationCreateRequest(
                        NotificationChannel.SMS,
                        NotificationRecipientType.CUSTOMER,
                        resolveCustomerId(contactInquiry),
                        null,
                        NotificationResourceType.CONTACT_INQUIRY,
                        contactInquiry.getContactInquiryId(),
                        phone,
                        normalizeOptional(
                                contactInquiry.getFullName()
                        ),
                        "Contact Inquiry Update",
                        message,
                        null,
                        null,
                        null,
                        null
                );

        notificationService.sendSms(request);
    }

    // ================================================================
    // ADMINISTRATOR IN-APP NOTIFICATIONS
    // ================================================================

    private void notifyAdministrators(
            ContactInquiry contactInquiry,
            String title,
            String message
    ) {
        for (
                AdminNotificationRecipientService.AdminNotificationRecipient
                        recipient
                : adminNotificationRecipientService.getActiveRecipients()
        ) {
            if (recipient.adminUserId() == null) {
                continue;
            }

            NotificationCreateRequest request =
                    new NotificationCreateRequest(
                            NotificationChannel.IN_APP,
                            NotificationRecipientType.ADMIN,
                            null,
                            recipient.adminUserId(),
                            NotificationResourceType.CONTACT_INQUIRY,
                            contactInquiry.getContactInquiryId(),
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

    // ================================================================
    // SUBMISSION MESSAGES
    // ================================================================

    private String buildInquirySubmittedText(
            ContactInquiry contactInquiry
    ) {
        return """
                Hello %s,

                We received your inquiry at Romelt TechCare.

                Reference number: %s
                Subject: %s
                Service: %s
                Preferred contact method: %s
                Submitted: %s

                What happens next:
                Our team will review your message and respond using your preferred contact method.

                Please keep your reference number for future communication.

                Romelt TechCare
                """.formatted(
                resolveCustomerName(contactInquiry),
                contactInquiry.getReferenceNumber(),
                resolveSubject(contactInquiry),
                resolveServiceType(contactInquiry),
                resolvePreferredContactMethod(contactInquiry),
                resolveSubmittedTime(contactInquiry)
        );
    }

    private String buildInquirySubmittedHtml(
            ContactInquiry contactInquiry
    ) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <p>Hello %s,</p>

                    <p>We received your inquiry at Romelt TechCare.</p>

                    <p>
                        <strong>Reference number:</strong> %s<br>
                        <strong>Subject:</strong> %s<br>
                        <strong>Service:</strong> %s<br>
                        <strong>Preferred contact method:</strong> %s<br>
                        <strong>Submitted:</strong> %s
                    </p>

                    <h3>What happens next</h3>

                    <p>
                        Our team will review your message and respond using
                        your preferred contact method.
                    </p>

                    <p>
                        Please keep your reference number for future
                        communication.
                    </p>

                    <p>Romelt TechCare</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        resolveCustomerName(contactInquiry)
                ),
                escapeHtml(
                        contactInquiry.getReferenceNumber()
                ),
                escapeHtml(
                        resolveSubject(contactInquiry)
                ),
                escapeHtml(
                        resolveServiceType(contactInquiry)
                ),
                escapeHtml(
                        resolvePreferredContactMethod(
                                contactInquiry
                        )
                ),
                escapeHtml(
                        resolveSubmittedTime(contactInquiry)
                )
        );
    }

    private String buildInquirySubmittedSms(
            ContactInquiry contactInquiry
    ) {
        return "Romelt TechCare received your inquiry "
                + contactInquiry.getReferenceNumber()
                + ". Our team will review it and contact you with the next step.";
    }

    private String buildAdministratorSubmittedMessage(
            ContactInquiry contactInquiry
    ) {
        return "New contact inquiry "
                + contactInquiry.getReferenceNumber()
                + " from "
                + resolveCustomerName(contactInquiry)
                + " regarding "
                + resolveSubject(contactInquiry)
                + " requires attention.";
    }

    // ================================================================
    // STATUS MESSAGES
    // ================================================================

    private String resolveStatusEmailTitle(
            ContactInquiry contactInquiry,
            ContactInquiryStatus status
    ) {
        return switch (status) {
            case NEW ->
                    "Inquiry Received — "
                            + contactInquiry.getReferenceNumber();

            case IN_PROGRESS ->
                    "Inquiry Under Review — "
                            + contactInquiry.getReferenceNumber();

            case RESPONDED ->
                    "Response to Your Inquiry — "
                            + contactInquiry.getReferenceNumber();

            case CLOSED ->
                    "Inquiry Closed — "
                            + contactInquiry.getReferenceNumber();

            case SPAM ->
                    throw new IllegalArgumentException(
                            "Customer email is not created for a spam inquiry."
                    );
        };
    }

    private String buildStatusText(
            ContactInquiry contactInquiry,
            ContactInquiryStatus status,
            String customerVisibleMessage
    ) {
        String customerName =
                resolveCustomerName(contactInquiry);

        String referenceNumber =
                contactInquiry.getReferenceNumber();

        return switch (status) {
            case NEW ->
                    """
                    Hello %s,

                    We received your inquiry %s.

                    Our team will review your message and contact you using your preferred contact method.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber
                    );

            case IN_PROGRESS ->
                    """
                    Hello %s,

                    Your inquiry %s is currently being reviewed.

                    %s

                    No action is required unless our team contacts you for additional information.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    customerVisibleMessage,
                                    "Our team is reviewing the information you provided."
                            )
                    );

            case RESPONDED ->
                    """
                    Hello %s,

                    Romelt TechCare has responded to your inquiry %s.

                    Response:
                    %s

                    Review this response and contact us if you need additional assistance.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    customerVisibleMessage,
                                    "Our team has reviewed your inquiry and provided an update."
                            )
                    );

            case CLOSED ->
                    """
                    Hello %s,

                    Your inquiry %s has been closed.

                    %s

                    Submit a new inquiry if you need additional assistance.

                    Romelt TechCare
                    """.formatted(
                            customerName,
                            referenceNumber,
                            resolveOptionalValue(
                                    customerVisibleMessage,
                                    "No further action is currently required."
                            )
                    );

            case SPAM ->
                    throw new IllegalArgumentException(
                            "Customer notification content is not created for a spam inquiry."
                    );
        };
    }

    private String buildStatusHtml(
            ContactInquiry contactInquiry,
            ContactInquiryStatus status,
            String customerVisibleMessage
    ) {
        String heading =
                switch (status) {
                    case NEW ->
                            "Inquiry Received";

                    case IN_PROGRESS ->
                            "Inquiry Under Review";

                    case RESPONDED ->
                            "Response Available";

                    case CLOSED ->
                            "Inquiry Closed";

                    case SPAM ->
                            throw new IllegalArgumentException(
                                    "Customer HTML is not created for a spam inquiry."
                            );
                };

        String body =
                buildStatusText(
                        contactInquiry,
                        status,
                        customerVisibleMessage
                );

        return """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <h2>%s</h2>

                    <p>
                        <strong>Reference number:</strong> %s
                    </p>

                    <p>%s</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(heading),
                escapeHtml(
                        contactInquiry.getReferenceNumber()
                ),
                escapeHtml(body)
                        .replace(
                                "\n",
                                "<br>"
                        )
        );
    }

    private String buildStatusSms(
            ContactInquiry contactInquiry,
            ContactInquiryStatus status
    ) {
        String reference =
                contactInquiry.getReferenceNumber();

        return switch (status) {
            case NEW ->
                    "Romelt TechCare received inquiry "
                            + reference
                            + ". We will contact you with the next step.";

            case IN_PROGRESS ->
                    "Romelt TechCare: Inquiry "
                            + reference
                            + " is now under review.";

            case RESPONDED ->
                    "Romelt TechCare responded to inquiry "
                            + reference
                            + ". Check your email or preferred contact method for details.";

            case CLOSED ->
                    "Romelt TechCare: Inquiry "
                            + reference
                            + " has been closed. Contact us if you need more help.";

            case SPAM ->
                    throw new IllegalArgumentException(
                            "Customer SMS is not created for a spam inquiry."
                    );
        };
    }

    private String buildAdministratorStatusMessage(
            ContactInquiry contactInquiry,
            ContactInquiryStatus previousStatus,
            ContactInquiryStatus currentStatus
    ) {
        if (currentStatus == ContactInquiryStatus.SPAM) {
            return "Contact inquiry "
                    + contactInquiry.getReferenceNumber()
                    + " from "
                    + resolveCustomerName(contactInquiry)
                    + " was marked as spam.";
        }

        return "Contact inquiry "
                + contactInquiry.getReferenceNumber()
                + " changed from "
                + createStatusDisplayName(previousStatus)
                + " to "
                + createStatusDisplayName(currentStatus)
                + ".";
    }

    // ================================================================
    // VALUE RESOLUTION
    // ================================================================

    /**
     * Returns the linked customer ID when the ContactInquiry entity
     * supports customer integration.
     *
     * Replace the null return with contactInquiry.getCustomerId() when
     * that field exists on your current entity.
     */
    private java.util.UUID resolveCustomerId(
            ContactInquiry contactInquiry
    ) {
        return null;
    }

    private String resolveCustomerName(
            ContactInquiry contactInquiry
    ) {
        return resolveOptionalValue(
                contactInquiry.getFullName(),
                "Customer"
        );
    }

    private String resolveSubject(
            ContactInquiry contactInquiry
    ) {
        return resolveOptionalValue(
                contactInquiry.getSubject(),
                "General inquiry"
        );
    }

    private String resolveServiceType(
            ContactInquiry contactInquiry
    ) {
        Object serviceType =
                contactInquiry.getServiceType();

        if (serviceType == null) {
            return "Technology service";
        }

        return createDisplayName(
                serviceType.toString()
        );
    }

    private String resolvePreferredContactMethod(
            ContactInquiry contactInquiry
    ) {
        Object preferredContactMethod =
                contactInquiry.getPreferredContactMethod();

        if (preferredContactMethod == null) {
            return "Email or telephone";
        }

        return createDisplayName(
                preferredContactMethod.toString()
        );
    }

    private String resolveSubmittedTime(
            ContactInquiry contactInquiry
    ) {
        Instant submittedAt =
                contactInquiry.getSubmittedAt();

        if (submittedAt == null) {
            submittedAt =
                    contactInquiry.getCreatedAt();
        }

        if (submittedAt == null) {
            return "Recently";
        }

        return DATE_TIME_FORMATTER.format(
                submittedAt.atZone(
                        ZoneId.of(DEFAULT_TIMEZONE)
                )
        );
    }

    private String createStatusDisplayName(
            ContactInquiryStatus status
    ) {
        if (status == null) {
            return "Unknown";
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

    // ================================================================
    // VALIDATION AND NORMALIZATION
    // ================================================================

    private void requirePersistedInquiry(
            ContactInquiry contactInquiry
    ) {
        if (
                contactInquiry == null
                        || contactInquiry.getContactInquiryId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted contact inquiry is required."
            );
        }

        if (
                normalizeOptional(
                        contactInquiry.getReferenceNumber()
                ) == null
        ) {
            throw new IllegalArgumentException(
                    "Contact inquiry reference number is required."
            );
        }

        if (contactInquiry.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry status is required."
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

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}