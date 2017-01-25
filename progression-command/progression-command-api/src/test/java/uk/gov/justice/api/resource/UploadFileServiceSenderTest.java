package uk.gov.justice.api.resource;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class UploadFileServiceSenderTest {

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Sender sender;

    @Mock
    private JsonObject payload;

    @InjectMocks
    UploadFileServiceSender uploadFileServiceSender;

    @Mock
    Function<Object, JsonEnvelope> enveloperFunction;

    private static final String SINGLE_DOCUMENT_COMMAND =
                    "progression.command.upload-case-documents";

    @Test
    public void testDoSend() {

        uploadFileServiceSender.doSend(payload, "userId", "session", "correlationId");

        ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, atLeast(1)).send(jsonEnvelopeCaptor.capture());

        JsonEnvelope envelope = jsonEnvelopeCaptor.getValue();
        assertThat(envelope.metadata().name(), equalTo(SINGLE_DOCUMENT_COMMAND));

    }


}
