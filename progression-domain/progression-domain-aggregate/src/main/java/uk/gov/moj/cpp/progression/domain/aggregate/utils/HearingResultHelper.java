package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("ALL")
public class HearingResultHelper {
    private static final String COMMITTED_TO_CC = "CommittedToCC";
    private static final String SENT_TO_CC = "SentToCC";

    private HearingResultHelper() {
    }

    /**
     * Checks the latest results to identify if the hearing contains any new or amended next hearing
     * results.
     *
     * @param hearing
     * @return
     */
    public static boolean doHearingContainNewOrAmendedNextHearingResults(final Hearing hearing) {
        final boolean prosecutionCasesContainNextHearingResults = doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
        final boolean courtApplicationsContainNextHearingResults = doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
        return prosecutionCasesContainNextHearingResults || courtApplicationsContainNextHearingResults;
    }

    private static boolean doProsecutionCasesContainNextHearingResults(final List<ProsecutionCase> prosecutionCases) {
        return isNotEmpty(prosecutionCases) && prosecutionCases.stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult ->
                        nonNull(judicialResult.getNextHearing()) && isNull(judicialResult.getNextHearing().getApplicationTypeCode()) && TRUE.equals(judicialResult.getIsNewAmendment()
                        )
                );
    }

    public static boolean isNextHearingDeleted(final Hearing resultedHearing, final Hearing aggregateHearing){
        if (aggregateHearing == null){
            return false;
        }
        final boolean doNewHearingHasNewHearing = doProsecutionCasesContainNextHearing(resultedHearing.getProsecutionCases()) ||
                doCourtApplicationsContainNextHearing(resultedHearing.getCourtApplications()) ;

        final boolean doOldHearingHasNewHearing = doProsecutionCasesContainNextHearingForPrevious(aggregateHearing.getProsecutionCases()) ||
                doCourtApplicationsContainNextHearing(aggregateHearing.getCourtApplications()) ;


        return doOldHearingHasNewHearing && ! doNewHearingHasNewHearing ;

    }

    private static boolean doProsecutionCasesContainNextHearing(final List<ProsecutionCase> prosecutionCases) {
        return isNotEmpty(prosecutionCases) && prosecutionCases.stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult ->
                        nonNull(judicialResult.getNextHearing())
                                && StringUtils.isEmpty(judicialResult.getNextHearing().getApplicationTypeCode())
                                && TRUE.equals(judicialResult.getIsNewAmendment())
                );
    }

    private static boolean doProsecutionCasesContainNextHearingForPrevious(final List<ProsecutionCase> prosecutionCases) {
        return isNotEmpty(prosecutionCases) && prosecutionCases.stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult ->
                        nonNull(judicialResult.getNextHearing())
                                && StringUtils.isEmpty(judicialResult.getNextHearing().getApplicationTypeCode())
                );
    }

    /**
     * Check if any of the judicial result's group matches the specified result definition groups
     *
     * @param hearing - hearing
     * @return - return true if judicial result's group has CommittedToCC or SENTTOCC
     */
    public static boolean checkResultLinesForCommittingCourt(final Hearing hearing) {
        final AtomicBoolean shouldPopulateCommittingCourt = new AtomicBoolean(false);

        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).forEach(
                prosecutionCase -> prosecutionCase.getDefendants().stream().filter(d -> nonNull(d.getOffences())).forEach(
                        defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults())).forEach(
                                offence -> offence.getJudicialResults().forEach(
                                        judicialResult -> {
                                            if (hasCommittingCourt(judicialResult)) {
                                                shouldPopulateCommittingCourt.set(true);
                                            }
                                        }
                                )
                        )
                )
        );

        final List<JudicialResult> judicialResultList = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .map(HearingResultHelper::getAllJudicialResultsFromApplication).flatMap(List::stream).collect(toList());

        judicialResultList.forEach(judicialResult -> {
            if (hasCommittingCourt(judicialResult)) {
                shouldPopulateCommittingCourt.set(true);
            }
        });

        return shouldPopulateCommittingCourt.get();
    }

    public static boolean hasHearingContainsRelatedNextHearings(final Hearing hearing) {
        return haveCourtApplicationsContainRelatedNextHearings(hearing.getCourtApplications())
                || haveProsecutionCasesContainRelatedNextHearings(hearing.getProsecutionCases());
    }

    /**
     * if existing HearingId is not present and next hearing date is not within the multi days
     * hearing then return true to create a new hearing else return false
     *
     * @param hearing hearing to verify
     * @return true if contains a next hearing result that is not for an existing hearing and the
     * hearing date it outside of the current hearings scheduled hearing days.
     */
    public static boolean hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing(final Hearing hearing) {

        final boolean haveProsecutionCasesContainNewNextHearing = isNotEmpty(hearing.getProsecutionCases()) && hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> isNotEmpty(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .anyMatch(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    return !isExistingHearingIdPresent(nextHearing) && (
                            isSingleDayHearing(hearing)
                                    || isNextHearingOutsideOfMultiDaysHearing(nextHearing, hearing));
                });

        final boolean haveCourtApplicationContainNewNextHearing = isNotEmpty(hearing.getCourtApplications()) && hearing.getCourtApplications().stream()
                .flatMap(courtApplication -> getAllJudicialResultsFromApplication(courtApplication).stream())
                .anyMatch(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    return !isExistingHearingIdPresent(nextHearing) && (
                            isSingleDayHearing(hearing)
                                    || isNextHearingOutsideOfMultiDaysHearing(nextHearing, hearing));
                });

        return haveCourtApplicationContainNewNextHearing || haveProsecutionCasesContainNewNextHearing;

    }

    public static NextHearingDetails createRelatedHearings(final Hearing hearing, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt, final SeedingHearing seedingHearing) {

        final List<ProsecutionCase> prosecutionCases = ofNullable(hearing.getProsecutionCases())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .collect(toList());

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .collect(toList());

        final Set<UUID> hearingIds = getAllocatedHearingIds(prosecutionCases, courtApplications);
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
                defendantIds.forEach((defendantId, offenceIds) ->
                        addDefendants(hearing, shouldPopulateCommittingCourt, committingCourt, seedingHearing, prosecutionCase,
                                defendantsToBeAdded, defendantId, offenceIds)
                );
                prosecutionCaseToBeAdded.getDefendants().addAll(defendantsToBeAdded);
                prosecutionCasesToBeAdded.add(prosecutionCaseToBeAdded);
            });
            addProsecutionCasesAndCourtApplicationsToHearingListingNeeds(nextHearings, hearingListingNeedsList, hearingId, prosecutionCasesToBeAdded, hearingsMap2.get(hearingId));
        });
        if (hearingsMap.isEmpty()) {
            hearingsMap2.forEach((hearingId, applications) -> addCourtApplicationsToHearingListingNeeds(nextHearings, hearingListingNeedsList, hearingId, applications));
        }
        return new NextHearingDetails(hearingsMap, hearingListingNeedsList, nextHearings);
    }

    public static Optional<CommittingCourt> getCommittingCourt(final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt) {

        final List<JudicialResult> judicialResults = offence.getJudicialResults();

        if (shouldPopulateCommittingCourt && nonNull(courtCentre) && isNotEmpty(judicialResults) && isNull(offence.getCommittingCourt())) {
            for (final JudicialResult judicialResult : judicialResults) {
                if (nonNull(judicialResult.getNextHearing()) && nonNull(judicialResult.getNextHearing().getJurisdictionType()) &&
                        judicialResult.getNextHearing().getJurisdictionType().equals(JurisdictionType.CROWN)) {
                    return Optional.of(CommittingCourt.committingCourt()
                            .withCourtHouseName(courtCentre.getName())
                            .withCourtHouseCode(courtCentre.getCode())
                            .withCourtHouseShortName(courtCentre.getCode())
                            .withCourtHouseType(JurisdictionType.MAGISTRATES)
                            .withCourtCentreId(courtCentre.getId())
                            .build());
                }
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("squid:S1126")
    public static boolean unscheduledNextHearingsRequiredFor(final Hearing hearing) {
        if (!isNull(hearing.getCourtApplications()) && hearing.getCourtApplications().stream()
                .anyMatch(HearingResultHelper::checksIfUnscheduledHearingNeedsToBeCreated)) {
            return true;
        }

        if (!isNull(hearing.getProsecutionCases()) && hearing.getProsecutionCases().stream()
                .anyMatch(pc -> pc.getDefendants().stream()
                        .anyMatch(d -> d.getOffences().stream()
                                .anyMatch(HearingResultHelper::checksIfUnscheduledHearingNeedsToBeCreated)))) {
            return true;
        }
        return false;
    }

    private static boolean checksIfUnscheduledHearingNeedsToBeCreated(final CourtApplication courtApplication) {
        return !isNull(courtApplication.getJudicialResults())
                && courtApplication.getJudicialResults().stream()
                .anyMatch(jr -> TRUE.equals(jr.getIsNewAmendment()) && (TRUE.equals(jr.getIsUnscheduled()) || hasNextHearingWithDateToBeFixed(jr)));
    }

    private static boolean checksIfUnscheduledHearingNeedsToBeCreated(final Offence offence) {
        return !isNull(offence.getJudicialResults())
                && offence.getJudicialResults().stream()
                .anyMatch(jr -> TRUE.equals(jr.getIsNewAmendment()) && (TRUE.equals(jr.getIsUnscheduled()) || hasNextHearingWithDateToBeFixed(jr)));
    }

    private static void addDefendants(final Hearing hearing, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt, final SeedingHearing seedingHearing, final ProsecutionCase prosecutionCase, final List<Defendant> defendantsToBeAdded, final UUID defendantId, final Set<UUID> offenceIds) {
        final Defendant defendant = getDefendant(prosecutionCase, defendantId);
        final Defendant defendantToBeAdded = createDefendant(defendant);
        final List<Offence> offencesToBeAdded = new ArrayList<>();
        offenceIds.forEach(offenceId -> {
            final Offence offence = getOffence(defendant, offenceId);
            final Offence offenceToBeAdded = createOffence(offence, hearing.getCourtCentre(), shouldPopulateCommittingCourt, committingCourt, seedingHearing);
            offencesToBeAdded.add(offenceToBeAdded);
        });
        defendantToBeAdded.getOffences().addAll(offencesToBeAdded);
        defendantsToBeAdded.add(defendantToBeAdded);
    }

    private static void addCourtApplicationsToHearingListingNeeds(final Map<UUID, NextHearing> nextHearings, final List<HearingListingNeeds> hearingListingNeedsList, final UUID hearingId, final List<CourtApplication> courtApplications) {
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

    private static void addProsecutionCasesAndCourtApplicationsToHearingListingNeeds(final Map<UUID, NextHearing> nextHearings, final List<HearingListingNeeds> hearingListingNeedsList, final UUID hearingId, final List<ProsecutionCase> prosecutionCasesToBeAdded, final List<CourtApplication> courtApplications) {
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

    private static Map<UUID, ProsecutionCase> getProsecutionCasesMap(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .collect(Collectors.toMap(ProsecutionCase::getId, pc -> pc));
    }

    private static Set<UUID> getAllocatedHearingIds(final List<ProsecutionCase> prosecutionCases, final List<CourtApplication> courtApplications) {
        final Set<UUID> hearingIds = new HashSet<>();
        prosecutionCases.stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> CollectionUtils.isNotEmpty(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .forEach(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    if (doesExistingHearingIdPresent(nextHearing)) {
                        hearingIds.add(nextHearing.getExistingHearingId());
                    }
                });
        final List<JudicialResult> judicialResults = ofNullable(courtApplications).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(courtApplication -> getAllJudicialResultsFromApplication(courtApplication).stream())
                .collect(Collectors.toList());

        judicialResults.forEach(judicialResult -> getExistingHearingId(judicialResult).ifPresent(hearingIds::add));

        return hearingIds;
    }

    private static Optional<UUID> getExistingHearingId(JudicialResult judicialResult) {
        final NextHearing nextHearing = judicialResult.getNextHearing();
        if (doesExistingHearingIdPresent(nextHearing)) {
            return ofNullable(nextHearing.getExistingHearingId());
        }
        return empty();
    }

    private static boolean doesExistingHearingIdPresent(final NextHearing nextHearing) {
        return nonNull(nextHearing) && nonNull(nextHearing.getExistingHearingId());
    }

    private static boolean hasNextHearingWithDateToBeFixed(final JudicialResult judicialResult) {

        return (judicialResult.getNextHearing() != null && judicialResult.getNextHearing().getDateToBeFixed() != null)
                ? judicialResult.getNextHearing().getDateToBeFixed() : Boolean.FALSE;
    }

    private static boolean isExistingHearingIdPresent(final NextHearing nextHearing) {
        return nonNull(nextHearing) && nonNull(nextHearing.getExistingHearingId());
    }

    private static boolean isSingleDayHearing(final Hearing hearing) {
        return nonNull(hearing.getHearingDays()) && hearing.getHearingDays().size() == 1;
    }

    private static ProsecutionCase createProsecutionCase(final ProsecutionCase prosecutionCase) {
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
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType())
                .build();
    }

    private static Defendant createDefendant(final Defendant defendant) {
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

    private static Offence createOffence(final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> existingCommittingCourt, final SeedingHearing seedingHearing) {

        final CommittingCourt committingCourt = (nonNull(existingCommittingCourt) && existingCommittingCourt.isPresent()) ? existingCommittingCourt.get() : getCommittingCourt(offence, courtCentre, shouldPopulateCommittingCourt).orElse(null);

        return Offence.offence()
                .withValuesFrom(offence)
                .withCommittingCourt(committingCourt)
                .withSeedingHearing(seedingHearing)
                .build();
    }

    private static Offence getOffence(final Defendant defendant, final UUID offenceId) {
        return defendant.getOffences().stream()
                .filter(off -> off.getId().equals(offenceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Offence not found"));
    }

    private static Defendant getDefendant(final ProsecutionCase prosecutionCase, final UUID defendantId) {
        return prosecutionCase.getDefendants().stream()
                .filter(def -> def.getId().equals(defendantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Defendant not found"));
    }

    private static boolean isNextHearingOutsideOfMultiDaysHearing(final NextHearing nextHearing, final Hearing hearing) {
        if (nonNull(nextHearing)) {
            if (nonNull(hearing.getHearingDays()) && nonNull(nextHearing.getListedStartDateTime())) {
                final List<LocalDate> hearingDays = hearing.getHearingDays().stream().map(HearingDay::getSittingDay).map(ZonedDateTime::toLocalDate).collect(toList());
                return hearingDays.stream().noneMatch(localDate -> localDate.equals(nextHearing.getListedStartDateTime().toLocalDate()));
            }
            return true;
        }
        return false;
    }

    private static void processApplications(final List<CourtApplication> courtApplications, final Map<UUID, List<CourtApplication>> hearingsMap2, final Map<UUID, NextHearing> nextHearings, final UUID hearingId) {
        courtApplications.forEach(courtApplication -> {
            final List<JudicialResult> judicialResults = getAllJudicialResultsFromApplication(courtApplication);
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

    private static void processProsecutionCases(final List<ProsecutionCase> prosecutionCases, final Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap, final Map<UUID, NextHearing> nextHearings, final UUID hearingId) {
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

    private static boolean doCourtApplicationsContainNextHearingResults(final List<CourtApplication> courtApplications) {
        return isNotEmpty(courtApplications) && courtApplications.stream()
                .flatMap(courtApplication -> getAllJudicialResultsFromApplication(courtApplication).stream())
                .anyMatch(judicialResult -> TRUE.equals(judicialResult.getIsNewAmendment()));
    }

    private static boolean doCourtApplicationsContainNextHearing(final List<CourtApplication> courtApplications) {
        return isNotEmpty(courtApplications) && courtApplications.stream()
                .flatMap(courtApplication -> getAllJudicialResultsFromApplication(courtApplication).stream())
                .anyMatch(judicialResult -> nonNull(judicialResult.getNextHearing()));
    }

    private static List<JudicialResult> getAllJudicialResultsFromApplication(final CourtApplication courtApplication) {
        final List<JudicialResult> judicialResults = courtApplication.getJudicialResults();
        final List<JudicialResult> updatedJudicialResults = Optional.ofNullable(judicialResults)
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new);

        ofNullable(courtApplication.getCourtOrder())
                .map(CourtOrder::getCourtOrderOffences)
                .orElseGet(ArrayList::new)
                .stream()
                .map(CourtOrderOffence::getOffence)
                .flatMap(o -> ofNullable(o.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .forEach(updatedJudicialResults::add);

        ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(cac -> ofNullable(cac.getOffences()).map(Collection::stream).orElseGet(Stream::empty))
                .flatMap(o -> ofNullable(o.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .forEach(updatedJudicialResults::add);

        return updatedJudicialResults;
    }

    private static boolean hasCommittingCourt(JudicialResult judicialResult) {
        return nonNull(judicialResult.getResultDefinitionGroup()) &&
                (judicialResult.getResultDefinitionGroup().contains(COMMITTED_TO_CC) ||
                        judicialResult.getResultDefinitionGroup().contains(SENT_TO_CC));
    }

    private static boolean haveCourtApplicationsContainRelatedNextHearings(final List<CourtApplication> courtApplications) {

        return !isEmpty(courtApplications) && courtApplications.stream()
                .filter(courtApplication -> isNotEmpty(courtApplication.getJudicialResults()))
                .flatMap(courtApplication -> courtApplication.getJudicialResults().stream())
                .anyMatch(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    return TRUE.equals(judicialResult.getIsNewAmendment()) && isExistingHearingIdPresent(nextHearing);
                });
    }

    private static boolean haveProsecutionCasesContainRelatedNextHearings(final List<ProsecutionCase> prosecutionCases) {

        return !isEmpty(prosecutionCases) && prosecutionCases.stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> isNotEmpty(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .anyMatch(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    return TRUE.equals(judicialResult.getIsNewAmendment()) && isExistingHearingIdPresent(nextHearing);
                });
    }

}
