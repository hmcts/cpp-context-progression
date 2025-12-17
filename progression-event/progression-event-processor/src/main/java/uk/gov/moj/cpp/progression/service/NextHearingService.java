package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;


@SuppressWarnings("squid:S1188")
public class NextHearingService {

    @Inject
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Inject
    private HearingResultHelper hearingResultHelper;

    private static boolean doesExistingHearingIdPresent(final NextHearing nextHearing) {
        return nonNull(nextHearing) && nonNull(nextHearing.getExistingHearingId());
    }

    public NextHearingDetails getNextHearingDetails(final Hearing hearing) {
        return getNextHearingDetails(hearing, false, Optional.empty());
    }

    public NextHearingDetails getNextHearingDetails(final Hearing hearing, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {
        final List<ProsecutionCase> prosecutionCases = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());
        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());
        final Set<UUID> hearingIds = getAllocatedHearingIds(hearing);
        final Map<UUID, ProsecutionCase> prosecutionCaseMap = getProsecutionCasesMap(prosecutionCases);

        final Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap = new HashMap<>();
        final Map<UUID, List<CourtApplication>> hearingsMap2 = new HashMap<>();
        final Map<UUID, NextHearing> nextHearings = new HashMap<>();
        hearingIds.forEach(hearingId -> {
            processProsecutionCases(prosecutionCases, hearingsMap, nextHearings, hearingId);
            processApplications(courtApplications, hearingsMap2, nextHearings, hearingId);
        });

