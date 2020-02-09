package uk.gov.justice.services;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.util.ArrayList;
import java.util.List;

public class HearingDatesMapper {
    public List<String> extractHearingDates(final Hearing hearing) {
        final List<String> hearingDates = new ArrayList<>();
        for (final HearingDay hearingDay : hearing.getHearingDays()) {
            if (hearingDay.getSittingDay() != null && hearingDay.getSittingDay().toLocalDate() != null) {
                hearingDates.add(hearingDay.getSittingDay().toLocalDate().toString());
            }
        }
        return hearingDates;
    }
}
