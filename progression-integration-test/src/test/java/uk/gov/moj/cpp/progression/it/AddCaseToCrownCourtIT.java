package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.Endpoint;
import uk.gov.moj.cpp.progression.helper.WireMockHelper;

public class AddCaseToCrownCourtIT extends AbstractIT {

    private String caseId;
    private String caseProgressionId;

    @Before
    public void createMockEndpoints() {
        WireMock.configureFor(HOST, PORT);

        WireMockHelper.stub(new Endpoint.EndpointBuilder()
                        .endpoint("/structure-query-api/query/api/rest/structure/case.*")
                        .forRequestType(WireMock::get).willReturnStatus(HttpStatus.SC_OK)
                        .withContentType("application/json")
                        .andBody(WireMockHelper.getPayload("structure.query.case-defendants.json"))
                        .build());

        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        Response writeResponse = postCommand(getCommandUri("/cases/addcasetocrowncourt"),
                        "application/vnd.progression.command.add-case-to-crown-court+json",
                        getJsonBodyStr("progression.command.add-case-to-crown-court.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
    }


    @After
    public void tearDown() {
        WireMock.reset();
    }

    private void waitForResponse(int i) throws InterruptedException {
        TimeUnit.SECONDS.sleep(i);
    }

    private String getQueryUri(String path) {
        return baseUri + prop.getProperty("base-uri-query") + path;
    }

    private String getCommandUri(String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }

    private Response postCommand(String uri, String mediaType, String jsonStringBody)
                    throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody).when()
                        .post(uri).then().extract().response();
    }

    private Response getCaseProgressionDetail(String uri, String mediaType) throws IOException {
        return given().spec(reqSpec).and().accept(mediaType).when().get(uri).then().extract()
                        .response();
    }

    private String getJsonBodyStr(String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                        .replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
                        .replace("TODAY", LocalDate.now().toString());
    }
}
