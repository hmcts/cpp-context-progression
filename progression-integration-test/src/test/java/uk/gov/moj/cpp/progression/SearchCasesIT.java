package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesByCaseUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({"squid:S1607"})
public class SearchCasesIT extends AbstractIT {

    private String firstName;
    private String middleName;
    private String lastName;

    private static final String JSON_RESULTS_DEFENDANT_PATH = "$.searchResults[0].defendantName";
    private static final String JSON_RESULTS_DOB = "$.searchResults[0].dob";
    private static final String DOB = "2010-01-01";
    private static ProsecutionCaseUpdateDefendantHelper helper;

    @Before
    public void setUp() {
        firstName = "Harry";
        middleName = "Jack";
        lastName = "Kane Junior";
    }

    @BeforeClass
    public static void setUpCommonData() throws IOException {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(randomUUID().toString(), randomUUID().toString());
        addProsecutionCaseToCrownCourt(caseId, defendantId);
    }

    @AfterClass
    public static void tearDown() {
        helper = null;
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveFirstName() {
        verifyCasesForSearchCriteria("harry", new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(firstName))});
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveLastName() {
        verifyCasesForSearchCriteria("KANE", new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString("Kane"))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstName() {
        verifyCasesForSearchCriteria(firstName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString("Harry"))});
    }

    @Test
    public void shouldGetProsecutionCaseByMiddleName() {
        verifyCasesForSearchCriteria(middleName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(middleName))});
    }

    @Test
    public void shouldGetProsecutionCaseByLastName() {
        verifyCasesForSearchCriteria("Kane+Junior", new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(lastName))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstAndMiddleName() {
        final String firstAndMiddleName = "Harry+Jack";
        verifyCasesForSearchCriteria(firstAndMiddleName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(middleName))});
    }

    @Test
    @Ignore
    public void shouldGetProsecutionCaseByFirstNameAfterDefendantUpdate() {
        verifyCasesForSearchCriteria(firstName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(firstName))});
        final String updatedFirstName = "updatedName";
        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForDefendentChanged();
        verifyCasesForSearchCriteria(updatedFirstName, new Matcher[]{withJsonPath(JSON_RESULTS_DEFENDANT_PATH, containsString(updatedFirstName))});
    }

    @Test
    public void shouldGetProsecutionCaseByReference() throws IOException {
        addProsecutionCaseWithUrn(randomUUID().toString(), randomUUID().toString(), "URN12345");
        verifyCasesByCaseUrn("URN12345", new Matcher[]{withJsonPath("$.searchResults[0].reference", equalTo("URN12345"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobDDMMMyy() {
        verifyCasesForSearchCriteria("01/01/10", new Matcher[]{withJsonPath(JSON_RESULTS_DOB, equalTo(DOB))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobyyyyMMDD() {
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
}

