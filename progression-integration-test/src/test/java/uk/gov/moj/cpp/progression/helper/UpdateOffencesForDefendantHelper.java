package uk.gov.moj.cpp.progression.helper;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.UUID;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateOffencesForDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOffencesForDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.command.update-offences-for-defendant+json";

    private static final String TEMPLATE_ADD_OFFENCE_FOR_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-offences-for-defendant.json";
    private static final String TEMPLATE_ADD_MULTIPLE_OFFENCE_FOR_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-offences-for-defendant-multiple-offence.json";

    private static final String OFFENCE_CODE = "PS123FG";


    private final MessageConsumer publicEventsConsumerForOffencesForDefendantUpdated =
            QueueUtil.publicEvents.createPublicConsumer(
                    "public.progression.events.offences-for-defendant-updated");
    private final String defendantId;
    private final String caseId;
    private String request;
    private String offenceId = UUID.randomUUID().toString();

    public UpdateOffencesForDefendantHelper(final String caseId, final String defendantId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED);
    }

    public void updateOffencesForDefendant() {
        updateOffencesForDefendant("EWAY", offenceId);
    }

    public void updateOffencesForDefendant(final String offenceId) {
        this.offenceId = offenceId;
        updateOffencesForDefendant("EWAY", offenceId);
    }

    public void updateOffencesForDefendant(final String modeOfTrial, final String offenceId) {
        final String jsonString = getPayload(TEMPLATE_ADD_OFFENCE_FOR_DEFENDANT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);

        populateOffence(jsonObjectPayload, 0, defendantId, offenceId, "add offence to defendant test", 1, 1);
        request = jsonObjectPayload.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateMultipleOffencesForDefendant() {
        final String jsonString = getPayload(TEMPLATE_ADD_MULTIPLE_OFFENCE_FOR_DEFENDANT_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        populateOffence(jsonObjectPayload, 0, defendantId, UUID.randomUUID().toString(), "3", 3, 1);

        populateOffence(jsonObjectPayload, 1, defendantId, UUID.randomUUID().toString(), "1", 1, 2);

        populateOffence(jsonObjectPayload, 2, defendantId, UUID.randomUUID().toString(), "2", 2, 3);

        request = jsonObjectPayload.toString();


        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    private static void populateOffence(final JSONObject jsonObjectPayload, final int index, final String defendantId, final String offenceId, final String wording, final int orderIndex, final int count) {
        final JSONObject jsonObject0 = jsonObjectPayload.getJSONArray("offences").getJSONObject(index);
        jsonObject0.put("defendantId", defendantId);
        jsonObject0.put("id", offenceId);
        jsonObject0.put("offenceCode", OFFENCE_CODE);
        jsonObject0.put("wording", wording);
        jsonObject0.put("indicatedPlea", getIndicatedPlea());
        jsonObject0.put("section", "Section 51");
        jsonObject0.put("startDate", "2010-08-01");
        jsonObject0.put("endDate", "2011-08-01");
        jsonObject0.put("orderIndex", orderIndex);
        jsonObject0.put("count", count);
    }


    private static JSONObject getIndicatedPlea() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", UUID.randomUUID().toString());
        jsonObject.put("value", "INDICATED_GUILTY");

        return jsonObject;
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

    public String getDefendantId() {
        return defendantId;
    }

}
