package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.UpdatedOffences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity", "squid:S2234"})
public class DefendantHelper {
    DefendantHelper() {
    }


    public static boolean isOffencesUpdated(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        if (!commandOffences.isEmpty() && !existingOffences.isEmpty()) {
            return isAddedOffences(commandOffences, existingOffences) || isDeletedOffences(commandOffences, existingOffences) || isUpdatedOffences(commandOffences, existingOffences);
        }
        return Boolean.TRUE;
    }

    public static boolean isAllDefendantProceedingConcluded(ProsecutionCase prosecutionCase, List<Defendant> updatedDefendants) {
        return prosecutionCase.getDefendants().stream().map(defendant -> {
            final List<Offence> udpatedOffences = new ArrayList<>();
            final boolean proceedingConcluded = defendant.getOffences().stream()
                    .map(offence -> getUpdatedOffence(udpatedOffences, offence, offence.getJudicialResults() != null ?
                            offence.getJudicialResults().stream()
                                    .anyMatch(judicialResult -> judicialResult.getCategory().equals(Category.FINAL)) : false))
                    .map(offence ->
                            offence.getProceedingsConcluded())
                    .collect(toList()).stream().allMatch(finalCategory -> finalCategory.equals(Boolean.TRUE));

            final Defendant updatedDefendant = getDefendant(defendant, udpatedOffences, proceedingConcluded);
            updatedDefendants.add(updatedDefendant);

            return proceedingConcluded;
        }).collect(toList()).stream().allMatch(proceedingConcluded -> proceedingConcluded.equals(Boolean.TRUE));
    }

    private static Defendant getDefendant(Defendant defendant, final List<Offence> udpatedOffences, boolean proceedingConcluded) {
        return Defendant.defendant()
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withOffences(udpatedOffences)
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withAliases(defendant.getAliases())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withJudicialResults(defendant.getJudicialResults())
                .withPncId(defendant.getPncId())
                .withCroNumber(defendant.getCroNumber())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withProceedingsConcluded(proceedingConcluded)
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .build();
    }

    private static Offence getUpdatedOffence(final List<Offence> udpatedOffences, Offence offence, boolean proceedingConcluded) {
        final Offence updatedOffence = Offence.offence()
                .withAllocationDecision(offence.getAllocationDecision())
                .withAquittalDate(offence.getAquittalDate())
                .withArrestDate(offence.getArrestDate())
                .withChargeDate(offence.getChargeDate())
                .withConvictionDate(offence.getConvictionDate())
                .withCount(offence.getCount())
                .withCustodyTimeLimit(offence.getCustodyTimeLimit())
                .withDateOfInformation(offence.getDateOfInformation())
                .withEndDate(offence.getEndDate())
                .withId(offence.getId())
                .withIndicatedPlea(offence.getIndicatedPlea())
                .withIsDiscontinued(offence.getIsDiscontinued())
                .withIntroducedAfterInitialProceedings(offence.getIntroducedAfterInitialProceedings())
                .withJudicialResults(offence.getJudicialResults())
                .withLaaApplnReference(offence.getLaaApplnReference())
                .withModeOfTrial(offence.getModeOfTrial())
                .withNotifiedPlea(offence.getNotifiedPlea())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                .withOffenceFacts(offence.getOffenceFacts())
                .withOffenceLegislation(offence.getOffenceLegislation())
                .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOrderIndex(offence.getOrderIndex())
                .withPlea(offence.getPlea())
                .withProceedingsConcluded(proceedingConcluded)
                .withStartDate(offence.getStartDate())
                .withVerdict(offence.getVerdict())
                .withVictims(offence.getVictims())
                .withWording(offence.getWording())
                .withWordingWelsh(offence.getWordingWelsh())
                .build();
        udpatedOffences.add(updatedOffence);
        return updatedOffence;
    }


