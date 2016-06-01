package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
/**
 * @author hshaik
 */
public class IndicateStatementsIntegrationTest extends AbstractIT {

    private String caseId;
    private String indicatestatementId;
    private int version;

    @Before
    public void setUp() {
        caseId = UUID.randomUUID().toString();
        indicatestatementId = UUID.randomUUID().toString();
        version = 0;
    }

    @Test
    public void IndicatestatementTest() throws IOException, InterruptedException {

        Response writeResponse = postCommand(getCommandUri("/cases/indicatestatement"),
                "application/vnd.progression.command.indicate-statement+json",
                getJsonBodyStr("progression.command.indicate-statement.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId + "/indicatestatements"),
                "application/vnd.progression.query.indicatestatementsdetails+json");

        assertThat(queryResponse.getStatusCode(), is(200));
        ResponseBody respBody = queryResponse.getBody();
        assertThat((ArrayList<HashMap>) respBody.path("indicatestatements"), hasSize(1));
        assertThat(queryResponse.jsonPath().getList("indicatestatements.caseId").contains(caseId),equalTo(true));

    }

    private void waitForResponse(int i) throws InterruptedException {
        TimeUnit.SECONDS.sleep(i);
    }

    private Response postCommand(String uri, String mediaType, String jsonStringBody) throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody).when().post(uri).then().extract().response();
    }

    private Response getCaseProgressionDetail(String uri, String mediaType) throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).when().get(uri).then().extract().response();
    }

    private String getJsonBodyStr(String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset()).replace("RANDOM_ID", indicatestatementId)
                .replace("RANDOM_CASE_ID", caseId).replace("VERSION", String.valueOf(version)).replace("TODAY", LocalDate.now().toString());
    }

    private String getCommandUri(String path) {
        return prop.getProperty("base-uri") + prop.getProperty("base-uri-command") + path;
    }

    private String getQueryUri(String path) {
        return prop.getProperty("base-uri") + prop.getProperty("base-uri-query") + path;
    }
}
