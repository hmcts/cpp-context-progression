package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class ListingStatusChangedVerificationHelper {

    public static void validateHearings(final JsonArray outputHearings,
                                        final DocumentContext hearingInput,
                                        final DocumentContext hearingConfirmedInput) {
        outputHearings.stream().forEach(hearingOutputDetail -> {
            final DocumentContext hearingOutputDocument = JsonPath.parse(hearingOutputDetail);

            final JsonObject hearingOutput = hearingOutputDocument.read("$");

            final String hearingIdOutput = hearingOutput.getString("hearingId");
            final JsonString hearingIdInput = hearingInput.read("$.hearing.id");
            final JsonString confirmedHearingIdInput = hearingConfirmedInput.read("$.confirmedHearing.id");

            assertThat(hearingIdOutput, anyOf(is(hearingIdInput.getString()), is(confirmedHearingIdInput.getString())));

            final String jurisdictionTypeOutput = hearingOutput.getString("jurisdictionType");
            final JsonString jurisdictionTypeInput = hearingInput.read("$.hearing.jurisdictionType");
            assertThat(jurisdictionTypeOutput, is(jurisdictionTypeInput.getString()));

            final String courtIdOutput = hearingOutput.getString("courtId");
            final JsonString courtIdInput = hearingInput.read("$.hearing.courtCentre.id");
            final JsonString courtIdHearingConfirmedInput = hearingConfirmedInput.read("$.confirmedHearing.courtCentre.id");
            assertThat(courtIdOutput, anyOf(is(courtIdInput.getString()), is(courtIdHearingConfirmedInput.getString())));

            final String courtCentreNameOutput = hearingOutput.getString("courtCentreName");
            final JsonString courtCentreNameInput = hearingInput.read("$.hearing.courtCentre.name");
            assertThat(courtCentreNameOutput, is(courtCentreNameInput.getString()));

            final String hearingTypeOutput = hearingOutput.getString("hearingTypeId");
            final JsonString hearingTypeInput = hearingInput.read("$.hearing.type.id");
            assertThat(hearingTypeOutput, is(hearingTypeInput.getString()));

            final String hearingTypeLabelOutput = hearingOutput.getString("hearingTypeLabel");
            final JsonString hearingTypeLabelInput = hearingInput.read("$.hearing.type.description");
            assertThat(hearingTypeLabelOutput, is(hearingTypeLabelInput.getString()));

            final JsonArray hearingDatesArrayOutput = hearingOutput.getJsonArray("hearingDates");
            final JsonArray confirmedHearingDatesArrayInput = hearingConfirmedInput.read("$.confirmedHearing.hearingDays");
            assertHearingDates(hearingDatesArrayOutput, confirmedHearingDatesArrayInput);

            final JsonArray confirmedJudiciaryArrayInput = hearingConfirmedInput.read("$.confirmedHearing.judiciary");
            final JsonArray judiciaryTypesArrayOutput = hearingOutput.getJsonArray("judiciaryTypes");
            assertJudiciaryTypes(judiciaryTypesArrayOutput, confirmedJudiciaryArrayInput);
        });
    }

    private static void assertHearingDates(final JsonArray hearingDatesArrayOutput,
                                           final JsonArray confirmedHearingDatesArrayInput) {

        final List<String> hearingDatesOutput = hearingDatesArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());

        assertThat(hearingDatesArrayOutput.size(), is(confirmedHearingDatesArrayInput.size()));
        confirmedHearingDatesArrayInput.stream().map(JsonObject.class::cast).forEach(hearingDateInput -> {
            final String sittingDay = hearingDateInput.getString("sittingDay").substring(0, 10);
            assertThat(hearingDatesOutput.contains(sittingDay), is(true));
        });
    }


    private static void assertJudiciaryTypes(final JsonArray judiciaryTypesArrayOutput,
                                             final JsonArray judiciaryTypesArrayInput) {

        final List<String> judiciaryTypesOutput = judiciaryTypesArrayOutput.stream().map(JsonValue.class::cast).map(JsonString.class::cast)
                .map(JsonString::getString).collect(Collectors.toList());
        assertThat(judiciaryTypesOutput.size(), is((1)));

        final String judiciaryInput = ((JsonObject) judiciaryTypesArrayInput.get(0)).getJsonObject("judicialRoleType")
                .getString("judiciaryType");

        assertThat(judiciaryTypesOutput.get(0).toLowerCase(), is(judiciaryInput.toLowerCase()));

    }
}
