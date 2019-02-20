package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
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
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class ProgressionSentenceHearingIT {

    private static final String PROGRESSION_COMMAND_SENTENCE_HEARING_DATE = "progression.command.sentence-hearing-date";

    private String caseId;
    private final LocalDate futureDate = LocalDate.now().plusDays(10);

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();

        createMockEndpoints();

    }

    @Test
    public void shouldAddSentenceHearing() throws Exception {
        addDefendant(caseId);

        Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyStr("progression.command.sentence-hearing-date.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));


        String querySentenceHearingDateResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        JsonObject caseProgressionDetailJsonObject = getJsonObject(querySentenceHearingDateResponse);

        assertTrue(caseProgressionDetailJsonObject.getString("sentenceHearingDate")
                .equals(LocalDate.now().toString()));

        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyForHearingDate("progression.command.sentence-hearing-date.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));


        querySentenceHearingDateResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        caseProgressionDetailJsonObject = getJsonObject(querySentenceHearingDateResponse);

        assertTrue(caseProgressionDetailJsonObject.getString("sentenceHearingDate")
                .equals(futureDate.toString()));
    }


    private String getJsonBodyStr(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("TODAY", LocalDate.now().toString());
    }

    private String getJsonBodyForHearingDate(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("TODAY", futureDate.toString());
    }

}
