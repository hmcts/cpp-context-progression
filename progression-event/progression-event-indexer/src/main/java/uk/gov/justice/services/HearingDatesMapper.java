package uk.gov.justice.services;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class HearingDatesMapper {
    public List<String> extractHearingDates(final Hearing hearing) {
        final List<String> hearingDates = new ArrayList<>();
        for (final HearingDay hearingDay :  ofNullable(hearing.getHearingDays()).orElse(Collections.emptyList())) {
            if (hearingDay.getSittingDay() != null && hearingDay.getSittingDay().toLocalDate() != null) {
                hearingDates.add(hearingDay.getSittingDay().toLocalDate().toString());
            }
        }
        return hearingDates;
    }
}
