package uk.gov.moj.cpp.progression.helper;

import static java.lang.String.format;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.progression.processor.InvalidHearingTimeException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S1172", "squid:S1192","squid:S1166"})
public class SummonsDataHelper {

    private static final String DELIMITER = " ";
    private static final String PERSON_DEFENDANT = "personDefendant";
    private static final String PERSON_DETAILS = "personDetails";
    private static final String ASSOCIATED_PERSONS = "associatedPersons";
    private static final String OFFENCES = "offences";
    private static final String ADDRESS = "address";
    private static final JsonObject EMPTY_JSON = Json.createObjectBuilder().build();
    private static final JsonArray EMPTY_JSON_ARRAY = Json.createArrayBuilder().build();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.TIME_HMMA.getValue());
    private static final ZoneId zid = ZoneId.of("Europe/London");

    private SummonsDataHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject extractDefendant(final JsonObject defendantJson) {
        final JsonObject addressJson = extractJsonObjectFromPersonDetails(defendantJson, "address");
        return createObjectBuilder()
                .add("name", extractName(defendantJson))
                .add("dateOfBirth", extractStringFromPersonDetails(defendantJson, "dateOfBirth"))
                .add("address", populateAddress(addressJson))
                .build();
    }

    public static JsonObject extractAddresse(final JsonObject defendantJson) {
        final JsonObject addressJson = extractJsonObjectFromPersonDetails(defendantJson, "address");
        return createObjectBuilder()
                .add("name", extractName(defendantJson))
                .add("address", populateAddress(addressJson))
                .build();
    }

    public static JsonObject extractGuardianAddresse(final JsonObject defendantJson) {
        final JsonObject guardianJson = extractJsonObjectFromDefendant(defendantJson);
        final JsonObject guardianPersonJson = guardianJson.getJsonObject("person");
        final JsonObject guardianAddressJson = guardianPersonJson.getJsonObject(ADDRESS);
        return createObjectBuilder()
                .add("name", extractNameFromPerson(guardianPersonJson))
                .add("address", populateAddress(guardianAddressJson))
                .build();
    }

    public static JsonObject extractYouth(final JsonObject defendantJson, final boolean isGuardian) {
        final JsonObject guardianJson = extractJsonObjectFromDefendant(defendantJson);
        final JsonObject guardianPersonJson = guardianJson.getJsonObject("person");
        final JsonObject guardianAddressJson = guardianPersonJson.getJsonObject(ADDRESS);
        JsonObject youthContentJson;
        if (isGuardian) {
            youthContentJson = createObjectBuilder()
                    .add("address", populateAddress(guardianAddressJson))
                    .build();
        } else {
            youthContentJson = createObjectBuilder()
                    .add("parentGuardianName", extractNameFromPerson(guardianPersonJson))
                    .add("address", populateAddress(guardianAddressJson))
                    .build();
        }
        return youthContentJson;
    }

    public static JsonArray extractOffences(final JsonObject defendantJson) {
        JsonArrayBuilder updatedOffencesArray = createArrayBuilder();
        JsonArray offencesJsonArray = defendantJson.containsKey(OFFENCES) ? defendantJson.getJsonArray(OFFENCES) : EMPTY_JSON_ARRAY;
        offencesJsonArray.getValuesAs(JsonObject.class).forEach(offence ->
                updatedOffencesArray.add(createObjectBuilder()
                        .add("offenceTitle", extractStringFromOffence(offence, "offenceTitle"))
                        .add("offenceTitleWelsh", extractStringFromOffence(offence, "offenceTitleWelsh"))
                        .add("offenceLegislation", extractStringFromOffence(offence, "offenceLegislation"))
                        .add("offenceLegislationWelsh", extractStringFromOffence(offence, "offenceLegislationWelsh"))
                        .build()));
        return updatedOffencesArray.build();
    }

    public static JsonObject extractReferralReason(final JsonObject referralReasonsJson, String id) {
        return referralReasonsJson.getJsonArray("referralReasons").getValuesAs(JsonObject.class).stream()
                .filter(e -> id.equals(e.getString("id")))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static JsonObject populateReferral(final JsonObject referralReason) {
        return createObjectBuilder()
                .add("id", referralReason.getString("id"))
                .add("referralReason", referralReason.getString("reason", EMPTY))
                .add("referralReasonWelsh", referralReason.getString("welshReason", EMPTY))
                .add("referralText", referralReason.getString("subReason", EMPTY))
                .add("referralTextWelsh", referralReason.getString("welshSubReason", EMPTY))
                .build();
    }

    public static JsonObject populateCourtCentreAddress(final JsonObject courtCentre) {
        return createObjectBuilder()
                .add("line1", courtCentre.getString("address1"))
                .add("line2", courtCentre.getString("address2", EMPTY))
                .add("line3", courtCentre.getString("address3", EMPTY))
                .add("line4", courtCentre.getString("address4", EMPTY))
                .add("line5", courtCentre.getString("address5", EMPTY))
                .add("postCode", courtCentre.getString("postcode", EMPTY))
                .build();
    }

    public static String extractCaseReference(final JsonObject prosecutionCaseIdentifierJson) {
        return prosecutionCaseIdentifierJson.containsKey("prosecutionAuthorityReference") ?
                prosecutionCaseIdentifierJson.getString("prosecutionAuthorityReference", EMPTY) :
                prosecutionCaseIdentifierJson.getString("caseURN", EMPTY);
    }

    private static JsonObject populateAddress(final JsonObject address) {
        return createObjectBuilder()
                .add("line1", address.getString("address1", EMPTY))
                .add("line2", address.getString("address2", EMPTY))
                .add("line3", address.getString("address3", EMPTY))
                .add("line4", address.getString("address4", EMPTY))
                .add("line5", address.getString("address5", EMPTY))
                .add("postCode", address.getString("postcode", EMPTY))
                .build();
    }

    private static String extractName(final JsonObject defendantJson) {
        final StringBuilder builder = new StringBuilder();
        final String firstName = extractStringFromPersonDetails(defendantJson, "firstName");
        builder.append(StringUtils.isNotBlank(firstName) ? firstName + DELIMITER : EMPTY);
        final String middleName = extractStringFromPersonDetails(defendantJson, "middleName");
        builder.append(StringUtils.isNotBlank(middleName) ? middleName + DELIMITER : EMPTY);
        builder.append(extractStringFromPersonDetails(defendantJson, "lastName"));
        return builder.toString();
    }

    private static String extractNameFromPerson(final JsonObject personJson) {
        final StringBuilder builder = new StringBuilder();
        final String firstName = personJson.getString("firstName", EMPTY);
        builder.append(StringUtils.isNotBlank(firstName) ? firstName + DELIMITER : EMPTY);
        final String middleName = personJson.getString("middleName", EMPTY);
        builder.append(StringUtils.isNotBlank(middleName) ? middleName + DELIMITER : EMPTY);
        builder.append(personJson.getString("lastName"));
        return builder.toString();
    }

    private static String extractStringFromOffence(final JsonObject offenceJson, String field) {
        return offenceJson.getString(field, EMPTY);
    }

    private static String extractStringFromPersonDetails(final JsonObject defendantJson, String field) {
        return checkIfPersonDetailsFieldExists(defendantJson, field) ?
                defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).getString(field) : EMPTY;
    }

    private static JsonObject extractJsonObjectFromPersonDetails(final JsonObject defendantJson, String field) {
        return checkIfPersonDetailsFieldExists(defendantJson, field) ?
                defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).getJsonObject(field) : EMPTY_JSON;
    }

    private static JsonObject extractJsonObjectFromDefendant(final JsonObject defendantJson) {
        return defendantJson.getJsonArray(ASSOCIATED_PERSONS).getValuesAs(JsonObject.class).stream()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private static boolean checkIfPersonDetailsFieldExists(final JsonObject defendantJson, final String field) {
        return defendantJson.containsKey(PERSON_DEFENDANT) && defendantJson.getJsonObject(PERSON_DEFENDANT).containsKey(PERSON_DETAILS)
                && defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).containsKey(field);
    }

    public static String getCourtTime(final ZonedDateTime hearingDateTime) {
        String courtTime;
        try {
            courtTime = TIME_FORMATTER.format(hearingDateTime.plusSeconds(
                    hearingDateTime.withZoneSameInstant(zid).getOffset().getTotalSeconds()));
        } catch (DateTimeException dte) {
            throw new InvalidHearingTimeException(format("Invalid hearing date time [ %s ] for generating notification / summon ", hearingDateTime));

        }
        return courtTime;
    }
}
