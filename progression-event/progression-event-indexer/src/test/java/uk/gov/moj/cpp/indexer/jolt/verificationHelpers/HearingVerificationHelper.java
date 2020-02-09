package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.IntStream.range;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class HearingVerificationHelper extends BaseVerificationHelper {
    private static final Logger logger = Logger.getLogger(HearingVerificationHelper.class.getName());
    private static final String INPUT_HEARING_JSON_PATH = "$.hearing";
    private static final String INPUT_HEARING_DAYS_JSON_PATH = "$.hearing.hearingDays";
    private static final String INPUT_JUDICIARY_JSON_PATH = "$.hearing.judiciary";
    private static final String ROOT = "$";
    private static final String OUTPUT_HEARINGS_JSON_PATH = "$.caseDocuments[%d].hearings[%d]";

    public void verifyHearings(final DocumentContext inputHearing,
                               final JsonObject outputCases,
                               final int caseIndex,
                               final int outputHearingIndex) {
        verifyHearing(inputHearing, outputCases, caseIndex, outputHearingIndex);
        validateHearingDaysAndDates(inputHearing, outputCases, caseIndex, outputHearingIndex);
        validateJudiciaryTypes(inputHearing, outputCases, caseIndex, outputHearingIndex);
    }

    private void validateJudiciaryTypes(final DocumentContext inputHearing,
                                        final JsonObject outputCases,
                                        final int caseIndex,
                                        final int outputHearingIndex) {
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, caseIndex, outputHearingIndex);

            final JsonArray judiciaryInput = inputHearing.read(INPUT_JUDICIARY_JSON_PATH);
            final int judiciaryInputSize = judiciaryInput.size();
            range(0, judiciaryInputSize)
                    .forEach(judiciaryIndex -> {
                        final String judiciaryRoleTypesInput = ((JsonString) inputHearing.read(format(INPUT_JUDICIARY_JSON_PATH) + "[" + judiciaryIndex + "].judicialRoleType.judiciaryType")).getString();
                        with(outputCases.toString())
                                .assertThat(hearingOutputIndexPath + ".judiciaryTypes[" + judiciaryIndex + "]", is(judiciaryRoleTypesInput));

                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Judiciary Types ", e.getMessage()));
        }
    }

    private void validateHearingDaysAndDates(final DocumentContext inputHearing,
                                             final JsonObject outputCases,
                                             final int caseIndex,
                                             final int outputHearingIndex) {
        try {
            final JsonArray hearingDaysInput = inputHearing.read(format(INPUT_HEARING_DAYS_JSON_PATH));
            final int hearingDaysSize = hearingDaysInput.size();
            range(0, hearingDaysSize)
                    .forEach(hearingDayIndex -> {
                        final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, caseIndex, outputHearingIndex);
                        validateHearingDayAndDates(inputHearing, outputCases, hearingOutputIndexPath, hearingDayIndex);
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing Days", e.getMessage()));
        }
    }

    private void validateHearingDayAndDates(final DocumentContext inputParties,
                                            final JsonObject outputCases,
                                            final String hearingOutputIndexPath,
                                            final int hearingDayIndex) {
        final LocalDate date = fromString(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString()).toLocalDate();
        try {
            final String sittingDay = ((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString();
            with(outputCases.toString())
                    .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes")).intValue()))
                    .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listingSequence", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listingSequence")).intValue()))
                    .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].sittingDay", is(fromString(sittingDay).format(DateTimeFormatter.ISO_INSTANT)))
                    .assertThat(hearingOutputIndexPath + ".hearingDates[" + hearingDayIndex + "]", is(date.toString()));
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating day and dates Hearing", e.getMessage()));
        }
    }

    public void verifyHearing(final DocumentContext inputParties,
                              final JsonObject outputCases,
                              final int caseIndex,
                              final int outputHearingIndex) {
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, caseIndex, outputHearingIndex);

            with(outputCases.toString())
                    .assertThat(hearingOutputIndexPath + ".boxWorkAssignedUserId", equalTo(((JsonString) inputParties.read(ROOT + ".boxWorkAssignedUserId")).getString()))
                    .assertThat(hearingOutputIndexPath + ".boxWorkTaskStatus", equalTo(((JsonString) inputParties.read(ROOT + ".boxWorkTaskStatus")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".jurisdictionType", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".jurisdictionType")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".courtCentre.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreName", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".courtCentre.name")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeId", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".type.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeLabel", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".type.description")).getString()))
                    .assertThat(hearingOutputIndexPath + ".isBoxHearing", equalTo(valueOf(inputParties.read(INPUT_HEARING_JSON_PATH + ".isBoxHearing").toString()).booleanValue()));
            incrementHearingsCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing", e.getMessage()));
        }
    }
}
