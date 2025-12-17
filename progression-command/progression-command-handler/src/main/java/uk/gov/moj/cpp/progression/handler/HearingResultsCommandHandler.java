package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.*;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class HearingResultsCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultsCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Handles("progression.command.process-hearing-results")
    public void processHearingResults(final Envelope<HearingResult> envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.process-hearing-results {}", envelope);
        }

        final HearingResult hearingResultShared = envelope.payload();

        final JsonObject payload = createObjectBuilder().add("category", "F").add("on", LocalDate.now().toString()).build();

        final MetadataBuilder metadata = metadataFrom(envelope.metadata()).withName("referencedata.query-result-definitions-with-category");

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadata, payload));

        final JsonArray resultsArray = jsonEnvelope.payloadAsJsonObject().getJsonArray("resultDefinitions");

        final List<UUID> resultIdList = resultsArray.stream()
                .map(jsonValue -> {
                    final JsonObject jsonObject = (JsonObject) jsonValue;
                    return UUID.fromString(jsonObject.getString("id"));
                })
                .collect(toList());

        final EventStream eventStream = eventSource.getStreamById(hearingResultShared.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events;
        if (isGroupProceedings(hearingResultShared.getHearing())) {
            final Optional<ProsecutionCase> groupMasterProsecutionCase = hearingResultShared.getHearing().getProsecutionCases().stream()
                    .filter(ProsecutionCase::getIsGroupMaster)
                    .findFirst();
            if (groupMasterProsecutionCase.isPresent()) {
                final HearingResult hearingResultsWithMemberCases = updateMemberCasesWithJudicialResultFromMasterCase(groupMasterProsecutionCase.get(), hearingResultShared);
                events = hearingAggregate.processHearingResults(hearingResultsWithMemberCases.getHearing(), hearingResultsWithMemberCases.getSharedTime(), hearingResultsWithMemberCases.getShadowListedOffences(), hearingResultsWithMemberCases.getHearingDay(), resultIdList);
            } else {
                LOGGER.error("Cannot handle groupProceedings hearing without a master case");
                return;
            }
        } else {
            events = hearingAggregate.processHearingResults(hearingResultShared.getHearing(), hearingResultShared.getSharedTime(), hearingResultShared.getShadowListedOffences(), hearingResultShared.getHearingDay(),resultIdList);
        }

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.store-booking-reference-court-schedule-ids")
    public void processStoreBookingReferencesWithCourtScheduleIdsCommand(final Envelope<StoreBookingReferenceCourtScheduleIds> envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.store-booking-reference-court-schedule-ids {}", envelope);
        }

        final StoreBookingReferenceCourtScheduleIds command = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.storeBookingReferencesWithCourtScheduleIds(command.getBookingReferenceCourtScheduleIds(), command.getHearingDay());

        appendEventsToStream(envelope, eventStream, events);
    }

    private boolean isGroupProceedings(final Hearing hearing) {
        return nonNull(hearing.getIsGroupProceedings())
                && hearing.getIsGroupProceedings()
                && isNotEmpty(hearing.getProsecutionCases());
    }

    private HearingResult updateMemberCasesWithJudicialResultFromMasterCase(final ProsecutionCase groupMasterProsecutionCase, final HearingResult hearingResult) {


        final List<JudicialResult> defendantJudicialResultList = new ArrayList<>();
        final List<JudicialResult> offenceJudicialResultList = new ArrayList<>();


        final Optional<Defendant> masterCaseDefendants = groupMasterProsecutionCase.getDefendants().stream().findFirst();
        if (masterCaseDefendants.isPresent() && nonNull(masterCaseDefendants.get().getDefendantCaseJudicialResults())) {
            defendantJudicialResultList.addAll(masterCaseDefendants.get().getDefendantCaseJudicialResults());
        }

        final Optional<Offence> masterCaseDefendantOffence = groupMasterProsecutionCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream()).findFirst();
        if (masterCaseDefendantOffence.isPresent() && nonNull(masterCaseDefendantOffence.get().getJudicialResults())) {
            offenceJudicialResultList.addAll(masterCaseDefendantOffence.get().getJudicialResults());
        }

        final EventStream stream = eventSource.getStreamById(groupMasterProsecutionCase.getGroupId());
        final GroupCaseAggregate groupCaseAggregate = aggregateService.get(stream, GroupCaseAggregate.class);

        final Hearing.Builder updatedHearingBuilder = Hearing.hearing().withValuesFrom(hearingResult.getHearing());
        final List<ProsecutionCase> updatedProsecutionCases = new ArrayList<>();

        updatedProsecutionCases.addAll(hearingResult.getHearing().getProsecutionCases());

        groupCaseAggregate.getMemberCases().stream().filter(caseId -> groupMasterProsecutionCase.getId().compareTo(caseId) != 0).forEach(memberCaseId -> {
            final EventStream eventStream = eventSource.getStreamById(memberCaseId);
            final CaseAggregate memberCaseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final ProsecutionCase prosecutionCaseWithMemberCases = prepareProsecutionCase(defendantJudicialResultList, offenceJudicialResultList, memberCaseAggregate.getProsecutionCase());
            updatedProsecutionCases.add(prosecutionCaseWithMemberCases);
        });
        updatedHearingBuilder.withProsecutionCases(updatedProsecutionCases);
        return HearingResult.hearingResult()
                .withValuesFrom(hearingResult)
                .withHearing(updatedHearingBuilder.build()).build();
    }

    private ProsecutionCase prepareProsecutionCase(final List<JudicialResult> defendantJudicialResults, final List<JudicialResult> offenceJudicialResults, final ProsecutionCase memberCase) {
        final ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase()
                .withValuesFrom(memberCase);
        final List<Defendant> defendants = new ArrayList<>();
        memberCase.getDefendants().forEach(defendant -> {
            final Defendant newDefendant = prepareDefendantsWithJudicialResult(defendantJudicialResults, offenceJudicialResults, defendant);
            defendants.add(newDefendant);
        });
        prosecutionCaseBuilder.withDefendants(defendants);
        return prosecutionCaseBuilder.build();
    }

    private Defendant prepareDefendantsWithJudicialResult(final List<JudicialResult> defendantJudicialResults, final List<JudicialResult> offenceJudicialResults, final Defendant defendant) {

        final List<JudicialResult> newDefendantJudicialResults;
        if (isNotEmpty(defendantJudicialResults)) {
            newDefendantJudicialResults = defendantJudicialResults.stream()
                    .map(this::prepareJudicialResult)
                    .collect(toList());
        } else {
            newDefendantJudicialResults = null;
        }

        final List<JudicialResult> newOffenceJudicialResults;
        if (isNotEmpty(offenceJudicialResults)) {
            newOffenceJudicialResults = offenceJudicialResults.stream()
                    .map(this::prepareJudicialResult)
                    .collect(toList());
        } else {
            newOffenceJudicialResults = null;
        }

        final Defendant.Builder newDefendantBuilder = Defendant.defendant()
                .withValuesFrom(defendant)
                .withDefendantCaseJudicialResults(newDefendantJudicialResults);

        final List<Offence> offences = new ArrayList<>();
        defendant.getOffences().forEach(offence -> {
            final Offence newOffence = Offence.offence()
                    .withValuesFrom(offence)
                    .withJudicialResults(newOffenceJudicialResults)
                    .build();
            offences.add(newOffence);
        });
        newDefendantBuilder.withOffences(offences);
        return newDefendantBuilder.build();
    }

    private JudicialResult prepareJudicialResult(final JudicialResult judicialResult) {
        return JudicialResult.judicialResult()
                .withValuesFrom(judicialResult)
                .withJudicialResultId(UUID.randomUUID())
                .build();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
