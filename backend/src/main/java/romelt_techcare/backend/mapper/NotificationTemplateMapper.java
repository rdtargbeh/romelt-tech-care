package romelt_techcare.backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.NotificationTemplateCreateRequest;
import romelt_techcare.backend.dto.NotificationTemplateResponse;
import romelt_techcare.backend.dto.NotificationTemplateSummaryResponse;
import romelt_techcare.backend.dto.NotificationTemplateUpdateRequest;
import romelt_techcare.backend.entity.NotificationTemplate;
import romelt_techcare.backend.enums.NotificationChannel;

import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts notification-template requests into persistent entities
 * and converts entities into administrator response DTOs.
 *
 * Responsibilities:
 * - Maps EMAIL, SMS, and IN_APP template creation requests.
 * - Applies template defaults and normalization.
 * - Applies partial template updates.
 * - Preserves immutable template identity fields during updates.
 * - Clears fields that are invalid for SMS and IN_APP channels.
 * - Maps complete and compact administrator responses.
 *
 * Template identity:
 * The following values identify one template version and are not
 * changed by update requests:
 *
 * - templateKey
 * - channel
 * - locale
 * - templateVersion
 *
 * Channel rules:
 *
 * EMAIL:
 * - subjectTemplate is required.
 * - bodyTextTemplate or bodyHtmlTemplate is required.
 *
 * SMS:
 * - bodyTextTemplate is required.
 * - subjectTemplate is null.
 * - bodyHtmlTemplate is null.
 *
 * IN_APP:
 * - subjectTemplate is the notification title.
 * - bodyTextTemplate is the notification message.
 * - bodyHtmlTemplate is null.
 * ================================================================
 */
@Component
public class NotificationTemplateMapper {

    /**
     * Converts an administrator create request into a new template
     * entity.
     */
    public NotificationTemplate toEntity(
            NotificationTemplateCreateRequest request,
            UUID administratorId
    ) {
        requireCreateRequest(request);
        requireAdministratorId(administratorId);
        validateCreateRequest(request);

        NotificationChannel channel = request.channel();

        String subjectTemplate =
                normalizeOptional(
                        request.subjectTemplate()
                );

        String bodyTextTemplate =
                normalizeOptional(
                        request.bodyTextTemplate()
                );

        String bodyHtmlTemplate =
                normalizeOptional(
                        request.bodyHtmlTemplate()
                );

        if (channel == NotificationChannel.SMS) {
            subjectTemplate = null;
            bodyHtmlTemplate = null;
        }

        if (channel == NotificationChannel.IN_APP) {
            bodyHtmlTemplate = null;
        }

        return NotificationTemplate.builder()
                .templateKey(
                        normalizeTemplateKey(
                                request.templateKey()
                        )
                )
                .channel(channel)
                .templateName(
                        normalizeRequired(
                                request.templateName(),
                                "Notification template name is required."
                        )
                )
                .subjectTemplate(subjectTemplate)
                .bodyTextTemplate(bodyTextTemplate)
                .bodyHtmlTemplate(bodyHtmlTemplate)
                .locale(
                        normalizeLocale(
                                request.locale()
                        )
                )
                .templateVersion(
                        request.templateVersion() == null
                                ? 1
                                : request.templateVersion()
                )
                .requiredVariablesJson(
                        copyRequiredVariables(
                                request.requiredVariablesJson()
                        )
                )
                .active(
                        request.active() == null
                                || request.active()
                )
                .createdByAdminUserId(administratorId)
                .updatedByAdminUserId(administratorId)
                .build();
    }

