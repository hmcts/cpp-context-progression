package uk.gov.moj.cpp.progression.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
