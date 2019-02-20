package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class ProgressionIT {

    private String caseId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        createMockEndpoints();

    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        addDefendant(caseId);
        Response writeResponse = addCaseToCrownCourt(caseId);
        assertThatRequestIsAccepted(writeResponse);

        pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.sending-committal-hearing-information+json",
                getJsonBodyStr("progression.command.sending-committal-hearing-information.json"));
        assertThatRequestIsAccepted(writeResponse);

        final String queryResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        JsonObject defendantsJsonObject = getJsonObject(queryResponse);
        assertTrue(defendantsJsonObject.getString("sendingCommittalDate")
                .equals(LocalDate.now().toString()));

        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyStr("progression.command.sentence-hearing-date.json"));
        assertThatRequestIsAccepted(writeResponse);


        final String queryDefendantsResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryDefendantsResponse);
        assertTrue(defendantsJsonObject.getString("sentenceHearingDate")
                .equals(LocalDate.now().toString()));



        final String queryDefendantsReviewResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryDefendantsReviewResponse);

        assertTrue(defendantsJsonObject.getString("status")
                .equals("INCOMPLETE"));



    }


    private String getJsonBodyStr(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", UUID.randomUUID().toString())
                .replace("DEF_ID_2", UUID.randomUUID().toString())
                .replace("TODAY", LocalDate.now().toString());
    }
}
