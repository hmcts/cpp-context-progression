package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import org.hamcrest.Matcher;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.util.UUID;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({"squid:S1607"})
@Ignore
public class SearchCasesIT {

    private String caseId;
    private String defendantId;
    private String firstName;
    private String middleName;
    private String lastName;

    private final String dob = "2010-01-01";
    private ProsecutionCaseUpdateDefendantHelper helper;

    @Before
    public void setUp() {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        firstName = "Harry";
        middleName="Jack";
        lastName="Kane Junior";
        createMockEndpoints();
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveFirstName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // then
        verifyCasesForSearchCriteria("harry", new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(firstName))});

    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveLastName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // then
        verifyCasesForSearchCriteria("KANE", new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString("Kane"))});

    }

    @Test
    public void shouldGetProsecutionCaseByFirstName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // then
        verifyCasesForSearchCriteria(firstName, new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString("Harry"))});

    }

    @Test
    public void shouldGetProsecutionCaseByMiddleName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria(middleName, new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(middleName))});
    }

    @Test
    public void shouldGetProsecutionCaseByLastName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("Kane+Junior", new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(lastName))});
    }

    @Test
    public void shouldGetProsecutionCaseByFirstAndMiddleName() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String firstAndMiddleName = "Harry+Jack";
        // then
        verifyCasesForSearchCriteria(firstAndMiddleName, new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(middleName))});
    }

   @Test
    public void shouldGetProsecutionCaseByFirstNameAfterDefendantUpdate() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyCasesForSearchCriteria(firstName, new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(firstName))});
        final String updatedFirstName = "updatedName";
        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForDefendentChanged();
        verifyCasesForSearchCriteria(updatedFirstName, new Matcher[]{withJsonPath("$.searchResults[0].defendantName",containsString(updatedFirstName))});
    }

    @Test
    public void shouldGetProsecutionCaseByReference() throws Exception {
        // when
        addProsecutionCaseWithUrn(caseId, defendantId,"URN12345");
        // then
        verifyCasesForSearchCriteria("URN12345", new Matcher[]{withJsonPath("$.searchResults[0].reference",equalTo("URN12345"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobDDMMMyy() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("1-Jan-2010", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobyyyyMMDD() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("01/01/10", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDobDMMyy() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("1/01/10", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOBDdMMyyyy() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("01/01/2010", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_d_MM_yyyy() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("1-01-2010", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_dd_MM_yy() throws Exception {
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // then
        verifyCasesForSearchCriteria("01-01-10", new Matcher[]{withJsonPath("$.searchResults[0].dob",equalTo("2010-01-01"))});
    }
}

