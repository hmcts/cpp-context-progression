package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getDefendantForDefendantId;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.command.update-defendant+json";

    private static final String TEMPLATE_UPDATE_DEFENDANT_PERSON_PAYLOAD = "raml/json/progression.command.update-defendant.json";
    private static final String TEMPLATE_PDATE_DEFENDANT_BAIL_STATUS_PAYLOAD = "raml/json/progression.command.update-defendant-bail-status.json";
    private static final String TEMPLATE_EMPTY_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-empty-defendant.json";
    private static final MessageConsumer publicEventCompleteSendingSheetInvalidatedConsumer = QueueUtil.publicEvents.createConsumer(
            "public.progression.events.sending-sheet-previously-completed");
    private final MessageConsumer publicEventsConsumerForDefendantUpdated =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.events.defendant-updated");
    private final String defendantId;
    private final String caseId;
    private final String personId;
    private final String documentId = randomUUID().toString();
    private String request;

    public UpdateDefendantHelper(final String caseId, final String defendantId, final String personId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.personId = personId;

        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_UPDATED);
    }

    public void updateDefendantPerson() {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PERSON_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("person").put("id", personId);
        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateDefendantBailStatus() {
        final String jsonString = getPayload(TEMPLATE_PDATE_DEFENDANT_BAIL_STATUS_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.put("documentId", documentId);
        jsonObjectPayload.put("bailStatus", "unconditional");
        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }


    public void updateDefendantBailStatus(final String documentId) {
        final String jsonString = getPayload(TEMPLATE_PDATE_DEFENDANT_BAIL_STATUS_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.put("documentId", documentId);
        jsonObjectPayload.put("bailStatus", "unconditional");
        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }


    public void verifyDefendantPersonUpdated() {

        poll(getDefendantForDefendantId(caseId, defendantId))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload()
                                .isJson(
                                        allOf(
                                                withJsonPath("$.defendantId", is(defendantId)),
                                                withJsonPath("$.person.firstName", is("Davi"))
                                        )
                                ));

    }

    public void verifyDefendantBailStatusUpdated() {

        poll(getDefendantForDefendantId(caseId, defendantId))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload()
                                .isJson(
                                        allOf(
                                                withJsonPath("$.defendantId", is(defendantId)),
                                                withJsonPath("$.bailStatus",
                                                        is("unconditional"))
                                        )
                                ));

    }

    public void verifyInMessagingQueueForDefendentUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForDefendantUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseId)))));
    }

    public void verifyEmptyUpdateDefendantPayload() {
        final String jsonString = getPayload(TEMPLATE_EMPTY_DEFENDANT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request, Response.Status.BAD_REQUEST.getStatusCode());
    }

    public void verifySendingSheetPreviouslyCompletedPublicEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventCompleteSendingSheetInvalidatedConsumer);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseId)))));
    }

}
