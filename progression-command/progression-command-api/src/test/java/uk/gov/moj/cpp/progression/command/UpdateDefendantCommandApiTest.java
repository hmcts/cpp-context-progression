package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class UpdateDefendantCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private UpdateDefendantCommandApi updateDefendantCommand;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> captor;

    @Test
    public void shouldUpdateDefendant() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("prosecutionCaseId", randomUUID().toString())
                .add("defendant", createObjectBuilder())
                .build());
        when(enveloper.withMetadataFrom(command, "progression.command.update-defendant-for-prosecution-case"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateDefendantCommand.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldUpdateMatchedDefendant() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("prosecutionCaseId", randomUUID().toString())
                .add("matchedDefendantHearingId", randomUUID().toString())
                .add("defendant", createObjectBuilder())
                .build());
        when(enveloper.withMetadataFrom(command, "progression.command.update-defendant-for-matched-defendant"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateDefendantCommand.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldReturnErrorWhenDefendantDoesNotHaveAddress() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("prosecutionCaseId", randomUUID().toString())
                .add("defendant", createObjectBuilder().add("personDefendant", createObjectBuilder().add("personDetails", createObjectBuilder())))
                .build());
        assertThrows(BadRequestException.class, () -> updateDefendantCommand.handle(command));
    }

    @Test
    void shouldUpdateMasterDefendant() {
        final String prosecutionCaseId =  randomUUID().toString();
        final String defendantId =  randomUUID().toString();
        final String masterDefendantId =  randomUUID().toString();

        final JsonEnvelope commandEnvelope = JsonEnvelope.envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("progression.update-master-defendant").build(),
        createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId)
                .add("id", defendantId)
                .add("masterDefendantId",  masterDefendantId)
                .add("processInactiveCase", true)
                .build());

        updateDefendantCommand.handleUpdateMasterDefendant(commandEnvelope);

        verify(sender, times(1)).send(captor.capture());

        final Envelope<JsonObject> command = captor.getValue();

        assertThat(command.metadata().name(), is("progression.command.update-matched-defendant"));
        assertThat(command.payload().getString("prosecutionCaseId"), is(prosecutionCaseId));
        assertThat(command.payload().getString("defendantId"), is(defendantId));
        assertThat(command.payload().getString("masterDefendantId"), is(masterDefendantId));
        assertThat(command.payload().getBoolean("processInactiveCase"), is(true));
    }
}
