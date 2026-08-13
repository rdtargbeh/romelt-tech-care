//package romelt_techcare.backend.repository;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//import romelt_techcare.backend.entity.Customer;
//import romelt_techcare.backend.enums.CustomerStatus;
//
//import java.util.Optional;
//import java.util.UUID;
//
///**
// * ================================================================
// * ROMELT TECHCARE — CUSTOMER REPOSITORY
// * ================================================================
// *
// * Purpose:
// * Provides persistence, matching, searching, and lifecycle access for
// * reusable customer/contact profiles.
// *
// * Responsibilities:
// * - Persists customer records.
// * - Checks customer-number uniqueness.
// * - Resolves active customers by normalized email.
// * - Resolves active customers by normalized telephone number.
// * - Supports conflict detection when email and phone identify
// *   different customer records.
// * - Supports administrator customer lists and keyword search.
// * - Excludes merged and soft-deleted records from normal resolution.
// *
// * Customer resolution:
// * Booking and inquiry services must search independently by:
// * 1. normalized email;
// * 2. normalized telephone number.
// *
// * If both values resolve to different customers, the service must
// * reject automatic resolution rather than merging by name.
// * ================================================================
// */
//@Repository
//public interface CustomerRepository
//        extends JpaRepository<Customer, UUID> {
//
//    boolean existsByCustomerNumber(
//            String customerNumber
//    );
//
//    /**
//     * Finds a usable, non-merged, non-deleted customer by normalized
//     * email.
//     */
//    @Query("""
//            select c
//            from Customer c
//            where c.normalizedEmail = :normalizedEmail
//              and c.deletedAt is null
//              and c.customerStatus not in (
//                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
//                    romelt_techcare.backend.enums.CustomerStatus.DELETED
//              )
//            """)
//    Optional<Customer> findUsableByNormalizedEmail(
//            @Param("normalizedEmail")
//            String normalizedEmail
//    );
//
//    /**
//     * Finds a usable, non-merged, non-deleted customer by normalized
//     * telephone number.
//     */
//    @Query("""
//            select c
//            from Customer c
//            where c.normalizedPhone = :normalizedPhone
//              and c.deletedAt is null
//              and c.customerStatus not in (
//                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
//                    romelt_techcare.backend.enums.CustomerStatus.DELETED
//              )
//            """)
//    Optional<Customer> findUsableByNormalizedPhone(
//            @Param("normalizedPhone")
//            String normalizedPhone
//    );
//
//    /**
//     * Finds a normal administrator-visible customer by ID.
//     *
//     * Merged and soft-deleted records are excluded.
//     */
//    @Query("""
//            select c
//            from Customer c
//            where c.customerId = :customerId
//              and c.deletedAt is null
//              and c.customerStatus <> romelt_techcare.backend.enums.CustomerStatus.MERGED
//            """)
//    Optional<Customer> findActiveRecordById(
//            @Param("customerId")
//            UUID customerId
//    );
//
//    /**
//     * Finds a customer including archived, blocked, inactive, and
//     * merged records, but excluding soft-deleted records.
//     */
//    Optional<Customer> findByCustomerIdAndDeletedAtIsNull(
//            UUID customerId
//    );
//
//    /**
//     * Finds the customer-number record excluding one existing
//     * customer.
//     */
//    boolean existsByCustomerNumberAndCustomerIdNot(
//            String customerNumber,
//            UUID customerId
//    );
//
//    /**
//     * Checks whether another usable customer already owns the
//     * normalized email.
//     */
//    @Query("""
//            select case when count(c) > 0 then true else false end
//            from Customer c
//            where c.normalizedEmail = :normalizedEmail
//              and c.customerId <> :excludedCustomerId
//              and c.deletedAt is null
//              and c.customerStatus not in (
//                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
//                    romelt_techcare.backend.enums.CustomerStatus.DELETED
//              )
//            """)
//    boolean existsUsableByNormalizedEmailAndCustomerIdNot(
//            @Param("normalizedEmail")
//            String normalizedEmail,
//            @Param("excludedCustomerId")
//            UUID excludedCustomerId
//    );
//
//    /**
//     * Checks whether another usable customer already owns the
//     * normalized telephone number.
//     */
//    @Query("""
//            select case when count(c) > 0 then true else false end
//            from Customer c
//            where c.normalizedPhone = :normalizedPhone
//              and c.customerId <> :excludedCustomerId
//              and c.deletedAt is null
//              and c.customerStatus not in (
//                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
//                    romelt_techcare.backend.enums.CustomerStatus.DELETED
//              )
//            """)
//    boolean existsUsableByNormalizedPhoneAndCustomerIdNot(
//            @Param("normalizedPhone")
//            String normalizedPhone,
//            @Param("excludedCustomerId")
//            UUID excludedCustomerId
//    );
//
//    /**
//     * Returns administrator customer records with optional keyword and
//     * status filters.
//     *
//     * Keyword searches:
//     * - customer number;
//     * - display name;
//     * - preferred name;
//     * - primary email;
//     * - primary telephone number.
//     *
//     * Soft-deleted and merged records are excluded from the normal
//     * customer list.
//     */
//    @Query("""
//            select c
//            from Customer c
//            where c.deletedAt is null
//              and c.customerStatus <> romelt_techcare.backend.enums.CustomerStatus.MERGED
//              and (
//                    :customerStatus is null
//                    or c.customerStatus = :customerStatus
//              )
//              and (
//                    :keyword is null
//                    or lower(c.customerNumber) like lower(concat('%', :keyword, '%'))
//                    or lower(c.displayName) like lower(concat('%', :keyword, '%'))
//                    or lower(coalesce(c.preferredName, '')) like lower(concat('%', :keyword, '%'))
package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.enums.CustomerStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, matching, searching, and lifecycle access for
 * reusable customer/contact profiles.
 *
 * Responsibilities:
 * - Persists customer records.
 * - Checks customer-number uniqueness.
 * - Resolves active customers by normalized email.
 * - Resolves active customers by normalized telephone number.
 * - Supports conflict detection when email and phone identify
 *   different customer records.
 * - Supports administrator customer lists and keyword search.
 * - Excludes merged and soft-deleted records from normal resolution.
 *
 * Customer resolution:
 * Booking and inquiry services must search independently by:
 * 1. normalized email;
 * 2. normalized telephone number.
 *
 * If both values resolve to different customers, the service must
 * reject automatic resolution rather than merging by name.
 *
 * PostgreSQL keyword-search note:
 * Nullable Hibernate query parameters can otherwise be inferred by
 * PostgreSQL as bytea in expressions such as lower(:keyword).
 *
 * The administrator search query therefore explicitly casts the
 * keyword parameter to String before applying lower(), ensuring that
 * PostgreSQL evaluates lower(varchar/text) rather than lower(bytea).
 * ================================================================
 */
