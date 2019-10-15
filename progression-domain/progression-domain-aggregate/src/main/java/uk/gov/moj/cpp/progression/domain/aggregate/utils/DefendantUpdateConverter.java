package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefendantUpdateConverter {

    private DefendantUpdateConverter(){}

    public static void updatedDefendantWithCTL(final DefendantUpdate updatedDefendant , final Map<UUID, Defendant> ctlForDefendant) {

        final PersonDefendant personDefendantToUpdate = new PersonDefendant(
                updatedDefendant.getPersonDefendant().getArrestSummonsNumber(),
                updatedDefendant.getPersonDefendant().getBailConditions(),
  //              updatedDefendant.getPersonDefendant().getBailReasons(),
                updatedDefendant.getPersonDefendant().getBailStatus(),
                ctlForDefendant.get(updatedDefendant.getId()).getPersonDefendant().getCustodyTimeLimit(),
                updatedDefendant.getPersonDefendant().getDriverLicenceCode(),
                updatedDefendant.getPersonDefendant().getDriverLicenseIssue(),
                updatedDefendant.getPersonDefendant().getDriverNumber(),
                updatedDefendant.getPersonDefendant().getEmployerOrganisation(),
                updatedDefendant.getPersonDefendant().getEmployerPayrollReference(),
                updatedDefendant.getPersonDefendant().getPerceivedBirthYear(),
                updatedDefendant.getPersonDefendant().getPersonDetails(),
                updatedDefendant.getPersonDefendant().getVehicleOperatorLicenceNumber()
        );

       DefendantUpdate.defendantUpdate()
                .withId(updatedDefendant.getId())
                .withLegalEntityDefendant(updatedDefendant.getLegalEntityDefendant())
                .withAliases(updatedDefendant.getAliases())
                .withAssociatedPersons(updatedDefendant.getAssociatedPersons())
                .withCroNumber(updatedDefendant.getCroNumber())
                .withDefenceOrganisation(updatedDefendant.getDefenceOrganisation())
                .withJudicialResults(updatedDefendant.getJudicialResults())
                .withMitigation(updatedDefendant.getMitigation())
                .withMitigationWelsh(updatedDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(updatedDefendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(updatedDefendant.getProsecutionAuthorityReference())
                .withPncId(updatedDefendant.getPncId())
                .withWitnessStatement(updatedDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(updatedDefendant.getWitnessStatementWelsh())
                .withOffences(updatedDefendant.getOffences())
                .withPersonDefendant(personDefendantToUpdate)
                .build();
    }

    public static void updateOffenceWithCTL(final DefendantUpdate updatedDefendant , final Offence offence , final Offence storedOffence ){
            final  Offence offenceToUpdate = new Offence(
                            offence.getAllocationDecision(),
                            offence.getAquittalDate(),
                            offence.getArrestDate(),
                            offence.getChargeDate(),
                            offence.getConvictionDate(),
                            offence.getCount(),
                            storedOffence.getCustodyTimeLimit(),
                            offence.getDateOfInformation(),
                            offence.getEndDate(),
                            offence.getId(),
                            offence.getIndicatedPlea(),
                           // offence.getIsDisposed(),
                            offence.getJudicialResults(),
                            //offence.getLaaApplnReference(),
                            offence.getModeOfTrial(),
                            offence.getNotifiedPlea(),
                            offence.getOffenceCode(),
                            offence.getOffenceDefinitionId(),
                            offence.getOffenceFacts(),
                            offence.getOffenceLegislation(),
                            offence.getOffenceLegislationWelsh(),
                            offence.getOffenceTitle(),
                            offence.getOffenceTitleWelsh(),
                            offence.getOrderIndex(),
                            offence.getPlea(),
                            offence.getStartDate(),
                            offence.getVerdict(),
                            offence.getVictims(),
                            offence.getWording(),
                            offence.getWordingWelsh()
                    );

                    final List<Offence> offences = new ArrayList<>();
                    offences.add(offenceToUpdate);
                    DefendantUpdate.defendantUpdate()
                            .withId(updatedDefendant.getId())
                            .withLegalEntityDefendant(updatedDefendant.getLegalEntityDefendant())
                            .withAliases(updatedDefendant.getAliases())
                            .withAssociatedPersons(updatedDefendant.getAssociatedPersons())
                            .withCroNumber(updatedDefendant.getCroNumber())
                            .withDefenceOrganisation(updatedDefendant.getDefenceOrganisation())
                            .withJudicialResults(updatedDefendant.getJudicialResults())
                            .withMitigation(updatedDefendant.getMitigation())
                            .withMitigationWelsh(updatedDefendant.getMitigationWelsh())
                            .withNumberOfPreviousConvictionsCited(updatedDefendant.getNumberOfPreviousConvictionsCited())
                            .withProsecutionAuthorityReference(updatedDefendant.getProsecutionAuthorityReference())
                            .withPncId(updatedDefendant.getPncId())
                            .withWitnessStatement(updatedDefendant.getWitnessStatement())
                            .withWitnessStatementWelsh(updatedDefendant.getWitnessStatementWelsh())
                            .withOffences(offences)
                            .withPersonDefendant(updatedDefendant.getPersonDefendant())
                            .build();


                }
}
