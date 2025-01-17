package uk.gov.justice.api.resource.utils;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class JudicialResultTransformer {
    public static JudicialResult toCourtJudicialResult(final JudicialResult result) {
        // If result contains resultWording then set the resultText with resultWording.
        if (isNotEmpty(result.getResultWording())) {
            return judicialResult()
                    .withAlwaysPublished(result.getAlwaysPublished())
                    .withAmendmentDate(result.getAmendmentDate())
                    .withAmendmentReason(result.getAmendmentReason())
                    .withAmendmentReasonId(result.getAmendmentReasonId())
                    .withApprovedDate(result.getApprovedDate())
                    .withCategory(result.getCategory())
                    .withCjsCode(result.getCjsCode())
                    .withCourtClerk(result.getCourtClerk())
                    .withD20(result.getD20())
                    .withDelegatedPowers(result.getDelegatedPowers())
                    .withDurationElement(result.getDurationElement())
                    .withExcludedFromResults(result.getExcludedFromResults())
                    .withFourEyesApproval(result.getFourEyesApproval())
                    .withIsAdjournmentResult(result.getIsAdjournmentResult())
                    .withIsAvailableForCourtExtract(result.getIsAvailableForCourtExtract())
                    .withIsConvictedResult(result.getIsConvictedResult())
                    .withIsDeleted(result.getIsDeleted())
                    .withIsFinancialResult(result.getIsFinancialResult())
                    .withJudicialResultId(result.getJudicialResultId())
                    .withJudicialResultPrompts(filterOutPromptsNotToBeShownInCourtExtract(result))
                    .withJudicialResultTypeId(result.getJudicialResultTypeId())
                    .withLabel(result.getLabel())
                    .withLastSharedDateTime(result.getLastSharedDateTime())
                    .withLifeDuration(result.getLifeDuration())
                    .withNextHearing(result.getNextHearing())
                    .withOrderedDate(result.getOrderedDate())
                    .withOrderedHearingId(result.getOrderedHearingId())
                    .withPostHearingCustodyStatus(result.getPostHearingCustodyStatus())
                    .withPublishedForNows(result.getPublishedForNows())
                    .withPublishedAsAPrompt(result.getPublishedAsAPrompt())
                    .withQualifier(result.getQualifier())
                    .withRank(result.getRank())
                    .withResultDefinitionGroup(result.getResultDefinitionGroup())
                    .withResultText(result.getResultText())
                    .withResultWording(result.getResultWording())
                    .withRollUpPrompts(result.getRollUpPrompts())
                    .withTerminatesOffenceProceedings(result.getTerminatesOffenceProceedings())
                    .withUrgent(result.getUrgent())
                    .withUsergroups(result.getUsergroups())
                    .withWelshLabel(result.getWelshLabel())
                    .withWelshResultWording(result.getWelshResultWording())
                    .withPoliceSubjectLineTitle(result.getPoliceSubjectLineTitle())
                    .build();
        }
        return judicialResult()
                .withValuesFrom(result)
                .withJudicialResultPrompts(filterOutPromptsNotToBeShownInCourtExtract(result))
                .build();
    }

    private static List<JudicialResultPrompt> filterOutPromptsNotToBeShownInCourtExtract(final JudicialResult result) {
        return CollectionUtils.isNotEmpty(result.getJudicialResultPrompts()) ? result.getJudicialResultPrompts().stream().filter(jrp -> "Y".equals(jrp.getCourtExtract())).toList() : result.getJudicialResultPrompts();
    }

}
