package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT LOG ENTITY
 * ================================================================
 *
 * Purpose:
 * Records an immutable history of significant administrator actions
 * performed against CMS-managed website content.
 *
 * Responsibilities:
 * - Records the administrator responsible for an action.
 * - Identifies the affected resource and action.
 * - Stores optional before-and-after JSON snapshots.
 * - Stores an optional human-readable change summary.
 * - Captures request-origin metadata such as IP address and user agent.
 * - Supports investigation, accountability, and content-history review.
 *
 * Immutability:
 * Audit records are append-only. This entity intentionally exposes no
 * update or delete business methods.
 *
 * Security:
 * beforeDataJson and afterDataJson must not contain:
 * - passwords;
 * - authentication tokens;
 * - invitation plain tokens;
 * - API keys;
 * - storage credentials;
 * - other sensitive secrets.
 *
 * JSON requirements:
 * When supplied, beforeDataJson and afterDataJson must be JSON objects,
 * matching the database constraints.
 * ================================================================
 */
@Entity
@Table(
        name = "website_content_audit_logs",
        indexes = {
                @Index(
                        name = "idx_website_content_audit_resource",
                        columnList =
                                "resource_type, resource_id, created_at"
                ),
                @Index(
                        name = "idx_website_content_audit_admin",
                        columnList =
                                "admin_user_id, created_at"
                ),
                @Index(
                        name = "idx_website_content_audit_action",
                        columnList =
                                "action, created_at"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteContentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "content_audit_log_id",
            nullable = false,
            updatable = false
    )
    private UUID contentAuditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            updatable = false,
            length = 60
    )
    private WebsiteContentAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            updatable = false,
            length = 60
    )
    private WebsiteContentAuditResourceType resourceType;

    @Column(
            name = "resource_id",
            updatable = false
    )
    private UUID resourceId;

    @Column(
            name = "resource_name",
            updatable = false,
            length = 255
    )
    private String resourceName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "before_data_json",
            updatable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode beforeDataJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "after_data_json",
            updatable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode afterDataJson;

    @Column(
            name = "change_summary",
            updatable = false,
            length = 1000
    )
    private String changeSummary;

    @Column(
            name = "ip_address",
            updatable = false,
            length = 64
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            updatable = false,
            length = 500
    )
    private String userAgent;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        normalizeFields();
        validateState();

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private void normalizeFields() {
        resourceName = normalizeOptional(resourceName);
        changeSummary = normalizeOptional(changeSummary);
        ipAddress = normalizeOptional(ipAddress);
        userAgent = normalizeOptional(userAgent);
    }

    private void validateState() {
        if (action == null) {
            throw new IllegalStateException(
                    "Website content audit action is required."
            );
        }

        if (resourceType == null) {
            throw new IllegalStateException(
                    "Website content audit resource type is required."
            );
        }

        validateMaximumLength(
                resourceName,
                255,
                "Resource name"
        );

        validateMaximumLength(
                changeSummary,
                1000,
                "Change summary"
        );

        validateMaximumLength(
                ipAddress,
                64,
                "IP address"
        );

        validateMaximumLength(
                userAgent,
                500,
                "User agent"
        );

        validateJsonObject(
                beforeDataJson,
                "Before-data JSON"
        );

        validateJsonObject(
                afterDataJson,
                "After-data JSON"
        );
    }

    private void validateJsonObject(
            JsonNode value,
            String fieldName
    ) {
        if (value != null && !value.isObject()) {
            throw new IllegalStateException(
                    fieldName + " must be a JSON object."
            );
        }
    }

    private void validateMaximumLength(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value != null
                        && value.length() > maximumLength
        ) {
            throw new IllegalStateException(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
            );
        }
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