package uk.gov.moj.cpp.progression.casedocument.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;

@RunWith(MockitoJUnitRunner.class)
public class NewCaseDocumentReceivedListenerTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory
                    .createEnveloperWithEvents(AssociateNewCaseDocumentCommand.class);

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;

    @InjectMocks
    NewCaseDocumentReceivedListener newCaseDocumentReceivedListener;


    @Ignore @Test //TODO ignore until structure context is notified
    public void shouldProcessEvent() {

        final String id = "71824c05-ec1d-4c0e-bc5e-b1ffff07ebee";

        final JsonEnvelope inputEnvelope = getEnvelope(id);

        mockNewCaseDocumentReceivedEvent(inputEnvelope);

        mockAssociateNewCaseDocumentCommand(inputEnvelope);

        newCaseDocumentReceivedListener.processEvent(inputEnvelope);

        ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);

        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        List<JsonEnvelope> envelopes = jsonEnvelopeCaptor.getAllValues();

        JsonEnvelope envelope = envelopes.get(0);

        assertThat(envelope.metadata().userId(), equalTo(Optional.of(id)));
        assertThat(envelope.metadata().sessionId(), equalTo(Optional.of(id)));
        assertThat(envelope.metadata().name(), equalTo("structure.command.add-case-document"));
        assertThat(envelope.asJsonObject().getString("caseId"), equalTo(id));
        assertThat(envelope.asJsonObject().getString("materialId"), equalTo("fileId-1"));
        assertThat(envelope.asJsonObject().getString("documentType"), equalTo("PLEA"));
    }

    private JsonEnvelope getEnvelope(final String id) {
        return DefaultJsonEnvelope.envelope()
                        .with(metadataOf(id, id).withUserId(id).withSessionId(id)
                                        .withClientCorrelationId(id))
                        .withPayloadOf(id, "cppCaseId").withPayloadOf("fileId-1", "fileId")
                        .withPayloadOf("application/pdf", "fileMimeType")
                        .withPayloadOf("fileName-1", "fileName").build();

    }

    private void mockNewCaseDocumentReceivedEvent(final JsonEnvelope envelope) {
        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(),
                        NewCaseDocumentReceivedEvent.class)).thenReturn(
                                        new NewCaseDocumentReceivedEvent(UUID.randomUUID(),
                                                        envelope.asJsonObject()));
    }

    private void mockAssociateNewCaseDocumentCommand(final JsonEnvelope envelope) {
        when(objectToJsonObjectConverter.convert(any(AssociateNewCaseDocumentCommand.class)))
                        .thenReturn(Json.createObjectBuilder()
                                        .add("caseId", envelope.asJsonObject()
                                                        .getString("cppCaseId"))
                                        .add("materialId", "fileId-1").add("documentType", "PLEA")
                                        .build());
    }

}
