package uk.gov.moj.cpp.progression.helper;

import static javax.json.Json.createObjectBuilder;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S1172", "squid:S1192"})
public class SummonsDataHelper {

    private static final String DELIMETER = " ";
    private static final String PERSON_DEFENDANT = "personDefendant";
    private static final String PERSON_DETAILS = "personDetails";
    private static final String OFFENCES = "offences";
    private static final JsonObject EMPTY_JSON = Json.createObjectBuilder().build();
    private static final JsonArray EMPTY_JSON_ARRAY = Json.createArrayBuilder().build();
    private static final String EMPTY = "";

    private SummonsDataHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject extractDefendant(final JsonObject defendantJson) {
        return createObjectBuilder()
                .add("id", defendantJson.getJsonString("id"))
                .add("name", extractName(defendantJson))
                .add("dateOfBirth", extractStringFromPersonDetails(defendantJson, "dateOfBirth"))
                .add("address", extractJsonObjectFromPersonDetails(defendantJson, "address"))
                .add(OFFENCES, extractOffences(defendantJson))
                .build();
    }

    private static JsonArray extractOffences(final JsonObject defendantJson) {
        return defendantJson.containsKey(OFFENCES) ? defendantJson.getJsonArray(OFFENCES) : EMPTY_JSON_ARRAY;
    }

    private static String extractName(final JsonObject defendantJson) {
        final StringBuilder builder = new StringBuilder();
        builder.append(extractStringFromPersonDetails(defendantJson, "firstName") + DELIMETER);
        builder.append(extractStringFromPersonDetails(defendantJson, "middleName") + DELIMETER);
        builder.append(extractStringFromPersonDetails(defendantJson, "lastName"));
        return builder.toString();
    }

    private static String extractStringFromPersonDetails(final JsonObject defendantJson, String field) {
        return checkIfPersonDetailsFieldExists(defendantJson, field) ?
                defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).getString(field) : EMPTY;
    }

    private static JsonObject extractJsonObjectFromPersonDetails(final JsonObject defendantJson, String field) {
        return checkIfPersonDetailsFieldExists(defendantJson, field) ?
                defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).getJsonObject(field) : EMPTY_JSON;
    }

    private static boolean checkIfPersonDetailsFieldExists(final JsonObject defendantJson, final String field) {
        return defendantJson.containsKey(PERSON_DEFENDANT) && defendantJson.getJsonObject(PERSON_DEFENDANT).containsKey(PERSON_DETAILS)
                && defendantJson.getJsonObject(PERSON_DEFENDANT).getJsonObject(PERSON_DETAILS).containsKey(field);
    }

    public static JsonObject extractReferralReason(final JsonObject referralReasonsJson, String id) {
        return referralReasonsJson.getJsonArray("referralReasons").getValuesAs(JsonObject.class).stream()
                .filter(e -> id.equals(e.getString("id")))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static JsonObject populateRefferal(final JsonObject referralReason) {
        return createObjectBuilder()
                .add("id", referralReason.getString("id"))
                .add("referralReason", referralReason.getString("reason", EMPTY))
                .add("referralReasonWelsh", referralReason.getString("welshReason", EMPTY))
                .add("referralText", referralReason.getString("subReason", EMPTY))
                .add("referralTextWelsh", referralReason.getString("welshSubReason", EMPTY))
                .build();
    }

    public static JsonObject populateCourtCentre(final JsonObject courtCentre) {
        return createObjectBuilder()
                .add("id", courtCentre.getString("id"))
                .add("name", courtCentre.getString("oucodeL1Name"))
                .add("welshName", courtCentre.getString("oucodeL3WelshName", EMPTY))
                .add("address",
                        createObjectBuilder()
                                .add("address1", courtCentre.getString("address1"))
                                .add("address2", courtCentre.getString("address2", EMPTY))
                                .add("address3", courtCentre.getString("address3", EMPTY))
                                .add("address4", courtCentre.getString("address4", EMPTY))
                                .add("address5", courtCentre.getString("address5", EMPTY))
                                .add("postcode", courtCentre.getString("postcode", EMPTY))
                                .build())
                .build();
    }

    public static String extractCaseReference(final JsonObject prosecutionCaseIdentifierJson) {
        return prosecutionCaseIdentifierJson.containsKey("prosecutionAuthorityReference") ?
                prosecutionCaseIdentifierJson.getString("prosecutionAuthorityReference", EMPTY) :
                prosecutionCaseIdentifierJson.getString("caseURN", EMPTY);
    }
}
