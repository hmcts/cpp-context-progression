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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class EditCaseNoteIT extends AbstractIT {

    private static final JmsMessageConsumerClient consumerForCaseNoteEdited = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-note-edited").getMessageConsumerClient();
    private final String caseId = randomUUID().toString();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

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
        assertThatRequestIsAccepted(writeResponse);
        verifyInMessageIsPresentInPublicEvent();
        verifyCaseNotesAndGetCaseNoteId(caseId, true);

        String bodyForFalse = "{\"isPinned\": false }";
        final Response writeResponseForFalse = postCommand(
                getWriteUrl(format("/cases/%s/notes/%s", caseId, caseNotesId)),
                "application/vnd.progression.edit-case-note+json",
                bodyForFalse);

        //Then
        assertThatRequestIsAccepted(writeResponseForFalse);
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

    private static void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCaseNoteEdited);
        assertTrue(message.isPresent());
    }



}
