package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import org.hamcrest.Matcher;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchCourtApplicationsIT {

    
    private static final String JSON_RESULTS_DEFENDANT_PATH     = "$.searchResults[0].defendantName";
    private static final String JSON_RESULTS_PROSECUTOR_PATH    = "$.searchResults[0].prosecutor";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer("public.progression.court-application-created");
    
    private static CourtApplicationRandomValues randomValues;

    @Before
    public void setUp() {
        createMockEndpoints();
    }

    @BeforeClass
    public static void createStandaloneApplication() throws Exception {
        randomValues = new CourtApplicationsHelper().new CourtApplicationRandomValues();
        addStandaloneCourtApplication(UUID.randomUUID().toString(), UUID.randomUUID().toString(), randomValues, "progression.command.create-standalone-court-application.json");
    }

    
    @Test
    public void shouldGetApplicationByApplicantNames() throws Exception {
        verifyCasesForSearchCriteria(randomValues.APPLICANT_FIRSTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH,containsString(randomValues.APPLICANT_FIRSTNAME))});
        verifyCasesForSearchCriteria(randomValues.APPLICANT_MIDDLENAME.toUpperCase(), new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH,containsString(randomValues.APPLICANT_MIDDLENAME))});
        verifyCasesForSearchCriteria(randomValues.APPLICANT_LASTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH,containsString(randomValues.APPLICANT_LASTNAME))});
    }
    
    @Test
    public void shouldGetApplicationByRespondentNames() throws Exception {

        verifyCasesForSearchCriteria(randomValues.RESPONDENT_FIRSTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH,containsString(randomValues.RESPONDENT_FIRSTNAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_MIDDLENAME.toUpperCase(), new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH,containsString(randomValues.RESPONDENT_MIDDLENAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_LASTNAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH,containsString(randomValues.RESPONDENT_LASTNAME))});
        verifyCasesForSearchCriteria(randomValues.RESPONDENT_ORGANISATION_NAME, new Matcher[]{withJsonPath(JSON_RESULTS_PROSECUTOR_PATH,containsString(randomValues.RESPONDENT_ORGANISATION_NAME))});
    }
    
    @Test
    public void shouldGetApplicationByReferenceNumber() throws Exception {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String applicationReferenceNumber = message.get().getString("arn");
        assertTrue(message.isPresent());
        verifyCasesForSearchCriteria(applicationReferenceNumber, new Matcher[]{withJsonPath("$.searchResults[0].reference",containsString(applicationReferenceNumber))});
    }
    

}

