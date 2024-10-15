package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberDecreased;
import uk.gov.justice.progression.courts.DecreaseListingNumberForProsecutionCase;
import uk.gov.justice.progression.courts.DeleteHearing;
import uk.gov.justice.progression.courts.DeleteHearingForCourtApplication;
import uk.gov.justice.progression.courts.DeleteHearingForProsecutionCase;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DeleteHearingCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDeleted.class, HearingDeletedForProsecutionCase.class, HearingDeletedForCourtApplication.class,
            ProsecutionCaseListingNumberDecreased.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private ApplicationAggregate applicationAggregate;

    @InjectMocks
    @Spy
    private DeleteHearingCommandHandler deleteHearingCommandHandler;

    @Test
    public void shouldHandleDeleteHearing() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCase1Id = UUID.randomUUID();
        final UUID prosecutionCase2Id = UUID.randomUUID();
        final List<UUID> caseIds = Arrays.asList(prosecutionCase1Id, prosecutionCase2Id);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearing> envelope = envelopeFrom(metadata, DeleteHearing.deleteHearing()
                .withHearingId(hearingId)
                .build());

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.deleteHearing(eq(hearingId)))
                .thenReturn(Stream.of(HearingDeleted.hearingDeleted()
                        .withProsecutionCaseIds(caseIds)
                        .withHearingId(hearingId)
                        .build()));

        deleteHearingCommandHandler.handleDeleteHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-deleted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.prosecutionCaseIds", equalTo(Arrays.asList(prosecutionCase1Id.toString(), prosecutionCase2Id.toString())))
                        ))
                ))
        );
    }

    @Test
    public void shouldHandleDeleteHearingWhenHearingWasDeleted() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCase1Id = UUID.randomUUID();
        final UUID prosecutionCase2Id = UUID.randomUUID();
        final List<UUID> caseIds = Arrays.asList(prosecutionCase1Id, prosecutionCase2Id);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearing> envelope = envelopeFrom(metadata, DeleteHearing.deleteHearing()
                .withHearingId(hearingId)
                .build());

        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        // Set hearing was deleted
        hearingAggregate.apply(HearingDeleted.hearingDeleted()
                .withProsecutionCaseIds(caseIds)
                .withHearingId(hearingId)
                .build());

        deleteHearingCommandHandler.handleDeleteHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> hearingDeletedEnvelope = envelopeStream
                .filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("progression.event.hearing-deleted"))
                .findAny();

        assertFalse(hearingDeletedEnvelope.isPresent());
    }

    @Test
    public void shouldHandleDeleteHearingForProsecutionCase() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-hearing-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearingForProsecutionCase> envelope = envelopeFrom(metadata, DeleteHearingForProsecutionCase.deleteHearingForProsecutionCase()
                .withHearingId(hearingId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build());

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        when(caseAggregate.deleteHearingRelatedToProsecutionCase(eq(hearingId), eq(prosecutionCaseId)))
                .thenReturn(Stream.of(HearingDeletedForProsecutionCase.hearingDeletedForProsecutionCase()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withHearingId(hearingId)
                        .build()));

        deleteHearingCommandHandler.handleDeleteHearingForProsecutionCase(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-deleted-for-prosecution-case"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId.toString()))
                                )
                        ))));
    }

    @Test
    public void shouldHandleDeleteHearingForCourtApplication() throws EventStreamException {

        final UUID hearingId = UUID.randomUUID();
        final UUID courtApplicationId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-hearing-for-court-application")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearingForCourtApplication> envelope = envelopeFrom(metadata, DeleteHearingForCourtApplication.deleteHearingForCourtApplication()
                .withHearingId(hearingId)
                .withCourtApplicationId(courtApplicationId)
                .build());

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(applicationAggregate.deleteHearingRelatedToCourtApplication(eq(hearingId), eq(courtApplicationId)))
                .thenReturn(Stream.of(HearingDeletedForCourtApplication.hearingDeletedForCourtApplication()
                        .withCourtApplicationId(courtApplicationId)
                        .withHearingId(hearingId)
                        .build()));

        deleteHearingCommandHandler.handleDeleteHearingForCourtApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-deleted-for-court-application"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.courtApplicationId", equalTo(courtApplicationId.toString()))
                                )
                        ))));
    }

    @Test
    public void shouldHandleRemoveDeletedHearingChildEntriesOnlyByBdfWhenHearingWasDeleted() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCase1Id = UUID.randomUUID();
        final UUID prosecutionCase2Id = UUID.randomUUID();
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        final List<UUID> caseIds = Arrays.asList(prosecutionCase1Id, prosecutionCase2Id);

        final Hearing hearing = getHearing(hearingId, prosecutionCase1Id, prosecutionCase2Id);

        // Set the hearing
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        // Set hearing was deleted
        hearingAggregate.apply(HearingDeleted.hearingDeleted()
                .withProsecutionCaseIds(caseIds)
                .withHearingId(hearingId)
                .build());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.remove-deleted-hearing-child-entries-bdf")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearing> envelope = envelopeFrom(metadata, DeleteHearing.deleteHearing()
                .withHearingId(hearingId)
                .build());

        deleteHearingCommandHandler.handleRemoveDeletedHearingChildEntriesOnlyByBdf(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope> hearingDeletedEnvelope = envelopeStream
                .filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("progression.event.hearing-deleted"))
                .findAny();

        assertTrue(hearingDeletedEnvelope.isPresent());
    }

    @Test
    public void shouldHandleRemoveDeletedHearingChildEntriesOnlyByBdf() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.remove-deleted-hearing-child-entries-bdf")
                .withId(randomUUID())
                .build();

        final Envelope<DeleteHearing> envelope = envelopeFrom(metadata, DeleteHearing.deleteHearing()
                .withHearingId(hearingId)
                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.deleteHearingOnlyByBdf(eq(hearingId)))
                .thenReturn(Stream.of(HearingDeleted.hearingDeleted()
                        .withHearingId(hearingId)
                        .build()));

        deleteHearingCommandHandler.handleRemoveDeletedHearingChildEntriesOnlyByBdf(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-deleted"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString()))
                        ))
                ))
        );
    }

    private Hearing getHearing(final UUID hearingId, final UUID caseId1, final UUID caseId2) {
        final List<ProsecutionCase> prosecutionCases = Arrays.asList(getProsecutionCase(caseId1), getProsecutionCase(caseId2));

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .build();
        return hearing;
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId) {
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(Collections.emptyList())
                .build();

        return prosecutionCase;
    }

    @Test
    public void shouldHandleDecreaseListingNumber() throws EventStreamException {
        final UUID caseId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.decrease-listing-number-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<DecreaseListingNumberForProsecutionCase> envelope = envelopeFrom(metadata, DecreaseListingNumberForProsecutionCase.decreaseListingNumberForProsecutionCase()
                .withProsecutionCaseId(caseId)
                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.decreaseListingNumbers(any()))
                .thenReturn(Stream.of(ProsecutionCaseListingNumberDecreased.prosecutionCaseListingNumberDecreased()
                        .withProsecutionCaseId(caseId)
                        .build()));

        deleteHearingCommandHandler.handleDecreaseListingNumber(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope> prosecutionCaseListingNumberDecreasedEnvelope = envelopeStream
                .filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("progression.event.prosecution-case-listing-number-decreased"))
                .findAny();

        assertTrue(prosecutionCaseListingNumberDecreasedEnvelope.isPresent());
    }
}
