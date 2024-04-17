package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.LocalDate;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:S2234"})
public class HearingHelper {

    private static final UtcClock utcClock = new UtcClock();

    HearingHelper() {
    }

    /**
     * Don't alter next hearings if any of the existing next hearings are dated in the past or
     * today.
     *
     * @param hearing - the hearing being shared with latest results
     * @return TRUE if next hearings should be generated from these shared results.
     */
    public static boolean isEligibleForNextHearings(final Hearing hearing) {
        boolean eligibleForNextHearings = true;

        final boolean isSingleDayHearing = nonNull(hearing.getHearingDays()) && hearing.getHearingDays().size() == 1;
        if (!isSingleDayHearing && hearing.getEarliestNextHearingDate() != null) {
            final LocalDate currentDay = utcClock.now().toLocalDate();
            final LocalDate earliestNextHearingDate = hearing.getEarliestNextHearingDate().toLocalDate();

            eligibleForNextHearings = currentDay.isBefore(earliestNextHearingDate);
        }
        return eligibleForNextHearings;
    }

}
