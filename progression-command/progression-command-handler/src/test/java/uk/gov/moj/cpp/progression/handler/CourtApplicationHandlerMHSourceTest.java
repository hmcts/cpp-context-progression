package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.CAAG;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.HOME;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.MH;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.UNKNOWN;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationReferredToBoxwork;
import uk.gov.justice.core.courts.ApplicationReferredToCourtHearing;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiateIgnored;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.HearingUpdatedWithCourtApplication;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.SendNotificationForApplicationIgnored;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.service.ApplicationDetailsEnrichmentService;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.service.ProgressionService;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the MH (Manage Hearing) applicationSource fix — AC1 and AC2.
 * <p>
 * AC1: MH source + ACTIVE case → offences must be nulled (no CCT-2487 offence selection).
 * AC2: MH source + INACTIVE/CLOSED case → offences must be preserved (as-is).
 */
@ExtendWith(MockitoExtension.class)
class CourtApplicationHandlerMHSourceTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtApplicationCreated.class,
            CourtApplicationAddedToCase.class,
            CourtApplicationProceedingsInitiated.class,
            ApplicationReferredToBoxwork.class,
            ApplicationReferredToCourtHearing.class,
            ApplicationReferredToExistingHearing.class,
            CourtApplicationProceedingsInitiateIgnored.class,
            CourtApplicationProceedingsEdited.class,
            CourtApplicationSummonsRejected.class,
            CourtApplicationSummonsApproved.class,
            InitiateCourtHearingAfterSummonsApproved.class,
            HearingResultedApplicationUpdated.class,
            HearingUpdatedWithCourtApplication.class,
            HearingPopulatedToProbationCaseworker.class,
            VejHearingPopulatedToProbationCaseworker.class,
            SendNotificationForApplicationInitiated.class,
            SendNotificationForApplicationIgnored.class,
            SendNotificationForAutoApplicationInitiated.class,
            CourtApplicationUpdated.class,
            ApplicationHearingDefendantUpdated.class
    );

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Mock
    private ApplicationDetailsEnrichmentService applicationDetailsEnrichmentService;

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter =
            new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter =
            new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Mock
    private ApplicationAggregate applicationAggregateMock;

    @InjectMocks
    private CourtApplicationHandler courtApplicationHandler;

    private UUID masterDefendantId;

    @BeforeEach
    void setUp() {
        masterDefendantId = randomUUID();
    }

    // -----------------------------------------------------------------------
    // AC1: Case hearing — MH source + ACTIVE case → offences must be nulled
    // -----------------------------------------------------------------------

    @Test
    void shouldNullOffencesForActiveCaseWhenApplicationSourceIsMH() throws Exception {
        final Offence activeOffence = offence()
                .withId(randomUUID())
                .withOffenceCode("OF001")
                .withWording("Some wording")
                .build();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .withOffences(singletonList(activeOffence))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].caseStatus", is("ACTIVE")),
                                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences")
                        ))
                )));
    }

    @Test
    void shouldNullOffencesForMultipleCasesWhenAllAreActiveAndSourceIsMH() throws Exception {
        final Offence offence = offence().withId(randomUUID()).withOffenceCode("OF002").build();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(asList(
                                courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .withOffences(singletonList(offence))
                                        .build(),
                                courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .withOffences(singletonList(offence))
                                        .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences"),
                                hasNoJsonPath("$.courtApplication.courtApplicationCases[1].offences")
                        ))
                )));
    }

    // -----------------------------------------------------------------------
    // AC2: Application hearing — MH source + INACTIVE case → offences kept
    // -----------------------------------------------------------------------

    @Test
    void shouldPreserveOffencesForInactiveCaseWhenApplicationSourceIsMH() throws Exception {
        final UUID offenceId = randomUUID();
        final Offence inactiveOffence = offence()
                .withId(offenceId)
                .withOffenceCode("OF003")
                .withWording("Breach wording")
                .build();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("INACTIVE")
                                .withOffences(singletonList(inactiveOffence))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].caseStatus", is("INACTIVE")),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString()))
                        ))
                )));
    }

    @Test
    void shouldPreserveOffencesForClosedCaseWhenApplicationSourceIsMH() throws Exception {
        final UUID offenceId = randomUUID();
        final Offence closedOffence = offence().withId(offenceId).withOffenceCode("OF004").build();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("CLOSED")
                                .withOffences(singletonList(closedOffence))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString()))
                        ))
                )));
    }

    // -----------------------------------------------------------------------
    // Mixed cases: one ACTIVE (offences nulled), one INACTIVE (offences kept)
    // -----------------------------------------------------------------------

    @Test
    void shouldNullOffencesOnlyForActiveCasesInMixedScenarioWithMHSource() throws Exception {
        final UUID activeOffenceId = randomUUID();
        final UUID inactiveOffenceId = randomUUID();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(asList(
                                courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .withOffences(singletonList(offence().withId(activeOffenceId).withOffenceCode("OF_A").build()))
                                        .build(),
                                courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("INACTIVE")
                                        .withOffences(singletonList(offence().withId(inactiveOffenceId).withOffenceCode("OF_I").build()))
                                        .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences"),
                                withJsonPath("$.courtApplication.courtApplicationCases[1].offences[0].id", is(inactiveOffenceId.toString()))
                        ))
                )));
    }

    // -----------------------------------------------------------------------
    // Non-MH sources must NOT trigger the suppression (as-is behaviour)
    // -----------------------------------------------------------------------

    @Test
    void shouldPreserveOffencesForActiveCaseWhenApplicationSourceIsHome() throws Exception {
        final UUID offenceId = randomUUID();
        shouldPreserveOffencesForActiveCaseWithSource(HOME, offenceId);
    }

    @Test
    void shouldPreserveOffencesForActiveCaseWhenApplicationSourceIsCaag() throws Exception {
        final UUID offenceId = randomUUID();
        shouldPreserveOffencesForActiveCaseWithSource(CAAG, offenceId);
    }

    @Test
    void shouldPreserveOffencesForActiveCaseWhenApplicationSourceIsUnknown() throws Exception {
        final UUID offenceId = randomUUID();
        shouldPreserveOffencesForActiveCaseWithSource(UNKNOWN, offenceId);
    }

    @Test
    void shouldPreserveOffencesForActiveCaseWhenApplicationSourceIsNull() throws Exception {
        final UUID offenceId = randomUUID();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                // no .withApplicationSource() — field absent, getApplicationSource() returns null
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .withOffences(singletonList(offence().withId(offenceId).withOffenceCode("OF_NULL").build()))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString()))
                        )
                )));
    }

    // -----------------------------------------------------------------------
    // Edge case: null caseStatus with MH source — treat as non-active (keep offences)
    // -----------------------------------------------------------------------

    @Test
    void shouldPreserveOffencesWhenCaseStatusIsNullAndSourceIsMH() throws Exception {
        final UUID offenceId = randomUUID();

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                // no .withCaseStatus() — null
                                .withOffences(singletonList(offence().withId(offenceId).withOffenceCode("OF_NULL_STATUS").build()))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString()))
                        )
                )));
    }

    // -----------------------------------------------------------------------
    // MH source — case with no offences at all should not fail
    // -----------------------------------------------------------------------

    @Test
    void shouldHandleActiveCaseWithNoOffencesWhenSourceIsMH() throws Exception {
        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(MH)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                // deliberately no offences
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(
                                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences")
                        )
                )));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void shouldPreserveOffencesForActiveCaseWithSource(
            final uk.gov.moj.cpp.progression.enums.ApplicationSource source,
            final UUID offenceId) throws Exception {

        final InitiateCourtApplicationProceedings command = initiateCourtApplicationProceedings()
                .withApplicationSource(source)
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildParty())
                        .withSubject(buildParty())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .withOffences(singletonList(offence().withId(offenceId).withOffenceCode("OF_NON_MH").build()))
                                .build()))
                        .build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = buildEnvelope(command);

        final ApplicationAggregate aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        aggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString()))
                        )
                )));
    }

    private Envelope<InitiateCourtApplicationProceedings> buildEnvelope(final InitiateCourtApplicationProceedings command) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        return envelopeFrom(metadata, command);
    }

    private CourtApplicationParty buildParty() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("TEST_CODE")
                        .build())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("1 Test Street")
                                                .withPostcode("TK1 1AA")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
