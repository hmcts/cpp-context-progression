package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

public class ProsecutionCaseDefendantListingStatusChangedVerificationHelper {

    private static final String SITTING_DAY = "sittingDay";
    private static final String LISTING_SEQUENCE = "listingSequence";
    private static final String LISTED_DURATION_MINUTES = "listedDurationMinutes";

    public static void validateApplicationOrCaseForListingStatusChanged(final DocumentContext hearingInput,
                                                                        final JsonArray applicationsOrCases,
                                                                        final JsonArray outputHearings,
                                                                        final String applicationOrCaseType) {
        applicationsOrCases.stream().forEach(applicationOrCase -> {
            outputHearings.stream().forEach(hearingOutputDetail -> {
                final DocumentContext inputApplication = JsonPath.parse(applicationOrCase);
                final DocumentContext hearingOutputDocument = JsonPath.parse(hearingOutputDetail);
                final String caseId = ((JsonString) hearingOutputDocument.read("$.caseId")).getString().toString();
                if (inputApplication.read("$.id").equals(hearingOutputDocument.read("$.caseId"))) {
                    final JsonString caseType = hearingOutputDocument.read("$._case_type");
                    assertThat(caseType.getString(), is(applicationOrCaseType));

                    if("APPLICATION".equals(caseType.getString())){
                        assertApplication(inputApplication, hearingOutputDocument);
                    }

                    final JsonValue isCrown = hearingOutputDocument.read("$._is_crown");
                    assertThat(isCrown.toString(), is("true"));

                    //We should not introduce  _is_sjp or _is_charging as those are set by SJP context only
                    assertThat(((JsonObject) hearingOutputDocument.json()).containsKey("_is_sjp"), is(false));
                    assertThat(((JsonObject) hearingOutputDocument.json()).containsKey("_is_charging"), is(false));

                    final JsonObject hearingOutput = hearingOutputDocument.read("$.hearings[0]");

                    final String hearingIdOutput = hearingOutput.getString("hearingId");
                    final JsonString hearingIdInput = hearingInput.read("$.hearing.id");
                    assertThat(hearingIdOutput, is(hearingIdInput.getString()));

                    final String jurisdictionTypeOutput = hearingOutput.getString("jurisdictionType");
                    final JsonString jurisdictionTypeInput = hearingInput.read("$.hearing.jurisdictionType");
                    assertThat(jurisdictionTypeOutput, is(jurisdictionTypeInput.getString()));

                    final String courtIdOutput = hearingOutput.getString("courtId");
                    final JsonString courtIdInput = hearingInput.read("$.hearing.courtCentre.id");
                    assertThat(courtIdOutput, is(courtIdInput.getString()));

                    final String courtCentreNameOutput = hearingOutput.getString("courtCentreName");
                    final JsonString courtCentreNameInput = hearingInput.read("$.hearing.courtCentre.name");
                    assertThat(courtCentreNameOutput, is(courtCentreNameInput.getString()));

                    final String hearingTypeOutput = hearingOutput.getString("hearingTypeId");
                    final JsonString hearingTypeInput = hearingInput.read("$.hearing.type.id");
                    assertThat(hearingTypeOutput, is(hearingTypeInput.getString()));

                    final String hearingTypeDescriptionOutput = hearingOutput.getString("hearingTypeLabel");
                    final JsonString hearingTypeDescriptionInput = hearingInput.read("$.hearing.type.description");
                    assertThat(hearingTypeDescriptionOutput, is(hearingTypeDescriptionInput.getString()));

                    final boolean isBoxHearingOutput = hearingOutput.getBoolean("isBoxHearing");
                    final JsonValue isBoxHearingInput = hearingInput.read("$.hearing.isBoxHearing");
                    assertThat(valueOf(isBoxHearingOutput), is(isBoxHearingInput.toString()));

                    final JsonArray hearingDatesArrayOutput = hearingOutput.getJsonArray("hearingDates");
                    final JsonArray hearingDatesArrayInput = hearingInput.read("$.hearing.hearingDays");

                    assertHearingDates(hearingDatesArrayOutput, hearingDatesArrayInput);

                    final JsonArray hearingDaysArrayOutput = hearingOutput.getJsonArray("hearingDays");
                    assertHearingDays(hearingDaysArrayOutput, hearingDatesArrayInput);

                    final JsonArray judiciaryTypesArrayOutput = hearingOutput.getJsonArray("judiciaryTypes");
                    final JsonArray judiciaryTypesArrayInput = hearingInput.read("$.hearing.judiciary");
                    assertJudiciaryTypes(judiciaryTypesArrayOutput, judiciaryTypesArrayInput);

                    if (applicationOrCaseType.equalsIgnoreCase("PROSECUTION")) {
                        final JsonArray defendantIdsArrayOutput = hearingOutput.getJsonArray("defendantIds");
                        assertDefendantIds(defendantIdsArrayOutput, hearingInput, caseId);
                    }
                }
            });
        });
    }

