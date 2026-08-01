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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS-HOUR EXCEPTION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores date-specific exceptions that override the normal weekly
 * Romelt TechCare business schedule.
 *
 * Responsibilities:
 * - Supports holidays and scheduled closures.
 * - Supports vacation periods through individual date records.
 * - Supports emergency closures.
 * - Supports special opening and closing times.
 * - Supports appointment-only service on a specific date.
 * - Associates each exception with one business profile.
 * - Records administrator creation and update attribution.
 * - Supports optimistic locking.
 *
 * Override behavior:
 * When an exception exists for a date, it takes precedence over the
 * normal WebsiteBusinessHour row for that date's weekday.
 *
 * Business rules:
 * - One exception may exist per business profile and date.
 * - Closed exceptions do not retain opening or closing times.
 * - Opening and closing times must either both be present or both null.
 * - Opening time must be before closing time.
 * ================================================================
 */
@Entity
@Table(
        name = "website_business_hour_exceptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_hour_exception_date",
                        columnNames = {
                                "business_profile_id",
                                "exception_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_hour_exceptions_date",
                        columnList = "business_profile_id, exception_date"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteBusinessHourException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "business_hour_exception_id",
            nullable = false,
            updatable = false
    )
    private UUID businessHourExceptionId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "business_profile_id",
            nullable = false
    )
    private WebsiteBusinessProfile businessProfile;

    @Column(
            name = "exception_date",
            nullable = false
    )
    private LocalDate exceptionDate;

    @Column(
            name = "exception_name",
            length = 180
    )
    private String exceptionName;

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
            length = 180
    )
    private String displayText;

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
     * Applies the complete editable exception state.
     */
    public void updateException(
            LocalDate newExceptionDate,
            String newExceptionName,
            boolean closed,
            boolean byAppointment,
            LocalTime newOpeningTime,
            LocalTime newClosingTime,
            String newDisplayText,
            AdminUser administrator
    ) {
        exceptionDate = newExceptionDate;
        exceptionName = newExceptionName;
        isClosed = closed;
        isByAppointment = byAppointment;

        openingTime = closed
                ? null
                : newOpeningTime;

        closingTime = closed
                ? null
                : newClosingTime;

        displayText = newDisplayText;
        updatedByAdminUser = administrator;
    }

    /**
     * Returns true when explicit operating times are configured.
     */
    public boolean hasOperatingTimes() {
        return openingTime != null
                && closingTime != null;
    }

    /**
     * Returns true when this exception represents a complete closure.
     */
    public boolean isCompleteClosure() {
        return Boolean.TRUE.equals(isClosed);
    }

    private void initializeDefaults() {
        if (isClosed == null) {
            isClosed = false;
        }

        if (isByAppointment == null) {
            isByAppointment = true;
        }
    }

    private void normalizeFields() {
        exceptionName = normalizeOptional(exceptionName);
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

        if (exceptionDate == null) {
            throw new IllegalStateException(
                    "Exception date is required."
            );
        }

        boolean onlyOneTimeProvided =
                (openingTime == null)
                        != (closingTime == null);

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