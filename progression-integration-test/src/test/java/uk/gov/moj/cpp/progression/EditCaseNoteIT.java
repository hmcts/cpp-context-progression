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

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Test;

public class EditCaseNoteIT extends AbstractIT {

    private static final MessageConsumer consumerForCaseNoteEdited = publicEvents.createPublicConsumer("public.progression.case-note-edited");
    private final String caseId = randomUUID().toString();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForCaseNoteEdited);
        assertTrue(message.isPresent());
    }

    @Test
    public void shouldEditCaseNote() throws Exception {

        //Given
        String caseNotesId = addCaseNotesAndVerify();
        String body = "{\"isPinned\": true }";
        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/cases/%s/notes/%s", caseId, caseNotesId)),
                "application/vnd.progression.edit-case-note+json",
                body);

        //Then
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyInMessageIsPresentInPublicEvent();
        verifyCaseNotesAndGetCaseNoteId(caseId, true);

        String bodyForFalse = "{\"isPinned\": false }";
        final Response writeResponseForFalse = postCommand(
                getWriteUrl(format("/cases/%s/notes/%s", caseId, caseNotesId)),
                "application/vnd.progression.edit-case-note+json",
                bodyForFalse);

        //Then
        assertThat(writeResponseForFalse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyInMessageIsPresentInPublicEvent();
        verifyCaseNotesAndGetCaseNoteId(caseId, false);



    }

    private String verifyCaseNotesAndGetCaseNoteId(final String caseId, final boolean isPinned) {
        String payload = poll(requestParams(getReadUrl(format("/cases/%s/notes", caseId)),
                "application/vnd.progression.query.case-notes+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseNotes[0].author.firstName", equalTo("testy")),
                                withJsonPath("$.caseNotes[0].author.lastName", equalTo("testx")),
                                withJsonPath("$.caseNotes[0].note", equalTo("test note")),
                                withJsonPath("$.caseNotes[0].isPinned", equalTo(isPinned))
                        ))).getPayload();

        JsonObject json = stringToJsonObjectConverter.convert(payload);
        return json.getJsonArray("caseNotes").getJsonObject(0).getString("id");
    }


    private String addCaseNotesAndVerify() throws Exception {
        String body = "{\"note\": \"test note\"}";
        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/cases/%s/notes", caseId)),
                "application/vnd.progression.add-case-note+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        return verifyCaseNotesAndGetCaseNoteId(caseId, false);
    }

}
