package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.time.LocalDate;
import java.util.Optional;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProsecutionCaseUpdateOffencesHelper extends AbstractTestHelper {

    public static final String OFFENCE_CODE = "TFL123";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateOffencesHelper.class);
    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-offences-for-prosecution-case+json";
    private static final String TEMPLATE_UPDATE_OFFENCES_PAYLOAD = "progression.update-offences-for-prosecution-case.json";
    private static final String TEMPLATE_UPDATE_SINGLE_DEFENDANT_OFFENCES_PAYLOAD = "progression.update-defendant-offences.json";
    private static final String TEMPLATE_UPDATE_OFFENCE_PLEA_PAYLOAD = "progression.update-hearing-offence-plea.json";
    private static final String TEMPLATE_UPDATE_OFFENCE_VERDICT_PAYLOAD = "progression.update-hearing-offence-verdict.json";
    private static final String WRITE_MEDIA_TYPE_PLEA = "application/vnd.progression.update-hearing-offence-plea+json";
    private static final String WRITE_MEDIA_TYPE_VERDICT = "application/vnd.progression.update-hearing-offence-verdict+json";
    private static final String WRITE_MEDIA_TYPE_UPDATE_DEFENDANT_OFFENCE = "application/vnd.progression.update-defendant-offences+json";
    private final String defendantId;
    private final String caseId;
    private final String offenceId;
    private String request;

    public ProsecutionCaseUpdateOffencesHelper(final String caseId, final String defendantId, final String offenceId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.offenceId = offenceId;
    }

    public void updateOffences() throws JSONException {
        updateOffences(this.offenceId, OFFENCE_CODE);
    }

    public void updateOffenceOfSingleDefendant() {
        updateOffenceOfSingleDefendant(this.offenceId, OFFENCE_CODE);
    }

    public void updateOffences(final String offenceId, final String offenceCode) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_OFFENCES_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("defendantId", defendantId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("id", offenceId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("offenceCode", offenceCode);

        request = jsonObjectPayload.toString();
        request = request.replace("REPORTING_RESTRICTION_ORDERED_DATE", LocalDate.now().plusDays(1).toString());
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateOffenceOfSingleDefendant(final String offenceId, final String offenceCode) {
        request = getPayload(TEMPLATE_UPDATE_SINGLE_DEFENDANT_OFFENCES_PAYLOAD)
                .replace("DEFENDANT_ID_1", defendantId)
                .replace("CASE_ID", caseId)
                .replace("OFFENCE_ID_1", offenceId)
                .replace("OFFENCE_ID_2", randomUUID().toString())
                .replace("OFFENCE_CODE_1", offenceCode)
                .replace("REPORTING_RESTRICTION_ORDERED_DATE", LocalDate.now().plusDays(1).toString());

        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants"), WRITE_MEDIA_TYPE_UPDATE_DEFENDANT_OFFENCE, request);
    }

    public void updateOffenceVerdict(final String hearingId, final String offenceId) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_OFFENCE_VERDICT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("verdict").put("offenceId", offenceId);
        jsonObjectPayload.put("hearingId", hearingId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/hearing/" + hearingId + "/verdict"), WRITE_MEDIA_TYPE_VERDICT, request);
    }

    public void updateOffencePlea(final String hearingId, final String offenceId) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_OFFENCE_PLEA_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("pleaModel").put("offenceId", offenceId);
        jsonObjectPayload.put("hearingId", hearingId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/hearing/" + hearingId + "/plea"), WRITE_MEDIA_TYPE_PLEA, request);
    }

    public void updateMultipleOffences(final String offenceId, final String secondOffenceId, final String offenceCode) throws JSONException {
        final String jsonString = getPayload("progression.update-multiple-offences-for-prosecution-case.json");
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("defendantId", defendantId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("id", offenceId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("offenceCode", offenceCode);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(1).put("id", secondOffenceId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(1).put("offenceCode", offenceCode);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }


    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ(final JmsMessageConsumerClient privateEventsConsumer) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyVerdictInActiveMQ(final JmsMessageConsumerClient consumerForDefendantListingStatusChanged, final Matcher<? super ReadContext>... matchers) {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessageAsJsonPath(consumerForDefendantListingStatusChanged, isJson(allOf(matchers)));
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyInMessagingQueueForOffencesUpdated(final JmsMessageConsumerClient publicEventsConsumerForOffencesUpdated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsConsumerForOffencesUpdated);
        assertTrue(message.isPresent());
    }


}
