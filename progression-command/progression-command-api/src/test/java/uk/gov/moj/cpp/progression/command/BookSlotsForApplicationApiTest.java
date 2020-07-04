package uk.gov.moj.cpp.progression.command;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BookSlotsForApplicationApiTest {

    private static final String BOOK_SLOTS_FOR_APPLICATION_NAME = "progression.book-slots-for-application";
    private static final String BOOK_SLOTS_FOR_APPLICATION_COMMAND_NAME = "progression.command.book-slots-for-application";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @InjectMocks
    private BookSlotsForApplicationApi bookSlotsForApplicationApi;

    @Test
    public void shouldHandleBookSlotsForApplicationCommand() {
        assertThat(BookSlotsForApplicationApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleBookSlotsForApplication").thatHandles(BOOK_SLOTS_FOR_APPLICATION_NAME)));
    }

    @Test
    public void shouldBookSlotsForApplication() {

        final JsonEnvelope commandEnvelope = buildEnvelope();

        bookSlotsForApplicationApi.handleBookSlotsForApplication(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(BOOK_SLOTS_FOR_APPLICATION_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingRequest", Json.createObjectBuilder().add("id", UUID.randomUUID().toString()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(BOOK_SLOTS_FOR_APPLICATION_NAME)
                .withId(UUID.randomUUID())
                .withUserId(UUID.randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
