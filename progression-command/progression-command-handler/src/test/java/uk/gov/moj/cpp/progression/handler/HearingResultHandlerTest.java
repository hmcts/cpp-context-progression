package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.hearing.courts.HearingResult.hearingResult;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCasesResulted;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingResulted.class,
            ProsecutionCaseDefendantListingStatusChangedV2.class,
            ProsecutionCasesResulted.class,
            ApplicationsResulted.class,
            LaaDefendantProceedingConcludedChanged.class);

    @InjectMocks
    private HearingResultHandler hearingResultHandler;


    private HearingAggregate aggregate;

    private final static String COMMITTING_CROWN_COURT_CODE = "CRCODE";
    private final static String COMMITTING_CROWN_COURT_NAME = "Committing Crown Court";
    private final static String COMMITTING_MAGS_COURT_CODE = "MGCODE";
    private final static String COMMITTING_MAGS_COURT_NAME = "Committing Mag Court";

    @Mock
    private GroupCaseAggregate groupCaseAggregate;

    @Mock
    private CaseAggregate caseAggregate;

    @Before
    public void setup() {
        aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);

        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(groupCaseAggregate);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingResultHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.hearing-result")
                ));
    }

    @Test
    public void shouldHandleProcessUpdateDefendantStatusWithoutGroupCases() throws EventStreamException {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1, offence2))
                .build();
        Defendant defendant2 = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1, offence2))
                .build();

        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        defendantList.add(defendant2);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(Boolean.FALSE)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        aggregate.apply(hearingResult.getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingResultedEnvelope = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    @Test
    public void shouldHandleProcessUpdateDefendantStatusWithGroupCases() throws EventStreamException {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1, offence2))
                .build();

        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(Boolean.TRUE)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final ProsecutionCase prosecutionMemberCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(Boolean.FALSE)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withIsGroupProceedings(Boolean.TRUE)
                        .withProsecutionCases(Arrays.asList(prosecutionCase, prosecutionMemberCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        aggregate.apply(hearingResult.getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);
        Set<UUID> prosecutionCasesUUIDs = new HashSet<>();
        prosecutionCasesUUIDs.add(prosecutionCase.getId());
        prosecutionCasesUUIDs.add(prosecutionMemberCase.getId());
        when(groupCaseAggregate.getMemberCases()).thenReturn(prosecutionCasesUUIDs);
        when(caseAggregate.getProsecutionCase()).thenReturn(prosecutionMemberCase);
        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingResultedEnvelope = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final HearingResult hearingResult = HearingResult.hearingResult()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        aggregate.apply(hearingResult.getHearing());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        final JsonEnvelope defendantListingStatusEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.prosecutionCase-defendant-listing-status-changed-v2")).findFirst().get();


        assertThat(hearingResultedEnvelope.payloadAsJsonObject().getJsonObject("hearing")
                , notNullValue());
        assertThat(hearingResultedEnvelope.metadata().name(), is("progression.event.hearing-resulted"));

        assertThat(defendantListingStatusEnvelope.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed-v2"));
        assertThat(defendantListingStatusEnvelope.payloadAsJsonObject().getJsonObject("hearing")
                , notNullValue());

    }

    @Test
    public void shouldProcessCommand_whenAtLeastOneofOffencesOfDefendantHaveFinalCategory_expectProceedingConcludedAsTrue() throws Exception {

        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(createDefendant(Arrays.asList(offence1, offence2))).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        aggregate.apply(hearingResult.getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));


    }

    @Test
    public void shouldProcessCommand_whenOneDefendantHaveFinalCategoryDefendantLevelAndOffencesOfSecondDefendantHaveFinalCategory_expectProceedingConcludedAsFalse() throws Exception {

        aggregate.apply(createPayload().getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, createPayload());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(false)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(false)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    private HearingResult createPayload(){
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        Offence offence3 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence3))
                .withDefendantCaseJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(UUID.randomUUID())
                        .build()))
                .build();
        Defendant defendant2 = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1, offence2))
                .build();

        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        defendantList.add(defendant2);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        return hearingResult;
    }

    @Test
    public void shouldProcessCommand_whenOnlyOneDefendantHaveFinalCategory_expectCaseStatusNotInActive() throws Exception {

        aggregate.apply(createHearingResultPayload().getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, createHearingResultPayload());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                        is(false)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                        is(false)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    private HearingResult createHearingResultPayload(){
        final Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build()))
                .build();

        final Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1))
                .withDefendantCaseJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(UUID.randomUUID())
                        .build()))
                .build();
        Defendant defendant2 = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence2))
                .build();

        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        defendantList.add(defendant2);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        return hearingResult;
    }

    @Test
    public void shouldProcessCommand_whenHearingHaveFinalCategory_expectCaseStatusInActive() throws Exception {

        aggregate.apply(createPayloadWIthHearingHavingFinalCategory().getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, createPayloadWIthHearingHavingFinalCategory());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    private HearingResult createPayloadWIthHearingHavingFinalCategory(){

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(createDefendant(createOffences(JudicialResultCategory.FINAL))).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .withDefendantJudicialResults(Arrays.asList(DefendantJudicialResult.defendantJudicialResult()
                                .withJudicialResult(JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.FINAL)
                                        .withOrderedDate(LocalDate.now())
                                        .build())
                                .build()))
                        .build())
                .build();

        return  hearingResult;
    }

    @Test
    public void shouldProcessCommand_whenAllOffencesOfDefendantHaveNoFinalCategory_expectProceedingConcludedAsFalse() throws Exception {

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withDefendants(createDefendant(createOffences(JudicialResultCategory.ANCILLARY))).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        aggregate.apply(hearingResult.getHearing());

        final Metadata metadata = getMetadata();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();
        assertThat(hearingResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-resulted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[0].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[1].offences[1].proceedingsConcluded",
                                        is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())))
                        ))


        );
    }

    @Test
    public void shouldUpdateHearingResultForCourtApplication() throws Exception {

        final Metadata metadata = getMetadata();
        final Hearing hearing = hearing()
                .withCourtApplications(getCourtApplications())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult().withHearing(hearing).build());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        assertThat(hearingResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-resulted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.courtApplications", notNullValue())
                                )
                        )
                ));

    }


    @Test
    public void shouldUpdateHearingResultForProsecutionCases() throws Exception {
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCases())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(), hearingResult().withHearing(hearing).build());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final JsonEnvelope hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-resulted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases", notNullValue())
                                )
                        )
                ));

    }


    @Test
    public void shouldUpdateHearingResultForProsecutionCasesAndCourtApplications() throws Exception {
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCases())
                .withCourtApplications(getCourtApplications())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(), hearingResult().withHearing(hearing).build());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        assertThat(hearingResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-resulted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases", notNullValue()),
                                withJsonPath("$.hearing.courtApplications", notNullValue()),
                                withJsonPath("$.hearing.courtApplications[0].courtOrder.orderingCourt", notNullValue()),
                                withJsonPath("$.hearing.courtApplications[0].courtApplicationCases", notNullValue())
                                )
                        )
                ));
    }

    @Test
    public void shouldHandleCommandForProsecutionCasesResulted() throws Exception {
        final CommittingCourt initCommittingCourt = CommittingCourt.committingCourt()
                .withCourtCentreId(UUID.randomUUID())
                .withCourtHouseShortName(COMMITTING_MAGS_COURT_CODE)
                .withCourtHouseName(COMMITTING_MAGS_COURT_NAME)
                .withCourtHouseCode(COMMITTING_MAGS_COURT_CODE)
                .withCourtHouseType(JurisdictionType.MAGISTRATES)
                .build();

        final Hearing initHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithCommittingCourt(initCommittingCourt))
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withCourtApplications(getCourtApplications())
                .build();

        aggregate.apply(ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(initHearing)
                .build());

        final CommittingCourt resultCommittingCourt = CommittingCourt.committingCourt()
                .withCourtCentreId(UUID.randomUUID())
                .withCourtHouseShortName(COMMITTING_CROWN_COURT_CODE)
                .withCourtHouseName(COMMITTING_CROWN_COURT_NAME)
                .withCourtHouseCode(COMMITTING_CROWN_COURT_CODE)
                .withCourtHouseType(JurisdictionType.CROWN)
                .build();
        final Hearing resultHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithCommittingCourt(resultCommittingCourt))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(getCourtApplications())
                .build();

        final UUID shadowOffenceId = randomUUID();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .withShadowListedOffences(Arrays.asList(shadowOffenceId))
                        .build());

        hearingResultHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.prosecution-cases-resulted")).findFirst().get();


        assertThat(prosecutionCasesResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-cases-resulted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases", notNullValue()),
                                withJsonPath("$.hearing.courtApplications", notNullValue()),
                                withJsonPath("$.shadowListedOffences", hasSize(1)),
                                withJsonPath("$.shadowListedOffences[0]", is(shadowOffenceId.toString())),
                                withJsonPath("$.committingCourt.courtHouseCode", is(COMMITTING_MAGS_COURT_CODE)),
                                withJsonPath("$.committingCourt.courtHouseName", is(COMMITTING_MAGS_COURT_NAME))
                                )
                        )
                ));
    }

    @Test
    public void shouldUpdateOffenceUnderCasesWhenHearingAdjourned() throws EventStreamException {

        final Hearing resultHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithAdjourn(LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyProsecutionCaseWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.prosecution-cases-resulted")).findFirst().get();
        verifyProsecutionCaseWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.prosecution-cases-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderCasesWhenHearingAdjournedWithBiggerDate() throws EventStreamException {

        final List<ProsecutionCase> prosecutionCases = getProsecutionCasesWithAdjourn(LocalDate.now());

        final Hearing firstHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithAdjournedOffences(prosecutionCases, LocalDate.now().minusDays(1)))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withProsecutionCases(prosecutionCases)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        hearingResultHandler.handle(envelopeFrom(getMetadata(), hearingResult().withHearing(resultHearing).build()));

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyProsecutionCaseWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.prosecution-cases-resulted")).findFirst().get();
        verifyProsecutionCaseWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.prosecution-cases-resulted");
    }

    @Test
    public void shouldNotUpdateOffenceUnderCasesWhenHearingAdjournedWithBeforeDate() throws EventStreamException {
        final List<ProsecutionCase> prosecutionCases = getProsecutionCasesWithAdjourn(LocalDate.now().minusDays(1));

        final Hearing firstHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithAdjournedOffences(prosecutionCases, LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withProsecutionCases(prosecutionCases)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        hearingResultHandler.handle(envelopeFrom(getMetadata(), hearingResult().withHearing(resultHearing).build()));

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyProsecutionCaseWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.prosecution-cases-resulted")).findFirst().get();
        verifyProsecutionCaseWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.prosecution-cases-resulted");
    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCasesWhenHearingAdjourned() throws EventStreamException {

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsCasesWithAdjourn(LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCasesWhenHearingAdjournedWithBiggerDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationsCasesWithAdjourn(LocalDate.now());

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now().minusDays(1)))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCasesWhenHearingAdjournedWithBeforeDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationsCasesWithAdjourn(LocalDate.now().minusDays(1));

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCourtOrderWhenHearingAdjourned() throws EventStreamException {

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsCourtOrderWithAdjourn(LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCourtOrderWhenHearingAdjournedWithBiggerDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationsCourtOrderWithAdjourn(LocalDate.now());

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now().minusDays(1)))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateOffenceUnderApplicationCourtOrderWhenHearingAdjournedWithBeforeDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationsCourtOrderWithAdjourn(LocalDate.now().minusDays(1));

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    //////////////////////////////////
    @Test
    public void shouldUpdateAllOffencesUnderApplicationWhenHearingAdjourned() throws EventStreamException {

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(getApplicationWithAdjourn(LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");
        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");

    }

    @Test
    public void shouldUpdateAllOffencesUnderApplicationWhenHearingAdjournedWithBiggerDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationWithAdjourn(LocalDate.now());

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now().minusDays(1)))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");
        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");
    }

    @Test
    public void shouldUpdateAllOffencesUnderApplicationWhenHearingAdjournedWithBeforeDate() throws EventStreamException {
        final List<CourtApplication> applications = getApplicationWithAdjourn(LocalDate.now().minusDays(1));

        final Hearing firstHearing = Hearing.hearing()
                .withCourtApplications(getApplicationsWithAdjournedOffences(applications, LocalDate.now()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();

        aggregate.apply(HearingResulted.hearingResulted().withHearing(firstHearing).build());

        final Hearing resultHearing = Hearing.hearing()
                .withCourtApplications(applications)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription("First Hearing").build())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(getMetadata(),
                hearingResult()
                        .withHearing(resultHearing)
                        .build());
        hearingResultHandler.handle(envelope);

        final List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        final JsonEnvelope hearingResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


        verifyApplicationCourtOrderWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");
        verifyApplicationCasesWithAdjournUpdate(hearingResultedEnvelope, "progression.event.hearing-resulted");

        final JsonEnvelope prosecutionCasesResultedEnvelope = envelopeStream.stream().filter(env -> env.metadata().name().equals("progression.event.applications-resulted")).findFirst().get();
        verifyApplicationCourtOrderWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");
        verifyApplicationCasesWithAdjournUpdate(prosecutionCasesResultedEnvelope, "progression.event.applications-resulted");
    }

    private void verifyProsecutionCaseWithAdjournUpdate(final JsonEnvelope prosecutionCasesResultedEnvelope, final String eventName) {
        assertThat(prosecutionCasesResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournDate", is(LocalDate.now().format(DateTimeFormatter.ISO_DATE))),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournedHearingType", is("AdjournmentReason"))
                                )
                        )
                ));
    }

    private void verifyApplicationCasesWithAdjournUpdate(final JsonEnvelope prosecutionCasesResultedEnvelope, final String eventName) {
        assertThat(prosecutionCasesResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.courtApplications[0].courtApplicationCases[0].offences[0].lastAdjournDate", is(LocalDate.now().format(DateTimeFormatter.ISO_DATE))),
                                withJsonPath("$.hearing.courtApplications[0].courtApplicationCases[0].offences[0].lastAdjournedHearingType", is("AdjournmentReason"))
                                )
                        )
                ));
    }

    private void verifyApplicationCourtOrderWithAdjournUpdate(final JsonEnvelope prosecutionCasesResultedEnvelope, final String eventName) {
        assertThat(prosecutionCasesResultedEnvelope,
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.lastAdjournDate", is(LocalDate.now().format(DateTimeFormatter.ISO_DATE))),
                                withJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.lastAdjournedHearingType", is("AdjournmentReason"))
                                )
                        )
                ));
    }

    private Metadata getMetadata() {
        return Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(UUID.randomUUID())
                .build();
    }

    private List<CourtApplication> getCourtApplications() {
        return singletonList(CourtApplication.courtApplication()
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .withId(randomUUID())
                .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                        .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence().build()).build())).build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                .build());
    }

    private List<ProsecutionCase> getProsecutionCases() {
        final List<Defendant> defendants = singletonList(Defendant.defendant()
                .withId(randomUUID())
                .withIsYouth(true)
                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withOffenceTitle("offence title").build()))
                .build());

        return singletonList(prosecutionCase().withId(randomUUID()).withDefendants(defendants).withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription()).build());
    }


    private List<ProsecutionCase> getProsecutionCasesWithCommittingCourt(CommittingCourt committingCourt) {
        final List<Defendant> defendants = singletonList(Defendant.defendant()
                .withId(randomUUID())
                .withIsYouth(true)
                .withOffences(singletonList(Offence.offence().withId(randomUUID())
                        .withCommittingCourt(committingCourt)
                        .withOffenceTitle("offence title").build()))
                .build());

        return singletonList(prosecutionCase().withId(randomUUID()).withDefendants(defendants).withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription()).build());
    }

    private List<ProsecutionCase> getProsecutionCasesWithAdjourn(final LocalDate orderedDate) {
        final List<Defendant> defendants = singletonList(Defendant.defendant()
                .withId(randomUUID())
                .withOffences(getOffenceWithAdjourn(orderedDate))
                .withProceedingsConcluded(true)
                .build());

        return singletonList(prosecutionCase().withId(randomUUID()).withDefendants(defendants).withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription()).build());
    }

    private List<ProsecutionCase> getProsecutionCasesWithAdjournedOffences(final List<ProsecutionCase> prosecutionCases, final LocalDate plusDays) {
        return prosecutionCases.stream().map(prosecutionCase -> ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> Defendant.defendant()
                                .withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream().map(offence -> Offence.offence()
                                        .withValuesFrom(offence)
                                        .withLastAdjournedHearingType("AdjournmentReason")
                                        .withLastAdjournDate(plusDays)
                                        .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build())
                .collect(Collectors.toList());
    }

    private List<CourtApplication> getApplicationsCasesWithAdjourn(final LocalDate orderedDate) {
        return singletonList(CourtApplication.courtApplication()
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(getOffenceWithAdjourn(orderedDate))
                        .build()))
                .build());
    }

    private List<CourtApplication> getApplicationsCourtOrderWithAdjourn(final LocalDate orderedDate) {
        return singletonList(CourtApplication.courtApplication()
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(getOffenceWithAdjourn(orderedDate).get(0))
                                .build()))
                        .build())
                .build());
    }

    private List<CourtApplication> getApplicationWithAdjourn(final LocalDate orderedDate) {
        return singletonList(CourtApplication.courtApplication()
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence().withId(randomUUID()).build())
                                .build()))
                        .build())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(singletonList(Offence.offence().withId(randomUUID()).build())).build()))
                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withOrderedDate(orderedDate)
                        .withNextHearing(NextHearing.nextHearing().withAdjournmentReason("AdjournmentReason").build())
                        .withCategory(JudicialResultCategory.INTERMEDIARY)
                        .build()))
                .build());
    }

    private List<CourtApplication> getApplicationsWithAdjournedOffences(final List<CourtApplication> applications, final LocalDate plusDays) {
        return applications.stream()
                .map(courtApplication -> CourtApplication.courtApplication()
                        .withValuesFrom(courtApplication)
                        .withCourtApplicationCases(ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                                        .withValuesFrom(courtApplicationCase)
                                        .withOffences(courtApplicationCase.getOffences().stream()
                                                .map(offence -> Offence.offence()
                                                        .withValuesFrom(offence)
                                                        .withLastAdjournedHearingType("AdjournmentReason")
                                                        .withLastAdjournDate(plusDays)
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .withCourtOrder(ofNullable(courtApplication.getCourtOrder())
                                .map(courtOrder -> CourtOrder.courtOrder()
                                        .withValuesFrom(courtOrder)
                                        .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence()
                                                        .withValuesFrom(courtOrderOffence)
                                                        .withOffence(of(courtOrderOffence.getOffence())
                                                                .map(offence -> Offence.offence()
                                                                        .withValuesFrom(offence)
                                                                        .withLastAdjournedHearingType("AdjournmentReason")
                                                                        .withLastAdjournDate(plusDays)
                                                                        .build())
                                                                .get())
                                                        .build())
                                                .collect(toList()))
                                        .build())
                                .orElse(null))
                        .build())
                .collect(Collectors.toList());
    }

    private List<Offence> getOffenceWithAdjourn(final LocalDate orderedDate) {
        return singletonList(Offence.offence().withId(randomUUID())
                .withOffenceTitle("offence title")
                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withOrderedDate(orderedDate)
                        .withNextHearing(NextHearing.nextHearing().withAdjournmentReason("AdjournmentReason").build())
                        .withCategory(JudicialResultCategory.INTERMEDIARY)
                        .build()))
                .build());
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    private  List<Offence> createOffences(final JudicialResultCategory judicialResultCategory){
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(judicialResultCategory).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(judicialResultCategory).build()))
                .build();

        return Arrays.asList(offence1, offence2);
    }

    private List<Defendant> createDefendant(final List<Offence> listOfOffences){
        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(listOfOffences)
                .build();
        Defendant defendant2 = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(listOfOffences)
                .build();

        return Arrays.asList(defendant, defendant2);

    }
}
