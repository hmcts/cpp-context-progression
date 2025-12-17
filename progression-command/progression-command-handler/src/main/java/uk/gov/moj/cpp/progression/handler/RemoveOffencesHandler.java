package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import uk.gov.justice.hearing.courts.RemoveOffencesFromExistingHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;


import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RemoveOffencesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOffencesHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.remove-offences-from-existing-hearing")
    public void removeOffencesFromExistingHearing(final Envelope<RemoveOffencesFromExistingHearing> removeOffencesEnvelope) throws EventStreamException {

        LOGGER.debug("progression.command.remove-offences-from-existing-hearing {}", removeOffencesEnvelope.payload());

        final RemoveOffencesFromExistingHearing removeOffencesFromExistingHearing = removeOffencesEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(removeOffencesFromExistingHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.removeOffenceFromHearing(removeOffencesFromExistingHearing.getHearingId(), removeOffencesFromExistingHearing.getOffenceIds(), removeOffencesFromExistingHearing.getIsResultFlow());
        appendEventsToStream(removeOffencesEnvelope, eventStream, events);
    }

}