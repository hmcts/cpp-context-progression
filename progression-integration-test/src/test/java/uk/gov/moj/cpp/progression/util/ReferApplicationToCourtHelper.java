package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.util.Optional;

import javax.json.JsonObject;

public class ReferApplicationToCourtHelper extends AbstractTestHelper {

    public static void verifyPublicEventForHearingExtended(final String hearingId, final JmsMessageConsumerClient publicEventsConsumerForHearingExtended) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsConsumerForHearingExtended);
        assertTrue(message.isPresent());
        final JsonObject publicHearingExtendedEvent = message.get();
        assertThat(publicHearingExtendedEvent.getString("hearingId"), is(hearingId));
        assertThat(publicHearingExtendedEvent.containsKey("prosecutionCases"), is(false));
    }

}
