package uk.gov.moj.cpp.progression.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.EnforcementAcknowledgmentError;
import uk.gov.justice.core.courts.NowsRequestWithAccountNumberUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingEnforcementResponseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(NowsRequestWithAccountNumberUpdated.class);

    @Spy
    protected JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private StagingEnforcementResponseHandler commandHandler;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private MaterialAggregate materialAggregate;

    @BeforeEach
    public void setup() {
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
    }

    @Test
    public void shouldApplyEnforcementAcknowledgement() throws EventStreamException {
        final UUID requestId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        final String accountNumber = "1234";
        final JsonObject payload = Json.createObjectBuilder()
                .add("originator", "courts")
                .add("acknowledgement", Json.createObjectBuilder().add("accountNumber", accountNumber)
                        .build())
                .add("requestId", requestId.toString())
                .add("materialId", materialId.toString()).build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final Stream<Object> objectStream = Stream.of(new NowsRequestWithAccountNumberUpdated(accountNumber, requestId));
        when(materialAggregate.saveAccountNumber(materialId, requestId, accountNumber)).thenReturn(objectStream);
        commandHandler.applyEnforcementAcknowledgement(envelope);
        verify(materialAggregate).saveAccountNumber(materialId, requestId, accountNumber);
    }

    @Test
    public void shouldEnforcementAcknowledgementError() throws EventStreamException {
        final UUID requestId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        final String errorCode = "ERR1234";
        final String errorMessage = "post code is invalid";
        final JsonObject payload = Json.createObjectBuilder()
                .add("originator", "courts")
                .add("acknowledgement", Json.createObjectBuilder().add("errorCode", errorCode)
                        .add("errorMessage", errorMessage)
                        .build())
                .add("requestId", requestId.toString())
                .add("materialId", materialId.toString()).build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final Stream<Object> objectStream = Stream.of(new EnforcementAcknowledgmentError(errorCode, errorMessage, requestId));
        when(materialAggregate.recordEnforcementError(requestId, errorCode, errorMessage)).thenReturn(objectStream);
        commandHandler.enforcementAcknowledgmentError(envelope);
        verify(materialAggregate).recordEnforcementError(requestId, errorCode, errorMessage);
    }
}