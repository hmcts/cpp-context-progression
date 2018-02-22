package uk.gov.moj.cpp.progression.helper;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;

public class PreAndPostConditionHelper {

    public static String addDefendant(String caseId) {
        String request = null;
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            request = addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();
            addDefendantHelper.verifyMinimalDefendantAdded();
        }
        return request;
    }

    public static void addDefendant(String caseId, String defendantId) {
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addFullDefendant(defendantId, UUID.randomUUID().toString().substring(0, 11));
        }
    }

    public static Response addCaseToCrownCourt(String caseId) throws IOException {
        return addCaseToCrownCourt(caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static Response addCaseToCrownCourt(String caseId, String firstDefendantId, String secondDefendantId) throws IOException {
        return postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.add-case-to-crown-court+json",
                getAddCaseToCrownCourtJsonBody(caseId, firstDefendantId, secondDefendantId));
    }

    public static Response getDefendants(String caseId) throws IOException {
        return getCommand(getQueryUri("/cases/" + caseId + "/defendants"), "application/vnd.progression.query.defendants+json");
    }

    public static Response getMagistrateCourts() throws IOException {
        return getCommand(getQueryUri("/crown-court/LCC/magistrate-courts"),
                "application/vnd.progression.query.crown-court.magistrate-courts+json");
    }

    public static Response getCaseProgression(final String uri, final String mediaType) throws IOException {
        return getCommand(uri, mediaType);
    }



    private static String getAddCaseToCrownCourtJsonBody(final String caseId, String firstDefendantId, String secondDefendantId) throws IOException {
        return Resources.toString(Resources.getResource("progression.command.add-case-to-crown-court.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", firstDefendantId)
                .replace("DEF_ID_2", secondDefendantId)
                .replace("TODAY", LocalDate.now().toString());
    }



    // Progression Test DSL for preconditions and assertions
    public static void givenCaseAddedToCrownCourt(String caseId, String firstDefendantId, String secondDefendantId) throws IOException {
        Response writeResponse = addCaseToCrownCourt(caseId, firstDefendantId, secondDefendantId);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static void givenCaseAddedToCrownCourt(String caseId) throws IOException {
        givenCaseAddedToCrownCourt(caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static void givenCaseProgressionDetail(String caseId) {
        pollForResponse(join("", "/cases/", caseId), "application/vnd.progression.query.caseprogressiondetail+json");
    }

    public static String getCaseProgressionFor(String caseId) {
        return pollForResponse(join("", "/cases/", caseId), "application/vnd.progression.query.caseprogressiondetail+json");
    }
}
