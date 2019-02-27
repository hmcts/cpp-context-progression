package uk.gov.moj.cpp.progression.nows;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.any;
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
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.notification.Subscription;
import uk.gov.moj.cpp.progression.domain.notification.Subscriptions;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class VariantSubscriptionProcessorTest {

    @InjectMocks
    private VariantSubscriptionProcessor target;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private NotificationRouter notificationRouter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> subscriptionContextCaptor;

    @Captor
    private ArgumentCaptor<UUID> subscriptionNowsTypeIdCaptor;

    @Captor
    private ArgumentCaptor<Sender> notifySenderCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> notifyContextCaptor;

    @Captor
    private ArgumentCaptor<String> notifyDestinationCaptor;

    @Captor
    private ArgumentCaptor<String> notifyChannelTypeCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> notifyPropertiesCaptor;

    @Captor
    private ArgumentCaptor<NotificationDocumentState> notifyNowsNotificationDocumentStateCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> subscriptionAsOfDateCaptor;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope context;

    private NotificationDocumentState nowsNotificationDocumentState;

    @Before
    public void before() {
        this.nowsNotificationDocumentState = NotificationDocumentState.notificationDocumentState()

                .withNowsTypeId(randomUUID())
                .withOriginatingCourtCentreId(randomUUID())
                .withUsergroups(asList("courtAdmin", "defenceCounsel"))
                .build();
    }

    private Subscription createUnrestrictedSubscription() {
        Subscription subscription1 = new Subscription();
        subscription1.setChannel(EmailNowNotificationChannel.EMAIL_TYPE);
        Map<String, String> channelProperties = new HashMap<>();
        final UUID templateId = randomUUID();
        channelProperties.put(EmailNowNotificationChannel.TEMPLATE_ID_PROPERTY_NAME, templateId.toString());
        channelProperties.put(EmailNowNotificationChannel.FROM_ADDRESS_PROPERTY_NAME, "noreplay@courts.uk.com");
        subscription1.setChannelProperties(channelProperties);
        return subscription1;
    }

    @Test
    public void testNotifyVariantCreatedUserGroupMatchFails() throws InvalidNotificationException {
        Subscription subscription = createUnrestrictedSubscription();
        subscription.setUserGroups(asList("groupA", "groupB"));
        testNotifyVariantCreated(subscription, 0);
    }

    @Test
    public void testNotifyVariantCreatedUserGroupMatchSucceeds() throws InvalidNotificationException {
        Subscription subscription = createUnrestrictedSubscription();
        subscription.setUserGroups(asList(nowsNotificationDocumentState.getUsergroups().get(0), "groupd"));
        testNotifyVariantCreated(subscription, 1);
    }


    @Test
    public void testNotifyVariantCreatedCourtCentreMatchFails() throws InvalidNotificationException {
        Subscription subscription = createUnrestrictedSubscription();
        subscription.setCourtCentreIds(asList(randomUUID()));
        testNotifyVariantCreated(subscription, 0);
    }

    @Test
    public void testNotifyVariantCreatedCourtCentreMatchSucceeds() throws InvalidNotificationException {
        Subscription subscription = createUnrestrictedSubscription();
        subscription.setCourtCentreIds(asList(nowsNotificationDocumentState.getOriginatingCourtCentreId(), randomUUID()));
        testNotifyVariantCreated(subscription, 1);
    }

    @Test
    public void testNotifyVariantCreatedUnrestrictedSubscription() throws InvalidNotificationException {
        testNotifyVariantCreated(createUnrestrictedSubscription(), 1);
    }

    private void testNotifyVariantCreated(Subscription subscription1, int expectedNotificationCount) throws InvalidNotificationException {
        final Subscriptions allSubscriptions = new Subscriptions();

        allSubscriptions.setSubscriptions(asList(subscription1));
        when(subscriptionClient.getAll(any(JsonEnvelope.class), any(UUID.class), any(LocalDate.class))).thenReturn(allSubscriptions);

        target.notifyVariantCreated(sender, context, nowsNotificationDocumentState);

        verify(subscriptionClient, times(1)).getAll(subscriptionContextCaptor.capture(), subscriptionNowsTypeIdCaptor.capture(), subscriptionAsOfDateCaptor.capture());
        assertThat(subscriptionContextCaptor.getValue(), is(context));
        assertThat(subscriptionNowsTypeIdCaptor.getValue(), is(nowsNotificationDocumentState.getNowsTypeId()));
        assertThat(subscriptionAsOfDateCaptor.getValue().until(LocalDate.now()).get(ChronoUnit.DAYS), lessThan(1l));

        verify(notificationRouter, times(expectedNotificationCount)).notify(notifySenderCaptor.capture(), notifyContextCaptor.capture(), notifyDestinationCaptor.capture(),
                notifyChannelTypeCaptor.capture(), notifyPropertiesCaptor.capture(), notifyNowsNotificationDocumentStateCaptor.capture());
        if (expectedNotificationCount > 0) {
            assertThat(notifySenderCaptor.getValue(), is(sender));
            assertThat(notifyContextCaptor.getValue(), is(context));
            assertThat(notifyDestinationCaptor.getValue(), is(subscription1.getDestination()));
            assertThat(notifyChannelTypeCaptor.getValue(), is(subscription1.getChannel()));
            assertThat(notifyPropertiesCaptor.getValue(), is(subscription1.getChannelProperties()));
            assertThat(notifyNowsNotificationDocumentStateCaptor.getValue(), is(nowsNotificationDocumentState));
        }

    }

}
