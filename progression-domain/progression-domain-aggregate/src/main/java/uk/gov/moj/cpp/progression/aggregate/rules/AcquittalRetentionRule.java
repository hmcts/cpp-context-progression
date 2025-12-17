package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.ACQUITTAL;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AcquittalRetentionRule implements RetentionRule {

    static final String ACQUITTAL_SENTENCE = "1Y0M0D";
    private HearingInfo hearingInfo;
    private final List<DefendantJudicialResult> defendantJudicialResults;
    private final List<Offence> defendantsOffences;
    static final UUID DISCH_RESULT_DEFINITION_ID = fromString("d3139b79-696b-4cb7-a39d-7f06fcc24f4a");

    public AcquittalRetentionRule(final HearingInfo hearingInfo, final List<DefendantJudicialResult> defendantJudicialResults, final List<Offence> defendantsOffences) {
        this.hearingInfo = hearingInfo;
        this.defendantJudicialResults = unmodifiableList(nonNull(defendantJudicialResults) ? defendantJudicialResults : emptyList());
        this.defendantsOffences = unmodifiableList(nonNull(defendantsOffences) ? defendantsOffences : emptyList());
    }

    @Override
    public boolean apply() {
        if (!isEmpty(defendantJudicialResults)) {
            final Optional<DefendantJudicialResult> defendantLevelAcquittalJudicialResult = defendantJudicialResults.stream()
                    .filter(djr -> nonNull(djr.getJudicialResult()))
                    .filter(djr -> djr.getJudicialResult().getCategory().equals(JudicialResultCategory.FINAL))
                    .filter(djr -> nonNull(djr.getJudicialResult().getJudicialResultTypeId()))
                    .filter(djr -> DISCH_RESULT_DEFINITION_ID.equals(djr.getJudicialResult().getJudicialResultTypeId()))
                    .findFirst();

            if (defendantLevelAcquittalJudicialResult.isPresent()) {
                return true;
            }
        }

        if (!isEmpty(defendantsOffences)) {
            final Optional<JudicialResult> offenceLevelAcquittalJudicialResult = defendantsOffences.stream()
                    .filter(offence -> nonNull(offence.getJudicialResults()))
                    .flatMap(offence -> offence.getJudicialResults().stream())
                    .filter(jr -> jr.getCategory().equals(JudicialResultCategory.FINAL))
                    .filter(jr -> nonNull(jr.getJudicialResultTypeId()))
                    .filter(jr -> DISCH_RESULT_DEFINITION_ID.equals(jr.getJudicialResultTypeId()))
                    .findFirst();
            return offenceLevelAcquittalJudicialResult.isPresent();
        }

        return false;
    }

    @Override
    public RetentionPolicy getPolicy() {
        return new RetentionPolicy(ACQUITTAL, ACQUITTAL_SENTENCE, hearingInfo);
    }
}
