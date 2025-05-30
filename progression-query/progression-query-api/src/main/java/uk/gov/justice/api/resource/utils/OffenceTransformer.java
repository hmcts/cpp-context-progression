package uk.gov.justice.api.resource.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getDeletedResultAmendments;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getResultsWithAmendments;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.progression.courts.exract.Amendments;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.Offences;

import java.time.LocalDate;
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

    private static LocalDate calculateAcquittalDate(final uk.gov.justice.progression.courts.Offences offences, final List<JudicialResult> results, final Set<String> guiltyPleaTypes) {
        if( isValidToSetAcquittalDate(offences, results, guiltyPleaTypes)){
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

    private static boolean isNotGuiltyPlea(final uk.gov.justice.progression.courts.Offences offences, final Set<String> guiltyPleaTypes) {
        return CollectionUtils.isNotEmpty(offences.getPleas()) && offences.getPleas().stream().map(Plea::getPleaValue).noneMatch(guiltyPleaTypes::contains);
    }

    private static boolean hasFinalResult(final List<JudicialResult> results) {
        return results != null && results.stream().filter(Objects::nonNull).anyMatch(result -> JudicialResultCategory.FINAL == result.getCategory());
    }

}
