package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.ProsecutionCase;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DeleteHearingCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDeleted.class, HearingDeletedForProsecutionCase.class, HearingDeletedForCourtApplication.class);

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

    @Before
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
    }

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

        // Set hearing was deleted
        hearingAggregate.apply(HearingDeleted.hearingDeleted()
                .withProsecutionCaseIds(caseIds)
                .withHearingId(hearingId)
                .build());

        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

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

        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

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

}
