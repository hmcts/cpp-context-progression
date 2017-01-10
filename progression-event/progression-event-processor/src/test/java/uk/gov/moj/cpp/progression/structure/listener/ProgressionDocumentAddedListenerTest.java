package uk.gov.moj.cpp.progression.structure.listener;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionDocumentAddedListenerTest {


    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;


    @InjectMocks
    ProgressionDocumentAddedListener progressionDocumentAddedListener;


    private static final String MATERIAL_COMMAND_ADD_MATERIAL = "material.add-material";


    @Test
    public void shouldProcessEvent() {

        final String randomId = UUID.randomUUID().toString();

        JsonEnvelope envelope = getEnvelope(randomId);

        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(),
                        NewCaseDocumentReceivedEvent.class)).thenReturn(
                                        new NewCaseDocumentReceivedEvent(UUID.randomUUID(),
                                                        envelope.payloadAsJsonObject()));

        progressionDocumentAddedListener.processEvent(envelope);

        ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, atLeast(1)).send(jsonEnvelopeCaptor.capture());

        JsonEnvelope resultEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(resultEnvelope.metadata().name(), equalTo(MATERIAL_COMMAND_ADD_MATERIAL));

    }

    private JsonEnvelope getEnvelope(final String id) {
        return DefaultJsonEnvelope.envelope()
                        .with(metadataOf(id, id).withUserId(id).withSessionId(id)
                                        .withClientCorrelationId(id))
                        .withPayloadOf(id, "cppCaseId").withPayloadOf("fileId-1", "fileId")
                        .withPayloadOf("application/pdf", "fileMimeType")
                        .withPayloadOf("fileName-1", "fileName").build();

    }

}
