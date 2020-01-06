package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class ProsecutionCaseDefendantListingStatusChangedVerificationHelper {

    public static void validateApplicationOrCaseForListingStatusChanged(final DocumentContext hearingInput,
                                                                        final JsonArray applicationsOrCases,
                                                                        final JsonArray outputHearings,
                                                                        final String applicationOrCaseType) {
        applicationsOrCases.stream().forEach(applicationOrCase -> {
            outputHearings.stream().forEach(hearingOutputDetail -> {
                final DocumentContext application1 = JsonPath.parse(applicationOrCase);


                final DocumentContext hearingOutputDocument = JsonPath.parse(hearingOutputDetail);
                if (application1.read("$.id").equals(hearingOutputDocument.read("$.caseId"))) {
                    final JsonString caseType = hearingOutputDocument.read("$._case_type");
                    assertThat(caseType.getString(), is(applicationOrCaseType));

                    if("APPLICATION".equals(caseType.getString())){
                        final JsonString dueDateOutput = application1.read("dueDate");
                        final JsonString dueDateInput = hearingOutputDocument.read("$.dueDate");
                        assertThat(dueDateOutput, is(dueDateInput));
                    }

                    final JsonValue isCrown = hearingOutputDocument.read("$._is_crown");
                    assertThat(isCrown.toString(), is("true"));

                    final JsonObject hearingOutput = hearingOutputDocument.read("$.hearings[0]");

                    final String boxWorkAssignedUserIdOutput = hearingOutput.getString("boxWorkAssignedUserId");
                    final JsonString boxWorkAssignedUserIdInput = hearingInput.read("$.boxWorkAssignedUserId");
                    assertThat(boxWorkAssignedUserIdOutput, is(boxWorkAssignedUserIdInput.getString()));

                    final String boxWorkTaskStatusOutput = hearingOutput.getString("boxWorkTaskStatus");
                    final JsonString boxWorkTaskStatusInput = hearingInput.read("$.boxWorkTaskStatus");
                    assertThat(boxWorkTaskStatusOutput, is(boxWorkTaskStatusInput.getString()));

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

                    final JsonArray judiciaryTypesArrayOutput = hearingOutput.getJsonArray("judiciaryTypes");
                    final JsonArray judiciaryTypesArrayInput = hearingInput.read("$.hearing.judiciary");
                    assertJudiciaryTypes(judiciaryTypesArrayOutput, judiciaryTypesArrayInput);
                }
            });
        });
    }

    private static void assertHearingDates(JsonArray hearingDatesArrayOutput, JsonArray hearingDatesArrayInput) {

        final List<String> hearingDatesOutput = hearingDatesArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());

        assertThat(hearingDatesOutput.size(), is(2));

        assertThat(hearingDatesArrayInput.size(), is(2));

        hearingDatesArrayInput.stream().map(JsonObject.class::cast).forEach(hearingDateInput -> {
            final String sittingDay = hearingDateInput.getString("sittingDay").substring(0, 10);
            assertThat(hearingDatesOutput.contains(sittingDay), is(true));
        });
    }

    private static void assertJudiciaryTypes(JsonArray judiciaryTypesArrayOutput, JsonArray judiciaryTypesArrayInput) {

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
