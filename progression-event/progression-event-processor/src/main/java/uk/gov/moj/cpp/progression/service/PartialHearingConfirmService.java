package uk.gov.moj.cpp.progression.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.ListNextHearings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2175"})
public class PartialHearingConfirmService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartialHearingConfirmService.class.getName());

    private static final Integer ESTIMATED_MINUTES = 30;

    public List<ProsecutionCase> getDifferences(final ConfirmedHearing confirmedHearing, final Hearing hearing) {

        if (isNotEmpty(confirmedHearing.getProsecutionCases())) {

            final List<UUID> confirmedOffences = confirmedHearing.getProsecutionCases().stream()
                    .map(ConfirmedProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .map(ConfirmedDefendant::getOffences)
                    .flatMap(Collection::stream)
                    .map(ConfirmedOffence::getId)
                    .collect(toList());

            final Hearing cloneHearing = Hearing.hearing()
                    .withValuesFrom(hearing)
                    .withProsecutionCases(hearing.getProsecutionCases().stream()
                            .map(pc -> ProsecutionCase.prosecutionCase().withValuesFrom(pc)
                                    .withDefendants(pc.getDefendants().stream().map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                            .withOffences(defendant.getOffences().stream().map(offence -> Offence.offence().withValuesFrom(offence).build()).collect(toList()))
                                            .build()).collect(toList()))
                                    .build())
                            .collect(toList()))
                    .build();

            final List<ProsecutionCase> deltaProsecutionCases = removeConfirmedCaseDefendantsOffences(cloneHearing, confirmedOffences);

            LOGGER.info("Delta ProsecutionCases is calculated for Partial Hearing. Delta is : {}", CollectionUtils.isEmpty(deltaProsecutionCases) ? "empty" : deltaProsecutionCases);

            return deltaProsecutionCases;
        }

        return emptyList();

    }


    public ListCourtHearing transformToListCourtHearing(final List<ProsecutionCase> deltaProsecutionCases, final Hearing hearing, final Hearing hearingInProgression) {

        return  ListCourtHearing.listCourtHearing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(randomUUID())
                        .withCourtApplications(hearing.getCourtApplications())
                        .withCourtCentre(hearingInProgression.getCourtCentre())
                        .withEstimatedMinutes(ESTIMATED_MINUTES)
                        .withEstimatedDuration(hearing.getEstimatedDuration())
                        .withProsecutionCases(deltaProsecutionCases)
                        .withType(hearing.getType())
                        .withJurisdictionType(hearing.getJurisdictionType())
                        .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                        .withEarliestStartDateTime(hearingInProgression.getHearingDays().get(0).getSittingDay())
                        .build()))
                .build();

    }

    public ListNextHearings transformToListNextCourtHearing(final List<ProsecutionCase> deltaProsecutionCases, final Hearing hearing, final Hearing hearingInProgression, final SeedingHearing seedingHearing) {
        return ListNextHearings.listNextHearings()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(randomUUID())
                        .withCourtApplications(hearing.getCourtApplications())
                        .withCourtCentre(hearingInProgression.getCourtCentre())
                        .withEstimatedMinutes(ESTIMATED_MINUTES)
                        .withEstimatedDuration(hearingInProgression.getEstimatedDuration())
                        .withProsecutionCases(deltaProsecutionCases)
                        .withType(hearing.getType())
                        .withJurisdictionType(hearing.getJurisdictionType())
                        .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                        .withEarliestStartDateTime(CollectionUtils.isNotEmpty(hearingInProgression.getHearingDays()) ?
                                hearingInProgression.getHearingDays().get(0).getSittingDay() : null)
                        .build()))
                .withHearingId(seedingHearing.getSeedingHearingId())
                .withSeedingHearing(seedingHearing)
                .build();
    }

    public UpdateHearingForPartialAllocation transformToUpdateHearingForPartialAllocation(final UUID hearingId, final List<ProsecutionCase> deltaProsecutionCases) {
        final List<ProsecutionCasesToRemove> prosecutionCasesToRemove = new ArrayList<>();
        deltaProsecutionCases.forEach(dpc -> prosecutionCasesToRemove.add(
                ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(dpc.getId())
                        .withDefendantsToRemove(transformToDefendantsToRemove(dpc.getDefendants()))
                        .build()
        ));
        return UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingId)
                .withProsecutionCasesToRemove(prosecutionCasesToRemove)
                .build();
    }

    public UpdateHearingForPartialAllocation transformConfirmProsecutionCasesToUpdateHearingForPartialAllocation(final UUID hearingId, final List<ConfirmedProsecutionCase> confirmedProsecutionCases) {
        final List<ProsecutionCasesToRemove> prosecutionCases = new ArrayList<>();
        confirmedProsecutionCases.forEach(prosecutionCase -> prosecutionCases.add(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                .withCaseId(prosecutionCase.getId())
                .withDefendantsToRemove(createDefendantsToRemove(prosecutionCase.getDefendants()))
                .build()));

        return UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingId)
                .withProsecutionCasesToRemove(prosecutionCases)
                .build();
    }

    /**
     * remove all prosecutionCasesDefendantsOffences which is not seeded by seedingHearing
     *
     * @param confirmedHearing
     * @param hearing
     * @param seedingHearing
     */
    public List<ProsecutionCase> getDeltaSeededProsecutionCases(final ConfirmedHearing confirmedHearing, final Hearing hearing, final SeedingHearing seedingHearing) {
        final List<ProsecutionCase> deltaSeededProsecutionCases = getDifferences(confirmedHearing, hearing);
        final List<UUID> deltaSeededOffenceIds = deltaSeededProsecutionCases.stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getSeedingHearing()) && offence.getSeedingHearing().getSeedingHearingId().equals(seedingHearing.getSeedingHearingId()))
                .map(Offence::getId)
                .collect(Collectors.toList());

        deltaSeededProsecutionCases.forEach(prosecutionCase -> prosecutionCase.getDefendants().forEach(defendant -> defendant.getOffences().removeIf(offence -> !deltaSeededOffenceIds.contains(offence.getId()))));
        deltaSeededProsecutionCases.forEach(prosecutionCase -> prosecutionCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        deltaSeededProsecutionCases.removeIf(prosecutionCase -> prosecutionCase.getDefendants().isEmpty());

        return deltaSeededProsecutionCases;
    }


    /**
     * The method generates prosecutionCases based on seedingHearing from deltaProsecutionCases.
     * Each related hearing's prosecutionCases can be populated by this method.
     *
     * @param confirmedHearing
     * @param hearing
     * @param seedingHearing
     * @return relatedSeedingHearingProsecutionCasesMap
     */
    public Map<SeedingHearing, List<ProsecutionCase>> getRelatedSeedingHearingsProsecutionCasesMap(final ConfirmedHearing confirmedHearing, final Hearing hearing, final SeedingHearing seedingHearing) {
        final List<ProsecutionCase> deltaProsecutionCases = getDifferences(confirmedHearing, hearing);
        final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingProsecutionCasesMap = new HashMap<>();
        if (nonNull(seedingHearing)) {
            final Set<SeedingHearing> relatedSeedingHearings = deltaProsecutionCases.stream()
                    .flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> nonNull(offence.getSeedingHearing()) && !offence.getSeedingHearing().getSeedingHearingId().equals(seedingHearing.getSeedingHearingId()))
                    .map(Offence::getSeedingHearing)
                    .collect(Collectors.toSet());
            for (final SeedingHearing relatedSeedingHearing : relatedSeedingHearings) {
                relatedSeedingHearingProsecutionCasesMap.put(relatedSeedingHearing, getRelatedDeltaProsecutionCases(deltaProsecutionCases, relatedSeedingHearing));
            }
        }
        return relatedSeedingHearingProsecutionCasesMap;
    }

    private List<ProsecutionCase> getRelatedDeltaProsecutionCases(final List<ProsecutionCase> deltaProsecutionCases, final SeedingHearing relatedSeedingHearing) {

        final List<UUID> deltaRelatedOffenceIds = deltaProsecutionCases.stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getSeedingHearing()) && offence.getSeedingHearing().getSeedingHearingId().equals(relatedSeedingHearing.getSeedingHearingId()))
                .map(Offence::getId)
                .collect(Collectors.toList());

        final List<ProsecutionCase> deltaRelatedProsecutionCases = deltaProsecutionCases.stream()
                .filter(prosecutionCase -> prosecutionCase.getDefendants().stream()
                        .flatMap(defendant -> defendant.getOffences().stream())
                        .anyMatch(offence -> deltaRelatedOffenceIds.contains(offence.getId())))
                .map(pc -> ProsecutionCase.prosecutionCase().withValuesFrom(pc)
                        .withDefendants(pc.getDefendants().stream().map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream().map(offence -> Offence.offence().withValuesFrom(offence).build()).collect(toList()))
                                .build()).collect(toList()))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        deltaRelatedProsecutionCases.forEach(prosecutionCase -> prosecutionCase.getDefendants()
                .forEach(defendant -> defendant.getOffences().removeIf(offence -> !deltaRelatedOffenceIds.contains(offence.getId()))));

        deltaRelatedProsecutionCases.forEach(prosecutionCase -> prosecutionCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        deltaRelatedProsecutionCases.removeIf(prosecutionCase -> prosecutionCase.getDefendants().isEmpty());

        return deltaRelatedProsecutionCases;
    }

    private List<DefendantsToRemove> transformToDefendantsToRemove(final List<Defendant> defendants) {
        final List<DefendantsToRemove> defendantsToRemoves = new ArrayList<>();
        defendants.forEach(d -> defendantsToRemoves.add(DefendantsToRemove.defendantsToRemove()
                .withDefendantId(d.getId())
                .withOffencesToRemove(transformToOffencesToRemove(d.getOffences()))
                .build()));
        return defendantsToRemoves;
    }

    private List<OffencesToRemove> transformToOffencesToRemove(final List<Offence> offences) {
        final List<OffencesToRemove> offencesToRemoves = new ArrayList<>();
        offences.forEach(o -> offencesToRemoves.add(OffencesToRemove.offencesToRemove()
                .withOffenceId(o.getId())
                .build()));
        return offencesToRemoves;
    }

    private List<ProsecutionCase> removeConfirmedCaseDefendantsOffences(final Hearing hearing, final List<UUID> confirmedOffences) {
        removeConfirmedCases(confirmedOffences, hearing.getProsecutionCases());

        for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
            removeConfirmedDefendants(confirmedOffences, prosecutionCase);
            for (final Defendant defendant : prosecutionCase.getDefendants()) {
                removeConfirmedOffences(confirmedOffences, defendant);
            }
        }
        return hearing.getProsecutionCases();
    }

    private void removeConfirmedOffences(final List<UUID> confirmedOffences, final Defendant defendant) {
        defendant.getOffences()
                .removeIf(o -> confirmedOffences.contains(o.getId()));
    }

    private void removeConfirmedDefendants(final List<UUID> confirmedOffences, final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().removeIf(d -> CollectionUtils.isEmpty(
                d.getOffences().stream()
                        .map(Offence::getId)
                        .filter(o -> !confirmedOffences.contains(o))
                        .collect(toList())
        ));
    }

    private void removeConfirmedCases(final List<UUID> confirmedOffences, final List<ProsecutionCase> differenceProsecutionCases) {
        differenceProsecutionCases.removeIf(p ->
                p.getDefendants().stream()
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getId)
                        .filter(o -> !confirmedOffences.contains(o))
                        .collect(toList()).isEmpty());


    }

    private List<DefendantsToRemove> createDefendantsToRemove(final List<ConfirmedDefendant> confirmedDefendants) {
        final List<DefendantsToRemove> defendants = new ArrayList<>();
        confirmedDefendants.forEach(defendant -> defendants.add(DefendantsToRemove.defendantsToRemove()
                .withDefendantId(defendant.getId())
                .withOffencesToRemove(createOffencesToRemove(defendant.getOffences()))
                .build()));
        return defendants;
    }

    private List<OffencesToRemove> createOffencesToRemove(final List<ConfirmedOffence> confirmedOffences) {
        final List<OffencesToRemove> offences = new ArrayList<>();
        confirmedOffences.forEach(offence -> offences.add(OffencesToRemove.offencesToRemove()
                .withOffenceId(offence.getId())
                .build()));
        return offences;
    }

}