    private static void assertDefendantIds(final JsonArray defendantIdsArrayOutput, final DocumentContext hearingInput, final String caseId) {

        final List<String> hearingDatesOutput = defendantIdsArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());

        final JsonArray casesInput = hearingInput.read("$.hearing.prosecutionCases");
        for (int i = 0; i < casesInput.size(); i++) {
            final JSONArray defendantIds = hearingInput.read("$.hearing.prosecutionCases[" + i + "].defendants[*].id");
            assertThat(hearingDatesOutput.size(), is(defendantIds.size()));
            for (int j = 0; j < defendantIds.size(); j++) {
                final String prosecutionCaseId = ((JsonString) hearingInput.read("$.hearing.prosecutionCases[" + i + "].defendants[" + j + "].prosecutionCaseId")).getString();
                if (prosecutionCaseId.equalsIgnoreCase(caseId)) {
                    assertThat(hearingDatesOutput.get(j), is(((JsonString) defendantIds.get(j)).getString()));
                }
            }
        }
    }

    private static void assertApplication(final DocumentContext inputApplicationEventPayload, final DocumentContext hearingOutputDocument) {

        assertApplicationProperty(inputApplicationEventPayload, hearingOutputDocument, "applicationReceivedDate", "$.applications[0].receivedDate");

        assertApplicationProperty(inputApplicationEventPayload, hearingOutputDocument, "applicationReference", "$.applications[0].applicationReference");

        assertApplicationProperty(inputApplicationEventPayload, hearingOutputDocument, "type.type", "$.applications[0].applicationType");
    }

    private static void assertApplicationProperty(final DocumentContext inputApplicationEventPayload, final DocumentContext transformedOutputDocument, final String inputPath, final String outputPath) {
        final JsonString expected = inputApplicationEventPayload.read(inputPath);
        final JsonString actual = transformedOutputDocument.read(outputPath);
        assertThat(actual, is(expected));
    }

    private static void assertHearingDates(JsonArray hearingDatesArrayOutput, JsonArray hearingDatesArrayInput) {

        final List<String> hearingDatesOutput = hearingDatesArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());

        assertThat(hearingDatesOutput.size(), is(2));

        assertThat(hearingDatesArrayInput.size(), is(2));

        hearingDatesArrayInput.stream().map(JsonObject.class::cast).forEach(hearingDateInput -> {
            final String sittingDay = hearingDateInput.getString(SITTING_DAY).substring(0, 10);
            assertThat(hearingDatesOutput.contains(sittingDay), is(true));
        });
    }

    private static void assertHearingDays(final JsonArray hearingDaysArrayOutput, final JsonArray hearingDaysArrayInput) {

        final int expectedSize = hearingDaysArrayInput.size();
        assertThat(hearingDaysArrayOutput, hasSize(expectedSize));

        for (int i = 0; i < expectedSize; i++) {

            final JsonObject expectedHearingDay = hearingDaysArrayInput.getJsonObject(i);
            final JsonObject actualHearingDay = hearingDaysArrayOutput.getJsonObject(i);

            assertHearingDay(actualHearingDay, expectedHearingDay);
        }
    }

    private static void assertHearingDay(final JsonObject actualHearingDay, final JsonObject expectedHearingDay) {
        assertThat(actualHearingDay.getString(SITTING_DAY), is(expectedHearingDay.getString(SITTING_DAY)));
        assertThat(actualHearingDay.getInt(LISTING_SEQUENCE), is(expectedHearingDay.getInt(LISTING_SEQUENCE)));
        assertThat(actualHearingDay.getInt(LISTED_DURATION_MINUTES), is(expectedHearingDay.getInt(LISTED_DURATION_MINUTES)));
    }

    private static void assertJudiciaryTypes(final JsonArray judiciaryTypesArrayOutput, final JsonArray judiciaryTypesArrayInput) {

        final List<String> judiciaryTypesOutput = judiciaryTypesArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());

        assertThat(judiciaryTypesOutput.size(), is(2));

        assertThat(judiciaryTypesArrayInput.size(), is(2));

        judiciaryTypesArrayInput.stream().map(JsonObject.class::cast).forEach(judiciary -> {

            final JsonObject judicialRoleType = judiciary.getJsonObject("judicialRoleType");

            final String judiciaryType = judicialRoleType.getString("judiciaryType");

            assertThat(judiciaryTypesOutput.contains(judiciaryType), is(true));
        });
    }

    public static void verifyCasesAndApplicationsCount(JsonObject outputCaseDocumentsJson, JsonArray courtApplications, JsonArray prosecutionCases) {
        int totalNumberOfHearings = courtApplications.size() + prosecutionCases.size();
        final JsonArray actualHearings = (JsonArray) outputCaseDocumentsJson.get("caseDocuments");
        assertThat(actualHearings.size(), is(totalNumberOfHearings));
    }
}
