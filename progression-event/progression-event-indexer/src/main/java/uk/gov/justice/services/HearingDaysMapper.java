package uk.gov.justice.services;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.DomainToIndexMapper.ISO_8601_FORMATTER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S3776")
public class HearingDaysMapper {

    final HearingDaySharedResultsMapper hasSharedResultsMapper;

    public HearingDaysMapper(final HearingDaySharedResultsMapper hearingDaySharedResultsMapper){
        this.hasSharedResultsMapper = hearingDaySharedResultsMapper;
    }

    public List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> extractHearingDays(final Hearing hearing) {
        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDayIndexes = new ArrayList<>();
        for (final HearingDay hearingDay : ofNullable(hearing.getHearingDays()).orElse(Collections.emptyList())) {

            if (nonNull(hearingDay)) {
                hearingDayIndexes.add(generateHearingIndex(hearing, hearingDay));
            }
        }
        return hearingDayIndexes;
    }

    private uk.gov.justice.services.unifiedsearch.client.domain.HearingDay generateHearingIndex(final Hearing hearing, final HearingDay hearingDay) {

        final uk.gov.justice.services.unifiedsearch.client.domain.HearingDay hearingDayIndex = new uk.gov.justice.services.unifiedsearch.client.domain.HearingDay();
        //if old event format, get courtCentre info from parent
        final UUID courtRoomId = isNull(hearingDay.getCourtRoomId()) ? hearing.getCourtCentre().getRoomId() : hearingDay.getCourtRoomId();
        final UUID courtCentreId = isNull(hearingDay.getCourtCentreId()) ? hearing.getCourtCentre().getId() : hearingDay.getCourtCentreId();

        if (nonNull(hearingDay.getListingSequence())) {
            hearingDayIndex.setListingSequence(hearingDay.getListingSequence());
        }
        if (nonNull(hearingDay.getListedDurationMinutes())) {
            hearingDayIndex.setListedDurationMinutes(hearingDay.getListedDurationMinutes());
        }
        if (nonNull(hearingDay.getSittingDay())) {
            hearingDayIndex.setSittingDay(hearingDay.getSittingDay().format(ISO_8601_FORMATTER));
        }
        if (nonNull(courtCentreId)) {
            hearingDayIndex.setCourtCentreId(courtCentreId.toString());
        }
        if (nonNull(courtRoomId)) {
            hearingDayIndex.setCourtRoomId(courtRoomId.toString());
        }
        final Boolean hasSharedResults = hearingDay.getHasSharedResults();
        if (nonNull(hasSharedResults)) {
            hearingDayIndex.setHasSharedResults(hasSharedResults);
        }
        setHearingResultsShared(hearing, hearingDayIndex);
        return hearingDayIndex;
    }

    private void setHearingResultsShared(final Hearing hearing, final uk.gov.justice.services.unifiedsearch.client.domain.HearingDay hearingDayIndex) {
        if (isNull(hearing.getHasSharedResults())) {
            if (hasSharedResultsMapper.shouldSetHasSharedResults(hearing)) {
                hearingDayIndex.setHasSharedResults(true);
            } else {
                hearingDayIndex.setHasSharedResults(false);
            }
        }
    }
}