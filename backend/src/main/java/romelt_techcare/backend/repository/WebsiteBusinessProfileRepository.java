package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for the shared public business
 * profile used throughout the Romelt TechCare website.
 *
 * Responsibilities:
 * - Retrieves the active business profile.
 * - Retrieves a profile with all branding media relationships.
 * - Enforces single-active-profile checks in the service layer.
 * - Provides pessimistic locking for profile updates and activation.
 * ================================================================
 */
@Repository
public interface WebsiteBusinessProfileRepository
        extends JpaRepository<WebsiteBusinessProfile, UUID> {

    /**
     * Retrieves the active profile with all media relationships.
     */
    @EntityGraph(attributePaths = {
            "primaryLogoMedia",
            "lightLogoMedia",
            "darkLogoMedia",
            "faviconMedia",
            "defaultSocialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessProfile> findByIsActiveTrue();

    /**
     * Retrieves one profile with all media relationships.
     */
    @EntityGraph(attributePaths = {
            "primaryLogoMedia",
            "lightLogoMedia",
            "darkLogoMedia",
            "faviconMedia",
            "defaultSocialImageMedia",
            "createdByAdminUser",
            "updatedByAdminUser"
    })
    Optional<WebsiteBusinessProfile> findByBusinessProfileId(
            UUID businessProfileId
    );

    /**
     * Retrieves and locks one profile for modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select profile
            from WebsiteBusinessProfile profile
            where profile.businessProfileId = :businessProfileId
            """)
    Optional<WebsiteBusinessProfile> findByIdForUpdate(
            @Param("businessProfileId")
            UUID businessProfileId
    );

    /**
     * Retrieves and locks the active profile.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select profile
            from WebsiteBusinessProfile profile
            where profile.isActive = true
            """)
    Optional<WebsiteBusinessProfile> findActiveForUpdate();

    /**
     * Determines whether an active profile exists.
     */
    boolean existsByIsActiveTrue();

    /**
     * Determines whether another active profile exists.
     */
    boolean existsByIsActiveTrueAndBusinessProfileIdNot(
            UUID businessProfileId
    );

    /**
     * Counts active profiles.
     */
    long countByIsActiveTrue();
}