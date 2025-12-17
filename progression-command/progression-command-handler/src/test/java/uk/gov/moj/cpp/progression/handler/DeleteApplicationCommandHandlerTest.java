package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
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

import uk.gov.justice.core.courts.CourtApplicationHearingDeleted;
import uk.gov.justice.core.courts.CourtApplicationRemovedFromSeedingHearing;
import uk.gov.justice.core.courts.DeleteCourtApplicationHearingRequested;
import uk.gov.justice.progression.courts.DeleteApplicationForCase;
import uk.gov.justice.progression.courts.DeleteCourtApplicationHearing;
import uk.gov.justice.progression.courts.RemoveApplicationFromSeedingHearing;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteApplicationCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DeleteCourtApplicationHearingRequested.class,
            CourtApplicationRemovedFromSeedingHearing.class,
            CourtApplicationHearingDeleted.class
    );

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

    @Mock
    private ApplicationAggregate applicationAggregate;

    @InjectMocks
    @Spy
    private DeleteApplicationCommandHandler deleteApplicationCommandHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
    }

    @Test
    public void shouldHandleDeleteApplicationForCase() throws EventStreamException {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-application-for-case")
                .withId(randomUUID())
                .build();

        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final Envelope<DeleteApplicationForCase> envelope = envelopeFrom(metadata, DeleteApplicationForCase.deleteApplicationForCase()
                .withApplicationId(applicationId)
                .withSeedingHearingId(seedingHearingId)
                .build());

        when(applicationAggregate.deleteCourtApplication(applicationId, seedingHearingId))
                .thenReturn(Stream.of(DeleteCourtApplicationHearingRequested.deleteCourtApplicationHearingRequested()
                        .withHearingId(hearingId)
                        .withSeedingHearingId(seedingHearingId)
                        .withApplicationId(applicationId)
                        .build()));

        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        deleteApplicationCommandHandler.handleDeleteApplicationForCase(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.delete-court-application-hearing-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.seedingHearingId", is(seedingHearingId.toString())),
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.applicationId", is(applicationId.toString()))
                        ))
                ))
        );
    }

    @Test
    public void shouldHandleDeleteCourtApplicationHearing() throws EventStreamException {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-application-for-case")
                .withId(randomUUID())
                .build();

        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final Envelope<DeleteCourtApplicationHearing> envelope = envelopeFrom(metadata, DeleteCourtApplicationHearing.deleteCourtApplicationHearing()
                .withApplicationId(applicationId)
                .withSeedingHearingId(seedingHearingId)
                .withHearingId(hearingId)
                .build());
        when(hearingAggregate.deleteCourtApplicationHearing(hearingId, applicationId, seedingHearingId))
                .thenReturn(Stream.of(CourtApplicationHearingDeleted.courtApplicationHearingDeleted()
                        .withHearingId(hearingId)
                        .withSeedingHearingId(seedingHearingId)
                        .withApplicationId(applicationId)
                        .build()));
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        deleteApplicationCommandHandler.handleDeleteCourtApplicationHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-hearing-deleted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.seedingHearingId", is(seedingHearingId.toString())),
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.applicationId", is(applicationId.toString()))
                        ))
                ))
        );
    }

    @Test
    public void shouldHandleRemoveApplicationFromSeedingHearing() throws EventStreamException {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.remove-application-from-seedingHearing")
                .withId(randomUUID())
                .build();
        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Envelope<RemoveApplicationFromSeedingHearing> envelope = envelopeFrom(metadata, RemoveApplicationFromSeedingHearing.removeApplicationFromSeedingHearing()
                .withApplicationId(applicationId)
                .withSeedingHearingId(seedingHearingId)
                .build());

        when(hearingAggregate.removeApplicationFromSeedingHearing(seedingHearingId, applicationId))
                .thenReturn(Stream.of(CourtApplicationRemovedFromSeedingHearing.courtApplicationRemovedFromSeedingHearing()
                        .withApplicationId(applicationId)
                        .withSeedingHearingId(seedingHearingId)
                        .build()));

        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        deleteApplicationCommandHandler.handleRemoveApplicationFromSeedingHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-removed-from-seeding-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.seedingHearingId", is(seedingHearingId.toString())),
                                withJsonPath("$.applicationId", is(applicationId.toString()))
                        ))
                ))
        );
    }
}
