package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.moj.cpp.progression.domain.event.defendant.BaseDefendantOffence;
import uk.gov.moj.cpp.progression.domain.event.defendant.BaseDefendantOffences;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChanged;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class DefendantOffenceHelper {



    private DefendantOffenceHelper() {

    }

    private static boolean isOffenceForDefendantChanged(
                    final OffenceForDefendant commandOffenceForDefendant,
                    final OffenceForDefendant previousOffenceForDefendant) {
        return !new EqualsBuilder()
                        .append(commandOffenceForDefendant.getOffenceCode(),
                                        previousOffenceForDefendant.getOffenceCode())
                        .append(commandOffenceForDefendant.getWording(),
                                        previousOffenceForDefendant.getWording())
                        .append(commandOffenceForDefendant.getStartDate(),
                                        previousOffenceForDefendant.getStartDate())
                        .append(commandOffenceForDefendant.getEndDate(),
                                        previousOffenceForDefendant.getEndDate())
                        .append(commandOffenceForDefendant.getCount(),
                                        previousOffenceForDefendant.getCount())
                        .append(commandOffenceForDefendant.getConvictionDate(),
                                        previousOffenceForDefendant.getConvictionDate())
                        .isEquals();
    }

    public static Optional<DefendantOffencesChanged> getDefendantOffencesChanged(final UUID caseId,
                                                                                 final UUID defendantId, final List<OffenceForDefendant> commandOffences,
                                                                                 final List<OffenceForDefendant> existingOffences) {



        final List<BaseDefendantOffence> deletedOffences =
                        getDeletedOffences(commandOffences, existingOffences);


        final List<BaseDefendantOffence> addedOffences =
                        getAddedOffences(commandOffences, existingOffences);

        final List<BaseDefendantOffence> updatedOffences =
                        getUpdatedOffences(commandOffences, existingOffences);

        boolean defendantOffencesChanged = false;
        final DefendantOffencesChanged defendantOffencesChangedEvent =
                        new DefendantOffencesChanged();
        if (!deletedOffences.isEmpty()) {
            defendantOffencesChanged = true;
            defendantOffencesChangedEvent.setDeletedOffences(
                    Arrays.asList(new BaseDefendantOffences(defendantId,caseId, deletedOffences)));
        }
        if (!addedOffences.isEmpty()) {
            defendantOffencesChanged = true;
            defendantOffencesChangedEvent.setAddedOffences(
                    Arrays.asList(new BaseDefendantOffences(defendantId, caseId, addedOffences)));
        }
        if (!updatedOffences.isEmpty()) {
            defendantOffencesChanged = true;
            defendantOffencesChangedEvent.setUpdatedOffences(
                    Arrays.asList(new BaseDefendantOffences(defendantId, caseId, updatedOffences)));
        }
        return defendantOffencesChanged ? Optional.of(defendantOffencesChangedEvent)
                        : Optional.empty();

    }

    private static final List<BaseDefendantOffence> getUpdatedOffences(
            final List<OffenceForDefendant> commandOffences,
            final List<OffenceForDefendant> existingOffences) {
        final List<BaseDefendantOffence> updatedOffences = new ArrayList<>();
        final List<OffenceForDefendant> commonExistingOffences = existingOffences.stream()
                        .filter(existingOffence -> commandOffences.stream()
                                        .map(OffenceForDefendant::getId)
                                        .collect(Collectors.toList())
                                        .contains(existingOffence.getId()))
                        .collect(Collectors.toList());


        final List<OffenceForDefendant> commonCommandOffences = commandOffences.stream()
                        .filter(commandOffence -> existingOffences.stream()
                                        .map(OffenceForDefendant::getId)
                                        .collect(Collectors.toList())
                                        .contains(commandOffence.getId()))
                        .collect(Collectors.toList());

        commonExistingOffences.forEach(
                        existingOffence -> commonCommandOffences.forEach(commandOffence -> {
                            if (existingOffence.getId().equals(commandOffence.getId())
                                            && isOffenceForDefendantChanged(existingOffence,
                                                            commandOffence)) {
                                updatedOffences.add(new BaseDefendantOffence(commandOffence.getId(),
                                                commandOffence.getOffenceCode(),
                                                commandOffence.getWording(),
                                                commandOffence.getStartDate(),
                                                commandOffence.getEndDate(),
                                                commandOffence.getCount(),
                                                commandOffence.getConvictionDate()));
                            }
                        }));
        return updatedOffences;
    }

    private static List<BaseDefendantOffence> getAddedOffences(
                    final List<OffenceForDefendant> commandOffences,
                    final List<OffenceForDefendant> existingOffences) {
        return commandOffences.stream()
                        .filter(commandOffence -> !existingOffences.stream()
                                        .map(OffenceForDefendant::getId)
                                        .collect(Collectors.toList())
                                        .contains(commandOffence.getId()))
                        .map(addedOffence -> new BaseDefendantOffence(addedOffence.getId(),
                                        addedOffence.getOffenceCode(), addedOffence.getWording(),
                                        addedOffence.getStartDate(), addedOffence.getEndDate(),
                                        addedOffence.getCount(), addedOffence.getConvictionDate()))
                        .collect(Collectors.toList());
    }

    private static List<BaseDefendantOffence> getDeletedOffences(
            final List<OffenceForDefendant> commandOffences,
            final List<OffenceForDefendant> existingOffences) {
        return existingOffences.stream()
                .filter(existingOffence -> !commandOffences.stream()
                        .map(OffenceForDefendant::getId)
                        .collect(Collectors.toList())
                        .contains(existingOffence.getId()))
                .map(deletedOffence -> new BaseDefendantOffence(deletedOffence.getId(),
                        deletedOffence.getOffenceCode(), deletedOffence.getWording(),
                        deletedOffence.getStartDate(), deletedOffence.getEndDate(),
                        deletedOffence.getCount(), deletedOffence.getConvictionDate()))
                .collect(Collectors.toList());
    }
}
