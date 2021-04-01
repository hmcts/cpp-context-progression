package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
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
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHouseType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.courts.ProsecutionCasesResulted;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.handler.HearingResultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
            ProsecutionCaseDefendantListingStatusChanged.class,
            ProsecutionCasesResulted.class);

    @InjectMocks
    private HearingResultHandler hearingResultHandler;


    private HearingAggregate aggregate;

    private final static String COMMITTING_CROWN_COURT_CODE = "CRCODE";
    private final static String COMMITTING_CROWN_COURT_NAME = "Committing Crown Court";
    private final static String COMMITTING_MAGS_COURT_CODE = "MGCODE";
    private final static String COMMITTING_MAGS_COURT_NAME = "Committing Mag Court";


    @Before
    public void setup() {
        aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingResultHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.hearing-result")
                ));
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

        final JsonEnvelope  hearingResultedEnvelope = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        final JsonEnvelope  defendantListingStatusEnvelope = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.prosecutionCase-defendant-listing-status-changed")).findFirst().get();


        assertThat(hearingResultedEnvelope.payloadAsJsonObject().getJsonObject("hearing")
                , notNullValue());
        assertThat(hearingResultedEnvelope.metadata().name(), is("progression.event.hearing-resulted"));

        assertThat(defendantListingStatusEnvelope.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed"));
        assertThat(defendantListingStatusEnvelope.payloadAsJsonObject().getJsonObject("hearing")
                , notNullValue());

    }

    @Test
    public void shouldProcessCommand_whenAtLeastOneofOffencesOfDefendantHaveFinalCategory_expectProceedingConcludedAsTrue() throws Exception {

        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
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
    public void shouldProcessCommand_whenAllOffencesOfDefendantHaveNoFinalCategory_expectProceedingConcludedAsFalse() throws Exception {

        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
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
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())

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
        final JsonEnvelope  hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();
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
        final JsonEnvelope  hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


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

        final JsonEnvelope  hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

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
        final JsonEnvelope  hearingResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();


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
                .withCourtHouseType(CourtHouseType.MAGISTRATES)
                .build();

        final Hearing initHearing = Hearing.hearing()
                .withProsecutionCases(getProsecutionCasesWithCommittingCourt(initCommittingCourt))
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withCourtApplications(getCourtApplications())
                .build();

        aggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(initHearing)
                .build());

        final CommittingCourt resultCommittingCourt = CommittingCourt.committingCourt()
                .withCourtCentreId(UUID.randomUUID())
                .withCourtHouseShortName(COMMITTING_CROWN_COURT_CODE)
                .withCourtHouseName(COMMITTING_CROWN_COURT_NAME)
                .withCourtHouseCode(COMMITTING_CROWN_COURT_CODE)
                .withCourtHouseType(CourtHouseType.CROWN)
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
        final JsonEnvelope  prosecutionCasesResultedEnvelope = envelopeStream.filter(env -> env.metadata().name().equals("progression.event.prosecution-cases-resulted")).findFirst().get();


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
                            .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build()).build())
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



}
