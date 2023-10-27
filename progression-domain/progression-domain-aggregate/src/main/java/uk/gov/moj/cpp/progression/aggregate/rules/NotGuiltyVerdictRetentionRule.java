package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NOT_GUILTY;

import uk.gov.justice.core.courts.Offence;

import java.util.List;

public class NotGuiltyVerdictRetentionRule implements RetentionRule {

    static final String NOT_GUILTY_SENTENCE = "1Y0M0D";
    static final List<String> NOT_GUILTY_CJS_VERDICT_CODES = asList("N", "NGJU", "NGJA", "NGJJ");

    private HearingInfo hearingInfo;
    private final List<Offence> defendantsOffences;

    public NotGuiltyVerdictRetentionRule(final HearingInfo hearingInfo, final List<Offence> defendantsOffences) {
        this.hearingInfo = hearingInfo;
        this.defendantsOffences = unmodifiableList(nonNull(defendantsOffences) ? defendantsOffences : emptyList());
    }

    @Override
    public boolean apply() {
        if (isNull(defendantsOffences) || defendantsOffences.isEmpty()
                || defendantsOffences.stream().anyMatch(o -> isNull(o.getVerdict()))) {
            return false;
        }

        return defendantsOffences.stream()
                .filter(offence -> nonNull(offence.getVerdict().getVerdictType()))
                .allMatch(offence -> NOT_GUILTY_CJS_VERDICT_CODES.contains(offence.getVerdict().getVerdictType().getCjsVerdictCode()));
    }

    @Override
    public RetentionPolicy getPolicy() {
        return new RetentionPolicy(NOT_GUILTY, NOT_GUILTY_SENTENCE, hearingInfo);
    }
}
