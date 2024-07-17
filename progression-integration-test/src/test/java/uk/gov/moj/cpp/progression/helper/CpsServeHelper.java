package uk.gov.moj.cpp.progression.helper;

import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;

public class CpsServeHelper extends AbstractTestHelper {

    public CpsServeHelper() {
        this.privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumerForMultipleSelectors(
                "progression.event.cotr-created","progression.event.prosecution-cotr-served","progression.event.prosecution-cotr-updated");

        this.publicEventsConsumer = QueueUtil.publicEvents.createPublicConsumerForMultipleSelectors(
                "public.progression.cotr-created","public.progression.prosecution-cotr-served", "public.progression.cotr-operation-failed", "public.progression.cotr-updated");
    }

    public JsonEnvelope getPrivateEvents() {
        return retrieveMessageAsEnvelope(privateEventsConsumer);
    }

    public JsonEnvelope getPublicEvents() {
        return retrieveMessageAsEnvelope(publicEventsConsumer);
    }
}
