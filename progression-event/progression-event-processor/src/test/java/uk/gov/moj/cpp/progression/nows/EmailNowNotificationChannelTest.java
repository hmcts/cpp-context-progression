package uk.gov.moj.cpp.progression.nows;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EmailNowNotificationChannelTest {

    @Mock
    private JsonEnvelope event;

    @Mock
    private Sender sender;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Enveloper enveloper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestEnvelopes;

    @Captor
    private ArgumentCaptor<Object> requestPayloads;

    @InjectMocks
    private EmailNowNotificationChannel emailNowNotificationChannel;

    @Test
    public void testEmailNotify() {

        final JsonObject jsonObject = Mockito.mock(JsonObject.class);

        final JsonEnvelope jsonEnvelope = Mockito.mock(JsonEnvelope.class);

        when(objectToJsonObjectConverter.convert(requestPayloads.capture())).thenReturn(jsonObject);

        when(enveloper.withMetadataFrom(event, "notificationnotify.send-email-notification")).thenReturn(e -> jsonEnvelope);

        final Notification notification = new Notification();

        emailNowNotificationChannel.notify(sender, event, notification);

        verify(sender, times(1)).sendAsAdmin(requestEnvelopes.capture());
    }
}