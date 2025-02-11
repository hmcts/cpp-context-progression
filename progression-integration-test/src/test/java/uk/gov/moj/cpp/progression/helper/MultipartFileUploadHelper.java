package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;


public class MultipartFileUploadHelper extends AbstractTestHelper {

    private final JmsMessageConsumerClient publicEventsConsumerForCourtDocUploaded = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.court-document-uploaded").getMessageConsumerClient();

    public void verifyInMessagingQueueForCourtDocUploaded(final UUID materialId) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsConsumerForCourtDocUploaded);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(allOf(withJsonPath("$.materialId", Matchers.hasToString(
                Matchers.containsString(materialId.toString())))
        )));
    }
}
