package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;

import uk.gov.justice.api.resource.dto.AmendmentType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.progression.courts.exract.Amendments;
import uk.gov.justice.progression.courts.exract.ApplicationResults;
import uk.gov.justice.progression.courts.exract.DefendantResults;
import uk.gov.justice.progression.courts.exract.DeletedApplicationResults;
import uk.gov.justice.progression.courts.exract.DeletedDefendantResults;
import uk.gov.justice.progression.courts.exract.DeletedResults;
import uk.gov.justice.progression.courts.exract.Results;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

public class JudicialResultTransformer {
    private static final String OFFENCE_LEVEL_RESULT = "O";
    private static final String DEFENDANT_LEVEL_RESULT = "D";

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

    public static List<Results> getResultsWithAmendments(final List<JudicialResult> results, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        if (CollectionUtils.isNotEmpty(results)) {
            return results.stream()
                    .map(result -> {
                        final Results.Builder resultsBuilder = Results.results().withResult(result);
                        if (nonNull(resultIdSlipRuleAmendmentsMap) && resultIdSlipRuleAmendmentsMap.containsKey(result.getJudicialResultId())) {
                            resultsBuilder.withAmendments(resultIdSlipRuleAmendmentsMap.get(result.getJudicialResultId()));
                        }

                        return resultsBuilder.build();
                    }).collect(toList());
        }
        return emptyList();
    }

    public static List<DeletedResults> getDeletedResultAmendments(final UUID offenceId, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        if (nonNull(resultIdSlipRuleAmendmentsMap)) {
            return resultIdSlipRuleAmendmentsMap.entrySet().stream()
                    .flatMap(amendmentList -> amendmentList.getValue().stream())
                    .filter(amendments -> AmendmentType.DELETED.name().equals(amendments.getAmendmentType()))
                    .filter(amendments -> OFFENCE_LEVEL_RESULT.equalsIgnoreCase(amendments.getResultLevel())
                            && offenceId.equals(amendments.getOffenceId()))
                    .map(amendments -> DeletedResults.deletedResults()
                            .withDefendantId(amendments.getDefendantId())
                            .withJudicialResultId(amendments.getJudicialResultId())
                            .withAmendmentDate(amendments.getAmendmentDate())
                            .withAmendmentType(amendments.getAmendmentType())
                            .withOffenceId(amendments.getOffenceId())
                            .withResultText(amendments.getResultText())
                            .withResultLevel(amendments.getResultLevel())
                            .build())
                    .distinct()
                    .sorted(comparing(DeletedResults::getAmendmentDate).reversed())
                    .toList();
        }
        return emptyList();
    }

    public static List<ApplicationResults> getApplicationResultsWithAmendmentsExcludingDefendantLevelResults(final List<JudicialResult> results, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        if (CollectionUtils.isNotEmpty(results)) {
            return results.stream()
                    .filter(result -> !DEFENDANT_LEVEL_RESULT.equals(result.getLevel()))
                    .map(result -> {
                        final ApplicationResults.Builder resultsBuilder = ApplicationResults.applicationResults().withResult(result);
                        if (nonNull(resultIdSlipRuleAmendmentsMap) && resultIdSlipRuleAmendmentsMap.containsKey(result.getJudicialResultId())) {
                            resultsBuilder.withAmendments(resultIdSlipRuleAmendmentsMap.get(result.getJudicialResultId()));
                        }
                        return resultsBuilder.build();
                    }).collect(toList());
        }

        return emptyList();
    }

    public static List<ApplicationResults> getApplicationResultsWithAmendments(final List<JudicialResult> results, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        if (CollectionUtils.isNotEmpty(results)) {
            return results.stream()
                    .map(result -> {
                        final ApplicationResults.Builder resultsBuilder = ApplicationResults.applicationResults().withResult(result);
                        if (nonNull(resultIdSlipRuleAmendmentsMap) && resultIdSlipRuleAmendmentsMap.containsKey(result.getJudicialResultId())) {
                            resultsBuilder.withAmendments(resultIdSlipRuleAmendmentsMap.get(result.getJudicialResultId()));
                        }
                        return resultsBuilder.build();
                    }).collect(toList());
        }

        return emptyList();
    }

