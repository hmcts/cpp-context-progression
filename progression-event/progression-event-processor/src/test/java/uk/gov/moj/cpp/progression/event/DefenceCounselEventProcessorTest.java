package uk.gov.moj.cpp.progression.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.FileUtil.givenPayload;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

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
public class DefenceCounselEventProcessorTest {


    private static final String PROGRESSION_COMMAND_HANDLER_ADD_HEARING_DEFENCE_COUNSEL="progression.command.handler.add-hearing-defence-counsel";
    private static final String PROGRESSION_COMMAND_HANDLER_UPDATE_HEARING_DEFENCE_COUNSEL="progression.command.handler.update-hearing-defence-counsel";
    private static final String PROGRESSION_COMMAND_HANDLER_REMOVE_HEARING_DEFENCE_COUNSEL="progression.command.handler.remove-hearing-defence-counsel";

    @Mock
    private Sender sender;

    @InjectMocks
    private DefenceCounselEventProcessor defenceCounselEventProcessor;

    private final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
            ArgumentCaptor.forClass(JsonEnvelope.class);

    @Test
    public void hearingDefenceCounselAdded() {
        verifyEventsHandled("/public.hearing.defence-counsel-added-or-updated.json",
                PROGRESSION_COMMAND_HANDLER_ADD_HEARING_DEFENCE_COUNSEL,
                getPayload("progression.command.handler.put-hearing-defence-counsel.json")
                        .replace("ACTION", "ADD"),
                envelope -> defenceCounselEventProcessor.hearingDefenceCounselAdded(envelope));
    }

    @Test
    public void hearingDefenceCounselUpdated() {
        verifyEventsHandled("/public.hearing.defence-counsel-added-or-updated.json",
                PROGRESSION_COMMAND_HANDLER_UPDATE_HEARING_DEFENCE_COUNSEL,
                getPayload("progression.command.handler.put-hearing-defence-counsel.json")
                        .replace("ACTION", "UPDATE"),
                envelope -> defenceCounselEventProcessor.hearingDefenceCounselUpdated(envelope));
    }

    @Test
    public void hearingDefenceCounselRemoved() {
        verifyEventsHandled("/public.hearing.counsel-removed.json",
                PROGRESSION_COMMAND_HANDLER_REMOVE_HEARING_DEFENCE_COUNSEL,
                getPayload("progression.command.handler.remove-hearing-defence-counsel.json")
                        .replace("COUNSEL_TYPE", "DEFENCE"),
                envelope -> defenceCounselEventProcessor.hearingDefenceCounselRemoved(envelope));
    }

    private void verifyEventsHandled(String inputPayloadFile,String expectedCommand,  String expectedPayload,
                                     Consumer<JsonEnvelope> handler) {
        //when

        handler.accept(envelopeFrom(metadataWithRandomUUIDAndName(),
                givenPayload(inputPayloadFile)));

        //then
        verifyCommandHandlerCalled(expectedPayload, expectedCommand);
    }

    private void verifyCommandHandlerCalled(final String expectedPayload, String expectedCommand) {
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(),
                is(expectedCommand));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(),
                is(toJsonObject(expectedPayload)));
    }

    private JsonObject toJsonObject(final String value) {
        return JsonObjects.createReader(new StringReader(value)).readObject();
    }
}
