package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListNewHearingApiTest {
    private static final String LIST_NEW_HEARING_NAME = "progression.list-new-hearing";
    private static final String LIST_COMMAND_NEW_HEARING_NAME = "progression.command.list-new-hearing";

    @InjectMocks
    private ListNewHearingApi listNewHearingApi;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private UUID uuid;
    private UUID userId;

    @Before
    public void setUp() {
        uuid = randomUUID();
        userId = randomUUID();
    }

    @Test
    public void shouldHandleAddDocumentCommand() {
        assertThat(ListNewHearingApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(LIST_NEW_HEARING_NAME)));
    }

    @Test
    public void shouldListNewHearing() {
        final JsonObject payload = CommandClientTestBase.readJson("json/progression.list-new-hearing.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(LIST_NEW_HEARING_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);


        listNewHearingApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(LIST_COMMAND_NEW_HEARING_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }
}
