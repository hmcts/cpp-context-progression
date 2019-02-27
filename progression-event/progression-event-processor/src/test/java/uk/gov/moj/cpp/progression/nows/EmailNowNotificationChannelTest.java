package uk.gov.moj.cpp.progression.nows;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

//TODO add failure paths
@RunWith(MockitoJUnitRunner.class)
public class EmailNowNotificationChannelTest {

    private String destination;
    private String replyToAddress;
    private Map<String, String> properties;
    private NotificationDocumentState nowsNotificationDocumentState;
    private UUID templateId = UUID.randomUUID();

    @InjectMocks
    private EmailNowNotificationChannel target;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Sender sender;

    @Mock
    JsonEnvelope event;

    @Captor
    private ArgumentCaptor<Object> requestPayloads;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestEnvelopes;

    @Before
    public void setupGreenPath() {
        destination = "a@test.com";
        replyToAddress = "b@test.com";
        properties = new HashMap<>();
        properties.put(EmailNowNotificationChannel.TEMPLATE_ID_PROPERTY_NAME, templateId.toString());
        properties.put(EmailNowNotificationChannel.FROM_ADDRESS_PROPERTY_NAME, replyToAddress);
        nowsNotificationDocumentState = NotificationDocumentState.notificationDocumentState()
                        .withDefendantName("David Bowie")
                         .withCourtClerkName("Iggy Pop")
                        .withCaseUrns(Arrays.asList("C123", "C124"))
                         .withPriority(null)
                         .withOrderName(null)
                .withCourtCentreName(null)
                         .withMaterialId(UUID.randomUUID())
                .build();
    }


    @Test
    public void testNotify() throws InvalidNotificationException {
        JsonObject jsonObject = Mockito.mock(JsonObject.class);
        JsonEnvelope jsonEnvelope = Mockito.mock(JsonEnvelope.class);
        when(objectToJsonObjectConverter.convert(requestPayloads.capture())).thenReturn(jsonObject);
        when(enveloper.withMetadataFrom(event, EmailNowNotificationChannel.NOTIFICATIONNOTIFY_EMAIL_METADATA_TYPE)).thenReturn(
                o -> jsonEnvelope
        );
        target.notify(sender, event, destination, properties, nowsNotificationDocumentState);
        assertThat(requestPayloads.getAllValues().size(), is(1));
        verify(sender, times(1)).sendAsAdmin(requestEnvelopes.capture());
        assertThat(requestEnvelopes.getValue(), is(jsonEnvelope));
        assertThat("", requestPayloads.getValue().getClass() == Notification.class);
        Notification payloadSent = (Notification) requestPayloads.getValue();
        assertThat(payloadSent.getSendToAddress(), is(destination));
        assertThat(payloadSent.getReplyToAddress(), is(replyToAddress));
        final String expectedCaseUrns = nowsNotificationDocumentState.getCaseUrns().stream().collect(Collectors.joining(","));
        assertThat(payloadSent.getPersonalisation().get(EmailNowNotificationChannel.CASE_URNS_PERSONALISATION_KEY), is(expectedCaseUrns));
        assertThat(payloadSent.getPersonalisation().get(EmailNowNotificationChannel.COURT_CLERK_NAME_PERSONALISATION_KEY), is(nowsNotificationDocumentState.getCourtClerkName()));
    }
}
