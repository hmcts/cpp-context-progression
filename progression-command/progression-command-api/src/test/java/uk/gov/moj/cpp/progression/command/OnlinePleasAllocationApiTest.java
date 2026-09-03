package uk.gov.moj.cpp.progression.command;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OnlinePleasAllocationApiTest {
    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private OnlinePleasAllocationApi onlinePleasAllocationApi;

    private static final String TRIGGER_DATE = "triggerDate";

    @Test
    public void testHandleRequestOpaPublicListNotice() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-public-list-notice", emptyMap());

        onlinePleasAllocationApi.handleRequestOpaPublicListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-public-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().toString()));
    }

    @Test
    public void testHandleRequestOpaPublicListNoticeWithTriggerDate() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-public-list-notice", singletonMap(TRIGGER_DATE, now().plusDays(5).toString()));

        onlinePleasAllocationApi.handleRequestOpaPublicListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-public-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().plusDays(5).toString()));
    }

    @Test
    public void testHandleRequestOpaPressListNotice() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-press-list-notice", emptyMap());

        onlinePleasAllocationApi.handleRequestOpaPressListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-press-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().toString()));
    }

    @Test
    public void testHandleRequestOpaPressListNoticeWithTriggerDate() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-press-list-notice", singletonMap(TRIGGER_DATE, now().plusDays(5).toString()));

        onlinePleasAllocationApi.handleRequestOpaPressListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-press-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().plusDays(5).toString()));
    }

    @Test
    public void testHandleRequestOpaResultListNotice() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-result-list-notice", emptyMap());

        onlinePleasAllocationApi.handleRequestOpaResultListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-result-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().toString()));
    }

    @Test
    public void testHandleRequestOpaResultListNoticeWithTriggerDate() {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.request-opa-result-list-notice", emptyMap());

        onlinePleasAllocationApi.handleRequestOpaResultListNotice(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope<JsonObject> value = envelopeCaptor.getValue();
        assertThat(value.metadata().name(), is("progression.command.request-opa-result-list-notice"));

        final String triggerDate = value.payload().getString(TRIGGER_DATE);
        assertNotNull(triggerDate);
        assertThat(triggerDate, is(now().toString()));
    }

    private JsonEnvelope getJsonEnvelope(final String event, final Map<String, String> values) {
        final JsonObjectBuilder builder = createObjectBuilder();

        values.forEach(builder::add);

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(event)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, builder.build());
    }
}
