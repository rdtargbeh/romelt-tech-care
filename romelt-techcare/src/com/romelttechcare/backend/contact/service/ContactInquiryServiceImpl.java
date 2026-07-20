package com.romelttechcare.backend.contact.service;

import com.romelttechcare.backend.contact.dto.ContactInquiryConfirmationResponse;
import com.romelttechcare.backend.contact.dto.ContactInquiryCreateRequest;
import com.romelttechcare.backend.contact.entity.ContactInquiry;
import com.romelttechcare.backend.contact.mapper.ContactInquiryMapper;
import com.romelttechcare.backend.contact.repository.ContactInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Year;
import java.util.Locale;

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
 * - Provides a future integration point for email notifications,
 *   spam checks, audit logging, and administrative assignments.
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

    private final ContactInquiryRepository contactInquiryRepository;
    private final ContactInquiryMapper contactInquiryMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ContactInquiryConfirmationResponse createInquiry(
            ContactInquiryCreateRequest request
    ) {
        String referenceNumber =
                generateUniqueReferenceNumber();

        ContactInquiry inquiry =
                contactInquiryMapper.toEntity(
                        request,
                        referenceNumber
                );

        ContactInquiry savedInquiry =
                contactInquiryRepository.save(inquiry);

        log.info(
                "Public contact inquiry created. inquiryId={}, referenceNumber={}",
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber()
        );

        /*
         * Future production integrations:
         * - Notify the business owner by email.
         * - Send a customer acknowledgement email.
         * - Record an audit or communication event.
         * - Run anti-spam and rate-limit checks.
         */

        return contactInquiryMapper
                .toConfirmationResponse(savedInquiry);
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