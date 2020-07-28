package uk.gov.justice.framework.tools.transformation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public class EventLogBuilder {

    private EventLogBuilder() {
        // should not create an instance of this class
    }

    public static Event eventLogFrom(
            final String eventName,
            final Long sequenceId,
            final UUID streamId,
            final ZonedDateTime createdAt) {

        return eventLogFrom(eventName, sequenceId, streamId, createdAt, empty());

    }

    public static Event eventLogFrom(
            final String eventName,
            final Long sequenceId,
            final UUID streamId,
            final ZonedDateTime createdAt,
            final long eventNumber) {

        return eventLogFrom(eventName, sequenceId, streamId, createdAt, of(eventNumber));
    }

    public static Event eventLogFrom(
            final String eventName,
            final Long sequenceId,
            final UUID streamId,
            final ZonedDateTime createdAt,
            final Optional<Long> eventNumber) {

        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataWithRandomUUID(eventName)
                        .createdAt(createdAt)
                        .withVersion(sequenceId)
                        .withStreamId(streamId)
                        .withSource("sample")
                )
                .withPayloadOf("test", "a string")
                .build();

        final Metadata metadata = jsonEnvelope.metadata();
        final UUID id = metadata.id();

        final String name = metadata.name();
        final String payload = jsonEnvelope.payloadAsJsonObject().toString();

        return new Event(
                id,
                streamId,
                sequenceId,
                name,
                metadata.asJsonObject().toString(),
                payload,
                createdAt,
                eventNumber);
    }


    public static Event eventLogFrom(
            final String eventName,
            final Long sequenceId,
            final UUID streamId,
            final ZonedDateTime createdAt,
            final Optional<Long> eventNumber,
            final String payStr) {

        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataWithRandomUUID(eventName)
                        .createdAt(createdAt)
                        .withVersion(sequenceId)
                        .withStreamId(streamId)
                        .withSource("sample")
                )
                .withPayloadOf(payStr, eventName)
                .build();

        final Metadata metadata = jsonEnvelope.metadata();
        final UUID id = metadata.id();

        final String name = metadata.name();
     //   final String payload = jsonEnvelope.payloadAsJsonObject().toString();

        return new Event(
                id,
                streamId,
                sequenceId,
                name,
                metadata.asJsonObject().toString(),
                payStr,
                createdAt,
                eventNumber);
    }
}
