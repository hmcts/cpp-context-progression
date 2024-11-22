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

import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.progression.courts.MarkHearingAsDuplicate;
import uk.gov.justice.progression.courts.MarkHearingAsDuplicateForCase;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class MarkHearingAsDuplicateCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingMarkedAsDuplicate.class,
            HearingMarkedAsDuplicateForCase.class);

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

    @InjectMocks
    @Spy
    private MarkHearingAsDuplicateCommandHandler markHearingAsDuplicateCommandHandler;


    @Test
    public void shouldHandleMarkHearingAsDuplicate() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID case1Id = UUID.randomUUID();
        final UUID case2Id = UUID.randomUUID();
        final UUID defendant1Id = UUID.randomUUID();
        final UUID defendant2Id = UUID.randomUUID();
        final List<UUID> caseIds = Arrays.asList(case1Id, case2Id);
        final List<UUID> defendantIds = Arrays.asList(defendant1Id, defendant2Id);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.mark-hearing-as-duplicate")
                .withId(randomUUID())
                .build();

        final Envelope<MarkHearingAsDuplicate> envelope = envelopeFrom(metadata, MarkHearingAsDuplicate.markHearingAsDuplicate()
                .withHearingId(hearingId)
                .withProsecutionCaseIds(caseIds)
                .withDefendantIds(defendantIds)
                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.markAsDuplicate(eq(hearingId), eq(caseIds), eq(defendantIds)))
                .thenReturn(Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                        .withCaseIds(caseIds)
                        .withDefendantIds(defendantIds)
                        .withHearingId(hearingId)
                        .build()));

        markHearingAsDuplicateCommandHandler.handleMarkHearingAsDuplicate(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-marked-as-duplicate"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.caseIds", equalTo(Arrays.asList(case1Id.toString(), case2Id.toString()))),
                                withJsonPath("$.defendantIds", equalTo(Arrays.asList(defendant1Id.toString(), defendant2Id.toString())))
                        ))
                ))
        );
    }

    @Test
    public void shouldHandleMarkHearingAsDuplicateForCase() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendant1Id = UUID.randomUUID();
        final UUID defendant2Id = UUID.randomUUID();
        final List<UUID> defendantIds = Arrays.asList(defendant1Id, defendant2Id);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.mark-hearing-as-duplicate-for-case")
                .withId(randomUUID())
                .build();

        final Envelope<MarkHearingAsDuplicateForCase> envelope = envelopeFrom(metadata, MarkHearingAsDuplicateForCase.markHearingAsDuplicateForCase()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withDefendantIds(defendantIds)
                .build());

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        when(caseAggregate.markHearingAsDuplicate(eq(hearingId), eq(caseId), eq(defendantIds)))
                .thenReturn(Stream.of(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                        .withCaseId(caseId)
                        .withHearingId(hearingId)
                        .withDefendantIds(defendantIds)
                        .build()));

        markHearingAsDuplicateCommandHandler.handleMarkHearingAsDuplicateForCase(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-marked-as-duplicate-for-case"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.defendantIds", equalTo(Arrays.asList(defendant1Id.toString(), defendant2Id.toString())))
                                )
                        ))));
    }
}
