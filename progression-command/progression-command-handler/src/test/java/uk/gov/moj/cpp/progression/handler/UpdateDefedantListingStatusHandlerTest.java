package uk.gov.moj.cpp.progression.handler;


import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.UpdateDefendantListingStatusV2;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefedantListingStatusHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private UpdateDefendantListingStatusHandler handler;

    private HearingAggregate aggregate;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionCaseDefendantListingStatusChangedV2.class,
            HearingPopulatedToProbationCaseworker.class, VejHearingPopulatedToProbationCaseworker.class);

    @BeforeEach
    public void setup() {
        aggregate = new HearingAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefendantListingStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-defendant-listing-status")
                ));
    }

    @Test
    public void shouldUpdateDefendantListingStatusV2() throws EventStreamException {
        final List<DefenceCounsel> defenceCounsels = new ArrayList<>();
        defenceCounsels.add(DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .build());

        Hearing hearing = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre().build())
                .withDefenceCounsels(defenceCounsels)
                .withId(randomUUID())
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withCourtRoomId(randomUUID())
                                .build(),
                        HearingDay.hearingDay()
                                .build()))
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(randomUUID())
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
                                        .withId(randomUUID())
                                        .withOffences(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        final UpdateDefendantListingStatusV2 updateDefendantListingStatusV2 = UpdateDefendantListingStatusV2.updateDefendantListingStatusV2()
                .withHearing(hearing)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.update-hearing-defence-counsel")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantListingStatusV2> envelope = envelopeFrom(metadata, updateDefendantListingStatusV2);
        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.prosecutionCase-defendant-listing-status-changed-v2")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
