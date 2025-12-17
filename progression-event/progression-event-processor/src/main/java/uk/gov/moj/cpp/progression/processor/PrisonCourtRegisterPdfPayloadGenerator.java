package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;

public class PrisonCourtRegisterPdfPayloadGenerator {

    private static final String ADDRESS = "address";
    private static final String SEPARATOR = ", ";
    private static final String DASH = "-";
    private static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    private static final DateTimeFormatter IN_DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
    private static final DateTimeFormatter OUT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue());
    private static final String CJS_RESULT_CODE = "cjsResultCode";
    private static final String RESULT_CODE = "resultCode";
    private static final String RESULT_TEXT = "resultText";
    private static final String RESULTS = "results";
    private static final String CONVICTION_DATE = "convictionDate";
    public static final String BLANK = " ";
    public static final String NEW_LINE = "\n";
    public static final String DESIRED_NEW_LINE = "####";

    @SuppressWarnings({"squid:S1188", "squid:S1192"})
    public JsonObject mapPayload(final JsonObject jsonObject) {

        final JsonObjectBuilder payloadForPdf = Json.createObjectBuilder();
        final JsonArrayBuilder casesArray = Json.createArrayBuilder();

        ofNullable(jsonObject.getJsonObject("prisonCourtRegister")).ifPresent(prisonCourtRegisterRequest -> {
            payloadForPdf.add("registerDate", LocalDate.now().format(OUT_DATE_FORMATTER));
            ofNullable(prisonCourtRegisterRequest.getJsonObject("hearingVenue")).ifPresent(hearingVenue -> {
                payloadForPdf.add("ljaName", hearingVenue.getString("ljaName", DASH));
                payloadForPdf.add("courtHouse", hearingVenue.getString("courtHouse", DASH));
                ofNullable(hearingVenue.getJsonObject("address")).ifPresent(
                        address -> payloadForPdf.add("courtHouseAddress", buildCourtHouseAddress(address))
                );
            });

            ofNullable(prisonCourtRegisterRequest.getJsonObject("custodyLocation"))
                    .ifPresent(custodyLocation ->
                            payloadForPdf.add("custodyLocation", custodyLocation.getString("name", DASH)));

            final JsonObject defendant = prisonCourtRegisterRequest.getJsonObject("defendant");


            defendant.getJsonArray("prosecutionCasesOrApplications").stream()
                    .map(JsonObject.class::cast)
                    .collect(toList())
                    .forEach(pcoa -> {
                        final JsonObjectBuilder caseJson = Json.createObjectBuilder();

                        buildNameAndAddress(defendant, caseJson);
                        buildHearingDetails(defendant, caseJson);
                        buildDefenceCounsel(defendant, caseJson);
                        if (jsonObject.containsKey("defendantType")){
                            caseJson.add("defendantType", jsonObject.getString("defendantType"));
                        }

                        caseJson.add("postHearingCustodyStatus", defendant.getString("postHearingCustodyStatus", DASH));
                        caseJson.add("officerInCase", DASH);
                        caseJson.add("parentGuardianName", DASH);
                        caseJson.add("parentGuardianAddress1", DASH);
                        caseJson.add("dateOfBirth", formatDate(defendant.getString("dateOfBirth", DASH)));
                        caseJson.add("age", getAge(defendant));
                        caseJson.add("gender", defendant.getString("gender", DASH));
                        caseJson.add("nationality", defendant.getString("nationality", DASH));
                        caseJson.add("aliases", buildAliases(defendant));
                        caseJson.add("caseReference", pcoa.getString("caseOrApplicationReference", DASH));
                        caseJson.add("dateOfHearing", formatZonedDate(prisonCourtRegisterRequest.getString("hearingDate")));
                        caseJson.add("prosecutorName", pcoa.getString("prosecutorName", DASH));
                        caseJson.add("arrestSummonsNumber", pcoa.getString("arrestSummonsNumber", DASH));

                        buildProsecutionCounsel(pcoa, caseJson);
                        buildDefendantResults(defendant, caseJson);
                        buildCaseResults(pcoa, caseJson);
                        buildOffences(pcoa, caseJson);
                        buildApplication(pcoa, caseJson);
                        casesArray.add(caseJson.build());
                    });
        });

        payloadForPdf.add("cases", casesArray);

        return payloadForPdf.build();
    }

    private void buildDefendantResults(final JsonObject defendant, final JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        ofNullable(defendant.getJsonArray("defendantResults")).ifPresent(results -> {
            final List<JsonObject> resultList = results.stream().map(JsonObject.class::cast).collect(toList());
            resultList.forEach(result -> {
                final JsonObjectBuilder resultBuilder = Json.createObjectBuilder()
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

    private void buildCaseResults(final JsonObject pcoa, final JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        if(!isApplicationValid(pcoa)){
            ofNullable(pcoa.getJsonArray(RESULTS)).ifPresent(results -> results.stream().map(JsonObject.class::cast).forEach(result -> {
                final JsonObjectBuilder resultBuilder = Json.createObjectBuilder()
                        .add(RESULT_CODE, result.getString(CJS_RESULT_CODE, DASH))
                        .add(RESULT_TEXT, prepareResultText(result.getString(RESULT_TEXT, "")));
                jsonArrayBuilder.add(resultBuilder.build());
            }));

            final JsonArray caseResults = jsonArrayBuilder.build();
            if (!caseResults.isEmpty()) {
                caseJson.add("caseResults", caseResults);
            }
        }
    }

    private String formatDate(final String dateInString) {
        if (dateInString.isEmpty() || dateInString.equals(DASH)) {
            return DASH;
        }
        final LocalDate dateTime = LocalDate.parse(dateInString, IN_DATE_FORMATTER);
        return dateTime.format(OUT_DATE_FORMATTER);
    }

    private String formatZonedDate(final String dateInString) {
        return ZonedDateTimes.fromString(dateInString).format(OUT_DATE_FORMATTER);
    }

    private void buildHearingDetails(final JsonObject defendant, final JsonObjectBuilder caseJson) {

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

    private void buildNameAndAddress(final JsonObject defendant, final JsonObjectBuilder caseJson) {
        caseJson.add("name", defendant.getString("name", DASH));
        ofNullable(defendant.getJsonObject(ADDRESS)).ifPresent(addressObj -> {
            Stream.of("1", "2", "3", "4", "5").map(a -> ADDRESS + a).forEach(a -> caseJson.add(a, addressObj.getString(a, "")));
            caseJson.add("postCode", addressObj.getString("postCode", ""));
        });
    }

    private String buildCourtHouseAddress(final JsonObject addressObj) {
        final String address = Stream.of("1", "2", "3", "4", "5")
                .map(a -> ADDRESS + a)
                .map(a -> addressObj.getString(a, ""))
                .filter(next -> !Strings.isNullOrEmpty(next))
                .collect(Collectors.joining(SEPARATOR));

        final StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(address);
        if (addressObj.containsKey("postcode")) {
            stringJoiner.add(addressObj.getString("postcode"));
        }
        return stringJoiner.toString();
    }

    private String buildAliases(final JsonObject defendant) {
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

    private void buildProsecutionCounsel(final JsonObject pcoa, final JsonObjectBuilder caseJson) {

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

    private void buildDefenceCounsel(final JsonObject defendant, final JsonObjectBuilder caseJson) {
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

    private void buildApplication(final JsonObject pcoaJson, final JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        if (isApplicationValid(pcoaJson) && nonNull(pcoaJson.getJsonArray(RESULTS))) {
            final List<String> resultList =
            IntStream
                    .range(0,pcoaJson.getJsonArray(RESULTS).size())
                    .mapToObj(i -> pcoaJson.getJsonArray(RESULTS).getJsonObject(i))
                    .map(result -> prepareResultText(result.getString(RESULT_TEXT, DASH)))
                    .collect(Collectors.toList());
            final JsonArrayBuilder resultJsonArrayBuilder  = Json.createArrayBuilder();
            resultList.stream().forEach(resultJsonArrayBuilder::add);

            final JsonObjectBuilder applicationBuilder = Json.createObjectBuilder()
                    .add("type", pcoaJson.getString("applicationType", DASH))
                    .add("result", resultJsonArrayBuilder.build()
                    );
            ofNullable(pcoaJson.getString("applicationDecision", null)).ifPresent(decision -> applicationBuilder.add("decision", decision));
            ofNullable(pcoaJson.getString("applicationDecisionDate", null)).ifPresent(decisionDate -> applicationBuilder.add("decisionDate", formatDate(decisionDate)));
            ofNullable(pcoaJson.getString("applicationResponse", null)).ifPresent(response -> applicationBuilder.add("response", response));
            ofNullable(pcoaJson.getString("applicationResponseDate", null)).ifPresent(responseDate -> applicationBuilder.add("responseDate", formatDate(responseDate)));
            jsonArrayBuilder.add(applicationBuilder.build());
        }

        final JsonArray applications = jsonArrayBuilder.build();
        if (!applications.isEmpty()) {
            caseJson.add("applications", applications);
        }
    }

    private boolean isApplicationValid(final JsonObject pcoaJson) {

        final List<String> applicationFields = Arrays.asList("applicationType",
                "applicationDecision", "applicationDecisionDate", "applicationResponse",
                "applicationResponseDate", "applicationResult");
        return applicationFields.stream().anyMatch(s -> !pcoaJson.getString(s, "").isEmpty());
    }



    private void buildOffences(final JsonObject pcoaJson, final JsonObjectBuilder caseJson) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        ofNullable(pcoaJson.getJsonArray("offences")).ifPresent(offences -> offences.stream().map(JsonObject.class::cast).filter(offenceJson -> nonNull(offenceJson.getJsonArray(RESULTS))).forEach(offenceJson -> {
            final String convictionDate = formatDate(offenceJson.getString(CONVICTION_DATE, DASH));
            final JsonObjectBuilder offenceBuilder = Json.createObjectBuilder()
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

    private void setResults(final JsonArray resultsArray, final JsonObjectBuilder offenceBuilder) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        ofNullable(resultsArray).ifPresent(results -> {
            final List<JsonObject> resultList = results.stream().map(JsonObject.class::cast).collect(toList());
            resultList.forEach(result -> {
                final JsonObjectBuilder resultBuilder = Json.createObjectBuilder()
                        .add(RESULT_CODE, result.getString(CJS_RESULT_CODE, DASH))
                        .add(RESULT_TEXT, prepareResultText(result.getString(RESULT_TEXT, "")));
                jsonArrayBuilder.add(resultBuilder.build());
            });
            offenceBuilder.add(RESULTS, jsonArrayBuilder);
        });
    }

    private void setPleaValue(final JsonObject offenceJson, final JsonObjectBuilder offenceBuilder) {
        final String pleaValue = "pleaValue";
        if (offenceJson.containsKey(pleaValue)) {
            final StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
            stringJoiner.add(offenceJson.getString(pleaValue));
            if (offenceJson.containsKey("pleaDate")) {
                stringJoiner.add(formatDate(offenceJson.getString("pleaDate")));
            }
            offenceBuilder
                    .add(pleaValue, stringJoiner.toString());
        } else {
            offenceBuilder
                    .add(pleaValue, DASH);
        }
    }

    private void setIndicatedPleaValue(final JsonObject offenceJson, final JsonObjectBuilder offenceBuilder) {
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

    private String getAge(final JsonObject defendant) {
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
}
