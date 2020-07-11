package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.moj.cpp.progression.helper.DefendantUpdateDifferenceCalculator;

public class DefendantUpdateDifferenceService {


    public DefendantUpdate calculateDefendantUpdate(final Defendant originalDefendantPreviousVersion,
                                                    final DefendantUpdate originalDefendantNextVersion,
                                                    final Defendant matchedDefendantPreviousVersion) {
        return new DefendantUpdateDifferenceCalculator(
                convertToDefendantUpdate(originalDefendantPreviousVersion),
                originalDefendantNextVersion,
                convertToDefendantUpdate(matchedDefendantPreviousVersion)).calculateDefendantUpdate();
    }

    private DefendantUpdate convertToDefendantUpdate(final Defendant originDefendant) {
        return DefendantUpdate.defendantUpdate()
                .withOffences(originDefendant.getOffences())
                .withPersonDefendant(originDefendant.getPersonDefendant())
                .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(originDefendant.getAssociatedPersons())
                .withId(originDefendant.getId())
                .withMasterDefendantId(originDefendant.getMasterDefendantId())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(originDefendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(originDefendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(originDefendant.getWitnessStatementWelsh())
                .withDefenceOrganisation(originDefendant.getDefenceOrganisation())
                .withPncId(originDefendant.getPncId())
                .withJudicialResults(originDefendant.getDefendantCaseJudicialResults())
                .withAliases(originDefendant.getAliases())
                .withIsYouth(originDefendant.getIsYouth())
                .withCroNumber(originDefendant.getCroNumber())
                .withAssociatedDefenceOrganisation(originDefendant.getAssociatedDefenceOrganisation())
                .withProceedingsConcluded(originDefendant.getProceedingsConcluded())
                .build();

    }
}
