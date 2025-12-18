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

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EditCaseNoteApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private CaseNoteApi caseNoteApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldEditCaseNote() {
        //Given
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("isPinned", true)
                .build();

        final JsonEnvelope commandEnvelope = createEditCaseNoteCommandEnvelope(payload);
        //When
        caseNoteApi.editCaseNote(commandEnvelope);
        //Then
        verifyEditCaseNoteResults(payload);
    }

    private void verifyEditCaseNoteResults(final JsonObject payload) {
        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.edit-case-note"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    private JsonEnvelope createEditCaseNoteCommandEnvelope(final JsonObject payload) {
        final UUID uuid = randomUUID();
        final UUID userId = randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.edit-case-note")
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

}
