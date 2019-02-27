package uk.gov.moj.cpp.progression.command.handler;

import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.newCaseDocumentReceivedEvent;

import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(Component.COMMAND_HANDLER)
public class NewCaseDocumentReceivedHandler extends CaseProgressionCommandHandler {


    @Handles("progression.command.upload-case-documents")
    public void uploadCaseDocument(final JsonEnvelope command) throws EventStreamException {
        final UUID streamId = UUID.randomUUID();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(newCaseDocumentReceivedEvent(command));
        final Stream<Object> events = streamBuilder.build();
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));
    }
}

