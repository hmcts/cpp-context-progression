package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated.extendHearingDefendantRequestCreated;
import static uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated.extendHearingDefendantRequestUpdated;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.HearingApplicationRequestCreated.hearingApplicationRequestCreated;
import static uk.gov.justice.core.courts.HearingDefendantRequestCreated.hearingDefendantRequestCreated;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged;
import static uk.gov.justice.core.courts.ProsecutionCasesResulted.prosecutionCasesResulted;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.progression.courts.ApplicationsResulted.applicationsResulted;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isAllDefendantProceedingConcluded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingHelper.isEligibleForNextHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.checkResultLinesForCommittingCourt;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.createRelatedHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.doHearingContainNextHearingResults;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.hasHearingContainsRelatedNextHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.unscheduledNextHearingsRequiredFor;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.CasesAddedForUpdatedRelatedHearing;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.DefendantRequestToExtendHearingCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationRequestCreated;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.core.courts.UnscheduledHearingRecorded;
import uk.gov.justice.core.courts.UnscheduledNextHearingsRequested;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.BookingReferencesAndCourtScheduleIdsStored;
import uk.gov.justice.progression.courts.DeleteNextHearingsRequested;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UnscheduledHearingAllocationNotified;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.NextHearingDetails;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1948", "squid:S1172"})
public class HearingAggregate implements Aggregate {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAggregate.class);
    private static final long serialVersionUID = 9128521802762667383L;
    private final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
    private final List<CourtApplicationPartyListingNeeds> applicationListingNeeds = new ArrayList<>();
    private Hearing hearing;
    private HearingListingStatus hearingListingStatus;
    private Boolean unscheduledHearingListedFromThisHearing;
    private boolean duplicate;
    private boolean deleted;
    private CommittingCourt committingCourt;
    private Map<String, Boolean> hasNextHearingForHearingDay = new HashMap<>();
    private Map<UUID, RelatedHearingUpdated> relatedHearingUpdatedMap = new HashMap<>();
    private Map<LocalDate, List<BookingReferenceCourtScheduleIds>> bookingReferencesAndCourtScheduleIdsForHearingDay = new HashMap<>();
    private Boolean notifyNCES = false;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingInitiateEnriched.class).apply(e ->
                        this.hearing = e.getHearing()
                ),
                when(ProsecutionCaseDefendantListingStatusChanged.class).apply(e -> {
                    this.hearing = e.getHearing();
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                    this.notifyNCES = nonNull(e.getNotifyNCES()) ? e.getNotifyNCES() : Boolean.FALSE;
                }),
                when(HearingResulted.class).apply(e -> {
                    this.hearing = e.getHearing();
                    this.committingCourt = findCommittingCourt(e.getHearing());
                }),
                when(HearingDefendantRequestCreated.class).apply(e -> {
                    if (!e.getDefendantRequests().isEmpty()) {
                        listDefendantRequests.addAll(e.getDefendantRequests());
                    }
                }),
                when(HearingApplicationRequestCreated.class).apply(e -> {
                    if (isNotEmpty(e.getApplicationRequests())) {
                        applicationListingNeeds.addAll(e.getApplicationRequests());
                    }
                }),
                when(ExtendHearingDefendantRequestUpdated.class).apply(e -> {
                    if (isNotEmpty(listDefendantRequests)) {
                        listDefendantRequests.clear();
                        listDefendantRequests.addAll(e.getDefendantRequests());
                    }
                }),
                when(UnscheduledHearingListingRequested.class).apply(e ->
                        this.unscheduledHearingListedFromThisHearing = true
                ),
                when(HearingDaysWithoutCourtCentreCorrected.class).apply(this::onHearingDaysWithoutCourtCentreCorrected),
                when(HearingMarkedAsDuplicate.class).apply(e ->
                        this.duplicate = true
                ),
                when(DefendantRequestToExtendHearingCreated.class).apply(e ->
                        listDefendantRequests.addAll(e.getDefendantRequests())
                ),
                when(UnscheduledHearingAllocationNotified.class).apply(e ->
                        this.notifyNCES = false
                ),
                when(HearingDeleted.class).apply(e ->
                        this.deleted = true
                ),
                when(NextHearingsRequested.class).apply(e ->
                        this.hasNextHearingForHearingDay.put(e.getSeedingHearing().getSittingDay(), true)
                ),
                when(RelatedHearingRequested.class).apply(e ->
                        this.hasNextHearingForHearingDay.put(e.getSeedingHearing().getSittingDay(), true)
                ),
                when(UnscheduledNextHearingsRequested.class).apply(
                        e -> this.hasNextHearingForHearingDay.put(e.getSeedingHearing().getSittingDay(), true)
                ),
                when(DeleteNextHearingsRequested.class).apply(e ->
                        this.hasNextHearingForHearingDay.put(e.getSeedingHearing().getSittingDay(), false)
                ),
                when(RelatedHearingUpdated.class).apply(e ->
                        this.relatedHearingUpdatedMap.put(e.getSeedingHearing().getSeedingHearingId(), e)
                ),
                when(CasesAddedForUpdatedRelatedHearing.class).apply(e ->
                        this.relatedHearingUpdatedMap.remove(e.getSeedingHearing().getSeedingHearingId())
                ),
                when(OffencesRemovedFromHearing.class).apply(this::onOffencesRemovedFromHearing),
                when(BookingReferencesAndCourtScheduleIdsStored.class).apply(e ->
                        this.bookingReferencesAndCourtScheduleIdsForHearingDay.put(e.getHearingDay(), e.getBookingReferenceCourtScheduleIds())
                ),
                otherwiseDoNothing());
    }

    @SuppressWarnings("pmd:NullAssignment")
    public Stream<Object> createSummonsData(final CourtCentre courtCentre, final ZonedDateTime hearingDateTime, final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds, final List<UUID> confirmedApplicationIds) {
        if (isNotEmpty(listDefendantRequests) || isNotEmpty(applicationListingNeeds)) {

            final List<ListDefendantRequest> listDefendantRequestsToSend = isNotEmpty(this.listDefendantRequests) ? this.listDefendantRequests : null;
            final List<UUID> confirmedApplicationIdsToSend = isNotEmpty(confirmedApplicationIds) ? confirmedApplicationIds : null;
            final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsToSend = isNotEmpty(applicationListingNeeds) ? applicationListingNeeds : null;
            final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIdsToSend = isNotEmpty(confirmedProsecutionCaseIds) ? confirmedProsecutionCaseIds : null;

            return apply(Stream.of(summonsDataPrepared()
                    .withSummonsData(
                            summonsData()
                                    .withHearingDateTime(hearingDateTime)
                                    .withCourtCentre(courtCentre)
                                    .withConfirmedProsecutionCaseIds(confirmedProsecutionCaseIdsToSend)
                                    .withListDefendantRequests(listDefendantRequestsToSend) // defensive - if collection, min size is 1
                                    .withConfirmedApplicationIds(confirmedApplicationIdsToSend)
                                    .withCourtApplicationPartyListingNeeds(courtApplicationPartyListingNeedsToSend) // defensive - if collection, min size is 1
                                    .build()
                    )
                    .build()));
        }
        return null;
    }

    @SuppressWarnings("pmd:NullAssignment")
    public Stream<Object> enrichInitiateHearing(final Hearing hearing) {
        if (!listDefendantRequests.isEmpty()) {
            final Hearing.Builder hearingBuilder = hearing();

            if (isNotEmpty(hearing.getProsecutionCases())) {
                final List<UUID> defendantIds = hearing.getProsecutionCases().stream()
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .map(Defendant::getId)
                        .collect(toList());

                final List<ReferralReason> referralReasons = listDefendantRequests.stream()
                        .filter(listDefendantRequest -> {
                            if (nonNull(listDefendantRequest.getReferralReason())) {
                                return defendantIds.contains(listDefendantRequest.getReferralReason().getDefendantId());
                            } else {
                                return false;
                            }
                        })
                        .map(ListDefendantRequest::getReferralReason)
                        .collect(toList());

                hearingBuilder.withDefendantReferralReasons(isNotEmpty(referralReasons) ? referralReasons : null);
            }

            final Hearing enrichedHearing = hearingBuilder
                    .withCourtCentre(hearing.getCourtCentre())
                    .withDefenceCounsels(hearing.getDefenceCounsels())
                    .withDefendantAttendance(hearing.getDefendantAttendance())
                    .withHasSharedResults(hearing.getHasSharedResults())
                    .withHearingCaseNotes(hearing.getHearingCaseNotes())
                    .withHearingDays(hearing.getHearingDays())
                    .withHearingLanguage(hearing.getHearingLanguage())
                    .withId(hearing.getId())
                    .withJudiciary(hearing.getJudiciary())
                    .withJurisdictionType(hearing.getJurisdictionType())
                    .withProsecutionCases(hearing.getProsecutionCases())
                    .withCourtApplications(hearing.getCourtApplications())
                    .withProsecutionCounsels(hearing.getProsecutionCounsels())
                    .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                    .withType(hearing.getType())
                    .build();

            return apply(Stream.of(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(enrichedHearing).build()));
        }

        return apply(Stream.of(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build()));
    }

    public Stream<Object> updateDefendantListingStatus(final Hearing hearing, final HearingListingStatus hearingListingStatus, final Boolean notifyNCES) {
        LOGGER.info("Hearing with id {} and the status: {} notifyNCES: {}", hearing.getId(), hearingListingStatus, notifyNCES);
        final ProsecutionCaseDefendantListingStatusChanged.Builder prosecutionCaseDefendantListingStatusChanged = ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged();
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (hearingListingStatus == HearingListingStatus.HEARING_INITIALISED && Boolean.TRUE.equals(this.notifyNCES)) {

            final UnscheduledHearingAllocationNotified unscheduledHearingAllocationNotified = UnscheduledHearingAllocationNotified.unscheduledHearingAllocationNotified()
                    .withHearing(hearing)
                    .build();

            streamBuilder.add(unscheduledHearingAllocationNotified);
        }

        prosecutionCaseDefendantListingStatusChanged.withNotifyNCES(notifyNCES);

        if (HearingListingStatus.HEARING_RESULTED == this.hearingListingStatus) {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(HearingListingStatus.HEARING_RESULTED);
        } else {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(hearingListingStatus);
        }

        prosecutionCaseDefendantListingStatusChanged.withHearing(hearing);

        streamBuilder.add(prosecutionCaseDefendantListingStatusChanged.build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> boxworkComplete() {
        LOGGER.debug("Boxwork Complete when hearing resulted");
        return apply(empty());
    }

    public Stream<Object> updateDefendantHearingResult(final UUID hearingId, final List<SharedResultLine> sharedResultLines) {
        LOGGER.debug("Defendant hearing result updated.");

        return apply(Stream.of(ProsecutionCaseDefendantHearingResultUpdated.prosecutionCaseDefendantHearingResultUpdated()
                .withHearingId(hearingId)
                .withSharedResultLines(sharedResultLines)
                .build()));
    }

    public Stream<Object> createHearingDefendantRequest(final List<ListDefendantRequest> listDefendantRequests) {
        LOGGER.debug("List Defendant Request is being created.");
        return apply(Stream.of(hearingDefendantRequestCreated().withDefendantRequests(listDefendantRequests).build()));
    }

    public Stream<Object> createHearingApplicationRequest(final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
        LOGGER.debug("Application summons request is being created.");
        return apply(Stream.of(hearingApplicationRequestCreated().withApplicationRequests(courtApplicationPartyListingNeeds).build()));
    }

    public Stream<Object> addBreachApplication(final AddBreachApplication addBreachApplication) {
        LOGGER.debug("Breach application creation is being requested.");

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        addBreachApplication.getBreachedApplications()
                .stream()
                .map(breachedApplication -> BreachApplicationCreationRequested.breachApplicationCreationRequested()
                        .withBreachedApplications(breachedApplication)
                        .withMasterDefendantId(addBreachApplication.getMasterDefendantId())
                        .withHearingId(addBreachApplication.getHearingId())
                        .build())
                .forEach(streamBuilder::add);

        return apply(streamBuilder.build());
    }

    /**
     * This command was used to share results before implementing amendReshare feature (GPE-15210)
     * and multiday share (DD-3429). As part of amendReshare feature (GPE-15210) and multiday share
     * (DD-3429) feature this command call is being replaced by {@link
     * HearingAggregate#processHearingResults}
     *
     * @param hearing
     * @param sharedTime
     * @param shadowListedOffences
     * @return
     */
    public Stream<Object> saveHearingResult(final Hearing hearing, final ZonedDateTime sharedTime, final List<UUID> shadowListedOffences) {
        LOGGER.debug("Hearing Resulted.");
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final List<ProsecutionCase> updatedProsecutionCases = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).map(this::getUpdatedProsecutionCase).collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        final Hearing updatedHearing = hearing()
                .withProsecutionCases(updatedProsecutionCases)
                .withDefendantJudicialResults(hearing.getDefendantJudicialResults())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withId(hearing.getId())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withType(hearing.getType())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withCourtApplications(hearing.getCourtApplications())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withJudiciary(hearing.getJudiciary())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(hearing.getCompanyRepresentatives())
                .withIntermediaries(hearing.getIntermediaries())
                .withIsEffectiveTrial(hearing.getIsEffectiveTrial())
                .withYouthCourtDefendantIds(hearing.getYouthCourtDefendantIds())
                .withYouthCourt(hearing.getYouthCourt())
                .build();

        streamBuilder.add(HearingResulted.hearingResulted()
                .withHearing(updatedHearing)
                .withSharedTime(sharedTime)
                .build());

        streamBuilder.add(prosecutionCaseDefendantListingStatusChanged()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());

        if (isNotEmpty(hearing.getProsecutionCases())) {
            streamBuilder.add(prosecutionCasesResulted()
                    .withHearing(hearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        if (isNotEmpty(hearing.getCourtApplications())) {
            streamBuilder.add(applicationsResulted()
                    .withHearing(hearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public ProsecutionCaseDefendantListingStatusChanged getSavedListingStatusChanged() {
        return new ProsecutionCaseDefendantListingStatusChanged(hearing, hearingListingStatus, notifyNCES);
    }

    public Stream<Object> updateListDefendantRequest(final List<ListDefendantRequest> listDefendantRequests, ConfirmedHearing confirmedHearing) {
        if (isEmpty(listDefendantRequests)) {
            return Stream.empty();
        }

        return apply(Stream.of(extendHearingDefendantRequestUpdated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    public Stream<Object> createListDefendantRequest(final ConfirmedHearing confirmedHearing) {
        if (isEmpty(listDefendantRequests)) {
            return Stream.empty();
        }

        return apply(Stream.of(extendHearingDefendantRequestCreated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    public Stream<Object> assignDefendantRequestFromCurrentHearingToExtendHearing(final UUID currentHearingId, final UUID extendHearingId) {
        if (isEmpty(listDefendantRequests)) {
            return Stream.empty();
        }
        return apply(Stream.of(DefendantRequestFromCurrentHearingToExtendHearingCreated.defendantRequestFromCurrentHearingToExtendHearingCreated()
                .withCurrentHearingId(currentHearingId)
                .withExtendHearingId(extendHearingId)
                .withDefendantRequests(listDefendantRequests)
                .build()));
    }

    public Stream<Object> assignDefendantRequestToExtendHearing(final UUID hearingId, final List<ListDefendantRequest> defendantRequests) {
        return apply(Stream.of(DefendantRequestToExtendHearingCreated.defendantRequestToExtendHearingCreated()
                .withHearingId(hearingId)
                .withDefendantRequests(defendantRequests)
                .build()));
    }

    public Stream<Object> listUnscheduledHearing(final Hearing hearing) {
        if (Boolean.TRUE.equals(this.unscheduledHearingListedFromThisHearing)) {
            LOGGER.info("Unscheduled hearing has been listing from this hearing with id {}", hearing.getId());
            return empty();
        }

        return apply(Stream.of(UnscheduledHearingListingRequested
                .unscheduledHearingListingRequested()
                .withHearing(hearing)
                .build()));
    }

    public Stream<Object> recordUnscheduledHearing(final UUID hearingId, final List<UUID> uuidList) {

        return apply(Stream.of(UnscheduledHearingRecorded
                .unscheduledHearingRecorded()
                .withHearingId(hearingId)
                .withUnscheduledHearingIds(uuidList)
                .build()));
    }

    public Stream<Object> markAsDuplicate(final UUID hearingId, final List<UUID> prosecutionCaseIds, final List<UUID> defendantIds) {
        if (this.duplicate) {
            return empty();
        }

        return apply(Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .withCaseIds(prosecutionCaseIds)
                .withDefendantIds(defendantIds)
                .build()));
    }

    private ProsecutionCase getUpdatedProsecutionCase(ProsecutionCase prosecutionCase) {
        final List<Defendant> updatedDefendants = new ArrayList<>();

        final boolean allDefendantProceedingConcluded = isAllDefendantProceedingConcluded(prosecutionCase, updatedDefendants);
        return ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withId(prosecutionCase.getId())
                .withDefendants(updatedDefendants)
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withCaseStatus(allDefendantProceedingConcluded ? CaseStatusEnum.INACTIVE.getDescription() : prosecutionCase.getCaseStatus())
                .build();
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    public Stream<Object> deleteHearing(final UUID hearingId) {

        if (this.deleted) {
            return empty();
        }

        final List<UUID> prosecutionCaseIds = isNotEmpty(hearing.getProsecutionCases()) ? getProsecutionCaseIds(hearing) : null;
        final List<UUID> courtApplicationIds = isNotEmpty(hearing.getCourtApplications()) ? getCourtApplicationIds(hearing) : null;

        return apply(Stream.of(HearingDeleted.hearingDeleted()
                .withHearingId(hearingId)
                .withCourtApplicationIds(courtApplicationIds)
                .withProsecutionCaseIds(prosecutionCaseIds)
                .build()));

    }

    public Stream<Object> removeOffenceFromHearing(final UUID hearingId, final List<UUID> offencesToBeRemoved) {

        if (this.deleted) {
            return empty();
        }

        final List<UUID> defendantsToBeRemoved = getDefendantsToBeRemoved(offencesToBeRemoved);
        final List<UUID> prosecutionCasesToBeRemoved = getProsecutionCasesToBeRemoved(defendantsToBeRemoved);

        return apply(Stream.of(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withOffenceIds(offencesToBeRemoved)
                .withDefendantIds(defendantsToBeRemoved)
                .withProsecutionCaseIds(prosecutionCasesToBeRemoved)
                .build()));
    }

    private List<UUID> getProsecutionCasesToBeRemoved(final List<UUID> defendantsToBeRemoved) {
        return hearing.getProsecutionCases()
                .stream()
                .filter(prosecutionCase -> prosecutionCase
                        .getDefendants()
                        .stream().filter(defendant -> !defendantsToBeRemoved.contains(defendant.getId()))
                        .collect(toList()).isEmpty())
                .map(ProsecutionCase::getId)
                .collect(toList());
    }

    private List<UUID> getDefendantsToBeRemoved(final List<UUID> offencesToBeRemoved) {

        return hearing.getProsecutionCases()
                .stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> defendant
                        .getOffences()
                        .stream()
                        .filter(offence -> !offencesToBeRemoved.contains(offence.getId()))
                        .collect(toList()).isEmpty())
                .map(Defendant::getId)
                .collect(toList());
    }

    private void onHearingDaysWithoutCourtCentreCorrected(final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected) {
        if (CollectionUtils.isNotEmpty(hearingDaysWithoutCourtCentreCorrected.getHearingDays())) {
            final UUID courtCentreId = hearingDaysWithoutCourtCentreCorrected.getHearingDays().get(0).getCourtCentreId();
            final UUID courtRoomId = hearingDaysWithoutCourtCentreCorrected.getHearingDays().get(0).getCourtRoomId();
            final List<HearingDay> hearingDaysToBeReplaced = createHearingDaysToBeReplaced(courtCentreId, courtRoomId);
            hearing.getHearingDays().clear();
            hearing.getHearingDays().addAll(hearingDaysToBeReplaced);
        } else {
            LOGGER.info("hearingDays send for hearingDaysWithoutCourtCentreCorrected is empty with this hearing id {}", hearingDaysWithoutCourtCentreCorrected.getId());
        }
    }

    private void onOffencesRemovedFromHearing(final OffencesRemovedFromHearing offencesRemovedFromHearing) {

        final List<UUID> offencesToBeRemoved = offencesRemovedFromHearing.getOffenceIds();
        final List<UUID> defendantsToBeRemoved = offencesRemovedFromHearing.getDefendantIds();
        final List<UUID> prosecutionCasesToBeRemoved = offencesRemovedFromHearing.getProsecutionCaseIds();

        removeProsecutionCasesDefendantsAndOffencesFromHearing(offencesToBeRemoved, defendantsToBeRemoved, prosecutionCasesToBeRemoved);
    }

    private void removeProsecutionCasesDefendantsAndOffencesFromHearing(final List<UUID> offencesToBeRemoved, final List<UUID> defendantsToBeRemoved,
                                                                        final List<UUID> prosecutionCasesToBeRemoved) {
        // Remove offences
        hearing.getProsecutionCases().forEach(
                prosecutionCase -> prosecutionCase.getDefendants().forEach(
                        defendant -> defendant.getOffences()
                                .removeIf(offence -> offencesToBeRemoved.contains(offence.getId()))
                )
        );

        // Remove defendants with no offences from all prosecution cases
        hearing.getProsecutionCases().forEach(
                prosecutionCase -> prosecutionCase.getDefendants()
                        .removeIf(defendant -> defendantsToBeRemoved.contains(defendant.getId()))
        );

        // Remove prosecution cases with no defendants
        hearing.getProsecutionCases()
                .removeIf(prosecutionCase -> prosecutionCasesToBeRemoved.contains(prosecutionCase.getId()));

    }

    private List<HearingDay> createHearingDaysToBeReplaced(final UUID courtCentreId, final UUID courtRoomId) {
        final List<HearingDay> hearingDayListToBeReplaced = new ArrayList<>();
        hearing.getHearingDays().forEach(hearingDay ->
                hearingDayListToBeReplaced.add(HearingDay.hearingDay()
                        .withValuesFrom(hearingDay)
                        .withCourtCentreId(courtCentreId)
                        .withCourtRoomId(courtRoomId).build())
        );
        return hearingDayListToBeReplaced;
    }

    private CommittingCourt findCommittingCourt(final Hearing hearing) {
        if (nonNull(hearing) && MAGISTRATES.equals(hearing.getJurisdictionType()) && nonNull(hearing.getProsecutionCases())) {
            final Optional<Offence> offence = hearing.getProsecutionCases().stream().flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(o -> nonNull(o.getCommittingCourt()))
                    .findFirst();
            if (offence.isPresent()) {
                return offence.get().getCommittingCourt();
            }
        }

        return this.committingCourt;
    }

    public Stream<Object> processHearingResults(final Hearing hearing, final ZonedDateTime sharedTime, final List<UUID> shadowListedOffences, final LocalDate hearingDay) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(createHearingResultedEvent(hearing, sharedTime, hearingDay));
        streamBuilder.add(createListingStatusResultedEvent(hearing));

        if (isNotEmpty(hearing.getProsecutionCases())) {
            streamBuilder.add(createProsecutionCasesResultedV2Event(hearing, shadowListedOffences, hearingDay));
        }

        if (isEligibleForNextHearings(hearing)) {
            final List<Object> nextHearingEvents = createNextHearingEvents(hearing, shadowListedOffences, hearingDay);
            nextHearingEvents.forEach(streamBuilder::add);
        }

        // DD-8648: Application Events - See uk.gov.moj.cpp.progression.event.HearingResultEventProcessor.resultApplications

        if (isNotEmpty(hearing.getCourtApplications())) {
            streamBuilder.add(applicationsResulted()
                    .withHearing(hearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateRelatedHearing(final HearingListingNeeds hearingListingNeeds,
                                               final Boolean isAdjourned,
                                               final UUID extendHearingFrom,
                                               final Boolean isPartiallyAllocated,
                                               final SeedingHearing seedingHearing,
                                               final List<UUID> shadowListedOffences) {
        return apply(Stream.of(
                RelatedHearingUpdated.relatedHearingUpdated()
                        .withExtendedHearingFrom(extendHearingFrom)
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(isAdjourned)
                        .withIsPartiallyAllocated(isPartiallyAllocated)
                        .withSeedingHearing(seedingHearing)
                        .withShadowListedOffences(shadowListedOffences)
                        .build()));
    }

    public Stream<Object> addCasesForUpdatedRelatedHearing(final UUID seedingHearingId) {
        if (relatedHearingUpdatedMap.containsKey(seedingHearingId)) {
            final RelatedHearingUpdated relatedHearingUpdated = relatedHearingUpdatedMap.get(seedingHearingId);
            return apply(Stream.of(CasesAddedForUpdatedRelatedHearing.casesAddedForUpdatedRelatedHearing()
                    .withExtendedHearingFrom(relatedHearingUpdated.getExtendedHearingFrom())
                    .withHearingRequest(relatedHearingUpdated.getHearingRequest())
                    .withIsAdjourned(relatedHearingUpdated.getIsAdjourned())
                    .withIsPartiallyAllocated(relatedHearingUpdated.getIsPartiallyAllocated())
                    .withSeedingHearing(relatedHearingUpdated.getSeedingHearing())
                    .withShadowListedOffences(relatedHearingUpdated.getShadowListedOffences())
                    .build()));
        }
        return apply(empty());
    }

    public Stream<Object> storeBookingReferencesWithCourtScheduleIds(final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds, final LocalDate hearingDay) {

        return apply(Stream.of(
                BookingReferencesAndCourtScheduleIdsStored.bookingReferencesAndCourtScheduleIdsStored()
                        .withHearingDay(hearingDay)
                        .withBookingReferenceCourtScheduleIds(bookingReferenceCourtScheduleIds)
                        .build()));

    }

    private ProsecutionCasesResultedV2 createProsecutionCasesResultedV2Event(final Hearing hearing, final List<UUID> shadowListedOffences, final LocalDate hearingDay) {
        return ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withHearing(hearing)
                .withHearingDay(hearingDay)
                .build();
    }

    private ProsecutionCaseDefendantListingStatusChanged createListingStatusResultedEvent(final Hearing hearing) {
        return new ProsecutionCaseDefendantListingStatusChanged(hearing, HearingListingStatus.HEARING_RESULTED, notifyNCES);
    }

    private HearingResulted createHearingResultedEvent(final Hearing hearing, final ZonedDateTime sharedTime, final LocalDate hearingDay) {
        final Hearing.Builder updatedHearingBuilder = Hearing.hearing();
        final List<ProsecutionCase> updatedProsecutionCases = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).map(this::getUpdatedProsecutionCase).collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        updatedHearingBuilder
                .withProsecutionCases(updatedProsecutionCases)
                .withDefendantJudicialResults(hearing.getDefendantJudicialResults())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withId(hearing.getId())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withType(hearing.getType())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withCourtApplications(hearing.getCourtApplications())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withJudiciary(hearing.getJudiciary())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(hearing.getCompanyRepresentatives())
                .withIntermediaries(hearing.getIntermediaries())
                .withIsEffectiveTrial(hearing.getIsEffectiveTrial())
                .withYouthCourtDefendantIds(hearing.getYouthCourtDefendantIds())
                .withYouthCourt(hearing.getYouthCourt());

        return HearingResulted.hearingResulted()
                .withHearing(updatedHearingBuilder.build())
                .withHearingDay(hearingDay)
                .withSharedTime(sharedTime).build();

    }

    private List<Object> createNextHearingEvents(final Hearing hearing, final List<UUID> shadowListedOffences, final LocalDate hearingDay) {

        final List<Object> events = new ArrayList<>();

        final String sittingDay = hearingDay.toString();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withSittingDay(sittingDay)
                .build();

        if (hasNextHearingForHearingDay.getOrDefault(sittingDay, false)) {
            events.add(DeleteNextHearingsRequested.deleteNextHearingsRequested()
                    .withHearingId(hearing.getId())
                    .withSeedingHearing(seedingHearing)
                    .build());
        }

        final boolean hasNewNextHearingsOrRelatedNextHearings = doHearingContainNextHearingResults(hearing);

        if (hasNewNextHearingsOrRelatedNextHearings) {
            final boolean shouldPopulateCommittingCourt = checkResultLinesForCommittingCourt(hearing);
            final CommittingCourt court = committingCourt == null ? findCommittingCourt(hearing) : committingCourt;

            final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = bookingReferencesAndCourtScheduleIdsForHearingDay.getOrDefault(hearingDay, emptyList());
            if (hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing(hearing)) {
                events.add(NextHearingsRequested.nextHearingsRequested()
                        .withHearing(hearing)
                        .withSeedingHearing(seedingHearing)
                        .withShadowListedOffences(shadowListedOffences)
                        .withCommittingCourt(shouldPopulateCommittingCourt ? court : null)
                        .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                        .build());
            }

            if (hasHearingContainsRelatedNextHearings(hearing)) {
                final NextHearingDetails nextHearingDetails = createRelatedHearings(hearing, shouldPopulateCommittingCourt, ofNullable(court), seedingHearing);
                nextHearingDetails.getHearingListingNeedsList().forEach(hearingListingNeeds ->
                        events.add(RelatedHearingRequested.relatedHearingRequested()
                                .withHearingRequest(hearingListingNeeds)
                                .withIsAdjourned(true)
                                .withSeedingHearing(seedingHearing)
                                .withShadowListedOffences(shadowListedOffences)
                                .build()));
            }
        }

        if (unscheduledNextHearingsRequiredFor(hearing)) {
            events.add(UnscheduledNextHearingsRequested.unscheduledNextHearingsRequested()
                    .withSeedingHearing(seedingHearing)
                    .withHearing(hearing)
                    .build());
        }

        return events;
    }

    private List<UUID> getProsecutionCaseIds(final Hearing hearing) {
        return hearing
                .getProsecutionCases()
                .stream()
                .map(ProsecutionCase::getId)
                .collect(toList());
    }

    private List<UUID> getCourtApplicationIds(final Hearing hearing) {
        return hearing
                .getCourtApplications()
                .stream()
                .map(CourtApplication::getId)
                .collect(toList());
    }
}
