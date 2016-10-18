package uk.gov.moj.cpp.progression.it;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.StubUtil;

public class ProgressionIntegrationTest extends AbstractIT {

	private String caseId;
	private String caseProgressionId;

	@Before
	public void createMockEndpoints() throws IOException {
		caseId = UUID.randomUUID().toString();
		StubUtil.resetStubs();
		StubUtil.setupStructureCaseStub(caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
		StubUtil.setupUsersGroupQueryStub();
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
		assertThat(queryResponse.getStatusCode(), equalTo(HttpStatus.SC_OK));

		writeResponse = postCommand(getCommandUri("/cases/issuedirection"),
				"application/vnd.progression.command.issue-direction+json",
				getJsonBodyStr("progression.command.issue-direction.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);

		writeResponse = postCommand(getCommandUri("/cases/sendingcommittalhearinginformation"),
				"application/vnd.progression.command.sending-committal-hearing-information+json",
				getJsonBodyStr("progression.command.sending-committal-hearing-information.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);
		queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
				"application/vnd.progression.query.caseprogressiondetail+json");
		assertTrue(queryResponse.getBody().path("sendingCommittalDate").equals(LocalDate.now().toString()));

		assertNull(queryResponse.getBody().path("isPSROrdered"));
		writeResponse = postCommand(getCommandUri("/cases/presentencereport"),
				"application/vnd.progression.command.pre-sentence-report+json",
				getJsonBodyStr("progression.command.pre-sentence-report.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);
		queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
				"application/vnd.progression.query.caseprogressiondetail+json");
		assertTrue(queryResponse.getBody().path("isPSROrdered"));

		writeResponse = postCommand(getCommandUri("/cases/sentencehearingdate"),
				"application/vnd.progression.command.sentence-hearing-date+json",
				getJsonBodyStr("progression.command.sentence-hearing-date.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);
		queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
				"application/vnd.progression.query.caseprogressiondetail+json");
		assertTrue(queryResponse.getBody().path("sentenceHearingDate").equals(LocalDate.now().toString()));

		writeResponse = postCommand(getCommandUri("/cases/casetobeassigned"),
				"application/vnd.progression.command.case-to-be-assigned+json",
				getJsonBodyStr("progression.command.case-to-be-assigned.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);
		queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
				"application/vnd.progression.query.caseprogressiondetail+json");
		assertTrue(queryResponse.getBody().path("status").equals("READY_FOR_REVIEW"));

		writeResponse = postCommand(getCommandUri("/cases/caseassignedforreview"),
				"application/vnd.progression.command.case-assigned-for-review+json",
				getJsonBodyStr("progression.command.case-assigned-for-review.json"));
		assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
		waitForResponse(5);
		queryResponse = getCaseProgressionDetail(getQueryUri("/cases/" + caseId),
				"application/vnd.progression.query.caseprogressiondetail+json");
		assertTrue(queryResponse.getBody().path("status").equals("ASSIGNED_FOR_REVIEW"));
	}

	private String getQueryUri(final String path) {
		return baseUri + prop.getProperty("base-uri-query") + path;
	}

	private String getCommandUri(final String path) {
		return baseUri + prop.getProperty("base-uri-command") + path;
	}

	private Response postCommand(final String uri, final String mediaType, final String jsonStringBody)
			throws IOException {
		return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody)
				.header("CJSCPPUID", UUID.randomUUID().toString()).when().post(uri).then().extract().response();
	}

	private Response getCaseProgressionDetail(final String uri, final String mediaType) throws IOException {
		return given().spec(reqSpec).and().accept(mediaType).header("CJSCPPUID", UUID.randomUUID().toString()).when()
				.get(uri).then().extract().response();
	}

	private String getJsonBodyStr(final String fileName) throws IOException {
		return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
				.replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
				.replace("DEF_ID_1", UUID.randomUUID().toString()).replace("DEF_ID_2", UUID.randomUUID().toString())
				.replace("TODAY", LocalDate.now().toString());
	}
}
