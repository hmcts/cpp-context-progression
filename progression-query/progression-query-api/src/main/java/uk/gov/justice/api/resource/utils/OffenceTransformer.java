package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getDeletedResultAmendments;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getResultsWithAmendments;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.progression.courts.exract.Amendments;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.Offences;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

public class OffenceTransformer {

    public static Offences toOffences(uk.gov.justice.progression.courts.Offences offences, final List<JudicialResult> results,
                                      final CommittedForSentence committedForSentence, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final Set<String> guiltyPleaTypes) {
        return uk.gov.justice.progression.courts.exract.Offences.offences()
                .withId(offences.getId())
                .withOrderIndex(offences.getOrderIndex())
                .withConvictionDate(offences.getConvictionDate())
                .withConvictingCourt(offences.getConvictingCourt())
                .withAquittalDate(isNull(offences.getAcquittalDate()) ? calculateAcquittalDate(offences, results, guiltyPleaTypes) : offences.getAcquittalDate())
                .withCount(offences.getCount())
                .withEndDate(offences.getEndDate())
                .withIndicatedPlea(offences.getIndicatedPlea())
                .withAllocationDecision(offences.getAllocationDecision())
                .withStartDate(offences.getStartDate())
                .withOffenceDefinitionId(offences.getOffenceDefinitionId())
                .withOffenceLegislation(offences.getOffenceLegislation())
                .withOffenceLegislationWelsh(offences.getOffenceLegislationWelsh())
                .withOffenceCode(offences.getOffenceCode())
                .withOffenceTitle(offences.getOffenceTitle())
                .withOffenceTitleWelsh(offences.getOffenceTitleWelsh())
                .withResults(getResultsWithAmendments(results, resultIdSlipRuleAmendmentsMap))
                .withDeletedResults(getDeletedResultAmendments(offences.getId(), resultIdSlipRuleAmendmentsMap))
                .withNotifiedPlea(offences.getNotifiedPlea())
                .withWording(offences.getWording())
                .withPleas(offences.getPleas())
                .withVerdicts(offences.getVerdicts())
                .withWordingWelsh(offences.getWordingWelsh())
                .withIndictmentParticular(offences.getIndictmentParticular())
                .withCommittedForSentence(committedForSentence)
                .withProceedingsConcluded(offences.getProceedingsConcluded())
                .build();
    }

    public static Offences toOffences(Offence offence, final List<JudicialResult> results,
                                      final CommittedForSentence committedForSentence, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final Set<String> guiltyPleaTypes) {
        return uk.gov.justice.progression.courts.exract.Offences.offences()
                .withId(offence.getId())
                .withOrderIndex(offence.getOrderIndex())
                .withConvictionDate(offence.getConvictionDate())
                .withConvictingCourt(offence.getConvictingCourt())
                .withAquittalDate(isNull(offence.getAquittalDate()) ? calculateAcquittalDate(offence, results, guiltyPleaTypes) : offence.getAquittalDate())
                .withCount(offence.getCount())
                .withEndDate(offence.getEndDate())
                .withIndicatedPlea(offence.getIndicatedPlea())
                .withAllocationDecision(offence.getAllocationDecision())
                .withStartDate(offence.getStartDate())
                .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                .withOffenceLegislation(offence.getOffenceLegislation())
                .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withResults(getResultsWithAmendments(results, resultIdSlipRuleAmendmentsMap))
                .withDeletedResults(getDeletedResultAmendments(offence.getId(), resultIdSlipRuleAmendmentsMap))
                .withNotifiedPlea(offence.getNotifiedPlea())
                .withWording(offence.getWording())
                .withPleas(nonNull(offence.getPlea()) ? List.of(offence.getPlea()) : emptyList())
                .withVerdicts(nonNull(offence.getVerdict()) ? List.of(offence.getVerdict()) : emptyList())
                .withWordingWelsh(offence.getWordingWelsh())
                .withIndictmentParticular(offence.getIndictmentParticular())
                .withCommittedForSentence(committedForSentence)
                .withProceedingsConcluded(offence.getProceedingsConcluded())
                .build();
    }


    private static LocalDate calculateAcquittalDate(final uk.gov.justice.progression.courts.Offences offences, final List<JudicialResult> results, final Set<String> guiltyPleaTypes) {
        if( isValidToSetAcquittalDate(offences, results, guiltyPleaTypes)){
            return getMaxOrderDate(results);
        }
        return null;
    }

    private static LocalDate calculateAcquittalDate(final Offence offence, final List<JudicialResult> results, final Set<String> guiltyPleaTypes) {
        if( isValidToSetAcquittalDate(offence, results, guiltyPleaTypes)){
            return getMaxOrderDate(results);
        }
        return null;
    }

    private static LocalDate getMaxOrderDate(final List<JudicialResult> results) {
        return results.stream()
                .filter(judicialResult -> judicialResult.getCategory() == JudicialResultCategory.FINAL)
                .map(JudicialResult::getOrderedDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static boolean isValidToSetAcquittalDate(final uk.gov.justice.progression.courts.Offences offences, final List<JudicialResult> results, final Set<String> guiltyPleaTypes) {
        return isNull(offences.getAcquittalDate()) &&
                isNotGuiltyPlea(offences, guiltyPleaTypes) &&
                hasFinalResult(results) &&
                isNull(offences.getConvictionDate());
    }

    private static boolean isValidToSetAcquittalDate(final Offence offence, final List<JudicialResult> results, final Set<String> guiltyPleaTypes) {
        return isNull(offence.getAquittalDate()) &&
                isNotGuiltyPlea(offence, guiltyPleaTypes) &&
                hasFinalResult(results) &&
                isNull(offence.getConvictionDate());
    }

    private static boolean isNotGuiltyPlea(final uk.gov.justice.progression.courts.Offences offences, final Set<String> guiltyPleaTypes) {
        return CollectionUtils.isNotEmpty(offences.getPleas()) && offences.getPleas().stream().map(Plea::getPleaValue).noneMatch(guiltyPleaTypes::contains);
    }

    private static boolean isNotGuiltyPlea(Offence offence, final Set<String> guiltyPleaTypes) {
        return nonNull(offence.getPlea()) && guiltyPleaTypes.stream().noneMatch(g -> g.equals(offence.getPlea().getPleaValue()));
    }

    private static boolean hasFinalResult(final List<JudicialResult> results) {
        return results != null && results.stream().filter(Objects::nonNull).anyMatch(result -> JudicialResultCategory.FINAL == result.getCategory());
    }

}
