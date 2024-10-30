package uk.gov.justice.api.resource;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

        uploadFileServiceSender.doSend(payload, "userId", "correlationId");

        ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, atLeast(1)).send(jsonEnvelopeCaptor.capture());

        JsonEnvelope envelope = jsonEnvelopeCaptor.getValue();
        assertThat(envelope.metadata().name(), equalTo(SINGLE_DOCUMENT_COMMAND));

    }


}
