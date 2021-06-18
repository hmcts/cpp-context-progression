package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.progression.courts.CreateProsecutionCaseInHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import javax.inject.Inject;
import java.util.stream.Stream;

@ServiceComponent(COMMAND_HANDLER)
public class CreateProsecutionCaseInHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateProsecutionCaseInHearingCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.create-prosecution-case-in-hearing")
    public void createProsecutionCaseInHearing(final Envelope<CreateProsecutionCaseInHearing> createProsecutionCaseInHearingEnvelope) throws EventStreamException {

        final CreateProsecutionCaseInHearing createProsecutionCaseInHearing = createProsecutionCaseInHearingEnvelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'progression.command.create-prosecution-case-in-hearing' received with payload {}", createProsecutionCaseInHearing);
        }

        final EventStream eventStream = eventSource.getStreamById(createProsecutionCaseInHearing.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate.createProsecutionCaseInHearing(createProsecutionCaseInHearing.getProsecutionCaseId());

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(createProsecutionCaseInHearingEnvelope)));

    }

}