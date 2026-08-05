package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the normal weekly operating schedule displayed throughout
 * the Romelt TechCare public website.
 *
 * Responsibilities:
 * - Associates one schedule row with one website business profile.
 * - Stores one entry for each day of the week.
 * - Stores open, closed, and appointment-only information.
 * - Stores optional opening and closing times.
 * - Stores administrator-controlled public display text.
 * - Supports ordering in administrator and public responses.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Day mapping:
 * 1 = Monday
 * 2 = Tuesday
 * 3 = Wednesday
 * 4 = Thursday
 * 5 = Friday
 * 6 = Saturday
 * 7 = Sunday
 *
 * Business rules:
 * - A business profile can have only one row per day.
 * - Closed days must not retain opening or closing times.
 * - Opening and closing times must either both be present or both be null.
 * - When both times are present, opening time must be before closing time.
 *
 * Special dates:
 * Holidays, temporary closures, and date-specific schedule overrides
 * belong to WebsiteBusinessHourException, not this entity.
 * ================================================================
 */
@Entity
@Table(
        name = "website_business_hours",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_hours_day",
                        columnNames = {
                                "business_profile_id",
                                "day_of_week"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_hours_order",
                        columnList =
                                "business_profile_id, display_order, day_of_week"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteBusinessHour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "business_hour_id",
            nullable = false,
            updatable = false
    )
    private UUID businessHourId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "business_profile_id",
            nullable = false
    )
    private WebsiteBusinessProfile businessProfile;

    /**
     * ISO day number from 1 through 7.
     */
    @Column(
            name = "day_of_week",
            nullable = false
    )
    private Short dayOfWeek;

    @Builder.Default
    @Column(
            name = "is_closed",
            nullable = false
    )
    private Boolean isClosed = false;

    @Builder.Default
    @Column(
            name = "is_by_appointment",
            nullable = false
    )
    private Boolean isByAppointment = true;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(
            name = "display_text",
            length = 120
    )
    private String displayText;

    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_user_id")
    private AdminUser updatedByAdminUser;

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

        initializeDefaults();
        normalizeFields();
        validateState();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        initializeDefaults();
        normalizeFields();
        validateState();

        updatedAt = Instant.now();
    }

    /**
     * Applies the complete editable schedule state.
     */
    public void updateSchedule(
            boolean closed,
            boolean byAppointment,
            LocalTime newOpeningTime,
            LocalTime newClosingTime,
            String newDisplayText,
            int newDisplayOrder,
            AdminUser administrator
    ) {
        isClosed = closed;
        isByAppointment = byAppointment;
        openingTime = closed ? null : newOpeningTime;
        closingTime = closed ? null : newClosingTime;
        displayText = newDisplayText;
        displayOrder = newDisplayOrder;
        updatedByAdminUser = administrator;
    }

    /**
     * Returns the standard Java day-of-week representation.
     */
    public DayOfWeek getJavaDayOfWeek() {
        validateDayOfWeek(dayOfWeek);

        return DayOfWeek.of(dayOfWeek);
    }

    /**
     * Returns a public-friendly day name.
     */
    public String getDayName() {
        String value = getJavaDayOfWeek().name();

        return value.charAt(0)
                + value.substring(1).toLowerCase();
    }

    /**
     * Returns true when explicit operating times are configured.
     */
    public boolean hasOperatingTimes() {
        return openingTime != null && closingTime != null;
    }

    private void initializeDefaults() {
        if (isClosed == null) {
            isClosed = false;
        }

        if (isByAppointment == null) {
            isByAppointment = true;
        }

        if (displayOrder == null) {
            displayOrder = dayOfWeek == null
                    ? 0
                    : dayOfWeek.intValue();
        }
    }

    private void normalizeFields() {
        displayText = normalizeOptional(displayText);

        if (Boolean.TRUE.equals(isClosed)) {
            openingTime = null;
            closingTime = null;
        }
    }

    private void validateState() {
        if (businessProfile == null) {
            throw new IllegalStateException(
                    "Website business profile is required."
            );
        }

        validateDayOfWeek(dayOfWeek);

        if (displayOrder < 0) {
            throw new IllegalStateException(
                    "Display order must not be negative."
            );
        }

        boolean onlyOneTimeProvided =
                (openingTime == null) != (closingTime == null);

        if (onlyOneTimeProvided) {
            throw new IllegalStateException(
                    "Opening time and closing time must either both be "
                            + "provided or both be empty."
            );
        }

        if (
                !Boolean.TRUE.equals(isClosed)
                        && openingTime != null
                        && !openingTime.isBefore(closingTime)
        ) {
            throw new IllegalStateException(
                    "Opening time must be before closing time."
            );
        }
    }

    private void validateDayOfWeek(
            Short value
    ) {
        if (value == null || value < 1 || value > 7) {
            throw new IllegalStateException(
                    "Day of week must be between 1 and 7."
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