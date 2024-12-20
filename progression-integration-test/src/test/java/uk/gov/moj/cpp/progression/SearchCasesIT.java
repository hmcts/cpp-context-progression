package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.APPLICATION_VND_PROGRESSION_REFER_CASES_TO_COURT_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesByCaseUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchCasesIT extends AbstractIT {

    private static final String JSON_RESULTS_DEFENDANT_PATH = "$.searchResults[0].defendantName";
    private static final String JSON_RESULTS_DOB = "$.searchResults[0].dob";
    private static final String DOB = "2010-01-01";
    private static final String CASE_URN = generateUrn();
    private static final String FIRST_NAME = randomAlphabetic(15);
    private static final String MIDDLE_NAME = randomAlphabetic(15);
    private static final String LAST_NAME = randomAlphabetic(15);


    @BeforeAll
    public static void setUpCommonData() throws IOException, JSONException {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCase(caseId, defendantId, CASE_URN, FIRST_NAME, LAST_NAME, MIDDLE_NAME);
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveFirstName() {
        verifyCasesForSearchCriteria(FIRST_NAME.toLowerCase(), new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(FIRST_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveLastName() {
        verifyCasesForSearchCriteria(LAST_NAME.toUpperCase(), new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(LAST_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstName() {
        verifyCasesForSearchCriteria(FIRST_NAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(FIRST_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByMiddleName() {
        verifyCasesForSearchCriteria(MIDDLE_NAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(MIDDLE_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByLastName() {
        verifyCasesForSearchCriteria(LAST_NAME, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(LAST_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstAndMiddleName() {
        final String firstAndMiddleName = FIRST_NAME + "+" + MIDDLE_NAME;
        verifyCasesForSearchCriteria(firstAndMiddleName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(MIDDLE_NAME))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstNameAfterDefendantUpdate() throws JSONException, IOException {
        final String firstName = randomAlphabetic(15);
        final String middleName = randomAlphabetic(15);
        final String lastName = randomAlphabetic(15);

        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final ProsecutionCaseUpdateDefendantHelper localHelper = new ProsecutionCaseUpdateDefendantHelper(caseId1, defendantId1);
        addProsecutionCase(caseId1, defendantId1, generateUrn(), firstName, lastName, middleName);

        verifyCasesForSearchCriteria(firstName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(firstName))});
        final String updatedFirstName = randomAlphabetic(20);
        final JmsMessageConsumerClient publicEventsCaseDefendantChanged = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-defendant-changed").getMessageConsumerClient();

        // when
        final String jsonString = getUpdatedPayload(updatedFirstName, middleName, lastName);
        localHelper.updateDefendant(jsonString);

        // then
        localHelper.verifyInMessagingQueueForDefendantChanged(publicEventsCaseDefendantChanged);
        verifyCasesForSearchCriteria(updatedFirstName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(updatedFirstName))});
    }

    @Test
    public void shouldGetProsecutionCaseByReference() throws IOException, JSONException {
        addProsecutionCaseWithUrn(randomUUID().toString(), randomUUID().toString(), "URN12345");
        verifyCasesByCaseUrn("URN12345", new Matcher[]{withJsonPath("$.searchResults[0].reference", equalTo("URN12345"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobDDMMMyy() {
        verifyCasesForSearchCriteria("01/01/10", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobDMMyy() {
        verifyCasesForSearchCriteria("1/01/10", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOBDdMMyyyy() {
        verifyCasesForSearchCriteria("01/01/2010", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_d_MM_yyyy() {
        verifyCasesForSearchCriteria("1-01-2010", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_dd_MM_yy() {
        verifyCasesForSearchCriteria("01-01-10", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    private static Response addProsecutionCase(final String caseId, final String defendantId, final String caseUrn, final String firstname, final String lastName, final String middleName) throws IOException {
        final String payload = getPayload("progression.command.prosecution-case-refer-to-court-search-test.json")
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_FIRST_NAME", firstname)
                .replace("RANDOM_MIDDLE_NAME", middleName)
                .replace("RANDOM_LAST_NAME", lastName)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);

        return postCommand(getWriteUrl("/refertocourt"),
                APPLICATION_VND_PROGRESSION_REFER_CASES_TO_COURT_JSON,
                payload);
    }

    private String getUpdatedPayload(final String firstName, final String middleName, final String lastName) {
        return getPayload("progression.update-defendant-for-prosecution-case-search-test.json")
                .replaceAll("RANDOM_FIRST_NAME", firstName)
                .replaceAll("RANDOM_MIDDLE_NAME", middleName)
                .replaceAll("RANDOM_LAST_NAME", lastName);
    }
}

