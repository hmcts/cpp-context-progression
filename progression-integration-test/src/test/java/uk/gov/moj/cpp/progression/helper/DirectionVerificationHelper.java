package uk.gov.moj.cpp.progression.helper;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.DMConstants.PROMPT_1;
import static uk.gov.moj.cpp.progression.DMConstants.TIMEOUT_IN_SECONDS;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;

public class DirectionVerificationHelper {

    public static String assertTransformedDirectionFromRefDB(final String directionId, final String caseId, final String orderDate, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/direction/%s/caseId/%s?orderDate=%s", directionId, caseId, orderDate)),
                "application/vnd.progression.query.direction+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    public static String assertTransformedDirectionFromRefDBNoOrderDate(final String directionId, final String caseId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/direction/%s/caseId/%s", directionId, caseId)),
                "application/vnd.progression.query.direction+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    public static String assertTransformedQueryFormDirectionFromRefDBWithoutCategories(final String caseId, final String formId, final String formType, final Matcher... matchers) {

        return poll(requestParams(getReadUrl(format("/directions/%s/courtform/%s?formType=%s", caseId, formId, formType)),
                "application/vnd.progression.query.form-directions+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    public static String assertTransformedQueryFormDirectionFromRefDB(final String caseId, final String formId, final String categories, final String formType, final Matcher... matchers) {

        return poll(requestParams(getReadUrl(format("/directions/%s/courtform/%s?categories=%s&formType=%s", caseId, formId, categories, formType)),
                "application/vnd.progression.query.form-directions+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    public static void verifyTransformedQueryFormDirection(final String caseId, final String formId, final String categories, final String formType, final String directionId) {
        assertTransformedQueryFormDirectionFromRefDB(caseId, formId, categories, formType, allOf(
                withJsonPath("$.directions[0].refData.directionRefDataId", equalTo(directionId)),
                withJsonPath("$.directions[0].prompts[0].id", equalTo(PROMPT_1)),
                withJsonPath("$.directions[0].prompts[0].header", equalTo(true))
        ));
    }

    public static void verifyTransformedQueryFormDirectionWithoutCategories(final String caseId, final String formId, final String formType, final String directionId) {
        assertTransformedQueryFormDirectionFromRefDBWithoutCategories(caseId, formId, formType, allOf(
                withJsonPath("$.directions[0].refData.directionRefDataId", equalTo(directionId)),
                withJsonPath("$.directions[0].prompts[0].id", equalTo(PROMPT_1)),
                withJsonPath("$.directions[0].prompts[0].header", equalTo(true))
        ));
    }

}
