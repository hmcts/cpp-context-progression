package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class ApplicationNoteIT extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_APPLICATION_NOTE_EDITED = "public.progression.application-note-edited";
    private static final JmsMessageConsumerClient consumerForApplicationNoteEdited = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROGRESSION_APPLICATION_NOTE_EDITED).getMessageConsumerClient();
    private final String VALUE_APPLICATION_ID = randomUUID().toString();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static void verifyInMessageIsPresentInPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForApplicationNoteEdited);
        assertTrue(message.isPresent());
    }

    @Test
    public void shouldEditApplicationNote() throws Exception {
        //Given
        String applicationNotesId = addApplicationNotesAndVerify();
        String body = "{\"isPinned\": true }";

        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/applications/%s/notes/%s", VALUE_APPLICATION_ID, applicationNotesId)),
                "application/vnd.progression.command.edit-application-note+json",
                body);

        //Then
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyInMessageIsPresentInPublicEvent();
        verifyApplicationNotesAndGetApplicationNoteId(VALUE_APPLICATION_ID, TRUE);

        String bodyForFalse = "{\"isPinned\": false }";
        final Response writeResponseForFalse = postCommand(
                getWriteUrl(format("/applications/%s/notes/%s", VALUE_APPLICATION_ID, applicationNotesId)),
                "application/vnd.progression.command.edit-application-note+json",
                bodyForFalse);

        //Then
        assertThat(writeResponseForFalse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyInMessageIsPresentInPublicEvent();
        verifyApplicationNotesAndGetApplicationNoteId(VALUE_APPLICATION_ID, FALSE);


    }

    private String verifyApplicationNotesAndGetApplicationNoteId(final String applicationId, final Boolean isPinned) {
        String payload = poll(requestParams(getReadUrl(format("/applications/%s/notes", applicationId)),
                "application/vnd.progression.query.application-notes+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(30, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationNotes[0].author.firstName", equalTo("testy")),
                                withJsonPath("$.applicationNotes[0].author.lastName", equalTo("testx")),
                                withJsonPath("$.applicationNotes[0].note", equalTo("test note")),
                                withJsonPath("$.applicationNotes[0].isPinned", equalTo(isPinned))
                        ))).getPayload();

        JsonObject json = stringToJsonObjectConverter.convert(payload);
        return json.getJsonArray("applicationNotes").getJsonObject(0).getString("id");
    }


    private String addApplicationNotesAndVerify() throws Exception {
        String body = "{\"note\": \"test note\"}";
        //When
        final Response writeResponse = postCommand(
                getWriteUrl(format("/applications/%s/notes", VALUE_APPLICATION_ID)),
                "application/vnd.progression.command.add-application-note+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        return verifyApplicationNotesAndGetApplicationNoteId(VALUE_APPLICATION_ID, FALSE);
    }
}
