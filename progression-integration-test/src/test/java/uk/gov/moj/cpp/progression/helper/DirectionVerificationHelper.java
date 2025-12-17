package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.progression.DMConstants.PROMPT_1;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;

import org.hamcrest.Matcher;

public class DirectionVerificationHelper {

    public static String assertTransformedQueryFormDirectionFromRefDBWithoutCategories(final String caseId, final String formId, final String formType, final Matcher... matchers) {

        return pollForResponse(format("/directions/%s/courtform/%s?formType=%s", caseId, formId, formType),
                "application/vnd.progression.query.form-directions+json",
                randomUUID().toString(),
                matchers);
    }

    public static String assertTransformedQueryFormDirectionFromRefDB(final String caseId, final String formId, final String categories, final String formType, final Matcher... matchers) {

        return pollForResponse(format("/directions/%s/courtform/%s?categories=%s&formType=%s", caseId, formId, categories, formType),
                "application/vnd.progression.query.form-directions+json",
                randomUUID().toString(),
                matchers);
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
