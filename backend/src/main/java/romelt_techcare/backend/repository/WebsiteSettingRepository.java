package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for general website and CMS
 * configuration settings.
 *
 * Responsibilities:
 * - Retrieves settings by identifier.
 * - Retrieves settings by group and key.
 * - Retrieves public non-sensitive settings.
 * - Supports administrator filtering and pagination.
 * - Supports pessimistic locking for update and deletion.
 * - Detects duplicate group-and-key combinations.
 *
 * Security:
 * Public queries always require:
 * - isPublic = true
 * - isSensitive = false
 * ================================================================
 */
@Repository
public interface WebsiteSettingRepository
        extends JpaRepository<WebsiteSetting, UUID> {

    /**
     * Retrieves one setting with administrator attribution.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteSetting> findByWebsiteSettingId(
            UUID websiteSettingId
    );

    /**
     * Retrieves a setting by its stable group and key.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteSetting> findBySettingGroupAndSettingKey(
            String settingGroup,
            String settingKey
    );

    /**
     * Retrieves one public non-sensitive setting.
     */
    Optional<WebsiteSetting>
    findBySettingGroupAndSettingKeyAndIsPublicTrueAndIsSensitiveFalse(
            String settingGroup,
            String settingKey
    );

    /**
     * Retrieves all settings belonging to one group.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    List<WebsiteSetting>
    findAllBySettingGroupOrderBySettingKeyAsc(
            String settingGroup
    );

    /**
     * Retrieves all public non-sensitive settings.
     */
    List<WebsiteSetting>
    findAllByIsPublicTrueAndIsSensitiveFalseOrderBySettingGroupAscSettingKeyAsc();

    /**
     * Retrieves all public non-sensitive settings in one group.
     */
    List<WebsiteSetting>
    findAllBySettingGroupAndIsPublicTrueAndIsSensitiveFalseOrderBySettingKeyAsc(
            String settingGroup
    );

    /**
     * Locks one setting for modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select setting
            from WebsiteSetting setting
            where setting.websiteSettingId = :websiteSettingId
            """)
    Optional<WebsiteSetting> findByIdForUpdate(
            @Param("websiteSettingId")
            UUID websiteSettingId
    );

    /**
     * Locks one setting by group and key.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select setting
            from WebsiteSetting setting
            where setting.settingGroup = :settingGroup
              and setting.settingKey = :settingKey
            """)
    Optional<WebsiteSetting> findByGroupAndKeyForUpdate(
            @Param("settingGroup")
            String settingGroup,

            @Param("settingKey")
            String settingKey
    );

    /**
     * Checks whether a group-and-key combination exists.
     */
    boolean existsBySettingGroupAndSettingKey(
            String settingGroup,
            String settingKey
    );

    /**
     * Checks whether another record uses the group-and-key combination.
     */
    boolean existsBySettingGroupAndSettingKeyAndWebsiteSettingIdNot(
            String settingGroup,
            String settingKey,
            UUID websiteSettingId
    );

    /**
     * Searches administrator-facing settings.
     */
    @EntityGraph(attributePaths = {
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    @Query("""
            select setting
            from WebsiteSetting setting
            where (
                    :keyword is null
                    or lower(setting.settingGroup)
                        like lower(concat('%', :keyword, '%'))
                    or lower(setting.settingKey)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(setting.description, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :settingGroup is null
                    or setting.settingGroup = :settingGroup
                  )
              and (
                    :isPublic is null
                    or setting.isPublic = :isPublic
                  )
              and (
                    :isSensitive is null
                    or setting.isSensitive = :isSensitive
                  )
            order by setting.settingGroup asc,
                     setting.settingKey asc
            """)
    Page<WebsiteSetting> searchWebsiteSettings(
            @Param("keyword")
            String keyword,

            @Param("settingGroup")
            String settingGroup,

            @Param("isPublic")
            Boolean isPublic,

            @Param("isSensitive")
            Boolean isSensitive,

            Pageable pageable
    );

    /**
     * Counts settings in one group.
     */
    long countBySettingGroup(
            String settingGroup
    );

    /**
     * Counts public non-sensitive settings.
     */
    long countByIsPublicTrueAndIsSensitiveFalse();

    /**
     * Counts sensitive settings.
     */
    long countByIsSensitiveTrue();
}