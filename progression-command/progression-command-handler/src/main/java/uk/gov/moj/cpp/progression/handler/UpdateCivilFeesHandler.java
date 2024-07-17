package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.UpdateCivilFees;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCivilFeesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCivilFeesHandler.class.getName());

    private static final String PROGRESSION_COMMAND_HANDLER_CIVIL_FEE = "progression.command.update-civil-fees";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles(PROGRESSION_COMMAND_HANDLER_CIVIL_FEE)
    public void handleCivilFee(final Envelope<UpdateCivilFees> updateCivilFeesEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-civil-fees {} ", updateCivilFeesEnvelope.payload());

        final UpdateCivilFees updateCivilFees = updateCivilFeesEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateCivilFees.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateCivilFees(updateCivilFees.getCaseId(),
                updateCivilFees.getCivilFees());

        appendEventsToStream(updateCivilFeesEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
