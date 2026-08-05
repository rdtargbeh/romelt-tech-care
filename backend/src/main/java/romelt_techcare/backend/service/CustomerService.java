package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.CustomerCreateRequest;
import romelt_techcare.backend.dto.CustomerMergeRequest;
import romelt_techcare.backend.dto.CustomerResolutionResult;
import romelt_techcare.backend.dto.CustomerResponse;
import romelt_techcare.backend.dto.CustomerSummaryResponse;
import romelt_techcare.backend.dto.CustomerUpdateRequest;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.CustomerStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines reusable customer-management and customer-resolution
 * operations.
 *
 * Customer creation paths:
 * 1. BOOKING
 *    - The booking service calls resolveOrCreateFromBooking().
 *    - An existing customer is reused by normalized email or phone.
 *    - A new customer is created only when no matching customer
 *      exists.
 *
 * 2. ADMINISTRATOR
 *    - An authenticated administrator manually creates a customer.
 *
 * Customer matching:
 * - Email and telephone are matched independently.
 * - Names are never used as the sole automatic matching key.
 * - If email and phone resolve to different customers, automatic
 *   resolution stops and reports a conflict.
 *
 * Business bookings:
 * Customer represents the individual contact person.
 * Business information remains on BookingRequest as a historical
 * snapshot.
 * ================================================================
 */
public interface CustomerService {

    /**
     * Resolves or creates the reusable customer linked to a booking.
     *
     * This method is called internally by BookingRequestServiceImpl.
     */
    CustomerResolutionResult resolveOrCreateFromBooking(
            BookingRequest bookingRequest
    );

    /**
     * Creates a customer manually through the administrator portal.
     */
    CustomerResponse createCustomer(
            AdminJwtPrincipal principal,
            CustomerCreateRequest request
    );

    /**
     * Returns a filtered administrator customer list.
     */
    Page<CustomerSummaryResponse> getCustomers(
            String keyword,
            CustomerStatus customerStatus,
            Pageable pageable
    );

    /**
     * Returns one complete customer record.
     */
    CustomerResponse getCustomer(
            UUID customerId
    );

    /**
     * Updates the current reusable customer profile.
     */
    CustomerResponse updateCustomer(
            AdminJwtPrincipal principal,
            UUID customerId,
            CustomerUpdateRequest request
    );

    /**
     * Archives a customer.
     */
    CustomerResponse archiveCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    );

    /**
     * Restores an archived customer.
     */
    CustomerResponse restoreCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    );

    /**
     * Soft-deletes a customer.
     */
    void deleteCustomer(
            AdminJwtPrincipal principal,
            UUID customerId
    );

    /**
     * Merges a duplicate customer into a surviving customer.
     */
    CustomerResponse mergeCustomer(
            AdminJwtPrincipal principal,
            UUID duplicateCustomerId,
            CustomerMergeRequest request
    );
}