package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.hearing.courts.RemoveOffencesFromExistingHearing;
import uk.gov.justice.progression.courts.HearingRemovedForProsecutionCase;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
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
import uk.gov.moj.cpp.progression.helper.EventStreamHelper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RemoveOffencesHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
             HearingRemovedForProsecutionCase.class, OffencesRemovedFromHearing.class);
    @Mock
    EventStreamHelper eventStreamHelper;
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
    private RemoveOffencesHandler removeOffencesHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleRemoveOffenceFromExistingHearing() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final List<UUID> offenceIds = Arrays.asList(offenceId, offenceId1);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.remove-offences-from-existing-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<RemoveOffencesFromExistingHearing> envelope = envelopeFrom(metadata, RemoveOffencesFromExistingHearing.removeOffencesFromExistingHearing()
                .withHearingId(hearingId)
                .withOffenceIds(offenceIds)
                .build());

        OffencesRemovedFromHearing build = OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withOffenceIds(offenceIds)
                .withHearingId(hearingId)
                .build();
        when(hearingAggregate.removeOffenceFromHearing(eq(hearingId), eq(offenceIds)))
                .thenReturn(Stream.of(build));

        removeOffencesHandler.removeOffencesFromExistingHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.offences-removed-from-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.offenceIds.[0]", is(offenceId1.toString()))
                        )
                        )
                ))
        );
    }

    public String[] jsonArrayToStringArray(final JSONArray jsonArray) {
        int arraySize = jsonArray.size();
        final String[] stringArray = new String[arraySize];

        for (int i = 0; i < arraySize; i++) {
            stringArray[i] = (String) jsonArray.get(i);
        }
        return stringArray;
    }
}
