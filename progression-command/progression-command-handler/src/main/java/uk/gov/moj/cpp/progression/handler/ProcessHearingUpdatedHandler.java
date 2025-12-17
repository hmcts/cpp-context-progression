package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.ProcessHearingUpdated;
import uk.gov.justice.core.courts.UpdateListingNumberToHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

@ServiceComponent(COMMAND_HANDLER)
public class ProcessHearingUpdatedHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.process-hearing-updated")
    public void handle(final Envelope<ProcessHearingUpdated> processHearingUpdatedEnvelope) throws EventStreamException {

        final ProcessHearingUpdated processHearingUpdated = processHearingUpdatedEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(processHearingUpdated.getConfirmedHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> event = hearingAggregate.processHearingUpdated(processHearingUpdated.getConfirmedHearing(), processHearingUpdated.getUpdatedHearing());
        appendEventsToStream(processHearingUpdatedEnvelope, eventStream, event);
    }

    @Handles("progression.command.update-listing-number-to-hearing")
    public void handleListingNumber(final Envelope<UpdateListingNumberToHearing> updateListingNumberToHearingEnvelope)  throws EventStreamException{
        final UpdateListingNumberToHearing updateListingNumberToHearing = updateListingNumberToHearingEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateListingNumberToHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateHearingWithListingNumber(updateListingNumberToHearing.getProsecutionCaseId(),updateListingNumberToHearing.getHearingId(),  updateListingNumberToHearing.getOffenceListingNumbers());
        appendEventsToStream(updateListingNumberToHearingEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
