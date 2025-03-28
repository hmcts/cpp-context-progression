package uk.gov.justice.api.resource.utils;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.api.resource.utils.ResultTextHelper.getResultText;

import uk.gov.justice.api.resource.dto.AmendmentRecord;
import uk.gov.justice.api.resource.dto.AmendmentType;
import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.dto.ResultLine;
import uk.gov.justice.api.resource.dto.ResultPrompt;
import uk.gov.justice.progression.courts.exract.Amendments;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResultAmendmentHelper {

    public static Map<UUID, List<Amendments>> extractAmendmentsDueToSlipRule(final List<DraftResultsWrapper> defendantResultsWithAmendments, final List<ResultDefinition> resultDefinitions, final UUID slipRuleReasonId) {

        final Map<UUID, List<Amendments>> resultLineAmendmentMap = new HashMap<>();
        if (isNotEmpty(defendantResultsWithAmendments)) {
            defendantResultsWithAmendments.forEach(draftResultsWrapper -> {
                final List<ResultLine> resultLines = draftResultsWrapper.getResultLines();
                if (isNotEmpty(resultLines)) {
                    resultLines
                            .forEach(resultLine -> {
                                final ResultDefinition resultDefinition = getAssociatedResultDefinition(resultLine.getResultDefinitionId(), resultDefinitions);
                                final ZonedDateTime sharedDate = nonNull(draftResultsWrapper.getLastSharedTime()) ? draftResultsWrapper.getLastSharedTime() : resultLine.getSharedDate();
                                final List<AmendmentRecord> amendmentsWithSlipRule = getAmendmentsWithSlipRule(sharedDate, resultLine.getAmendmentsLog().getAmendmentsRecord(), slipRuleReasonId);

                                if (isNotEmpty(amendmentsWithSlipRule)) {
                                    final List<Amendments> amendmentList = amendmentsWithSlipRule.stream()
                                            .map(amendmentRecord -> toAmendment(resultLine, amendmentRecord, resultDefinition))
                                            .distinct()
                                            .sorted(comparing(Amendments::getAmendmentDate).reversed())
                                            .toList();
                                    resultLineAmendmentMap.put(resultLine.getResultLineId(), amendmentList);
                                }
                            });
                }
            });
        }
        return resultLineAmendmentMap;
    }

    private static ResultDefinition getAssociatedResultDefinition(final UUID resultDefinitionId, final List<ResultDefinition> resultDefinitions) {
        return resultDefinitions.stream().filter(resultDefinition -> resultDefinitionId.equals(resultDefinition.getId())).findFirst().orElse(null);
    }

    public static List<UUID> getResultDefinitionsInSlipRuleAmendments(final List<DraftResultsWrapper> defendantResultsWithAmendments, final UUID slipRuleReasonId) {
        return filterSlipRuleAmendments(defendantResultsWithAmendments, slipRuleReasonId).stream()
                .map(ResultLine::getResultDefinitionId)
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<ResultLine> filterSlipRuleAmendments(final List<DraftResultsWrapper> defendantResultsWithAmendments, final UUID slipRuleReasonId) {
        final List<ResultLine> resultLinesWithSlipRuleAmendments = new ArrayList<>();

        if (isNotEmpty(defendantResultsWithAmendments)) {
            defendantResultsWithAmendments.forEach(draftResultsWrapper -> {
                final List<ResultLine> resultLines = draftResultsWrapper.getResultLines();
                if (isNotEmpty(resultLines)) {
                    resultLinesWithSlipRuleAmendments.addAll(resultLines.stream()
                            .filter(resultLine -> nonNull(resultLine.getAmendmentsLog())
                                    && nonNull(resultLine.getAmendmentsLog().getAmendmentsRecord()))
                            .filter(resultLine -> {
                                final ZonedDateTime sharedDate = nonNull(draftResultsWrapper.getLastSharedTime()) ? draftResultsWrapper.getLastSharedTime() : resultLine.getSharedDate();
                                return isNotEmpty(getAmendmentsWithSlipRule(sharedDate, resultLine.getAmendmentsLog().getAmendmentsRecord(), slipRuleReasonId));
                            })
                            .toList());
                }
            });
        }

        return resultLinesWithSlipRuleAmendments;
    }

    private static List<AmendmentRecord> getAmendmentsWithSlipRule(final ZonedDateTime sharedDate, final List<AmendmentRecord> amendmentsRecord, final UUID slipRuleReasonId) {
        return amendmentsRecord.stream()
                .filter(amendmentRecord -> nonNull(sharedDate) && amendmentRecord.getAmendmentDate().isBefore(sharedDate))
                .filter(amendmentRecord -> slipRuleReasonId.equals(amendmentRecord.getAmendmentReason().getId()))
                .toList();
    }

    private static Amendments toAmendment(final ResultLine resultLine, final AmendmentRecord amendmentRecord, final ResultDefinition resultDefinition) {
        final List<ResultPrompt> amendmentPrompts = isNotEmpty(amendmentRecord.getResultPromptsRecord()) ? amendmentRecord.getResultPromptsRecord() : resultLine.getResultPrompts();
        final String resultText = isNotEmpty(amendmentPrompts) ? getResultText(resultDefinition, amendmentPrompts) : resultDefinition.getLabel();

        return Amendments.amendments()
                .withDefendantId(resultLine.getDefendantId())
                .withJudicialResultId(resultLine.getResultLineId())
                .withOffenceId(resultLine.getOffenceId())
                .withApplicationId(resultLine.getApplicationId())
                .withResultLevel(resultLine.getResultLevel())
                .withAmendmentType(getAmendmentType(resultLine, amendmentRecord.getResultPromptsRecord()).name())
                .withResultText(resultText)
                .withAmendmentDate(amendmentRecord.getAmendmentDate())
                .build();
    }

    private static AmendmentType getAmendmentType(final ResultLine resultLine, final List<ResultPrompt> amendmentPrompts) {
        if (Boolean.TRUE.equals(resultLine.getDeleted())) {
            return AmendmentType.DELETED;
        }

        final boolean singleAmendmentRecord = nonNull(resultLine.getAmendmentsLog())
                && resultLine.getAmendmentsLog().getAmendmentsRecord().size() == 1
                && resultLine.getAmendmentDate().equals(resultLine.getAmendmentsLog().getAmendmentsRecord().get(0).getAmendmentDate());

        if (singleAmendmentRecord &&
                (isPromptsAdded(resultLine.getResultPrompts(), amendmentPrompts)
                        || isPromptsEqual(resultLine.getResultPrompts(), amendmentPrompts))) {
            return AmendmentType.ADDED;
        }

        return AmendmentType.AMENDED;
    }

    private static boolean isPromptsAdded(final List<ResultPrompt> resultLinePrompts, final List<ResultPrompt> amendmentPrompts) {
        return isEmpty(amendmentPrompts) && isNotEmpty(resultLinePrompts);
    }

    private static boolean isPromptsEqual(final List<ResultPrompt> resultLinePrompts, final List<ResultPrompt> amendmentPrompts) {
        if (isEmpty(resultLinePrompts) && isEmpty(amendmentPrompts)) {
            return true;
        }

        if (isNotEmpty(resultLinePrompts) && isNotEmpty(amendmentPrompts) && resultLinePrompts.size() == amendmentPrompts.size()) {
            for (ResultPrompt resultLinePrompt : resultLinePrompts) {
                if (!amendmentPrompts.contains(resultLinePrompt)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
