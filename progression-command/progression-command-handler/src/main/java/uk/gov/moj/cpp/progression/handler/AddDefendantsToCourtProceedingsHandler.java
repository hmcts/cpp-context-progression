package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReplayDefendantsAddedToCourtProceedings;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    private static final String SOW_REF_VALUE = "MoJ";

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
        final List<String> offenceCodes = prosecutionCase.getDefendants().stream().flatMap(defendant -> defendant.getOffences().stream())
                .map(Offence::getOffenceCode)
                .distinct()
                .collect(Collectors.toList());
        final Optional<String> sowRef = getSowRef(prosecutionCase);
        final Optional<List<JsonObject>> offencesJsonObjectOptional = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelopeFrom(addDefendantEnvelope.metadata(), JsonValue.NULL), requester, sowRef);

        final Stream<Object> events = caseAggregate.defendantsAddedToCourtProceedings(
                addDefendantsToCourtProceedings.getDefendants(),
                addDefendantsToCourtProceedings.getListHearingRequests(), offencesJsonObjectOptional);

        appendEventsToStream(addDefendantEnvelope, eventStream, events);
    }

    @Handles("progression.command.replay-defendants-added-to-court-proceedings")
    public void handleReplay(final Envelope<ReplayDefendantsAddedToCourtProceedings> replayAddDefendantEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(replayAddDefendantEnvelope.payload().getDefendants().get(0).getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.replayDefendantsAddedToCourtProceedings(
                replayAddDefendantEnvelope.payload().getDefendants(),
                replayAddDefendantEnvelope.payload().getListHearingRequests(),
                replayAddDefendantEnvelope.payload().getInterval());

        appendEventsToStream(replayAddDefendantEnvelope, eventStream, events);
    }

    private static Optional<String> getSowRef(final ProsecutionCase prosecutionCase) {
        boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        return isCivil ? Optional.of(SOW_REF_VALUE) : Optional.empty();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
