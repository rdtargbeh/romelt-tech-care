package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SETTINGS GROUP RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns all public non-sensitive settings in one group as a compact
 * key-value object.
 *
 * Example:
 *
 * {
 *   "settingGroup": "BRANDING",
 *   "settings": {
 *     "PRIMARY_COLOR": "#1976D2",
 *     "SECONDARY_COLOR": "#D4AF37",
 *     "ACCENT_COLOR": "#C62828"
 *   }
 * }
 * ================================================================
 */
public record PublicWebsiteSettingsGroupResponse(

        String settingGroup,

        Map<String, JsonNode> settings
) {
}