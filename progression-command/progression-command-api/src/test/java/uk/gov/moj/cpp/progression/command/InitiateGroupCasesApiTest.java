package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

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
public class InitiateGroupCasesApiTest {

    private static final String REQUEST_NAME = "progression.initiate-court-proceedings-for-group-cases";
    private static final String COMMAND_NAME = "progression.command.initiate-court-proceedings-for-group-cases";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private InitiateGroupCasesApi initiateCivilBulkCasesApi;

    @Test
    public void shouldHandleRequest() {
        assertThat(InitiateGroupCasesApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(REQUEST_NAME)));
    }

    @Test
    public void shouldSendCommand() {

        final JsonEnvelope commandEnvelope = buildEnvelope();


        initiateCivilBulkCasesApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("civilBulkInitiateCourtProceedings", JsonObjects.createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(REQUEST_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
