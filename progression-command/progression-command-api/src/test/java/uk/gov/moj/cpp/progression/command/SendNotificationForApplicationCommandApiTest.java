package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.SendNotificationForApplication;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.function.Function;

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
public class SendNotificationForApplicationCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Function<Object, JsonEnvelope> function;
    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private SendNotificationForApplicationApi sendNotificationForApplicationApi;

    @Test
    public void shouldInitialCourtProceedingsForCourtApplication() {

        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.send-notification-for-application"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        sendNotificationForApplicationApi.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

}