        final List<HearingListingNeeds> hearingListingNeedsList = new ArrayList<>();
        hearingsMap.forEach((hearingId, prosecutionCaseIds) -> {
            final List<ProsecutionCase> prosecutionCasesToBeAdded = new ArrayList<>();
            prosecutionCaseIds.forEach((prosecutionCaseId, defendantIds) -> {
                final ProsecutionCase prosecutionCase = prosecutionCaseMap.get(prosecutionCaseId);
                final ProsecutionCase prosecutionCaseToBeAdded = createProsecutionCase(prosecutionCase);
                final List<Defendant> defendantsToBeAdded = new ArrayList<>();
                defendantIds.forEach((defendantId, offenceIds) -> {
                    final Defendant defendant = getDefendant(prosecutionCase, defendantId);
                    final Defendant defendantToBeAdded = createDefendant(defendant);
                    final List<Offence> offencesToBeAdded = new ArrayList<>();
                    offenceIds.forEach(offenceId -> {
                        final Offence offence = getOffence(defendant, offenceId);
                        final Offence offenceToBeAdded = createOffence(offence, hearing.getCourtCentre(), shouldPopulateCommittingCourt, committingCourt);
                        offencesToBeAdded.add(offenceToBeAdded);
                    });
                    defendantToBeAdded.getOffences().addAll(offencesToBeAdded);
                    defendantsToBeAdded.add(defendantToBeAdded);
                });
                prosecutionCaseToBeAdded.getDefendants().addAll(defendantsToBeAdded);
                prosecutionCasesToBeAdded.add(prosecutionCaseToBeAdded);
            });
            addToHearingListingNeeds(nextHearings, hearingListingNeedsList, hearingId, prosecutionCasesToBeAdded, hearingsMap2.get(hearingId));
        });
        if(hearingsMap.isEmpty()) {
            hearingsMap2.forEach((hearingId, applications) -> addToHearingListingNeeds(nextHearings, hearingListingNeedsList, hearingId, applications));
        }
        return new NextHearingDetails(hearingListingNeedsList, nextHearings);
    }

    private void processApplications(List<CourtApplication> courtApplications, Map<UUID, List<CourtApplication>> hearingsMap2, Map<UUID, NextHearing> nextHearings, UUID hearingId) {
        courtApplications.forEach(courtApplication -> {
            final List<JudicialResult> judicialResults = hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication);
            judicialResults.forEach(judicialResult -> {
                final NextHearing nextHearing = judicialResult.getNextHearing();
                if (doesExistingHearingIdPresent(nextHearing) && (nextHearing.getExistingHearingId().equals(hearingId))) {
                    final List<CourtApplication> courtApplicationList = hearingsMap2.getOrDefault(hearingId, new ArrayList<>());
                    final boolean containsObject = courtApplicationList.stream()
                            .anyMatch(courtApplication1 -> courtApplication1.getId().equals(courtApplication.getId()));
                    if (!containsObject) {
                        courtApplicationList.add(courtApplication);
                    }
                    hearingsMap2.put(hearingId, courtApplicationList);
                    nextHearings.put(hearingId, nextHearing);
                }
            });
        });
    }

    private void processProsecutionCases(List<ProsecutionCase> prosecutionCases, Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap, Map<UUID, NextHearing> nextHearings, UUID hearingId) {
        final Map<UUID, Map<UUID, Set<UUID>>> prosecutionCasesMap = new HashMap<>();
        prosecutionCases.forEach(prosecutionCase -> {
            final Map<UUID, Set<UUID>> defendantsMap = new HashMap<>();
            prosecutionCase.getDefendants().forEach(defendant -> {
                final Set<UUID> offenceIds = new HashSet<>();
                defendant.getOffences().forEach(offence -> {
                    if (isNotEmpty(offence.getJudicialResults())) {
                        offence.getJudicialResults().forEach(judicialResult -> {
                            final NextHearing nextHearing = judicialResult.getNextHearing();
                            if (doesExistingHearingIdPresent(nextHearing) && (nextHearing.getExistingHearingId().equals(hearingId))) {
                                offenceIds.add(offence.getId());
                                defendantsMap.put(defendant.getId(), offenceIds);
                                prosecutionCasesMap.put(prosecutionCase.getId(), defendantsMap);
                                hearingsMap.put(hearingId, prosecutionCasesMap);
                                nextHearings.put(hearingId, nextHearing);
                            }
                        });
                    }
                });
            });
        });
    }

    private void addToHearingListingNeeds(final Map<UUID, NextHearing> nextHearings, final List<HearingListingNeeds> hearingListingNeedsList, final UUID hearingId, final List<ProsecutionCase> prosecutionCasesToBeAdded, List<CourtApplication> courtApplications) {
        final NextHearing nextHearing = nextHearings.get(hearingId);
        hearingListingNeedsList.add(HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(nextHearing.getCourtCentre())
                .withProsecutionCases(prosecutionCasesToBeAdded)
                .withCourtApplications(courtApplications)
                .withId(hearingId)
                .withJurisdictionType(nextHearing.getJurisdictionType())
                .withEstimatedMinutes(30)
                .withEstimatedDuration(nextHearing.getEstimatedDuration())
                .withType(nextHearing.getType())
                .build());
    }

    private void addToHearingListingNeeds(final Map<UUID, NextHearing> nextHearings, final List<HearingListingNeeds> hearingListingNeedsList, final UUID hearingId, List<CourtApplication> courtApplications) {
        final NextHearing nextHearing = nextHearings.get(hearingId);
        hearingListingNeedsList.add(HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(nextHearing.getCourtCentre())
                .withProsecutionCases(null)
                .withCourtApplications(courtApplications)
                .withId(hearingId)
                .withJurisdictionType(nextHearing.getJurisdictionType())
                .withEstimatedMinutes(30)
                .withEstimatedDuration(nextHearing.getEstimatedDuration())
                .withType(nextHearing.getType())
                .build());
    }

    public Map<UUID, List<ConfirmedProsecutionCase>> getConfirmedHearings(final Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap) {
        final Map<UUID, List<ConfirmedProsecutionCase>> confirmedProsecutionCaseMap = new HashMap<>();
        hearingsMap.forEach((hearingId, prosecutionCasesMap) -> {
            final List<ConfirmedProsecutionCase> confirmedProsecutionCases = new ArrayList<>();
            prosecutionCasesMap.forEach((prosecutionCaseId, defendantsMap) -> {
                final List<ConfirmedDefendant> confirmedDefendants = new ArrayList<>();
                defendantsMap.forEach((defendantId, offenceIds) -> {
                    final List<ConfirmedOffence> confirmedOffences = new ArrayList<>();
                    offenceIds.forEach(offenceId -> confirmedOffences.add(getConfirmedOffence(offenceId)));
                    confirmedDefendants.add(getConfirmedDefendant(defendantId, confirmedOffences));
                });
                confirmedProsecutionCases.add(getConfirmedProsecutionCase(prosecutionCaseId, confirmedDefendants));
            });
            confirmedProsecutionCaseMap.put(hearingId, confirmedProsecutionCases);
        });
        return confirmedProsecutionCaseMap;
    }

    private ConfirmedProsecutionCase getConfirmedProsecutionCase(final UUID prosecutionCaseId, final List<ConfirmedDefendant> confirmedDefendants) {
        return ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(confirmedDefendants)
                .build();
    }

    private ConfirmedDefendant getConfirmedDefendant(final UUID defendantId, final List<ConfirmedOffence> confirmedOffences) {
        return ConfirmedDefendant.confirmedDefendant()
                .withId(defendantId)
                .withOffences(confirmedOffences)
                .build();
    }

    private ConfirmedOffence getConfirmedOffence(final UUID offenceId) {
        return ConfirmedOffence.confirmedOffence()
                .withId(offenceId)
                .build();
    }

    private ProsecutionCase createProsecutionCase(final ProsecutionCase prosecutionCase) {
        return ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withId(prosecutionCase.getId())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withClassOfCase(prosecutionCase.getClassOfCase())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withCpsOrganisationId(prosecutionCase.getCpsOrganisationId())
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .build();
    }

    private Defendant createDefendant(final Defendant defendant) {
        return Defendant.defendant()
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withOffences(new ArrayList<>())
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withAliases(defendant.getAliases())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withAssociationLockedByRepOrder(defendant.getAssociationLockedByRepOrder())
                .withCroNumber(defendant.getCroNumber())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withIsYouth(defendant.getIsYouth())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withPncId(defendant.getPncId())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .build();
    }

    private Offence createOffence(final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> existingCommittingCourt) {

        final CommittingCourt committingCourt = (nonNull(existingCommittingCourt) && existingCommittingCourt.isPresent()) ? existingCommittingCourt.get() : offenceToCommittingCourtConverter.convert(offence, courtCentre, shouldPopulateCommittingCourt).orElse(null);

        return Offence.offence().withValuesFrom(offence)
                .withCommittingCourt(committingCourt)
                .build();
    }

    private Map<UUID, ProsecutionCase> getProsecutionCasesMap(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .collect(Collectors.toMap(ProsecutionCase::getId, pc -> pc));
    }

    private Set<UUID> getAllocatedHearingIds(final Hearing hearing) {
        final Set<UUID> hearingIds = new HashSet<>();
        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> CollectionUtils.isNotEmpty(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .forEach(judicialResult -> getExistingHearingId(judicialResult).ifPresent(hearingIds::add));

        final List<JudicialResult> judicialResults = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(courtApplication -> hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication).stream())
                .collect(Collectors.toList());

        judicialResults.forEach(judicialResult -> getExistingHearingId(judicialResult).ifPresent(hearingIds::add));

        return hearingIds;
    }

    private Optional<UUID> getExistingHearingId(JudicialResult judicialResult) {
        final NextHearing nextHearing = judicialResult.getNextHearing();
        if (doesExistingHearingIdPresent(nextHearing)) {
            return ofNullable(nextHearing.getExistingHearingId());
        }
        return empty();
    }

    private Offence getOffence(final Defendant defendant, final UUID offenceId) {
        return defendant.getOffences().stream()
                .filter(off -> off.getId().equals(offenceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Offence not found"));
    }

    private Defendant getDefendant(final ProsecutionCase prosecutionCase, final UUID defendantId) {
        return prosecutionCase.getDefendants().stream()
                .filter(def -> def.getId().equals(defendantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Defendant not found"));
    }
}
