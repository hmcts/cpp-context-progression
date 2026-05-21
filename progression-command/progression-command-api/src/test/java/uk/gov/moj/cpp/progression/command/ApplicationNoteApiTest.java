package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationNoteApiTest {

    private final static String PROGRESSION_COMMAND_ADD_APPLICATION_NOTE = "progression.command.add-application-note";
    private final static String PROGRESSION_COMMAND_EDIT_APPLICATION_NOTE = "progression.command.edit-application-note";
    private final static String PROGRESSION_COMMAND_HANDLER_ADD_APPLICATION_NOTE = "progression.command.handler.add-application-note";
    private final static String PROGRESSION_COMMAND_HANDLER_EDIT_APPLICATION_NOTE = "progression.command.handler.edit-application-note";
    @Mock
    private Sender sender;

    @InjectMocks
    private ApplicationNoteApi applicationNoteApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldAddApplicationNote() {
        //Given
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("note", "Sample application note")
                .add("isPinned", false)
                .build();

        //When
        applicationNoteApi.addApplicationNote(createCommandEnvelope(PROGRESSION_COMMAND_ADD_APPLICATION_NOTE, payload));

        //Then
        verifyResult(PROGRESSION_COMMAND_HANDLER_ADD_APPLICATION_NOTE, payload);
    }

    @Test
    public void shouldEditApplicationNote() {
        //Given
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("isPinned", true)
                .build();

        //When
        applicationNoteApi.editApplicationNote(createCommandEnvelope(PROGRESSION_COMMAND_EDIT_APPLICATION_NOTE, payload));

        //Then
        verifyResult(PROGRESSION_COMMAND_HANDLER_EDIT_APPLICATION_NOTE, payload);
    }

    private JsonEnvelope createCommandEnvelope(final String commandName, final JsonObject payload) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(commandName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private void verifyResult(final String commandHandlerName, final JsonObject payload) {
        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(commandHandlerName));
        assertThat(capturedEnvelope.payload(), is(payload));
    }
}
