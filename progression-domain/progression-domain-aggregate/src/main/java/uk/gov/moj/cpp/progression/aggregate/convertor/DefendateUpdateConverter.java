package uk.gov.moj.cpp.progression.aggregate.convertor;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;

public class DefendateUpdateConverter {
    public static DefendantUpdate convert(final uk.gov.justice.core.courts.Defendant defendant){
        return DefendantUpdate.defendantUpdate()
                .withAliases(defendant.getAliases())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withCroNumber(defendant.getCroNumber())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withId(defendant.getId())
                .withIsYouth(defendant.getIsYouth())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withOffences(defendant.getOffences())
                .withPncId(defendant.getPncId())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .build();
    }

    public static DefendantUpdate convertWithPersonDefendant(final uk.gov.justice.core.courts.Defendant defendant, final PersonDefendant personDefendant){
        return DefendantUpdate.defendantUpdate()
                .withAliases(defendant.getAliases())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withPersonDefendant(personDefendant)
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withCroNumber(defendant.getCroNumber())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withId(defendant.getId())
                .withIsYouth(defendant.getIsYouth())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withOffences(defendant.getOffences())
                .withPncId(defendant.getPncId())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .build();
    }
}
