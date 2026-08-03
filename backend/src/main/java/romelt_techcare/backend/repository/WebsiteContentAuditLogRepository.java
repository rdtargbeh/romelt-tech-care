package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteContentAuditLog;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT LOG REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides read and append persistence operations for immutable CMS
 * audit records.
 *
 * Restrictions:
 * Audit-log deletion and update methods must not be exposed through
 * the service or controller.
 * ================================================================
 */
@Repository
public interface WebsiteContentAuditLogRepository
        extends JpaRepository<WebsiteContentAuditLog, UUID> {

    @EntityGraph(attributePaths = "adminUser")
    Optional<WebsiteContentAuditLog>
    findByContentAuditLogId(
            UUID contentAuditLogId
    );

    @EntityGraph(attributePaths = "adminUser")
    @Query("""
            select auditLog
            from WebsiteContentAuditLog auditLog
            where (
                    :keyword is null
                    or lower(coalesce(auditLog.resourceName, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(auditLog.changeSummary, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :adminUserId is null
                    or auditLog.adminUser.adminUserId = :adminUserId
                  )
              and (
                    :action is null
                    or auditLog.action = :action
                  )
              and (
                    :resourceType is null
                    or auditLog.resourceType = :resourceType
                  )
              and (
                    :resourceId is null
                    or auditLog.resourceId = :resourceId
                  )
              and (
                    :createdFrom is null
                    or auditLog.createdAt >= :createdFrom
                  )
              and (
                    :createdUntil is null
                    or auditLog.createdAt < :createdUntil
                  )
            order by auditLog.createdAt desc
            """)
    Page<WebsiteContentAuditLog> searchAuditLogs(
            @Param("keyword")
            String keyword,

            @Param("adminUserId")
            UUID adminUserId,

            @Param("action")
            WebsiteContentAuditAction action,

            @Param("resourceType")
            WebsiteContentAuditResourceType resourceType,

            @Param("resourceId")
            UUID resourceId,

            @Param("createdFrom")
            Instant createdFrom,

            @Param("createdUntil")
            Instant createdUntil,

            Pageable pageable
    );

    @EntityGraph(attributePaths = "adminUser")
    Page<WebsiteContentAuditLog>
    findAllByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "adminUser")
    Page<WebsiteContentAuditLog>
    findAllByAdminUser_AdminUserIdOrderByCreatedAtDesc(
            UUID adminUserId,
            Pageable pageable
    );

    long countByResourceTypeAndResourceId(
            WebsiteContentAuditResourceType resourceType,
            UUID resourceId
    );

    long countByAction(
            WebsiteContentAuditAction action
    );
}