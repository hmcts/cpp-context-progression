package uk.gov.moj.cpp.progression.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class MatchedDefendantCriteriaTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final boolean DEFAULT_PROCEEDINGS_CONCLUDED = false;
    private static final boolean DEFAULT_CROWN_OR_MAGISTRATES = true;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private final String DEFAULT_COURT_ORDER_VALIDITY_DATE = LocalDate.now().toString();

    private static final String PAGE_SIZE = "pageSize";
    private static final String PNC_ID = "pncId";
    private static final String CRO_NUMBER = "croNumber";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String ADDRESS_LINE = "addressLine1";
    private static final String PROCEEDINGS_CONCLUDED = "proceedingsConcluded";
    private static final String CROWN_OR_MAGISTRATES = "crownOrMagistrates";
    private static final String COURT_ORDER_VALIDITY_DATE = "courtOrderValidityDate";

    private static final String SAMPLE_PNC_ID_WITH_SLASH = "2099/1234567L";
    private static final String SAMPLE_PNC_ID_WITHOUT_SLASH = "20991234567L";
    private static final String SAMPLE_PNC_ID_LESS_THAN_12_CHARS = "20991234";
    private static final String SAMPLE_CRO_NUMBER = "123456/20L";
    private static final String SAMPLE_ADDRESS_LINE1 = "addressLine1";
    private static final String SAMPLE_LAST_NAME = "SMITT";
    private static final String SAMPLE_FIRST_NAME = "Teagan";
    private static final LocalDate SAMPLE_DATE_OF_BIRTH = LocalDate.of(1987, 12, 5);

    @Test
    public void shouldGetNextExactCriteria_whenPncIdLessThan12Chars() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_LESS_THAN_12_CHARS, SAMPLE_CRO_NUMBER));

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonFirstCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_PNC_ID_LESS_THAN_12_CHARS, jsonFirstCriteria.getString(PNC_ID));
        assertEquals(SAMPLE_LAST_NAME, jsonFirstCriteria.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFirstCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonSecondCriteria.getString(CRO_NUMBER));
        assertEquals(SAMPLE_LAST_NAME, jsonSecondCriteria.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonSecondCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonThirdCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonThirdCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_FIRST_NAME, jsonThirdCriteria.getString(FIRST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonThirdCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonThirdCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonThirdCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonThirdCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonThirdCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(8, jsonThirdCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldGetNextExactCriteria_whenPncIdWithSlash() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_WITH_SLASH, SAMPLE_CRO_NUMBER));

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonFirstCriteriaFirstSubStep = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITH_SLASH, jsonFirstCriteriaFirstSubStep.getString(PNC_ID));
        assertEquals(SAMPLE_LAST_NAME, jsonFirstCriteriaFirstSubStep.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaFirstSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaFirstSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaFirstSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaFirstSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFirstCriteriaFirstSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonFirstCriteriaSecondSubStep = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITHOUT_SLASH, jsonFirstCriteriaSecondSubStep.getString(PNC_ID));
        assertEquals(SAMPLE_LAST_NAME, jsonFirstCriteriaSecondSubStep.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaSecondSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaSecondSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaSecondSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaSecondSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFirstCriteriaSecondSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonSecondCriteria.getString(CRO_NUMBER));
        assertEquals(SAMPLE_LAST_NAME, jsonSecondCriteria.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonSecondCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonThirdCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonThirdCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_FIRST_NAME, jsonThirdCriteria.getString(FIRST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonThirdCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonThirdCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonThirdCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonThirdCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonThirdCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(8, jsonThirdCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldGetNextExactCriteria_whenPncIdWithoutSlash() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_WITHOUT_SLASH, ""));

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonFirstCriteriaFirstSubStep = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITH_SLASH, jsonFirstCriteriaFirstSubStep.getString(PNC_ID));
        assertEquals(SAMPLE_LAST_NAME, jsonFirstCriteriaFirstSubStep.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaFirstSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaFirstSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaFirstSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaFirstSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFirstCriteriaFirstSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonFirstCriteriaSecondSubStep = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITHOUT_SLASH, jsonFirstCriteriaSecondSubStep.getString(PNC_ID));
        assertEquals(SAMPLE_LAST_NAME, jsonFirstCriteriaSecondSubStep.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaSecondSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaSecondSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaSecondSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaSecondSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFirstCriteriaSecondSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonSecondCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_FIRST_NAME, jsonSecondCriteria.getString(FIRST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonSecondCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(8, jsonSecondCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldNotGetPncIdCriteria_whenPncIdIsEmpty() {
        final Defendant defendant = getSampleDefendant("", SAMPLE_CRO_NUMBER);
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonCriteria.getString(CRO_NUMBER));
        assertEquals(SAMPLE_LAST_NAME, jsonCriteria.getString(LAST_NAME));
        assertEquals(DEFAULT_PAGE_SIZE, jsonCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldNotGetCroNumberCriteria_whenCroNumberIsEmpty() {
        final Defendant defendant = getSampleDefendant("", "");
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);

        assertTrue(matchedDefendantCriteria.nextExactCriteria());
        final JsonObject jsonCriteria = matchedDefendantCriteria.getExactCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_FIRST_NAME, jsonCriteria.getString(FIRST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(8, jsonCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldNotGetAnyExactCriteria_whenDefendantHasNotFirstNameEmptyOrDOBOrAddressline() {
        final Defendant defendantWithoutFirstName = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withDateOfBirth(SAMPLE_DATE_OF_BIRTH)
                                .withAddress(Address.address().withAddress1(SAMPLE_ADDRESS_LINE1).build())
                                .build())

                        .build())
                .build();
        MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendantWithoutFirstName);
        assertFalse(matchedDefendantCriteria.nextExactCriteria());


        final Defendant defendantWithoutDOB = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withFirstName(SAMPLE_FIRST_NAME)
                                .withAddress(Address.address().withAddress1(SAMPLE_ADDRESS_LINE1).build())
                                .build())

                        .build())
                .build();

        matchedDefendantCriteria = new MatchedDefendantCriteria(defendantWithoutDOB);
        assertFalse(matchedDefendantCriteria.nextExactCriteria());

        final Defendant defendantWithoutAddressLine = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withFirstName(SAMPLE_FIRST_NAME)
                                .withDateOfBirth(SAMPLE_DATE_OF_BIRTH)
                                .build())

                        .build())
                .build();

        matchedDefendantCriteria = new MatchedDefendantCriteria(defendantWithoutAddressLine);
        assertFalse(matchedDefendantCriteria.nextExactCriteria());
    }

    @Test
    public void shouldGetNextPartialCriteria_whenPncIdLessThan12Chars() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_LESS_THAN_12_CHARS, SAMPLE_CRO_NUMBER));

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFirstCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_PNC_ID_LESS_THAN_12_CHARS, jsonFirstCriteria.getString(PNC_ID));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonFirstCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonSecondCriteria.getString(CRO_NUMBER));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonSecondCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonThirdCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonThirdCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonThirdCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonThirdCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonThirdCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonThirdCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonThirdCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonThirdCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(7, jsonThirdCriteria.entrySet().size());


        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFourthCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonFourthCriteria.getString(LAST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonFourthCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFourthCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFourthCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFourthCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFourthCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFourthCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFifthCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonThirdCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonFifthCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFifthCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFifthCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFifthCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFifthCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFifthCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    @Test
    public void shouldGetNextPartialCriteria_whenPncIdWithSlash() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_WITHOUT_SLASH, SAMPLE_CRO_NUMBER));

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFirstCriteriaFirstSubStep = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITH_SLASH, jsonFirstCriteriaFirstSubStep.getString(PNC_ID));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaFirstSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaFirstSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaFirstSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaFirstSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonFirstCriteriaFirstSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFirstCriteriaSecondSubStep = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITHOUT_SLASH, jsonFirstCriteriaSecondSubStep.getString(PNC_ID));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaSecondSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaSecondSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaSecondSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaSecondSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonFirstCriteriaSecondSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonSecondCriteria.getString(CRO_NUMBER));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonSecondCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonThirdCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonThirdCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonThirdCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonThirdCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonThirdCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonThirdCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonThirdCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonThirdCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(7, jsonThirdCriteria.entrySet().size());


        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFourthCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonFourthCriteria.getString(LAST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonFourthCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFourthCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFourthCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFourthCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFourthCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFourthCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFifthCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonThirdCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonFifthCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFifthCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFifthCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFifthCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFifthCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFifthCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    @Test
    public void shouldGetNextPartialCriteria_whenPncIdWithoutSlash() {

        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(getSampleDefendant(SAMPLE_PNC_ID_WITHOUT_SLASH, ""));

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFirstCriteriaFirstSubStep = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITH_SLASH, jsonFirstCriteriaFirstSubStep.getString(PNC_ID));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaFirstSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaFirstSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaFirstSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaFirstSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonFirstCriteriaFirstSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFirstCriteriaSecondSubStep = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_PNC_ID_WITHOUT_SLASH, jsonFirstCriteriaSecondSubStep.getString(PNC_ID));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFirstCriteriaSecondSubStep.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFirstCriteriaSecondSubStep.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFirstCriteriaSecondSubStep.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFirstCriteriaSecondSubStep.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonFirstCriteriaSecondSubStep.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonSecondCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonSecondCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonSecondCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonSecondCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonSecondCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonSecondCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonSecondCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonSecondCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(7, jsonSecondCriteria.entrySet().size());


        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonThirdCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonThirdCriteria.getString(LAST_NAME));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonThirdCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonThirdCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonThirdCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonThirdCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonThirdCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonThirdCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonFourthCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonSecondCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonFourthCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonFourthCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonFourthCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonFourthCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonFourthCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(6, jsonFourthCriteria.entrySet().size());

        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    @Test
    public void shouldNotGetPncIdCriteria_whenPncIdIsNull() {
        final Defendant defendant = getSampleDefendant(null, SAMPLE_CRO_NUMBER);
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_CRO_NUMBER, jsonCriteria.getString(CRO_NUMBER));
        assertEquals(DEFAULT_PAGE_SIZE, jsonCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(5, jsonCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    @Test
    public void shouldNotGetCroNumberCriteria_whenCroNumberIsNull() {
        final Defendant defendant = getSampleDefendant("", "");
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        final JsonObject jsonCriteria = matchedDefendantCriteria.getPartialCriteria().build();
        assertEquals(SAMPLE_LAST_NAME, jsonCriteria.getString(LAST_NAME));
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonCriteria.getString(ADDRESS_LINE));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonCriteria.getString(DATE_OF_BIRTH));
        assertEquals(DEFAULT_PAGE_SIZE, jsonCriteria.getInt(PAGE_SIZE));
        assertEquals(DEFAULT_PROCEEDINGS_CONCLUDED, jsonCriteria.getBoolean(PROCEEDINGS_CONCLUDED));
        assertEquals(DEFAULT_COURT_ORDER_VALIDITY_DATE, jsonCriteria.getString(COURT_ORDER_VALIDITY_DATE));
        assertEquals(DEFAULT_CROWN_OR_MAGISTRATES, jsonCriteria.getBoolean(CROWN_OR_MAGISTRATES));
        assertEquals(7, jsonCriteria.entrySet().size());

        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        assertTrue(matchedDefendantCriteria.nextPartialCriteria());
        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    @Test
    public void shouldNotGetAnyPartialCriteria_whenDefendantHasNotFirstNameEmptyOrDOBOrAddressline() {
        final Defendant defendant = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .build())

                        .build())
                .build();
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);
        assertFalse(matchedDefendantCriteria.nextPartialCriteria());
    }

    private Defendant getSampleDefendant(String pncId, String croNumber) {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withMasterDefendantId(UUID.randomUUID())
                .withPncId(pncId)
                .withCroNumber(croNumber)
                .withProceedingsConcluded(true)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withFirstName(SAMPLE_FIRST_NAME)
                                .withDateOfBirth(SAMPLE_DATE_OF_BIRTH)
                                .withAddress(Address.address()
                                        .withAddress1(SAMPLE_ADDRESS_LINE1)
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
