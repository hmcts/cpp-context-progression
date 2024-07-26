package uk.gov.moj.cpp.progression;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.io.IOException;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Test;

public class UpdateCivilFeesIT extends AbstractIT {
    private String caseId = randomUUID().toString();

    private static final MessageConsumer consumerForCivilFeesUpdated = publicEvents.createPublicConsumer("public.progression.civil-fees-response");

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

    private static void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForCivilFeesUpdated);
        assertTrue(message.isPresent());
    }
}