    /**
     * Applies partial editable-field changes to an existing template
     * version.
     *
     * Null means preserve the existing value.
     *
     * Empty strings clear optional fields, subject to channel
     * validation after all requested changes are applied.
     */
    public void updateEntity(
            NotificationTemplate template,
            NotificationTemplateUpdateRequest request,
            UUID administratorId
    ) {
        requireTemplate(template);
        requireUpdateRequest(request);
        requireAdministratorId(administratorId);

        if (!request.hasChanges()) {
            throw new IllegalArgumentException(
                    "At least one notification template change is required."
            );
        }

        if (!request.hasValidRequiredVariables()) {
            throw new IllegalArgumentException(
                    "Notification template required variables must be a JSON array containing only non-blank strings."
            );
        }

        if (request.templateName() != null) {
            template.setTemplateName(
                    normalizeRequired(
                            request.templateName(),
                            "Notification template name is required."
                    )
            );
        }

        if (request.subjectTemplate() != null) {
            template.setSubjectTemplate(
                    normalizeOptional(
                            request.subjectTemplate()
                    )
            );
        }

        if (request.bodyTextTemplate() != null) {
            template.setBodyTextTemplate(
                    normalizeOptional(
                            request.bodyTextTemplate()
                    )
            );
        }

        if (request.bodyHtmlTemplate() != null) {
            template.setBodyHtmlTemplate(
                    normalizeOptional(
                            request.bodyHtmlTemplate()
                    )
            );
        }

        if (request.requiredVariablesJson() != null) {
            template.setRequiredVariablesJson(
                    copyRequiredVariables(
                            request.requiredVariablesJson()
                    )
            );
        }

        applyChannelSpecificFields(template);

        validateEntityContent(template);

        if (request.active() != null) {
            if (request.active()) {
                template.activate(administratorId);
            } else {
                template.deactivate(administratorId);
            }
        } else {
            template.setUpdatedByAdminUserId(
                    administratorId
            );
        }
    }

    /**
     * Maps one complete administrator response.
     */
    public NotificationTemplateResponse toResponse(
            NotificationTemplate template
    ) {
        requireTemplate(template);

        return new NotificationTemplateResponse(
                template.getNotificationTemplateId(),
                template.getTemplateKey(),
                template.getChannel(),
                template.getTemplateName(),
                template.getSubjectTemplate(),
                template.getBodyTextTemplate(),
                template.getBodyHtmlTemplate(),
                template.getLocale(),
                template.getTemplateVersion(),
                template.copyRequiredVariablesJson(),
                template.isActive(),
                template.getCreatedByAdminUserId(),
                template.getUpdatedByAdminUserId(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                template.getRowVersion()
        );
    }

    /**
     * Maps a compact administrator list response.
     */
    public NotificationTemplateSummaryResponse toSummaryResponse(
            NotificationTemplate template
    ) {
        requireTemplate(template);

        return new NotificationTemplateSummaryResponse(
                template.getNotificationTemplateId(),
                template.getTemplateKey(),
                template.getChannel(),
                template.getTemplateName(),
                template.getLocale(),
                template.getTemplateVersion(),
                template.isActive(),
                hasText(template.getBodyTextTemplate()),
                hasText(template.getBodyHtmlTemplate()),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    /**
     * Normalizes template keys to lowercase hyphen-separated values.
     */
    public String normalizeTemplateKey(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Notification template key is required."
                );

        return normalized
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }

    /**
     * Normalizes locales into a consistent language-region format.
     *
     * Examples:
     * en_us → en-US
     * EN-us → en-US
     * en → en
     */
    public String normalizeLocale(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return "en-US";
        }

        String[] parts =
                normalized
                        .replace('_', '-')
                        .split("-", 3);

        if (parts.length == 1) {
            return parts[0]
                    .toLowerCase(Locale.ROOT);
        }

        return parts[0]
                .toLowerCase(Locale.ROOT)
                + "-"
                + parts[1]
                .toUpperCase(Locale.ROOT);
    }

    private void validateCreateRequest(
            NotificationTemplateCreateRequest request
    ) {
        if (!request.hasValidRequiredVariables()) {
            throw new IllegalArgumentException(
                    "Notification template required variables must be a JSON array containing only non-blank strings."
            );
        }

        if (!request.hasValidChannelContent()) {
            throw new IllegalArgumentException(
                    resolveChannelContentError(
                            request.channel()
                    )
            );
        }

        if (!request.hasValidChannelSpecificFields()) {
            throw new IllegalArgumentException(
                    resolveChannelSpecificFieldError(
                            request.channel()
                    )
            );
        }

        if (
                request.templateVersion() != null
                        && request.templateVersion() < 1
        ) {
            throw new IllegalArgumentException(
                    "Notification template version must be greater than zero."
            );
        }
    }

    private void applyChannelSpecificFields(
            NotificationTemplate template
    ) {
        NotificationChannel channel =
                template.getChannel();

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification template channel is required."
            );
        }

