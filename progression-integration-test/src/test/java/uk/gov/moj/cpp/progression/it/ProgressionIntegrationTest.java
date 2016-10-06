package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.StubUtil;

/**
 * @author hshaik
 */
public class ProgressionIntegrationTest extends AbstractIT {

    private String caseId;
    private String caseProgressionId;
    private int version;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        version = 0;
        StubUtil.resetStubs();
        StubUtil.setupUsersGroupQueryStub();
    }

    @Test
    public void progressionTest() throws IOException, InterruptedException {

        Response writeResponse = postCommand(getCommandUri("/cases/sendcasetocrowncourt"),
                        "application/vnd.progression.command.send-to-crown-court+json",
                        getJsonBodyStr("progression.command.send-to-crown-court.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");

        assertThat(queryResponse.getStatusCode(), is(200));
        assertTrue(queryResponse.getBody().path("caseProgressionId").equals(caseProgressionId));
        assertTrue(queryResponse.getBody().path("caseId").equals(caseId));
        version = (queryResponse.getBody().path("version"));

        assertNull(queryResponse.getBody().path("isAllStatementsIdentified"));

        writeResponse = postCommand(getCommandUri("/cases/indicateallstatementsidentified"),
                        "application/vnd.progression.command.indicate-all-statements-identified+json",
                        getJsonBodyStr("progression.command.indicate-all-statements-identified.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("isAllStatementsIdentified"));
        version = (queryResponse.getBody().path("version"));

        assertNull(queryResponse.getBody().path("isAllStatementsServed"));
        writeResponse = postCommand(getCommandUri("/cases/indicateallstatementsserved"),
                        "application/vnd.progression.command.indicate-all-statements-served+json",
                        getJsonBodyStr("progression.command.indicate-all-statements-served.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("isAllStatementsServed"));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/issuedirection"),
                        "application/vnd.progression.command.issue-direction+json",
                        getJsonBodyStr("progression.command.issue-direction.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("directionIssuedOn")
                        .equals(LocalDate.now().toString()));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/vacateptphearing"),
                        "application/vnd.progression.command.vacate-ptp-hearing+json",
                        getJsonBodyStr("progression.command.vacate-ptp-hearing.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("ptpHearingVacatedDate")
                        .equals(LocalDate.now().toString()));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/sendingcommittalhearinginformation"),
                        "application/vnd.progression.command.sending-committal-hearing-information+json",
                        getJsonBodyStr("progression.command.sending-committal-hearing-information.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("sendingCommittalDate")
                        .equals(LocalDate.now().toString()));
        version = (queryResponse.getBody().path("version"));

        assertNull(queryResponse.getBody().path("isPSROrdered"));
        writeResponse = postCommand(getCommandUri("/cases/presentencereport"),
                        "application/vnd.progression.command.pre-sentence-report+json",
                        getJsonBodyStr("progression.command.pre-sentence-report.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("isPSROrdered"));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/sentencehearingdate"),
                        "application/vnd.progression.command.sentence-hearing-date+json",
                        getJsonBodyStr("progression.command.sentence-hearing-date.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("sentenceHearingDate")
                        .equals(LocalDate.now().toString()));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/casetobeassigned"),
                        "application/vnd.progression.command.case-to-be-assigned+json",
                        getJsonBodyStr("progression.command.case-to-be-assigned.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("status").equals("READY_FOR_REVIEW"));
        version = (queryResponse.getBody().path("version"));

        writeResponse = postCommand(getCommandUri("/cases/caseassignedforreview"),
                        "application/vnd.progression.command.case-assigned-for-review+json",
                        getJsonBodyStr("progression.command.case-assigned-for-review.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                        "application/vnd.progression.query.caseprogressiondetail+json");
        assertTrue(queryResponse.getBody().path("status").equals("ASSIGNED_FOR_REVIEW"));
        version = (queryResponse.getBody().path("version"));
    }

    private void waitForResponse(final int i) throws InterruptedException {
        TimeUnit.SECONDS.sleep(i);
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
                        .replace("VERSION", String.valueOf(version))
                        .replace("TODAY", LocalDate.now().toString());
    }

    private String getCommandUri(final String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }

    private String getQueryUri(final String path) {
        return baseUri + prop.getProperty("base-uri-query") + path;
    }


}
