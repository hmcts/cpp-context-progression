package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingEnforcementAcknowledgmentEventProcessorTest {

    @InjectMocks
    private StagingEnforcementAcknowledgmentEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope queryResponseEnvelope;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldProcessAcknowledgementWhenOriginatorIsCourts() {
        final String requestId = UUID.randomUUID().toString();
        final String materialId_1 = UUID.randomUUID().toString();
        final String materialId_2 = UUID.randomUUID().toString();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("originator", "courts")
                .add("acknowledgement", JsonObjects.createObjectBuilder().add("accountNumber", 1234)
                        .build())
                .add("requestId", requestId).build();
        when(envelope.metadata()).thenReturn(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("public.stagingenforcement.enforce-financial-imposition-acknowledgement").build());
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final JsonArray jsonResponseArray = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder().add("requestId", requestId).add("materialId", materialId_1).add("payload", "").build())
                .add(JsonObjects.createObjectBuilder().add("requestId", requestId).add("materialId", materialId_2).add("payload", "").build())
                .build();
        JsonObject jsonObject = JsonObjects.createObjectBuilder().add("nowDocumentRequests", jsonResponseArray).build();
        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);
        eventProcessor.processAcknowledgement(envelope);
        verify(sender, times(2)).sendAsAdmin(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().metadata().name(), is("progression.command.apply-enforcement-acknowledgement"));
    }

    @Test
    public void shouldProcessEnforcementAcknowledgementErrorWhenOriginatorIsCourts(){
        final String requestId = UUID.randomUUID().toString();
        final String materialId_1 = UUID.randomUUID().toString();
        final String materialId_2 = UUID.randomUUID().toString();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("originator", "courts")
                .add("acknowledgement", JsonObjects.createObjectBuilder().add("errorCode", "ERR1234").add("errorMessage", "post code is invalid")
                        .build())
                .add("requestId", requestId).build();
        when(envelope.metadata()).thenReturn(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("public.stagingenforcement.enforce-financial-imposition-acknowledgement").build());
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final JsonArray jsonResponseArray = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder().add("requestId", requestId).add("materialId", materialId_1).add("payload", "").build())
                .add(JsonObjects.createObjectBuilder().add("requestId", requestId).add("materialId", materialId_2).add("payload", "").build())
                .build();
        JsonObject jsonObject = JsonObjects.createObjectBuilder().add("nowDocumentRequests", jsonResponseArray).build();
        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);
        eventProcessor.processAcknowledgement(envelope);
        verify(sender, times(2)).sendAsAdmin(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().metadata().name(), is("progression.command.enforcement-acknowledgement-error"));
    }

    @Test
    public void shouldNotProcessEnforcementAcknowledgementWhenNoMaterialsForRequestId() {
        final String requestId = UUID.randomUUID().toString();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("originator", "courts")
                .add("acknowledgement", JsonObjects.createObjectBuilder().add("accountNumber", 1234)
                        .build())
                .add("requestId", requestId).build();
        when(envelope.metadata()).thenReturn(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("public.stagingenforcement.enforce-financial-imposition-acknowledgement").build());
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        JsonObject jsonObject = JsonObjects.createObjectBuilder().add("nowDocumentRequests", JsonObjects.createArrayBuilder().build()).build();
        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);

        eventProcessor.processAcknowledgement(envelope);
        verifyNoInteractions(sender);
    }
}