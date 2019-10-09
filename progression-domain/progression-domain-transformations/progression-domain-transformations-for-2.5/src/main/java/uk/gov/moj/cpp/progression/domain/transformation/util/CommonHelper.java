package uk.gov.moj.cpp.progression.domain.transformation.util;

import static java.util.Objects.isNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@SuppressWarnings({"squid:S1155", "squid:S1188", "squid:S3776"})
public class CommonHelper {
    public static final String LEVEL = "level";
    public static final String OFFENCE = "offence";
    public static final String OFFENCES = "offences";
    public static final String DEFENDANT = "defendant";
    public static final String CASE = "case";
    public static final String ID = "id";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String NUMBER_OF_PREVIOUS_CONVICTIONS_CITED = "numberOfPreviousConvictionsCited";
    public static final String PROSECUTION_AUTHORITY_REFERENCE = "prosecutionAuthorityReference";
    public static final String WITNESS_STATEMENT = "witnessStatement";
    public static final String WITNESS_STATEMENT_WELSH = "witnessStatementWelsh";
    public static final String MITIGATION = "mitigation";
    public static final String CRO_NUMBER = "croNumber";
    public static final String PERSON_DEFENDANT = "personDefendant";
    public static final String PNC_ID = "pncId";
    public static final String ASSOCIATED_PERSONS = "associatedPersons";
    public static final String DEFENCE_ORGANISATION = "defenceOrganisation";
    public static final String LEGAL_ENTITY_DEFENDANT = "legalEntityDefendant";
    public static final String ALIASES = "aliases";
    public static final String JUDICIAL_RESULTS = "judicialResults";
    public static final String ROLE = "role";
    public static final String PERSON = "person";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String HEARING_DAYS = "hearingDays";
    public static final String JURISDICTION_TYPE = "jurisdictionType";
    public static final String TYPE = "type";
    public static final String DEFENDANT_REFERRAL_REASONS = "defendantReferralReasons";
    public static final String HEARING_LANGUAGE = "hearingLanguage";
    public static final String JUDICIARY = "judiciary";
    public static final String PROSECUTION_CASES = "prosecutionCases";
    public static final String REPORTING_RESTRICTION_REASON = "reportingRestrictionReason";
    public static final String HAS_SHARED_RESULTS = "hasSharedResults";
    public static final String COURT_APPLICATIONS = "courtApplications";
    public static final String HEARING_CASE_NOTES = "hearingCaseNotes";
    public static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    public static final String DEFENCE_COUNSELS = "defenceCounsels";
    public static final String APPLICATION_PARTY_COUNSELS = "applicationPartyCounsels";
    public static final String DEFENDANT_ATTENDANCE = "defendantAttendance";
    public static final String COURT_APPLICATION_PARTY_ATTENDANCE = "courtApplicationPartyAttendance";
    public static final String CRACKED_INEFFECTIVE_TRIAL = "crackedIneffectiveTrial";
    public static final String ORIGINATING_HEARING_ID = "originatingHearingId";
    public static final String NOTE_DATE_TIME = "noteDateTime";
    public static final String NOTE_TYPE = "noteType";
    public static final String NOTE = "note";
    public static final String COURT_CLERK = "courtClerk";
    public static final String JUDICIAL_RESULT_ID = "judicialResultId";
    public static final String ORDERED_HEARING_ID = "orderedHearingId";
    public static final String LABEL = "label";
    public static final String IS_ADJOURNMENT_RESULT = "isAdjournmentResult";
    public static final String IS_FINANCIAL_RESULT = "isFinancialResult";
    public static final String IS_CONVICTED_RESULT = "isConvictedResult";
    public static final String IS_AVAILABLE_FOR_COURT_EXTRACT = "isAvailableForCourtExtract";
    public static final String ORDERED_DATE = "orderedDate";
    public static final String CATEGORY = "category";
    public static final String WELSH_LABEL = "welshLabel";
    public static final String C_JS_CODE = "cJSCode";
    public static final String RANK = "rank";
    public static final String LAST_SHARED_DATE_TIME = "lastSharedDateTime";
    public static final String USERGROUPS = "usergroups";
    public static final String JUDICIAL_RESULT_PROMPTS = "judicialResultPrompts";
    public static final String MODE_OF_TRIAL = "modeOfTrial";
    public static final String DATE_OF_INFORMATION = "dateOfInformation";
    public static final String CONVICTION_DATE = "convictionDate";
    public static final String NOTIFIED_PLEA = "notifiedPlea";
    public static final String INDICATED_PLEA = "indicatedPlea";
    public static final String PLEA = "plea";
    public static final String WORDING_WELSH = "wordingWelsh";
    public static final String END_DATE = "endDate";
    public static final String ARREST_DATE = "arrestDate";
    public static final String CHARGE_DATE = "chargeDate";
    public static final String ORDER_INDEX = "orderIndex";
    public static final String OFFENCE_TITLE_WELSH = "offenceTitleWelsh";
    public static final String OFFENCE_LEGISLATION = "offenceLegislation";
    public static final String OFFENCE_LEGISLATION_WELSH = "offenceLegislationWelsh";
    public static final String VERDICT = "verdict";
    public static final String OFFENCE_ID = "offenceId";
    public static final String VERDICT_DATE = "verdictDate";
    public static final String VERDICT_TYPE = "verdictType";
    public static final String JURORS = "jurors";
    public static final String LESSER_OR_ALTERNATIVE_OFFENCE = "lesserOrAlternativeOffence";
    public static final String CATEGORY_TYPE = "categoryType";
    public static final String SEQUENCE = "sequence";
    public static final String DESCRIPTION = "description";
    public static final String OFFENCE_FACTS = "offenceFacts";
    public static final String VEHICLE_REGISTRATION = "vehicleRegistration";
    public static final String ALCOHOL_READING_AMOUNT = "alcoholReadingAmount";
    public static final String ALCOHOL_READING_METHOD = "alcoholReadingMethod";
    public static final String OFFENCE_DEFINITION_ID = "offenceDefinitionId";
    public static final String OFFENCE_CODE = "offenceCode";
    public static final String OFFENCE_TITLE = "offenceTitle";
    public static final String WORDING = "wording";
    public static final String START_DATE = "startDate";
    public static final String COUNT = "count";
    public static final String SHARED_HEARING_LINES = "sharedHearingLines";
    public static final String VERDICT_TYPE_ID = "verdictTypeId";
    public static final String PERSON_DETAILS = "personDetails";
    public static final String BAIL_STATUS = "bailStatus";
    public static final String CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    public static final String PERCEIVED_BIRTH_YEAR = "perceivedBirthYear";
    public static final String DRIVER_NUMBER = "driverNumber";
    public static final String ARREST_SUMMONS_NUMBER = "arrestSummonsNumber";
    public static final String EMPLOYER_ORGANISATION = "employerOrganisation";
    public static final String EMPLOYER_PAYROLL_REFERENCE = "employerPayrollReference";
    public static final String GENDER = "gender";
    public static final String TITLE = "title";
    public static final String FIRST_NAME = "firstName";
    public static final String MIDDLE_NAME = "middleName";
    public static final String LAST_NAME = "lastName";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String NATIONALITY_ID = "nationalityId";
    public static final String NATIONALITY_CODE = "nationalityCode";
    public static final String NATIONALITY_DESCRIPTION = "nationalityDescription";
    public static final String ADDITIONAL_NATIONALITY_ID = "additionalNationalityId";
    public static final String ADDITIONAL_NATIONALITY_CODE = "additionalNationalityCode";
    public static final String ADDITIONAL_NATIONALITY_DESCRIPTION = "additionalNationalityDescription";
    public static final String DISABILITY_STATUS = "disabilityStatus";
    public static final String INTERPRETER_LANGUAGE_NEEDS = "interpreterLanguageNeeds";
    public static final String DOCUMENTATION_LANGUAGE_NEEDS = "documentationLanguageNeeds";
    public static final String NATIONAL_INSURANCE_NUMBER = "nationalInsuranceNumber";
    public static final String OCCUPATION = "occupation";
    public static final String OCCUPATION_CODE = "occupationCode";
    public static final String SPECIFIC_REQUIREMENTS = "specificRequirements";
    public static final String ADDRESS = "address";
    public static final String CONTACT = "contact";
    public static final String SELF_ETHNICITY_ID = "selfEthnicityId";
    public static final String OBSERVED_ETHNICITY_ID = "observedEthnicityId";
    public static final String ETHNICITY = "ethnicity";
    public static final String OBSERVED_ETHNICITY_CODE = "observedEthnicityCode";
    public static final String OBSERVED_ETHNICITY_DESCRIPTION = "observedEthnicityDescription";
    public static final String SELF_DEFINED_ETHNICITY_CODE = "selfDefinedEthnicityCode";
    public static final String SELF_DEFINED_ETHNICITY_DESCRIPTION = "selfDefinedEthnicityDescription";
    public static final String INITIATION_CODE = "initiationCode";
    public static final String PROSECUTION_CASE_IDENTIFIER = "prosecutionCaseIdentifier";
    public static final String DEFENDANTS = "defendants";
    public static final String ORIGINATING_ORGANISATION = "originatingOrganisation";
    public static final String CASE_STATUS = "caseStatus";
    public static final String STATEMENT_OF_FACTS = "statementOfFacts";
    public static final String STATEMENT_OF_FACTS_WELSH = "statementOfFactsWelsh";
    public static final String BREACH_PROCEEDINGS_PENDING = "breachProceedingsPending";
    public static final String APPEAL_PROCEEDINGS_PENDING = "appealProceedingsPending";
    public static final String POLICE_OFFICER_IN_CASE = "policeOfficerInCase";
    public static final String CASE_MARKERS = "caseMarkers";
    private static final String USER_ID = "userId";