@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, UUID> {

    // =====================================================================
    // CUSTOMER NUMBER
    // =====================================================================

    boolean existsByCustomerNumber(
            String customerNumber
    );

    // =====================================================================
    // NORMALIZED EMAIL RESOLUTION
    // =====================================================================

    /**
     * Finds a usable, non-merged, non-deleted customer by normalized
     * email.
     */
    @Query("""
            select c
            from Customer c
            where c.normalizedEmail = :normalizedEmail
              and c.deletedAt is null
              and c.customerStatus not in (
                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
                    romelt_techcare.backend.enums.CustomerStatus.DELETED
              )
            """)
    Optional<Customer> findUsableByNormalizedEmail(
            @Param("normalizedEmail")
            String normalizedEmail
    );

    // =====================================================================
    // NORMALIZED PHONE RESOLUTION
    // =====================================================================

    /**
     * Finds a usable, non-merged, non-deleted customer by normalized
     * telephone number.
     */
    @Query("""
            select c
            from Customer c
            where c.normalizedPhone = :normalizedPhone
              and c.deletedAt is null
              and c.customerStatus not in (
                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
                    romelt_techcare.backend.enums.CustomerStatus.DELETED
              )
            """)
    Optional<Customer> findUsableByNormalizedPhone(
            @Param("normalizedPhone")
            String normalizedPhone
    );

    // =====================================================================
    // CUSTOMER BY ID
    // =====================================================================

    /**
     * Finds a normal administrator-visible customer by ID.
     *
     * Merged and soft-deleted records are excluded.
     */
    @Query("""
            select c
            from Customer c
            where c.customerId = :customerId
              and c.deletedAt is null
              and c.customerStatus <> romelt_techcare.backend.enums.CustomerStatus.MERGED
            """)
    Optional<Customer> findActiveRecordById(
            @Param("customerId")
            UUID customerId
    );

    /**
     * Finds a customer including archived, blocked, inactive, and
     * merged records, but excluding soft-deleted records.
     */
    Optional<Customer> findByCustomerIdAndDeletedAtIsNull(
            UUID customerId
    );

    // =====================================================================
    // CUSTOMER NUMBER CONFLICT
    // =====================================================================

    /**
     * Finds the customer-number record excluding one existing
     * customer.
     */
    boolean existsByCustomerNumberAndCustomerIdNot(
            String customerNumber,
            UUID customerId
    );

    // =====================================================================
    // EMAIL CONFLICT
    // =====================================================================

    /**
     * Checks whether another usable customer already owns the
     * normalized email.
     */
    @Query("""
            select case when count(c) > 0 then true else false end
            from Customer c
            where c.normalizedEmail = :normalizedEmail
              and c.customerId <> :excludedCustomerId
              and c.deletedAt is null
              and c.customerStatus not in (
                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
                    romelt_techcare.backend.enums.CustomerStatus.DELETED
              )
            """)
    boolean existsUsableByNormalizedEmailAndCustomerIdNot(
            @Param("normalizedEmail")
            String normalizedEmail,

            @Param("excludedCustomerId")
            UUID excludedCustomerId
    );

    // =====================================================================
    // PHONE CONFLICT
    // =====================================================================

    /**
     * Checks whether another usable customer already owns the
     * normalized telephone number.
     */
    @Query("""
            select case when count(c) > 0 then true else false end
            from Customer c
            where c.normalizedPhone = :normalizedPhone
              and c.customerId <> :excludedCustomerId
              and c.deletedAt is null
              and c.customerStatus not in (
                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
                    romelt_techcare.backend.enums.CustomerStatus.DELETED
              )
            """)
    boolean existsUsableByNormalizedPhoneAndCustomerIdNot(
            @Param("normalizedPhone")
            String normalizedPhone,

            @Param("excludedCustomerId")
            UUID excludedCustomerId
    );

    // =====================================================================
    // ADMINISTRATOR CUSTOMER SEARCH
    // =====================================================================

    /**
     * Returns administrator customer records with optional keyword and
     * status filters.
     *
     * Keyword searches:
     * - customer number;
     * - display name;
     * - preferred name;
     * - primary email;
     * - primary telephone number.
     *
     * Soft-deleted and merged records are excluded from the normal
     * customer list.
     *
     * PostgreSQL:
     * The keyword parameter is explicitly cast to String before lower()
     * is applied. This prevents PostgreSQL from treating a nullable
     * Hibernate parameter as bytea and producing:
     *
     * function lower(bytea) does not exist
     */
    @Query("""
            select c
            from Customer c
            where c.deletedAt is null
              and c.customerStatus <> romelt_techcare.backend.enums.CustomerStatus.MERGED
              and (
                    :customerStatus is null
                    or c.customerStatus = :customerStatus
              )
              and (
                    :keyword is null
                    or lower(c.customerNumber) like concat(
                        '%',
                        lower(cast(:keyword as string)),
                        '%'
                    )
                    or lower(c.displayName) like concat(
                        '%',
                        lower(cast(:keyword as string)),
                        '%'
                    )
                    or lower(coalesce(c.preferredName, '')) like concat(
                        '%',
                        lower(cast(:keyword as string)),
                        '%'
                    )
                    or lower(coalesce(c.primaryEmail, '')) like concat(
                        '%',
                        lower(cast(:keyword as string)),
                        '%'
                    )
                    or lower(coalesce(c.primaryPhone, '')) like concat(
                        '%',
                        lower(cast(:keyword as string)),
                        '%'
                    )
              )
            """)
    Page<Customer> searchCustomers(
            @Param("keyword")
            String keyword,

            @Param("customerStatus")
            CustomerStatus customerStatus,

            Pageable pageable
    );

    // =====================================================================
    // CUSTOMER SELECTOR
    // =====================================================================

    /**
     * Returns all normal customer records for selectors when no search
     * filter is needed.
     */
    @Query("""
            select c
            from Customer c
            where c.deletedAt is null
              and c.customerStatus not in (
                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
                    romelt_techcare.backend.enums.CustomerStatus.DELETED
              )
            """)
    Page<Customer> findAllUsableCustomers(
            Pageable pageable
    );
}




//                    or lower(coalesce(c.primaryEmail, '')) like lower(concat('%', :keyword, '%'))
//                    or lower(coalesce(c.primaryPhone, '')) like lower(concat('%', :keyword, '%'))
//              )
//            """)
//    Page<Customer> searchCustomers(
//            @Param("keyword")
//            String keyword,
//            @Param("customerStatus")
//            CustomerStatus customerStatus,
//            Pageable pageable
//    );
//
//    /**
//     * Returns all normal customer records for selectors when no search
//     * filter is needed.
//     */
//    @Query("""
//            select c
//            from Customer c
//            where c.deletedAt is null
//              and c.customerStatus not in (
//                    romelt_techcare.backend.enums.CustomerStatus.MERGED,
//                    romelt_techcare.backend.enums.CustomerStatus.DELETED
//              )
//            """)
//    Page<Customer> findAllUsableCustomers(
//            Pageable pageable
//    );
//}