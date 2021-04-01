package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

public class ReferApplicationToCourtHelper extends AbstractTestHelper {

    private static final MessageConsumer publicEventsConsumerForHearingExtended =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.events.hearing-extended");

    private static final MessageConsumer applicationReferralToExistingHearingMessageConsumer =
            QueueUtil.privateEvents.createConsumer(
                    "progression.event.application-referral-to-existing-hearing");

    private static final MessageConsumer hearingApplicationLinkCreated =
            QueueUtil.privateEvents.createConsumer(
                    "progression.event.hearing-application-link-created");

    public ReferApplicationToCourtHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("listing.command.list-court-hearing");
    }

    public static void verifyHearingInMessagingQueueForReferToCourt() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(applicationReferralToExistingHearingMessageConsumer);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("courtHearing"), is(notNullValue()));
    }

    public static void verifyHearingApplicationLinkCreated(final String hearingId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(hearingApplicationLinkCreated);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("hearing").getString("id"), is(hearingId));
        assertThat(message.get().getJsonObject("hearing").containsKey("prosecutionCases"), is(false));
    }

    public static void verifyPublicEventForHearingExtended(final String hearingId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForHearingExtended);
        assertTrue(message.isPresent());
        final JsonObject publicHearingExtendedEvent = message.get();
        assertThat(publicHearingExtendedEvent.getString("hearingId"), is(hearingId));
        assertThat(publicHearingExtendedEvent.containsKey("prosecutionCases"), is(false));
    }

}
