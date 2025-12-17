package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:S1168", "java:S3358"})
public class DateOfNextHearingConverter extends LAAConverter {

    public String convert(final List<Hearing> hearingList) {
        if (isEmpty(hearingList)) {
            return null;
        }

        final ZonedDateTime now = ZonedDateTime.now();

        return hearingList.stream()
                .map(Hearing::getHearingDays)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(List::stream)
                .map(HearingDay::getSittingDay)
                .filter(Objects::nonNull)
                .sorted()
                .reduce((nearest, current) -> {
                    if (current.isAfter(now)) {
                        return nearest.isAfter(now) ? nearest : current;
                    } else {
                        return nearest.isAfter(now) ? nearest : (current.isAfter(nearest) ? current : nearest);
                    }
                })
                .map(DateOfNextHearingConverter::formatZonedDateTime)
                .orElse(null);
    }
}