package uk.gov.justice.services;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HearingDaysMapper {

    public List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> extractHearingDays(final Hearing hearing) {
        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = new ArrayList<>();
        for (final HearingDay hearingDay : hearing.getHearingDays()) {
            final uk.gov.justice.services.unifiedsearch.client.domain.HearingDay hearingDayIndex
                    = new uk.gov.justice.services.unifiedsearch.client.domain.HearingDay();
            if (hearingDay != null) {
                if (hearingDay.getListingSequence() != null) {
                    hearingDayIndex.setListingSequence(hearingDay.getListingSequence());
                }
                if (hearingDay.getListedDurationMinutes() != null) {
                    hearingDayIndex.setListedDurationMinutes(hearingDay.getListedDurationMinutes());
                }
                if (hearingDay.getSittingDay() != null) {
                    hearingDayIndex.setSittingDay(hearingDay.getSittingDay().format(DateTimeFormatter.ISO_INSTANT));
                }
                hearingDays.add(hearingDayIndex);
            }
        }
        return hearingDays;
    }
}
