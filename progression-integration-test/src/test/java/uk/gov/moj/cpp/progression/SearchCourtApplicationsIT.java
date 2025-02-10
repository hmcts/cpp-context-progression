package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesByCaseUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;

import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchCourtApplicationsIT extends AbstractIT {


    private static final String JSON_RESULTS_DEFENDANT_PATH = "$.searchResults[0].defendantName";
    private static final String JSON_RESULTS_PROSECUTOR_PATH = "$.searchResults[0].prosecutor";

    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-application-created").getMessageConsumerClient();

    private static CourtApplicationRandomValues randomValues;
    private static String applicationId;

    @BeforeEach
    public void createStandaloneApplication() throws Exception {
        applicationId = randomUUID().toString();
        randomValues = new CourtApplicationRandomValues();
        addStandaloneCourtApplication(applicationId, randomUUID().toString(), randomValues, "progression.command.create-standalone-court-application.json");
    }


    @Test
    public void shouldGetApplicationByApplicantNames() {
        verifyCasesForSearchCriteria(randomValues.APPLICANT_FIRSTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(randomValues.APPLICANT_FIRSTNAME))});
        verifyCasesForSearchCriteria(randomValues.APPLICANT_MIDDLENAME.toUpperCase(), new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(randomValues.APPLICANT_MIDDLENAME))});
        verifyCasesForSearchCriteria(randomValues.APPLICANT_LASTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(randomValues.APPLICANT_LASTNAME))});
    }

    @Test
    public void shouldGetApplicationByRespondentNames() {

        verifyCasesForSearchCriteria(randomValues.RESPONDENT_FIRSTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH, containsString(randomValues.RESPONDENT_FIRSTNAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_MIDDLENAME.toUpperCase(), new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH, containsString(randomValues.RESPONDENT_MIDDLENAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_LASTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH, containsString(randomValues.RESPONDENT_LASTNAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_ORGANISATION_NAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH, containsString(randomValues.RESPONDENT_ORGANISATION_NAME))});
    }

    @Test
    public void shouldGetApplicationByReferenceNumber() {
        final JsonPath message = retrieveMessageAsJsonPath(consumerForCourtApplicationCreated, isJson(Matchers.allOf(
                        withJsonPath("$.courtApplication.id", CoreMatchers.is(applicationId))
                )
        ));
        assertThat(message, notNullValue());
        final String applicationReferenceNumber = message.getString("courtApplication.applicationReference");
        verifyCasesByCaseUrn(applicationReferenceNumber, new Matcher[]{withJsonPath("$.searchResults[0].reference", containsString(applicationReferenceNumber))});
    }


}

