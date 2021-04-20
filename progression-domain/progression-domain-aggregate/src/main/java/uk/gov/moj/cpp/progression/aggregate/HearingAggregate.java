package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
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
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.progression.courts.ApplicationsResulted.applicationsResulted;
import static uk.gov.justice.progression.courts.ProsecutionCasesResulted.prosecutionCasesResulted;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isAllDefendantProceedingConcluded;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
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
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.core.courts.UnscheduledHearingRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.UnscheduledHearingAllocationNotified;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private static final long serialVersionUID = -4848796143895994633L;
    private final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
    private final List<CourtApplicationPartyListingNeeds> applicationListingNeeds = new ArrayList<>();
    private Hearing hearing;
    private HearingListingStatus hearingListingStatus;
    private Boolean unscheduledHearingListedFromThisHearing;
    private boolean duplicate;
    private CommittingCourt committingCourt;
    private Boolean notifyNCES = false;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ProsecutionCaseDefendantListingStatusChanged.class).apply(e -> {
                    this.hearing = e.getHearing();
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                    this.notifyNCES = nonNull(e.getNotifyNCES()) ? e.getNotifyNCES(): Boolean.FALSE;
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

            if(isNotEmpty(hearing.getProsecutionCases())) {
                final List<UUID> defendantIds = hearing.getProsecutionCases().stream()
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .map(Defendant::getId)
                        .collect(Collectors.toList());

                final List<ReferralReason> referralReasons = listDefendantRequests.stream()
                        .filter(listDefendantRequest -> {
                            if (nonNull(listDefendantRequest.getReferralReason())) {
                                return defendantIds.contains(listDefendantRequest.getReferralReason().getDefendantId());
                            } else {
                                return false;
                            }
                        })
                        .map(ListDefendantRequest::getReferralReason)
                        .collect(Collectors.toList());

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

        if (hearingListingStatus == HearingListingStatus.HEARING_INITIALISED && Boolean.TRUE.equals(this.notifyNCES)){

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

    public Stream<Object> saveHearingResult(final Hearing hearing, final ZonedDateTime sharedTime,  List<UUID> shadowListedOffences) {
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

        streamBuilder.add(hearingResulted().withHearing(updatedHearing).withSharedTime(sharedTime).build());

        streamBuilder.add(prosecutionCaseDefendantListingStatusChanged().withHearing(hearing).withHearingListingStatus(HearingListingStatus.HEARING_RESULTED).build());

        if (isNotEmpty(hearing.getProsecutionCases())) {
            streamBuilder.add(prosecutionCasesResulted()
                    .withHearing(hearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        if(isNotEmpty(hearing.getCourtApplications())) {
            streamBuilder.add(applicationsResulted()
                    .withHearing(hearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public ProsecutionCaseDefendantListingStatusChanged getSavedListingStatusChanged() {
        return new ProsecutionCaseDefendantListingStatusChanged(hearing, hearingListingStatus, false);
    }

    public Stream<Object> updateListDefendantRequest(final List<ListDefendantRequest> listDefendantRequests, ConfirmedHearing confirmedHearing) {
        if (isEmpty(listDefendantRequests)){
            return Stream.empty();
        }

        return apply(Stream.of(extendHearingDefendantRequestUpdated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    public Stream<Object> createListDefendantRequest(final ConfirmedHearing confirmedHearing) {
        if (isEmpty(listDefendantRequests)){
            return Stream.empty();
        }

        return apply(Stream.of(extendHearingDefendantRequestCreated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    public Stream<Object> assignDefendantRequestFromCurrentHearingToExtendHearing(final UUID currentHearingId, final UUID extendHearingId) {
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
}
