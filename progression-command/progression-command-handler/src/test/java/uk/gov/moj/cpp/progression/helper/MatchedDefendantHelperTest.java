package uk.gov.moj.cpp.progression.helper;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Defendants;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.additionalproperties.AdditionalPropertiesModule;
import uk.gov.justice.services.common.converter.jackson.jsr353.InclusionAwareJSR353Module;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MatchedDefendantHelperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final UUID SAMPLE_DEFENDANT_ID = UUID.randomUUID();
    private static final UUID SAMPLE_MASTER_DEFENDANT_ID = UUID.randomUUID();
    private static final String SAMPLE_PNC_ID = "2099/1234567L";
    private static final String SAMPLE_CRO_NUMBER = "123456/20L";
    private static final String SAMPLE_LAST_NAME = "SMITT";
    private static final String SAMPLE_FIRST_NAME = "Teagan";
    private static final String SAMPLE_CASE_URN = "99AB21233";
    private static final String SAMPLE_MIDDLE_NAME = "M.";
    private static final String SAMPLE_POSTCODE = "HA1 1QF";
    private static final String SAMPLE_PROSECUTION_AUTHORITY_REFERENCE = "PAR1234567";
    private static final String SAMPLE_ADDRESS_LINE1 = "addressLine1";
    private static final String SAMPLE_ADDRESS_LINE2 = "addressLine2";
    private static final String SAMPLE_ADDRESS_LINE3 = "addressLine3";
    private static final String SAMPLE_ADDRESS_LINE4 = "addressLine4";
    private static final String SAMPLE_ADDRESS_LINE5 = "addressLine5";
    private static final LocalDate SAMPLE_DATE_OF_BIRTH = LocalDate.of(1987, 12, 5);
    private static final ZonedDateTime SAMPLE_COURT_PROCEEDINGS_INITIATED = new UtcClock().now();

    private static final UUID SAMPLE_CASE_DEFENDANT_ID = UUID.randomUUID();
    private static final UUID SAMPLE_CASE_MASTER_DEFENDANT_ID = UUID.randomUUID();
    private static final UUID SAMPLE_CASE_PROSECUTION_ID = UUID.randomUUID();
    private static final String SAMPLE_CASE_REFERENCE = "PAR1234568";
    private static final String SAMPLE_CASE_PNC_ID = "2099/1234567M";
    private static final String SAMPLE_CASE_CRO_NUMBER = "123456/20M";
    private static final String SAMPLE_CASE_LAST_NAME = "SMIT";
    private static final String SAMPLE_CASE_FIRST_NAME = "Ten";
    private static final String SAMPLE_CASE_MIDDLE_NAME = "N.";
    private static final String SAMPLE_CASE_DATE_OF_BIRTH = "1988-12-5";
    private static final String SAMPLE_CASE_ADDRESS_LINE1 = "addressLine1_";
    private static final String SAMPLE_CASE_ADDRESS_LINE2 = "addressLine2_";
    private static final String SAMPLE_CASE_ADDRESS_LINE3 = "addressLine3_";
    private static final String SAMPLE_CASE_ADDRESS_LINE4 = "addressLine4_";
    private static final String SAMPLE_CASE_ADDRESS_LINE5 = "addressLine5_";
    private static final String SAMPLE_CASE_POSTCODE = "HA1 1QG";
    private static final ZonedDateTime SAMPLE_CASE_COURT_PROCEEDINGS_INITIATED = new UtcClock().now().plusDays(-1);
    public static final UUID SAMPLE_PROSECUTION_ID = UUID.randomUUID();

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private MatchedDefendantHelper matchedDefendantHelper;

    @BeforeEach
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule(PROPERTIES))
                .registerModule(new JavaTimeModule())
                .registerModule(new InclusionAwareJSR353Module())
                .registerModule(new AdditionalPropertiesModule());

        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter);

    }

    @Test
    public void shouldTransformToPayload() {
        final Defendant defendant = getSampleDefendant(SAMPLE_MIDDLE_NAME);
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE);
        final List<Cases> casesList = getSampleCasesList();

        final String payload = matchedDefendantHelper.transformToPartialMatchDefendantPayload(defendant, prosecutionCase, casesList);
        final JsonObject jsonPayload = stringToJsonObjectConverter.convert(payload);

        assertEquals(SAMPLE_DEFENDANT_ID.toString(), jsonPayload.getString("defendantId"));
        assertEquals(SAMPLE_MASTER_DEFENDANT_ID.toString(), jsonPayload.getString("masterDefendantId"));
        assertEquals(SAMPLE_PROSECUTION_ID.toString(), jsonPayload.getString("prosecutionCaseId"));
        assertEquals(SAMPLE_CASE_URN, jsonPayload.getString("caseReference"));
        assertEquals(SAMPLE_FIRST_NAME, jsonPayload.getString("firstName"));
        assertEquals(SAMPLE_MIDDLE_NAME, jsonPayload.getString("middleName"));
        assertEquals(SAMPLE_LAST_NAME, jsonPayload.getString("lastName"));
        assertEquals(FORMATTER.format(SAMPLE_DATE_OF_BIRTH), jsonPayload.getString("dateOfBirth"));
        assertEquals(ZONE_DATETIME_FORMATTER.format(SAMPLE_COURT_PROCEEDINGS_INITIATED), jsonPayload.getString("courtProceedingsInitiated"));
        assertEquals(SAMPLE_PNC_ID, jsonPayload.getString("pncId"));
        assertEquals(SAMPLE_CRO_NUMBER, jsonPayload.getString("croNumber"));
        assertEquals(SAMPLE_ADDRESS_LINE1, jsonPayload.getJsonObject("address").getString("addressLine1"));
        assertEquals(SAMPLE_ADDRESS_LINE2, jsonPayload.getJsonObject("address").getString("addressLine2"));
        assertEquals(SAMPLE_ADDRESS_LINE3, jsonPayload.getJsonObject("address").getString("addressLine3"));
        assertEquals(SAMPLE_ADDRESS_LINE4, jsonPayload.getJsonObject("address").getString("addressLine4"));
        assertEquals(SAMPLE_ADDRESS_LINE5, jsonPayload.getJsonObject("address").getString("addressLine5"));
        assertEquals(SAMPLE_POSTCODE, jsonPayload.getJsonObject("address").getString("postcode"));
        assertEquals(1, jsonPayload.getInt("defendantsMatchedCount"));

        final JsonObject defendantsMatched = (JsonObject) jsonPayload.getJsonArray("defendantsMatched").get(0);
        assertEquals(SAMPLE_CASE_DEFENDANT_ID.toString(), defendantsMatched.getString("defendantId"));
        assertEquals(SAMPLE_CASE_PROSECUTION_ID.toString(), defendantsMatched.getString("prosecutionCaseId"));
        assertEquals(SAMPLE_CASE_REFERENCE, defendantsMatched.getString("caseReference"));
        assertEquals(SAMPLE_CASE_MASTER_DEFENDANT_ID.toString(), defendantsMatched.getString("masterDefendantId"));
        assertEquals(SAMPLE_CASE_CRO_NUMBER, defendantsMatched.getString("croNumber"));
        assertEquals(SAMPLE_CASE_DATE_OF_BIRTH, defendantsMatched.getString("dateOfBirth"));
        assertEquals(SAMPLE_CASE_FIRST_NAME, defendantsMatched.getString("firstName"));
        assertEquals(SAMPLE_CASE_MIDDLE_NAME, defendantsMatched.getString("middleName"));
        assertEquals(SAMPLE_CASE_LAST_NAME, defendantsMatched.getString("lastName"));
        assertEquals(SAMPLE_CASE_PNC_ID, defendantsMatched.getString("pncId"));

    }

    @Test
    public void shouldTransformToPayload_whenCaseUrnIsNull() {
        final Defendant defendant = getSampleDefendant(SAMPLE_MIDDLE_NAME);
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(null, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE);
        final List<Cases> casesList = getSampleCasesList();

        final String payload = matchedDefendantHelper.transformToPartialMatchDefendantPayload(defendant, prosecutionCase, casesList);
        final JsonObject jsonPayload = stringToJsonObjectConverter.convert(payload);
        assertEquals(SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, jsonPayload.getString("caseReference"));
    }


    @Test
    public void shouldAddMiddleNameToDefendantName_whenMiddleNameExists() {
        final Defendant defendant = getSampleDefendant(SAMPLE_MIDDLE_NAME);
        final String defendantName = matchedDefendantHelper.getDefendantName(defendant);
        assertEquals(SAMPLE_FIRST_NAME + " " + SAMPLE_MIDDLE_NAME + " " + SAMPLE_LAST_NAME, defendantName);
    }

    @Test
    public void shouldNotAddMiddleNameToDefendantName_whenMiddleNameNonExists() {
        final Defendant defendant = getSampleDefendant("");
        final String defendantName = matchedDefendantHelper.getDefendantName(defendant);
        assertEquals(SAMPLE_FIRST_NAME + " " + SAMPLE_LAST_NAME, defendantName);
    }

    @Test
    public void shouldAddToJsonObjectNullSafe() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        final String stringValue = null;
        matchedDefendantHelper.addToJsonObjectNullSafe(builder, "key", stringValue);
        final UUID uuidValue = null;
        matchedDefendantHelper.addToJsonObjectNullSafe(builder, "key", uuidValue);
        final ZonedDateTime dateValue = null;
        matchedDefendantHelper.addToJsonObjectNullSafe(builder, "key", dateValue);
    }

    private Defendant getSampleDefendant(String middleName) {
        return Defendant.defendant()
                .withId(SAMPLE_DEFENDANT_ID)
                .withMasterDefendantId(SAMPLE_MASTER_DEFENDANT_ID)
                .withPncId(SAMPLE_PNC_ID)
                .withCroNumber(SAMPLE_CRO_NUMBER)
                .withCourtProceedingsInitiated(SAMPLE_COURT_PROCEEDINGS_INITIATED)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withFirstName(SAMPLE_FIRST_NAME)
                                .withMiddleName(middleName)
                                .withDateOfBirth(SAMPLE_DATE_OF_BIRTH)
                                .withAddress(Address.address()
                                        .withPostcode(SAMPLE_POSTCODE)
                                        .withAddress1(SAMPLE_ADDRESS_LINE1)
                                        .withAddress2(SAMPLE_ADDRESS_LINE2)
                                        .withAddress3(SAMPLE_ADDRESS_LINE3)
                                        .withAddress4(SAMPLE_ADDRESS_LINE4)
                                        .withAddress5(SAMPLE_ADDRESS_LINE5)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private ProsecutionCase getSampleProsecutionCase(final String caseUrn, final String prosecutionAuthorityReference) {
        return ProsecutionCase.prosecutionCase()
                .withId(SAMPLE_PROSECUTION_ID)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                        .build())
                .build();
    }

    private List<Cases> getSampleCasesList() {
        return Arrays.asList(Cases.cases()
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withCourtProceedingsInitiated(SAMPLE_CASE_COURT_PROCEEDINGS_INITIATED)
                        .withCroNumber(SAMPLE_CASE_CRO_NUMBER)
                        .withDateOfBirth(SAMPLE_CASE_DATE_OF_BIRTH)
                        .withDefendantId(SAMPLE_CASE_DEFENDANT_ID.toString())
                        .withFirstName(SAMPLE_CASE_FIRST_NAME)
                        .withLastName(SAMPLE_CASE_LAST_NAME)
                        .withMiddleName(SAMPLE_CASE_MIDDLE_NAME)
                        .withMasterDefendantId(SAMPLE_CASE_MASTER_DEFENDANT_ID.toString())
                        .withPncId(SAMPLE_CASE_PNC_ID)
                        .withAddress(Address.address()
                                .withPostcode(SAMPLE_CASE_POSTCODE)
                                .withAddress1(SAMPLE_CASE_ADDRESS_LINE1)
                                .withAddress2(SAMPLE_CASE_ADDRESS_LINE2)
                                .withAddress3(SAMPLE_CASE_ADDRESS_LINE3)
                                .withAddress4(SAMPLE_CASE_ADDRESS_LINE4)
                                .withAddress5(SAMPLE_CASE_ADDRESS_LINE5)
                                .build())
                        .build()))
                .withCaseReference(SAMPLE_CASE_REFERENCE)
                .withProsecutionCaseId(SAMPLE_CASE_PROSECUTION_ID.toString())
                .build());
    }

}
