package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.IntStream.range;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.LocalDate;
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
    private static final String OUTPUT_HEARINGS_JSON_PATH = "hearings[%d]";

    public void verifyHearings(final DocumentContext inputHearing,
                               final JsonObject outputCase,
                               final int outputHearingIndex) {
        verifyHearing(inputHearing, outputCase,  outputHearingIndex);
        validateHearingDaysAndDates(inputHearing, outputCase,  outputHearingIndex);
        validateJudiciaryTypes(inputHearing, outputCase, outputHearingIndex);
        verifyHearingWithDefenceCounsels(inputHearing, outputCase, outputHearingIndex);
    }

    public void verifyHearingsWithoutCourtCentre(final DocumentContext inputHearing,
                                                 final JsonObject outputCase,
                                                 final int outputHearingIndex) {
        verifyHearing(inputHearing, outputCase,  outputHearingIndex);
        validateHearingDaysAndDatesWithoutCourCentre(inputHearing, outputCase,  outputHearingIndex);
        validateJudiciaryTypes(inputHearing, outputCase, outputHearingIndex);
    }

    public void validateJudiciaryTypes(final DocumentContext inputHearing,
                                        final JsonObject outputCases,
                                        final int outputHearingIndex) {
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, outputHearingIndex);

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
                                             final int outputHearingIndex) {
        try {
            final JsonArray hearingDaysInput = inputHearing.read(format(INPUT_HEARING_DAYS_JSON_PATH));
            final int hearingDaysSize = hearingDaysInput.size();
            range(0, hearingDaysSize)
                    .forEach(hearingDayIndex -> {
                        final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH,  outputHearingIndex);
                        validateHearingDayAndDates(inputHearing, outputCases, hearingOutputIndexPath, hearingDayIndex);
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing Days", e.getMessage()));
        }
    }

    private void validateHearingDaysAndDatesWithoutCourCentre(final DocumentContext inputHearing,
                                             final JsonObject outputCases,
                                             final int outputHearingIndex) {
        try {
            final JsonArray hearingDaysInput = inputHearing.read(format(INPUT_HEARING_DAYS_JSON_PATH));
            final int hearingDaysSize = hearingDaysInput.size();
            range(0, hearingDaysSize)
                    .forEach(hearingDayIndex -> {
                        final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH,  outputHearingIndex);
                        validateHearingDayAndDatesWithoutCourtCentreInHearingDays(inputHearing, outputCases, hearingOutputIndexPath, hearingDayIndex);
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing Days", e.getMessage()));
        }
    }

    public void validateHearingDayAndDatesWithoutCourtCentreInHearingDays(final DocumentContext inputParties,
                                                                           final JsonObject outputCases,
                                                                           final String hearingOutputIndexPath,
                                                                           final int hearingDayIndex) {
        final LocalDate date = ZonedDateTimes.fromString(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString()).toLocalDate();

        with(outputCases.toString())
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes")).intValue()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listingSequence", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listingSequence")).intValue()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].sittingDay", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].courtCentreId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".courtCentre.id")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].courtRoomId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".courtCentre.roomId")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDates[" + hearingDayIndex + "]", is(date.toString()));
    }

    private void validateHearingDayAndDates(final DocumentContext inputParties,
                                            final JsonObject outputCases,
                                            final String hearingOutputIndexPath,
                                            final int hearingDayIndex) {
        final LocalDate date = ZonedDateTimes.fromString(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString()).toLocalDate();

        with(outputCases.toString())
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listedDurationMinutes")).intValue()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].listingSequence", equalTo(((JsonNumber) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].listingSequence")).intValue()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].sittingDay", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].sittingDay")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].courtCentreId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].courtCentreId")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDays[" + hearingDayIndex + "].courtRoomId", is(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".hearingDays[" + hearingDayIndex + "].courtRoomId")).getString()))
                .assertThat(hearingOutputIndexPath + ".hearingDates[" + hearingDayIndex + "]", is(date.toString()));
    }

    public void verifyHearing(final DocumentContext event,
                              final JsonObject outputCases,
                              final int outputHearingIndex) {
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, outputHearingIndex);

            with(outputCases.toString())
                    .assertThat(hearingOutputIndexPath + ".hearingId", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".jurisdictionType", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".jurisdictionType")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtId", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.name")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomId", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.roomId")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.roomName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreWelshName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.welshName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomWelshName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.welshRoomName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreCode", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.code")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeId", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".type.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeLabel", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".type.description")).getString()))
                    .assertThat(hearingOutputIndexPath + ".isBoxHearing", equalTo(valueOf(event.read(INPUT_HEARING_JSON_PATH + ".isBoxHearing").toString()).booleanValue()))
                    .assertThat(hearingOutputIndexPath + ".isVirtualBoxHearing", equalTo(valueOf(event.read(INPUT_HEARING_JSON_PATH + ".isVirtualBoxHearing").toString()).booleanValue()));
            incrementHearingsCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing", e.getMessage()));
        }
    }

    private void verifyHearingWithEstimatedDuration(final DocumentContext event,
                                                    final JsonObject outputCases,
                                                    final int outputHearingIndex) {
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, outputHearingIndex);

            with(outputCases.toString())
                    .assertThat(hearingOutputIndexPath + ".hearingId", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".jurisdictionType", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".jurisdictionType")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtId", is(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.name")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomId", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.roomId")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.roomName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreWelshName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.welshName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreRoomWelshName", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.welshRoomName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".courtCentreCode", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".courtCentre.code")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeId", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".type.id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".hearingTypeLabel", equalTo(((JsonString) event.read(INPUT_HEARING_JSON_PATH + ".type.description")).getString()))
                    .assertThat(hearingOutputIndexPath + ".isBoxHearing", equalTo(valueOf(event.read(INPUT_HEARING_JSON_PATH + ".isBoxHearing").toString()).booleanValue()))
                    .assertThat(hearingOutputIndexPath + ".isVirtualBoxHearing", equalTo(valueOf(event.read(INPUT_HEARING_JSON_PATH + ".isVirtualBoxHearing").toString()).booleanValue()))
                    .assertThat(hearingOutputIndexPath + ".estimatedDuration", equalTo(((JsonString) (event.read(INPUT_HEARING_JSON_PATH + ".estimatedDuration"))).getString()));
            incrementHearingsCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Hearing", e.getMessage()));
        }
    }

    private void verifyHearingWithDefenceCounsels(final DocumentContext inputParties,
                                                  final JsonObject outputCases,
                                                  final int outputHearingIndex){
        try {
            final String hearingOutputIndexPath = format(OUTPUT_HEARINGS_JSON_PATH, outputHearingIndex);

            with(outputCases.toString())
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].defendants[0]", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].defendants[0]")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].attendanceDays[0]", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].attendanceDays[0]")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].firstName", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].firstName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].id",equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].id")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].lastName", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].lastName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].middleName", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].middleName")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].status", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].status")).getString()))
                    .assertThat(hearingOutputIndexPath + ".defenceCounsels[0].title", equalTo(((JsonString) inputParties.read(INPUT_HEARING_JSON_PATH + ".defenceCounsels[0].title")).getString()))
            ;
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Defence Counsels Hearing %s", e.getMessage()));
        }
    }
}