    private CommonHelper() {
    }

    public static JsonObject transformDelegatePowers(final JsonObject existingCourtClerk) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(FIRST_NAME, existingCourtClerk.getJsonString(FIRST_NAME))
                .add(LAST_NAME, existingCourtClerk.getJsonString(LAST_NAME));
        if (existingCourtClerk.containsKey("id")) {
            jsonObjectBuilder.add(USER_ID, existingCourtClerk.getJsonString("id"));
        } else {
            jsonObjectBuilder.add(USER_ID, existingCourtClerk.getJsonString(USER_ID));
        }
        return jsonObjectBuilder.build();
    }

    public static Map<String, JsonArray> arrangeSharedResultLineByLevel(final JsonArray sharedResultLines) {
        final Map<String, JsonArray> sharedResultByLevel = new HashMap<>();
        if (sharedResultLines != null && sharedResultLines.size() > 0) {
            sharedResultLines.forEach(resultLine -> {
                final JsonObject result = (JsonObject) resultLine;
                if (result.getString(LEVEL).equalsIgnoreCase(OFFENCE)) {
                    final JsonArray offenceArray = sharedResultByLevel.get(OFFENCE);
                    if (isNull(offenceArray)) {
                        sharedResultByLevel.put(OFFENCE, createArrayBuilder().add(result).build());
                    } else {
                        sharedResultByLevel.put(OFFENCE, addToJsonArray(offenceArray, result));
                    }
                } else if (result.getString(LEVEL).equalsIgnoreCase(DEFENDANT)) {
                    final JsonArray defendantArray = sharedResultByLevel.get(DEFENDANT);
                    if (isNull(defendantArray)) {
                        sharedResultByLevel.put(DEFENDANT, createArrayBuilder().add(result).build());
                    } else {
                        sharedResultByLevel.put(DEFENDANT, addToJsonArray(defendantArray, result));
                    }
                } else {
                    final JsonArray caseArray = sharedResultByLevel.get(CASE);
                    if (isNull(caseArray)) {
                        sharedResultByLevel.put(DEFENDANT, createArrayBuilder().add(result).build());
                    } else {
                        sharedResultByLevel.put(DEFENDANT, addToJsonArray(caseArray, result));
                    }
                }
            });
        }
        return sharedResultByLevel;
    }

    private static JsonArray addToJsonArray(final JsonArray offenceArray, final JsonValue jsonValue) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        IntStream.range(0, offenceArray.size()).mapToObj(offenceArray::getJsonObject).forEach(jsonArrayBuilder::add);
        jsonArrayBuilder.add(jsonValue);
        return jsonArrayBuilder.build();
    }
}
