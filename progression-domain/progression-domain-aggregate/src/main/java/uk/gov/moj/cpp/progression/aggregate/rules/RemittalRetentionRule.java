package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.REMITTAL;

import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.stream.Stream;

public class RemittalRetentionRule implements RetentionRule {

    static final String REMITTAL_SENTENCE = "7Y0M0D";
    private HearingInfo hearingInfo;
    private final List<Offence> defendantsOffences;
    private List<String> remitResultIds;

    public RemittalRetentionRule(final HearingInfo hearingInfo, final List<Offence> defendantsOffences, final List<String> remitResultIds) {
        this.hearingInfo = hearingInfo;
        this.defendantsOffences = unmodifiableList(nonNull(defendantsOffences) ? defendantsOffences : emptyList());
        this.remitResultIds = unmodifiableList(nonNull(remitResultIds) ? remitResultIds : emptyList());
    }

    @Override
    public boolean apply() {
        if (isEmpty(defendantsOffences) || isEmpty(remitResultIds)) {
            return false;
        }

        return defendantsOffences.stream()
                .flatMap(offence -> offence.getJudicialResults()!=null?offence.getJudicialResults().stream(): Stream.empty())
                .filter(judicialResult -> nonNull(judicialResult.getJudicialResultTypeId()))
                .anyMatch(judicialResult -> remitResultIds.contains(judicialResult.getJudicialResultTypeId().toString()));
    }

    @Override
    public RetentionPolicy getPolicy() {
        return new RetentionPolicy(REMITTAL, REMITTAL_SENTENCE, hearingInfo);
    }
}
