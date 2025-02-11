package uk.gov.moj.cpp.progression;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class UpdateCivilFeesIT extends AbstractIT {
    private String caseId = randomUUID().toString();

    private final JmsMessageConsumerClient consumerForCivilFeesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.civil-fees-response").getMessageConsumerClient();

    @Test
    public void shouldUpdateCivilFees() throws IOException {
        //Given
        String body = getPayload("stub-data/update-civil-fees.json");

        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/cases/update-civil-fees", caseId)),
                "application/vnd.progression.update-civil-fees+json",
                body);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        verifyInMessageIsPresentInPublicEvent();
    }

    private void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCivilFeesUpdated);
        assertTrue(message.isPresent());
    }
}
