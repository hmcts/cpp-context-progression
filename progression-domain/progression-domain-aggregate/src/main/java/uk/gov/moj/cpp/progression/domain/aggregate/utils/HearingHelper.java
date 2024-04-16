package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Objects.nonNull;

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


    /**
     * This method gets the earliest listed start time from list of ListHearingRequest
     *
     * @param listHearingRequests  - details of listing of hearing
     * @return get the earliest Listed start date
     */
    public static ZonedDateTime getEarliestListedStartDateTime(final List<ListHearingRequest> listHearingRequests) {

        return listHearingRequests.stream()
                .map(listHearingRequest-> nonNull(listHearingRequest.getListedStartDateTime()) ? listHearingRequest.getListedStartDateTime() : listHearingRequest.getEarliestStartDateTime())
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }


}
