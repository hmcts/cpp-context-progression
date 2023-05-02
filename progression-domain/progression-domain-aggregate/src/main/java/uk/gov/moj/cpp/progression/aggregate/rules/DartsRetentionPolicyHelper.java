package uk.gov.moj.cpp.progression.aggregate.rules;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Offence;

import java.util.ArrayList;
import java.util.List;

public class DartsRetentionPolicyHelper {

    final List<RetentionRule> retentionRuleList = new ArrayList<>();
    final DefaultRetentionRule defaultRetentionRule;

    public DartsRetentionPolicyHelper(final HearingInfo hearingInfo, final List<Offence> defendantsOffences,
                                      final List<DefendantJudicialResult> defendantJudicialResults,
                                      final List<String> remitResultIds) {
        retentionRuleList.add(new LifeSentenceRetentionRule(hearingInfo, defendantsOffences));
        retentionRuleList.add(new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, defendantsOffences));
        retentionRuleList.add(new RemittalRetentionRule(hearingInfo, defendantsOffences, remitResultIds));
        retentionRuleList.add(new AcquittalRetentionRule(hearingInfo, defendantJudicialResults, defendantsOffences));
        retentionRuleList.add(new NotGuiltyVerdictRetentionRule(hearingInfo, defendantsOffences));
        retentionRuleList.add(new NonCustodialSentenceRetentionRule(hearingInfo));
        defaultRetentionRule = new DefaultRetentionRule(hearingInfo);
    }

    public RetentionPolicy getRetentionPolicy() {
        final RetentionRule retentionRuleApplied = retentionRuleList.stream()
                .filter(RetentionRule::apply)
                .findFirst()
                .orElse(defaultRetentionRule);

        return retentionRuleApplied.getPolicy();
    }

    public List<RetentionRule> getRetentionRuleList() {
        return retentionRuleList;
    }
}
