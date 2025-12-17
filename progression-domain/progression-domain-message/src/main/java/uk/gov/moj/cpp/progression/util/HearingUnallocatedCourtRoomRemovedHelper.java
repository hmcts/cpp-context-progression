package uk.gov.moj.cpp.progression.util;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.util.ArrayList;
import java.util.List;

public class HearingUnallocatedCourtRoomRemovedHelper {
    private HearingUnallocatedCourtRoomRemovedHelper(){

    }

    public static Hearing updateHearingOnCourtroomRemoval(final Hearing hearing, final Integer estimatedMinutes) {
        if (hearing != null) {
            CourtCentre courtCentreWithoutRoom = CourtCentre.
                    courtCentre().
                    withValuesFrom(hearing.getCourtCentre()).
                    withRoomId(null).
                    withRoomName(null).
                    withWelshRoomName(null).build();

            Hearing.Builder hearingBuilder = Hearing.hearing().withValuesFrom(hearing);
            hearingBuilder = hearingBuilder.withCourtCentre(courtCentreWithoutRoom);
            // final String estDurationStr = this.hearing.getEstimatedDuration();
            final List<HearingDay> hds = hearing.getHearingDays();
            if (hds != null && !hds.isEmpty()) {
                //multi-day hearing should be reduced to single day
                final HearingDay firstHearingDay = hds.get(0);
                final HearingDay hearingDayWithoutRoom = HearingDay.hearingDay().
                        withValuesFrom(firstHearingDay).withCourtRoomId(null).
                        withListedDurationMinutes(estimatedMinutes == null || hds.size() == 1 ? firstHearingDay.getListedDurationMinutes() : estimatedMinutes).
                        build();
                List<HearingDay> hearingDayWithoutRoomSingleton = new ArrayList<>();
                hearingDayWithoutRoomSingleton.add(hearingDayWithoutRoom);
                hearingBuilder = hearingBuilder.withHearingDays(hearingDayWithoutRoomSingleton);
            }

            return hearingBuilder.build();
        }

        return hearing;
    }
}
