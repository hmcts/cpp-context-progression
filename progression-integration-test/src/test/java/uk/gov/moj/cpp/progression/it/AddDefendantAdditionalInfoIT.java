package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.StubUtil;



public class AddDefendantAdditionalInfoIT extends AbstractIT {

    private String caseId;
    private String caseProgressionId;
    private String defendantId;
    private String defendant2Id;
    
    @Before
    public void createMockEndpoints() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        defendant2Id =  UUID.randomUUID().toString();
        StubUtil.resetStubs();
        StubUtil.setupStructureCaseStub(caseId, defendantId,defendant2Id, caseProgressionId);
        StubUtil.setupUsersGroupQueryStub();

    }

    @Test
    public void shouldAddAdditionalInfoForDefendant() throws Exception {

        Response writeResponse = postCommand(getCommandUri("/cases/addcasetocrowncourt"),
                        "application/vnd.progression.command.add-case-to-crown-court+json",
                        StubUtil.getJsonBodyStr("progression.command.add-case-to-crown-court.json",
                                        caseId, defendantId, defendant2Id, caseProgressionId));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(
                        getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        JsonObject defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                        equalTo(Boolean.FALSE));

        writeResponse = postCommand(
                        getCommandUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.command.add-defendant-additional-information+json",
                        StubUtil.getJsonBodyStr(
                                        "progression.command.add-defendant-additional-information.json",
                                        caseId, defendantId, defendant2Id, caseProgressionId));

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        waitForResponse(5);
        
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));


        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());
        assertThat(defendantsJsonObject.getString("status"),
        equalTo("INCOMPLETE"));

        queryResponse = getCaseProgressionDetail(
                        getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                        "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                        equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("probation").getJsonObject("preSentenceReport").getBoolean("psrIsRequested"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("probation").getJsonObject("preSentenceReport").getString("provideGuidance"),equalTo("guidance"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("probation").getJsonObject("preSentenceReport").getBoolean("drugAssessment"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("probation").getBoolean("dangerousnessAssessment"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("defence").getJsonObject("statementOfMeans").getBoolean("isStatementOfMeans"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("defence").getJsonObject("statementOfMeans").getString("details"),equalTo("meansDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("defence").getJsonObject("medicalDocumentation").getBoolean("isMedicalDocumentation"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("defence").getJsonObject("medicalDocumentation").getString("details"),equalTo("medicalDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("defence").getString("otherDetails"),equalTo("otherDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("prosecution").getJsonObject("ancillaryOrders").getBoolean("isAncillaryOrders"),equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("prosecution").getJsonObject("ancillaryOrders").getString("details"),equalTo("ancillaryOrdersDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getJsonObject("prosecution").getString("otherDetails"),equalTo("otherDetails"));
        
        
        writeResponse = postCommand(
                getCommandUri("/cases/" + caseId + "/defendants/" + defendant2Id),
                "application/vnd.progression.command.add-defendant-additional-information+json",
                StubUtil.getJsonBodyStr(
                                "progression.command.add-defendant-additional-information.json",
                                caseId, defendantId, defendant2Id, caseProgressionId));

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        waitForResponse(5);

        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));
        
        
        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());
        assertThat(defendantsJsonObject.getString("status"),
        equalTo("PENDING_FOR_SENTENCING_HEARING"));
        
    }


    @Test
    public void shouldSetNoMoreInformationRequired() throws Exception {

        Response writeResponse = postCommand(getCommandUri("/cases/addcasetocrowncourt"),
                "application/vnd.progression.command.add-case-to-crown-court+json",
                StubUtil.getJsonBodyStr("progression.command.add-case-to-crown-court.json",
                        caseId, defendantId, defendant2Id, caseProgressionId));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        waitForResponse(5);
        Response queryResponse = getCaseProgressionDetail(
                getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        JsonObject defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                equalTo(Boolean.FALSE));

        LocalDateTime currentDateTime = LocalDateTime.now();
        writeResponse = postCommand(
                getCommandUri("/cases/" + caseId + "/defendants/" + defendantId),
                "application/vnd.progression.command.no-more-information-required+json",StubUtil.getJsonBodyStr(
                        "progression.command.no-more-information-required.json",
                        caseId, defendantId, defendant2Id, caseProgressionId));

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        

        waitForResponse(5);
        
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));


        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());
        assertThat(defendantsJsonObject.getString("status"),
        equalTo("INCOMPLETE"));

        queryResponse = getCaseProgressionDetail(
                getQueryUri("/cases/" + caseId + "/defendants/" + defendantId),
                "application/vnd.progression.query.defendant+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());
        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                        equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation").getBoolean("noMoreInformationRequired"),
                equalTo(Boolean.TRUE));

        LocalDateTime reviewDecisionDateTime = LocalDateTime.parse(defendantsJsonObject.getString("sentenceHearingReviewDecisionDateTime"));
        assertThat(currentDateTime.getYear(),equalTo(reviewDecisionDateTime.getYear()));
        assertThat(currentDateTime.getMonth(),equalTo(reviewDecisionDateTime.getMonth()));
        assertThat(currentDateTime.getDayOfMonth(),equalTo(reviewDecisionDateTime.getDayOfMonth()));
        assertThat(currentDateTime.getHour(),equalTo(reviewDecisionDateTime.getHour()));
        assertThat(currentDateTime.getMinute(),equalTo(reviewDecisionDateTime.getMinute()));

        writeResponse = postCommand(
                getCommandUri("/cases/" + caseId + "/defendants/" + defendant2Id),
                "application/vnd.progression.command.no-more-information-required+json",StubUtil.getJsonBodyStr(
                        "progression.command.no-more-information-required.json",
                        caseId, defendantId, defendant2Id, caseProgressionId));

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        

        waitForResponse(5);
        
        queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");
        assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));


        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());
        assertThat(defendantsJsonObject.getString("status"),
        equalTo("READY_FOR_SENTENCING_HEARING"));

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



    public static JsonObject getJsonObject(final String jsonAsString) {
        JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }
}
