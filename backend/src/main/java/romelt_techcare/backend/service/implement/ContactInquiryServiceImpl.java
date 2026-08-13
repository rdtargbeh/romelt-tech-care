package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.ContactInquiryConfirmationResponse;
import romelt_techcare.backend.dto.ContactInquiryCreateRequest;
import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.event.ContactInquirySubmittedEvent;
import romelt_techcare.backend.mapper.ContactInquiryMapper;
import romelt_techcare.backend.repository.ContactInquiryRepository;
import romelt_techcare.backend.service.ContactInquiryService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.security.SecureRandom;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Processes and stores public contact inquiries.
 *
 * Responsibilities:
 * - Validates incoming public inquiry requests.
 * - Generates a unique customer-facing reference number.
 * - Normalizes and stores accepted inquiries.
 * - Records an immutable content audit event.
 * - Publishes a contact-inquiry-submitted application event.
 * - Returns a safe public confirmation response.
 *
 * Notification behavior:
 * This service does not send email, SMS, or in-app notifications
 * directly.
 *
 * After the inquiry transaction commits successfully,
 * ContactNotificationListener handles ContactInquirySubmittedEvent
 * and delegates notification delivery to ContactNotificationService.
 *
 * This prevents email or SMS provider failures from rolling back a
 * valid customer inquiry.
 *
 * Reference format:
 * RTCI-YYYY-XXXXXXXX
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactInquiryServiceImpl
        implements ContactInquiryService {

    private static final String REFERENCE_PREFIX =
            "RTCI";

    private static final int MAX_REFERENCE_ATTEMPTS =
            10;

    private final ContactInquiryRepository
            contactInquiryRepository;

    private final ContactInquiryMapper
            contactInquiryMapper;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    private final ApplicationEventPublisher
            applicationEventPublisher;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Creates a public contact inquiry.
     *
     * Processing order:
     * 1. Validate the incoming request.
     * 2. Generate a unique inquiry reference.
     * 3. Map and persist the inquiry.
     * 4. Record an immutable audit entry.
     * 5. Publish the submitted event.
     * 6. Return the customer confirmation response.
     *
     * The notification listener executes after transaction commit.
     */
    @Override
    @Transactional
    public ContactInquiryConfirmationResponse createInquiry(
            ContactInquiryCreateRequest request
    ) {
        requireCreateRequest(request);

        String referenceNumber =
                generateUniqueReferenceNumber();

        ContactInquiry inquiry =
                contactInquiryMapper.toEntity(
                        request,
                        referenceNumber
                );

        ContactInquiry savedInquiry =
                contactInquiryRepository.saveAndFlush(
                        inquiry
                );

        requirePersistedInquiry(savedInquiry);

        JsonNode afterSnapshot =
                createInquirySnapshot(
                        savedInquiry
                );

        websiteContentAuditLogService.recordAudit(
                null,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.CONTACT_INQUIRY,
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber(),
                null,
                afterSnapshot,
                "Public contact inquiry submitted.",
                null
        );

        /*
         * Published inside the active transaction.
         *
         * ContactNotificationListener uses:
         * @TransactionalEventListener(AFTER_COMMIT)
         *
         * Therefore, notifications are processed only when this
         * transaction commits successfully.
         */
        applicationEventPublisher.publishEvent(
                new ContactInquirySubmittedEvent(
                        savedInquiry
                )
        );

        log.info(
                "Public contact inquiry created and notification event published. inquiryId={}, referenceNumber={}, status={}",
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber(),
                savedInquiry.getStatus()
        );

        return contactInquiryMapper
                .toConfirmationResponse(
                        savedInquiry
                );
    }

    /**
     * Creates a non-sensitive audit snapshot.
     *
     * The complete customer message, email address, telephone number,
     * IP address, and request metadata are intentionally excluded.
     */
    private JsonNode createInquirySnapshot(
            ContactInquiry inquiry
    ) {
        requirePersistedInquiry(inquiry);

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "contactInquiryId",
                inquiry.getContactInquiryId()
        );

        fields.put(
                "referenceNumber",
                inquiry.getReferenceNumber()
        );

        fields.put(
                "status",
                inquiry.getStatus()
        );

        fields.put(
                "subject",
                inquiry.getSubject()
        );

        fields.put(
                "serviceType",
                inquiry.getServiceType()
        );

        fields.put(
                "preferredContactMethod",
                inquiry.getPreferredContactMethod()
        );

        fields.put(
                "submittedAt",
                inquiry.getSubmittedAt()
        );

        fields.put(
                "createdAt",
                inquiry.getCreatedAt()
        );

        fields.put(
                "updatedAt",
                inquiry.getUpdatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    /**
     * Generates a unique customer-facing inquiry reference number.
     */
    private String generateUniqueReferenceNumber() {
        for (
                int attempt = 0;
                attempt < MAX_REFERENCE_ATTEMPTS;
                attempt++
        ) {
            String referenceNumber =
                    createReferenceNumber();

            if (
                    !contactInquiryRepository
                            .existsByReferenceNumber(
                                    referenceNumber
                            )
            ) {
                return referenceNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique contact inquiry reference number."
        );
    }

    /**
     * Creates one candidate inquiry reference.
     */
    private String createReferenceNumber() {
        String randomSegment =
                Long.toUnsignedString(
                                secureRandom.nextLong(),
                                36
                        )
                        .toUpperCase(Locale.ROOT)
                        .replace("-", "");

        if (randomSegment.length() < 8) {
            randomSegment =
                    String.format(
                                    "%8s",
                                    randomSegment
                            )
                            .replace(' ', '0');
        }

        randomSegment =
                randomSegment.substring(
                        0,
                        8
                );

        return "%s-%d-%s".formatted(
                REFERENCE_PREFIX,
                Year.now().getValue(),
                randomSegment
        );
    }

    /**
     * Validates the incoming public create command.
     */
    private void requireCreateRequest(
            ContactInquiryCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry information is required."
            );
        }
    }

    /**
     * Ensures notification and audit operations receive a persisted
     * inquiry.
     */
    private void requirePersistedInquiry(
            ContactInquiry inquiry
    ) {
        if (
                inquiry == null
                        || inquiry.getContactInquiryId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted contact inquiry is required."
            );
        }

        if (
                normalizeOptional(
                        inquiry.getReferenceNumber()
                ) == null
        ) {
            throw new IllegalArgumentException(
                    "Contact inquiry reference number is required."
            );
        }

        if (inquiry.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry status is required."
            );
        }
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