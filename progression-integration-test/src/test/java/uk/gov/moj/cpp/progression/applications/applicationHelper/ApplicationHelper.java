package uk.gov.moj.cpp.progression.applications.applicationHelper;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.helper.RestHelper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;

public class ApplicationHelper {

    public static Response initiateCourtProceedingsForCourtApplication(final String applicationId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getCourtApplicationJson(applicationId, null, null, null, fileName));
    }

    public static Response initiateCourtProceedingsForCourtApplication(final String applicationId, final String caseId, final String fileName) throws IOException {
        final String payload = getCourtApplicationJson(applicationId, caseId, null, null, fileName);
        return initiateCourtProceedingsForCourtApplication(payload);
    }

    public static Response initiateCourtProceedingsForCourtApplicationWithCourtHearing(final String applicationId, final String caseId, final String hearingId, final String fileName) throws IOException {
        final String payload = getCourtApplicationJson(applicationId, caseId, hearingId, null, fileName);
        return initiateCourtProceedingsForCourtApplication(payload);
    }

    public static Response initiateCourtProceedingsForCourtApplication(final String payload) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                payload);
    }

    public static Response initiateCourtProceedingsForCourtApplication(final String applicationId, final String caseId_1, final String caseId_2, final String fileName, final String hearingId) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getCourtApplicationJson2(applicationId, caseId_1, caseId_2, fileName, hearingId));
    }

    public static Response intiateCourtProceedingForApplication(final String applicationId, final String caseId, final String defendantId, final String masterDefendantId,final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getCourtApplicationJson3(applicationId, caseId, defendantId, masterDefendantId, hearingId, fileName));
    }

    public static Response initiateCourtProceedingsForCourtApplication(final String applicationId, final String caseId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getCourtApplicationJson(applicationId, caseId, hearingId, null, fileName));
    }

    public static Response addBreachApplicationForExistingHearing(final String hearingId, final String masterDefendantId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/add-breach-application"),
                "application/vnd.progression.add-breach-application+json",
                getCourtApplicationJson(null, null, hearingId, masterDefendantId, fileName));
    }

    public static void pollForCourtApplication(final String applicationId, final Matcher... matchers) {
        poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(matchers)));
    }

    public static void pollForCourtApplicationCase(final String caseId) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId),
                PROGRESSION_QUERY_PROSECUTION_CASE_JSON).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(withJsonPath("$.prosecutionCase.id", is(caseId)))));
    }

    private static String getCourtApplicationJson(final String applicationId, final String caseId, final String hearingId, final String masterDefendantId, final String fileName) throws IOException {

        String payload = Resources.toString(getResource(fileName), Charset.defaultCharset());

        if (isNotBlank(applicationId)) {
            payload = payload.replace("APPLICATION_ID", applicationId);
        }

        if (isNotBlank(caseId)) {
            payload = payload.replace("CASE_ID", caseId);
        }
        if (isNotBlank(hearingId)) {
            payload = payload.replace("HEARING_ID", hearingId);
        }
        if (isNotBlank(masterDefendantId)) {
            payload = payload.replace("MASTER_DEFENDANT_ID", masterDefendantId);
        }

        return payload;
    }

    private static String getCourtApplicationJson2(final String applicationId, final String caseId_1, final String caseId_2, final String fileName, final String hearingId) throws IOException {
        String payloadJson;
        payloadJson = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("APPLICATION_ID", applicationId)
                .replace("CASE_ID_1", caseId_1)
                .replace("CASE_ID_2", caseId_2)
                .replace("HEARING_ID", hearingId);
        return payloadJson;
    }

    private static String getCourtApplicationJson3(final String applicationId, final String caseId, final String defendantId, final String masterDefendantId, final String hearingId, final String fileName) throws IOException {
        String payloadJson;
        payloadJson = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("APPLICATION_ID", applicationId)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("MASTERDEFENDANTID", masterDefendantId)
                .replace("HEARING_ID", hearingId);
        return payloadJson;
    }
}
