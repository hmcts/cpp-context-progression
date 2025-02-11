package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.getStandaloneCourtApplicationWithRespondentsJsonBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.io.IOException;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class StandaloneApplicationRespondentsIT extends AbstractIT {

    private static CourtApplicationsHelper.CourtApplicationRandomValues randomValues;

    @Test
    public void shouldGetStandaloneApplicationWithMultipleRespondents() throws Exception {
        randomValues = new CourtApplicationsHelper.CourtApplicationRandomValues();
        addStandaloneCourtApplicationWithRespondents("progression.create-court-application-with-respondents.json", randomValues);
        pollForApplicationAtAGlance(randomValues.RANDOM_APPLICATION_ID,
                withJsonPath("$.applicationId", equalTo(randomValues.RANDOM_APPLICATION_ID)),
                withJsonPath("$.respondentDetails[0].name", equalTo(randomValues.RESPONDENT_ORGANISATION_NAME)),
                withJsonPath("$.respondentDetails[0].isProbationBreach", equalTo(true)),
                withJsonPath("$.respondentDetails[0].address.address1", equalTo(randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS1)),
                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativeName", equalTo(format("%s %s", randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME, randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME))),
                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativePosition", equalTo(randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_POSITION)),
                withJsonPath("$.respondentDetails[1].name", equalTo(format("%s %s", randomValues.RANDOM_INDIVIDUAL_FIRST_NAME, randomValues.RANDOM_INDIVIDUAL_LAST_NAME))),
                withJsonPath("$.respondentDetails[1].address.address1", equalTo(randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS1)),
                withJsonPath("$.respondentDetails[1].respondentRepresentatives[0].representativeName", equalTo(randomValues.RANDOM_INDIVIDUAL_ORG_NAME))
        );
    }

    public static Response addStandaloneCourtApplicationWithRespondents(final String fileName, CourtApplicationsHelper.CourtApplicationRandomValues randomValues) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getStandaloneCourtApplicationWithRespondentsJsonBody(fileName, randomValues));
    }
}


