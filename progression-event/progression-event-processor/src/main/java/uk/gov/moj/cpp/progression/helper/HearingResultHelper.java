package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class HearingResultHelper {

    public boolean doProsecutionCasesContainNextHearingResults(final List<ProsecutionCase> prosecutionCases) {
        if (isNotEmpty(prosecutionCases)) {
            final long count = prosecutionCases.stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .map(Defendant::getOffences)
                    .flatMap(Collection::stream)
                    .map(Offence::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                    .count();
            return count > 0;
        } else {
            return false;
        }
    }

    public boolean doCourtApplicationsContainNextHearingResults(final List<CourtApplication> courtApplications) {
        if (isNotEmpty(courtApplications)) {
            final long count = courtApplications.stream()
                    .map(CourtApplication::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                    .count();
            return count > 0;
        } else {
            return false;
        }
    }

}
