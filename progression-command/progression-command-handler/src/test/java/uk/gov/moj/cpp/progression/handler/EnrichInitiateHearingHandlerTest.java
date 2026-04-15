package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CommandEnrichHearingInitiate;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.EnrichHearingInitiated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EnrichInitiateHearingHandlerTest {

    public static final String DEFENCE_REQUEST = "Defence request";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDefendantRequestCreated.class,
            EnrichHearingInitiated.class,
            HearingInitiateEnriched.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream caseEventStream;

    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private EnrichInitiateHearingHandler testObj;

    @Test
    public void erichHearingInitiate() throws Exception {

    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new EnrichInitiateHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("enrichHearingInitiate")
                        .thatHandles("progression.command-enrich-hearing-initiate")
                ));
    }

    @Test
    void shouldProcessCommand() throws Exception {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        //Given
        final CommandEnrichHearingInitiate arbitraryInitiateObj = generateInitiateTestObj(caseId, hearingId);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command-enrich-hearing-initiate")
                .withId(randomUUID())
                .build();

        final Envelope<CommandEnrichHearingInitiate> envelope = envelopeFrom(metadata, arbitraryInitiateObj);

        //When
        final HearingAggregate hearingAggregate = new HearingAggregate();
        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(hearingId)).thenReturn(eventStream);
        when(eventSource.getStreamById(caseId)).thenReturn(caseEventStream);

        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(aggregateService.get(caseEventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        testObj.enrichHearingInitiate(envelope);

        //Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.hearing-initiate-enriched"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearing", notNullValue()))
                                ))
                )
        );

        final Stream<JsonEnvelope> caseEnvelopeStream = verifyAppendAndGetArgumentFrom(caseEventStream);

        assertThat(caseEnvelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.enrich-hearing-initiated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.courtCentreId", notNullValue()),
                                        withJsonPath("$.courtCentreId", notNullValue()),
                                        withJsonPath("$.courtRoomId", notNullValue()),
                                        withJsonPath("$.hearingId",  is(hearingId.toString())),
                                        withJsonPath("$.hearingListingStatus", notNullValue()))
                                ))
                )
        );
    }

    @Test
    void shouldProcessCommandAndReferralReason() throws Exception {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        //Given
        final CommandEnrichHearingInitiate arbitraryInitiate = generateInitiateTestObj(caseId, hearingId);
        ListDefendantRequest arbitraryListDefendantRequest = generateInitiateListDefendantRequest(arbitraryInitiate);

        final HearingAggregate hearingAggregate = new HearingAggregate();
        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventSource.getStreamById(caseId)).thenReturn(caseEventStream);

        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(aggregateService.get(caseEventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        hearingAggregate.createHearingDefendantRequest(Arrays.asList(arbitraryListDefendantRequest));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command-enrich-hearing-initiate")
                .withId(randomUUID())
                .build();

        final Envelope<CommandEnrichHearingInitiate> envelope = envelopeFrom(metadata, arbitraryInitiate);

        //When
        testObj.enrichHearingInitiate(envelope);

        //Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.hearing-initiate-enriched"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.hearing", notNullValue()),
                                                withJsonPath("$.hearing.defendantReferralReasons[0].description", is(DEFENCE_REQUEST))
                                        )
                                ))

                )
        );

        final Stream<JsonEnvelope> caseEnvelopeStream = verifyAppendAndGetArgumentFrom(caseEventStream);

        assertThat(caseEnvelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.enrich-hearing-initiated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.courtCentreId", notNullValue()),
                                        withJsonPath("$.courtCentreId", notNullValue()),
                                        withJsonPath("$.courtRoomId", notNullValue()),
                                        withJsonPath("$.hearingId",  is(hearingId.toString())),
                                        withJsonPath("$.hearingListingStatus", notNullValue()))
                                ))
                )
        );
    }

    private ListDefendantRequest generateInitiateListDefendantRequest(final CommandEnrichHearingInitiate arbitraryInitiate) {
        return ListDefendantRequest.listDefendantRequest()
                .withReferralReason(ReferralReason.referralReason()
                        .withDefendantId(arbitraryInitiate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId())
                        .withDescription(DEFENCE_REQUEST)
                        .build())
                .build();
    }

    private CommandEnrichHearingInitiate generateInitiateTestObj() {
        return generateInitiateTestObj(randomUUID(), randomUUID());
    }
    private CommandEnrichHearingInitiate generateInitiateTestObj(final UUID caseId, final UUID hearingId) {
        return CommandEnrichHearingInitiate.commandEnrichHearingInitiate().withHearing(
                Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).withRoomId(randomUUID()).build())
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .withIsBoxHearing(true)
                        .withCourtApplications(Collections.singletonList(CourtApplication.courtApplication().build()))
                        .build()
        ).build();

    }
}
