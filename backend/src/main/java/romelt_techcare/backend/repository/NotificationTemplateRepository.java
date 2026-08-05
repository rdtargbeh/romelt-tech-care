package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.NotificationTemplate;
import romelt_techcare.backend.enums.NotificationChannel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, lookup, activation, versioning, and search
 * operations for EMAIL, SMS, and IN_APP notification templates.
 *
 * Responsibilities:
 * - Persists notification-template versions.
 * - Finds one exact template version.
 * - Finds the active version for a key, channel, and locale.
 * - Checks version-identity uniqueness.
 * - Determines the latest template version number.
 * - Deactivates the currently active version before activating another.
 * - Supports administrator template lists and filters.
 * - Locks template records during activation and update operations.
 *
 * Active-template rule:
 * PostgreSQL allows only one active template for the same:
 *
 * templateKey + channel + locale
 *
 * The service must deactivate the current active version before saving
 * another active version for the same identity.
 *
 * Template identity:
 * templateKey + channel + locale + templateVersion
 *
 * Soft deletion:
 * Notification templates are retained for historical event and
 * delivery references. They are deactivated rather than deleted
 * through normal application operations.
 * ================================================================
 */
@Repository
public interface NotificationTemplateRepository
        extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Finds one exact template version.
     */
    Optional<NotificationTemplate>
    findByTemplateKeyAndChannelAndLocaleAndTemplateVersion(
            String templateKey,
            NotificationChannel channel,
            String locale,
            Integer templateVersion
    );

    /**
     * Finds the currently active version for a template key, channel,
     * and locale.
     */
    Optional<NotificationTemplate>
    findByTemplateKeyAndChannelAndLocaleAndActiveTrue(
            String templateKey,
            NotificationChannel channel,
            String locale
    );

    /**
     * Locks the currently active template version.
     *
     * Used when replacing or deactivating an active version.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select template
            from NotificationTemplate template
            where template.templateKey = :templateKey
              and template.channel = :channel
              and template.locale = :locale
              and template.active = true
            """)
    Optional<NotificationTemplate> findActiveForUpdate(
            @Param("templateKey")
            String templateKey,

            @Param("channel")
            NotificationChannel channel,

            @Param("locale")
            String locale
    );

    /**
     * Locks one template record for update, activation, or
     * deactivation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select template
            from NotificationTemplate template
            where template.notificationTemplateId =
                :notificationTemplateId
            """)
    Optional<NotificationTemplate> findByIdForUpdate(
            @Param("notificationTemplateId")
            UUID notificationTemplateId
    );

    /**
     * Checks whether the exact template-version identity already
     * exists.
     */
    boolean existsByTemplateKeyAndChannelAndLocaleAndTemplateVersion(
            String templateKey,
            NotificationChannel channel,
            String locale,
            Integer templateVersion
    );

    /**
     * Checks whether the exact template-version identity is already
     * used by another record.
     */
    boolean existsByTemplateKeyAndChannelAndLocaleAndTemplateVersionAndNotificationTemplateIdNot(
            String templateKey,
            NotificationChannel channel,
            String locale,
            Integer templateVersion,
            UUID notificationTemplateId
    );

    /**
     * Returns the highest existing version number for one template
     * key, channel, and locale.
     */
    @Query("""
            select max(template.templateVersion)
            from NotificationTemplate template
            where template.templateKey = :templateKey
              and template.channel = :channel
              and template.locale = :locale
            """)
    Integer findMaximumTemplateVersion(
            @Param("templateKey")
            String templateKey,

            @Param("channel")
            NotificationChannel channel,

            @Param("locale")
            String locale
    );

    /**
     * Returns all versions for one logical template identity, newest
     * version first.
     */
    List<NotificationTemplate>
    findByTemplateKeyAndChannelAndLocaleOrderByTemplateVersionDesc(
            String templateKey,
            NotificationChannel channel,
            String locale
    );

    /**
     * Returns all active templates for one channel and locale.
     */
    List<NotificationTemplate>
    findByChannelAndLocaleAndActiveTrueOrderByTemplateKeyAsc(
            NotificationChannel channel,
            String locale
    );

    /**
     * Returns all active templates for one logical template key.
     *
     * Multiple records may be returned because each channel and locale
     * may have its own active version.
     */
    List<NotificationTemplate>
    findByTemplateKeyAndActiveTrueOrderByChannelAscLocaleAsc(
            String templateKey
    );

    /**
     * Deactivates any currently active version for the supplied
     * template key, channel, and locale.
     *
     * The excludedTemplateId allows an already-active target template
     * to remain active.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update NotificationTemplate template
            set template.active = false,
                template.updatedByAdminUserId = :administratorId,
                template.updatedAt = CURRENT_TIMESTAMP
            where template.templateKey = :templateKey
              and template.channel = :channel
              and template.locale = :locale
              and template.active = true
              and (
                    :excludedTemplateId is null
                    or template.notificationTemplateId
                        <> :excludedTemplateId
              )
            """)
    int deactivateOtherActiveVersions(
            @Param("templateKey")
            String templateKey,

            @Param("channel")
            NotificationChannel channel,

            @Param("locale")
            String locale,

            @Param("excludedTemplateId")
            UUID excludedTemplateId,

            @Param("administratorId")
            UUID administratorId
    );

    /**
     * Returns a paginated administrator-searchable template list.
     *
     * Supported filters:
     * - keyword
     * - template key
     * - channel
     * - locale
     * - template version
     * - active status
     *
     * Keyword searches:
     * - template key
     * - template name
     * - locale
     */
    @Query("""
            select template
            from NotificationTemplate template
            where (
                    :keyword is null
                    or lower(template.templateKey) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(template.templateName) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(template.locale) like lower(
                        concat('%', :keyword, '%')
                    )
              )
              and (
                    :templateKey is null
                    or template.templateKey = :templateKey
              )
              and (
                    :channel is null
                    or template.channel = :channel
              )
              and (
                    :locale is null
                    or template.locale = :locale
              )
              and (
                    :templateVersion is null
                    or template.templateVersion = :templateVersion
              )
              and (
                    :active is null
                    or template.active = :active
              )
            """)
    Page<NotificationTemplate> searchTemplates(
            @Param("keyword")
            String keyword,

            @Param("templateKey")
            String templateKey,

            @Param("channel")
            NotificationChannel channel,

            @Param("locale")
            String locale,

            @Param("templateVersion")
            Integer templateVersion,

            @Param("active")
            Boolean active,

            Pageable pageable
    );

    /**
     * Counts active template versions.
     */
    long countByActiveTrue();

    /**
     * Counts active templates for one delivery channel.
     */
    long countByChannelAndActiveTrue(
            NotificationChannel channel
    );
}