package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HearingToHearingListingNeedsTransformer {

    @Inject
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Inject
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    public List<HearingListingNeeds> transform(final Hearing hearing) {
        final List<UUID> bookingReferenceList = hearingBookingReferenceListExtractor.extractBookingReferenceList(hearing);
        final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap = provisionalBookingServiceAdapter.getSlots(bookingReferenceList);
        final Map<String, HearingListingNeeds> hearingListingNeedsMap = new HashMap<>();

        hearing.getProsecutionCases().stream().forEach(
                prosecutionCase -> prosecutionCase.getDefendants().stream().forEach(
                        defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults())).forEach(
                                offence -> offence.getJudicialResults().stream().forEach(
                                        judicialResult -> transform(prosecutionCase, defendant, offence, judicialResult, hearingListingNeedsMap, bookingReferenceCourtScheduleIdMap, hearing.getJudiciary())
                                )

                        )
                )
        );
        return new ArrayList<>(hearingListingNeedsMap.values());
    }

    private void transform(final ProsecutionCase prosecutionCase,
                           final Defendant defendant,
                           final Offence offence,
                           final JudicialResult judicialResult,
                           final Map<String, HearingListingNeeds> hearingListingNeedsMap,
                           final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap,
                           final List<JudicialRole> judiciaries){
        if (isNull(judicialResult.getNextHearing()) || nonNull(judicialResult.getNextHearing().getExistingHearingId()) || nonNull(judicialResult.getNextHearing().getDateToBeFixed())) {
            return;
        }

        final NextHearing nextHearing = judicialResult.getNextHearing();

        final UUID bookingReference = nextHearing.getBookingReference();

        final String key;
        if (nonNull(bookingReference)) {
            if (!bookingReferenceCourtScheduleIdMap.containsKey(bookingReference)) {
                throw new IllegalStateException("CourtScheduleId not found for BookingReference:" + bookingReference.toString());
            }
            key = createMapKey(bookingReferenceCourtScheduleIdMap.get(bookingReference));
        } else {
            key = createMapKey(nextHearing);
        }

        final HearingListingNeeds hearingListingNeeds;

        if (hearingListingNeedsMap.containsKey(key)) {
            hearingListingNeeds = hearingListingNeedsMap.get(key);
        } else {
            hearingListingNeeds = createHearingListingNeeds(nextHearing, judiciaries);
            hearingListingNeedsMap.put(key, hearingListingNeeds);
        }

        addProsecutionCase(hearingListingNeeds, prosecutionCase, defendant, offence);
    }

    private void addProsecutionCase(final HearingListingNeeds hearingListingNeeds, final ProsecutionCase prosecutionCase, final Defendant defendant, final Offence offence) {

        final ProsecutionCase prosecutionCaseInNeeds = createProsecutionCase(hearingListingNeeds, prosecutionCase);
        final Defendant defendantInNeeds = createDefendant(prosecutionCaseInNeeds, defendant);
        createOffence(defendantInNeeds, offence);

    }

    private ProsecutionCase createProsecutionCase(final HearingListingNeeds hearingListingNeeds, final ProsecutionCase prosecutionCase) {
        final Optional<ProsecutionCase> prosecutionCaseInNeedsOpt = hearingListingNeeds.getProsecutionCases().stream().filter(pc -> pc.getId().equals(prosecutionCase.getId())).findFirst();
        if (prosecutionCaseInNeedsOpt.isPresent()) {
            return prosecutionCaseInNeedsOpt.get();
        }
        final ProsecutionCase prosecutionCaseInNeeds = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withId(prosecutionCase.getId())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withClassOfCase(prosecutionCase.getClassOfCase())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .build();
        hearingListingNeeds.getProsecutionCases().add(prosecutionCaseInNeeds);
        return prosecutionCaseInNeeds;
    }

    private Defendant createDefendant(final ProsecutionCase prosecutionCaseInNeeds, final Defendant defendant) {
        final Optional<Defendant> defendantInNeedsOpt = prosecutionCaseInNeeds.getDefendants().stream().filter(d -> d.getId().equals(defendant.getId())).findFirst();
        if (defendantInNeedsOpt.isPresent()) {
            return defendantInNeedsOpt.get();
        }
        final Defendant defendantInNeeds = Defendant.defendant()
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
                .withDefendantCaseJudicialResults(defendant.getDefendantCaseJudicialResults())
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

        prosecutionCaseInNeeds.getDefendants().add(defendantInNeeds);
        return defendantInNeeds;
    }

    private Offence createOffence(final Defendant defendantInNeeds, final Offence offence) {
        final Optional<Offence> offenceInNeedsOpt = defendantInNeeds.getOffences().stream().filter(d -> d.getId().equals(offence.getId())).findFirst();
        if (offenceInNeedsOpt.isPresent()) {
            return offenceInNeedsOpt.get();
        }
        final Offence offenceInNeeds = Offence.offence()
                .withProceedingsConcluded(offence.getProceedingsConcluded())
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
                .withIntroducedAfterInitialProceedings(offence.getIntroducedAfterInitialProceedings())
                .withIsDiscontinued(offence.getIsDiscontinued())
                .withIsDisposed(offence.getIsDisposed())
                .withLaaApplnReference(offence.getLaaApplnReference())
                .withLaidDate(offence.getLaidDate())
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
                .withStartDate(offence.getStartDate())
                .withVerdict(offence.getVerdict())
                .withVictims(offence.getVictims())
                .withWording(offence.getWording())
                .withWordingWelsh(offence.getWordingWelsh())
                .build();

        defendantInNeeds.getOffences().add(offenceInNeeds);
        return offenceInNeeds;
    }

    private HearingListingNeeds createHearingListingNeeds(final NextHearing nextHearing, final List<JudicialRole> judiciaries) {
        WeekCommencingDate weekCommencingDate = null;
        if (nonNull(nextHearing.getWeekCommencingDate())) {
            weekCommencingDate = WeekCommencingDate.weekCommencingDate()
                    .withStartDate(nextHearing.getWeekCommencingDate())
                    .withDuration(1)
                    .build();
        }

        return HearingListingNeeds.hearingListingNeeds()
                .withBookingReference(nextHearing.getBookingReference())
                .withCourtCentre(nextHearing.getCourtCentre())
                .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                .withType(nextHearing.getType())
                .withJudiciary((nonNull(nextHearing.getReservedJudiciary()) && nextHearing.getReservedJudiciary()) ? judiciaries : nextHearing.getJudiciary())
                .withJurisdictionType(nextHearing.getJurisdictionType())
                .withListedStartDateTime(nextHearing.getListedStartDateTime())
                .withWeekCommencingDate(weekCommencingDate)
                .withListingDirections(null)
                .withProsecutionCases(new ArrayList<>())
                .withProsecutorDatesToAvoid(null)
                .withReportingRestrictionReason(null)
                .withCourtApplicationPartyListingNeeds(null)
                .withCourtApplications(null)
                .withDefendantListingNeeds(null)
                .withEarliestStartDateTime(null)
                .withEndDate(null)
                .withId(randomUUID())
                .build();
    }

    private String createMapKey(NextHearing nextHearing) {
        return new StringBuilder()
                .append("Location:").append(nextHearing.getCourtCentre().getCourtHearingLocation()).append(',')
                .append("RoomId:").append(nextHearing.getCourtCentre().getRoomId()).append(',')
                .append("ListedStartDateTime:").append(nextHearing.getListedStartDateTime()).append(',')
                .append("WeekCommencingDate:").append(nextHearing.getWeekCommencingDate()).append(',')
                .append("EstimatedMinutes:").append(nextHearing.getEstimatedMinutes()).append(',')
                .append("Type:").append(nextHearing.getType().getId())
                .toString();
    }

    private String createMapKey(Set<UUID> courtScheduleIds) {
        final String mergedCourtScheduleIds = courtScheduleIds.stream().map(UUID::toString).sorted().collect(Collectors.joining(","));
        return "CourtScheduleId:" + mergedCourtScheduleIds;
    }
}