package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.command.handler.PatchAndResendLaaOutcomeConcluded.patchAndResendLaaOutcomeConcluded;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.PatchAndResendLaaOutcomeConcluded;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;

@ExtendWith(MockitoExtension.class)
class PatchAndResendLaaCaseOutcomeHandlerTest {
    private static final ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventStream eventStream;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(LaaDefendantProceedingConcludedResent.class);

    @InjectMocks
    private PatchAndResendLaaCaseOutcomeHandler handler;

    @Test
    void shouldHandleCommandSuccessfullyWhenLAAEventFoundForResultingDateWithEmptyHearingID() throws Exception {
        final ZonedDateTime resultSharedDateTime = ZonedDateTime.now();
        final LaaDefendantProceedingConcludedChanged.Builder laaEventBuilder = laaDefendantProceedingConcludedChanged()
                .withValuesFrom(handlerTestHelper.convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class));

        Stream<JsonEnvelope> eventLogStream = Stream.<JsonEnvelope>builder()
                .add(existingLaaDefendantProceedingEvent(resultSharedDateTime, laaEventBuilder.withHearingId(null).build()))
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(eventLogStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        final PatchAndResendLaaOutcomeConcluded payload = patchAndResendLaaOutcomeConcluded()
                .withCaseId(UUID.randomUUID())
                .withHearingId(UUID.randomUUID())
                .withResultDate(resultSharedDateTime.toLocalDate())
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.patch-and-resend-laa-outcome-concluded")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchAndResendLaaOutcomeConcluded> envelope = envelopeFrom(metadataBuilder, payload);

        handler.handle(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).toList();
        String actual = eventList.get(0).payloadAsJsonObject().getJsonObject("laaDefendantProceedingConcludedChanged").toString();
        final LaaDefendantProceedingConcludedChanged expectedEvent = laaEventBuilder.withHearingId(payload.getHearingId()).build();
        final String expectedPayload = objectToJsonConverter.convert(expectedEvent).toString();
        assertEquals(expectedPayload, actual, JSONCompareMode.STRICT);
    }

    @Test
    void shouldHandleCommandWhenNoMatchingEventFoundWithResultingDate() {
        final ZonedDateTime resultSharedDateTime = ZonedDateTime.now();
        Stream<JsonEnvelope> eventLogStream = Stream.<JsonEnvelope>builder()
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(eventLogStream);

        final PatchAndResendLaaOutcomeConcluded payload = patchAndResendLaaOutcomeConcluded()
                .withCaseId(UUID.randomUUID())
                .withHearingId(UUID.randomUUID())
                .withResultDate(resultSharedDateTime.toLocalDate())
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.patch-and-resend-laa-outcome-concluded")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchAndResendLaaOutcomeConcluded> envelope = envelopeFrom(metadataBuilder, payload);

        final RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> handler.handle(envelope));
        assertThat(runtimeException.getMessage(), containsString("single progression.event.laa-defendant-proceeding-concluded-changed event was expected but no of events found:0, for request:"));
    }

    @Test
    void shouldHandleCommandWhenMatchingEventFoundForResultingDateWithHearingID() throws IOException {
        final ZonedDateTime resultSharedDateTime = ZonedDateTime.now();

        final LaaDefendantProceedingConcludedChanged.Builder laaEventBuilder = laaDefendantProceedingConcludedChanged()
                .withValuesFrom(handlerTestHelper.convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class));

        Stream<JsonEnvelope> eventLogStream = Stream.<JsonEnvelope>builder()
                .add(existingLaaDefendantProceedingEvent(resultSharedDateTime, laaEventBuilder.withHearingId(UUID.randomUUID()).build()))
                .build();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(eventLogStream);

        final PatchAndResendLaaOutcomeConcluded payload = patchAndResendLaaOutcomeConcluded()
                .withCaseId(UUID.randomUUID())
                .withHearingId(UUID.randomUUID())
                .withResultDate(resultSharedDateTime.toLocalDate())
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.patch-and-resend-laa-outcome-concluded")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchAndResendLaaOutcomeConcluded> envelope = envelopeFrom(metadataBuilder, payload);

        final RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> handler.handle(envelope));
        assertThat(runtimeException.getMessage(), containsString("single progression.event.laa-defendant-proceeding-concluded-changed event was expected but no of events found:0, for request:"));
    }

    @Test
    void shouldHandleCommandWhenMoreThanOneMatchingEventsFound() throws IOException {
        final ZonedDateTime resultSharedDateTime = ZonedDateTime.now();
        final LaaDefendantProceedingConcludedChanged.Builder laaEventBuilder = laaDefendantProceedingConcludedChanged()
                .withValuesFrom(handlerTestHelper.convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class));

        Stream<JsonEnvelope> eventLogStream = Stream.<JsonEnvelope>builder()
                .add(existingLaaDefendantProceedingEvent(resultSharedDateTime, laaEventBuilder.withHearingId(null).build()))
                .add(existingLaaDefendantProceedingEvent(resultSharedDateTime, laaEventBuilder.withHearingId(null).build()))
                .build();        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(eventLogStream);

        final PatchAndResendLaaOutcomeConcluded payload = patchAndResendLaaOutcomeConcluded()
                .withCaseId(UUID.randomUUID())
                .withHearingId(UUID.randomUUID())
                .withResultDate(resultSharedDateTime.toLocalDate())
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.patch-and-resend-laa-outcome-concluded")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchAndResendLaaOutcomeConcluded> envelope = envelopeFrom(metadataBuilder, payload);

        final RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> handler.handle(envelope));
        assertThat(runtimeException.getMessage(), containsString("single progression.event.laa-defendant-proceeding-concluded-changed event was expected but no of events found:2, for request:"));
    }

    private JsonEnvelope existingLaaDefendantProceedingEvent(final ZonedDateTime createdAt, final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged) {
        final Metadata eventMetadata = metadataBuilder().withName("progression.event.laa-defendant-proceeding-concluded-changed")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .createdAt(createdAt)
                .build();

        return JsonEnvelope.envelopeFrom(eventMetadata, objectToJsonConverter.convert(laaDefendantProceedingConcludedChanged));
    }
}