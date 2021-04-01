package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
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
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
                .forEach(prosecutionCase -> prosecutionCase.getDefendants()
                        .forEach(defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults()))
                                .forEach(offence -> offence.getJudicialResults()
                                        .forEach(judicialResult ->
                                                transform(
                                                        prosecutionCase,
                                                        defendant,
                                                        offence,
                                                        judicialResult,
                                                        hearingListingNeedsMap,
                                                        bookingReferenceCourtScheduleIdMap,
                                                        hearing.getJudiciary(),
                                                        hearing.getCourtCentre(),
                                                        shouldPopulateCommittingCourt,
                                                        committingCourt)))));

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

        return new ArrayList<>(hearingListingNeedsMap.values());
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
                           final List<JudicialRole> judiciaries,
                           final CourtCentre courtCentre,
                           final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {

        if (validateNextHearing(judicialResult)) {
            return;
        }

        final NextHearing nextHearing = judicialResult.getNextHearing();

        final UUID bookingReference = nextHearing.getBookingReference();

        if (nonNull(bookingReference) && !bookingReferenceCourtScheduleIdMap.containsKey(bookingReference)) {
            logger.warn("CourtScheduleId not found for BookingReference: {} for prosecutionCase: {}", bookingReference, prosecutionCase.getId());
            return;
        }

        final String key = getKey(bookingReferenceCourtScheduleIdMap, nextHearing, bookingReference);

        final HearingListingNeeds hearingListingNeeds = hearingListingNeedsMap.computeIfAbsent(key, v -> createHearingListingNeeds(nextHearing, judiciaries));

        addProsecutionCase(hearingListingNeeds, prosecutionCase, defendant, offence, courtCentre, shouldPopulateCommittingCourt, committingCourt);
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

        final HearingListingNeeds hearingListingNeeds = addCourtApplication(createHearingListingNeeds(nextHearing, judiciaries), courtApplication, prosecutionCases);

        hearingListingNeedsMap.computeIfAbsent(key, v -> hearingListingNeeds);

    }

    private String getKey(Map<UUID, Set<UUID>> bookingReferenceCourtScheduleIdMap, NextHearing nextHearing, UUID bookingReference) {
        final String key;
        if (nonNull(bookingReference)) {
            key = createMapKey(bookingReferenceCourtScheduleIdMap.get(bookingReference));
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

    private void addProsecutionCase(final HearingListingNeeds hearingListingNeeds, final ProsecutionCase prosecutionCase, final Defendant defendant,
                                    final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {

        final ProsecutionCase prosecutionCaseInNeeds = createProsecutionCase(hearingListingNeeds, prosecutionCase);
        final Defendant defendantInNeeds = createDefendant(prosecutionCaseInNeeds, defendant);
        createOffence(defendantInNeeds, offence, courtCentre, shouldPopulateCommittingCourt, committingCourt);
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

    private void createOffence(final Defendant defendantInNeeds, final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> existingCommittingCourt) {
        final Optional<Offence> offenceInNeedsOpt = defendantInNeeds.getOffences().stream().filter(d -> d.getId().equals(offence.getId())).findFirst();
        if (offenceInNeedsOpt.isPresent()) {
            return;
        }

        final CommittingCourt committingCourt = (nonNull(existingCommittingCourt) && existingCommittingCourt.isPresent()) ? existingCommittingCourt.get() : offenceToCommittingCourtConverter.convert(offence, courtCentre, shouldPopulateCommittingCourt).orElse(null);

        final Offence offenceInNeeds = Offence.offence()
                .withValuesFrom(offence)
                .withCommittingCourt(committingCourt)
                .build();

        defendantInNeeds.getOffences().add(offenceInNeeds);
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

    private String createMapKey(final NextHearing nextHearing) {
        return "Location:" + nextHearing.getCourtCentre().getCourtHearingLocation() + ',' +
                "RoomId:" + nextHearing.getCourtCentre().getRoomId() + ',' +
                "ListedStartDateTime:" + nextHearing.getListedStartDateTime() + ',' +
                "WeekCommencingDate:" + nextHearing.getWeekCommencingDate() + ',' +
                "EstimatedMinutes:" + nextHearing.getEstimatedMinutes() + ',' +
                "Type:" + nextHearing.getType().getId();
    }

    private String createMapKey(final Set<UUID> courtScheduleIds) {
        final String mergedCourtScheduleIds = courtScheduleIds.stream().map(UUID::toString).sorted().collect(Collectors.joining(","));
        return "CourtScheduleId:" + mergedCourtScheduleIds;
    }
}
