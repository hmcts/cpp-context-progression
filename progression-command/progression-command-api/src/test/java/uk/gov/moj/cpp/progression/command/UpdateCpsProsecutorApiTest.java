package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCpsProsecutorApiTest {

    private static final String UPDATE_CPS_PROSECUTOR_DETAILS = "update-cps-prosecutor-details";
    private static final String COMMAND_UPDATE_CPS_PROSECUTOR_DETAILS = "progression.command.update-cps-prosecutor-details";
    @Mock
    private Sender sender;

    @InjectMocks
    private UpdateCpsProsecutorApi updateCpsProsecutorApi;

    private JsonEnvelope envelope;

    private UUID uuid;
    private UUID userId;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @BeforeEach
    public void setUp() {
        uuid = randomUUID();
        userId = randomUUID();
    }

    @Test
    public void handleUpdateCpsProsecutorDetails() {
        envelope = buildEnvelope();
        updateCpsProsecutorApi.handleUpdateCpsProsecutorDetails(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final Envelope newCommand = envelopeArgumentCaptor.getValue();

        assertThat(newCommand.metadata().name(), equalTo(COMMAND_UPDATE_CPS_PROSECUTOR_DETAILS));
        assertThat(newCommand.payload(), equalTo(envelope.payload()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder().build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(UPDATE_CPS_PROSECUTOR_DETAILS)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}