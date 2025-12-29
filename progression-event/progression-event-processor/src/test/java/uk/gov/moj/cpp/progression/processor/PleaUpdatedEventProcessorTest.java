package uk.gov.moj.cpp.progression.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.test.FileUtil.givenPayload;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaUpdatedEventProcessorTest {

    private static final String PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED = "public.hearing.hearing-offence-plea-updated";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_OFFENCE_PLEA = "progression.command.update-hearing-offence-plea";
    private static final String JSON = ".json";

    private final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

    @Mock
    private Sender sender;

    @InjectMocks
    private PleaUpdatedEventProcessor pleaUpdatedEventProcessor;

    @Test
    public void hearingPleaUpdated() throws IOException {
        verifyEventsHandled("/" + PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED + JSON,
                FileUtil.getPayload(PROGRESSION_COMMAND_UPDATE_HEARING_OFFENCE_PLEA + JSON),
                envelope -> pleaUpdatedEventProcessor.hearingPleaUpdated(envelope));
    }

    private void verifyEventsHandled(String inputPayloadFile, String expectedPayload,
                                     Consumer<JsonEnvelope> handler) throws IOException {
        handler.accept(envelopeFrom(metadataWithRandomUUIDAndName(), givenPayload(inputPayloadFile)));

        verifyCommandHandlerCalled(expectedPayload);
    }

    private void verifyCommandHandlerCalled(final String expectedPayload) {
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertEquals(PROGRESSION_COMMAND_UPDATE_HEARING_OFFENCE_PLEA, senderJsonEnvelopeCaptor.getValue().metadata().name());
        assertEquals(toJsonObject(expectedPayload), senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject());
    }

    private JsonObject toJsonObject(final String value) {
        return JsonObjects.createReader(new StringReader(value)).readObject();
    }
}
