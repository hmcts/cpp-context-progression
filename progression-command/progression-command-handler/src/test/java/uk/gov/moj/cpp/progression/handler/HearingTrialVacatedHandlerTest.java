package uk.gov.moj.cpp.progression.handler;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.ProgressionHearingTrialVacated;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.handler.HearingTrialVacatedHandler;

import java.util.ArrayList;
import java.util.List;
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
public class HearingTrialVacatedHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingTrialVacated.class);

    @InjectMocks
    private HearingTrialVacatedHandler handler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingTrialVacatedHandler(), isHandler(COMMAND_HANDLER)
                .with(method("hearingTrialVacated")
                        .thatHandles("progression.command.hearing-trial-vacated")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final ProgressionHearingTrialVacated hearingTrialVacated = ProgressionHearingTrialVacated.progressionHearingTrialVacated()
                .withHearingId(UUID.randomUUID())
                .withVacatedTrialReasonId(UUID.randomUUID())
                .build();
        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
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

        hearingAggregate.apply(hearingTrialVacated.getHearingId());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-trial-vacated")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ProgressionHearingTrialVacated> envelope = envelopeFrom(metadata, hearingTrialVacated);

        handler.hearingTrialVacated(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingTrialVacatedEvent = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-trial-vacated")).findFirst().get();

        assertThat(hearingTrialVacatedEvent.payloadAsJsonObject()
                , notNullValue());
        assertThat(hearingTrialVacatedEvent.metadata().name(), is("progression.event.hearing-trial-vacated"));

    }
}
