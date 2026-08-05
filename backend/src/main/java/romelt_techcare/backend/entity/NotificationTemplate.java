package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.NotificationChannel;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores reusable and versioned EMAIL, SMS, and IN_APP notification
 * templates.
 *
 * Responsibilities:
 * - Identifies a logical template using a stable template key.
 * - Separates templates by delivery channel.
 * - Supports locale-specific and versioned templates.
 * - Stores required rendering variables.
 * - Supports activation and deactivation.
 * - Preserves administrator attribution.
 * - Supports optimistic locking.
 *
 * Channel requirements:
 *
 * EMAIL:
 * - subjectTemplate is required.
 * - bodyTextTemplate or bodyHtmlTemplate is required.
 *
 * SMS:
 * - bodyTextTemplate is required.
 * - subjectTemplate and bodyHtmlTemplate remain null.
 *
 * IN_APP:
 * - subjectTemplate is the administrator notification title.
 * - bodyTextTemplate is the administrator notification message.
 * - bodyHtmlTemplate remains null.
 *
 * Examples of IN_APP notifications:
 * - New booking request received.
 * - Booking status changed.
 * - New contact inquiry received.
 * - Contact inquiry responded to or closed.
 * ================================================================
 */
@Entity
@Table(
        name = "notification_templates",
        indexes = {
                @Index(
                        name = "idx_notification_templates_active",
                        columnList = "channel, locale, template_key"
                ),
                @Index(
                        name = "idx_notification_templates_key",
                        columnList = "template_key, channel, locale, template_version"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_template_key_channel_locale_version",
                        columnNames = {
                                "template_key",
                                "channel",
                                "locale",
                                "template_version"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_template_id",
            nullable = false,
            updatable = false
    )
    private UUID notificationTemplateId;

    @Column(
            name = "template_key",
            nullable = false,
            length = 160
    )
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_name", nullable = false, length = 180)
    private String templateName;

    /**
     * EMAIL subject or IN_APP notification title.
     */
    @Column(
            name = "subject_template",
            length = 500
    )
    private String subjectTemplate;

    /**
     * EMAIL plain-text body, SMS message, or IN_APP notification
     * message.
     */
    @Lob
    @Column(
            name = "body_text_template",
            columnDefinition = "TEXT"
    )
    private String bodyTextTemplate;

    /**
     * EMAIL HTML content.
     *
     * Must remain null for SMS and IN_APP templates.
     */
    @Lob
    @Column(
            name = "body_html_template",
            columnDefinition = "TEXT"
    )
    private String bodyHtmlTemplate;

    @Builder.Default
    @Column(
            name = "locale",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'en-US'"
    )
    private String locale = "en-US";

    @Builder.Default
    @Column(
            name = "template_version",
            nullable = false,
            columnDefinition = "integer default 1"
    )
    private Integer templateVersion = 1;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "required_variables_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode requiredVariablesJson =
            JsonNodeFactory.instance.arrayNode();

    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false,
            columnDefinition = "boolean default true"
    )
    private boolean active = true;

    @Column(
            name = "created_by_admin_user_id",
            updatable = false
    )
    private UUID createdByAdminUserId;

    @Column(
            name = "updated_by_admin_user_id"
    )
    private UUID updatedByAdminUserId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(
            name = "row_version",
            nullable = false
    )
    private Long rowVersion;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        normalizeFields();
        validateTemplate();

        if (createdAt == null) {
            createdAt = now;
        }

        if (rowVersion == null) {
            rowVersion = 0L;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
        validateTemplate();

        updatedAt = Instant.now();
    }

    public void activate(
            UUID administratorId
    ) {
        requireAdministrator(administratorId);

        active = true;
        updatedByAdminUserId = administratorId;
    }

    public void deactivate(
            UUID administratorId
    ) {
        requireAdministrator(administratorId);

        active = false;
        updatedByAdminUserId = administratorId;
    }

    public void updateContent(
            String newTemplateName,
            String newSubjectTemplate,
            String newBodyTextTemplate,
            String newBodyHtmlTemplate,
            JsonNode newRequiredVariablesJson,
            UUID administratorId
    ) {
        requireAdministrator(administratorId);

        templateName = newTemplateName;
        subjectTemplate = newSubjectTemplate;
        bodyTextTemplate = newBodyTextTemplate;
        bodyHtmlTemplate = newBodyHtmlTemplate;

        requiredVariablesJson =
                newRequiredVariablesJson == null
                        ? JsonNodeFactory.instance.arrayNode()
                        : newRequiredVariablesJson.deepCopy();

        updatedByAdminUserId = administratorId;

        normalizeFields();
        validateTemplate();
    }

    public JsonNode copyRequiredVariablesJson() {
        if (requiredVariablesJson == null) {
            return JsonNodeFactory.instance.arrayNode();
        }

        return requiredVariablesJson.deepCopy();
    }

    private void validateTemplate() {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification template channel is required."
            );
        }

        if (templateVersion == null || templateVersion < 1) {
            throw new IllegalArgumentException(
                    "Notification template version must be greater than zero."
            );
        }

        validateRequiredVariables();

        switch (channel) {
            case EMAIL -> validateEmailTemplate();
            case SMS -> validateSmsTemplate();
            case IN_APP -> validateInAppTemplate();
        }
    }

    private void validateRequiredVariables() {
        if (
                requiredVariablesJson == null
                        || !requiredVariablesJson.isArray()
        ) {
            throw new IllegalArgumentException(
                    "Notification template required variables must be a JSON array."
            );
        }

        for (JsonNode variable : requiredVariablesJson) {
            if (
                    variable == null
                            || !variable.isTextual()
                            || variable.asText().trim().isEmpty()
            ) {
                throw new IllegalArgumentException(
                        "Every required notification template variable must be a non-blank string."
                );
            }
        }
    }

    private void validateEmailTemplate() {
        if (isBlank(subjectTemplate)) {
            throw new IllegalArgumentException(
                    "Email notification template subject is required."
            );
        }

        if (
                isBlank(bodyTextTemplate)
                        && isBlank(bodyHtmlTemplate)
        ) {
            throw new IllegalArgumentException(
                    "Email notification template requires a text or HTML body."
            );
        }
    }

    private void validateSmsTemplate() {
        if (isBlank(bodyTextTemplate)) {
            throw new IllegalArgumentException(
                    "SMS notification template text body is required."
            );
        }

        subjectTemplate = null;
        bodyHtmlTemplate = null;
    }

    private void validateInAppTemplate() {
        if (isBlank(subjectTemplate)) {
            throw new IllegalArgumentException(
                    "In-app notification title is required."
            );
        }

        if (isBlank(bodyTextTemplate)) {
            throw new IllegalArgumentException(
                    "In-app notification message is required."
            );
        }

        bodyHtmlTemplate = null;
    }

    private void normalizeFields() {
        templateKey = normalizeTemplateKey(templateKey);

        templateName = requireText(
                templateName,
                "Notification template name is required."
        );

        subjectTemplate =
                normalizeOptional(subjectTemplate);

        bodyTextTemplate =
                normalizeOptional(bodyTextTemplate);

        bodyHtmlTemplate =
                normalizeOptional(bodyHtmlTemplate);

        locale = normalizeLocale(locale);

        if (templateVersion == null || templateVersion < 1) {
            templateVersion = 1;
        }

        if (requiredVariablesJson == null) {
            requiredVariablesJson =
                    JsonNodeFactory.instance.arrayNode();
        }
    }

    private String normalizeTemplateKey(
            String value
    ) {
        String normalized = requireText(
                value,
                "Notification template key is required."
        );

        return normalized
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }

    private String normalizeLocale(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return "en-US";
        }

        String[] localeParts =
                normalized
                        .replace('_', '-')
                        .split("-", 3);

        if (localeParts.length == 1) {
            return localeParts[0]
                    .toLowerCase(Locale.ROOT);
        }

        return localeParts[0]
                .toLowerCase(Locale.ROOT)
                + "-"
                + localeParts[1]
                .toUpperCase(Locale.ROOT);
    }

    private String requireText(
            String value,
            String message
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
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

    private void requireAdministrator(
            UUID administratorId
    ) {
        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }
}