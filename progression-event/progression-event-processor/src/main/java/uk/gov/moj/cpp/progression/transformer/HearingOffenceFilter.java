package uk.gov.moj.cpp.progression.transformer;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter the case level and application level offences carried by a hearing before it is sent to
 * hearing context.
 * <p>
 * Filtration applied to the hearing payload only — the application held in progression viewstore is
 * a separate, untouched copy. A concluded offence belongs under courtApplicationCases; an active
 * offence must never sit there and is moved to the prosecutionCases side. When all offences under
 * courtApplicationCases are concluded (inactive), they are left under courtApplicationCases and
 * prosecutionCases is dropped entirely.
 */
public final class HearingOffenceFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingOffenceFilter.class);

    private HearingOffenceFilter() {
    }

    /**
     * Resolves the defendant that owns an offence within a prosecution case. Used only as a
     * fallback when an active application offence is not already present under the hearing's
     * prosecution cases.
     */
    @FunctionalInterface
    public interface OffenceOwnerResolver {
        Optional<UUID> resolveDefendantIdByOffenceId(UUID prosecutionCaseId, UUID offenceId);
    }

    public static Hearing filterOffences(final Hearing hearing, final OffenceOwnerResolver ownerResolver) {
        if (isNull(hearing) || isEmpty(hearing.getCourtApplications()) || !hasApplicationOffences(hearing.getCourtApplications())) {
            return hearing;
        }

        // Application hearing: every application offence is concluded -> keep the application, drop prosecution cases.
        if (allApplicationOffencesConcluded(hearing.getCourtApplications())) {
            return Hearing.hearing().withValuesFrom(hearing).withProsecutionCases(null).build();
        }

        // Active offences exist under the application -> move them to the prosecution side.
        final Set<UUID> activeApplicationOffenceIds = hearing.getCourtApplications().stream()
                .filter(application -> nonNull(application.getCourtApplicationCases()))
                .flatMap(application -> application.getCourtApplicationCases().stream())
                .filter(applicationCase -> nonNull(applicationCase.getOffences()))
                .flatMap(applicationCase -> applicationCase.getOffences().stream())
                .filter(HearingOffenceFilter::isActive)
                .map(Offence::getId)
                .collect(toSet());

        // Working copy of the prosecution cases keeping only the active offences the application references.
        final List<ProsecutionCase> workingProsecutionCases = copyKeepingReferencedOffences(hearing.getProsecutionCases(), activeApplicationOffenceIds);

        // Offence ids now present under the prosecution side ("kept"/deduped).
        final Set<UUID> movedOffenceIds = collectOffenceIds(workingProsecutionCases);

        // Fallback: any active application offence not yet present under prosecution -> move it across.
        addMissingActiveOffences(hearing.getCourtApplications(), movedOffenceIds, workingProsecutionCases, ownerResolver);

        final List<CourtApplication> filteredApplications = removeMovedOffencesFromApplications(hearing.getCourtApplications(), movedOffenceIds);
        final List<ProsecutionCase> filteredProsecutionCases = dropEmptyDefendantsAndCases(workingProsecutionCases);

        return Hearing.hearing()
                .withValuesFrom(hearing)
                .withCourtApplications(filteredApplications)
                .withProsecutionCases(isEmpty(filteredProsecutionCases) ? null : filteredProsecutionCases)
                .build();
    }

    private static boolean hasApplicationOffences(final List<CourtApplication> applications) {
        return applications.stream()
                .filter(application -> nonNull(application.getCourtApplicationCases()))
                .flatMap(application -> application.getCourtApplicationCases().stream())
                .anyMatch(applicationCase -> isNotEmpty(applicationCase.getOffences()));
    }

    private static boolean allApplicationOffencesConcluded(final List<CourtApplication> applications) {
        return applications.stream()
                .filter(application -> nonNull(application.getCourtApplicationCases()))
                .flatMap(application -> application.getCourtApplicationCases().stream())
                .filter(applicationCase -> isNotEmpty(applicationCase.getOffences()))
                .flatMap(applicationCase -> applicationCase.getOffences().stream())
                .allMatch(HearingOffenceFilter::isConcluded);
    }

    private static List<ProsecutionCase> copyKeepingReferencedOffences(final List<ProsecutionCase> prosecutionCases, final Set<UUID> referencedOffenceIds) {
        if (isEmpty(prosecutionCases)) {
            return new ArrayList<>();
        }
        return prosecutionCases.stream()
                .map(prosecutionCase -> ProsecutionCase.prosecutionCase()
                        .withValuesFrom(prosecutionCase)
                        .withDefendants(copyDefendantsKeepingReferencedOffences(prosecutionCase.getDefendants(), referencedOffenceIds))
                        .build())
                .collect(toList());
    }

    private static List<Defendant> copyDefendantsKeepingReferencedOffences(final List<Defendant> defendants, final Set<UUID> referencedOffenceIds) {
        if (isEmpty(defendants)) {
            return new ArrayList<>();
        }
        return defendants.stream()
                .map(defendant -> {
                    final List<Offence> keptOffences = isNull(defendant.getOffences()) ? new ArrayList<>()
                            : defendant.getOffences().stream()
                            .filter(offence -> referencedOffenceIds.contains(offence.getId()))
                            .collect(toList());
                    return Defendant.defendant().withValuesFrom(defendant).withOffences(keptOffences).build();
                })
                .collect(toList());
    }

    private static void addMissingActiveOffences(final List<CourtApplication> applications,
                                                 final Set<UUID> movedOffenceIds,
                                                 final List<ProsecutionCase> workingProsecutionCases,
                                                 final OffenceOwnerResolver ownerResolver) {
        applications.stream()
                .filter(application -> nonNull(application.getCourtApplicationCases()))
                .flatMap(application -> application.getCourtApplicationCases().stream())
                .filter(applicationCase -> nonNull(applicationCase.getOffences()))
                .forEach(applicationCase -> applicationCase.getOffences().stream()
                        .filter(HearingOffenceFilter::isActive)
                        .filter(offence -> !movedOffenceIds.contains(offence.getId()))//the offences not already in prosecutionCases
                        .forEach(offence -> moveOffenceToProsecution(applicationCase.getProsecutionCaseId(), offence, movedOffenceIds, workingProsecutionCases, ownerResolver)));
    }

    private static void moveOffenceToProsecution(final UUID prosecutionCaseId,
                                                 final Offence offence,
                                                 final Set<UUID> movedOffenceIds,
                                                 final List<ProsecutionCase> workingProsecutionCases,
                                                 final OffenceOwnerResolver ownerResolver) {
        final Optional<UUID> defendantId = isNull(ownerResolver) ? Optional.empty()
                : ownerResolver.resolveDefendantIdByOffenceId(prosecutionCaseId, offence.getId());

        if (defendantId.isEmpty()) {
            LOGGER.warn("Could not resolve owning defendant for active application offence {} in prosecution case {}; leaving it on the application side",
                    offence.getId(), prosecutionCaseId);
            return;
        }

        final Optional<Defendant> targetDefendant = workingProsecutionCases.stream()
                .filter(prosecutionCase -> prosecutionCaseId.equals(prosecutionCase.getId()))
                .filter(prosecutionCase -> nonNull(prosecutionCase.getDefendants()))
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> defendantId.get().equals(defendant.getId()))
                .findFirst();

        if (targetDefendant.isEmpty()) {
            LOGGER.warn("Resolved defendant {} for active application offence {} is not present in the hearing's prosecution case {}; leaving it on the application side",
                    defendantId.get(), offence.getId(), prosecutionCaseId);
            return;
        }

        // The working prosecution cases are fresh copies built with mutable offence lists, so adding here
        // never touches the input graph.
        targetDefendant.get().getOffences().add(offence);
        movedOffenceIds.add(offence.getId());
    }

    private static List<CourtApplication> removeMovedOffencesFromApplications(final List<CourtApplication> applications, final Set<UUID> movedOffenceIds) {
        return applications.stream()
                .map(application -> CourtApplication.courtApplication()
                        .withValuesFrom(application)
                        .withCourtApplicationCases(removeMovedOffencesFromCases(application.getCourtApplicationCases(), movedOffenceIds))
                        .build())
                .collect(toList());
    }

    private static List<CourtApplicationCase> removeMovedOffencesFromCases(final List<CourtApplicationCase> applicationCases, final Set<UUID> movedOffenceIds) {
        if (isEmpty(applicationCases)) {
            return applicationCases;
        }
        return applicationCases.stream()
                .map(applicationCase -> {
                    if (isNull(applicationCase.getOffences())) {
                        return applicationCase;
                    }
                    final List<Offence> remainingOffences = applicationCase.getOffences().stream()
                            .filter(offence -> !movedOffenceIds.contains(offence.getId()))
                            .collect(toList());
                    return CourtApplicationCase.courtApplicationCase()
                            .withValuesFrom(applicationCase)
                            .withOffences(remainingOffences.isEmpty() ? null : remainingOffences)
                            .build();
                })
                .collect(toList());
    }

    private static List<ProsecutionCase> dropEmptyDefendantsAndCases(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .map(prosecutionCase -> ProsecutionCase.prosecutionCase()
                        .withValuesFrom(prosecutionCase)
                        .withDefendants(dropDefendantsWithoutOffences(prosecutionCase.getDefendants()))
                        .build())
                .filter(prosecutionCase -> isNotEmpty(prosecutionCase.getDefendants()))
                .collect(toList());
    }

    private static List<Defendant> dropDefendantsWithoutOffences(final List<Defendant> defendants) {
        if (isEmpty(defendants)) {
            return emptyList();
        }
        return defendants.stream()
                .filter(defendant -> isNotEmpty(defendant.getOffences()))
                .collect(toList());
    }

    private static Set<UUID> collectOffenceIds(final List<ProsecutionCase> prosecutionCases) {
        final Set<UUID> offenceIds = new HashSet<>();
        prosecutionCases.stream()
                .filter(prosecutionCase -> nonNull(prosecutionCase.getDefendants()))
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> nonNull(defendant.getOffences()))
                .flatMap(defendant -> defendant.getOffences().stream())
                .forEach(offence -> offenceIds.add(offence.getId()));
        return offenceIds;
    }

    private static boolean isConcluded(final Offence offence) {
        return isTrue(offence.getProceedingsConcluded());
    }

    private static boolean isActive(final Offence offence) {
        return !isConcluded(offence);
    }

}
