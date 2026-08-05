package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR ACCOUNT STATUS
 * ================================================================
 *
 * Purpose:
 * Defines whether an administrator account may authenticate and
 * access protected administrative resources.
 *
 * Authentication behavior:
 * - ACTIVE accounts may sign in.
 * - INACTIVE accounts are disabled.
 * - LOCKED accounts are blocked because of security or policy rules.
 * ================================================================
 */
public enum AdminStatus {

    /**
     * Account is enabled and may authenticate.
     */
    ACTIVE,

    /**
     * Account has been disabled administratively.
     */
    INACTIVE,

    /**
     * Account has been locked because of failed sign-in attempts
     * or another security action.
     */
    LOCKED
}