package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

public class CourtRegisterPdfPayloadGenerator {
    private static final String ADDRESS = "address";
    private static final String PARENT_GUARDIAN = "parentGuardian";
    private static final String PLEA_DATE = "pleaDate";
    private static final String SEPARATOR = ", ";
    private static final String DASH = "-";
    private static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    private static final DateTimeFormatter IN_DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
    private static final DateTimeFormatter OUT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue());
    private static final String CJS_RESULT_CODE = "cjsResultCode";
    private static final String RESULT_CODE = "resultCode";
    private static final String RESULT_TEXT = "resultText";
    private static final String RESULTS = "results";
    private static final String POSTCODE = "postCode";
    private static final String CONVICTION_DATE = "convictionDate";
    public static final String BLANK = " ";
    public static final String NEW_LINE = "\n";
    public static final String DESIRED_NEW_LINE = "####";

    @SuppressWarnings({"squid:S1188", "squid:S1192"})
    public JsonObject mapPayload(final JsonObject jsonObject) {
        final JsonObjectBuilder payloadForPdf = JsonObjects.createObjectBuilder();
        jsonObject.getJsonArray("courtRegisterDocumentRequests").stream().findAny().map(JsonObject.class::cast)
                .ifPresent(json -> {
                    payloadForPdf.add("registerDate", formatZonedDate(json.getString("registerDate")));
                    Optional.ofNullable(json.getJsonObject("hearingVenue")).ifPresent(hearingVenue -> {
                        if (hearingVenue.containsKey("ljaName")) {
                            payloadForPdf.add("ljaName", hearingVenue.getString("ljaName"));
                        }
                        payloadForPdf.add("courtHouse", hearingVenue.getString("courtHouse", DASH));
                        Optional.ofNullable(hearingVenue.getJsonObject("address")).ifPresent(
                                address -> payloadForPdf.add("courtHouseAddress", buildCourtHouseAddress(address))
                        );
                    });
                });

        final JsonArrayBuilder caseArray = JsonObjects.createArrayBuilder();
        jsonObject.getJsonArray("courtRegisterDocumentRequests").stream().map(JsonObject.class::cast)
                .forEach(courtRegisterDocumentRequest -> courtRegisterDocumentRequest.getJsonArray("defendants").stream().map(r -> (JsonObject) r)
                        .forEach(defendant -> defendant.getJsonArray("prosecutionCasesOrApplications").stream().map(JsonObject.class::cast).collect(Collectors.toList())
                                .forEach(pcoa -> {
                                    final JsonObjectBuilder caseJson = JsonObjects.createObjectBuilder();
                                    caseJson.add("defendantType", courtRegisterDocumentRequest.getString("defendantType", ""));
                                    buildNameAndAddress(defendant, caseJson);
                                    buildHearingDetails(defendant, caseJson);
                                    buildDefenceCounsel(defendant, caseJson);

                                    caseJson.add("postHearingCustodyStatus", defendant.getString("postHearingCustodyStatus", DASH));
                                    caseJson.add("officerInCase", DASH);
                                    buildParentGuardianNameAndAddress(defendant, caseJson);
                                    caseJson.add("dateOfBirth", formatDate(defendant.getString("dateOfBirth", DASH)));
                                    caseJson.add("age", getAge(defendant));
                                    caseJson.add("gender", defendant.getString("gender", DASH));
                                    caseJson.add("nationality", defendant.getString("nationality", DASH));
                                    caseJson.add("aliases", buildAliases(defendant));
                                    caseJson.add("caseReference", pcoa.getString("caseOrApplicationReference", DASH));
                                    caseJson.add("dateOfHearing", formatZonedDate(courtRegisterDocumentRequest.getString("hearingDate")));
                                    caseJson.add("prosecutorName", pcoa.getString("prosecutorName", DASH));
                                    caseJson.add("arrestSummonsNumber", pcoa.getString("arrestSummonsNumber", DASH));
                                    buildProsecutionCounsel(pcoa, caseJson);
                                    buildDefendantResults(defendant, caseJson);
                                    buildCaseResults(pcoa, caseJson);
                                    buildOffences(pcoa, caseJson);
                                    buildApplication(pcoa, caseJson);
                                    caseArray.add(caseJson.build());
                                })));
        payloadForPdf.add("cases", caseArray);

        return payloadForPdf.build();
    }

    private void buildDefendantResults(JsonObject defendant, JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        Optional.ofNullable(defendant.getJsonArray("defendantResults")).ifPresent(results -> {
            final List<JsonObject> resultList = results.stream().map(JsonObject.class::cast).collect(Collectors.toList());
            resultList.forEach(result -> {
                final JsonObjectBuilder resultBuilder = JsonObjects.createObjectBuilder()
                        .add(RESULT_CODE, result.getString(CJS_RESULT_CODE, DASH))
                        .add(RESULT_TEXT, prepareResultText(result.getString(RESULT_TEXT, "")));
                jsonArrayBuilder.add(resultBuilder.build());
            });
        });

        final JsonArray defendantResults = jsonArrayBuilder.build();
        if (!defendantResults.isEmpty()) {
            caseJson.add("defendantResults", defendantResults);
        }
    }

    private void buildCaseResults(JsonObject pcoa, JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        Optional.ofNullable(pcoa.getJsonArray(RESULTS)).ifPresent(results -> results.stream().map(JsonObject.class::cast).forEach(result -> {
            final JsonObjectBuilder resultBuilder = JsonObjects.createObjectBuilder()
                    .add(RESULT_CODE, result.getString(CJS_RESULT_CODE, DASH))
                    .add(RESULT_TEXT, prepareResultText(result.getString(RESULT_TEXT, "")));
            jsonArrayBuilder.add(resultBuilder.build());
        }));

        final JsonArray caseResults = jsonArrayBuilder.build();
        if (!caseResults.isEmpty()) {
            caseJson.add("caseResults", caseResults);
        }
    }

    private void buildApplication(final JsonObject pcoaJson, JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();

        if (isApplicationValid(pcoaJson)) {
            final JsonObject application = JsonObjects.createObjectBuilder()
                    .add("type", pcoaJson.getString("applicationType", DASH))
                    .add("decision", pcoaJson.getString("applicationDecision", DASH))
                    .add("decisionDate", formatDate(pcoaJson.getString("applicationDecisionDate", DASH)))
                    .add("response", pcoaJson.getString("applicationResponse", DASH))
                    .add("responseDate", formatDate(pcoaJson.getString("applicationResponseDate", DASH)))
                    .add("result", prepareResultText(pcoaJson.getString("applicationResult", DASH)))
                    .build();
            jsonArrayBuilder.add(application);
        }

        final JsonArray applications = jsonArrayBuilder.build();
        if (!applications.isEmpty()) {
            caseJson.add("applications", applications);
        }
    }

    private void buildHearingDetails(JsonObject defendant, JsonObjectBuilder caseJson) {

        final String jurisdiction = "jurisdiction";
        final String hearingType = "hearingType";
        final String defendantAppearanceDetails = "defendantAppearanceDetails";
        final String attendingSolicitorName = "attendingSolicitorName";
        if (defendant.containsKey("hearing")) {
            final JsonObject defendantHearing = defendant.getJsonObject("hearing");
            caseJson.add(jurisdiction, defendantHearing.getString(jurisdiction, DASH));
            caseJson.add(hearingType, defendantHearing.getString(hearingType, DASH));
            caseJson.add(defendantAppearanceDetails, defendantHearing.getString(defendantAppearanceDetails, DASH));
            caseJson.add(attendingSolicitorName, defendantHearing.getString(attendingSolicitorName, DASH));
        } else {
            caseJson.add(jurisdiction, DASH);
            caseJson.add(hearingType, DASH);
            caseJson.add(defendantAppearanceDetails, DASH);
            caseJson.add(attendingSolicitorName, DASH);
        }
    }

    private void buildNameAndAddress(JsonObject defendant, JsonObjectBuilder caseJson) {
        caseJson.add("name", defendant.getString("name", DASH));
        Optional.ofNullable(defendant.getJsonObject(ADDRESS)).ifPresent(addressObj -> {
            Stream.of("1", "2", "3", "4", "5").map(a -> ADDRESS + a).forEach(a -> caseJson.add(a, addressObj.getString(a, "")));
            caseJson.add(POSTCODE, addressObj.getString(POSTCODE, ""));
        });
    }

    private void buildParentGuardianNameAndAddress(JsonObject defendant, JsonObjectBuilder caseJson) {
        if (defendant.containsKey(PARENT_GUARDIAN)) {
            final JsonObject parentGuardian = defendant.getJsonObject(PARENT_GUARDIAN);
            caseJson.add("parentGuardianName", parentGuardian.getString("name", DASH));
            final JsonObject addressObj = parentGuardian.getJsonObject("address");
            Stream.of("1", "2", "3", "4", "5").map(a -> ADDRESS + a)
                    .forEach(a -> caseJson.add(PARENT_GUARDIAN + StringUtils.capitalize(a), addressObj.getString(a, "")));
            caseJson.add("parentGuardianPostCode", addressObj.getString(POSTCODE, ""));
        } else {
            caseJson.add("parentGuardianName", DASH);
            caseJson.add("parentGuardianAddress1", DASH);
        }
    }

    private void buildProsecutionCounsel(JsonObject pcoa, JsonObjectBuilder caseJson) {
        if (pcoa.containsKey(PROSECUTION_COUNSELS)) {
            final String prosecutionCounselName = pcoa.getJsonArray(PROSECUTION_COUNSELS).stream()
                    .map(JsonObject.class::cast)
                    .map(r -> r.getString("name", ""))
                    .collect(Collectors.joining(SEPARATOR));
            final String prosecutionCounselStatus = pcoa.getJsonArray(PROSECUTION_COUNSELS).stream()
                    .map(JsonObject.class::cast)
                    .map(r -> r.getString("status", ""))
                    .collect(Collectors.joining(SEPARATOR));
            caseJson.add("prosecutionCounselName", prosecutionCounselName);
            caseJson.add("prosecutionCounselStatus", prosecutionCounselStatus);
        } else {
            caseJson.add("prosecutionCounselName", DASH);
            caseJson.add("prosecutionCounselStatus", DASH);
        }
    }

    private void buildDefenceCounsel(JsonObject defendant, JsonObjectBuilder caseJson) {
        if (defendant.containsKey("defenceCounsels")) {
            final JsonArray defenceCounsels = defendant.getJsonArray("defenceCounsels");
            caseJson.add("defenceCounselName", defenceCounsels.stream().map(JsonObject.class::cast).map(r -> r.getString("name"))
                    .collect(Collectors.joining(SEPARATOR)));
            caseJson.add("defenceCounselStatus", defenceCounsels.stream().map(JsonObject.class::cast).map(r -> r.getString("status"))
                    .collect(Collectors.joining(SEPARATOR)));
        } else {
            caseJson.add("defenceCounselName", DASH);
            caseJson.add("defenceCounselStatus", DASH);
        }
    }

    private void buildOffences(final JsonObject pcoaJson, JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        Optional.ofNullable(pcoaJson.getJsonArray("offences")).ifPresent(offences -> offences.stream().map(JsonObject.class::cast).forEach(offenceJson -> {
            final String convictionDate = formatDate(offenceJson.getString(CONVICTION_DATE, DASH));
            final JsonObjectBuilder offenceBuilder = JsonObjects.createObjectBuilder()
                    .add("offenceCode", offenceJson.getString("offenceCode", DASH))
                    .add("offenceTitle", clearUndesiredCharacters(offenceJson.getString("offenceTitle", DASH)))
                    .add("wording", addNewLineIfDesired(clearUndesiredCharacters(offenceJson.getString("wording", DASH))))
                    .add("allocationDecision", offenceJson.getString("allocationDecision", DASH))
                    .add(CONVICTION_DATE, convictionDate)
                    .add("verdictCode", offenceJson.getString("verdictCode", DASH));
            setResults(offenceJson.getJsonArray(RESULTS), offenceBuilder);
            setPleaValue(offenceJson, offenceBuilder);
            setIndicatedPleaValue(offenceJson, offenceBuilder);

            jsonArrayBuilder.add(offenceBuilder.build());
        }));

        final JsonArray offences = jsonArrayBuilder.build();
        if (!offences.isEmpty()) {
            caseJson.add("offences", offences);
        }
    }

    private void setResults(final JsonArray resultsArray, JsonObjectBuilder offenceBuilder) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        Optional.ofNullable(resultsArray).ifPresent(results -> {
            final List<JsonObject> resultList = results.stream().map(JsonObject.class::cast).collect(Collectors.toList());
            resultList.forEach(result -> {
                final JsonObjectBuilder resultBuilder = JsonObjects.createObjectBuilder()
                        .add(RESULT_CODE, result.getString(CJS_RESULT_CODE, DASH))
                        .add(RESULT_TEXT, prepareResultText(result.getString(RESULT_TEXT, "")));
                jsonArrayBuilder.add(resultBuilder.build());
            });
            offenceBuilder.add(RESULTS, jsonArrayBuilder);
        });
    }

    private void setPleaValue(JsonObject offenceJson, JsonObjectBuilder offenceBuilder) {
        final String pleaValue = "pleaValue";
        if (offenceJson.containsKey(pleaValue)) {
            final StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
            stringJoiner.add(offenceJson.getString(pleaValue));
            if (offenceJson.containsKey(PLEA_DATE)) {
                stringJoiner.add(formatDate(offenceJson.getString(PLEA_DATE)));
            }
            offenceBuilder
                    .add(pleaValue, stringJoiner.toString());
        } else {
            offenceBuilder
                    .add(pleaValue, DASH);
        }
    }

    private void setIndicatedPleaValue(JsonObject offenceJson, JsonObjectBuilder offenceBuilder) {
        final String indicatedPleaValue = "indicatedPleaValue";
        if (offenceJson.containsKey(indicatedPleaValue)) {
            final StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
            stringJoiner.add(offenceJson.getString(indicatedPleaValue));
            if (offenceJson.containsKey(CONVICTION_DATE)) {
                stringJoiner.add(formatDate(offenceJson.getString(CONVICTION_DATE)));
            }
            offenceBuilder
                    .add(indicatedPleaValue, stringJoiner.toString());
        } else {
            offenceBuilder
                    .add(indicatedPleaValue, DASH);
        }
    }

    private String buildAliases(JsonObject defendant) {
        if (defendant.containsKey("aliases")) {
            return defendant.getJsonArray("aliases").stream()
                    .map(JsonObject.class::cast)
                    .map(a -> Stream.of(
                            a.getString("title", ""), a.getString("firstName", ""), a.getString("middleName", ""), a.getString("lastName", ""))
                            .filter(next -> !Strings.isNullOrEmpty(next))
                            .collect(Collectors.joining(BLANK)))
                    .filter(next -> !Strings.isNullOrEmpty(next))
                    .collect(Collectors.joining(SEPARATOR));
        } else {
            return DASH;
        }
    }

    private String buildCourtHouseAddress(JsonObject addressObj) {
        final String address = Stream.of("1", "2", "3", "4", "5")
                .map(a -> ADDRESS + a)
                .map(a -> addressObj.getString(a, ""))
                .filter(next -> !Strings.isNullOrEmpty(next))
                .collect(Collectors.joining(SEPARATOR));

        final StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(address);
        if (addressObj.containsKey(POSTCODE)) {
            stringJoiner.add(addressObj.getString(POSTCODE));
        }
        return stringJoiner.toString();
    }

    private String getAge(JsonObject defendant) {
        if (defendant.containsKey("dateOfBirth")) {
            return String.valueOf(Period.between(LocalDate.parse(defendant.getString("dateOfBirth")), LocalDate.now()).getYears());
        } else {
            return DASH;
        }
    }

    private String clearUndesiredCharacters(final String input) {
        return input.replaceAll("\\s+", BLANK).trim();
    }

    private String addNewLineIfDesired(final String input) {
        return input.replaceAll(DESIRED_NEW_LINE, NEW_LINE).trim();
    }

    private String prepareResultText(final String resultText){
        String updateResultText =  resultText.replaceAll(NEW_LINE, DESIRED_NEW_LINE).trim();
        updateResultText = clearUndesiredCharacters(updateResultText);
        return addNewLineIfDesired(updateResultText);
    }

    private boolean isApplicationValid(JsonObject pcoaJson) {

        final List<String> applicationFields = Arrays.asList("applicationType",
                "applicationDecision", "applicationDecisionDate", "applicationResponse",
                "applicationResponseDate", "applicationResult");
        return applicationFields.stream().anyMatch(s -> !pcoaJson.getString(s, "").isEmpty());
    }

    private String formatDate(String dateInString) {
        if (dateInString.isEmpty() || dateInString.equals(DASH)) {
            return DASH;
        }
        final LocalDate dateTime = LocalDate.parse(dateInString, IN_DATE_FORMATTER);
        return dateTime.format(OUT_DATE_FORMATTER);
    }

    private String formatZonedDate(String dateInString) {
        return ZonedDateTimes.fromString(dateInString).format(OUT_DATE_FORMATTER);
    }
}
