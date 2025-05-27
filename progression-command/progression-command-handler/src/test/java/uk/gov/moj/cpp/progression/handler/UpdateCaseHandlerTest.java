package uk.gov.moj.cpp.progression.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequested;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingResultedUpdateCase;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberUpdated;
import uk.gov.justice.core.courts.UpdateListingNumberToProsecutionCase;
import uk.gov.justice.progression.courts.CaseRetentionLengthCalculated;
import uk.gov.justice.progression.courts.IncreaseListingNumberToProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JudicialResultCategory.ANCILLARY;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.core.courts.JudicialResultCategory.INTERMEDIARY;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

@ExtendWith(MockitoExtension.class)
public class UpdateCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingResultedUpdateCase.class,
            HearingResultedCaseUpdated.class, DefendantTrialRecordSheetRequested.class, LaaDefendantProceedingConcludedChanged.class,
            CaseRetentionLengthCalculated.class, ProsecutionCaseListingNumberUpdated.class,
            ProsecutionCaseListingNumberIncreased.class);

    @InjectMocks
    private UpdateCaseHandler updateCaseHandler;

    private CaseAggregate aggregate;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();
    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> eventsStreamCaptor;


    @BeforeEach
    public void setup() {
        aggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCaseHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.hearing-resulted-update-case")
                ));
    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAtLeastOneofOffencesOfDefendantHaveFinalCategory_expectProceedingConcludedAsTrue() throws Exception {

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                                randomUUID()),
                        handlerTestHelper.convertFromFile("json/hearing-resulted-update-case.json", HearingResultedUpdateCase.class));

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(envelope.payload().getProsecutionCase())
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())

                                        )
                                )),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.defendant-trial-record-sheet-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", notNullValue()),
                                                withJsonPath("$.defendantId", notNullValue())
                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAllOffencesOfDefendantDoNotHaveFinalCategory_expectProceedingConcludedAsFalse() throws Exception {

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                                randomUUID()),
                        handlerTestHelper.convertFromFile("json/hearing-resulted-update-case-1.json", HearingResultedUpdateCase.class));

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(envelope.payload().getProsecutionCase())
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())
                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAllOffencesOfDefendantHaveNoFinalCategory_expectProceedingConcludedAsFalse() throws Exception {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .withOffences(asList(offence1, offence2))
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(defendantList).build();


        HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                                randomUUID()),
                        hearingResultedUpdateCase);

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())


                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAllOffencesOfDefendantHaveFinalCategoryDefendantHaveNoFinalCategory_expectProceedingConcludedAsTrue() throws Exception {

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                                randomUUID()),
                        createCommandPayloadWithHearingResultedUpdateCase());

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())
                                        )
                                )),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.defendant-trial-record-sheet-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", notNullValue()),
                                                withJsonPath("$.defendantId", notNullValue())
                                        )
                                ))
                )));
    }

    @Test
    public void shouldMarkCaseInactive_inMultipleHearings_whenAllOffencesOfDefendantHaveFinalCategory() throws Exception {
        final String caseURN = "case" + string(6).next();
        final UUID caseId = randomUUID();
        final Offence offence1 = getOffence(JudicialResultCategory.INTERMEDIARY);
        final Offence offence2 = getOffence(JudicialResultCategory.FINAL);
        final Offence offence3 = getOffence(ANCILLARY);
        final Defendant defendant = getDefendant(caseId, asList(offence1, offence2, offence3), emptyList());

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseURN, caseId, singletonList(defendant));

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        //hearing resulted with defendant one offence 1 FINAL
        final HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .withCourtCentre(getCourtCentre())
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case", randomUUID()),
                        hearingResultedUpdateCase);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendant.getId().toString())),
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[2].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())
                                        )
                                ))
                )));

        //hearing resulted for the same case and defendant with other offence 1 & 3 FINAL
        defendant.getOffences().remove(0);
        defendant.getOffences().add(0, getOffence(offence1.getId(), JudicialResultCategory.FINAL));
        defendant.getOffences().remove(2);
        defendant.getOffences().add(2, getOffence(offence3.getId(), JudicialResultCategory.FINAL));
        final HearingResultedUpdateCase hearingResultedUpdateCase2 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .withCourtCentre(getCourtCentre())
                .build();

        final Envelope<HearingResultedUpdateCase> envelope2 =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case", randomUUID()),
                        hearingResultedUpdateCase2);

        updateCaseHandler.handle(envelope2);

        verify(eventStream, times(2)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload = eventsList.get(0).payloadAsJsonObject();

        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getString("id"), is(defendant.getId().toString()));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(1).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(2).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.INACTIVE.getDescription()));

    }

    @Test
    public void shouldMarkCaseInactive_whenAllOffencesOfAllDefendantsHaveFinalCategory_inMultipleHearings() throws Exception {
        final String caseURN = "case" + string(6).next();
        final UUID caseId = randomUUID();
        final Offence offence1 = getOffence(JudicialResultCategory.FINAL);
        final Offence offence2 = getOffence(JudicialResultCategory.FINAL);
        final Defendant defendant1 = getDefendant(caseId, asList(offence1, offence2), emptyList());

        final Offence offence3 = getOffence(JudicialResultCategory.INTERMEDIARY);
        final Defendant defendant2 = getDefendant(caseId, singletonList(offence3), emptyList());
        final ProsecutionCase prosecutionCase = getProsecutionCase(caseURN, caseId, asList(defendant1, defendant2));

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        //hearing resulted with only defendant1 offences FINAL
        final HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .withCourtCentre(getCourtCentre())
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case", randomUUID()),
                        hearingResultedUpdateCase);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendant1.getId().toString())),
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())
                                        )
                                )),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.defendant-trial-record-sheet-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", notNullValue()),
                                                withJsonPath("$.defendantId", notNullValue())
                                        )
                                ))
                )));

        //hearing resulted for the same case with only defendant2 offences FINAL
        defendant2.getOffences().remove(0);
        final Offence updatedOffence3 = getOffence(offence3.getId(), JudicialResultCategory.FINAL);
        final Offence offence4 = getOffence(JudicialResultCategory.FINAL);
        defendant2.getOffences().add(updatedOffence3);
        defendant2.getOffences().add(offence4);
        final HearingResultedUpdateCase hearingResultedUpdateCase2 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .withCourtCentre(getCourtCentre())
                .build();

        final Envelope<HearingResultedUpdateCase> envelope2 =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case", randomUUID()),
                        hearingResultedUpdateCase2);

        updateCaseHandler.handle(envelope2);

        verify(eventStream, times(2)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload = eventsList.get(0).payloadAsJsonObject();

        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(1).getString("id"), is(defendant2.getId().toString()));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(1).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(1).getJsonArray("offences").getJsonObject(0).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(1).getJsonArray("offences").getJsonObject(1).getBoolean("proceedingsConcluded"), is(true));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.INACTIVE.getDescription()));

    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenOneOfOffenceHasNoJudicialResults_expectProceedingConcludedAsFalse() throws Exception {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).withIsNewAmendment(Boolean.TRUE).build(), JudicialResult.judicialResult()
                        .withCategory(ANCILLARY).withIsNewAmendment(Boolean.TRUE).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(asList(offence1, offence2))
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(defendantList).build();

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                                randomUUID()),
                        hearingResultedUpdateCase);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())


                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenOneOfOffenceIsAmendedWithFinalResult_expectProceedingConcludedAsTrueAndCaseInActive() throws Exception {
        final UUID caseId = randomUUID();
        final Offence offence1 = Offence.offence().withId(randomUUID()).build();
        final Offence offence2 = Offence.offence().withId(randomUUID()).build();
        final Offence offence3 = Offence.offence().withId(randomUUID()).build();
        final Offence offence4 = Offence.offence().withId(randomUUID()).build();

        final List<Defendant> defendantList = new ArrayList<>();
        final Defendant defendant1 = Defendant.defendant().withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence1, offence2))
                .build();
        defendantList.add(defendant1);
        final Defendant defendant2 = Defendant.defendant().withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence3, offence4))
                .build();
        defendantList.add(defendant2);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(defendantList).build();

        //case created event
        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        //Hearing H1
        // D1 O1 - A,  D1 O2 - I, D2 O1 - I, D2 O2 - I
        final Offence offence1Hearing1 = Offence.offence().withId(offence1.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(ANCILLARY).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Offence offence2Hearing1 = Offence.offence().withId(offence2.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.TRUE).build())).build();

        final Offence offence3Hearing1 = Offence.offence().withId(offence3.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Offence offence4Hearing1 = Offence.offence().withId(offence4.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Defendant defendant1Hearing1 = Defendant.defendant().withId(defendant1.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence1Hearing1, offence2Hearing1))
                .build();
        final Defendant defendant2Hearing1 = Defendant.defendant().withId(defendant2.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence3Hearing1, offence4Hearing1))
                .build();

        final ProsecutionCase prosecutionCaseHearing1 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant1Hearing1, defendant2Hearing1)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing1 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withProsecutionCase(prosecutionCaseHearing1)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing1);

        updateCaseHandler.handle(envelope);

        verify(eventStream, times(1)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload = eventsList.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, defendant1.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, 0, offence1.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, 1, offence2.getId(), false);

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, defendant2.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, 0, offence3.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, 1, offence4.getId(), false);

        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.ACTIVE.getDescription()));

        //Hearing H2
        // H2 D2 O1 - F
        final Offence offence3Hearing2 = Offence.offence().withId(offence3.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Defendant defendant2Hearing2 = Defendant.defendant().withId(defendant2.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence3Hearing2))
                .build();

        final ProsecutionCase prosecutionCaseHearing2 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant2Hearing2)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing2 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withProsecutionCase(prosecutionCaseHearing2)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope2 = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing2);

        updateCaseHandler.handle(envelope2);

        verify(eventStream, times(2)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList2 = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload2 = eventsList2.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 0, defendant2Hearing2.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 0, 0, offence3Hearing2.getId(), true);

        assertThat(hearingResultedCaseUpdatedPayload2.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.ACTIVE.getDescription()));


        //Hearing H3
        // H3 D1 O2 - F, D2 O2 - F
        final Offence offence2Hearing3 = Offence.offence().withId(offence2.getId())
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Defendant defendant1Hearing3 = Defendant.defendant().withId(defendant1.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence2Hearing3))
                .build();
        final Offence offence4Hearing3 = Offence.offence().withId(offence4.getId())
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Defendant defendant2Hearing3 = Defendant.defendant().withId(defendant2.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence4Hearing3))
                .build();

        final ProsecutionCase prosecutionCaseHearing3 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant1Hearing3, defendant2Hearing3)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing3 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withProsecutionCase(prosecutionCaseHearing3)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope3 = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing3);

        updateCaseHandler.handle(envelope3);

        verify(eventStream, times(3)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList3 = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload3 = eventsList3.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload3, 0, defendant1Hearing3.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload3, 0, 0, offence2Hearing3.getId(), true);

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload3, 1, defendant2Hearing3.getId(), true);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload3, 1, 0, offence4Hearing3.getId(), true);

        assertThat(hearingResultedCaseUpdatedPayload3.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.ACTIVE.getDescription()));


        //Hearing H4 is Amended H1
        // D1 O1 - A + F
        // no change to other results of Hearing1 i.e
        // D1 O2 - I  same as Hearing1
        // D2 O1 - I  same as Hearing1
        // D2 O2 - I  same as Hearing1

        final Offence offence1Hearing4 = Offence.offence().withId(offence1.getId())
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();

        final Offence offence2Hearing4 = Offence.offence().withId(offence2.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.FALSE).build())).build();
        final Defendant defendant1Hearing4 = Defendant.defendant().withId(defendant1.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence1Hearing4, offence2Hearing4))
                .build();

        final Offence offence3Hearing4 = Offence.offence().withId(offence3.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.FALSE).build())).build();
        final Offence offence4Hearing4 = Offence.offence().withId(offence4.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(INTERMEDIARY).withIsNewAmendment(Boolean.FALSE).build())).build();

        final Defendant defendant2Hearing4 = Defendant.defendant().withId(defendant2.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence3Hearing4, offence4Hearing4))
                .build();

        final ProsecutionCase prosecutionCaseHearing4 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant1Hearing4, defendant2Hearing4)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing4 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withProsecutionCase(prosecutionCaseHearing4)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope4 = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing4);

        updateCaseHandler.handle(envelope4);

        verify(eventStream, times(4)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList4 = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload4 = eventsList4.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 0, defendant1Hearing4.getId(), true);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 0, 0, offence1Hearing4.getId(), true);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 0, 1, offence2Hearing4.getId(), true);

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 1, defendant2Hearing4.getId(), true);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 1, 0, offence3Hearing4.getId(), true);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload4, 1, 1, offence4Hearing4.getId(), true);

        assertThat(hearingResultedCaseUpdatedPayload4.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.INACTIVE.getDescription()));
    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenOneOffencesOnDefendantNotHaveFinalResultYet_expectProceedingConcludedAsFalseAndCaseActive() throws Exception {
        final UUID caseId = randomUUID();
        final Offence offence1 = Offence.offence().withId(randomUUID()).build();
        final Offence offence2 = Offence.offence().withId(randomUUID()).build();
        final Offence offence3 = Offence.offence().withId(randomUUID()).build();

        final Defendant defendant1 = Defendant.defendant().withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence1, offence2, offence3))
                .build();

        final Offence offence21 = Offence.offence().withId(randomUUID()).build();
        final Offence offence22 = Offence.offence().withId(randomUUID()).build();
        final Offence offence23 = Offence.offence().withId(randomUUID()).build();

        final Defendant defendant2 = Defendant.defendant().withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence21, offence22, offence23))
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(asList(defendant1, defendant2)).build();

        //case created event
        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        //Hearing H1
        // D1 D level - F,  D2 D level - F
        final ProsecutionCase prosecutionCaseHearing1 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant1, defendant2)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing1 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withDefendantJudicialResults(asList(DefendantJudicialResult.defendantJudicialResult().withMasterDefendantId(defendant1.getId())
                                .withJudicialResult(JudicialResult.judicialResult().withOffenceId(offence1.getId()).withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build()).build(),
                        DefendantJudicialResult.defendantJudicialResult().withMasterDefendantId(defendant2.getId())
                                .withJudicialResult(JudicialResult.judicialResult().withOffenceId(offence21.getId()).withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build()).build()))
                .withProsecutionCase(prosecutionCaseHearing1)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing1);

        updateCaseHandler.handle(envelope);

        verify(eventStream, times(1)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload = eventsList.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, defendant1.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, 0, offence1.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, 1, offence2.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 0, 2, offence3.getId(), false);

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, defendant2.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, 0, offence21.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, 1, offence22.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload, 1, 2, offence23.getId(), false);

        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.ACTIVE.getDescription()));

        // Hearing H2
        // H2 D1 O1 - F, D2 O2 - F
        final Offence offence1Hearing2 = Offence.offence().withId(offence1.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Offence offence21Hearing2 = Offence.offence().withId(offence21.getId())
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(FINAL).withIsNewAmendment(Boolean.TRUE).build())).build();
        final Defendant defendant1Hearing2 = Defendant.defendant().withId(defendant1.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence1Hearing2))
                .build();

        final Defendant defendant2Hearing2 = Defendant.defendant().withId(defendant2.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence21Hearing2))
                .build();

        final ProsecutionCase prosecutionCaseHearing2 = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withCaseStatus(CaseStatusEnum.ACTIVE.getDescription())
                .withDefendants(asList(defendant1Hearing2, defendant2Hearing2)).build();

        final HearingResultedUpdateCase hearingResultedUpdateCaseHearing2 = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withHearingId(randomUUID())
                .withProsecutionCase(prosecutionCaseHearing2)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope2 = envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                randomUUID()), hearingResultedUpdateCaseHearing2);

        updateCaseHandler.handle(envelope2);

        verify(eventStream, times(2)).append(eventsStreamCaptor.capture());
        final List<JsonEnvelope> eventsList2 = eventsStreamCaptor.getValue().collect(Collectors.toList());
        final JsonObject hearingResultedCaseUpdatedPayload2 = eventsList2.get(0).payloadAsJsonObject();

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 0, defendant1.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 0, 0, offence1.getId(), true);

        assertDefendantProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 1, defendant2.getId(), false);
        assertDefendantOffenceProceedingsConcluded(hearingResultedCaseUpdatedPayload2, 1, 0, offence21.getId(), true);

        assertThat(hearingResultedCaseUpdatedPayload2.getJsonObject("prosecutionCase").getString("caseStatus"), is(CaseStatusEnum.ACTIVE.getDescription()));
    }

    private void assertDefendantProceedingsConcluded(final JsonObject hearingResultedCaseUpdatedPayload, final int defendantIdx, final UUID defendantId, final boolean proceedingsConcluded){
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(defendantIdx).getString("id"), is(defendantId.toString()));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(defendantIdx).getBoolean("proceedingsConcluded"), is(proceedingsConcluded));
    }

    private void assertDefendantOffenceProceedingsConcluded(final JsonObject hearingResultedCaseUpdatedPayload, final int defendantIdx, final int defendantOffenceIdx, final UUID offenceId, final boolean proceedingsConcluded){
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(defendantIdx).getJsonArray("offences").getJsonObject(defendantOffenceIdx).getString("id"), is(offenceId.toString()));
        assertThat(hearingResultedCaseUpdatedPayload.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(defendantIdx).getJsonArray("offences").getJsonObject(defendantOffenceIdx).getBoolean("proceedingsConcluded"), is(proceedingsConcluded));
    }

    @Test
    public void shouldHandleUpdateListingNumber() throws EventStreamException {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(asList(offence1, offence2))
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(defendantList).build();

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        final List<OffenceListingNumbers> offenceListingNumbers = new ArrayList<>();
        offenceListingNumbers.add(OffenceListingNumbers.offenceListingNumbers()
                .withOffenceId(offence1.getId())
                .withListingNumber(Integer.valueOf(100))
                .build());

        final UpdateListingNumberToProsecutionCase updateListingNumberToProsecutionCase = UpdateListingNumberToProsecutionCase.updateListingNumberToProsecutionCase()
                .withOffenceListingNumbers(offenceListingNumbers)
                .withProsecutionCaseId(prosecutionCase.getId())
                .build();
        final Envelope<UpdateListingNumberToProsecutionCase> envelope =
                envelopeFrom(metadataFor("progression.command.update-listing-number-to-prosecution-case",
                                randomUUID()),
                        updateListingNumberToProsecutionCase);

        updateCaseHandler.handleUpdateListingNumber(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.prosecution-case-listing-number-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCaseId", is(prosecutionCase.getId().toString()))
                                        )
                                ))
                )));
    }

    @Test
    public void shouldHandleUpdateListingNumber1() throws EventStreamException {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(asList(offence1, offence2))
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .withDefendants(defendantList).build();

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        final List<OffenceListingNumbers> offenceListingNumbers = new ArrayList<>();
        offenceListingNumbers.add(OffenceListingNumbers.offenceListingNumbers()
                .withOffenceId(offence1.getId())
                .withListingNumber(Integer.valueOf(100))
                .build());

        final IncreaseListingNumberToProsecutionCase increaseListingNumberToProsecutionCase = IncreaseListingNumberToProsecutionCase.increaseListingNumberToProsecutionCase()
                .withHearingId(randomUUID())
                .withOffenceIds(asList(offence1.getId()))
                .build();
        final Envelope<IncreaseListingNumberToProsecutionCase> envelope =
                envelopeFrom(metadataFor("progression.command.increase-listing-number-to-prosecution-case",
                                randomUUID()),
                        increaseListingNumberToProsecutionCase);

        updateCaseHandler.handleIncreaseListingNumber(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.prosecution-case-listing-number-increased"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCaseId", is(prosecutionCase.getId().toString()))
                                        )
                                ))
                )));
    }

    private HearingResultedUpdateCase createCommandPayloadWithHearingResultedUpdateCase() {
        final String caseURN = "case" + string(6).next();
        final UUID caseId = randomUUID();
        final Offence offence1 = getOffence(JudicialResultCategory.FINAL);
        final Offence offence2 = getOffence(JudicialResultCategory.FINAL);

        final List<JudicialResult> defendantCaseJRs = asList(JudicialResult.judicialResult()
                .withCategory(ANCILLARY)
                .build());
        final Defendant defendant = getDefendant(caseId, asList(offence1, offence2), defendantCaseJRs);
        final ProsecutionCase prosecutionCase = getProsecutionCase(caseURN, caseId, singletonList(defendant));

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        this.aggregate.apply(prosecutionCaseCreated);

        return HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .withCourtCentre(getCourtCentre())
                .build();
    }

    private static CourtCentre getCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();
    }

    private static ProsecutionCase getProsecutionCase(final String caseURN, final UUID caseId, final List<Defendant> defendants) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withDefendants(defendants).build();
    }

    private static Defendant getDefendant(final UUID caseId, final List<Offence> offences, final List<JudicialResult> defendantCaseJRs) {
        return Defendant.defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .withOffences(new ArrayList<>(offences))
                .withDefendantCaseJudicialResults(defendantCaseJRs)
                .build();
    }

    private static Offence getOffence(final JudicialResultCategory judicialResultCategory) {
        return Offence.offence()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(judicialResultCategory).build(),
                        JudicialResult.judicialResult().withCategory(ANCILLARY).withIsNewAmendment(Boolean.TRUE).build()))

                .build();
    }

    private static Offence getOffence(final UUID offenceId, final JudicialResultCategory judicialResultCategory) {
        return Offence.offence()
                .withId(offenceId)
                .withProceedingsConcluded(false)
                .withJudicialResults(asList(JudicialResult.judicialResult().withCategory(judicialResultCategory).build(),
                        JudicialResult.judicialResult().withCategory(ANCILLARY).withIsNewAmendment(Boolean.TRUE).build()))

                .build();
    }

}
