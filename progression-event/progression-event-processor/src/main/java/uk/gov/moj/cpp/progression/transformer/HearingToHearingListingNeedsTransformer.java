package uk.gov.moj.cpp.progression.transformer;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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

import org.slf4j.Logger;


public class HearingToHearingListingNeedsTransformer {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Inject
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    @Inject
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Inject
    private HearingResultHelper hearingResultHelper;

    public List<HearingListingNeeds> transform(final Hearing hearing, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {
        final List<UUID> bookingReferenceList = hearingBookingReferenceListExtractor.extractBookingReferenceList(hearing);
        final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap = provisionalBookingServiceAdapter.getSlots(bookingReferenceList);
        final Map<String, HearingListingNeeds> hearingListingNeedsMap = new HashMap<>();

        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(prosecutionCase -> prosecutionCase.getDefendants().stream().forEach(
                                defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults())).forEach(
                                        offence -> offence.getJudicialResults().stream().forEach(
                                                judicialResult -> transform(prosecutionCase, defendant, offence, judicialResult, hearingListingNeedsMap,
                                                        bookingReferenceCourtScheduleIdMap, hearing, shouldPopulateCommittingCourt, committingCourt)
                                        )
                                )
                        )
                );

        transformForApplications(hearing, bookingReferenceCourtScheduleIdMap, hearingListingNeedsMap);

