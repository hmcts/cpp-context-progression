package uk.gov.moj.cpp.progression.nows;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class NotificationRouterTest {

    private String destination;
    private String replyToAddress;
    private String channelType;
    private Map<String, String> properties;
    private NotificationDocumentState nowsNotificationDocumentState;

    @InjectMocks
    private NotificationRouter target;

    @Mock
    private EmailNowNotificationChannel emailNowNotificationChannel;

    @Mock
    private Sender sender;

    @Mock
    JsonEnvelope event;

    @Captor
    private ArgumentCaptor<Sender> senderCapture;

    @Captor
    private ArgumentCaptor<JsonEnvelope> eventCapture;

    @Captor
    private ArgumentCaptor<String> destinationCapture;

    @Captor
    private ArgumentCaptor<Map<String, String>> propertiesCapture;

    @Captor
    private ArgumentCaptor<NotificationDocumentState> nowsNotificationDocumentStateArgumentCaptor;

    @Before
    public void setupGreenPath() {
        destination = "a@test.com";
        replyToAddress = "b@test.com";
        channelType = EmailNowNotificationChannel.EMAIL_TYPE;
        properties = new HashMap<>();
        nowsNotificationDocumentState = NotificationDocumentState.notificationDocumentState().build();
    }

    @Test
    public void testNotifyEmail() throws InvalidNotificationException {
        target.notify(sender, event, destination, channelType, properties, nowsNotificationDocumentState);
        verify(emailNowNotificationChannel, times(1))
                .notify(senderCapture.capture(), eventCapture.capture(), destinationCapture.capture(), propertiesCapture.capture(),
                        nowsNotificationDocumentStateArgumentCaptor.capture());
        assertThat(senderCapture.getValue(), is(sender));
        assertThat(eventCapture.getValue(), is(event));
        assertThat(destinationCapture.getValue(), is(destination));
        assertThat(propertiesCapture.getValue(), is(properties));
        assertThat(nowsNotificationDocumentStateArgumentCaptor.getValue(), is(nowsNotificationDocumentState));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotifyUnknownChannel() throws InvalidNotificationException {
        channelType = "pigeon";
        target.notify(sender, event, destination, channelType, properties, nowsNotificationDocumentState);
    }


}
