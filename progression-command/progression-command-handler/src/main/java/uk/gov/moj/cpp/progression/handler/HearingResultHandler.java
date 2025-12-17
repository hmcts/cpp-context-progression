package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.hearing.courts.HearingResult;
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
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class HearingResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.hearing-result")
    public void handle(final Envelope<HearingResult> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.hearing-result {}", envelope);
        final HearingResult hearingResult = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(hearingResult.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        Stream<Object> events ;
        if (isGroupProceedings(hearingResult.getHearing())) {
            final Optional<ProsecutionCase> groupMasterProsecutionCase = hearingResult.getHearing().getProsecutionCases().stream().filter(ProsecutionCase::getIsGroupMaster).findFirst();
            if (groupMasterProsecutionCase.isPresent()) {
                final HearingResult hearingResultsWithMemberCases = getUpdateDefendantListingStatusWithMemberCases(groupMasterProsecutionCase.get(), hearingResult);
                events = hearingAggregate.saveHearingResult(hearingResultsWithMemberCases.getHearing(), hearingResultsWithMemberCases.getSharedTime(), hearingResultsWithMemberCases.getShadowListedOffences());
            }else{
                LOGGER.error("Cannot handle groupProceedings hearing without a master case");
                return;
            }
        } else {
            events = hearingAggregate.saveHearingResult(hearingResult.getHearing(), hearingResult.getSharedTime(), hearingResult.getShadowListedOffences());
        }

        appendEventsToStream(envelope, eventStream, events);
    }

    private HearingResult getUpdateDefendantListingStatusWithMemberCases(final ProsecutionCase groupMasterProsecutionCase, final HearingResult hearingResult) {

        final Optional<Defendant> masterCaseDefendants = groupMasterProsecutionCase.getDefendants().stream().findFirst();

        final List<JudicialResult> defendantJudicialResultList = new ArrayList<>();
        if (masterCaseDefendants.isPresent() && nonNull(masterCaseDefendants.get().getDefendantCaseJudicialResults())) {
            defendantJudicialResultList.addAll(masterCaseDefendants.get().getDefendantCaseJudicialResults());
        }

        final Optional<Offence> masterCaseDefendantOffence = groupMasterProsecutionCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream()).findFirst();

        final List<JudicialResult> offenceJudicialResultList = new ArrayList<>();
        if(masterCaseDefendantOffence.isPresent() && nonNull(masterCaseDefendantOffence.get().getJudicialResults())) {
            offenceJudicialResultList.addAll(masterCaseDefendantOffence.get().getJudicialResults());
        }

        final EventStream stream = eventSource.getStreamById(groupMasterProsecutionCase.getGroupId());
        final GroupCaseAggregate groupCaseAggregate = aggregateService.get(stream, GroupCaseAggregate.class);
        final Hearing.Builder updatedHearingBuilder = Hearing.hearing().withValuesFrom(hearingResult.getHearing());
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        prosecutionCases.add(groupMasterProsecutionCase);

        groupCaseAggregate.getMemberCases().stream().filter(caseId -> groupMasterProsecutionCase.getId().compareTo(caseId) != 0).forEach(memberCaseId -> {
            final EventStream eventStream = eventSource.getStreamById(memberCaseId);
            final CaseAggregate memberCaseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final ProsecutionCase prosecutionCaseWithMemberCases = prepareProsecutionCase(defendantJudicialResultList, offenceJudicialResultList, memberCaseAggregate);
            prosecutionCases.add(prosecutionCaseWithMemberCases);
        });
        updatedHearingBuilder.withProsecutionCases(prosecutionCases);
        return HearingResult.hearingResult()
                .withValuesFrom(hearingResult)
                .withHearing(updatedHearingBuilder.build()).build();
    }

    private ProsecutionCase prepareProsecutionCase(final List<JudicialResult> defendantJudicialResults,final List<JudicialResult> offenceJudicialResults,final CaseAggregate caseAggregate) {
        final ProsecutionCase.Builder prosecutionCaseBuilder = ProsecutionCase.prosecutionCase()
                .withValuesFrom(caseAggregate.getProsecutionCase());
        final List<Defendant> defendants = new ArrayList<>();
        caseAggregate.getProsecutionCase().getDefendants().forEach(defendant -> {
            final Defendant newDefendant = prepareDefendantsWithJudicialResult(defendantJudicialResults, offenceJudicialResults, defendant);
            defendants.add(newDefendant);
        });
        prosecutionCaseBuilder.withDefendants(defendants);
        return prosecutionCaseBuilder.build();
    }

    private Defendant prepareDefendantsWithJudicialResult(final List<JudicialResult> defendantJudicialResults,final List<JudicialResult> offenceJudicialResults,final Defendant defendant) {
        final List<JudicialResult> newDefendantJudicialResults = defendantJudicialResults.stream().
                map(this::prepareJudicialResult).collect(Collectors.toList());
        final List<JudicialResult> newOffenceJudicialResults = offenceJudicialResults.stream().
                map(this::prepareJudicialResult).collect(Collectors.toList());
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

    private boolean isGroupProceedings(final Hearing hearing) {
        return nonNull(hearing.getIsGroupProceedings()) && hearing.getIsGroupProceedings() && isNotEmpty(hearing.getProsecutionCases()) ;
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