    private static boolean isDeletedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getAddedOffences(existingOffences, commandOffences).isEmpty();
    }

    private static boolean isAddedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getAddedOffences(commandOffences, existingOffences).isEmpty();
    }

    private static List<Offence> getAddedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return commandOffences.stream().filter(commandOffence -> !existingOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(commandOffence.getId())).collect(Collectors.toList());
    }

    private static boolean isUpdatedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getUpdatedOffences(commandOffences, existingOffences).isEmpty();
    }

    private static List<Offence> getUpdatedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        final List<Offence> updatedOffences = new ArrayList<>();
        final List<Offence> commonExistingOffences = existingOffences.stream().filter(existingOffence -> commandOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(existingOffence.getId())).collect(Collectors.toList());


        final List<Offence> commonCommandOffences = commandOffences.stream().filter(commandOffence -> existingOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(commandOffence.getId())).collect(Collectors.toList());

        commonExistingOffences.forEach(existingOffence -> commonCommandOffences.forEach(commandOffence -> {
            if (existingOffence.getId().equals(commandOffence.getId()) && isOffenceForDefendantChanged(commandOffence, existingOffence)) {
                updatedOffences.add(commandOffence);
            }
        }));
        return updatedOffences;
    }

    private static boolean isOffenceForDefendantChanged(final Offence commandOffenceForDefendant, final Offence previousOffenceForDefendant) {
        return !new EqualsBuilder().append(commandOffenceForDefendant.getOffenceCode(), previousOffenceForDefendant.getOffenceCode())
                .append(commandOffenceForDefendant.getWording(), previousOffenceForDefendant.getWording())
                .append(commandOffenceForDefendant.getStartDate(), previousOffenceForDefendant.getStartDate())
                .append(commandOffenceForDefendant.getOffenceTitle(), previousOffenceForDefendant.getOffenceTitle())
                .append(commandOffenceForDefendant.getCount(), previousOffenceForDefendant.getCount())
                .append(nonNull(commandOffenceForDefendant.getOffenceTitleWelsh()) ? commandOffenceForDefendant.getOffenceTitleWelsh() : previousOffenceForDefendant.getOffenceTitleWelsh(), previousOffenceForDefendant.getOffenceTitleWelsh())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislation()) ? commandOffenceForDefendant.getOffenceLegislation() : previousOffenceForDefendant.getOffenceLegislation(), previousOffenceForDefendant.getOffenceLegislation())
                .append(nonNull(commandOffenceForDefendant.getLaaApplnReference()) ? commandOffenceForDefendant.getLaaApplnReference() : previousOffenceForDefendant.getLaaApplnReference(), previousOffenceForDefendant.getLaaApplnReference())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislationWelsh()) ? commandOffenceForDefendant.getOffenceLegislationWelsh() : previousOffenceForDefendant.getOffenceLegislationWelsh(), previousOffenceForDefendant.getOffenceLegislationWelsh()).isEquals();
    }


    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantChanged(final List<Offence> offences, final List<Offence> existingOffences, final UUID prosecutionCaseId, final UUID defendantId) {
        final List<Offence> offencesAddedList = DefendantHelper.getAddedOffences(offences, existingOffences);
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        builder.withModifiedDate(LocalDate.now());
        boolean defendantOffencesChanged = false;
        if (!offencesAddedList.isEmpty()) {
            final List<AddedOffences> addedOffences = Arrays.asList(AddedOffences.addedOffences().withProsecutionCaseId(prosecutionCaseId).withDefendantId(defendantId).withOffences(offencesAddedList).build());
            builder.withAddedOffences(addedOffences);
            defendantOffencesChanged = true;
        }

        final List<Offence> offencesModifiedList = DefendantHelper.getUpdatedOffences(offences, existingOffences);
        if (!offencesModifiedList.isEmpty()) {
            final List<UpdatedOffences> updatedOffences = Arrays.asList(UpdatedOffences.updatedOffences().withDefendantId(defendantId).withProsecutionCaseId(prosecutionCaseId).withOffences(offencesModifiedList).build());
            builder.withUpdatedOffences(updatedOffences);
            defendantOffencesChanged = true;
        }

        final List<Offence> offencesDeletedList = DefendantHelper.getAddedOffences(existingOffences, offences);
        if (!offencesDeletedList.isEmpty()) {
            final List<DeletedOffences> deletedOffences = Arrays.asList(DeletedOffences.deletedOffences().withDefendantId(defendantId).withProsecutionCaseId(prosecutionCaseId).withOffences(offencesDeletedList.stream().map(Offence::getId).collect(Collectors.toList())).build());

            builder.withDeletedOffences(deletedOffences);
            defendantOffencesChanged = true;
        }
        return defendantOffencesChanged ? of(builder.build()) : Optional.empty();
    }

    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantUpdated(final List<Offence> offences,
                                                                                       final List<Offence> existingOffences,
                                                                                       final UUID prosecutionCaseId,
                                                                                       final UUID defendantId) {
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        final List<Offence> updatedOffences = DefendantHelper.getUpdatedOffences(offences, existingOffences);
        if (!updatedOffences.isEmpty()) {
            return buildOffencesForDefendantChanged(prosecutionCaseId, defendantId, builder, updatedOffences);
        }
        return Optional.empty();
    }

    private static Optional<OffencesForDefendantChanged> buildOffencesForDefendantChanged(final UUID prosecutionCaseId,
                                                                                          final UUID defendantId,
                                                                                          final OffencesForDefendantChanged.Builder builder,
                                                                                          final List<Offence> offencesModifiedList) {
        final List<UpdatedOffences> updatedOffences = singletonList(createOffence(prosecutionCaseId, defendantId, offencesModifiedList));
        builder.withUpdatedOffences(updatedOffences);
        builder.withModifiedDate(LocalDate.now());
        return of(builder.build());
    }

    private static UpdatedOffences createOffence(final UUID prosecutionCaseId, final UUID defendantId, final List<Offence> offencesModifiedList) {
        return UpdatedOffences.updatedOffences()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offencesModifiedList)
                .build();
    }

}
