package uk.gov.moj.cpp.progression.service.amp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves {@code PcrEventPayload} with {@code JsonNode payload} produces valid JSON AMP accepts.
 * Root cause: {@code @JsonRawValue} on a field is ignored by the RESTEasy Jackson provider →
 * payload serialised as a quoted string → AMP's {@code Map<String, Object>} 400.
 * Fix: {@code JsonNode} serialises inline natively.
 */
class PcrEventPayloadSerializationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializing_pcr_payload_as_json_node_produces_valid_json_accepted_by_amp() throws Exception {
        final JsonNode rawPcrPayload = loadFixture("pcr-payload-production-sample.json");
        final PcrEventPayload payload = buildProductionPayload(rawPcrPayload);

        final String json = OBJECT_MAPPER.writeValueAsString(payload);

        final JsonNode parsed = assertDoesNotThrow(
                () -> OBJECT_MAPPER.readTree(json),
                "Serialized PcrEventPayload must be valid JSON that AMP's Jackson can parse");
        assertNotNull(parsed);
        assertThat(json, containsString("\"payload\":{"));
        assertThat(json, not(containsString("actual bodily harm.\nContrary")));
        assertThat(json, containsString("bodily harm.\\nContrary"));
    }

    @Test
    void serialized_payload_carries_real_content_not_jsonnode_introspection_fields() throws Exception {
        final JsonNode rawPcrPayload = loadFixture("pcr-payload-production-sample.json");
        final PcrEventPayload payload = buildProductionPayload(rawPcrPayload);

        final String json = OBJECT_MAPPER.writeValueAsString(payload);
        final JsonNode parsed = OBJECT_MAPPER.readTree(json);
        final JsonNode payloadNode = parsed.get("payload");

        assertThat(payloadNode.isObject(), is(true));
        assertThat(payloadNode.has("cases"), is(true));
        assertThat(payloadNode.has("registerDate"), is(true));
        assertThat(payloadNode.get("cases").isArray(), is(true));

        for (final String introspectionKey : List.of(
                "nodeType", "containerNode", "object", "array", "textual",
                "valueNode", "missingNode", "int", "long", "double", "float", "pojo")) {
            assertThat("payload must not contain JsonNode introspection field '" + introspectionKey + "'",
                    payloadNode.has(introspectionKey), is(false));
        }
        assertThat(json, not(containsString("\"nodeType\":")));
        assertThat(json, not(containsString("\"containerNode\":")));

        assertThat(parsed.get("timestamp").asText(), is("2026-05-29T10:23:29Z"));
        assertThat(parsed.get("eventId").asText(), is("a4554152-10fb-44fe-a015-226f8d547c91"));
        assertThat(parsed.get("materialId").asText(), is("886a3d9c-2543-4fdd-8b5c-1597e3d36ebb"));
        assertThat(parsed.get("hearingId").asText(), is("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
        assertThat(parsed.get("defendant").get("name").asText(), is("Leo Kuhn"));
    }

    private PcrEventPayload buildProductionPayload(final JsonNode rawPayload) {
        return PcrEventPayload.builder()
                .eventType(PcrEventType.PRISON_COURT_REGISTER_GENERATED)
                .eventId(UUID.fromString("a4554152-10fb-44fe-a015-226f8d547c91"))
                .materialId(UUID.fromString("886a3d9c-2543-4fdd-8b5c-1597e3d36ebb"))
                .hearingId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"))
                .timestamp(Instant.parse("2026-05-29T10:23:29Z"))
                .defendant(PcrEventPayloadDefendant.builder()
                        .masterDefendantId(UUID.fromString("f08465c5-0000-0000-0000-000000000000"))
                        .name("Leo Kuhn")
                        .dateOfBirth(LocalDate.of(2000, 3, 24))
                        .custodyEstablishmentDetails(PcrEventPayloadCustodyEstablishmentDetails.builder()
                                .emailAddress("lavenderhill@prison.gov.uk")
                                .build())
                        .cases(List.of(PcrEventPayloadDefendantCases.builder()
                                .urn("28DI8140839")
                                .build()))
                        .build())
                .payload(rawPayload)
                .build();
    }

    private JsonNode loadFixture(final String name) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new IllegalStateException("Fixture not found: " + name);
            return OBJECT_MAPPER.readTree(new String(is.readAllBytes(), StandardCharsets.UTF_8).trim());
        }
    }
}
