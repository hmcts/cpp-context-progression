package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.Endpoint;
import uk.gov.moj.cpp.progression.helper.WireMockHelper;

public class AddDefendantAdditionalInfoIT extends AbstractIT {

    private String caseId;
    private String caseProgressionId;
    private String defendantId;
    private String defendantProgressionId;

    @Before
    public void createMockEndpoints() throws IOException {
        WireMock.configureFor(HOST, PORT);
        WireMock.reset();
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        defendantProgressionId = "";

    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {

        final JsonObject structurePayload = WireMockHelper
                        .getJsonObject(getJsonBodyStr("structure.query.case-defendants.json"));
        WireMockHelper.stub(new Endpoint.EndpointBuilder()
                        .endpoint("/structure-query-api/query/api/rest/structure/case.*")
                        .forRequestType(WireMock::get).willReturnStatus(HttpStatus.SC_OK)
                        .withContentType("application/json").andBody(structurePayload).build());

        final JsonObject object = WireMockHelper
                        .getJsonObject(getJsonBodyStr("users-groups-system-user.json"));
        WireMockHelper.stub(new Endpoint.EndpointBuilder()
                        .endpoint("/usersgroups-query-api/query/api/rest/usersgroups/users/.*")
                        .forRequestType(WireMock::get).willReturnStatus(HttpStatus.SC_OK)
                        .withContentType("application/json").andBody(object).build());


        Response writeResponse = postCommand(getCommandUri("/cases/addcasetocrowncourt"),
                        "application/vnd.progression.command.add-case-to-crown-court+json",
                        getJsonBodyStr("progression.command.add-case-to-crown-court.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(
                        getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        JsonObject defendantsJsonObject =
                        WireMockHelper.getJsonObject(queryResponse.getBody().asString());

        defendantProgressionId = defendantsJsonObject.getString("defendantProgressionId");
        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                        equalTo(Boolean.FALSE));

        writeResponse = postCommand(
                        getCommandUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.command.add-defendant-additional-information+json",
                        getJsonBodyStr("progression.command.add-defendant-additional-information.json"));

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        waitForResponse(5);

        queryResponse = getCaseProgressionDetail(
                        getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        defendantsJsonObject = WireMockHelper.getJsonObject(queryResponse.getBody().asString());

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                        equalTo(Boolean.TRUE));
    }

    @After
    public void tearDown() {
        WireMock.reset();
    }

    private void waitForResponse(final int i) throws InterruptedException {
        TimeUnit.SECONDS.sleep(i);
    }

    private String getQueryUri(final String path) {
        return baseUri + prop.getProperty("base-uri-query") + path;
    }

    private String getCommandUri(final String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }

    private Response postCommand(final String uri, final String mediaType,
                    final String jsonStringBody) throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody)
                        .header("CJSCPPUID", UUID.randomUUID().toString()).when().post(uri).then()
                        .extract().response();
    }

    private Response getCaseProgressionDetail(final String uri, final String mediaType)
                    throws IOException {
        return given().spec(reqSpec).and().accept(mediaType)
                        .header("CJSCPPUID", UUID.randomUUID().toString()).when().get(uri).then()
                        .extract().response();
    }

    private String getJsonBodyStr(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                        .replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
                        .replace("DEF_ID_1", defendantId)
                        .replace("DEF_ID_2", UUID.randomUUID().toString())
                        .replace("DEF_PRG_ID", defendantProgressionId)
                        .replace("TODAY", LocalDate.now().toString());
    }
}
