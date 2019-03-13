package uk.gov.moj.cpp.progression;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


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
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // when
        final String response = getCasesForSearchCriteria("harry");
        // then
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains(firstName));
    }

    @Test
    public void shouldGetProsecutionCaseByCaseInsensitiveLastName() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // when
        final String response = getCasesForSearchCriteria("KANE");
        // then
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains("Kane"));
    }

    @Test
    public void shouldGetProsecutionCaseByFirstName() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // when
        final String response = getCasesForSearchCriteria(firstName);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains("Harry"));
    }

    @Test
    public void shouldGetProsecutionCaseByMiddleName() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria(middleName);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);

        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains(middleName));
    }

    @Test
    public void shouldGetProsecutionCaseByLastName() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("Kane+Junior");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains(lastName));
    }

    @Test
    public void shouldGetProsecutionCaseByFirstAndMiddleName() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String firstAndMiddleName = "Harry+Jack";
        // when
        final String response = getCasesForSearchCriteria(firstAndMiddleName);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains(middleName));
    }

    @Test
    public void shouldGetProsecutionCaseByFirstNameAfterDefendantUpdate() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getCasesForSearchCriteria(firstName);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        final String updatedFirstName = "updatedName";
        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForDefendentChanged();
        response = getCasesForSearchCriteria(updatedFirstName);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertTrue(prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("defendantName").contains(updatedFirstName));
    }

    @Test
    public void shouldGetProsecutionCaseByReference() throws Exception {
        // given
        addProsecutionCaseWithUrn(caseId, defendantId,"URN12345");
        // when
        final String response = getCasesForSearchCriteria("URN12345");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("URN12345", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("reference"));
    }

    @Test
    public void shouldGetProsecutionCaseByDobDDMMMyy() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("1-Jan-2010");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }

    @Test
    public void shouldGetProsecutionCaseByDobyyyyMMDD() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("01/01/10");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }

    @Test
    public void shouldGetProsecutionCaseByDobDMMyy() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("1/01/10");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }

    @Test
    public void shouldGetProsecutionCaseByDOBDdMMyyyy() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("01/01/2010");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_d_MM_yyyy() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("1-01-2010");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }

    @Test
    public void shouldGetProsecutionCaseByDOB_dd_MM_yy() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getCasesForSearchCriteria("01-01-10");
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertNotNull(prosecutioncasesJsonObject);
        assertEquals("2010-01-01", prosecutioncasesJsonObject.getJsonArray("searchResults").getJsonObject(0).getString("dob"));
    }
}

