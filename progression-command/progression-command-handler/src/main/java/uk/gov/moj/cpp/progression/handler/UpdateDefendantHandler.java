package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.UpdateCaseDefendantWithDriverNumber;
import uk.gov.justice.core.courts.UpdateDefendantAddressOnCase;
import uk.gov.justice.core.courts.UpdateDefendantForHearing;
import uk.gov.justice.core.courts.UpdateDefendantForMatchedDefendant;
import uk.gov.justice.core.courts.UpdateDefendantForProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingWithNewDefendant;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;
    @Inject
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-defendant-for-prosecution-case")
    public void handle(final Envelope<UpdateDefendantForProsecutionCase> updateDefendantEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.command.update-defendant-for-prosecution-case, caseId :: {}", updateDefendantEnvelope.payload().getProsecutionCaseId());
        }
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(updateDefendantEnvelope.metadata(), JsonValue.NULL);
        final UpdateDefendantForProsecutionCase defendantDetailsToUpdate = updateDefendantEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(defendantDetailsToUpdate.getDefendant().getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final List<UUID> allHearingIdsForCase = prosecutionCaseQueryService.getAllHearingIdsForCase(jsonEnvelope, defendantDetailsToUpdate.getDefendant().getProsecutionCaseId());
        final Stream<Object> events = caseAggregate.updateDefendantDetails(defendantDetailsToUpdate.getDefendant(),allHearingIdsForCase);

        appendEventsToStream(updateDefendantEnvelope, eventStream, events);

    }

     @Handles("progression.command.update-defendant-address-on-case")
    public void handleUpdateDefendantAddressOnCase(final Envelope<UpdateDefendantAddressOnCase> updateDefendantEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.command.update-defendant-address-on-case {}", updateDefendantEnvelope.payload().getProsecutionCaseId());
        }

        final UpdateDefendantAddressOnCase  defendantDetailsToUpdate = updateDefendantEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(defendantDetailsToUpdate.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateDefendantAddress(defendantDetailsToUpdate.getDefendant());

        appendEventsToStream(updateDefendantEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-matched-defendant-custodial-information")
    public void handleCommandUpdateMatchedDefendantCustodialInformation(final Envelope<UpdateMatchedDefendantCustodialInformation> updateDefendantEnvelope) throws EventStreamException {
        LOGGER.info("progression.command.update-matched-defendant-custodial-information {}", updateDefendantEnvelope.payload());

        final UpdateMatchedDefendantCustodialInformation defendantDetailsToUpdate = updateDefendantEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(defendantDetailsToUpdate.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateDefendantCustodialInformationDetails(defendantDetailsToUpdate);

        appendEventsToStream(updateDefendantEnvelope, eventStream, events);

    }

    @Handles("progression.command.update-cps-defendant-id")
    public void handleUpdateCpsDefendantId(final Envelope<UpdateCpsDefendantId> updateCpsDefendantIdEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-cps-defendant-id {}", updateCpsDefendantIdEnvelope.payload());

        final UpdateCpsDefendantId updateCpsDefendantId = updateCpsDefendantIdEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateCpsDefendantId.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateCpsDefendantId(updateCpsDefendantId.getCaseId(), updateCpsDefendantId.getDefendantId(), updateCpsDefendantId.getCpsDefendantId());

        appendEventsToStream(updateCpsDefendantIdEnvelope, eventStream, events);

    }

    @Handles("progression.command.update-defendant-for-matched-defendant")
    public void handleUpdateDefendantForMatchedDefendant(final Envelope<UpdateDefendantForMatchedDefendant> updateDefendantEnvelope) throws EventStreamException {
        final UpdateDefendantForMatchedDefendant defendantDetailsToUpdate = updateDefendantEnvelope.payload();
        final UUID hearingId = defendantDetailsToUpdate.getMatchedDefendantHearingId();
        final DefendantUpdate defendantUpdate = defendantDetailsToUpdate.getDefendant();

        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.recordUpdateMatchedDefendantDetailRequest(defendantUpdate);
        appendEventsToStream(updateDefendantEnvelope, eventStream, events);

    }

    @Handles("progression.command.update-defendant-for-hearing")
    public void handleUpdateDefendantForHearing(final Envelope<UpdateDefendantForHearing> updateDefendantEnvelope) throws EventStreamException {
        final UpdateDefendantForHearing defendantDetailsToUpdate = updateDefendantEnvelope.payload();
        final UUID hearingId = defendantDetailsToUpdate.getHearingId();
        final DefendantUpdate defendantUpdate = defendantDetailsToUpdate.getDefendant();

        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateDefendant(hearingId, defendantUpdate);
        appendEventsToStream(updateDefendantEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-hearing-with-new-defendant")
    public void handleUpdateHearingWithNewDefendant(final Envelope<UpdateHearingWithNewDefendant> updateHearingWithNewDefendantEnvelope) throws EventStreamException {
        final UpdateHearingWithNewDefendant defendantDetailsToUpdate = updateHearingWithNewDefendantEnvelope.payload();
        final UUID hearingId = defendantDetailsToUpdate.getHearingId();

        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.addDefendant(hearingId, defendantDetailsToUpdate.getProsecutionCaseId(), defendantDetailsToUpdate.getDefendants());
        appendEventsToStream(updateHearingWithNewDefendantEnvelope, eventStream, events);
    }

    @Handles("progression.update-case-defendant-with-driver-number")
    public void handlerUpdateDefendantWithDriverNumber(final Envelope<UpdateCaseDefendantWithDriverNumber> updateCaseDefendantWithDriverNumberEnvelope) throws EventStreamException {
        LOGGER.debug("progression.update-case-defendant-with-driver-number {}", updateCaseDefendantWithDriverNumberEnvelope.payload());

        final UpdateCaseDefendantWithDriverNumber updateCaseDefendantWithDriverNumber = updateCaseDefendantWithDriverNumberEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateCaseDefendantWithDriverNumber.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateDefendantWithDriverNumber(updateCaseDefendantWithDriverNumber.getDefendantId(), updateCaseDefendantWithDriverNumber.getProsecutionCaseId(), updateCaseDefendantWithDriverNumber.getDriverNumber());

        appendEventsToStream(updateCaseDefendantWithDriverNumberEnvelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
