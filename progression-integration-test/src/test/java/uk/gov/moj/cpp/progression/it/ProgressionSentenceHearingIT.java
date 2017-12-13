package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseProgressionDetail;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
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
    private String caseProgressionId;
    private String sentenceHearingId;
    private LocalDate futureDate = LocalDate.now().plusDays(10);

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = caseId;
        sentenceHearingId = UUID.randomUUID().toString();
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
                "application/vnd.progression.command.add-sentence-hearing+json",
                getJsonBodyStr("progression.command.add-sentence-hearing.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String querySentenceHearingIdResponse =
                pollForResponse(join("", "/cases/", caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        JsonObject sentenceHearingIdJsonObject = getJsonObject(querySentenceHearingIdResponse);

        assertTrue(sentenceHearingIdJsonObject.getString("sentenceHearingId")
                .equals(sentenceHearingId));

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

    @Test
    public void shouldNotAddSentenceHearingDate_CapabilityDisabled() throws Exception {
        givenAddSentenceHearingDateCapabilityDisabled();
        addDefendant(caseId);

        Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyStr("progression.command.sentence-hearing-date.json"));

        assertThatResponseIndicatesFeatureDisabled(writeResponse);
    }

    private void givenAddSentenceHearingDateCapabilityDisabled() {
        stubSetStatusForCapability(PROGRESSION_COMMAND_SENTENCE_HEARING_DATE, false);
    }

    private String getJsonBodyStr(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
                .replace("SENTENCE_HEARING_ID", sentenceHearingId)
                .replace("TODAY", LocalDate.now().toString());
    }

    private String getJsonBodyForHearingDate(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_ID", caseProgressionId)
                .replace("TODAY", futureDate.toString());
    }

}
