package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MultipartFileUploadHelper extends AbstractTestHelper{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateOffencesHelper.class);

    private final MessageConsumer publicEventsConsumerForCourtDocUploaded =
            QueueUtil.publicEvents.createConsumer("public.progression.events.court-document-uploaded");

    public void verifyInMessagingQueueForCourtDocUploaded(final UUID materialId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForCourtDocUploaded);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(allOf(withJsonPath("$.materialId", Matchers.hasToString(
                        Matchers.containsString(materialId.toString())))
        )));
    }
}
