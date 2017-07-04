package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory;

import java.util.UUID;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NewCaseDocumentReceivedHandler extends CaseProgressionCommandHandler {

    private transient ProgressionEventFactory progressionEventFactory = new ProgressionEventFactory();

    @Handles("progression.command.upload-case-documents")
    public void uploadCaseDocument(final JsonEnvelope command) throws EventStreamException {
        final UUID streamId = UUID.randomUUID();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.newCaseDocumentReceivedEvent(command));
        final Stream<Object> events = streamBuilder.build();
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));
    }
}

