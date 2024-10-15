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

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.staginghmi.courts.UpdateHearingFromHmi;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HmiHandlerTest {
    private static final String UPDATE_HEARING_FROM_HMI_COMMAND = "progression.command.update-hearing-from-hmi";
    private static final String HEARING_MOVED_TO_UNALLOCATED_EVENT = "progression.event.hearing-moved-to-unallocated";
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingMovedToUnallocated.class);

    @InjectMocks
    private HmiHandler handler;


    private HearingAggregate aggregate;


    @Test
    public void shouldHandleCommand() {
        assertThat(new HmiHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateHearingHmi")
                        .thatHandles(UPDATE_HEARING_FROM_HMI_COMMAND)
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        UpdateHearingFromHmi updateHearingFromHmi = UpdateHearingFromHmi.updateHearingFromHmi()
                .withHearingId(randomUUID())
                .withStartDate(LocalDate.now().toString())
                .build();
        Hearing hearing = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre().build())
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withCourtRoomId(randomUUID())
                                .build(),
                        HearingDay.hearingDay()
                                .build()))
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
                .build();
        aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        aggregate.apply(updateHearingFromHmi.getHearingId());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(UPDATE_HEARING_FROM_HMI_COMMAND)
                .withId(randomUUID())
                .build();

        final Envelope<UpdateHearingFromHmi> envelope = envelopeFrom(metadata, updateHearingFromHmi);

        handler.handleUpdateHearingHmi(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingMovedToUnallocatedEvent = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals(HEARING_MOVED_TO_UNALLOCATED_EVENT)).findFirst().get();

        assertThat(hearingMovedToUnallocatedEvent.payloadAsJsonObject()
                , notNullValue());
        assertThat(hearingMovedToUnallocatedEvent.metadata().name(), is(HEARING_MOVED_TO_UNALLOCATED_EVENT));

    }
}
