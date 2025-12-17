package uk.gov.moj.cpp.progression.handler;


import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.courts.ResendLaaOutcomeConcluded;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;

@ExtendWith(MockitoExtension.class)
public class ResendLaaCaseOutcomeHandlerTest {

    @InjectMocks
    private ResendLaaCaseOutcomeHandler resendLaaCaseOutcomeHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            LaaDefendantProceedingConcludedResent.class);

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void testResendDefendantProceedingConludedToLaa() throws EventStreamException, IOException {
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        ResendLaaOutcomeConcluded resendLaaOutcomeConcluded = ResendLaaOutcomeConcluded.resendLaaOutcomeConcluded().withCaseId(caseId).build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.resend-laa-outcome-concluded").withUserId(randomUUID().toString()).build());
        Envelope<ResendLaaOutcomeConcluded> resendLaaOutcomeConcludedEnvelope = envelopeFrom(metadataBuilder, resendLaaOutcomeConcluded);
        Stream<JsonEnvelope> jsonEnvelopeStream = Stream.<JsonEnvelope>builder()
                .add(getJsonEnvelope(hearingId1))
                .add(getJsonEnvelope(hearingId2))
                .build();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(jsonEnvelopeStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        resendLaaCaseOutcomeHandler.handle(resendLaaOutcomeConcludedEnvelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).collect(toList());
        String laaDefendantProceedingConcludedResentPayload1 = eventList.get(0).payloadAsJsonObject().toString();
        String expectedPayload1 = Resources.toString(getResource("json/expected.progression.event.laa-defendant-proceeding-concluded-resent.json"), defaultCharset())
                .replace("HEARING_ID", hearingId1.toString());
        assertEquals(expectedPayload1, laaDefendantProceedingConcludedResentPayload1, JSONCompareMode.STRICT);


        String laaDefendantProceedingConcludedResentPayload2 = eventList.get(1).payloadAsJsonObject().toString();
        String expectedPayload = Resources.toString(getResource("json/expected.progression.event.laa-defendant-proceeding-concluded-resent.json"), defaultCharset())
                .replace("HEARING_ID", hearingId2.toString());
        assertEquals(expectedPayload, laaDefendantProceedingConcludedResentPayload2, JSONCompareMode.STRICT);

    }

    @Test
    public void testResendDefendantProceedingConludedToLaaFailed() throws EventStreamException, IOException {
        final UUID caseId = randomUUID();

        ResendLaaOutcomeConcluded resendLaaOutcomeConcluded = ResendLaaOutcomeConcluded.resendLaaOutcomeConcluded().withCaseId(caseId).build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.resend-laa-outcome-concluded").withUserId(randomUUID().toString()).build());
        Envelope<ResendLaaOutcomeConcluded> resendLaaOutcomeConcludedEnvelope = envelopeFrom(metadataBuilder, resendLaaOutcomeConcluded);
        Stream<JsonEnvelope> jsonEnvelopeStream = Stream.empty();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventStream.read()).thenReturn(jsonEnvelopeStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        try {
            resendLaaCaseOutcomeHandler.handle(resendLaaOutcomeConcludedEnvelope);
            fail("IllegalArgumentException not thrown for case id which does not exist");
        } catch (IllegalArgumentException e){
            final String message = format("event progression.event.laa-defendant-proceeding-concluded-changed not found for caseId : %s", caseId);
            assertThat(e.getMessage(), is(message));
        }


    }

    private JsonEnvelope getJsonEnvelope(UUID hearingId) throws IOException {

        final Metadata eventMetadata = metadataBuilder().withName("progression.event.laa-defendant-proceeding-concluded-changed")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged =
        LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withValuesFrom(handlerTestHelper.convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class))
                .withHearingId(hearingId)
                .build();

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

        return JsonEnvelope.envelopeFrom(eventMetadata, objectToJsonObjectConverter.convert(laaDefendantProceedingConcludedChanged));

    }


}