        return new ArrayList<>(hearingListingNeedsMap.values());
    }

    /**
     * Transform the Hearing information with SeedHearing to List of hearing and If next hearing is
     * from a single day hearing or not within the multi days hearing then create a new hearing
     * record
     */
    public List<HearingListingNeeds> transformWithSeedHearing(final Hearing hearing, final Optional<CommittingCourt> committingCourt, final SeedingHearing seedingHearing, final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIds) {
        final Map<String, HearingListingNeeds> hearingListingNeedsMap = new HashMap<>();

        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(
                        prosecutionCase -> prosecutionCase.getDefendants().stream().forEach(
                                defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults()))
                                        .map(offence -> Offence.offence().withValuesFrom(offence).withSeedingHearing(seedingHearing).build())
                                        .forEach(offence -> offence.getJudicialResults().stream()
                                                .forEach(judicialResult -> {
                                                            if (!isNextHearingApplication(judicialResult) && (isSingleDayHearingWithNextHearing(judicialResult.getNextHearing(), hearing) || isNextHearingOutsideOfMultiDaysHearing(judicialResult.getNextHearing(), hearing))) {
                                                                transform(prosecutionCase, defendant, offence, judicialResult, hearingListingNeedsMap, bookingReferenceCourtScheduleIds,
                                                                        hearing, committingCourt.isPresent(), committingCourt);
                                                            }
                                                        }
                                                )
                                        )
                        )
                );

        transformForApplications(hearing, bookingReferenceCourtScheduleIds, hearingListingNeedsMap);
        setSeedingHearingForApplicationsOffences(hearing.getCourtApplications(), seedingHearing);

        return new ArrayList<>(hearingListingNeedsMap.values());
    }

    private boolean isNextHearingApplication(final JudicialResult judicialResult) {
        return nonNull(judicialResult.getNextHearing()) && isNotEmpty(judicialResult.getNextHearing().getApplicationTypeCode());
    }

    private void setSeedingHearingForApplicationsOffences(final List<CourtApplication> courtApplications, final SeedingHearing seedingHearing) {
        ofNullable(courtApplications).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(courtApplication -> {
                            ofNullable(courtApplication.getCourtOrder())
                                    .map(CourtOrder::getCourtOrderOffences)
                                    .orElseGet(ArrayList::new)
                                    .stream()
                                    .map(CourtOrderOffence::getOffence)
                                    .map(o -> Offence.offence().withValuesFrom(o).withSeedingHearing(seedingHearing).build());


                            ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                                    .flatMap(cac -> ofNullable(cac.getOffences()).map(Collection::stream).orElseGet(Stream::empty))
                                    .map(o -> Offence.offence().withValuesFrom(o).withSeedingHearing(seedingHearing).build());
                        }
                );
    }

    /**
     * This method returns map of booking references and list of court schedule ids by combining
     * both previous booking references and current booking references.
     *
     * @param hearing                                       has current booking references.
     * @param previousBookingReferencesWithCourtScheduleIds previous booking references.
     * @return
     */
    public Map<UUID, Set<UUID>> getCombinedBookingReferencesAndCourtScheduleIds(final Hearing hearing, final Map<UUID, List<UUID>> previousBookingReferencesWithCourtScheduleIds) {

        final List<UUID> bookingReferences = hearingBookingReferenceListExtractor.extractBookingReferenceList(hearing);
        final List<UUID> notBookedIds = bookingReferences.stream()
                .filter(id -> !previousBookingReferencesWithCourtScheduleIds.keySet().contains(id))
                .collect(toList());
        final Map<UUID, Set<UUID>> currentBookingReferencesWithCourtScheduleIds = provisionalBookingServiceAdapter.getSlots(notBookedIds);
        final Map<UUID, Set<UUID>> combinedBookingReferencesWithCourtScheduleIds = new HashMap<>(currentBookingReferencesWithCourtScheduleIds);
        previousBookingReferencesWithCourtScheduleIds.forEach((k, v) -> combinedBookingReferencesWithCourtScheduleIds.put(k, new HashSet<>(v)));

        return combinedBookingReferencesWithCourtScheduleIds;

    }

    public List<HearingListingNeeds> transform(final Hearing hearing) {
        return transform(hearing, false, Optional.empty());
    }

    private void transform(final ProsecutionCase prosecutionCase,
                           final Defendant defendant,
                           final Offence offence,
                           final JudicialResult judicialResult,
                           final Map<String, HearingListingNeeds> hearingListingNeedsMap,
                           final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap,
                           final Hearing hearing,
                           final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {
        if (isNull(judicialResult.getNextHearing()) || nonNull(judicialResult.getNextHearing().getExistingHearingId()) || nonNull(judicialResult.getNextHearing().getDateToBeFixed())) {
            return;
        }

        final NextHearing nextHearing = judicialResult.getNextHearing();

        final UUID bookingReference = nextHearing.getBookingReference();

        final String key;
        if (nonNull(bookingReference)) {
            if (!bookingReferenceCourtScheduleIdMap.containsKey(bookingReference)) {
                logger.warn("CourtScheduleId not found for BookingReference: {} for prosecutionCase: {}", bookingReference, prosecutionCase.getId());
                return;
            }
            key = createMapKey(bookingReferenceCourtScheduleIdMap.get(bookingReference), nextHearing.getType());
        } else {
            key = createMapKey(nextHearing);
        }

        final HearingListingNeeds hearingListingNeeds;

        if (hearingListingNeedsMap.containsKey(key)) {
            hearingListingNeeds = hearingListingNeedsMap.get(key);
        } else {
            hearingListingNeeds = createHearingListingNeeds(nextHearing, hearing.getJudiciary(), prosecutionCase.getIsGroupMaster());
            hearingListingNeedsMap.put(key, hearingListingNeeds);
        }

        addProsecutionCase(hearingListingNeeds, prosecutionCase, defendant, offence, hearing, shouldPopulateCommittingCourt, committingCourt);
    }

    private void transformForApplications(final Hearing hearing, final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap, final Map<String, HearingListingNeeds> hearingListingNeedsMap) {
        ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(courtApplication -> {
                    final List<JudicialResult> judicialResults = hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication);
                    judicialResults.forEach(judicialResult -> transform(courtApplication,
                            judicialResult,
                            hearingListingNeedsMap,
                            bookingReferenceCourtScheduleIdMap,
                            hearing.getJudiciary(),
                            hearing.getProsecutionCases()));
                });
    }

    private void transform(final CourtApplication courtApplication,
                           final JudicialResult judicialResult,
                           final Map<String, HearingListingNeeds> hearingListingNeedsMap,
                           final Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap,
                           final List<JudicialRole> judiciaries, List<ProsecutionCase> prosecutionCases) {

        if (validateNextHearing(judicialResult)) {
            return;
        }

        final NextHearing nextHearing = judicialResult.getNextHearing();

        final UUID bookingReference = nextHearing.getBookingReference();

        if (nonNull(bookingReference) && !bookingReferenceCourtScheduleIdMap.containsKey(bookingReference)) {
            logger.warn("CourtScheduleId not found for BookingReference: {} for Application Id: {}", bookingReference, courtApplication.getId());
            return;
        }

        final String key = getKey(bookingReferenceCourtScheduleIdMap, nextHearing, bookingReference);

        final HearingListingNeeds hearingListingNeeds = addCourtApplication(createHearingListingNeeds(nextHearing, judiciaries, false), courtApplication, prosecutionCases);

        if (hearingListingNeedsMap.containsKey(key)) {
            if (isNull(hearingListingNeedsMap.get(key).getCourtApplications())) {
                final HearingListingNeeds hearingListingNeedsWithCourtApplication = addCourtApplication(hearingListingNeedsMap.get(key), courtApplication);
                hearingListingNeedsMap.put(key, hearingListingNeedsWithCourtApplication);
            } else if (nonNull(hearingListingNeedsMap.get(key).getCourtApplications())
                    && isNewApplication(hearingListingNeedsMap.get(key), courtApplication)) {
                hearingListingNeedsMap.get(key).getCourtApplications().add(courtApplication);
            }
        } else {
            hearingListingNeedsMap.put(key, hearingListingNeeds);
        }

    }

    private boolean isNewApplication(final HearingListingNeeds hearingListingNeeds, final CourtApplication courtApplication) {
        if(nonNull(hearingListingNeeds.getCourtApplications()) && nonNull(courtApplication.getId())) {
           return  !hearingListingNeeds.getCourtApplications().stream().anyMatch(x -> courtApplication.getId().equals(x.getId()));
        }
        return true;
    }

    private String getKey(Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap, NextHearing nextHearing, UUID bookingReference) {
        final String key;
        if (nonNull(bookingReference)) {
            key = createMapKey(bookingReferenceCourtScheduleIdMap.get(bookingReference), nextHearing.getType());
        } else {
            key = createMapKey(nextHearing);
        }
        return key;
    }

    private boolean validateNextHearing(JudicialResult judicialResult) {
        return isNull(judicialResult.getNextHearing()) || nonNull(judicialResult.getNextHearing().getExistingHearingId()) || nonNull(judicialResult.getNextHearing().getDateToBeFixed());
    }

    private HearingListingNeeds addCourtApplication(final HearingListingNeeds hearingListingNeeds, final CourtApplication courtApplication, List<ProsecutionCase> prosecutionCases) {
        final HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds).withProsecutionCases(prosecutionCases);
        if (isEmpty(hearingListingNeeds.getCourtApplications())) {
            final List<CourtApplication> courtApplications = new ArrayList<>();
            courtApplications.add(courtApplication);
            builder.withCourtApplications(courtApplications);
        } else {
            hearingListingNeeds.getCourtApplications().add(courtApplication);
        }
        return builder.build();
    }

    private HearingListingNeeds addCourtApplication(final HearingListingNeeds hearingListingNeeds, final CourtApplication courtApplication) {
        final HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds);

        final List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(courtApplication);
        builder.withCourtApplications(courtApplications);

        return builder.build();
    }

    private void addProsecutionCase(final HearingListingNeeds hearingListingNeeds, final ProsecutionCase prosecutionCase, final Defendant defendant,
                                    final Offence offence, final Hearing hearing, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {

        final ProsecutionCase prosecutionCaseInNeeds = createProsecutionCase(hearingListingNeeds, prosecutionCase);
        final Defendant defendantInNeeds = createDefendant(prosecutionCaseInNeeds, defendant);
        createOffence(defendantInNeeds, offence, hearing, shouldPopulateCommittingCourt, committingCourt);

    }

    private ProsecutionCase createProsecutionCase(final HearingListingNeeds hearingListingNeeds, final ProsecutionCase prosecutionCase) {
        final Optional<ProsecutionCase> prosecutionCaseInNeedsOpt = hearingListingNeeds.getProsecutionCases().stream().filter(pc -> pc.getId().equals(prosecutionCase.getId())).findFirst();
        if (prosecutionCaseInNeedsOpt.isPresent()) {
            return prosecutionCaseInNeedsOpt.get();
        }
        final ProsecutionCase prosecutionCaseInNeeds = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(new ArrayList<>())
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
                .withValuesFrom(defendant)
                .withOffences(new ArrayList<>())
                .build();

        prosecutionCaseInNeeds.getDefendants().add(defendantInNeeds);
        return defendantInNeeds;
    }

    private Offence createOffence(final Defendant defendantInNeeds, final Offence offence, final Hearing hearing,
                                  final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> existingCommittingCourt) {
        final Optional<Offence> offenceInNeedsOpt = defendantInNeeds.getOffences().stream().filter(d -> d.getId().equals(offence.getId())).findFirst();
        if (offenceInNeedsOpt.isPresent()) {
            return offenceInNeedsOpt.get();
        }

        final CommittingCourt committingCourt = (nonNull(existingCommittingCourt) && existingCommittingCourt.isPresent()) ?
                existingCommittingCourt.get() : offenceToCommittingCourtConverter.convert(offence, hearing.getCourtCentre(), shouldPopulateCommittingCourt).orElse(null);

        final Offence offenceInNeeds = Offence.offence()
                .withValuesFrom(offence)
                .withCommittingCourt(committingCourt)
                .build();

        defendantInNeeds.getOffences().add(offenceInNeeds);
        return offenceInNeeds;
    }

    private HearingListingNeeds createHearingListingNeeds(final NextHearing nextHearing, final List<JudicialRole> judiciaries, final Boolean isGroupProceedings) {
        WeekCommencingDate weekCommencingDate = null;
        if (nonNull(nextHearing.getWeekCommencingDate())) {
            weekCommencingDate = WeekCommencingDate.weekCommencingDate()
                    .withStartDate(nextHearing.getWeekCommencingDate())
                    .withDuration(1)
                    .build();
        }

        final HearingListingNeeds.Builder hearingListingNeedsBuilder = HearingListingNeeds.hearingListingNeeds()
                .withBookingReference(nextHearing.getBookingReference())
                .withBookedSlots(nextHearing.getHmiSlots())
                .withCourtCentre(nextHearing.getCourtCentre())
                .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                .withEstimatedDuration(nextHearing.getEstimatedDuration())
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
                .withId(randomUUID());

        if(TRUE.equals(isGroupProceedings)) {
            hearingListingNeedsBuilder.withIsGroupProceedings(TRUE);
        }
        return hearingListingNeedsBuilder.build();
    }

    private String createMapKey(final NextHearing nextHearing) {
        return "Location:" + nextHearing.getCourtCentre().getCourtHearingLocation() + ',' +
                "RoomId:" + nextHearing.getCourtCentre().getRoomId() + ',' +
                "ListedStartDateTime:" + nextHearing.getListedStartDateTime() + ',' +
                "WeekCommencingDate:" + nextHearing.getWeekCommencingDate() + ',' +
                "EstimatedMinutes:" + nextHearing.getEstimatedMinutes() + ',' +
                "Type:" + nextHearing.getType().getId();
    }

    private String createMapKey(final Set<UUID> courtScheduleIds, final HearingType type) {
        final String mergedCourtScheduleIds = courtScheduleIds.stream().map(UUID::toString).sorted().collect(Collectors.joining(","));
        return "CourtScheduleId:" + mergedCourtScheduleIds+ ',' +
                "Type:" + type.getId();
    }


    private boolean isSingleDayHearingWithNextHearing(final NextHearing nextHearing, final Hearing hearing) {
        if (nonNull(nextHearing)) {
            return hearing.getHearingDays() != null && hearing.getHearingDays().size() == 1;
        }
        return false;
    }

    private boolean isNextHearingOutsideOfMultiDaysHearing(final NextHearing nextHearing, final Hearing hearing) {
        if (nonNull(nextHearing)) {
            if (nonNull(hearing.getHearingDays()) && nonNull(nextHearing.getListedStartDateTime())) {
                final List<LocalDate> hearingDays = hearing.getHearingDays().stream().map(HearingDay::getSittingDay).map(ZonedDateTime::toLocalDate).collect(toList());
                return hearingDays.stream().noneMatch(localDate -> localDate.equals(nextHearing.getListedStartDateTime().toLocalDate()));
            }
            return true;
        }
        return false;
    }
}
