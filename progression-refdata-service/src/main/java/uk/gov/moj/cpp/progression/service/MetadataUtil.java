package uk.gov.moj.cpp.progression.service;



import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MetadataUtil {

    private MetadataUtil() {
    }

    public static Metadata metadataWithNewActionName(final Metadata metadata, final String actionName) {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(actionName)
                .createdAt(ZonedDateTime.now())
                .withCausation(metadata.causation().toArray(new UUID[metadata.causation().size()]));

        metadata.clientCorrelationId().ifPresent(metadataBuilder::withClientCorrelationId);
        metadata.sessionId().ifPresent(metadataBuilder::withSessionId);
        metadata.streamId().ifPresent(metadataBuilder::withStreamId);
        metadata.userId().ifPresent(metadataBuilder::withUserId);

        return metadataBuilder.build();
    }
}