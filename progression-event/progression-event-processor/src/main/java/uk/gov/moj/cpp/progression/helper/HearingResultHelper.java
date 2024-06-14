package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingResultHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultHelper.class.getCanonicalName());

    private static final UUID APPROVED_SUMMONS = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");

    public boolean doHearingContainNextHearingResults(final Hearing hearing) {
        final boolean prosecutionCasesContainNextHearingResults = doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
        final boolean courtApplicationsContainNextHearingResults = doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
        return prosecutionCasesContainNextHearingResults || courtApplicationsContainNextHearingResults;
    }

    public List<JudicialResult> getAllJudicialResultsFromApplication(final CourtApplication courtApplication){
        final List<JudicialResult> judicialResults = courtApplication.getJudicialResults();
        final List<JudicialResult> updatedJudicialResults = Optional.ofNullable(judicialResults)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);

        ofNullable(courtApplication.getCourtOrder())
                .map(CourtOrder::getCourtOrderOffences)
                .orElseGet(ArrayList::new)
                .stream()
                .map(CourtOrderOffence::getOffence)
                .flatMap(o -> ofNullable(o.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .forEach(updatedJudicialResults::add);

        ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(cac -> ofNullable(cac.getOffences()).map(Collection::stream).orElseGet(Stream::empty))
                .flatMap(o -> ofNullable(o.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .forEach(updatedJudicialResults::add);

        return updatedJudicialResults;
    }

    private boolean doProsecutionCasesContainNextHearingResults(final List<ProsecutionCase> prosecutionCases) {
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

    private boolean doCourtApplicationsContainNextHearingResults(final List<CourtApplication> courtApplications) {
        if (isNotEmpty(courtApplications)) {
            final long count =  courtApplications.stream()
                    .flatMap(courtApplication -> this.getAllJudicialResultsFromApplication(courtApplication).stream())
                    .map(JudicialResult::getNextHearing)
                    .filter(Objects::nonNull)
                    .count();
            return count > 0;
        } else {
            return false;
        }
    }

    public boolean isSummonsApproved(final CourtApplication courtApplication) {
        return courtApplication.getJudicialResults().stream()
                    .filter(Objects::nonNull)
                    .map(JudicialResult::getJudicialResultTypeId)
                    .anyMatch(APPROVED_SUMMONS::equals);
    }

    public boolean isSummonsRequiredForRespondents(final List<CourtApplicationParty> respondents) {
        if(isNotEmpty(respondents)) {
            final Predicate<CourtApplicationParty> summonRequiredFilter = CourtApplicationParty::getSummonsRequired;
            final Predicate<CourtApplicationParty> summonNotRequiredFilter = courtApplicationParty -> !courtApplicationParty.getSummonsRequired();

            final boolean summonRequired = respondents.stream().anyMatch(summonRequiredFilter);
            final boolean summonNotRequired = respondents.stream().anyMatch(summonNotRequiredFilter);
            if (summonRequired && summonNotRequired) {
                LOGGER.error("Application has respondents with summon required and summon not required");
                throw new DataValidationException("Application has respondents with summon required and summon not required");
            }
            return summonRequired;
        }
        return false;
    }
}
