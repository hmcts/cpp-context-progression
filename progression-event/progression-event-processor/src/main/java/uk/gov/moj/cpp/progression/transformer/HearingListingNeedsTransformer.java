package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingLanguageNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1188")
public class HearingListingNeedsTransformer {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Inject
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    public List<HearingListingNeeds> transform(final Hearing hearing) {
        final List<UUID> bookingReferences = hearingBookingReferenceListExtractor.extractBookingReferences(hearing.getCourtApplications());
        final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap = provisionalBookingServiceAdapter.getSlots(bookingReferences);
        final Map<String, HearingListingNeeds> hearingListingNeedsMap = new HashMap<>();

        hearing.getCourtApplications().forEach(courtApplication -> {
            final ProsecutionCase linkedProsecutionCase = retrieveProsecutionCase(hearing.getProsecutionCases(), courtApplication.getLinkedCaseId());

            if (isNotEmpty(courtApplication.getJudicialResults())) {
                courtApplication.getJudicialResults().forEach(
                        judicialResult -> transform(courtApplication,
                                judicialResult,
                                hearingListingNeedsMap,
                                bookingReferenceCourtScheduleIdMap,
                                hearing.getJudiciary(),
                                linkedProsecutionCase,
                                hearing.getHearingLanguage()));
            }
        });
        return transformToListingRequest(new ArrayList<>(hearingListingNeedsMap.values()));
    }

    private List<HearingListingNeeds> transformToListingRequest(final List<HearingListingNeeds> hearingListingNeeds) {
        final List<HearingListingNeeds> updatedHearingListingNeeds = new ArrayList<>();
        if (isNotEmpty(hearingListingNeeds)) {
            hearingListingNeeds.forEach(hln -> updatedHearingListingNeeds.add(getUpdatedHearingListingNeeds(hln)));
        }
        return updatedHearingListingNeeds;
    }

    private HearingListingNeeds getUpdatedHearingListingNeeds(final HearingListingNeeds hearingListingNeeds) {
        return HearingListingNeeds.hearingListingNeeds()
                            .withId(hearingListingNeeds.getId())
                            .withCourtApplications(getList(hearingListingNeeds.getCourtApplications()))
                            .withProsecutionCases(getList(hearingListingNeeds.getProsecutionCases()))
                            .withCourtCentre(hearingListingNeeds.getCourtCentre())
                            .withEstimatedMinutes(hearingListingNeeds.getEstimatedMinutes())
                            .withType(hearingListingNeeds.getType())
                            .withJurisdictionType(hearingListingNeeds.getJurisdictionType())
                            .withEarliestStartDateTime(hearingListingNeeds.getEarliestStartDateTime())
                            .withReportingRestrictionReason(hearingListingNeeds.getReportingRestrictionReason())
                            .withDefendantListingNeeds(getList(hearingListingNeeds.getDefendantListingNeeds()))
                            .withJudiciary(hearingListingNeeds.getJudiciary())
                            .withListingDirections(hearingListingNeeds.getListingDirections())
                            .withCourtApplicationPartyListingNeeds(getList(hearingListingNeeds.getCourtApplicationPartyListingNeeds()))
                            .withListedStartDateTime(hearingListingNeeds.getListedStartDateTime())
                            .withProsecutorDatesToAvoid(hearingListingNeeds.getProsecutorDatesToAvoid())
                            .withEndDate(hearingListingNeeds.getEndDate())
                            .withBookingReference(hearingListingNeeds.getBookingReference())
                            .withWeekCommencingDate(hearingListingNeeds.getWeekCommencingDate())
                            .withBookedSlots(hearingListingNeeds.getBookedSlots())
                            .build();
    }

