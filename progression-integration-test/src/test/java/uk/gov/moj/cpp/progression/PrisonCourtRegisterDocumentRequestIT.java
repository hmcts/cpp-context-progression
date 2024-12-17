package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class PrisonCourtRegisterDocumentRequestIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();

    private ProsecutionCaseUpdateDefendantHelper helper;
    private String courtCentreId;


    @BeforeEach
    public void setup() {
        courtCentreId = randomUUID().toString();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
    }

    @Test
    public void shouldAddPrisonCourtDocumentRequest() throws IOException {

        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final String body = getPayload("progression.prison-court-register-document-request.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));


        writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId, 2);
    }

    @Test
    public void shouldAddPrisonCourtDocumentRequestWithApplication() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID defendantId = randomUUID();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId.toString(), defendantId.toString());

        initiateCourtProceedingsForMatchedDefendants(caseId.toString(), defendantId.toString(), defendantId.toString());
        pollProsecutionCasesProgressionFor(caseId.toString());

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId.toString(), defendantId.toString(), defendantId.toString());

        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        pollForApplication(courtApplicationId.toString());

        final String body = getPayload("progression.prison-court-register-document-request-with_application.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%COURT_APPLICATION_ID%", courtApplicationId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId, 1);
    }


    public void verifyPrisonCourtRegisterRequestsExists(final UUID courtCentreId, final UUID hearingId, int fileIdCount) {
        verifyPrisonCourtRegisterDocumentRequests(courtCentreId, allOf(
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.prisonCourtRegisterDocumentRequests[0].payload", containsString(hearingId.toString())),
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId", hasSize(fileIdCount))
        ));
    }

    private void verifyPrisonCourtRegisterDocumentRequests(final UUID courtCentreId, final Matcher... matchers) {
        poll(requestParams(getReadUrl(StringUtils.join("/prison-court-register/request/", courtCentreId)),
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(10, TimeUnit.SECONDS)
                .until(
                        status().is(javax.ws.rs.core.Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

}
