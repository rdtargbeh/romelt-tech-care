package romelt_techcare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT SNAPSHOT SERVICE
 * ================================================================
 *
 * Purpose:
 * Creates controlled JSON-object snapshots for CMS audit records.
 *
 * Security:
 * Callers must provide only approved non-sensitive fields.
 * ================================================================
 */
public interface WebsiteContentAuditSnapshotService {

    JsonNode createSnapshot(
            Map<String, ?> fields
    );
}