        switch (channel) {
            case EMAIL -> {
                // EMAIL may use both text and HTML bodies.
            }

            case SMS -> {
                template.setSubjectTemplate(null);
                template.setBodyHtmlTemplate(null);
            }

            case IN_APP ->
                    template.setBodyHtmlTemplate(null);
        }
    }

    private void validateEntityContent(
            NotificationTemplate template
    ) {
        NotificationChannel channel =
                template.getChannel();

        switch (channel) {
            case EMAIL -> {
                if (!hasText(template.getSubjectTemplate())) {
                    throw new IllegalArgumentException(
                            "Email notification template subject is required."
                    );
                }

                if (
                        !hasText(template.getBodyTextTemplate())
                                && !hasText(
                                template.getBodyHtmlTemplate()
                        )
                ) {
                    throw new IllegalArgumentException(
                            "Email notification template requires a text or HTML body."
                    );
                }
            }

            case SMS -> {
                if (!hasText(template.getBodyTextTemplate())) {
                    throw new IllegalArgumentException(
                            "SMS notification template text body is required."
                    );
                }
            }

            case IN_APP -> {
                if (!hasText(template.getSubjectTemplate())) {
                    throw new IllegalArgumentException(
                            "In-app notification title is required."
                    );
                }

                if (!hasText(template.getBodyTextTemplate())) {
                    throw new IllegalArgumentException(
                            "In-app notification message is required."
                    );
                }
            }
        }

        JsonNode requiredVariables =
                template.getRequiredVariablesJson();

        if (
                requiredVariables == null
                        || !requiredVariables.isArray()
        ) {
            throw new IllegalArgumentException(
                    "Notification template required variables must be a JSON array."
            );
        }

        for (JsonNode variable : requiredVariables) {
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

    private JsonNode copyRequiredVariables(
            JsonNode source
    ) {
        if (source == null) {
            return JsonNodeFactory.instance.arrayNode();
        }

        if (!source.isArray()) {
            throw new IllegalArgumentException(
                    "Notification template required variables must be a JSON array."
            );
        }

        return source.deepCopy();
    }

    private String resolveChannelContentError(
            NotificationChannel channel
    ) {
        if (channel == null) {
            return "Notification template channel is required.";
        }

        return switch (channel) {
            case EMAIL ->
                    "Email notification templates require a subject and a text or HTML body.";

            case SMS ->
                    "SMS notification templates require a text body.";

            case IN_APP ->
                    "In-app notification templates require a title and message.";
        };
    }

    private String resolveChannelSpecificFieldError(
            NotificationChannel channel
    ) {
        if (channel == null) {
            return "Notification template channel is required.";
        }

        return switch (channel) {
            case EMAIL ->
                    "Email notification template content is invalid.";

            case SMS ->
                    "SMS notification templates cannot contain a subject or HTML body.";

            case IN_APP ->
                    "In-app notification templates cannot contain an HTML body.";
        };
    }

    private String normalizeRequired(
            String value,
            String message
    ) {
        String normalized =
                normalizeOptional(value);

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

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }

    private void requireCreateRequest(
            NotificationTemplateCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification template create request is required."
            );
        }
    }

    private void requireUpdateRequest(
            NotificationTemplateUpdateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification template update request is required."
            );
        }
    }

    private void requireTemplate(
            NotificationTemplate template
    ) {
        if (template == null) {
            throw new IllegalArgumentException(
                    "Notification template is required."
            );
        }
    }

    private void requireAdministratorId(
            UUID administratorId
    ) {
        if (administratorId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }
    }
}