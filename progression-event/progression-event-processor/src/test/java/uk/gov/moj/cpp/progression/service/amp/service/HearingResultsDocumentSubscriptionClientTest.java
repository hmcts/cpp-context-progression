package uk.gov.moj.cpp.progression.service.amp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventType;

import java.time.Instant;
import java.util.UUID;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class HearingResultsDocumentSubscriptionClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(WRITE_DATES_AS_TIMESTAMPS);

    private final HearingResultsDocumentSubscriptionClient client = new HearingResultsDocumentSubscriptionClient();

    @Test
    void serialize_emits_real_payload_object_not_jsonnode_introspection_fields() throws Exception {
        final JsonNode documentPayload = OBJECT_MAPPER.readTree(
                "{\"cases\":[{\"caseId\":\"11111111-1111-1111-1111-111111111111\"}],\"registerDate\":\"2026-05-29\"}");

        final PcrEventPayload payload = PcrEventPayload.builder()
                .eventType(PcrEventType.PRISON_COURT_REGISTER_GENERATED)
                .eventId(UUID.fromString("a4554152-10fb-44fe-a015-226f8d547c91"))
                .materialId(UUID.fromString("886a3d9c-2543-4fdd-8b5c-1597e3d36ebb"))
                .hearingId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"))
                .timestamp(Instant.parse("2026-05-29T10:23:29Z"))
                .defendant(PcrEventPayloadDefendant.builder()
                        .masterDefendantId(UUID.fromString("f08465c5-0000-0000-0000-000000000000"))
                        .name("Leo Kuhn")
                        .build())
                .payload(documentPayload)
                .build();

        final String body = client.serialize(payload);
        final JsonNode parsed = OBJECT_MAPPER.readTree(body);

        assertThat(parsed.get("payload").isObject(), is(true));
        assertThat(parsed.get("payload").has("cases"), is(true));
        assertThat(parsed.get("payload").has("nodeType"), is(false));
        assertThat(parsed.get("payload").has("containerNode"), is(false));
        assertThat(body, containsString("\"payload\":{"));
        assertThat(body, not(containsString("\"nodeType\":")));
    }
}