package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.HearingListingStatus.SENT_FOR_LISTING;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.UnallocateHearingRemoveCourtroom;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingUnallocatedCourtroomRemoved;
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
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.List;
import java.util.Optional;
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
public class HearingUnallocatedCourtRoomRemovedHandlerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final Integer ESTIMATED_MINUTES = 30;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingUnallocatedCourtroomRemoved.class,
            uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2.class);

    @InjectMocks
    private HearingUnallocatedCourtRoomRemovedHandler handler;

    private HearingAggregate hearingAggregate;

    @BeforeEach
    void setUp() {
        hearingAggregate = new HearingAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingUnallocatedCourtRoomRemovedHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleHearingUnallocatedCourtRoomRemoved")
                        .thatHandles("progression.command.unallocate-hearing-remove-courtroom")
                ));
    }

    @Test
    public void shouldProcessUnallocateHearingRemoveCourtroomCommand() throws EventStreamException {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .build();

        hearingAggregate.apply(hearing);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(ESTIMATED_MINUTES)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());

        // Verify both events are present
        assertThat(events.size(), is(2));
        
        // Find the HearingUnallocatedCourtroomRemoved event
        final Optional<JsonEnvelope> unallocatedEvent = events.stream()
                .filter(e -> e.metadata().name().equals("progression.event.hearing-unallocated-courtroom-removed"))
                .findFirst();
        
        assertThat(unallocatedEvent.isPresent(), is(true));
        assertThat(unallocatedEvent.get(), jsonEnvelope(
                metadata()
                        .withName("progression.event.hearing-unallocated-courtroom-removed"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                        withJsonPath("$.estimatedMinutes", is(ESTIMATED_MINUTES))
                        )
                ))
        );
    }

    @Test
    public void shouldGenerateProsecutionCaseDefendantListingStatusChangedV2Event() throws EventStreamException {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .build();

        hearingAggregate.apply(hearing);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(ESTIMATED_MINUTES)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());

        // Verify both events are present
        assertThat(events.size(), is(2));
        
        // Find the ProsecutionCaseDefendantListingStatusChangedV2 event
        final Optional<JsonEnvelope> listingStatusEvent = events.stream()
                .filter(e -> e.metadata().name().equals("progression.event.prosecutionCase-defendant-listing-status-changed-v2"))
                .findFirst();
        
        assertThat(listingStatusEvent.isPresent(), is(true));
        assertThat(listingStatusEvent.get(), jsonEnvelope(
                metadata()
                        .withName("progression.event.prosecutionCase-defendant-listing-status-changed-v2"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        withJsonPath("$.hearing", notNullValue()),
                        withJsonPath("$.hearingListingStatus", is(SENT_FOR_LISTING.name())),
                        withJsonPath("$.notifyNCES", is(false))
                        )
                ))
        );
    }

    @Test
    public void shouldNotAppendEventsWhenHearingIsDeleted() throws EventStreamException {
        // Given
        final HearingAggregate aggregate = new HearingAggregate();
        // Set aggregate to deleted state which will return empty stream
        aggregate.apply(HearingDeleted.hearingDeleted()
                .withHearingId(HEARING_ID)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);

        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(ESTIMATED_MINUTES)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then - verify that no events were appended (stream is empty)
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());
        assertThat(events.size(), is(0));
    }

    @Test
    public void shouldNotAppendEventsWhenHearingIsResulted() throws EventStreamException {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .build();

        hearingAggregate.apply(hearing);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());
        // Set hearing to resulted status
        hearingAggregate.apply(HearingResulted.hearingResulted()
                .withHearing(hearing)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(ESTIMATED_MINUTES)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then - verify that no events were appended (stream is empty)
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());
        assertThat(events.size(), is(0));
    }

    @Test
    public void shouldExtractHearingIdAndEstimatedMinutesFromPayload() throws EventStreamException {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .build();

        hearingAggregate.apply(hearing);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final Integer customEstimatedMinutes = 45;
        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(customEstimatedMinutes)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then
        verify(eventSource).getStreamById(HEARING_ID);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());

        // Verify both events are present
        assertThat(events.size(), is(2));
        
        // Find the HearingUnallocatedCourtroomRemoved event
        final Optional<JsonEnvelope> unallocatedEvent = events.stream()
                .filter(e -> e.metadata().name().equals("progression.event.hearing-unallocated-courtroom-removed"))
                .findFirst();
        
        assertThat(unallocatedEvent.isPresent(), is(true));
        assertThat(unallocatedEvent.get(), jsonEnvelope(
                metadata()
                        .withName("progression.event.hearing-unallocated-courtroom-removed"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                        withJsonPath("$.estimatedMinutes", is(customEstimatedMinutes))
                        )
                ))
        );
    }

    @Test
    public void shouldHandleCommandWithNullEstimatedMinutes() throws EventStreamException {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .build())
                .build();

        hearingAggregate.apply(hearing);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final UnallocateHearingRemoveCourtroom payload = UnallocateHearingRemoveCourtroom.unallocateHearingRemoveCourtroom()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(null)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unallocate-hearing-remove-courtroom")
                .withId(randomUUID())
                .build();

        final Envelope<UnallocateHearingRemoveCourtroom> envelope = envelopeFrom(metadata, payload);

        // When
        handler.handleHearingUnallocatedCourtRoomRemoved(envelope);

        // Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<JsonEnvelope> events = envelopeStream.collect(Collectors.toList());

        // Verify both events are present
        assertThat(events.size(), is(2));
        
        // Find the HearingUnallocatedCourtroomRemoved event
        final Optional<JsonEnvelope> unallocatedEvent = events.stream()
                .filter(e -> e.metadata().name().equals("progression.event.hearing-unallocated-courtroom-removed"))
                .findFirst();
        
        assertThat(unallocatedEvent.isPresent(), is(true));
        assertThat(unallocatedEvent.get(), jsonEnvelope(
                metadata()
                        .withName("progression.event.hearing-unallocated-courtroom-removed"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        withJsonPath("$.hearingId", is(HEARING_ID.toString()))
                        )
                ))
        );
    }
}

