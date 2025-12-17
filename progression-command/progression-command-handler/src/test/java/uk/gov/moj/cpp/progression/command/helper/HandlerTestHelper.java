package uk.gov.moj.cpp.progression.command.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HandlerTestHelper {


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();


    private HandlerTestHelper() {
    }


    public static List<JsonEnvelope> toList(final Stream<JsonEnvelope> jsonEnvelopeStream) {
        return jsonEnvelopeStream.collect(Collectors.toList());
    }

    //TODO: refactor into matcher (needs techpod support)
    public static void matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName,
                                  final JsonValue expectedResultPayload) {

        final Iterator<JsonEnvelope> jsonEnvelopeIterator = toList(jsonEnvelopeStream).iterator();

        matchEvent(jsonEnvelopeIterator, eventName, expectedResultPayload);

    }

    public static void matchEvent(final List<JsonEnvelope> jsonEnvelopeList,
                                  final String eventName,
                                  final JsonValue expectedResultPayload) {

        matchEvent(jsonEnvelopeList.iterator(), eventName, expectedResultPayload);

    }

    public static void matchEvent(final Iterator<JsonEnvelope> jsonEnvelopeIterator,
                                  final String eventName,
                                  final JsonValue expectedResultPayload) {

        boolean matched = false;

        while (jsonEnvelopeIterator.hasNext()) {
            final JsonEnvelope jsonEnvelope = jsonEnvelopeIterator.next();
            if (jsonEnvelope.metadata().name().equals(eventName)) {
                matched = true;
                final JsonNode actualEvent = generatedEventAsJsonNode(jsonEnvelope.payloadAsJsonObject());
                assertThat(actualEvent, equalTo(generatedEventAsJsonNode(expectedResultPayload)));
                break;
            }

        }

        assertTrue(matched);
    }


    public static Metadata metadataFor(final String commandName, final UUID commandId) {
        return metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }

    public static JsonNode generatedEventAsJsonNode(final Object generatedEvent) {
        return OBJECT_MAPPER.valueToTree(generatedEvent);
    }
}
