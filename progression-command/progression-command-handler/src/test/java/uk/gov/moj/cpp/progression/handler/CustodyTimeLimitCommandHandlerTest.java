package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ExtendCustodyTimeLimit;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.CustodyTimeLimitClockStopped;
import uk.gov.justice.progression.courts.CustodyTimeLimitExtended;
import uk.gov.justice.progression.courts.StopCustodyTimeLimitClock;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CustodyTimeLimitCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CustodyTimeLimitClockStopped.class, CustodyTimeLimitExtended.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private CustodyTimeLimitCommandHandler custodyTimeLimitCommandHandler;

    @Test
    public void shouldHandleStopCustodyTimeLimitClock() throws EventStreamException {

        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                        .withId(caseId)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(offenceId)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .build())))
                                                .build()))
                                        .build(),
                                ProsecutionCase.prosecutionCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(asList(Offence.offence()
                                                        .withId(randomUUID())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.stop-custody-time-limit-clock")
                .withId(randomUUID())
                .build();
        final Envelope<StopCustodyTimeLimitClock> envelope = envelopeFrom(metadata, StopCustodyTimeLimitClock.stopCustodyTimeLimitClock()
                .withHearingId(hearingId)
                .withOffenceIds(asList(offenceId))
                .build());
        custodyTimeLimitCommandHandler.handleStopCustodyTimeLimitClock(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.custody-time-limit-clock-stopped"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.offenceIds", equalTo(asList(offenceId.toString()))),
                                withJsonPath("$.caseIds", equalTo(asList(caseId.toString())))
                        ))
                )
        ));

    }

    @Test
    public void shouldHandleExtendCustodyTimeLimitWhenNoOtherHearingsAreRelatedToCase() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate extendedTimeLimit = LocalDate.now();

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearingId));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.extend-custody-time-limit")
                .withId(randomUUID())
                .build();
        final Envelope<ExtendCustodyTimeLimit> envelope = envelopeFrom(metadata, ExtendCustodyTimeLimit.extendCustodyTimeLimit()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withExtendedTimeLimit(extendedTimeLimit)
                .withOffenceId(offenceId)
                .build());
        custodyTimeLimitCommandHandler.handleExtendCustodyTimeLimit(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldHandleExtendCustodyTimeLimitWhenAnotherHearingIsRelatedToCase() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID firstHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate extendedTimeLimit = LocalDate.now();

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(ProsecutionCaseCreated.prosecutionCaseCreated().withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withId(offenceId)
                                        .build())))
                                .build()))
                        .build())
                .build());
        caseAggregate.apply(CaseLinkedToHearing.caseLinkedToHearing().withHearingId(firstHearingId).build());
        caseAggregate.apply(CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearingId).build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.extend-custody-time-limit")
                .withId(randomUUID())
                .build();
        final Envelope<ExtendCustodyTimeLimit> envelope = envelopeFrom(metadata, ExtendCustodyTimeLimit.extendCustodyTimeLimit()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withExtendedTimeLimit(extendedTimeLimit)
                .withOffenceId(offenceId)
                .build());
        custodyTimeLimitCommandHandler.handleExtendCustodyTimeLimit(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.custody-time-limit-extended"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingIds", equalTo(asList(firstHearingId.toString()))),
                                withJsonPath("$.offenceId", equalTo(offenceId.toString())),
                                withJsonPath("$.extendedTimeLimit", equalTo(extendedTimeLimit.toString()))
                        ))
                )
        ));

    }
}
