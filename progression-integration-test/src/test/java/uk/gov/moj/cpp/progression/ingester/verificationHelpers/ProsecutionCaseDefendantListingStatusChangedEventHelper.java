package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

public class ProsecutionCaseDefendantListingStatusChangedEventHelper {

    private static final String HEARING_DAYS = "hearingDays";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String IS_BOX_HEARING = "isBoxHearing";
    private static final String BOX_WORK_TASK_STATUS = "boxWorkTaskStatus";
    private static final String BOX_WORK_ASSIGNED_USER_ID = "boxWorkAssignedUserId";

    public static void assertCase(final JsonObject outputCase, final List<String> caseIds) {
        assertTrue(caseIds.contains(outputCase.getString("caseId")));
        assertThat(outputCase.getBoolean("_is_crown"), is(true));
    }

    public static void assertHearing(final JsonObject outputHearing, final JsonObject inputHearing, final JsonObject prosecutionCaseDefendantListingStatusChangedEvent, boolean withoutCourtCentreInHearingDays) {
        assertThat(outputHearing.getString("courtCentreName"), is(inputHearing.getJsonObject("courtCentre").getString("name")));
        assertThat(outputHearing.getString("hearingTypeId"), is(inputHearing.getJsonObject("type").getString("id")));
        assertThat(outputHearing.getString("hearingTypeLabel"), is(inputHearing.getJsonObject("type").getString("description")));
        assertThat(outputHearing.getJsonArray("hearingDates").getString(0), is(inputHearing.getJsonArray(HEARING_DAYS).getJsonObject(0).getString("sittingDay").substring(0, 10)));
        if (!withoutCourtCentreInHearingDays) {
            assertThat(outputHearing.getJsonArray(HEARING_DAYS).getJsonObject(0), is(inputHearing.getJsonArray(HEARING_DAYS).getJsonObject(0)));
        }
        assertThat(outputHearing.getString(JURISDICTION_TYPE), is(inputHearing.getString(JURISDICTION_TYPE)));
        assertThat(outputHearing.getString("courtId"), is(inputHearing.getJsonObject("courtCentre").getString("id")));

        assertThat(outputHearing.getBoolean(IS_BOX_HEARING), is(inputHearing.getBoolean(IS_BOX_HEARING)));
        assertThat(outputHearing.getString(BOX_WORK_ASSIGNED_USER_ID), is(prosecutionCaseDefendantListingStatusChangedEvent.getString(BOX_WORK_ASSIGNED_USER_ID)));
        assertThat(outputHearing.getString(BOX_WORK_TASK_STATUS), is(prosecutionCaseDefendantListingStatusChangedEvent.getString(BOX_WORK_TASK_STATUS)));
    }

    public static void assertJudiciaryTypes(JsonArray judiciaryTypesArrayOutput, JsonArray judiciaryTypesArrayInput) {

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
}
