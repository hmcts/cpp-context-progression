package uk.gov.moj.cpp.progression.it;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.join;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMagistrateCourts;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class ProgressionIT {

    private String caseId;
    private String caseProgressionId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        createMockEndpoints(caseId);
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        Response writeResponse = addCaseToCrownCourt(caseId, caseProgressionId);
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

        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.case-to-be-assigned+json",
                getJsonBodyStr("progression.command.case-to-be-assigned.json"));
        assertThatRequestIsAccepted(writeResponse);

        final String queryDefendantsReviewResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryDefendantsReviewResponse);

        assertTrue(defendantsJsonObject.getString("status")
                .equals("READY_FOR_REVIEW"));

        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.case-assigned-for-review+json",
                getJsonBodyStr("progression.command.case-assigned-for-review.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String queryDefendantsAssignedResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryDefendantsAssignedResponse);

        assertTrue(defendantsJsonObject.getString("status").equals("ASSIGNED_FOR_REVIEW"));
    }

    @Test
    public void shouldGetMagistrateCourtsForLCC() throws Exception {
        // given
        List<String> expectedMagistrateCourts = newArrayList("Liverpool", "Bootle", "Birkenhead", "Warrington");
        // and

        // when
        Response queryResponse = getMagistrateCourts();

        // then
        assertThat(queryResponse.getStatusCode(), equalTo(SC_OK));
        // and
        List<String> actualMagistrateCourts = Lists.newArrayList();
        queryResponse.getBody().<List<Map<String, String>>>path("values").forEach(value -> value.forEach((k, s) -> {
            assertThat(k, is("name"));
            actualMagistrateCourts.add(s);
        }));
        // and
        assertThat(actualMagistrateCourts, is(expectedMagistrateCourts));
    }


    private String getJsonBodyStr(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", UUID.randomUUID().toString())
                .replace("DEF_ID_2", UUID.randomUUID().toString())
                .replace("TODAY", LocalDate.now().toString());
    }
}
