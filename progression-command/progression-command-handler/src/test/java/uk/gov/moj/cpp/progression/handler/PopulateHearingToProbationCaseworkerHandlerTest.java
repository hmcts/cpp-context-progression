package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PopulateHearingToProbationCaseworker;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PopulateHearingToProbationCaseworkerHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingPopulatedToProbationCaseworker.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private PopulateHearingToProbationCaseworkerHandler populateHearingToProbationCaseworkerHandler;

    private HearingAggregate hearingAggregate;

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandlePopulateHearingToProbationCaseworker() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.populate-hearing-to-probation-caseworker")
                .withId(randomUUID())
                .build();
        final Envelope<PopulateHearingToProbationCaseworker> envelope = envelopeFrom(metadata, PopulateHearingToProbationCaseworker.populateHearingToProbationCaseworker()
                .withHearingId(hearingId)
                .build());

        populateHearingToProbationCaseworkerHandler.handlePopulateHearingToProbationCaseworker(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.hearing-populated-to-probation-caseworker"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString()))
                        ))
                )
        ));
    }

    @Test
    public void shouldHandlePopulateApplicationHearingToProbationCaseworker() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .withCourtApplicationCases(new ArrayList<>(asList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build())))
                        .build())))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.populate-hearing-to-probation-caseworker")
                .withId(randomUUID())
                .build();
        final Envelope<PopulateHearingToProbationCaseworker> envelope = envelopeFrom(metadata, PopulateHearingToProbationCaseworker.populateHearingToProbationCaseworker()
                .withHearingId(hearingId)
                .build());

        populateHearingToProbationCaseworkerHandler.handlePopulateHearingToProbationCaseworker(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.hearing-populated-to-probation-caseworker"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString()))
                        ))
                )
        ));
    }

    @Test
    public void shouldNotInvokeProbationCaseworker() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withIsYouth(true)
                                        .build())
                                .build())
                        .withCourtApplicationCases(new ArrayList<>(asList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build())))
                        .build())))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.populate-hearing-to-probation-caseworker")
                .withId(randomUUID())
                .build();
        final Envelope<PopulateHearingToProbationCaseworker> envelope = envelopeFrom(metadata, PopulateHearingToProbationCaseworker.populateHearingToProbationCaseworker()
                .withHearingId(hearingId)
                .build());

        populateHearingToProbationCaseworkerHandler.handlePopulateHearingToProbationCaseworker(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream.collect(Collectors.toList()).size(),  is(0));
    }

    @Test
    public void shouldInvokeProbationCaseworkerWithAtLeastOneNonYouthApplication() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withIsYouth(true)
                                        .build())
                                .build())
                        .build())))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withIsYouth(false)
                                        .build())
                                .build())
                        .build())))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.populate-hearing-to-probation-caseworker")
                .withId(randomUUID())
                .build();
        final Envelope<PopulateHearingToProbationCaseworker> envelope = envelopeFrom(metadata, PopulateHearingToProbationCaseworker.populateHearingToProbationCaseworker()
                .withHearingId(hearingId)
                .build());

        populateHearingToProbationCaseworkerHandler.handlePopulateHearingToProbationCaseworker(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream.collect(Collectors.toList()).size(),  is(1));
    }
}
