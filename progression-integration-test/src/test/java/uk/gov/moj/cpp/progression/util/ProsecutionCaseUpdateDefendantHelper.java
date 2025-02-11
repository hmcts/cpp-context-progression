package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;


public class ProsecutionCaseUpdateDefendantHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-defendant-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_DEFENDANT_PAYLOAD = "progression.update-defendant-for-prosecution-case.json";
    private static final String TEMPLATE_UPDATE_DEFENDANT_WITH_CUSTODY_ESTABLISHMENT_PAYLOAD = "progression.update-defendant-with-custody-establishment-for-prosecution-case.json";
    private static final String TEMPLATE_UPDATE_DEFENDANT_WITH_EMPTY_CUSTODY_ESTABLISHMENT_PAYLOAD = "progression.update-defendant-with-empty-custody-establishment-for-prosecution-case.json";
    private static final String TEMPLATE_UPDATE_YOUTH_FLAG_PAYLOAD = "progression.update-youth-flag-for-defendant.json";

    private String request;

    private final String defendantId;

    private final String caseId;

    public ProsecutionCaseUpdateDefendantHelper(final String caseId, final String defendantId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
    }

    public void updateDefendantWithCustody() throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PAYLOAD);
        updateDefendantWithCustody(jsonString);
    }

    public void updateDefendantWithCustodyEstablishmentInfo(final String caseId, final String defendantId, final String masterDefendantId) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_WITH_CUSTODY_ESTABLISHMENT_PAYLOAD)
                .replaceAll("%DEFENDANT_ID%", defendantId)
                .replaceAll("%MASTER_DEFENDANT_ID%", masterDefendantId)
                .replaceAll("%PROSECUTION_CASE_ID%", caseId);
        updateDefendantWithCustodyEstablishment(caseId, defendantId, jsonString);
    }

    public void updateDefendantWithEmptyCustodyEstablishmentInfo(final String caseId, final String defendantId, final String masterDefendantId) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_WITH_EMPTY_CUSTODY_ESTABLISHMENT_PAYLOAD)
                .replaceAll("%DEFENDANT_ID%", defendantId)
                .replaceAll("%MASTER_DEFENDANT_ID%", masterDefendantId)
                .replaceAll("%PROSECUTION_CASE_ID%", caseId);
        updateDefendantWithEmptyCustodyEstablishment(caseId, defendantId, jsonString);
    }

    public void updateDefendantWithHearingLanguageNeeds(final String hearingLanguage) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("personDetails").put("hearingLanguageNeeds", hearingLanguage);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateDefendantWithPoliceBailInfo(final String policeBailStatusId, final String policeBailStatusDesc, final String policeBailConditions) throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("policeBailStatus").put("id", policeBailStatusId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("policeBailStatus").put("description", policeBailStatusDesc);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").put("policeBailConditions", policeBailConditions);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("bailStatus").put("id", policeBailStatusId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("bailStatus").put("description", policeBailStatusDesc);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").put("bailConditions", policeBailConditions);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateDefendant(final String jsonString) throws JSONException{
        updateDefendant(jsonString, Response.Status.ACCEPTED.getStatusCode());
    }

    public void updateDefendant(final String jsonString, final int statusCode) throws JSONException {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request, statusCode);
    }

    public void updateDefendantWithCustody(final String jsonString) throws JSONException {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("custodialEstablishment").put("name", "HMP Croydon Category A");
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("custodialEstablishment").put("id", UUID.randomUUID());

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateDefendantWithCustodyEstablishment(final String caseId, final String defendantId, final String jsonString) throws JSONException {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("custodialEstablishment").put("name", "HMP Croydon Category A");
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("custodialEstablishment").put("id", UUID.randomUUID());

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateDefendantWithEmptyCustodyEstablishment(final String caseId, final String defendantId, final String jsonString) throws JSONException {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateYouthFlagForDefendant() throws JSONException {
        final String jsonString = getPayload(TEMPLATE_UPDATE_YOUTH_FLAG_PAYLOAD);
        updateDefendant(jsonString);
    }

    public void updateDateOfBirthForDefendant(final String prosecutionCaseId, final String defendantId, final LocalDate newDateOfBirth) throws JSONException {
        final JSONObject jsonObjectPayload = new JSONObject(getPayload("progression.update-date-of-birth-for-defendant.json"));
        jsonObjectPayload.getJSONObject("defendant").getJSONObject("personDefendant").getJSONObject("personDetails").put("dateOfBirth", newDateOfBirth.toString());
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", prosecutionCaseId);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInMessagingQueueForDefendantChanged(final JmsMessageConsumerClient publicEventsCaseDefendantChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsCaseDefendantChanged);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendant.prosecutionCaseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
    }

}
