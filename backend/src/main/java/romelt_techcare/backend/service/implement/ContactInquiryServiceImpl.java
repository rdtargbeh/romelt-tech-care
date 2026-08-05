package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.ContactInquiryConfirmationResponse;
import romelt_techcare.backend.dto.ContactInquiryCreateRequest;
import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
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
 * - Generates a customer-facing reference number.
 * - Normalizes and stores accepted inquiries.
 * - Returns a safe confirmation response.
 * - Records an immutable content audit event.
 * - Provides future integration points for notifications and spam
 *   processing.
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

    private static final String REFERENCE_PREFIX = "RTCI";
    private static final int MAX_REFERENCE_ATTEMPTS = 10;

    private final ContactInquiryRepository
            contactInquiryRepository;

    private final ContactInquiryMapper
            contactInquiryMapper;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Override
    @Transactional
    public ContactInquiryConfirmationResponse createInquiry(
            ContactInquiryCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry information is required."
            );
        }

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

        JsonNode afterSnapshot =
                createInquirySnapshot(savedInquiry);

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

        log.info(
                "Public contact inquiry created. inquiryId={}, referenceNumber={}",
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber()
        );

        /*
         * Future production integrations:
         * - Notify the business owner by email.
         * - Send a customer acknowledgement email.
         * - Run anti-spam and rate-limit checks.
         */

        return contactInquiryMapper
                .toConfirmationResponse(savedInquiry);
    }

    private JsonNode createInquirySnapshot(
            ContactInquiry inquiry
    ) {
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

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

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
                            .existsByReferenceNumber(referenceNumber)
            ) {
                return referenceNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique contact inquiry reference number."
        );
    }

    private String createReferenceNumber() {
        String randomSegment = Long
                .toUnsignedString(
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
                    ).replace(' ', '0');
        }

        randomSegment =
                randomSegment.substring(0, 8);

        return "%s-%d-%s".formatted(
                REFERENCE_PREFIX,
                Year.now().getValue(),
                randomSegment
        );
    }
}