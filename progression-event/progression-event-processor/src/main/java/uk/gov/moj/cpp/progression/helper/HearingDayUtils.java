package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.core.courts.HearingDay;

import java.time.ZonedDateTime;
import java.util.List;

public class HearingDayUtils {

    private HearingDayUtils() {
    }

    public static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);

    }
}
