package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Offence;
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
            if (existingOffence.getId().equals(commandOffence.getId()) && isOffenceForDefendantChanged(existingOffence, commandOffence)) {
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
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislationWelsh()) ? commandOffenceForDefendant.getOffenceLegislationWelsh() : previousOffenceForDefendant.getOffenceLegislationWelsh(), previousOffenceForDefendant.getOffenceLegislationWelsh()).isEquals();
    }


    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantChanged(final List<Offence> offences, final List<Offence> existingOffences, final UUID prosecutionCaseId, final UUID defendantId) {
        final List<Offence> offencesAddedList = DefendantHelper.getAddedOffences(offences, existingOffences);
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        builder.withModifiedDate(LocalDate.now().toString());
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

        return defendantOffencesChanged ? Optional.of(builder.build()) : Optional.empty();
    }

}
