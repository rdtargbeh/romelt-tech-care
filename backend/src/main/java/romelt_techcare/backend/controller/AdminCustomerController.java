package romelt_techcare.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.CustomerCreateRequest;
import romelt_techcare.backend.dto.CustomerMergeRequest;
import romelt_techcare.backend.dto.CustomerResponse;
import romelt_techcare.backend.dto.CustomerSummaryResponse;
import romelt_techcare.backend.dto.CustomerUpdateRequest;
import romelt_techcare.backend.enums.CustomerStatus;
import romelt_techcare.backend.service.CustomerService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for managing the
 * reusable Romelt TechCare customer directory.
 *
 * Responsibilities:
 * - Creates customers manually through the administrator portal.
 * - Returns paginated customer lists.
 * - Searches customers by name, customer number, email, or phone.
 * - Filters customers by operational status.
 * - Returns one complete customer profile.
 * - Updates current customer contact information.
 * - Archives and restores customer profiles.
 * - Soft-deletes customer profiles.
 * - Merges duplicate customers into a surviving customer.
 *
 * Customer creation paths:
 *
 * AUTOMATIC:
 * Public and administrator booking submissions call CustomerService
 * internally to resolve or create a customer. Those operations do not
 * use this controller.
 *
 * MANUAL:
 * An authenticated administrator may create a customer through this
 * controller for:
 * - telephone requests;
 * - walk-in customers;
 * - referrals;
 * - imported contacts;
 * - customers created before a booking exists.
 *
 * Business-booking rule:
 * Customer represents the reusable individual contact person.
 * Business information remains attached to each BookingRequest as a
 * historical snapshot.
 *
 * Security:
 * - Every endpoint requires administrator authentication through the
 *   existing Spring Security configuration.
 * - Internal notes and communication notes must never be exposed
 *   through public endpoints.
 * - No public customer-management controller should be created.
 *
 * Base path:
 * /api/v1/admin/customers
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerService customerService;

    /**
     * Creates a reusable customer manually.
     *
     * Endpoint:
     * POST /api/v1/admin/customers
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            CustomerCreateRequest request
    ) {
        CustomerResponse response =
                customerService.createCustomer(
                        principal,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Returns a paginated administrator customer list.
     *
     * Supported filters:
     * - keyword
     * - customerStatus
     *
     * Keyword searches:
     * - customer number;
     * - display name;
     * - preferred name;
     * - email;
     * - phone.
     *
     * Endpoint:
     * GET /api/v1/admin/customers
     */
    @GetMapping
    public ResponseEntity<Page<CustomerSummaryResponse>> getCustomers(
            @RequestParam(
                    required = false
            )
            String keyword,

            @RequestParam(
                    required = false
            )
            CustomerStatus customerStatus,

            Pageable pageable
    ) {
        Page<CustomerSummaryResponse> response =
                customerService.getCustomers(
                        keyword,
                        customerStatus,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns one complete customer profile.
     *
     * Endpoint:
     * GET /api/v1/admin/customers/{customerId}
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable
            UUID customerId
    ) {
        CustomerResponse response =
                customerService.getCustomer(
                        customerId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the reusable customer profile.
     *
     * Existing booking snapshots are not modified.
     *
     * Endpoint:
     * PUT /api/v1/admin/customers/{customerId}
     */
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerId,

            @Valid
            @RequestBody
            CustomerUpdateRequest request
    ) {
        CustomerResponse response =
                customerService.updateCustomer(
                        principal,
                        customerId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Archives a customer profile.
     *
     * Archived customers remain available for historical reporting
     * but are not treated as normal active customer records.
     *
     * Endpoint:
     * POST /api/v1/admin/customers/{customerId}/archive
     */
    @PostMapping("/{customerId}/archive")
    public ResponseEntity<CustomerResponse> archiveCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerId
    ) {
        CustomerResponse response =
                customerService.archiveCustomer(
                        principal,
                        customerId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Restores an archived customer.
     *
     * Endpoint:
     * POST /api/v1/admin/customers/{customerId}/restore
     */
    @PostMapping("/{customerId}/restore")
    public ResponseEntity<CustomerResponse> restoreCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerId
    ) {
        CustomerResponse response =
                customerService.restoreCustomer(
                        principal,
                        customerId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a customer.
     *
     * Historical bookings and related records are retained.
     *
     * Endpoint:
     * DELETE /api/v1/admin/customers/{customerId}
     */
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerId
    ) {
        customerService.deleteCustomer(
                principal,
                customerId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Merges a duplicate customer into a surviving customer.
     *
     * The customer identified by customerId is the duplicate record.
     * The request identifies the surviving customer.
     *
     * Endpoint:
     * POST /api/v1/admin/customers/{customerId}/merge
     */
    @PostMapping("/{customerId}/merge")
    public ResponseEntity<CustomerResponse> mergeCustomer(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerId,

            @Valid
            @RequestBody
            CustomerMergeRequest request
    ) {
        CustomerResponse response =
                customerService.mergeCustomer(
                        principal,
                        customerId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}