package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_APPLICATION_AAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import org.junit.jupiter.api.Test;

public class StandaloneApplicationRespondentsIT extends AbstractIT {

    private static CourtApplicationsHelper.CourtApplicationRandomValues randomValues;

    @Test
    public void shouldGetStandaloneApplicationWithMultipleRespondents() throws Exception {
        randomValues = new CourtApplicationsHelper.CourtApplicationRandomValues();
        CourtApplicationsHelper.addStandaloneCourtApplicationWithRespondents("progression.create-court-application-with-respondents.json", randomValues);
        poll(requestParams(getReadUrl("/applications/" + randomValues.RANDOM_APPLICATION_ID), PROGRESSION_QUERY_APPLICATION_AAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", equalTo(randomValues.RANDOM_APPLICATION_ID)),
                                withJsonPath("$.respondentDetails[0].name", equalTo(randomValues.RESPONDENT_ORGANISATION_NAME)),
                                withJsonPath("$.respondentDetails[0].isProbationBreach", equalTo(true)),
                                withJsonPath("$.respondentDetails[0].address.address1", equalTo(randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS1)),
                                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativeName", equalTo(format("%s %s", randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME, randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME))),
                                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativePosition", equalTo(randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_POSITION)),
                                withJsonPath("$.respondentDetails[1].name", equalTo(format("%s %s", randomValues.RANDOM_INDIVIDUAL_FIRST_NAME, randomValues.RANDOM_INDIVIDUAL_LAST_NAME))),
                                withJsonPath("$.respondentDetails[1].address.address1", equalTo(randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS1)),
                                withJsonPath("$.respondentDetails[1].respondentRepresentatives[0].representativeName", equalTo(randomValues.RANDOM_INDIVIDUAL_ORG_NAME))
                        )));
    }
}


