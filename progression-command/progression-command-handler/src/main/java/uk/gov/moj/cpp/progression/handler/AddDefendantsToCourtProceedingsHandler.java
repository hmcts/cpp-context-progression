package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDefendantsToCourtProceedingsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantsToCourtProceedingsHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private MatchedDefendantLoadService matchedDefendantLoadService;

    @Handles("progression.command.add-defendants-to-court-proceedings")
    public void handle(final Envelope<AddDefendantsToCourtProceedings> addDefendantEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-defendants-to-court-proceedings {}", addDefendantEnvelope.payload());

        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = addDefendantEnvelope.payload();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(addDefendantsToCourtProceedings.getDefendants().get(0).getProsecutionCaseId())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(addDefendantsToCourtProceedings.getDefendants())
                .build();

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(addDefendantEnvelope, prosecutionCase);

        final EventStream eventStream = eventSource.getStreamById(addDefendantsToCourtProceedings.getDefendants().get(0).getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.defendantsAddedToCourtProceedings(
                addDefendantsToCourtProceedings.getDefendants(),
                addDefendantsToCourtProceedings.getListHearingRequests());

        appendEventsToStream(addDefendantEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
