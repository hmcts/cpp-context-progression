package uk.gov.justice.services;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.LocalDate;
import java.util.List;

public class HearingDaySharedResultsMapper {

    public boolean shouldSetHasSharedResults(final Hearing hearing) {
        final List<HearingDay> hearingDays = hearing.getHearingDays();
        boolean applicationShared = false;
        boolean caseShared = false;
        applicationShared = isApplicationShared(hearing, hearingDays, applicationShared);
        caseShared = isCaseShared(hearing, hearingDays, caseShared);
        return applicationShared || caseShared;
    }

    private boolean isCaseShared(final Hearing hearing, final List<HearingDay> hearingDays, boolean caseShared) {
        if (nonNull(hearing.getProsecutionCases())) {
            caseShared = hearing.getProsecutionCases().stream()
                    .filter(prosecutionCase -> nonNull(prosecutionCase.getDefendants()))
                    .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .filter(defendant -> nonNull(defendant.getOffences()))
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> nonNull(offence.getJudicialResults()))
                    .flatMap(offence -> offence.getJudicialResults().stream())
                    .map(judicialResult ->
                            hearingDays.stream().anyMatch(hearingDay ->
                                    hearingDay.getSittingDay().toLocalDate().equals(judicialResult.getOrderedDate()))
                    )
                    .findFirst()
                    .orElse(false);
        }
        return caseShared;
    }

    private boolean isApplicationShared(final Hearing hearing, final List<HearingDay> hearingDays, boolean applicationShared) {
        if (nonNull(hearing.getCourtApplications())) {
            applicationShared = hearing.getCourtApplications().stream()
                    .filter(courtApplication -> nonNull(courtApplication.getJudicialResults()))
                    .flatMap(courtApplication -> courtApplication.getJudicialResults().stream())
                    .map(judicialResult -> {
                        final LocalDate orderedDate = judicialResult.getOrderedDate();
                        return hearingDays.stream().anyMatch(hearingDay ->
                                hearingDay.getSittingDay().toLocalDate().equals(orderedDate));
                    }).findFirst().orElse(false);
        }
        return applicationShared;
    }


}


