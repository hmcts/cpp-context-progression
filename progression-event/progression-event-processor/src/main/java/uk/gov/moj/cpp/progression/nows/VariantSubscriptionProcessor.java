package uk.gov.moj.cpp.progression.nows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.notification.Subscription;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VariantSubscriptionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantSubscriptionProcessor.class);

    private final SubscriptionClient subscriptionClient;

    private final NotificationRouter notificationRouter;

    @Inject
    public VariantSubscriptionProcessor(final SubscriptionClient subscriptionClient, final NotificationRouter notificationRouter) {
        this.subscriptionClient = subscriptionClient;
        this.notificationRouter = notificationRouter;
    }

    public void notifyVariantCreated(Sender sender, JsonEnvelope context, NotificationDocumentState nowsNotificationDocumentState) {

        final List<Subscription> subscriptions = subscriptionClient.getAll(context, nowsNotificationDocumentState.getNowsTypeId(), LocalDate.now()).getSubscriptions();

        if(subscriptions.isEmpty()) {
            LOGGER.error("No subscription retrieved for nowTypeId - {}", nowsNotificationDocumentState.getNowsTypeId());
        } else {
            LOGGER.info("Displaying subscription for nowTypeId - {}", nowsNotificationDocumentState.getNowsTypeId());
            subscriptions.forEach(subscription -> LOGGER.info(subscription.toString()));
        }

        subscriptions.stream()
                .filter(subscription -> userGroupMatch(subscription, nowsNotificationDocumentState.getUsergroups()))
                .filter(subscription -> courtCentreMatch(subscription, nowsNotificationDocumentState.getOriginatingCourtCentreId()))
                .forEach(subscription -> notify(sender, context, nowsNotificationDocumentState, subscription));
    }

    private void notify(Sender sender, JsonEnvelope context, NotificationDocumentState nowsNotificationDocumentState, Subscription subscription) {
        try {
            notificationRouter.notify(sender, context, subscription.getDestination(), subscription.getChannel(),
                    subscription.getChannelProperties(), nowsNotificationDocumentState);
        } catch (InvalidNotificationException ex) {
            LOGGER.error("failed to send notification to " + subscription.getDestination(), ex);
        }
    }

    private boolean userGroupMatch(final Subscription subscription, List<String> usergroups) {
        if (subscription.getUserGroups() == null || subscription.getUserGroups().isEmpty()) {
            return true;
        } else {
            return usergroups.stream().anyMatch(ug -> subscription.getUserGroups().contains(ug));
        }
    }

    private boolean courtCentreMatch(final Subscription subscription, final UUID courtCentreId) {
        if (subscription.getCourtCentreIds() == null || subscription.getCourtCentreIds().isEmpty()) {
            return true;
        } else {
            return subscription.getCourtCentreIds().contains(courtCentreId);
        }
    }

}
