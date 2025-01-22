package uk.gov.justice.api.resource.utils;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.Offences;

import java.util.List;

public class OffenceTransformer {

    public static Offences toOffences(uk.gov.justice.progression.courts.Offences offences, final List<JudicialResult> results,
                                      final CommittedForSentence committedForSentence) {
        return uk.gov.justice.progression.courts.exract.Offences.offences()
                .withId(offences.getId())
                .withOrderIndex(offences.getOrderIndex())
                .withConvictionDate(offences.getConvictionDate())
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
                .withResults(results)
                .withNotifiedPlea(offences.getNotifiedPlea())
                .withWording(offences.getWording())
                .withPleas(offences.getPleas())
                .withVerdicts(offences.getVerdicts())
                .withWordingWelsh(offences.getWordingWelsh())
                .withIndictmentParticular(offences.getIndictmentParticular())
                .withCommittedForSentence(committedForSentence)
                .build();
    }
}