    public static List<DeletedApplicationResults> getDeletedApplicationResultsWithAmendmentsExcludingDefendantLevelResults(final UUID applicationId, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        if (nonNull(resultIdSlipRuleAmendmentsMap)) {
            return resultIdSlipRuleAmendmentsMap.entrySet().stream()
                    .flatMap(amendmentList -> amendmentList.getValue().stream())
                    .filter(amendments -> AmendmentType.DELETED.name().equals(amendments.getAmendmentType()))
                    .filter(amendments -> applicationId.equals(amendments.getApplicationId()))
                    .filter(amendments -> isNull(amendments.getOffenceId()))
                    .filter(amendments -> !DEFENDANT_LEVEL_RESULT.equalsIgnoreCase(amendments.getResultLevel()))
                    .map(amendments -> DeletedApplicationResults.deletedApplicationResults()
                            .withDefendantId(amendments.getDefendantId())
                            .withJudicialResultId(amendments.getJudicialResultId())
                            .withAmendmentDate(amendments.getAmendmentDate())
                            .withAmendmentType(amendments.getAmendmentType())
                            .withApplicationId(amendments.getApplicationId())
                            .withResultText(amendments.getResultText())
                            .withResultLevel(amendments.getResultLevel())
                            .build())
                    .distinct()
                    .sorted(comparing(DeletedApplicationResults::getAmendmentDate).reversed())
                    .toList();
        }
        return emptyList();
    }

    public static List<DefendantResults> getDefendantResultsWithAmendments(final List<JudicialResult> results, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        if (CollectionUtils.isNotEmpty(results)) {
            return results.stream()
                    .map(result -> {
                        final DefendantResults.Builder resultsBuilder = DefendantResults.defendantResults().withResult(result);
                        if (nonNull(resultIdSlipRuleAmendmentsMap) && resultIdSlipRuleAmendmentsMap.containsKey(result.getJudicialResultId())) {
                            resultsBuilder.withAmendments(resultIdSlipRuleAmendmentsMap.get(result.getJudicialResultId()));
                        }
                        return resultsBuilder.build();
                    }).collect(toList());
        }

        return emptyList();
    }

    public static List<DeletedDefendantResults> getDeletedDefendantResultsWithAmendments(final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        if (nonNull(resultIdSlipRuleAmendmentsMap)) {
            return resultIdSlipRuleAmendmentsMap.entrySet().stream()
                    .flatMap(amendmentList -> amendmentList.getValue().stream())
                    .filter(amendments -> AmendmentType.DELETED.name().equals(amendments.getAmendmentType()))
                    .filter(amendments -> nonNull(amendments.getOffenceId()))
                    .filter(amendments -> DEFENDANT_LEVEL_RESULT.equalsIgnoreCase(amendments.getResultLevel()))
                    .map(amendments -> DeletedDefendantResults.deletedDefendantResults()
                            .withDefendantId(amendments.getDefendantId())
                            .withJudicialResultId(amendments.getJudicialResultId())
                            .withAmendmentDate(amendments.getAmendmentDate())
                            .withAmendmentType(amendments.getAmendmentType())
                            .withOffenceId(amendments.getOffenceId())
                            .withResultText(amendments.getResultText())
                            .withResultLevel(amendments.getResultLevel())
                            .build())
                    .distinct()
                    .sorted(comparing(DeletedDefendantResults::getAmendmentDate).reversed())
                    .toList();
        }
        return emptyList();
    }

    private static List<JudicialResultPrompt> filterOutPromptsNotToBeShownInCourtExtract(final JudicialResult result) {
        return CollectionUtils.isNotEmpty(result.getJudicialResultPrompts()) ? result.getJudicialResultPrompts().stream().filter(jrp -> "Y".equals(jrp.getCourtExtract())).toList() : result.getJudicialResultPrompts();
    }

    public static List<DeletedApplicationResults> getDeletedApplicationResultsWithAmendments(final UUID applicationId, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        if (nonNull(resultIdSlipRuleAmendmentsMap)) {
            return resultIdSlipRuleAmendmentsMap.entrySet().stream()
                    .flatMap(amendmentList -> amendmentList.getValue().stream())
                    .filter(amendments -> AmendmentType.DELETED.name().equals(amendments.getAmendmentType()))
                    .filter(amendments -> applicationId.equals(amendments.getApplicationId()))
                    .map(amendments -> DeletedApplicationResults.deletedApplicationResults()
                            .withDefendantId(amendments.getDefendantId())
                            .withJudicialResultId(amendments.getJudicialResultId())
                            .withAmendmentDate(amendments.getAmendmentDate())
                            .withAmendmentType(amendments.getAmendmentType())
                            .withApplicationId(amendments.getApplicationId())
                            .withResultText(amendments.getResultText())
                            .withResultLevel(amendments.getResultLevel())
                            .build())
                    .distinct()
                    .sorted(comparing(DeletedApplicationResults::getAmendmentDate).reversed())
                    .toList();
        }
        return emptyList();
    }

}