    private ProsecutionCase retrieveProsecutionCase(final List<ProsecutionCase> prosecutionCases, final UUID linkedProsecutionCase) {
        if (isNull(linkedProsecutionCase)) {
            return null;
        }
        if (isNotEmpty(prosecutionCases)) {
            return prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId().equals(linkedProsecutionCase))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Linked Prosecution Case is not available in the hearing"));
        }
        return null;
    }

    private void transform(final CourtApplication courtApplication,
                           final JudicialResult judicialResult,
                           final Map<String, HearingListingNeeds> hearingListingNeedsMap,
                           final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap,
                           final List<JudicialRole> judiciaries,
                           final ProsecutionCase linkedProsecutionCase,
                           final HearingLanguage hearingLanguage) {
        if (isNull(judicialResult.getNextHearing()) || nonNull(judicialResult.getNextHearing().getExistingHearingId())) {
            return;
        }

        final NextHearing nextHearing = judicialResult.getNextHearing();
        final UUID bookingReference = nextHearing.getBookingReference();

        final String key;
        if (nonNull(bookingReference)) {
            if (!bookingReferenceCourtScheduleIdMap.containsKey(bookingReference)) {
                logger.warn("CourtScheduleId not found for BookingReference: {} for courtApplication: {}", bookingReference, courtApplication.getId());
                return;
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
        final CourtApplication newCourtApplication = createApplication(courtApplication);
        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = createCourtApplicationPartyListingNeeds(courtApplication, hearingLanguage);
        hearingListingNeeds.getCourtApplications().add(newCourtApplication);
        hearingListingNeeds.getCourtApplicationPartyListingNeeds().addAll(courtApplicationPartyListingNeeds);
        if (nonNull(linkedProsecutionCase)) {
            hearingListingNeeds.getProsecutionCases().add(linkedProsecutionCase);
        }
    }

    private List<CourtApplicationPartyListingNeeds> createCourtApplicationPartyListingNeeds(final CourtApplication courtApplication, final HearingLanguage hearingLanguage) {
        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = new ArrayList<>();
        if (nonNull(courtApplication.getApplicant().getProsecutingAuthority())) {
            final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                    .withCourtApplicationId(courtApplication.getId())
                    .withCourtApplicationPartyId(courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityId())
                    .withHearingLanguageNeeds(nonNull(hearingLanguage) ? HearingLanguageNeeds.valueOf(hearingLanguage.name()) : HearingLanguageNeeds.ENGLISH)
                    .build();
            courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
        }
        if (isNotEmpty(courtApplication.getRespondents())) {
            courtApplication.getRespondents().forEach(courtApplicationRespondent -> {
                if (nonNull(courtApplicationRespondent.getPartyDetails().getProsecutingAuthority())) {
                    final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                            .withCourtApplicationId(courtApplication.getId())
                            .withCourtApplicationPartyId(courtApplicationRespondent.getPartyDetails().getId())
                            .withHearingLanguageNeeds(HearingLanguageNeeds.valueOf(hearingLanguage.name()))
                            .build();
                    courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
                }
            });
        }
        return courtApplicationPartyListingNeedsList;
    }

    private CourtApplication createApplication(final CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(courtApplication.getRespondents())
                .withType(courtApplication.getType())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withLinkedCaseId(courtApplication.getLinkedCaseId())
                .withApplicationOutcome(courtApplication.getApplicationOutcome())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withOutOfTimeReasons(courtApplication.getOutOfTimeReasons())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withCourtApplicationPayment(courtApplication.getCourtApplicationPayment())
                .withOrderingCourt(courtApplication.getOrderingCourt())
                .withApplicationDecisionSoughtByDate(courtApplication.getApplicationDecisionSoughtByDate())
                .withBreachedOrder(courtApplication.getBreachedOrder())
                .withBreachedOrderDate(courtApplication.getBreachedOrderDate())
                .withDueDate(courtApplication.getDueDate())
                .withRemovalReason(courtApplication.getRemovalReason())
                .build();
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
                .withProsecutionCases(new ArrayList<>())
                .withReportingRestrictionReason(nextHearing.getReportingRestrictionReason())
                .withCourtApplicationPartyListingNeeds(new ArrayList<>())
                .withCourtApplications(new ArrayList<>())
                .withId(randomUUID())
                .build();
    }

    private String createMapKey(final NextHearing nextHearing) {
        return new StringBuilder()
                .append("Location:").append(nextHearing.getCourtCentre().getCourtHearingLocation()).append(',')
                .append("RoomId:").append(nextHearing.getCourtCentre().getRoomId()).append(',')
                .append("ListedStartDateTime:").append(nextHearing.getListedStartDateTime()).append(',')
                .append("WeekCommencingDate:").append(nextHearing.getWeekCommencingDate()).append(',')
                .append("EstimatedMinutes:").append(nextHearing.getEstimatedMinutes()).append(',')
                .append("Type:").append(nextHearing.getType().getId())
                .toString();
    }

    private String createMapKey(final Set<UUID> courtScheduleIds) {
        final String mergedCourtScheduleIds = courtScheduleIds.stream().map(UUID::toString).sorted().collect(joining(","));
        return new StringBuilder()
                .append("CourtScheduleId:").append(mergedCourtScheduleIds)
                .toString();
    }

    private  <T> List<T> getList(final List<T> list) {
        return isNotEmpty(list) ? list : null;
    }
}