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
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefenceCounselAdded;
import uk.gov.justice.core.courts.HearingDefenceCounselRemoved;
import uk.gov.justice.core.courts.HearingDefenceCounselUpdated;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
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
import uk.gov.moj.cpp.progression.command.AddHearingDefenceCounsel;
import uk.gov.moj.cpp.progression.command.RemoveHearingDefenceCounsel;
import uk.gov.moj.cpp.progression.command.UpdateHearingDefenceCounsel;

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
public class DefenceCounselCommandHandlerTest {
    private static final String ADD_HEARING_DEFENCE_COUNSEL_COMMAND = "progression.command.handler.add-hearing-defence-counsel";
    private static final String HEARING_DEFENCE_COUNSEL_ADDED_EVENT = "progression.event.hearing-defence-counsel-added";
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDefenceCounselAdded.class,
            ProsecutionCaseDefendantListingStatusChanged.class,
            HearingDefenceCounselUpdated.class,
            HearingDefenceCounselRemoved.class);

    @InjectMocks
    private DefenceCounselCommandHandler handler;


    private HearingAggregate aggregate;

    @BeforeEach
    public void setup() {
        aggregate = new HearingAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new DefenceCounselCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddHearingDefenceCounsel")
                        .thatHandles(ADD_HEARING_DEFENCE_COUNSEL_COMMAND)
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        AddHearingDefenceCounsel addHearingDefenceCounsel = AddHearingDefenceCounsel.addHearingDefenceCounsel()
                .withHearingId(randomUUID())
                .withDefenceCounsel(DefenceCounsel.defenceCounsel()
                        .build())
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
        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        aggregate.apply(addHearingDefenceCounsel.getHearingId());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_HEARING_DEFENCE_COUNSEL_COMMAND)
                .withId(randomUUID())
                .build();

        final Envelope<AddHearingDefenceCounsel> envelope = envelopeFrom(metadata, addHearingDefenceCounsel);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        handler.handleAddHearingDefenceCounsel(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  prosecutionCaseDefendantListingStatusChanged = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals(HEARING_DEFENCE_COUNSEL_ADDED_EVENT)).findFirst().get();

        assertThat(prosecutionCaseDefendantListingStatusChanged.payloadAsJsonObject()
                , notNullValue());
        assertThat(prosecutionCaseDefendantListingStatusChanged.metadata().name(), is(HEARING_DEFENCE_COUNSEL_ADDED_EVENT));

    }

    @Test
    public void shouldProcessUpdateCommand() throws Exception {
        UpdateHearingDefenceCounsel updateHearingDefenceCounsel = UpdateHearingDefenceCounsel.updateHearingDefenceCounsel()
                .withHearingId(randomUUID())
                .withDefenceCounsel(DefenceCounsel.defenceCounsel()
                        .build())
                .build();

        createHearing();

        aggregate.apply(updateHearingDefenceCounsel.getHearingId());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.update-hearing-defence-counsel")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateHearingDefenceCounsel> envelope = envelopeFrom(metadata, updateHearingDefenceCounsel);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        handler.handleUpdateHearingDefenceCounsel(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  prosecutionCaseDefendantListingStatusChanged = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-defence-counsel-updated")).findFirst().get();

        assertThat(prosecutionCaseDefendantListingStatusChanged.payloadAsJsonObject()
                , notNullValue());
        assertThat(prosecutionCaseDefendantListingStatusChanged.metadata().name(), is("progression.event.hearing-defence-counsel-updated"));

    }

    private void createHearing(){
        final List<DefenceCounsel> defenceCounsels = new ArrayList<>();
        defenceCounsels.add(DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .build());

        Hearing hearing = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre().build())
                .withDefenceCounsels(defenceCounsels)
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
        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());
    }

    @Test
    public void shouldHandleRemoveHearingDefenceCounsel() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final RemoveHearingDefenceCounsel removeHearingDefenceCounsel = RemoveHearingDefenceCounsel.removeHearingDefenceCounsel()
                .withHearingId(hearingId)
                .build();

        createHearing();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.update-hearing-defence-counsel")
                .withId(randomUUID())
                .build();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        final Envelope<RemoveHearingDefenceCounsel> envelope = envelopeFrom(metadata, removeHearingDefenceCounsel);

        handler.handleRemoveHearingDefenceCounsel(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  prosecutionCaseDefendantListingStatusChanged = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.prosecutionCase-defendant-listing-status-changed")).findFirst().get();

        assertThat(prosecutionCaseDefendantListingStatusChanged.payloadAsJsonObject()
                , notNullValue());
        assertThat(prosecutionCaseDefendantListingStatusChanged.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed"));
    }
}
