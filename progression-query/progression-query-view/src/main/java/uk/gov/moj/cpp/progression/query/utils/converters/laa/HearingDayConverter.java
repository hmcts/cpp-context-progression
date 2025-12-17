package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.progression.query.laa.HearingDay;

import java.util.List;

@SuppressWarnings("squid:S1168")
public class HearingDayConverter extends LAAConverter {

    public List<HearingDay> convert(final List<uk.gov.justice.core.courts.HearingDay> hearingDays) {
        if (isEmpty(hearingDays)) {
            return null;
        }
        return hearingDays.stream().map(hearingDay -> HearingDay.hearingDay()
                .withHasSharedResults(hearingDay.getHasSharedResults())
                .withListedDurationMinutes(hearingDay.getListedDurationMinutes())
                .withListingSequence(hearingDay.getListingSequence())
                .withSittingDay(ofNullable(hearingDay.getSittingDay()).map(HearingDayConverter::formatZonedDateTime).orElse(null))
                .build()).toList();
    }

}