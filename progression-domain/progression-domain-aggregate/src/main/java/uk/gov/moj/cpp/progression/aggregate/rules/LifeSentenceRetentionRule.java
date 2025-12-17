package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.lang.Boolean.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.LIFE;

import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.UUID;

public class LifeSentenceRetentionRule implements RetentionRule {

    static final String LIFE_SENTENCE = "99Y0M0D";
    private HearingInfo hearingInfo;
    private final List<Offence> defendantsOffences;
    static final UUID LIFE_JUDICIAL_RESULT_PROMPT_TYPE_ID = fromString("9dbe839c-3804-4c47-bf9e-5be6f9b9b3bb");
    static final String LIFE_JUDICIAL_RESULT_PROMPT_REF = "totalCustodialPeriodIsLife";


    public LifeSentenceRetentionRule(final HearingInfo hearingInfo, final List<Offence> defendantsOffences) {
        this.hearingInfo = hearingInfo;
        this.defendantsOffences = unmodifiableList(nonNull(defendantsOffences) ? defendantsOffences : emptyList());
    }

    @Override
    public boolean apply() {
        if (isNull(defendantsOffences)) {
            return false;
        }

        return defendantsOffences.stream()
                .filter(offence -> nonNull(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .filter(jrStream -> nonNull(jrStream.getJudicialResultPrompts()))
                .flatMap(jrStream -> jrStream.getJudicialResultPrompts().stream())
                .filter(judicialResultPrompt -> LIFE_JUDICIAL_RESULT_PROMPT_TYPE_ID.equals(judicialResultPrompt.getJudicialResultPromptTypeId()) &&
                        LIFE_JUDICIAL_RESULT_PROMPT_REF.equals(judicialResultPrompt.getPromptReference()))
                .anyMatch(judicialResultPrompt -> Boolean.TRUE.equals(valueOf(judicialResultPrompt.getValue())));
    }

    @Override
    public RetentionPolicy getPolicy() {
        return new RetentionPolicy(LIFE, LIFE_SENTENCE, hearingInfo);
    }
}
