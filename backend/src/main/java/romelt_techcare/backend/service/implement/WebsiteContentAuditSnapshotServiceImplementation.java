package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT SNAPSHOT IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Converts explicitly selected fields into JSON-object audit
 * snapshots.
 *
 * The implementation rejects non-object output to remain aligned with
 * the database JSON constraints.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class WebsiteContentAuditSnapshotServiceImplementation
        implements WebsiteContentAuditSnapshotService {

    private final ObjectMapper objectMapper;

    @Override
    public JsonNode createSnapshot(
            Map<String, ?> fields
    ) {
        Map<String, ?> safeFields =
                fields == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(fields);

        JsonNode snapshot =
                objectMapper.valueToTree(safeFields);

        if (!snapshot.isObject()) {
            throw new IllegalStateException(
                    "Website content audit snapshot must be a JSON object."
            );
        }

        return snapshot;
    }
}