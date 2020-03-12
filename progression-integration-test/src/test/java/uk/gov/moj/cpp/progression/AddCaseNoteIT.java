package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Test;

public class AddCaseNoteIT extends AbstractIT {

    private String caseId = randomUUID().toString();

    private static final MessageConsumer consumerForCaseNoteAdded = publicEvents.createConsumer("public.progression.case-note-added");

    @Test
    public void shouldAddCaseNote() throws IOException {
        //Given
        String body = "{\"note\": \"test note\"}";
        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/cases/%s/notes", caseId)),
                "application/vnd.progression.add-case-note+json",
                body);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        verifyInMessageIsPresentInPublicEvent();
        verifyGetCaseNotes(caseId);
    }

    private static void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForCaseNoteAdded);
        assertTrue(message.isPresent());
    }

    private static void verifyGetCaseNotes(final String caseId) {
        poll(requestParams(getReadUrl(format("/cases/%s/notes", caseId)),
                "application/vnd.progression.query.case-notes+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseNotes[0].author.firstName", equalTo("testy")),
                                withJsonPath("$.caseNotes[0].author.lastName", equalTo("testx")),
                                withJsonPath("$.caseNotes[0].note", equalTo("test note"))
                        ))).getPayload();
    }


}
