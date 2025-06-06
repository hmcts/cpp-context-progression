package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
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
import static uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2;
import static uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3.prosecutionCaseDefendantListingStatusChangedV3;
import static uk.gov.justice.core.courts.ProsecutionCasesResulted.prosecutionCasesResulted;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.progression.courts.ApplicationsResulted.applicationsResulted;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isAllDefendantProceedingConcluded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DuplicateApplicationsHelper.deDupAllApplications;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingHelper.isEligibleForNextHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.checkResultLinesForCommittingCourt;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.createRelatedHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.hasHearingContainsRelatedNextHearings;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.isNextHearingDeleted;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingResultHelper.unscheduledNextHearingsRequiredFor;
import static uk.gov.moj.cpp.progression.util.CaseHelper.addCaseToHearing;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.*;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreated;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreatedV2;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.BookingReferencesAndCourtScheduleIdsStored;
import uk.gov.justice.progression.courts.CaseAddedToHearingBdf;
import uk.gov.justice.progression.courts.CustodyTimeLimitClockStopped;
import uk.gov.justice.progression.courts.DeleteNextHearingsRequested;
import uk.gov.justice.progression.courts.DeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.ExtendCustodyTimeLimitResulted;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.progression.courts.OffenceInHearingDeleted;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.RelatedHearingUpdatedForAdhocHearing;
import uk.gov.justice.progression.courts.UnscheduledHearingAllocationNotified;
import uk.gov.justice.progression.courts.VejDeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.progression.event.OpaPressListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPressListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPressListNoticeSent;
import uk.gov.justice.progression.event.OpaPublicListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPublicListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPublicListNoticeSent;
import uk.gov.justice.progression.event.OpaResultListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaResultListNoticeGenerated;
import uk.gov.justice.progression.event.OpaResultListNoticeSent;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.staginghmi.courts.UpdateHearingFromHmi;
import uk.gov.moj.cpp.progression.court.HearingAddMissingResultsBdf;
import uk.gov.moj.cpp.progression.court.HearingResultedBdf;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.NextHearingDetails;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.OpaNoticeHelper;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaNoticeDocument;
import uk.gov.moj.cpp.progression.util.CaseHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1948", "squid:S1172", "squid:S1188", "squid:S3655"})
public class HearingAggregate implements Aggregate {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAggregate.class);
    private static final long serialVersionUID = 8888819367477517205L;
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
    private Map<UUID,UUID> initiatedApplicationIdsForResultIds = new HashMap<>();
    private Map<UUID,LocalDate> initiatedApplicationsIssueDateForResultIds = new HashMap<>();
    private final List<AddedOffencesMovedToHearing> addedOffencesMovedToHearings = new ArrayList<>();
    private Boolean isHearingInitiateEnriched = false;
    private List<ProsecutionCase> seededProsecutionCases = null;

    private ZonedDateTime resultSharedDateTime;

    //allocationId -> OpaNoticeType -> NoticeSentDate
    private final Map<UUID, Set<LocalDate>> opaPublicListNoticesSent = new HashMap<>();
    private final Map<UUID, Set<LocalDate>> opaPressListNoticesSent = new HashMap<>();
    private final Map<UUID, Set<LocalDate>> opaResultListNoticesSent = new HashMap<>();

    private final Map<UUID, Map<UUID, List<UUID>>> allDocumentsSharedWithUserGroup = new HashMap<>();
    private final Map<UUID, Map<UUID, List<UUID>>> allDocumentsSharedWithUser = new HashMap<>();

    // we need to know if offence was added hearing because the offence was added to case of the hearing.
    // The offence was not resulted from another hearing or not extended from another hearing.
    private final Set<UUID> newOffences = new HashSet<>();

    @VisibleForTesting
    public Hearing getHearing() {
        return this.hearing;
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingInitiateEnriched.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;
                    isHearingInitiateEnriched = true;
                }),
                when(HearingForApplicationCreated.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                }),
                when(HearingForApplicationCreatedV2.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                }),

                when(HearingVerdictUpdated.class).apply(e -> {
                    updateVerdict(e);
                }),
                when(ProsecutionCaseDefendantListingStatusChanged.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                    this.notifyNCES = nonNull(e.getNotifyNCES()) ? e.getNotifyNCES() : Boolean.FALSE;
                }),
                when(ProsecutionCaseDefendantListingStatusChangedV2.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                    this.notifyNCES = nonNull(e.getNotifyNCES()) ? e.getNotifyNCES() : Boolean.FALSE;
                }),
                when(ProsecutionCaseDefendantListingStatusChangedV3.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.hearingListingStatus = e.getHearingListingStatus();
                    this.notifyNCES = nonNull(e.getNotifyNCES()) ? e.getNotifyNCES() : Boolean.FALSE;
                }),
                when(HearingResulted.class).apply(e -> {
                    setHearing(e.getHearing());
                    this.committingCourt = findCommittingCourt(e.getHearing());
                    this.resultSharedDateTime = e.getSharedTime();
                    this.hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
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
                when(HearingTrialVacated.class).apply(this::updateVacateTrial),
                when(HearingMarkedAsDuplicate.class).apply(e ->
                        this.duplicate = true
                ),
                when(DefendantRequestToExtendHearingCreated.class).apply(e ->
                        listDefendantRequests.addAll(e.getDefendantRequests())
                ),
                when(HearingDefendantUpdated.class).apply(this::onHearingDefendantUpdated),
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
                when(CustodyTimeLimitClockStopped.class).apply(this::onCustodyTimeLimitClockStopped),
                when(ExtendCustodyTimeLimitResulted.class).apply(this::onExtendCustodyTimeLimitResulted),
                when(CaseMarkersUpdatedInHearing.class).apply(this::handleCaseMarkesUpdate),
                when(HearingUpdatedWithCourtApplication.class).apply(this::handleCourtApplicationUpdate),
                when(HearingUpdatedProcessed.class).apply(this::updateHearing),
                when(HearingOffencesUpdated.class).apply(this::updateOffenceInHearing),
                when(HearingOffencesUpdatedV2.class).apply(this::updateOffenceInHearingV2),
                when(HearingUpdatedForAllocationFields.class).apply(this::handleHearingUpdatedForAllocationFields),
                when(NewDefendantAddedToHearing.class).apply(this::addNewDefendant),
                when(HearingExtended.class).apply(e -> this.updateHearingExtended(e.getHearingRequest())),
                when(HearingExtendedProcessed.class).apply(e -> this.updateHearingExtended(e.getHearingRequest())),
                when(ListingNumberUpdated.class).apply(this::updateListingNumbers),
                when(HearingListingNumberUpdated.class).apply(this::updateListingNumbers),
                when(HearingMovedToUnallocated.class).apply(this::handleHearingMovedToUnallocated),
                when(OpaPublicListNoticeDeactivated.class).apply(this::deactivatePublicListNotice),
                when(OpaPressListNoticeDeactivated.class).apply(this::deactivatePressListNotice),
                when(OpaResultListNoticeDeactivated.class).apply(this::deactivateResultListNotice),
                when(OpaPublicListNoticeSent.class).apply(this::addPublicListNoticeSent),
                when(OpaPressListNoticeSent.class).apply(this::addPressListNoticeSent),
                when(OpaResultListNoticeSent.class).apply(this::addResultListNoticeSent),
                when(InitiateApplicationForCaseRequested.class).apply(this::handleInitiateApplicationForCaseRequested),
                when(CourtApplicationRemovedFromSeedingHearing.class).apply(this::handleCourtApplicationRemovedFromSeedingHearing),
                when(AddedOffencesMovedToHearing.class).apply(this::handleAddedOffencesMovedToHearing),
                when(AllCourtDocumentsShared.class).apply(this::updateAllCourtDocumentsShared),
                when(CaseAddedToHearingBdf.class).apply(this::handleCaseAddedToHearingBdf),
                otherwiseDoNothing());
    }

    private void handleAddedOffencesMovedToHearing(AddedOffencesMovedToHearing addedOffencesMovedToHearing) {
        if(isHearingInitiateEnriched){
            return;
        }
        if(! addedOffencesMovedToHearings.contains(addedOffencesMovedToHearing)) {
            addedOffencesMovedToHearings.add(addedOffencesMovedToHearing);
        }
    }

    private void handleHearingMovedToUnallocated(final HearingMovedToUnallocated hearingMovedToUnallocated) {
        setHearing(hearingMovedToUnallocated.getHearing());
        this.hearingListingStatus = HearingListingStatus.SENT_FOR_LISTING;
    }

    private void handleInitiateApplicationForCaseRequested(final InitiateApplicationForCaseRequested initiateApplicationForCaseRequested) {
        this.initiatedApplicationIdsForResultIds.put(initiateApplicationForCaseRequested.getApplicationId(), initiateApplicationForCaseRequested.getResultId());
        if (!initiatedApplicationsIssueDateForResultIds.containsKey(initiateApplicationForCaseRequested.getResultId())) {
            this.initiatedApplicationsIssueDateForResultIds.put(initiateApplicationForCaseRequested.getResultId(), initiateApplicationForCaseRequested.getIssueDate());
        }
    }

    private void handleCourtApplicationRemovedFromSeedingHearing(CourtApplicationRemovedFromSeedingHearing courtApplicationRemovedFromSeedingHearing) {
        this.initiatedApplicationIdsForResultIds.remove(courtApplicationRemovedFromSeedingHearing.getApplicationId());
    }

    private void updateAllCourtDocumentsShared(final AllCourtDocumentsShared allCourtDocumentsShared) {
        final UUID defendantId = allCourtDocumentsShared.getDefendantId();
        final UUID caseId = allCourtDocumentsShared.getCaseId();
        final UUID userGroupId = allCourtDocumentsShared.getUserGroupId();
        final UUID userId = allCourtDocumentsShared.getUserId();
        Map<UUID, List<UUID>> allDocumentsSharedCaseDefMap;
        if (nonNull(userGroupId)) {
            allDocumentsSharedCaseDefMap = allDocumentsSharedWithUserGroup.get(userGroupId);
        } else {
            allDocumentsSharedCaseDefMap = allDocumentsSharedWithUser.get(userId);
        }
        if (isNull(allDocumentsSharedCaseDefMap)) {
            allDocumentsSharedCaseDefMap = new HashMap<>();
        }

        final Map<UUID, List<UUID>> finalAllDocumentsSharedCaseDefMap = allDocumentsSharedCaseDefMap;
        if(isNull(finalAllDocumentsSharedCaseDefMap.get(caseId))){
            finalAllDocumentsSharedCaseDefMap.put(caseId, new ArrayList<>());
        }
        finalAllDocumentsSharedCaseDefMap.get(caseId).add(defendantId);

        ofNullable(userGroupId).ifPresentOrElse(
                value -> allDocumentsSharedWithUserGroup.put(userGroupId, finalAllDocumentsSharedCaseDefMap),
                () -> allDocumentsSharedWithUser.put(userId, finalAllDocumentsSharedCaseDefMap));
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

    public Stream<Object> extendHearing(final HearingListingNeeds hearingListingNeeds, final ExtendHearing extendHearing) {

        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withExtendedHearingFrom(extendHearing.getExtendedHearingFrom())
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(extendHearing.getIsAdjourned())
                        .withIsPartiallyAllocated(extendHearing.getIsPartiallyAllocated())
                        .withShadowListedOffences(extendHearing.getShadowListedOffences())
                        .withIsUnAllocatedHearing(extendHearing.getIsUnAllocatedHearing())
                        .build()));
    }

    public Stream<Object> extendHearing(final HearingListingNeeds hearingListingNeeds) {
        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(Boolean.FALSE)
                        .build()));
    }

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

                if(isNotEmpty(referralReasons)) {
                    hearingBuilder.withDefendantReferralReasons(referralReasons);
                }
            }

            final Hearing enrichedHearing = hearingBuilder
                    .withCourtCentre(hearing.getCourtCentre())
                    .withDefenceCounsels(hearing.getDefenceCounsels())
                    .withDefendantAttendance(hearing.getDefendantAttendance())
                    .withHasSharedResults(hearing.getHasSharedResults())
                    .withHearingCaseNotes(hearing.getHearingCaseNotes())
                    .withHearingDays(hearing.getHearingDays())
                    .withHearingLanguage(hearing.getHearingLanguage())
                    .withEstimatedDuration(hearing.getEstimatedDuration())
                    .withId(hearing.getId())
                    .withJudiciary(hearing.getJudiciary())
                    .withJurisdictionType(hearing.getJurisdictionType())
                    .withProsecutionCases(hearing.getProsecutionCases())
                    .withCourtApplications(hearing.getCourtApplications())
                    .withProsecutionCounsels(hearing.getProsecutionCounsels())
                    .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                    .withType(hearing.getType())
                    .withIsGroupProceedings(hearing.getIsGroupProceedings())
                    .withNumberOfGroupCases(hearing.getNumberOfGroupCases())
                    .build();

            addNewOffencesToHearing(enrichedHearing);
            return apply(Stream.of(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(enrichedHearing).build()));
        }


        final Hearing.Builder enrichedHearingBuilder = Hearing.hearing()
                .withValuesFrom(hearing);

        if (nonNull(this.hearing)) {
            enrichedHearingBuilder.withNumberOfGroupCases(this.hearing.getNumberOfGroupCases());
        }
        final Hearing enrichedHearing = enrichedHearingBuilder.build();
        addNewOffencesToHearing(enrichedHearing);
        return apply(Stream.of(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(enrichedHearing).build()));
    }

    public Stream<Object> createHearingForApplication(final Hearing hearing, final HearingListingStatus hearingListingStatus, final List<ListHearingRequest> listHearingRequests) {
        final HearingForApplicationCreatedV2.Builder hearingForApplicationCreated = HearingForApplicationCreatedV2.hearingForApplicationCreatedV2();
        LOGGER.info("Hearing with id {} and the status: {}", hearing.getId(), hearingListingStatus);

        hearingForApplicationCreated.withHearing(hearing)
                .withHearingListingStatus(hearingListingStatus);
        hearingForApplicationCreated.withListHearingRequests(listHearingRequests);

        return Stream.of(hearingForApplicationCreated.build());
    }

    public Stream<Object> updateDefendantListingStatus(final Hearing hearing, final HearingListingStatus hearingListingStatus, final Boolean notifyNCES, final List<ListHearingRequest> listHearingRequests) {
        final Hearing hearingWithOriginalListingNumbers = getHearingWithOriginalListingNumbers(hearing);
        LOGGER.info("Hearing with id {} and the status: {} notifyNCES: {}", hearingWithOriginalListingNumbers.getId(), hearingListingStatus, notifyNCES);
        final ProsecutionCaseDefendantListingStatusChangedV2.Builder prosecutionCaseDefendantListingStatusChanged = prosecutionCaseDefendantListingStatusChangedV2();
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (hearingListingStatus == HearingListingStatus.HEARING_INITIALISED && Boolean.TRUE.equals(this.notifyNCES)) {
            final UnscheduledHearingAllocationNotified unscheduledHearingAllocationNotified = UnscheduledHearingAllocationNotified.unscheduledHearingAllocationNotified()
                    .withHearing(hearingWithOriginalListingNumbers)
                    .build();

            streamBuilder.add(unscheduledHearingAllocationNotified);
        }

        prosecutionCaseDefendantListingStatusChanged.withNotifyNCES(notifyNCES);
        prosecutionCaseDefendantListingStatusChanged.withListHearingRequests(listHearingRequests);

        if (HearingListingStatus.HEARING_RESULTED == this.hearingListingStatus) {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(HearingListingStatus.HEARING_RESULTED);
        } else {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(hearingListingStatus);
        }

        prosecutionCaseDefendantListingStatusChanged.withHearing(hearingWithOriginalListingNumbers);

        streamBuilder.add(prosecutionCaseDefendantListingStatusChanged.build());
        final Stream<Object> events = apply(streamBuilder.build());
        return Stream.concat(Stream.concat(events, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }

    public Stream<Object> addDefenceCounselToHearing(final DefenceCounsel defenceCounsel) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final List<DefenceCounsel> newList = ofNullable(hearing.getDefenceCounsels()).map(Collection::stream).orElseGet(Stream::empty).collect(toList());
        newList.add(defenceCounsel);
        final Hearing hearingWithDefenceCounsel = Hearing.hearing().withValuesFrom(hearing)
                .withDefenceCounsels(newList.stream().distinct().collect(toList()))
                .build();
        streamBuilder.add(HearingDefenceCounselAdded.hearingDefenceCounselAdded().withHearingId(this.hearing.getId()).withDefenceCounsel(defenceCounsel).build());

        streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(this.hearingListingStatus)
                .withNotifyNCES(this.notifyNCES)
                .withHearing(hearingWithDefenceCounsel)
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateDefendantListingStatusV3(final Hearing hearing, final HearingListingStatus hearingListingStatus, final Boolean notifyNCES,
                                                         final ListNextHearingsV3 listNextHearings) {
        final Hearing hearingWithOriginalListingNumbers = getHearingWithOriginalListingNumbers(hearing);
        LOGGER.info("Hearing with id {} and the status: {} notifyNCES: {}", hearingWithOriginalListingNumbers.getId(), hearingListingStatus, notifyNCES);
        final ProsecutionCaseDefendantListingStatusChangedV3.Builder prosecutionCaseDefendantListingStatusChanged = prosecutionCaseDefendantListingStatusChangedV3();
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        prosecutionCaseDefendantListingStatusChanged.withNotifyNCES(notifyNCES);

        if (HearingListingStatus.HEARING_RESULTED == this.hearingListingStatus) {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(HearingListingStatus.HEARING_RESULTED);
        } else {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(hearingListingStatus);
        }

        prosecutionCaseDefendantListingStatusChanged.withHearing(hearingWithOriginalListingNumbers);

        prosecutionCaseDefendantListingStatusChanged.withListNextHearings(listNextHearings);

        streamBuilder.add(prosecutionCaseDefendantListingStatusChanged.build());
        final Stream<Object> events = apply(streamBuilder.build());
        return Stream.concat(Stream.concat(events, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }

    public Stream<Object> updateHearingWithDefenceCounsel(final DefenceCounsel defenceCounsel) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();


        final Hearing hearingWithDefenceCounsel = Hearing.hearing().withValuesFrom(hearing)
                .withDefenceCounsels(hearing.getDefenceCounsels().stream()
                        .map(defenceCounselFromAggregate -> defenceCounselFromAggregate.getId().equals(defenceCounsel.getId()) ? defenceCounsel : defenceCounselFromAggregate)
                        .collect(toList()))
                .build();
        streamBuilder.add(HearingDefenceCounselUpdated.hearingDefenceCounselUpdated().withHearingId(this.hearing.getId()).withDefenceCounsel(defenceCounsel).build());

        streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(this.hearingListingStatus)
                .withNotifyNCES(this.notifyNCES)
                .withHearing(hearingWithDefenceCounsel)
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> removeDefenceCounselFromHearing(final UUID defenceCounselId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Hearing hearingWithOutDefenceCounsel = Hearing.hearing().withValuesFrom(hearing)
                .withDefenceCounsels(hearing.getDefenceCounsels().stream()
                        .filter(defenceCounselFromAggregate -> !defenceCounselFromAggregate.getId().equals(defenceCounselId))
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
        streamBuilder.add(HearingDefenceCounselRemoved.hearingDefenceCounselRemoved().withHearingId(this.hearing.getId()).withId(defenceCounselId).build());

        streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(this.hearingListingStatus)
                .withNotifyNCES(this.notifyNCES)
                .withHearing(hearingWithOutDefenceCounsel)
                .build());
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
        final List<BreachedApplications> breachedApplications = addBreachApplication.getBreachedApplications().stream().map(HearingAggregate::getBreachedApplicationsWithId).collect(toList());
        final List<UUID> breachedApplicationIds = breachedApplications.stream().map(BreachedApplications::getId).collect(toList());

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        breachedApplications
                .stream()
                .map(breachedApplication -> BreachApplicationCreationRequested.breachApplicationCreationRequested()
                        .withBreachedApplications(breachedApplication)
                        .withMasterDefendantId(addBreachApplication.getMasterDefendantId())
                        .withHearingId(addBreachApplication.getHearingId())
                        .build())
                .forEach(streamBuilder::add);
        streamBuilder.add(new BreachApplicationsToBeAddedToHearing(breachedApplicationIds, addBreachApplication.getHearingId()));
        return apply(streamBuilder.build());
    }

    private static BreachedApplications getBreachedApplicationsWithId(final BreachedApplications ba) {
        return new BreachedApplications.Builder().withId(randomUUID())
                .withApplicationType(ba.getApplicationType())
                .withCourtOrder(ba.getCourtOrder())
                .build();
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

        final Hearing hearingWithOriginalListingNumber = getHearingWithOriginalListingNumbers(hearing);
        final List<ProsecutionCase> updatedProsecutionCasesForOriginalHearing = ofNullable(hearingWithOriginalListingNumber.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(this::updateCaseForAdjourn)
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        final List<ProsecutionCase> updatedProsecutionCases = ofNullable(updatedProsecutionCasesForOriginalHearing).map(Collection::stream).orElseGet(Stream::empty)
                .map(prosecutionCase -> getUpdatedProsecutionCase(prosecutionCase, hearingWithOriginalListingNumber.getDefendantJudicialResults()))
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        final List<CourtApplication> updatedCourtApplications = ofNullable(hearingWithOriginalListingNumber.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .map(this::updateApplicationWithAdjourn)
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        final Hearing updatedHearing = hearing()
                .withProsecutionCases(updatedProsecutionCases)
                .withDefendantJudicialResults(hearingWithOriginalListingNumber.getDefendantJudicialResults())
                .withIsBoxHearing(hearingWithOriginalListingNumber.getIsBoxHearing())
                .withId(hearingWithOriginalListingNumber.getId())
                .withHearingDays(hearingWithOriginalListingNumber.getHearingDays())
                .withCourtCentre(hearingWithOriginalListingNumber.getCourtCentre())
                .withJurisdictionType(hearingWithOriginalListingNumber.getJurisdictionType())
                .withType(hearingWithOriginalListingNumber.getType())
                .withHearingLanguage(hearingWithOriginalListingNumber.getHearingLanguage())
                .withCourtApplications(updatedCourtApplications)
                .withReportingRestrictionReason(hearingWithOriginalListingNumber.getReportingRestrictionReason())
                .withJudiciary(hearingWithOriginalListingNumber.getJudiciary())
                .withDefendantAttendance(hearingWithOriginalListingNumber.getDefendantAttendance())
                .withDefendantReferralReasons(hearingWithOriginalListingNumber.getDefendantReferralReasons())
                .withHasSharedResults(hearingWithOriginalListingNumber.getHasSharedResults())
                .withDefenceCounsels(hearingWithOriginalListingNumber.getDefenceCounsels())
                .withProsecutionCounsels(hearingWithOriginalListingNumber.getProsecutionCounsels())
                .withRespondentCounsels(hearingWithOriginalListingNumber.getRespondentCounsels())
                .withApplicationPartyCounsels(hearingWithOriginalListingNumber.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(hearingWithOriginalListingNumber.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(hearingWithOriginalListingNumber.getReportingRestrictionReason())
                .withHearingCaseNotes(hearingWithOriginalListingNumber.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(hearingWithOriginalListingNumber.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(hearingWithOriginalListingNumber.getCompanyRepresentatives())
                .withIntermediaries(hearingWithOriginalListingNumber.getIntermediaries())
                .withIsEffectiveTrial(hearingWithOriginalListingNumber.getIsEffectiveTrial())
                .withYouthCourtDefendantIds(hearingWithOriginalListingNumber.getYouthCourtDefendantIds())
                .withYouthCourt(hearingWithOriginalListingNumber.getYouthCourt())
                .withEstimatedDuration(hearingWithOriginalListingNumber.getEstimatedDuration())
                .build();

        streamBuilder.add(HearingResulted.hearingResulted()
                .withHearing(updatedHearing)
                .withSharedTime(sharedTime)
                .build());

        final Hearing originalHearing = hearing().withValuesFrom(hearingWithOriginalListingNumber).withProsecutionCases(updatedProsecutionCasesForOriginalHearing)
                .withCourtApplications(updatedCourtApplications).build();

        streamBuilder.add(prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(originalHearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());

        if (isNotEmpty(hearingWithOriginalListingNumber.getProsecutionCases())) {
            streamBuilder.add(prosecutionCasesResulted()
                    .withHearing(originalHearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        if (isNotEmpty(hearingWithOriginalListingNumber.getCourtApplications())) {
            streamBuilder.add(applicationsResulted()
                    .withHearing(originalHearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public ProsecutionCaseDefendantListingStatusChangedV2 getSavedListingStatusChanged() {
        return ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2().withHearing(hearing).withHearingListingStatus(HearingListingStatus.HEARING_RESULTED).withNotifyNCES(notifyNCES).build();
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

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (isNull(hearingListingStatus)) {

            final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChanged =
                    prosecutionCaseDefendantListingStatusChangedV2()
                            .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                            .withHearing(hearing)
                            .build();

            streamBuilder.add(prosecutionCaseDefendantListingStatusChanged);
        }

        streamBuilder.add(UnscheduledHearingListingRequested
                .unscheduledHearingListingRequested()
                .withHearing(hearing)
                .build());

        return apply(streamBuilder.build());
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
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .withCaseIds(prosecutionCaseIds)
                .withDefendantIds(defendantIds)
                .build());

        streamBuilder.add(DeletedHearingPopulatedToProbationCaseworker.deletedHearingPopulatedToProbationCaseworker()
                .withHearing(filterHearingForProbationCaseWorker())
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> processHearingUpdated(final ConfirmedHearing confirmedHearing, final Hearing updatedHearing) {
        final Hearing updatedHearingWithListingNumbers = getHearingWithOriginalListingNumbers(updatedHearing);

        return apply(Stream.of(HearingUpdatedProcessed.hearingUpdatedProcessed()
                .withHearing(updatedHearingWithListingNumbers)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    private Hearing getHearingWithOriginalListingNumbers(final Hearing hearing) {
        if (nonNull(this.hearing) && nonNull(this.hearing.getProsecutionCases())) {
            return hearing().withValuesFrom(hearing)
                    .withProsecutionCases(ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                            .map(this::getProsecutionCaseWithOriginalListingNumbers)
                            .collect(toList()))
                    .build();
        } else {
            return hearing;
        }
    }

    private ProsecutionCase getProsecutionCaseWithOriginalListingNumbers(final ProsecutionCase prosecutionCase) {
        final Map<UUID, Integer> listingMap = this.hearing.getProsecutionCases().stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .filter(offence -> nonNull(offence.getListingNumber()))
                .collect(Collectors.toMap(Offence::getId, Offence::getListingNumber, Math::max));

        return ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> Defendant.defendant().withValuesFrom(defendant).withMasterDefendantId(findMatchedMasterDefendantId(defendant))
                                .withOffences(ofNullable(defendant.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                        .map(offence -> Offence.offence().withValuesFrom(offence)
                                                .withListingNumber(ofNullable(listingMap.get(offence.getId())).orElse(offence.getListingNumber()))
                                                .build())
                                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                                .build())
                        .collect(toList()))
                .build();
    }

    @SuppressWarnings("squid:S1066")
    private UUID findMatchedMasterDefendantId(final Defendant defendant){
        if(null != this.hearing && null != this.hearing.getProsecutionCases()) {
            if(null == defendant.getMasterDefendantId() || defendant.getId().equals(defendant.getMasterDefendantId())) {
                final Optional<Defendant> matchedDefendant = this.hearing.getProsecutionCases().stream().flatMap(x -> x.getDefendants().stream()).filter(d -> d.getId().equals(defendant.getId())).findFirst();
                if (matchedDefendant.isPresent()) {
                   final  UUID matchedMasterDefendantId = matchedDefendant.get().getMasterDefendantId();
                     return  matchedMasterDefendantId == null ? defendant.getMasterDefendantId() : matchedMasterDefendantId;
                }
            }
        }
        return  defendant.getMasterDefendantId();
    }
    public Stream<Object> processHearingExtended(final HearingListingNeeds hearingRequest, final List<UUID> shadowListedOffences) {
        return apply(Stream.of(HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(hearingRequest)
                .withHearing(hearing)
                .withShadowListedOffences(shadowListedOffences)
                .build()
        ));
    }

    private void updateListingNumbers(final ListingNumberUpdated listingNumberUpdated) {
        final Map<UUID, Integer> listingMap = listingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber, Math::max));

        handleListingNumber(listingMap);
    }

    private void updateListingNumbers(final HearingListingNumberUpdated hearingListingNumberUpdated) {
        final Map<UUID, Integer> listingMap = hearingListingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber, Math::max));

        handleListingNumber(listingMap);
    }

    private void handleListingNumber(final Map<UUID, Integer> listingMap) {
        hearing = Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                                .withDefendants(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                                .withOffences(defendant.getOffences().stream()
                                                        .map(offence -> Offence.offence().withValuesFrom(offence)
                                                                .withListingNumber(ofNullable(listingMap.get(offence.getId())).orElse(offence.getListingNumber()))
                                                                .build())
                                                        .collect(toList()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
    }

    private void setHearing(final Hearing hearing) {

        Hearing updatedHearing = dedupAllReportingRestrictions(hearing);
        updatedHearing = deDupAllApplications(updatedHearing);
        if(isNull(this.hearing) || isNull(this.hearing.getProsecutionCases())) {
            this.hearing = updatedHearing;
        }else {
            updateHearingWithSeedingHearings(updatedHearing);
        }
    }

    private void updateHearingWithSeedingHearings(final Hearing updatedHearing) {
        final Map<UUID, SeedingHearing> seedingMap = this.hearing.getProsecutionCases().stream()
                .flatMap(pc->pc.getDefendants().stream())
                .flatMap(def -> def.getOffences().stream())
                .filter(offence -> nonNull(offence.getSeedingHearing()))
                .collect(Collectors.toMap(Offence::getId, Offence::getSeedingHearing,  (existing, replacement) -> replacement));

        this.hearing = Hearing.hearing().withValuesFrom(updatedHearing)
                .withProsecutionCases(ofNullable(updatedHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                                .withDefendants(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                                .withOffences(defendant.getOffences().stream()
                                                        .map(offence -> Offence.offence().withValuesFrom(offence)
                                                                .withSeedingHearing(Optional.ofNullable(offence.getSeedingHearing()).orElse(seedingMap.get(offence.getId())))
                                                                .build())
                                                        .collect(toList()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
    }

    private void updateHearingExtended(final HearingListingNeeds hearingRequest) {
        final List<ProsecutionCase> newlyAddedProsecutionCase = new ArrayList<>();
        this.hearing = Hearing.hearing().withValuesFrom(this.hearing).withCourtCentre(hearingRequest.getCourtCentre()).build();
        if (this.hearing.getProsecutionCases() != null && hearingRequest.getProsecutionCases() != null) {
            hearingRequest.getProsecutionCases().forEach(prosecutionCase -> {
                final Optional<ProsecutionCase> extendedProsecutionCase = this.hearing.getProsecutionCases().stream().filter(prosecutionCase1 -> prosecutionCase1.getId().equals(prosecutionCase.getId())).findAny();
                if (extendedProsecutionCase.isPresent()) {
                    final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream().filter(defendant -> extendedProsecutionCase.get().getDefendants().stream().anyMatch(defendant1 -> !defendant1.getId().equals(defendant.getId()))).findAny();
                    optionalDefendant.ifPresent(newDefendant -> extendedProsecutionCase.get().getDefendants().add(newDefendant));
                } else {
                    newlyAddedProsecutionCase.add(prosecutionCase);
                }
            });
            this.hearing.getProsecutionCases().addAll(newlyAddedProsecutionCase);
        }
        if (hearingRequest.getCourtApplications() != null) {
            this.hearing.getCourtApplications().addAll(hearingRequest.getCourtApplications());
        }
    }

    private void handleCaseAddedToHearingBdf(CaseAddedToHearingBdf caseAddedToHearingBdf) {
        if(isNull(this.hearing)){
            return;
        }
        final Hearing updatedHearing = addCaseToHearing(this.hearing, caseAddedToHearingBdf.getProsecutionCases());

        this.committingCourt = findCommittingCourt( updatedHearing);
        setHearing(updatedHearing);
    }

    private void handleCaseMarkesUpdate(final CaseMarkersUpdatedInHearing caseMarkersUpdatedInHearing) {
        hearing = Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(hearing.getProsecutionCases().stream()
                        .map(prosecutionCase -> prosecutionCase.getId().equals(caseMarkersUpdatedInHearing.getProsecutionCaseId()) ?
                                ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase).withCaseMarkers(caseMarkersUpdatedInHearing.getCaseMarkers()).build() : prosecutionCase)
                        .collect(toList()))
                .build();
    }

    private void handleHearingUpdatedForAllocationFields(final HearingUpdatedForAllocationFields hearingUpdatedForAllocationFields) {
        hearing = Hearing.hearing().withValuesFrom(hearing)
                .withType(hearingUpdatedForAllocationFields.getType())
                .withCourtCentre(hearingUpdatedForAllocationFields.getCourtCentre())
                .withHearingDays(hearingUpdatedForAllocationFields.getHearingDays())
                .withHearingLanguage(hearingUpdatedForAllocationFields.getHearingLanguage())
                .withCourtApplications(addOrUpdateApplication(hearing.getCourtApplications(), hearingUpdatedForAllocationFields.getCourtApplication()))
                .build();


    }

    private List<CourtApplication> addOrUpdateApplication(final List<CourtApplication> courtApplications, final CourtApplication courtApplication) {

        if (isNull(courtApplication)) {
            return courtApplications;
        }
        else if (isNull(courtApplications)) {
            return Stream.of(courtApplication).collect(toList());
        }  else {
            if (courtApplications.stream().anyMatch(app -> app.getId().equals(courtApplication.getId()))) {
                return courtApplications.stream().map(app -> app.getId().equals(courtApplication.getId()) ? courtApplication : app).collect(toList());
            } else {
                courtApplications.add(courtApplication);
                return courtApplications;
            }
        }
    }

    private void handleCourtApplicationUpdate(final HearingUpdatedWithCourtApplication hearingUpdatedWithCourtApplication) {
        hearing = Hearing.hearing().withValuesFrom(hearing)
                .withCourtApplications(hearing.getCourtApplications().stream()
                        .map(application -> application.getId().equals(hearingUpdatedWithCourtApplication.getCourtApplication().getId()) ? hearingUpdatedWithCourtApplication.getCourtApplication() : application)
                        .collect(toList()))
                .build();
    }

    private ProsecutionCase getUpdatedProsecutionCase(ProsecutionCase prosecutionCase, List<DefendantJudicialResult> hearingdefendantJudicialResults) {
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
                .withCpsOrganisationId(prosecutionCase.getCpsOrganisationId())
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withCaseStatus(allDefendantProceedingConcluded ? CaseStatusEnum.INACTIVE.getDescription() : prosecutionCase.getCaseStatus())
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType())
                .withIsCivil(prosecutionCase.getIsCivil())
                .withIsGroupMember(prosecutionCase.getIsGroupMember())
                .withIsGroupMaster(prosecutionCase.getIsGroupMaster())
                .build();
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    public Stream<Object> updateDefendant(final UUID hearingId, final DefendantUpdate defendantUpdate) {

        if(this.deleted || this.duplicate || HearingListingStatus.HEARING_RESULTED.equals(this.hearingListingStatus)){
            return Stream.empty();
        }

        return apply(Stream.of(HearingDefendantUpdated.hearingDefendantUpdated()
                .withDefendant(defendantUpdate)
                .withHearingId(hearingId)
                .build()));

    }

    public Stream<Object> recordUpdateMatchedDefendantDetailRequest(final DefendantUpdate defendantUpdate) {
        final UUID defendantId = defendantUpdate.getId();

        if (isNull(hearing.getProsecutionCases())){
            if (isNull(hearing.getCourtApplications())){
                return empty();
            }
            return apply(Stream.of(ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.prosecutionCaseUpdateDefendantsWithMatchedRequestedV2()
                    .withDefendantUpdate(defendantUpdate)
                    .withMatchedDefendants(asList())
                    .build()));
        }

        final Optional<Defendant> originalDefendantPreviousVersion = hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> defendant.getId().equals(defendantId))
                .findFirst();

        if (originalDefendantPreviousVersion.isPresent()) {
            final UUID masterDefendantId = originalDefendantPreviousVersion.get().getMasterDefendantId();
            final List<Defendant> matchedDefendants = hearing.getProsecutionCases().stream()
                    .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .filter(defendant -> Objects.equals(defendant.getMasterDefendantId(), masterDefendantId)
                            && !Objects.equals(defendant.getId(), defendantId))
                    .collect(Collectors.toList());

            return apply(Stream.of(ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.prosecutionCaseUpdateDefendantsWithMatchedRequestedV2()
                    .withDefendantUpdate(defendantUpdate)
                    .withDefendant(originalDefendantPreviousVersion.get())
                    .withMatchedDefendants(matchedDefendants)
                    .build()));
        } else {
            LOGGER.info("Cannot Find Defendant in Hearing Stream {}", defendantId);
            return empty();
        }
    }

    public Stream<Object> deleteHearing(final UUID hearingId) {

        if (this.deleted  || (nonNull(hearingListingStatus) && hearingListingStatus.equals(HearingListingStatus.HEARING_RESULTED)))  {
            return empty();
        }

        return populateHearingObjectStream(hearingId);
    }
    public Stream<Object> resultHearingByBdf(final UUID hearingId) {
        return apply(Stream.of(HearingResultedBdf.hearingResultedBdf()
                .withHearingId(hearingId)
                .build()));
    }

    public Stream<Object> removeApplicationFromSeedingHearing(final UUID hearingId, final UUID applicationId) {
        return apply(Stream.of(CourtApplicationRemovedFromSeedingHearing.courtApplicationRemovedFromSeedingHearing()
                .withApplicationId(applicationId)
                .withSeedingHearingId(hearingId)
                .build()));
    }

    public Stream<Object> updateHearingByBdf(final UUID hearingId, final UUID caseId, final UUID defendantId, final UUID offenceId,
                                             final List<JudicialResult> defendantCaseJudicialResults, final List<JudicialResult> offenceJudicialResults) {

        return apply(Stream.of(HearingAddMissingResultsBdf.hearingAddMissingResultsBdf()
                .withHearingId(hearingId)
                .withProsecutionCaseId(caseId)
                .withDefendantId(defendantId)
                .withOffenceId(offenceId)
                .withDefendantCaseJudicialResults(defendantCaseJudicialResults)
                .withOffenceJudicialResults(offenceJudicialResults)
                .build()));
    }

    private Stream<Object> populateHearingObjectStream(final UUID hearingId) {
        if (isNull(hearing)) {
            return Stream.empty();
        }
        final List<UUID> prosecutionCaseIds = isNotEmpty(hearing.getProsecutionCases()) ? getProsecutionCaseIds(hearing) : null;
        final List<UUID> offenceIds = isNotEmpty(hearing.getProsecutionCases()) ? getProsecutionCaseOffenceIds(hearing) : null;
        final List<UUID> courtApplicationIds = isNotEmpty(hearing.getCourtApplications()) ? getCourtApplicationIds(hearing) : null;

        final Stream.Builder builder = Stream.builder();
        if(nonNull(hearingListingStatus) && !hearingListingStatus.equals(HearingListingStatus.HEARING_RESULTED)) {
            builder.add(HearingDeleted.hearingDeleted()
                    .withHearingId(hearingId)
                    .withCourtApplicationIds(courtApplicationIds)
                    .withProsecutionCaseIds(prosecutionCaseIds)
                    .build());
        }
        if (isNotEmpty(prosecutionCaseIds)) {
            builder.add(OffenceInHearingDeleted.offenceInHearingDeleted()
                    .withProsecutionCaseIds(prosecutionCaseIds)
                    .withOffenceIds(offenceIds)
                    .build());
        }
        final Stream<Object> deleteEvent = apply(builder.build());
        return Stream.concat(Stream.concat(deleteEvent, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }

    /**
     * DO NOT USE THIS FUNCTION EXCEPT FOR THE PURPOSE MENTIONED BELOW.
     * The aggregate function is being added to be invoked only by the BDF, purpose of this function to raise 'progression.event.hearing-deleted'
     * event to remove any child entries of deleted hearing entity from the view store.
     *
     * @param hearingId The already deleted hearing id
     * @return The Stream object
     */
    public Stream<Object> deleteHearingOnlyByBdf(final UUID hearingId) {

        return populateHearingObjectStream(hearingId);
    }

    public Stream<Object> hearingTrialVacated(final UUID hearingId, final UUID vacatedTrialReasonId) {

        if (nonNull(this.hearing) && !this.deleted) {

            return apply(Stream.of(HearingTrialVacated.hearingTrialVacated()
                    .withHearingId(hearingId)
                    .withVacatedTrialReasonId(vacatedTrialReasonId)
                    .build()));
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> removeOffenceFromHearing(final UUID hearingId, final List<UUID> offencesToBeRemoved, final Boolean isResultFlow) {

        if (this.deleted) {
            return empty();
        }

        final List<UUID> existingOffences = this.hearing.getProsecutionCases().stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(def -> def.getOffences().stream())
                .map(offence -> offence.getId())
                .filter(id -> offencesToBeRemoved.contains(id))
                .collect(toList());

        if (existingOffences.isEmpty()) {
            return empty();
        }

        final List<UUID> defendantsToBeRemoved = getDefendantsToBeRemoved(offencesToBeRemoved);
        final List<UUID> prosecutionCasesToBeRemoved = getProsecutionCasesToBeRemoved(defendantsToBeRemoved);

        return apply(Stream.of(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withOffenceIds(offencesToBeRemoved)
                .withDefendantIds(defendantsToBeRemoved)
                .withProsecutionCaseIds(prosecutionCasesToBeRemoved)
                .withIsResultFlow(isResultFlow)
                .build()));
    }


    public Stream<Object> updateApplication(final CourtApplication courtApplication) {
        final Stream<Object> event1 = apply(Stream.of(HearingUpdatedWithCourtApplication.hearingUpdatedWithCourtApplication()
                .withCourtApplication(courtApplication)
                .build()));

        return Stream.concat(Stream.concat(event1, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }

    public Stream<Object> updateApplicationHearing(final DefendantUpdate defendantUpdate) {
        if(nonNull(hearing) && nonNull(hearing.getCourtApplications())) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("updateApplicationWithUpdatedDefendant called for hearing id {}", hearing.getId());
            }
            final List<CourtApplication> updatedCourtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                    .map(application -> updateApplicationWithUpdatedDefendantInfo(application,defendantUpdate))
                    .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
            final Hearing updatedHearing = Hearing.hearing().withValuesFrom(hearing)
                    .withCourtApplications(updatedCourtApplications).build();
            final Stream<Object> event1 = apply(Stream.of(ApplicationHearingDefendantUpdated.applicationHearingDefendantUpdated()
                            .withDefendant(defendantUpdate)
                            .withHearing(updatedHearing).build()));
            return Stream.concat(Stream.concat(event1, populateHearingToProbationCaseWorker()), populateHearingToVEP());
        }
       return Stream.empty();
    }

    private CourtApplication updateApplicationWithUpdatedDefendantInfo(final CourtApplication persistedApplication, final DefendantUpdate defendant) {
        final boolean isDefendantOrganisation = nonNull(defendant.getLegalEntityDefendant());
        final UUID updatedDefendantId = ofNullable(defendant.getMasterDefendantId()).orElse(defendant.getId());
        final CourtApplication.Builder courtApplication = CourtApplication.courtApplication().withValuesFrom(persistedApplication);
        updateApplicantWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        updateSubjectWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        updateRespondentsWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        return courtApplication.build();
    }

    private static void updateSubjectWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                        final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(nonNull(persistedApplication.getSubject()) && nonNull(persistedApplication.getSubject().getMasterDefendant()) &&
                updatedDefendantId.equals(persistedApplication.getSubject().getMasterDefendant().getMasterDefendantId())){
            if(isDefendantOrganisation){
                courtApplication.withSubject(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getSubject())
                        .withMasterDefendant(buildOrganisationDefendant(persistedApplication.getSubject().getMasterDefendant() , defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
            }else{
                courtApplication.withSubject(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getSubject())
                        .withMasterDefendant(buildPersonDefendant(persistedApplication.getSubject().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
            }
        }
    }

    private static void updateApplicantWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                          final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(nonNull(persistedApplication.getApplicant()) && nonNull(persistedApplication.getApplicant().getMasterDefendant()) &&
                updatedDefendantId.equals(persistedApplication.getApplicant().getMasterDefendant().getMasterDefendantId())){
            if(isDefendantOrganisation){
                courtApplication.withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getApplicant())
                        .withMasterDefendant(buildOrganisationDefendant(persistedApplication.getApplicant().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
            }else{
                courtApplication.withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getApplicant())
                        .withMasterDefendant(buildPersonDefendant(persistedApplication.getApplicant().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
            }
        }
    }

    private static void updateRespondentsWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                            final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(nonNull(persistedApplication.getRespondents())) {
            final Optional<CourtApplicationParty> updatedRespondent = persistedApplication.getRespondents().stream()
                    .filter(resp -> nonNull(resp.getMasterDefendant()) && resp.getMasterDefendant().getMasterDefendantId()
                            .equals(updatedDefendantId)).findFirst();

            if (updatedRespondent.isPresent()) {
                LOGGER.info("Match found for updated Defendant in Application Respondents");
                final List<CourtApplicationParty> courtApplicationRespondentsList = new ArrayList<>();
                persistedApplication.getRespondents().stream()
                        .filter(resp -> isNull(resp.getMasterDefendant()) || !resp.getMasterDefendant().getMasterDefendantId().equals(updatedDefendantId))
                        .forEach(courtApplicationRespondentsList::add);

                if (!isDefendantOrganisation) {
                    courtApplicationRespondentsList.add(CourtApplicationParty.courtApplicationParty()
                            .withValuesFrom(updatedRespondent.get())
                            .withMasterDefendant(buildPersonDefendant(updatedRespondent.get().getMasterDefendant(), defendant))
                            .withUpdatedOn(LocalDate.now())
                            .build());
                } else {
                    courtApplicationRespondentsList.add(CourtApplicationParty.courtApplicationParty()
                            .withValuesFrom(updatedRespondent.get())
                            .withMasterDefendant(buildOrganisationDefendant(updatedRespondent.get().getMasterDefendant(), defendant))
                            .withUpdatedOn(LocalDate.now())
                            .build());
                }
                courtApplication.withRespondents(courtApplicationRespondentsList);
            }
        }
    }

    private static MasterDefendant buildOrganisationDefendant(final MasterDefendant masterDefendant, final DefendantUpdate defendant) {
        return MasterDefendant.masterDefendant()
                .withValuesFrom(masterDefendant)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withValuesFrom(masterDefendant.getLegalEntityDefendant())
                        .withOrganisation(Organisation.organisation()
                                .withValuesFrom(masterDefendant.getLegalEntityDefendant().getOrganisation())
                                .withAddress(defendant.getLegalEntityDefendant().getOrganisation().getAddress())
                                .build()).build()).build();
    }

    private static MasterDefendant buildPersonDefendant(final MasterDefendant masterDefendant, final DefendantUpdate defendant) {
        return MasterDefendant.masterDefendant()
                .withValuesFrom(masterDefendant)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withValuesFrom(masterDefendant.getPersonDefendant())
                        .withPersonDetails(Person.person()
                                .withValuesFrom(masterDefendant.getPersonDefendant().getPersonDetails())
                                .withAddress(defendant.getPersonDefendant().getPersonDetails().getAddress())
                                .withDateOfBirth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())
                                .withNationalityId(defendant.getPersonDefendant().getPersonDetails().getNationalityId())
                                .withNationalityDescription(defendant.getPersonDefendant().getPersonDetails().getNationalityDescription())
                                .withNationalityCode(defendant.getPersonDefendant().getPersonDetails().getNationalityCode())
                                .build())
                        .withCustodialEstablishment(defendant.getPersonDefendant().getCustodialEstablishment()).build()).build();
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

    private void onCustodyTimeLimitClockStopped(final CustodyTimeLimitClockStopped custodyTimeLimitClockStopped) {
        final List<UUID> offenceIds = custodyTimeLimitClockStopped.getOffenceIds();
        this.hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .forEach(defendant -> {
                    final List<Offence> updatedOffences = defendant.getOffences().stream()
                            .filter(o -> offenceIds.contains(o.getId()))
                            .collect(toList());
                    for (final Offence offence : updatedOffences) {
                        final int index = defendant.getOffences().indexOf(offence);
                        defendant.getOffences().remove(offence);
                        defendant.getOffences().add(index, Offence.offence()
                                .withValuesFrom(offence)
                                .withCustodyTimeLimit(null)
                                .withCtlClockStopped(true)
                                .build());
                    }


                });

    }

    private void updateVacateTrial(final HearingTrialVacated hearingTrialVacated) {
        if (nonNull(hearingTrialVacated.getVacatedTrialReasonId())) {
            final Hearing.Builder builder = Hearing.hearing().withValuesFrom(this.hearing)
                    .withIsVacatedTrial(true);
            this.hearing = builder.build();
        } else {
            final Hearing.Builder builder = Hearing.hearing().withValuesFrom(this.hearing)
                    .withIsVacatedTrial(false);
            this.hearing = builder.build();

        }

    }

    private void updateHearing(final HearingUpdatedProcessed hearingUpdatedProcessed) {
        setHearing(hearingUpdatedProcessed.getHearing());
    }

    private void onExtendCustodyTimeLimitResulted(final ExtendCustodyTimeLimitResulted event) {
        final UUID offenceId = event.getOffenceId();
        this.hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> defendant.getOffences().stream()
                        .anyMatch(offence -> offence.getId().equals(offenceId)))
                .forEach(defendant -> {

                    final Optional<Offence> offence = defendant.getOffences().stream()
                            .filter(o -> o.getId().equals(offenceId))
                            .findFirst();

                    if (offence.isPresent()) {
                        final int index = defendant.getOffences().indexOf(offence.get());
                        defendant.getOffences().remove(offence.get());
                        defendant.getOffences().add(index, Offence.offence()
                                .withValuesFrom(offence.get())
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withValuesFrom(nonNull(offence.get().getCustodyTimeLimit()) ?
                                                offence.get().getCustodyTimeLimit() : CustodyTimeLimit.custodyTimeLimit().build())
                                        .withTimeLimit(event.getExtendedTimeLimit())
                                        .withIsCtlExtended(true)
                                        .build())
                                .build());
                    }
                });
    }

    private void onHearingDefendantUpdated(final HearingDefendantUpdated event) {
        if (nonNull(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().forEach(prosecutionCase -> {
                final Optional<Defendant> defendant = prosecutionCase.getDefendants().stream()
                        .filter(d -> d.getId().equals(event.getDefendant().getId()))
                        .findFirst();
                if (defendant.isPresent()) {
                    final Defendant updatedDefendant = fromUpdatedDefendant(defendant.get(), event.getDefendant());
                    final int index = prosecutionCase.getDefendants().indexOf(defendant.get());
                    prosecutionCase.getDefendants().remove(index);
                    prosecutionCase.getDefendants().add(index, updatedDefendant);
                }
            });
        }
    }

    private Defendant fromUpdatedDefendant(final Defendant defendant, final DefendantUpdate defendantUpdate) {
        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withOffences(nonNull(defendant.getOffences())?enrichOffence(defendant.getOffences(),defendantUpdate.getOffences()):defendant.getOffences())
                .withNumberOfPreviousConvictionsCited(defendantUpdate.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendantUpdate.getProsecutionAuthorityReference())
                .withWitnessStatement(defendantUpdate.getWitnessStatement())
                .withWitnessStatementWelsh(defendantUpdate.getWitnessStatementWelsh())
                .withMitigation(defendantUpdate.getMitigation())
                .withMitigationWelsh(defendantUpdate.getMitigationWelsh())
                .withAssociatedPersons(defendantUpdate.getAssociatedPersons())
                .withDefenceOrganisation(defendantUpdate.getDefenceOrganisation())
                .withPersonDefendant(defendantUpdate.getPersonDefendant())
                .withLegalEntityDefendant(defendantUpdate.getLegalEntityDefendant())
                .withPncId(defendantUpdate.getPncId())
                .withAliases(defendantUpdate.getAliases())
                .withIsYouth(defendantUpdate.getIsYouth())
                .build();



    }

    private List<Offence> enrichOffence(final List<Offence> offences, final List<Offence> updatedOffences) {
        if (isNull(updatedOffences)){
            return offences;
        }
        return
                offences.stream()
                        .map(offence -> {
                            final Optional<Offence> updatedOffence = updatedOffences.stream().filter(offence1 -> offence1.getId().equals(offence.getId())).findFirst();
                            return new Offence.Builder()
                                    .withValuesFrom(offence)
                                    .withLaaApplnReference(laaContract(offence, updatedOffence))
                                    .build();
                        })
                        .collect(toList());

    }

    private LaaReference laaContract(final Offence offence, final Optional<Offence> updatedOffence) {
        return updatedOffence.map(Offence::getLaaApplnReference).orElse(offence.getLaaApplnReference());


    }

    private void onOffencesRemovedFromHearing(final OffencesRemovedFromHearing offencesRemovedFromHearing) {

        if(ofNullable(offencesRemovedFromHearing.getIsResultFlow()).orElse(false)){
            seededProsecutionCases = ofNullable(this.hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                    .map(pc -> ProsecutionCase.prosecutionCase().withValuesFrom(pc)
                            .withDefendants(pc.getDefendants().stream().map(def -> Defendant.defendant().withValuesFrom(def)
                                    .withOffences(def.getOffences().stream().map(off -> Offence.offence().withValuesFrom(off).build()).toList())
                                    .build()).toList())
                            .build()).toList();
        } else {
            // update newOffences when an offence was deleted from the hearing.
            final Set<UUID> offences = hearing.getProsecutionCases().stream().flatMap( pc -> pc.getDefendants().stream())
                    .flatMap(def -> def.getOffences().stream()).map(Offence::getId).collect(Collectors.toSet());
            newOffences.removeIf( off -> !offences.contains(off));
        }
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

    public Stream<Object> processCreateNextHearing(final CreateNextHearing createNextHearing) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(NextHearingsRequested.nextHearingsRequested()
                .withHearing(createNextHearing.getHearing())
                .withCommittingCourt(createNextHearing.getCommittingCourt())
                .withSeedingHearing(createNextHearing.getSeedingHearing())
                .withShadowListedOffences(createNextHearing.getShadowListedOffences())
                .withPreviousBookingReferencesWithCourtScheduleIds(createNextHearing.getPreviousBookingReferencesWithCourtScheduleIds())
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> processHearingResults(final Hearing hearing, final ZonedDateTime sharedTime, final List<UUID> shadowListedOffences, final LocalDate hearingDay) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final List<ProsecutionCase> updatedProsecutionCasesForOriginalHearing = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(this::updateCaseForAdjourn)
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        final List<CourtApplication> updatedCourtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .map(this::updateApplicationWithAdjourn)
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
        final Hearing updatedHearing = Hearing.hearing().withValuesFrom(hearing)
                .withCourtApplications(updatedCourtApplications)
                .withProsecutionCases(updatedProsecutionCasesForOriginalHearing).build();

        streamBuilder.add(createListingStatusResultedEvent(updatedHearing));
        streamBuilder.add(createHearingResultedEvent(updatedHearing, sharedTime, hearingDay));

        final List<JudicialResult> judicialResults = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());
        judicialResults.addAll(ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getDefendantCaseJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList()));
        if(isNotEmpty(hearing.getProsecutionCases()) && isNotEmpty(judicialResults)) {
            streamBuilder.add(createProsecutionCasesResultedV2Event(updatedHearing, shadowListedOffences, hearingDay));
        }

        if (isNotEmpty(hearing.getProsecutionCases())) {
            final List<InitiateApplicationForCaseRequested> requestInitiateApplicationForCaseEvents = createRequestInitiateApplicationForCaseEvents(updatedHearing);
            requestInitiateApplicationForCaseEvents.forEach(streamBuilder::add);

            final List<DeleteApplicationForCaseRequested> deleteApplicationForCaseRequestedEvents = createDeleteApplicationForCaseRequestedEvents(updatedHearing);
            deleteApplicationForCaseRequestedEvents.forEach(streamBuilder::add);
        }

        if (isEligibleForNextHearings(hearing)) {
            final List<Object> nextHearingEvents = createNextHearingEvents(updatedHearing, shadowListedOffences, hearingDay);
            nextHearingEvents.forEach(streamBuilder::add);
        }

        if (isNotEmpty(hearing.getCourtApplications())) {
            streamBuilder.add(applicationsResulted()
                    .withHearing(updatedHearing)
                    .withShadowListedOffences(shadowListedOffences)
                    .withCommittingCourt(this.committingCourt)
                    .build());
        }

        addExtendCustodyTimeLimitResulted(hearing, streamBuilder);


        return apply(streamBuilder.build());
    }

    public Stream<Object> updateRelatedHearing(final HearingListingNeeds hearingListingNeeds,
                                               final Boolean isAdjourned,
                                               final UUID extendHearingFrom,
                                               final Boolean isPartiallyAllocated,
                                               final SeedingHearing seedingHearing,
                                               final List<UUID> shadowListedOffences) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final HearingListingNeeds newHearingListingNeeds = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds).build();

        if (! HearingListingStatus.HEARING_RESULTED.equals(this.hearingListingStatus)) {
            final Set<ProsecutionCase> resultCases = new HashSet<>();
            getProsecutionCasesAfterMergeAtDifferentLevel(hearingListingNeeds, resultCases);

            newHearingListingNeeds.getProsecutionCases().clear();
            newHearingListingNeeds.getProsecutionCases().addAll(resultCases);

            final Hearing prosecutionCaseDefendantListingHearing = Hearing.hearing()
                    .withId(newHearingListingNeeds.getId())
                    .withHearingDays(this.getHearing().getHearingDays())
                    .withHasSharedResults(false)
                    .withCourtApplications(newHearingListingNeeds.getCourtApplications())
                    .withCourtCentre(newHearingListingNeeds.getCourtCentre())
                    .withJurisdictionType(newHearingListingNeeds.getJurisdictionType())
                    .withType(newHearingListingNeeds.getType())
                    .withReportingRestrictionReason(newHearingListingNeeds.getReportingRestrictionReason())
                    .withJudiciary(newHearingListingNeeds.getJudiciary())
                    .withBookingType(newHearingListingNeeds.getBookingType())
                    .withProsecutionCases(newHearingListingNeeds.getProsecutionCases()).build();

            final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = prosecutionCaseDefendantListingStatusChangedV2()
                    .withHearing(prosecutionCaseDefendantListingHearing)
                    .withHearingListingStatus(this.hearingListingStatus)
                    .build();

            streamBuilder.add(prosecutionCaseDefendantListingStatusChangedV2);
        }


        final RelatedHearingUpdated relatedHearingUpdated = RelatedHearingUpdated.relatedHearingUpdated()
                .withExtendedHearingFrom(extendHearingFrom)
                .withHearingRequest(newHearingListingNeeds)
                .withIsAdjourned(isAdjourned)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .withSeedingHearing(seedingHearing)
                .withShadowListedOffences(shadowListedOffences)
                .build();

        streamBuilder.add(relatedHearingUpdated);

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateRelatedHearingForAdhocHearing(final HearingListingNeeds hearingListingNeeds,
                                                              final Boolean sendNotificationToParties) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final HearingListingNeeds newHearingListingNeeds = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds).build();


        final Set<ProsecutionCase> resultCases = new HashSet<>();
        getProsecutionCasesAfterMergeAtDifferentLevel(hearingListingNeeds, resultCases);

        newHearingListingNeeds.getProsecutionCases().clear();
        newHearingListingNeeds.getProsecutionCases().addAll(resultCases);

        final Hearing prosecutionCaseDefendantListingHearing = Hearing.hearing()
                .withId(newHearingListingNeeds.getId())
                .withHearingDays(this.getHearing().getHearingDays())
                .withHasSharedResults(false)
                .withCourtApplications(newHearingListingNeeds.getCourtApplications())
                .withCourtCentre(newHearingListingNeeds.getCourtCentre())
                .withJurisdictionType(newHearingListingNeeds.getJurisdictionType())
                .withType(newHearingListingNeeds.getType())
                .withReportingRestrictionReason(newHearingListingNeeds.getReportingRestrictionReason())
                .withJudiciary(newHearingListingNeeds.getJudiciary())
                .withBookingType(newHearingListingNeeds.getBookingType())
                .withProsecutionCases(newHearingListingNeeds.getProsecutionCases()).build();

        final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(prosecutionCaseDefendantListingHearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .build();

        streamBuilder.add(prosecutionCaseDefendantListingStatusChangedV2);



        final RelatedHearingUpdatedForAdhocHearing relatedHearingUpdated = RelatedHearingUpdatedForAdhocHearing.relatedHearingUpdatedForAdhocHearing()
                .withHearingRequest(newHearingListingNeeds)
                .withSendNotificationToParties(sendNotificationToParties)
                .build();

        streamBuilder.add(relatedHearingUpdated);
        final Stream events = apply(streamBuilder.build());
        return Stream.concat(Stream.concat(events, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }


    /**
     * This method is responsible for merging cases, defendants and offences between HearingListingNeeds which is passed in payload and hearing in aggregate.
     * Since the case can be splitted at case, defendant and offence levels when we are merging here we need to compare at every level and merge it back.
     *
     * @param hearingListingNeeds HearingListingNeeds which is passed in payload
     * @param resultCases         Result cases
     * @return set of new case with merged cases, defendant or offence based on which level spilt has happened.
     */
    private Set<ProsecutionCase> getProsecutionCasesAfterMergeAtDifferentLevel(HearingListingNeeds hearingListingNeeds, final Set<ProsecutionCase> resultCases) {
        if (nonNull(this.hearing.getProsecutionCases()) && nonNull(hearingListingNeeds.getProsecutionCases())) {

            // Collects combined set of case Ids
            final Set<UUID> combinedSetOfCaseIds = hearingListingNeeds.getProsecutionCases().stream().map(ProsecutionCase::getId).collect(Collectors.toSet());
            combinedSetOfCaseIds.addAll(this.hearing.getProsecutionCases().stream().map(ProsecutionCase::getId).collect(Collectors.toSet()));

            combinedSetOfCaseIds.stream().forEach(caseId -> {
                final Optional<ProsecutionCase> caseInHearingOptional = this.hearing.getProsecutionCases().stream().filter(hearingCase -> hearingCase.getId().equals(caseId)).findFirst();
                final Optional<ProsecutionCase> caseInPayloadOptional = hearingListingNeeds.getProsecutionCases().stream().filter(payloadCase -> payloadCase.getId().equals(caseId)).findFirst();

                if (caseInHearingOptional.isPresent()) {
                    if (caseInPayloadOptional.isPresent()) {
                        final ProsecutionCase mergedProsecutionCase = getMergedProsecutionFromPayloadAndHearing(caseInHearingOptional.get(), caseInPayloadOptional.get());
                        resultCases.add(mergedProsecutionCase);
                    } else {
                        resultCases.add(ProsecutionCase.prosecutionCase().withValuesFrom(caseInHearingOptional.get()).build());
                    }
                } else {
                    resultCases.add(ProsecutionCase.prosecutionCase().withValuesFrom(caseInPayloadOptional.get()).build());
                }
            });
        }
        return resultCases;
    }

    /**
     * Merge Defendant and Offences at the case level
     *
     * @param caseInHearing - The case from hearing aggregate
     * @param caseInPayload - The case from input payload
     * @return - The post process defendant and the offences merges from both the prosecution cases
     */
    private ProsecutionCase getMergedProsecutionFromPayloadAndHearing(final ProsecutionCase caseInHearing, final ProsecutionCase caseInPayload) {
        // Collects combined set of Defendant Ids
        final Set<UUID> combinedSetOfDefendantIds = caseInHearing.getDefendants().stream().map(Defendant::getId).collect(Collectors.toSet());
        combinedSetOfDefendantIds.addAll(caseInPayload.getDefendants().stream().map(Defendant::getId).collect(Collectors.toSet()));

        final Set<Defendant> defendantResult = new HashSet<>();
        combinedSetOfDefendantIds.stream().forEach(defId -> {
            final Optional<Defendant> defInHearingCaseOptional = caseInHearing.getDefendants().stream().filter(hearingDef -> hearingDef.getId().equals(defId)).findFirst();
            final Optional<Defendant> defInPayloadCaseOptional = caseInPayload.getDefendants().stream().filter(payloadDef -> payloadDef.getId().equals(defId)).findFirst();

            if (defInHearingCaseOptional.isPresent()) {
                if (defInPayloadCaseOptional.isPresent()) {
                    defendantResult.add(getMergedDefendantFromPayloadAndHearingCase(defInHearingCaseOptional.get(), defInPayloadCaseOptional.get()));
                } else {
                    defendantResult.add(Defendant.defendant().withValuesFrom(defInHearingCaseOptional.get()).build());
                }
            } else {
                defendantResult.add(Defendant.defendant().withValuesFrom(defInPayloadCaseOptional.get()).build());
            }
        });
        return ProsecutionCase.prosecutionCase().withValuesFrom(caseInPayload).withDefendants(new ArrayList<>(defendantResult)).build();
    }

    /**
     * Merge Offences at the defendant level
     *
     * @param defendantInHearingCase - The defendant from hearing aggregate
     * @param defendantInPayloadCase - The defendant from input payload
     * @return - The post process offences merges from both the defendant
     */
    private Defendant getMergedDefendantFromPayloadAndHearingCase(final Defendant defendantInHearingCase, final Defendant defendantInPayloadCase) {
        // Collects combined set of Offence Ids
        final Set<UUID> combinedSetOfOffenceIds = defendantInHearingCase.getOffences().stream().map(Offence::getId).collect(Collectors.toSet());
        combinedSetOfOffenceIds.addAll(defendantInPayloadCase.getOffences().stream().map(Offence::getId).collect(Collectors.toSet()));

        final Set<Offence> offenceResult = new HashSet<>();
        combinedSetOfOffenceIds.stream().forEach(offenceId -> {
            final Optional<Offence> offenceInHearingDefOptional = defendantInHearingCase.getOffences().stream().filter(hearingDef -> hearingDef.getId().equals(offenceId)).findFirst();
            final Optional<Offence> offenceInPayloadDefOptional = defendantInPayloadCase.getOffences().stream().filter(payloadDef -> payloadDef.getId().equals(offenceId)).findFirst();
            if (offenceInHearingDefOptional.isPresent()) {
                offenceResult.add(Offence.offence().withValuesFrom(offenceInHearingDefOptional.get()).build());
            } else {
                offenceResult.add(Offence.offence().withValuesFrom(offenceInPayloadDefOptional.get()).build());
            }
        });
        return Defendant.defendant().withValuesFrom(defendantInPayloadCase).withOffences(new ArrayList<>(offenceResult)).build();
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

    public Stream<Object> stopCustodyTimeLimitClock(final UUID hearingId, final List<UUID> offenceIds) {

        final List<UUID> caseIds = hearing.getProsecutionCases().stream()
                .filter(prosecutionCase -> prosecutionCase.getDefendants().stream()
                        .flatMap(defendant -> defendant.getOffences().stream())
                        .anyMatch(offence -> offenceIds.contains(offence.getId())))
                .map(ProsecutionCase::getId)
                .collect(toList());

        return apply(Stream.of(CustodyTimeLimitClockStopped.custodyTimeLimitClockStopped()
                .withCaseIds(caseIds)
                .withHearingId(hearingId)
                .withOffenceIds(offenceIds)
                .build()));
    }


    public Stream<Object> updateAllocationFields(final UpdateHearingForAllocationFields updateHearingForAllocationFields) {
        if(nonNull(this.hearing)) {
            final Stream<Object> event1 = apply(Stream.of(HearingUpdatedForAllocationFields.hearingUpdatedForAllocationFields()
                    .withHearingDays(updateHearingForAllocationFields.getHearingDays())
                    .withCourtCentre(updateHearingForAllocationFields.getCourtCentre())
                    .withHearingLanguage(updateHearingForAllocationFields.getHearingLanguage())
                    .withType(updateHearingForAllocationFields.getType())
                    .withCourtApplication(updateHearingForAllocationFields.getCourtApplication())
                    .build()));

            return Stream.concat(Stream.concat(event1,
                    populateHearingToProbationCaseWorker()),
                    populateHearingToVEP());
        } else  {
            return apply(empty());
        }
    }

    public Stream<Object> populateHearingToProbationCaseWorker() {
        if (HearingListingStatus.SENT_FOR_LISTING.equals(this.hearingListingStatus)
                || isNull(hearing)  || Boolean.TRUE.equals(hearing.getIsBoxHearing())
                || HearingListingStatus.HEARING_RESULTED.equals(this.hearingListingStatus)) {
            return apply(empty());
        }

        final Hearing filteredOutHearing = filterHearingForProbationCaseWorker();

        if (isEmpty(filteredOutHearing.getProsecutionCases()) && isEmpty(filteredOutHearing.getCourtApplications())) {
            return apply(empty());
        }

        if (this.deleted) {
            return Stream.of(DeletedHearingPopulatedToProbationCaseworker.deletedHearingPopulatedToProbationCaseworker()
                    .withHearing(filteredOutHearing)
                    .build());
        } else {
            return Stream.of(HearingPopulatedToProbationCaseworker.hearingPopulatedToProbationCaseworker()
                    .withHearing(filteredOutHearing)
                    .build());
        }
    }

    private Hearing filterHearingForProbationCaseWorker() {
        Hearing filteredOutHearing = getFilteredOutHearing();

        filteredOutHearing = filterOutYouth(filteredOutHearing);
        return filteredOutHearing;
    }

    public Stream<Object> populateHearingToVEP() {
        if (HearingListingStatus.SENT_FOR_LISTING.equals(this.hearingListingStatus)
                || Boolean.TRUE.equals(hearing.getIsBoxHearing())
        ) {
            return apply(empty());
        }

        Hearing filteredOutHearing = getFilteredOutHearing();

        if (filteredOutHearing.getIsBoxHearing() != null && filteredOutHearing.getIsBoxHearing()) {
            return apply(empty());
        }

        if (this.deleted) {
            return apply(Stream.of(VejDeletedHearingPopulatedToProbationCaseworker.vejDeletedHearingPopulatedToProbationCaseworker()
                    .withHearing(filteredOutHearing)
                    .build()));
        } else {
            return apply(Stream.of(VejHearingPopulatedToProbationCaseworker.vejHearingPopulatedToProbationCaseworker()
                    .withHearing(filteredOutHearing)
                    .build()));
        }
    }


    private Hearing getFilteredOutHearing() {
        Hearing filteredOutHearing;
        filteredOutHearing = Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                                .withDefendants(prosecutionCase.getDefendants() != null ? new ArrayList<>(prosecutionCase.getDefendants()) : null)
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .withCourtApplications(hearing.getCourtApplications() != null ? new ArrayList<>(hearing.getCourtApplications()) : null)
                .build();

        return filteredOutHearing;
    }

    public Stream<Object> updateCaseMarkers(final List<Marker> caseMarkers, final UUID prosecutionCaseId, final UUID hearingId) {
        return apply(Stream.of(CaseMarkersUpdatedInHearing.caseMarkersUpdatedInHearing()
                .withHearingId(hearingId)
                .withCaseMarkers(caseMarkers)
                .withProsecutionCaseId(prosecutionCaseId)
                .build()));
    }


    public Stream<Object> updateOffence(UUID defendantId, final List<Offence> updatedOffences, final List<Offence> newOffences) {
        if (isNull(this.hearing.getHasSharedResults()) || !this.hearing.getHasSharedResults()) {
            LOGGER.info("Hearing with id {} and the status: {} is either not yet set or not shared, offence can be updated\"", hearing.getId(), hearingListingStatus);
            final Set<UUID> existingOffences = this.hearing.getProsecutionCases().stream()
                    .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .filter(defendant -> defendant.getId().equals(defendantId))
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .map(Offence::getId)
                    .collect(Collectors.toSet());

            final HearingOffencesUpdatedV2.Builder hearingOffencesUpdatedV2Builder = HearingOffencesUpdatedV2.hearingOffencesUpdatedV2()
                    .withDefendantId(defendantId)
                    .withHearingId(this.hearing.getId())
                    .withNewOffences(newOffences);

            if(updatedOffences != null){
                hearingOffencesUpdatedV2Builder.withUpdatedOffences(updatedOffences.stream().filter(offence -> existingOffences.contains(offence.getId()))
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())));
            }
            final HearingOffencesUpdatedV2 hearingOffencesUpdatedV2 = hearingOffencesUpdatedV2Builder.build();
            if(CollectionUtils.isEmpty(hearingOffencesUpdatedV2.getNewOffences()) && CollectionUtils.isEmpty(hearingOffencesUpdatedV2.getUpdatedOffences())){
                return Stream.empty();
            }else{
                return apply(Stream.of(hearingOffencesUpdatedV2));
            }
        } else {
            LOGGER.info("Hearing with id {} and the status: {} is already shared, offence can't be updated\"", hearing.getId(), hearingListingStatus);
            return apply(empty());
        }
    }


    public Stream<Object> addDefendant(final UUID hearingId, final UUID prosecutionCaseId, final List<Defendant> defendants) {
        return apply(Stream.of(NewDefendantAddedToHearing.newDefendantAddedToHearing().withHearingId(hearingId)
                .withDefendants(defendants)
                .withProsecutionCaseId(prosecutionCaseId).build()));
    }

    public Stream<Object> updateOffencesWithListingNumber(final List<OffenceListingNumbers> offenceListingNumbers) {
        final Map<UUID, Integer> listingMap = offenceListingNumbers.stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber, Math::max));
        return apply(Stream.of(ListingNumberUpdated.listingNumberUpdated()
                .withOffenceListingNumbers(offenceListingNumbers)
                .withHearingId(this.hearing.getId())
                .withProsecutionCaseIds(ofNullable(this.hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .filter(prosecutionCase -> prosecutionCase.getDefendants().stream().flatMap(defendant -> defendant.getOffences().stream()).anyMatch(offence -> nonNull(listingMap.get(offence.getId()))))
                        .map(ProsecutionCase::getId)
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build()));

    }

    public Stream<Object> updateHearingWithListingNumber(final UUID prosecutionCaseId, final UUID hearingId, final List<OffenceListingNumbers> offenceListingNumbers) {
        return apply(Stream.of(HearingListingNumberUpdated.hearingListingNumberUpdated()
                .withOffenceListingNumbers(offenceListingNumbers)
                .withHearingId(hearingId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build()));

    }

    public Stream<Object> updateHearingWithVerdict(final Verdict verdict) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final Hearing hearingWithNewVerdict = ofNullable(verdict.getApplicationId()).map(applicationId -> getHearingWithNewVerdictForApplication(applicationId, verdict)).orElseGet(() -> getHearingWithNewVerdictForOffences(verdict));

        streamBuilder.add(HearingVerdictUpdated.hearingVerdictUpdated().withHearingId(hearingWithNewVerdict.getId()).withVerdict(verdict).build());

        streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(this.hearingListingStatus)
                .withNotifyNCES(this.notifyNCES)
                .withHearing(hearingWithNewVerdict)
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> deleteCourtApplicationHearing(final UUID hearingId, final UUID applicationId, final UUID seedingHearingId) {
        final boolean isHearingAlreadyResulted = ofNullable(this.hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .anyMatch(c -> CollectionUtils.isNotEmpty(c.getJudicialResults()));
        if (isHearingAlreadyResulted) {
            return apply(Stream.of(DeleteCourtApplicationHearingIgnored.deleteCourtApplicationHearingIgnored()
                    .withApplicationId(applicationId)
                    .withHearingId(hearingId)
                    .withSeedingHearingId(seedingHearingId)
                    .build()));
        } else {
            return apply(Stream.of(CourtApplicationHearingDeleted.courtApplicationHearingDeleted()
                    .withApplicationId(applicationId)
                    .withHearingId(hearingId)
                    .withSeedingHearingId(seedingHearingId)
                    .build()));
        }
    }

    private void updateVerdict(final HearingVerdictUpdated hearingVerdictUpdated) {
        this.hearing = ofNullable(hearingVerdictUpdated.getVerdict().getApplicationId())
                .map(applicationId -> getHearingWithNewVerdictForApplication(applicationId, hearingVerdictUpdated.getVerdict()))
                .orElseGet(() -> getHearingWithNewVerdictForOffences(hearingVerdictUpdated.getVerdict()));
    }

    private Hearing getHearingWithNewVerdictForApplication(final UUID applicationId, final Verdict verdict) {
        Hearing hearingObj;
        if(nonNull(verdict.getIsDeleted()) && verdict.getIsDeleted()) {
            hearingObj = Hearing.hearing().withValuesFrom(hearing)
                    .withCourtApplications(ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                            .map(courtApplication -> !courtApplication.getId().equals(applicationId) ? courtApplication : CourtApplication.courtApplication()
                                    .withValuesFrom(courtApplication)
                                    .withVerdict(null)
                                    .build()).collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                    .build();
        } else {
            hearingObj = Hearing.hearing().withValuesFrom(hearing)
                    .withCourtApplications(ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                            .map(courtApplication -> !courtApplication.getId().equals(applicationId) ? courtApplication : CourtApplication.courtApplication()
                                    .withValuesFrom(courtApplication)
                                    .withVerdict(verdict)
                                    .build()).collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                    .build();
        }

        return hearingObj;
    }

    private Hearing getHearingWithNewVerdictForOffences(final Verdict verdict) {
        return Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(getProsecutionCasesWithNewVerdict(verdict))
                .withCourtApplications(ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(courtApplication -> CourtApplication.courtApplication().withValuesFrom(courtApplication)
                                .withCourtApplicationCases(getCourtApplicationCasesWithNewVerdict(verdict, courtApplication))
                                .withCourtOrder(getCourtOrderWithNewVerdict(verdict, courtApplication))
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
    }

    private CourtOrder getCourtOrderWithNewVerdict(final Verdict verdict, final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtOrder())
                .map(courtOrder -> CourtOrder.courtOrder().withValuesFrom(courtOrder)
                        .withCourtOrderOffences(ofNullable(courtOrder.getCourtOrderOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence().withValuesFrom(courtOrderOffence)
                                        .withOffence(getOffenceWithNewVerdict(courtOrderOffence.getOffence(), verdict))
                                        .build())
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .build())
                .orElse(courtApplication.getCourtOrder());
    }

    private List<CourtApplicationCase> getCourtApplicationCasesWithNewVerdict(final Verdict verdict, final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase().withValuesFrom(courtApplicationCase)
                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(offence -> getOffenceWithNewVerdict(offence, verdict))
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .build())
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private List<ProsecutionCase> getProsecutionCasesWithNewVerdict(final Verdict verdict) {
        return ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                        .withDefendants(prosecutionCase.getDefendants().stream()
                                .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                        .withOffences(defendant.getOffences().stream()
                                                .map(offence -> getOffenceWithNewVerdict(offence, verdict))
                                                .collect(toList()))
                                        .build())
                                .collect(toList()))
                        .build())
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private Offence getOffenceWithNewVerdict(final Offence offence, final Verdict verdict) {
        if (verdict.getOffenceId().equals(offence.getId())) {
            if(nonNull(verdict.getIsDeleted()) && verdict.getIsDeleted()) {
                return Offence.offence().withValuesFrom(offence)
                        .withVerdict(null)
                        .build();
            } else {
                return Offence.offence().withValuesFrom(offence)
                        .withVerdict(verdict)
                        .build();
            }
        } else {
            return offence;
        }
    }

    public Stream<Object> updateHearingWithPlea(final PleaModel pleaModel) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final Hearing hearingWithNewPlea = ofNullable(pleaModel.getApplicationId()).map(applicationId -> getHearingWithNewPleaForApplication(applicationId, pleaModel.getPlea())).orElseGet(() -> getHearingWithNewPleaForOffences(pleaModel));

        if (!hearingWithNewPlea.equals(this.hearing)) {

            streamBuilder.add(HearingPleaUpdated.hearingPleaUpdated().withHearingId(this.hearing.getId()).withPleaModel(pleaModel).build());

            streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                    .withHearingListingStatus(this.hearingListingStatus)
                    .withNotifyNCES(this.notifyNCES)
                    .withHearing(hearingWithNewPlea)
                    .build());

            return apply(streamBuilder.build());
        } else {
            return empty();
        }
    }

    public Stream<Object> updateIndex(final Hearing newHearing, final HearingListingStatus hearingListingStatus, final Boolean notifyNCES) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearingListingStatus(hearingListingStatus)
                .withNotifyNCES(notifyNCES)
                .withHearing(newHearing)
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object>  updateHearingDetailsInUnifiedSearch(final UUID hearingId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if(hearing == null){
            LOGGER.error("Hearing not found with stream id: {}", hearingId);
        } else {
            streamBuilder.add(CaseHearingDetailsUpdatedInUnifiedSearch.caseHearingDetailsUpdatedInUnifiedSearch()
                    .withHearing(hearing)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    private Hearing getHearingWithNewPleaForApplication(final UUID applicationId, final Plea plea) {
        return Hearing.hearing().withValuesFrom(hearing)
                .withCourtApplications(ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(courtApplication -> !courtApplication.getId().equals(applicationId) ? courtApplication : CourtApplication.courtApplication()
                                .withValuesFrom(courtApplication)
                                .withPlea(plea)
                                .build()).collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
    }

    private Hearing getHearingWithNewPleaForOffences(final PleaModel pleaModel) {
        return Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(getProsecutionCasesWithNewPlea(pleaModel))
                .withCourtApplications(ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(courtApplication -> CourtApplication.courtApplication().withValuesFrom(courtApplication)
                                .withCourtApplicationCases(getCourtApplicationCasesWithNewPlea(pleaModel, courtApplication))
                                .withCourtOrder(getCourtOrderWithNewPlea(pleaModel, courtApplication))
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                .build();
    }

    private CourtOrder getCourtOrderWithNewPlea(final PleaModel pleaModel, final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtOrder())
                .map(courtOrder -> CourtOrder.courtOrder().withValuesFrom(courtOrder)
                        .withCourtOrderOffences(ofNullable(courtOrder.getCourtOrderOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence().withValuesFrom(courtOrderOffence)
                                        .withOffence(getOffenceWithNewPlea(courtOrderOffence.getOffence(), pleaModel))
                                        .build())
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .build())
                .orElse(courtApplication.getCourtOrder());
    }


    private List<CourtApplicationCase> getCourtApplicationCasesWithNewPlea(final PleaModel pleaModel, final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase().withValuesFrom(courtApplicationCase)
                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(offence -> getOffenceWithNewPlea(offence, pleaModel))
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .build())
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private List<ProsecutionCase> getProsecutionCasesWithNewPlea(final PleaModel pleaModel) {
        return ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                        .withDefendants(prosecutionCase.getDefendants().stream()
                                .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                        .withOffences(defendant.getOffences().stream()
                                                .map(offence -> getOffenceWithNewPlea(offence, pleaModel))
                                                .collect(toList()))
                                        .build())
                                .collect(toList()))
                        .build())
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private Offence getOffenceWithNewPlea(final Offence offence, final PleaModel pleaModel) {
        if (pleaModel.getOffenceId().equals(offence.getId())) {
            return Offence.offence().withValuesFrom(offence)
                    .withPlea(ofNullable(pleaModel.getPlea()).orElse(offence.getPlea()))
                    .withIndicatedPlea(ofNullable(pleaModel.getIndicatedPlea()).orElse(offence.getIndicatedPlea()))
                    .withAllocationDecision(ofNullable(pleaModel.getAllocationDecision()).orElse(offence.getAllocationDecision()))
                    .build();
        } else {
            return offence;
        }
    }


    private void updateOffenceInHearing(final HearingOffencesUpdated hearingOffencesUpdated) {
        if (isNotEmpty(this.hearing.getProsecutionCases()) &&
                (isNull(this.hearing.getHasSharedResults()) || !this.hearing.getHasSharedResults())) {
            this.hearing.getProsecutionCases().stream()
                    .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .forEach(defendant -> {
                                if (defendant.getId().equals(hearingOffencesUpdated.getDefendantId())) {
                                    defendant.getOffences().clear();
                                    defendant.getOffences().addAll(hearingOffencesUpdated.getUpdatedOffences().stream()
                                            .sorted(Comparator.comparing(o -> ofNullable(o.getOrderIndex()).orElse(0)))
                                            .collect(toList()));
                                }
                            }
                    );
        }
    }

    private void updateOffenceInHearingV2(final HearingOffencesUpdatedV2 hearingOffencesUpdated) {
        if (isNotEmpty(this.hearing.getProsecutionCases()) &&
                (isNull(this.hearing.getHasSharedResults()) || !this.hearing.getHasSharedResults())) {
            final Hearing hearingUpdated = Hearing.hearing().withValuesFrom(this.hearing)
                    .withProsecutionCases(this.hearing.getProsecutionCases().stream().map(pc -> ProsecutionCase.prosecutionCase().withValuesFrom(pc)
                            .withDefendants(pc.getDefendants().stream().map(def -> Defendant.defendant().withValuesFrom(def)
                                    .withOffences(def.getOffences().stream().map(off -> Offence.offence().withValuesFrom(off).build()).collect(toList())).build()).collect(toList()))
                            .build()).collect(toList()))
                    .build();

            hearingUpdated.getProsecutionCases().stream()
                    .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .forEach(defendant -> {
                                if (defendant.getId().equals(hearingOffencesUpdated.getDefendantId())) {
                                    if(hearingOffencesUpdated.getUpdatedOffences() != null) {
                                        defendant.getOffences().replaceAll(offence -> hearingOffencesUpdated.getUpdatedOffences().stream().filter(o -> o.getId().equals(offence.getId())).findFirst().orElse(offence) );
                                    }
                                    if(hearingOffencesUpdated.getNewOffences() != null){
                                        defendant.getOffences().addAll(hearingOffencesUpdated.getNewOffences());
                                    }
                                    defendant.getOffences().sort(Comparator.comparing(o -> ofNullable(o.getOrderIndex()).orElse(0)));
                                }
                            }
                    );
            // these offence was added to case, so I need to mark them.
            ofNullable(hearingOffencesUpdated.getNewOffences()).orElseGet(ArrayList::new).forEach(offence -> newOffences.add(offence.getId()));

            updateHearingWithSeedingHearings(hearingUpdated);
        }
    }

    private Hearing filterOutYouth(final Hearing filteredOutHearing) {
        if (isNotEmpty(filteredOutHearing.getProsecutionCases())) {
            filteredOutHearing.getProsecutionCases()
                    .forEach(prosecutionCase -> prosecutionCase.getDefendants().removeIf(this::isDefendantYouth));
            filteredOutHearing.getProsecutionCases().removeIf(prosecutionCase -> prosecutionCase.getDefendants().isEmpty());
        }

        if (isNotEmpty(filteredOutHearing.getCourtApplications())) {
            filteredOutHearing.getCourtApplications().removeIf(courtApplication -> isMasterDefendantYouth(courtApplication.getSubject().getMasterDefendant()));
        }
        if (isEmpty(filteredOutHearing.getProsecutionCases()) || isEmpty(filteredOutHearing.getCourtApplications())) {
            return Hearing.hearing().withValuesFrom(filteredOutHearing)
                    .withProsecutionCases(isEmpty(filteredOutHearing.getProsecutionCases()) ? null : filteredOutHearing.getProsecutionCases())
                    .withCourtApplications(isEmpty(filteredOutHearing.getCourtApplications()) ? null : filteredOutHearing.getCourtApplications())
                    .build();
        } else {
            return filteredOutHearing;
        }
    }

    private boolean isMasterDefendantYouth(final MasterDefendant defendant) {
        return nonNull(defendant) && nonNull(defendant.getIsYouth()) && defendant.getIsYouth();
    }

    private boolean isDefendantYouth(final Defendant defendant) {
        return nonNull(defendant.getIsYouth()) && defendant.getIsYouth();
    }

    public Stream<Object> listNewHearing(final UUID hearingId, final CourtHearingRequest courtHearingRequest, final Boolean sendNotificationToParties) {
        return apply(Stream.of(ListHearingRequested.listHearingRequested()
                .withHearingId(hearingId)
                .withListNewHearing(courtHearingRequest)
                .withSendNotificationToParties(sendNotificationToParties)
                .build()));
    }

    public Stream<Object> updateHearing(final UpdateHearingFromHmi updateHearingFromHmi) {
        if (isUnAllocated(updateHearingFromHmi)) {
            return apply(Stream.of(HearingMovedToUnallocated.hearingMovedToUnallocated()
                    .withHearing(convertHearingToUnAllocated(updateHearingFromHmi, this.hearing))
                    .build()));
        }
        return Stream.empty();
    }

    private Hearing convertHearingToUnAllocated(final UpdateHearingFromHmi updateHearingFromHmi, final Hearing hearing) {
        final Hearing.Builder builder = Hearing.hearing().withValuesFrom(hearing);

        if (nonNull(updateHearingFromHmi.getStartDate())) {
            builder.withHearingDays(hearing.getHearingDays().stream()
                    .map(hearingDay -> HearingDay.hearingDay().withValuesFrom(hearingDay)
                            .withCourtRoomId(updateHearingFromHmi.getCourtRoomId())
                            .build())
                    .collect(toList()));
        } else {
            builder.withHearingDays(null);
        }
        builder.withJudiciary(null);

        builder.withCourtCentre(CourtCentre.courtCentre().withValuesFrom(hearing.getCourtCentre())
                .withRoomId(updateHearingFromHmi.getCourtRoomId())
                .withAddress(null)
                .withCode(null)
                .withLja(null)
                .withRoomId(null)
                .withRoomName(null)
                .build());

        if (nonNull(hearing.getProsecutionCases())) {
            builder.withProsecutionCases(hearing.getProsecutionCases().stream()
                    .map(prosecutionCase -> ProsecutionCase.prosecutionCase()
                            .withValuesFrom(prosecutionCase)
                            .withDefendants(prosecutionCase.getDefendants().stream()
                                    .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                            .withOffences(defendant.getOffences().stream()
                                                    .map(offence -> Offence.offence().withValuesFrom(offence)
                                                            .withListingNumber(null)
                                                            .build())
                                                    .collect(toList()))
                                            .build())
                                    .collect(toList()))
                            .build())
                    .collect(toList()));
        }

        return builder.build();
    }

    private boolean isUnAllocated(final UpdateHearingFromHmi updateHearingFromHmi) {
        if (isNull(updateHearingFromHmi.getStartDate()) && nonNull(hearing.getHearingDays())) {
            return true;
        }
        if (isNull(updateHearingFromHmi.getCourtRoomId()) && ofNullable(hearing.getHearingDays()).map(hearingDays -> nonNull(hearingDays.get(0).getCourtRoomId())).orElse(false)) {
            return true;
        }
        return false;

    }

    private ProsecutionCasesResultedV2 createProsecutionCasesResultedV2Event(final Hearing hearing, final List<UUID> shadowListedOffences, final LocalDate hearingDay) {
        return ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withHearing(hearing)
                .withHearingDay(hearingDay)
                .build();
    }

    private List<InitiateApplicationForCaseRequested> createRequestInitiateApplicationForCaseEvents(final Hearing hearing) {
        final List<InitiateApplicationForCaseRequested> events = new ArrayList<>();
        hearing.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant -> {
                    final List<JudicialResult> judicialResults = ofNullable(defendant.getOffences()).map(Collection::stream).orElseGet(Stream::empty).filter(offence -> offence.getJudicialResults() != null)
                            .flatMap(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)).collect(toList());

                    createInitiateApplicationForCaseRequestedEvents(hearing, events, prosecutionCase, defendant, judicialResults);
                }));
        return events;
    }

    private void createInitiateApplicationForCaseRequestedEvents(final Hearing hearing, final List<InitiateApplicationForCaseRequested> events, final ProsecutionCase prosecutionCase, final Defendant defendant, final List<JudicialResult> judicialResults) {
        judicialResults.forEach(judicialResult -> {
            LOGGER.info("Priority application Types: {}", judicialResult.getJudicialResultTypeId());
            if (nonNull(judicialResult.getIsNewAmendment()) && Boolean.TRUE.equals(judicialResult.getIsNewAmendment())) {
                final NextHearing nextHearing = judicialResult.getNextHearing();
                if (nextHearing != null && nonNull(nextHearing.getIsFirstReviewHearing()) && Boolean.TRUE.equals(nextHearing.getIsFirstReviewHearing()) && nonNull(nextHearing.getApplicationTypeCode())) {
                    LOGGER.info("Next hearing populated with application Type{}", nextHearing.getApplicationTypeCode());
                    final UUID judicialResultId = judicialResult.getJudicialResultId();
                    events.add(InitiateApplicationForCaseRequested.initiateApplicationForCaseRequested()
                            .withProsecutionCase(prosecutionCase)
                            .withDefendant(defendant)
                            .withNextHearing(nextHearing)
                            .withApplicationId(randomUUID())
                            .withHearing(hearing)
                            .withResultId(judicialResultId)
                            .withOldApplicationId(this.initiatedApplicationIdsForResultIds.entrySet().stream()
                                    .filter(e -> e.getValue().equals(judicialResultId))
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .orElse(null))
                            .withIsAmended(this.initiatedApplicationIdsForResultIds.containsValue(judicialResultId))
                            .withIssueDate(this.initiatedApplicationsIssueDateForResultIds.containsKey(judicialResultId) ? this.initiatedApplicationsIssueDateForResultIds.get(judicialResultId) : LocalDate.now())
                            .build());
                }
            }
        });
    }

    /**
     * This method aims to delete applications which have been created in the previous result
     * and have been amended in the current result.
     *
     * @param hearing
     * @return
     */
    private List<DeleteApplicationForCaseRequested> createDeleteApplicationForCaseRequestedEvents(final Hearing hearing) {
        final List<DeleteApplicationForCaseRequested> events = new ArrayList<>();
        final List<JudicialResult> judicialResults = hearing.getProsecutionCases().stream().map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());

        raiseDeleteApplicationEventIfSameJudicialResultIdHasAmendment(hearing, events, judicialResults);
        raiseDeleteApplicationEventIfPreviousJudicialResultIdIsNotInTheAmendmentResult(hearing, events, judicialResults);
        return events;

    }

    /**
     * This method aims to raise events for delete applications which have been created in the previous result
     * and have been amended with the same result id in the current result.
     *
     * @param hearing
     * @param events
     * @param judicialResults
     */
    private void raiseDeleteApplicationEventIfSameJudicialResultIdHasAmendment(final Hearing hearing, final List<DeleteApplicationForCaseRequested> events, final List<JudicialResult> judicialResults) {
        judicialResults.forEach(judicialResult -> {
            if (nonNull(judicialResult.getIsNewAmendment()) && Boolean.TRUE.equals(judicialResult.getIsNewAmendment())) {
                initiatedApplicationIdsForResultIds.entrySet().stream()
                        .filter(e -> judicialResult.getJudicialResultId().equals(e.getValue()))
                        .forEach(e ->
                                events.add(DeleteApplicationForCaseRequested.deleteApplicationForCaseRequested()
                                        .withSeedingHearingId(hearing.getId())
                                        .withApplicationId(e.getKey())
                                        .build()));
            }
        });
    }

    /**
     * This method aims to raise events for delete applications which have been created in the previous result
     * and have been amended with the different result id in the current result.
     *
     *
     * @param hearing
     * @param events
     * @param judicialResults
     */
    private void raiseDeleteApplicationEventIfPreviousJudicialResultIdIsNotInTheAmendmentResult(final Hearing hearing, final List<DeleteApplicationForCaseRequested> events, final List<JudicialResult> judicialResults) {
        initiatedApplicationIdsForResultIds.entrySet().stream()
                .filter(e -> judicialResults.stream()
                        .noneMatch(j -> j.getJudicialResultId().equals(e.getValue())))
                .forEach(e ->
                        events.add(DeleteApplicationForCaseRequested.deleteApplicationForCaseRequested()
                                .withSeedingHearingId(hearing.getId())
                                .withApplicationId(e.getKey())
                                .build()));
    }

    private ProsecutionCaseDefendantListingStatusChangedV2 createListingStatusResultedEvent(final Hearing hearing) {
        return ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2().withHearing(hearing).withHearingListingStatus(HearingListingStatus.HEARING_RESULTED).withNotifyNCES(notifyNCES).build();
    }

    private HearingResulted createHearingResultedEvent(final Hearing hearing, final ZonedDateTime sharedTime, final LocalDate hearingDay) {
        final Hearing.Builder updatedHearingBuilder = Hearing.hearing();
        final List<ProsecutionCase> updatedProsecutionCases = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).map(prosecutionCase -> getUpdatedProsecutionCase(prosecutionCase, hearing.getDefendantJudicialResults())).collect(collectingAndThen(Collectors.toList(), getListOrNull()));

        updatedHearingBuilder
                .withProsecutionCases(updatedProsecutionCases)
                .withDefendantJudicialResults(hearing.getDefendantJudicialResults())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withId(hearing.getId())
                .withIsGroupProceedings(hearing.getIsGroupProceedings())
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

        final boolean hasNewOrAmendedNextHearingsOrRelatedNextHearings = doHearingContainNewOrAmendedNextHearingResults(hearing);
        final boolean hasNewOrAmendedUnscheduledNextHearings = unscheduledNextHearingsRequiredFor(hearing);
        final boolean isNextHearingDeleted = isNextHearingDeleted(hearing, this.hearing);

        if (isDeleteNextHearing(sittingDay, hasNewOrAmendedNextHearingsOrRelatedNextHearings, hasNewOrAmendedUnscheduledNextHearings, isNextHearingDeleted)) {
            events.add(DeleteNextHearingsRequested.deleteNextHearingsRequested()
                    .withHearingId(hearing.getId())
                    .withSeedingHearing(seedingHearing)
                    .build());
        }
        final boolean shouldPopulateCommittingCourt = checkResultLinesForCommittingCourt(hearing);
        final CommittingCourt court = committingCourt == null ? findCommittingCourt(hearing) : committingCourt;
        final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = bookingReferencesAndCourtScheduleIdsForHearingDay.getOrDefault(hearingDay, emptyList());


        if (hasNewOrAmendedNextHearingsOrRelatedNextHearings) {


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

        if (hasNewOrAmendedUnscheduledNextHearings) {
            events.add(UnscheduledNextHearingsRequested.unscheduledNextHearingsRequested()
                    .withSeedingHearing(seedingHearing)
                    .withHearing(hearing)
                    .build());
        }

        if(events.stream().anyMatch(event -> event instanceof DeleteNextHearingsRequested) &&
                events.stream().anyMatch(event -> event instanceof NextHearingsRequested)){
            events.removeIf(event -> event instanceof DeleteNextHearingsRequested);
            events.removeIf(event -> event instanceof NextHearingsRequested);
            DeletePreviousHearingsAndCreateNextHearing deletePreviousHearingsAndCreateNextHearing = DeletePreviousHearingsAndCreateNextHearing.deletePreviousHearingsAndCreateNextHearing()
                    .withCreateNextHearing(CreateNextHearing.createNextHearing()
                            .withHearing(hearing)
                            .withSeedingHearing(seedingHearing)
                            .withShadowListedOffences(shadowListedOffences)
                            .withCommittingCourt(shouldPopulateCommittingCourt ? court : null)
                            .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                            .build())
                    .withDeletePreviousHearings(DeletePreviousHearings.deletePreviousHearings()
                            .withHearingId(hearing.getId())
                            .withSeedingHearing(seedingHearing)
                            .build())
                    .build();
            events.add(deletePreviousHearingsAndCreateNextHearing);

        }

        return events;
    }

    public Stream<Object> requestRelatedHearingForAdhocHearing(final HearingListingNeeds hearingRequest, final Boolean sendNotificationToParties) {
        return apply(Stream.of(RelatedHearingRequestedForAdhocHearing.relatedHearingRequestedForAdhocHearing()
                .withHearingRequest(hearingRequest)
                .withSendNotificationToParties(sendNotificationToParties)
                .build()));
    }

    private boolean isDeleteNextHearing(final String sittingDay, final boolean hasNewOrAmendedNextHearingsOrRelatedNextHearings, final boolean hasNewOrAmendedUnscheduledNextHearings, final boolean isNextHearingDeleted) {
        return (hasNewOrAmendedNextHearingsOrRelatedNextHearings || hasNewOrAmendedUnscheduledNextHearings || isNextHearingDeleted)
                && hasNextHearingForHearingDay.getOrDefault(sittingDay, Boolean.FALSE);
    }

    /**
     * This method is to raise ExtendCustodyTimeLimitResulted event per offence which has extended
     * flag in the custody time limit. It finds caseId associated with offence id. 
     *
     * @param hearing
     * @param streamBuilder
     */
    private void addExtendCustodyTimeLimitResulted(final Hearing hearing, final Stream.Builder<Object> streamBuilder) {
        final List<Offence> extendedCTLOffences = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getCustodyTimeLimit()) && Boolean.TRUE.equals(offence.getCustodyTimeLimit().getIsCtlExtended()))
                .collect(toList());

        for (final Offence extendedCTLOffence : extendedCTLOffences) {
            final UUID caseId = hearing.getProsecutionCases().stream()
                    .filter(prosecutionCase -> prosecutionCase.getDefendants().stream()
                            .flatMap(defendant -> defendant.getOffences().stream())
                            .anyMatch(offence -> extendedCTLOffence.getId().equals(offence.getId())))
                    .map(ProsecutionCase::getId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Offence not found"));

            streamBuilder.add(ExtendCustodyTimeLimitResulted.extendCustodyTimeLimitResulted()
                    .withExtendedTimeLimit(extendedCTLOffence.getCustodyTimeLimit().getTimeLimit())
                    .withCaseId(caseId)
                    .withHearingId(hearing.getId())
                    .withOffenceId(extendedCTLOffence.getId())
                    .build());
        }
    }

    private List<UUID> getProsecutionCaseIds(final Hearing hearing) {
        return hearing
                .getProsecutionCases()
                .stream()
                .map(ProsecutionCase::getId)
                .collect(toList());
    }

    private List<UUID> getProsecutionCaseOffenceIds(final Hearing hearing) {
        return hearing
                .getProsecutionCases()
                .stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getId)
                .collect(toList());
    }

    private List<UUID> getCourtApplicationIds(final Hearing hearing) {
        return hearing
                .getCourtApplications()
                .stream()
                .map(CourtApplication::getId)
                .collect(toList());
    }


    private ProsecutionCase updateCaseForAdjourn(final ProsecutionCase prosecutionCase) {
        return ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream()
                                        .map(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)
                                                .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                                                .findAny()
                                                .map(judicialResult -> updateOffenceWithAdjournFromCase(defendant.getId(), offence, judicialResult.getOrderedDate(), judicialResult.getNextHearing().getAdjournmentReason()))
                                                .orElse(offence))
                                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                                .build())
                        .collect(toList()))
                .build();

    }

    private Optional<Offence> getMatchedOffenceWithGreaterOrderedDateFromCaseInState(final UUID offenceId, final LocalDate orderedDate) {
        return ofNullable(this.hearing).map(hearingFromAggregate -> ofNullable(hearingFromAggregate.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty)
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .filter(offenceFromAggregate -> offenceFromAggregate.getId().equals(offenceId))
                .filter(offenceFromAggregate -> ofNullable(offenceFromAggregate.getLastAdjournDate()).orElse(LocalDate.MIN).isAfter(orderedDate))
                .findAny();
    }

    private Offence updateOffenceWithAdjournFromCase(final UUID defendantId, final Offence offence, final LocalDate orderedDate, final String adjournmentReason) {
        final Optional<Offence> matchedOffence = getMatchedOffenceWithGreaterOrderedDateFromCaseInState(offence.getId(), orderedDate);
        final Offence updatedOffence = updateLastAdjournmentDateAndHearingType(matchedOffence, offence, orderedDate, adjournmentReason);
        return rebuildOffence(getPreviousRestrictions(defendantId, updatedOffence.getId()), updatedOffence);
    }

    private List<ReportingRestriction> getPreviousRestrictions(final UUID defendantId, final UUID offenceId) {
        return ofNullable(this.hearing).map(hearingFromAggregate -> ofNullable(hearingFromAggregate.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty)
                .map(ProsecutionCase::getDefendants)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(defendant -> defendant.getId().equals(defendantId))
                .map(Defendant::getOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(offence -> offence.getId().equals(offenceId))
                .map(Offence::getReportingRestrictions)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream).collect(toList());
    }

    private Offence rebuildOffence(final List<ReportingRestriction> previousRestrictions, final Offence updatedOffence) {
        final Offence.Builder offenceBuilder = Offence.offence().withValuesFrom(updatedOffence);
        final List<ReportingRestriction> finalreportingRestrictions = CollectionUtils.isNotEmpty(updatedOffence.getReportingRestrictions()) ? updatedOffence.getReportingRestrictions() : new ArrayList<>();
        if (CollectionUtils.isNotEmpty(previousRestrictions)) {
            finalreportingRestrictions.addAll(previousRestrictions);
        }
        if (CollectionUtils.isNotEmpty(finalreportingRestrictions)) {
            offenceBuilder.withReportingRestrictions(dedupReportingRestrictions(finalreportingRestrictions));
        }
        return offenceBuilder.build();
    }

    private Offence updateLastAdjournmentDateAndHearingType(final Optional<Offence> optOffenceFromAggregate, final Offence offence, final LocalDate orderedDate, final String adjournmentReason) {
        return optOffenceFromAggregate.map(offenceFromAggregate -> Offence.offence().withValuesFrom(offence)
                        .withLastAdjournDate(offenceFromAggregate.getLastAdjournDate())
                        .withLastAdjournedHearingType(offenceFromAggregate.getLastAdjournedHearingType())
                        .build())
                .orElseGet(() -> Offence.offence().withValuesFrom(offence)
                        .withLastAdjournDate(orderedDate)
                        .withLastAdjournedHearingType(adjournmentReason)
                        .build());
    }

    private Optional<Offence> getMatchedOffenceWithGreaterOrderedDateFromApplicationInState(final UUID offenceId, final LocalDate orderedDate) {
        return Stream.of(ofNullable(this.hearing).map(hearingFromAggregate -> ofNullable(hearingFromAggregate.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty)
                                .map(CourtApplication::getCourtApplicationCases)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .map(CourtApplicationCase::getOffences)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream),
                        ofNullable(this.hearing).map(hearingFromAggregate -> ofNullable(hearingFromAggregate.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty)
                                .map(CourtApplication::getCourtOrder)
                                .filter(Objects::nonNull)
                                .map(CourtOrder::getCourtOrderOffences)
                                .flatMap(Collection::stream)
                                .map(CourtOrderOffence::getOffence))
                .flatMap(i -> i)
                .filter(offenceFromAggregate -> offenceFromAggregate.getId().equals(offenceId))
                .filter(offenceFromAggregate -> ofNullable(offenceFromAggregate.getLastAdjournDate()).orElse(LocalDate.MIN).isAfter(orderedDate))
                .findAny();
    }

    private Offence updateOffenceWithAdjournFromApplication(final Offence offence, final JudicialResult judicialResult) {
        final Optional<Offence> matchedOffence = getMatchedOffenceWithGreaterOrderedDateFromApplicationInState(offence.getId(), judicialResult.getOrderedDate());
        return updateLastAdjournmentDateAndHearingType(matchedOffence, offence, judicialResult.getOrderedDate(), judicialResult.getNextHearing().getAdjournmentReason());
    }


    private CourtApplication updateApplicationWithAdjourn(final CourtApplication courtApplication) {
        final Optional<JudicialResult> applicationJudicialResult = ofNullable(courtApplication.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                .findAny();

        return CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withCourtApplicationCases(getCourtApplicationCasesWithUpdatedOffences(courtApplication, applicationJudicialResult))
                .withCourtOrder(getCourtOrderWithUpdatedOffences(courtApplication, applicationJudicialResult))
                .build();
    }

    private CourtOrder getCourtOrderWithUpdatedOffences(final CourtApplication courtApplication, final Optional<JudicialResult> applicationJudicialResult) {
        return ofNullable(courtApplication.getCourtOrder())
                .map(courtOrder -> CourtOrder.courtOrder()
                        .withValuesFrom(courtOrder)
                        .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence()
                                        .withValuesFrom(courtOrderOffence)
                                        .withOffence(of(courtOrderOffence.getOffence())
                                                .map(this::getOffenceWithAdjourned)
                                                .map(offence -> applicationJudicialResult.map(judicialResult -> updateOffenceWithAdjournFromApplication(offence, judicialResult)).orElse(offence))
                                                .get())
                                        .build())
                                .collect(toList()))
                        .build())
                .orElse(null);
    }

    private List<CourtApplicationCase> getCourtApplicationCasesWithUpdatedOffences(final CourtApplication courtApplication, final Optional<JudicialResult> applicationJudicialResult) {
        return ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                        .withValuesFrom(courtApplicationCase)
                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(this::getOffenceWithAdjourned)
                                .map(offence -> applicationJudicialResult.map(judicialResult -> updateOffenceWithAdjournFromApplication(offence, judicialResult)).orElse(offence))
                                .collect(collectingAndThen(Collectors.toList(), getListOrNull())))
                        .build())
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private Offence getOffenceWithAdjourned(final Offence offence) {
        return ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                .findAny()
                .map(judicialResult -> updateOffenceWithAdjournFromApplication(offence, judicialResult))
                .orElse(offence);
    }

    public void addNewDefendant(final NewDefendantAddedToHearing newDefendantAddedToHearing) {
        this.hearing.getProsecutionCases().stream().filter(prosecutionCase -> prosecutionCase.getId().equals(newDefendantAddedToHearing.getProsecutionCaseId()))
                .forEach(prosecutionCase -> prosecutionCase.getDefendants().addAll(newDefendantAddedToHearing.getDefendants()));

    }

    public HearingType getHearingType() {
        return hearing.getType();
    }

    public ZonedDateTime getHearingDate() {
        return hearing.getEarliestNextHearingDate();
    }

    public Stream<Object> generateOpaPublicListNotice(final ProsecutionCase pCaseFromCaseAggregate,
                                                      final UUID defendantId,
                                                      final LocalDate triggerDate) {
        final Prosecutor prosecutor = pCaseFromCaseAggregate.getProsecutor();
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPublicListNotice(pCaseFromCaseAggregate.getId(), defendantId, hearing, prosecutor);
        final OpaPublicListNoticeGenerated event = OpaPublicListNoticeGenerated.opaPublicListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(hearing.getId())
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .withOpaNotice(document)
                .build();

        return Stream.of(event);
    }

    public Stream<Object> generateOpaPressListNotice(final ProsecutionCase pCaseFromCaseAggregate,
                                                     final UUID defendantId,
                                                     final OnlinePleasAllocation pleasAllocation,
                                                     final LocalDate triggerDate) {
        final Prosecutor prosecutor = pCaseFromCaseAggregate.getProsecutor();
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaPressListNotice(pCaseFromCaseAggregate.getId(), defendantId, hearing, prosecutor, pleasAllocation);
        final OpaPressListNoticeGenerated event = OpaPressListNoticeGenerated.opaPressListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(hearing.getId())
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .withOpaNotice(document)
                .build();

        return Stream.of(event);
    }

    public Stream<Object> generateOpaResultListNotice(final ProsecutionCase pCaseFromCaseAggregate,
                                                      final UUID defendantId,
                                                      final LocalDate triggerDate) {
        final OpaNoticeDocument document = OpaNoticeHelper.generateOpaResultListNotice(pCaseFromCaseAggregate.getId(), defendantId, hearing);
        final OpaResultListNoticeGenerated event = OpaResultListNoticeGenerated.opaResultListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(hearing.getId())
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .withOpaNotice(document)
                .build();

        return Stream.of(event);
    }

    private boolean isYouthDefendant(final UUID defendantId) {
        return hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantId)))
                .findFirst().map(Defendant::getIsYouth).orElse(false);
    }

    public boolean isPublicListNoticeAlreadySent(final UUID defendantId, final LocalDate triggerDate) {
        return opaPublicListNoticesSent.getOrDefault(defendantId, emptySet())
                .contains(triggerDate);
    }

    public boolean isPressListNoticeAlreadySent(final UUID defendantId, final LocalDate triggerDate) {
        return opaPressListNoticesSent.getOrDefault(defendantId, emptySet())
                .contains(triggerDate);
    }

    public boolean isResultListNoticeAlreadySent(final UUID defendantId, final LocalDate triggerDate) {
        return opaResultListNoticesSent.getOrDefault(defendantId, emptySet())
                .contains(triggerDate);
    }

    private void deactivatePublicListNotice(final OpaPublicListNoticeDeactivated event) {
        opaPublicListNoticesSent.remove(event.getDefendantId());
    }

    private void deactivatePressListNotice(final OpaPressListNoticeDeactivated event) {
        opaPressListNoticesSent.remove(event.getDefendantId());
    }

    private void deactivateResultListNotice(final OpaResultListNoticeDeactivated event) {
        opaResultListNoticesSent.remove(event.getDefendantId());
    }

    private boolean isRequestDateBeforeHearingDate(final LocalDate requestDate) {
        if(ofNullable(hearing.getHearingDays()).isPresent()) {
            return hearing.getHearingDays().stream()
                    .map(HearingDay::getSittingDay)
                    .sorted()
                    .findFirst()
                    .map(ZonedDateTime::toLocalDate)
                    .filter(requestDate::isBefore)
                    .isPresent();
        } else {
            return false;
        }
    }

    public boolean checkOpaPublicListCriteria(final UUID defendantId, final LocalDate requestDate) {
        return !isYouthDefendant(defendantId)
                && !hasSharedResults()
                && isRequestDateBeforeHearingDate(requestDate);
    }

    public boolean checkOpaPressListCriteria(final LocalDate requestDate) {
        return !hasSharedResults()
                && isRequestDateBeforeHearingDate(requestDate);
    }

    public boolean checkOpaResultListCriteria(final UUID defendantId, final LocalDate requestDate) {
        return !isYouthDefendant(defendantId)
                && ofNullable(resultSharedDateTime)
                .map(ZonedDateTime::toLocalDate)
                .filter(this::isRequestDateBeforeHearingDate)
                .map(rsd -> requestDate.isBefore(rsd.plusDays(5)))
                .orElse(false);
    }

    public Stream<Object> generateDeactivateOpaPublicListNotice(final UUID caseId, final UUID defendantId, final UUID hearingId) {
        final OpaPublicListNoticeDeactivated event = OpaPublicListNoticeDeactivated.opaPublicListNoticeDeactivated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withHearingId(hearingId)
                .build();

        return Stream.of(event);
    }

    public Stream<Object> generateDeactivateOpaPressListNotice(final UUID caseId, final UUID defendantId, final UUID hearingId) {
        final OpaPressListNoticeDeactivated event = OpaPressListNoticeDeactivated.opaPressListNoticeDeactivated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withHearingId(hearingId)
                .build();

        return Stream.of(event);
    }

    public Stream<Object> generateDeactivateOpaResultListNotice(final UUID caseId, final UUID defendantId, final UUID hearingId) {
        final OpaResultListNoticeDeactivated event = OpaResultListNoticeDeactivated.opaResultListNoticeDeactivated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withHearingId(hearingId)
                .build();

        return Stream.of(event);
    }

    private boolean hasSharedResults() {
        return ofNullable(hearing.getHasSharedResults()).orElse(Boolean.FALSE);
    }

    public Stream<Object> opaPublicListNoticeSent(final UUID notificationId,
                                                  final UUID hearingId,
                                                  final UUID defendantId,
                                                  final LocalDate triggerDate) {
        return apply(Stream.of(OpaPublicListNoticeSent.opaPublicListNoticeSent()
                .withNotificationId(notificationId)
                .withHearingId(hearingId)
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .build()));
    }

    public Stream<Object> opaPressListNoticeSent(final UUID notificationId,
                                                 final UUID hearingId,
                                                 final UUID defendantId,
                                                 final LocalDate triggerDate) {
        return apply(Stream.of(OpaPressListNoticeSent.opaPressListNoticeSent()
                .withNotificationId(notificationId)
                .withHearingId(hearingId)
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .build()));
    }

    public Stream<Object> opaResultListNoticeSent(final UUID notificationId,
                                                  final UUID hearingId,
                                                  final UUID defendantId,
                                                  final LocalDate triggerDate) {
        return apply(Stream.of(OpaResultListNoticeSent.opaResultListNoticeSent()
                .withNotificationId(notificationId)
                .withHearingId(hearingId)
                .withDefendantId(defendantId)
                .withTriggerDate(triggerDate)
                .build()));
    }

    public Stream<Object> shareAllCourtDocuments(final UUID hearingId, final UUID caseId, final UUID defendantId, final UUID userGroupId, final UUID userId, final UUID sharedByUser) {
        if (isAlreadyAllDocumentShared(caseId, defendantId, userGroupId, userId)) {
            return apply(Stream.of(DuplicateShareAllCourtDocumentsRequestReceived.duplicateShareAllCourtDocumentsRequestReceived()
                    .withApplicationHearingId(hearingId)
                    .withCaseId(caseId)
                    .withDefendantId(defendantId)
                    .withUserGroupId(userGroupId)
                    .withUserId(userId)
                    .withSharedByUser(sharedByUser)
                    .withDateShared(ZonedDateTime.now())
                    .build()));
        }

        return apply(Stream.of(AllCourtDocumentsShared.allCourtDocumentsShared()
                .withApplicationHearingId(hearingId)
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withUserGroupId(userGroupId)
                .withUserId(userId)
                .withDateShared(ZonedDateTime.now())
                .withSharedByUser(sharedByUser)
                .build()));

    }

    private boolean isAlreadyAllDocumentShared(final UUID caseId, final UUID defendantId, final UUID userGroupId, final UUID userId) {
        Map<UUID, List<UUID>> allDocumentsSharedDefCaseMap;
        if (nonNull(userGroupId)) {
            allDocumentsSharedDefCaseMap = this.allDocumentsSharedWithUserGroup.get(userGroupId);
        } else {
            allDocumentsSharedDefCaseMap = this.allDocumentsSharedWithUser.get(userId);
        }

        return nonNull(allDocumentsSharedDefCaseMap) && allDocumentsSharedDefCaseMap.containsKey(caseId) && allDocumentsSharedDefCaseMap.get(caseId).contains(defendantId);
    }

    private void addPublicListNoticeSent(final OpaPublicListNoticeSent event) {
        final Set<LocalDate> sentList = opaPublicListNoticesSent.getOrDefault(event.getDefendantId(), new HashSet<>());
        sentList.add(event.getTriggerDate());

        opaPublicListNoticesSent.put(event.getDefendantId(), sentList);
    }

    private void addPressListNoticeSent(final OpaPressListNoticeSent event) {
        final Set<LocalDate> sentList = opaPressListNoticesSent.getOrDefault(event.getDefendantId(), new HashSet<>());
        sentList.add(event.getTriggerDate());

        opaPressListNoticesSent.put(event.getDefendantId(), sentList);
    }

    private void addResultListNoticeSent(final OpaResultListNoticeSent event) {
        final Set<LocalDate> sentList = opaResultListNoticesSent.getOrDefault(event.getDefendantId(), new HashSet<>());
        sentList.add(event.getTriggerDate());

        opaResultListNoticesSent.put(event.getDefendantId(), sentList);
    }

    public ZonedDateTime getLatestSittingDay() {
        if (isNotEmpty(hearing.getHearingDays()) && hearing.getHearingDays().stream()
                .anyMatch(hd -> nonNull(hd) && nonNull(hd.getSittingDay()))) {
            return hearing.getHearingDays().stream()
                    .filter(hd -> nonNull(hd) && nonNull(hd.getSittingDay()))
                    .max(Comparator.comparing(HearingDay::getSittingDay))
                    .get().getSittingDay();
        }
        return null;
    }

    public Stream<Object> moveOffencesFromHearing(final MoveOffencesFromOldNextHearing moveOffencesFromOldNextHearing) {
        // This is oldNext Hearing and newNext hearing will be created
        // So we need to move new offences to new next hearing
        // because seeded hearing does not have new offences, and it can't create new next hearing with new offences.
        final List<ProsecutionCase> prosecutionCases = ofNullable(seededProsecutionCases).orElse(this.hearing.getProsecutionCases());
        if(isEmpty(prosecutionCases)){
            return Stream.empty();
        }

        final boolean hasOneCase = prosecutionCases.size() == 1 && prosecutionCases.get(0).getDefendants().stream()
                .flatMap(def -> def.getOffences().stream())
                .noneMatch(off -> isNull(off.getSeedingHearing()) && !newOffences.contains(off.getId()) ); // the offence extended from another hearing.

        final List<ProsecutionCase> seededCases;
        if(hasOneCase) {
            seededCases =prosecutionCases.stream().toList();
        } else {
            seededCases = prosecutionCases.stream()
                    .filter(pc -> ! pc.getDefendants().stream().flatMap(def -> def.getOffences().stream())
                            .filter(offence -> nonNull(offence.getSeedingHearing()))
                            .filter(offence -> offence.getSeedingHearing().getSeedingHearingId().equals(moveOffencesFromOldNextHearing.getSeedingHearingId()))
                            .findFirst().isEmpty())
                    .toList();
        }

        return apply(Stream.of(OffencesMovedToNewNextHearing.offencesMovedToNewNextHearing()
                        .withHearingId(moveOffencesFromOldNextHearing.getNewHearingId())
                        .withSeededCase(seededCases.stream().map(pc -> SeededCase.seededCase()
                                .withId(pc.getId())
                                .withSeededDefendants(pc.getDefendants().stream()
                                        .filter(def -> def.getOffences().stream()
                                                .anyMatch(off -> nonNull(off.getSeedingHearing())))
                                        .filter(def -> def.getOffences().stream()
                                                .anyMatch(off -> newOffences.contains(off.getId())))
                                        .map(def -> SeededDefendant.seededDefendant()
                                                .withId(def.getId())
                                                .withSeededOffences(def.getOffences().stream().filter(off -> newOffences.contains(off.getId())).toList())
                                                .build())
                                        .collect(collectingAndThen(toList(), l -> l.isEmpty() || hasOneCase ? null : l)))
                                .withNewDefendants(pc.getDefendants().stream()
                                        .filter(def -> hasOneCase || def.getOffences().stream()
                                                .allMatch(off -> newOffences.contains(off.getId())))
                                        .collect(collectingAndThen(toList(), l -> l.isEmpty() ? null : l)))
                                .build())
                                .filter(seededCase -> nonNull(seededCase.getSeededDefendants()) || nonNull(seededCase.getNewDefendants()))
                                .collect(toList()))
                .build()).filter(event -> ! event.getSeededCase().isEmpty()).map(o -> o));
    }


    public Stream<Object> moveOffencesToHearing(final MoveOffencesToNewNextHearing moveOffencesToNewNextHearing) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        moveOffencesToNewNextHearing.getSeededCase().stream().forEach(seededCase ->
                        concatAllDefendants(seededCase).filter(def->  this.hearing.getProsecutionCases().stream()
                                        .filter(pcase -> pcase.getId().equals(seededCase.getId()))
                                        .flatMap(pcase -> pcase.getDefendants().stream())
                                        .filter(pdef -> pdef.getId().equals(def.getId()))
                                        .anyMatch(pdef -> def.getSeededOffences().stream().anyMatch( off -> notInHearingState(seededCase.getId(), pdef.getId(), off.getId()))))
                                .forEach(def -> {
                                    streamBuilder.add(AddedOffencesMovedToHearing.addedOffencesMovedToHearing()
                                            .withHearingId(moveOffencesToNewNextHearing.getHearingId())
                                            .withDefendantId(def.getId())
                                            .withIsHearingInitiateEnriched(isHearingInitiateEnriched)
                                            .withNewOffences(def.getSeededOffences().stream().filter(off -> notInHearingState(seededCase.getId(), def.getId(), off.getId())).toList())
                                            .build());
                                    if(this.isHearingInitiateEnriched){
                                        streamBuilder.add(OffencesForDefendantChanged.offencesForDefendantChanged()
                                                .withModifiedDate(LocalDate.now())
                                                .withAddedOffences(singletonList(AddedOffences.addedOffences()
                                                        .withDefendantId(def.getId())
                                                        .withProsecutionCaseId(seededCase.getId())
                                                        .withOffences(def.getSeededOffences().stream().filter(off -> notInHearingState(seededCase.getId(), def.getId(), off.getId())).toList())
                                                        .build())
                                                )
                                                .build());
                                    }
                                    streamBuilder.add(OffencesForDefendantChanged.offencesForDefendantChanged()
                                                    .withModifiedDate(LocalDate.now())
                                            .withAddedOffences(singletonList(AddedOffences.addedOffences()
                                                            .withDefendantId(def.getId())
                                                            .withProsecutionCaseId(seededCase.getId())
                                                            .withOffences(def.getSeededOffences().stream().filter(off -> notInHearingState(seededCase.getId(), def.getId(), off.getId())).toList())
                                                            .build())
                                                    )
                                            .build());
                                }
                               )
                );

        moveOffencesToNewNextHearing.getSeededCase().stream()
                .filter(seededCase -> nonNull(seededCase.getNewDefendants()))
                .filter(seededCase-> seededCase.getNewDefendants().stream().anyMatch(def ->  notInHearingState(seededCase.getId(), def.getId()) ))
                .forEach(seededCase -> streamBuilder.add(AddedDefendantsMovedToHearing.addedDefendantsMovedToHearing()
                                .withHearingId(moveOffencesToNewNextHearing.getHearingId())
                                .withProsecutionCaseId(seededCase.getId())
                                .withDefendants(seededCase.getNewDefendants().stream().filter(def ->  notInHearingState(seededCase.getId(), def.getId())).toList())
                        .build()));


        return apply(streamBuilder.build());
    }

    public Stream<Object> addCasesToHearingBdf(final UUID hearingId, final List<ProsecutionCase> cases) {
        final Stream<Object>  events = apply(Stream.of(CaseAddedToHearingBdf.caseAddedToHearingBdf()
                        .withHearingId(hearingId)
                        .withProsecutionCases(cases)
                .build()));

        return Stream.concat(Stream.concat(events, populateHearingToProbationCaseWorker()), populateHearingToVEP());
    }

    private boolean notInHearingState(final UUID caseId, final UUID defId){
        return this.hearing.getProsecutionCases().stream()
                .filter(pcase -> pcase.getId().equals(caseId))
                .flatMap(pcase -> pcase.getDefendants().stream())
                .noneMatch(pdef -> pdef.getId().equals(defId));
    }
    private boolean notInHearingState(final UUID caseId, final UUID defId, final UUID offId) {
        return  this.hearing.getProsecutionCases().stream().filter(pcase -> pcase.getId().equals(caseId))
                .flatMap(pcase -> pcase.getDefendants().stream())
                .filter(def -> def.getId().equals(defId))
                .flatMap(def-> def.getOffences().stream())
                .noneMatch(off -> off.getId().equals(offId));

    }

    private Stream<SeededDefendant> concatAllDefendants(SeededCase seededCase){
        return Stream.concat(ofNullable(seededCase.getNewDefendants()).map(Collection::stream).orElseGet(Stream::empty)
                    .map(def -> SeededDefendant.seededDefendant().withId(def.getId()).withSeededOffences(def.getOffences()).build()),
                ofNullable(seededCase.getSeededDefendants()).map(Collection::stream).orElseGet(Stream::empty));
    }

    private void addNewOffencesToHearing(final Hearing hearing) {
        if (addedOffencesMovedToHearings.isEmpty()){
            return;
        }
        addedOffencesMovedToHearings.forEach(addedOffencesMovedToHearing ->
                hearing.getProsecutionCases().stream().map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .filter(defendant -> defendant.getId().equals(addedOffencesMovedToHearing.getDefendantId()))
                        .forEach(defendant -> addedOffencesMovedToHearing.getNewOffences().stream().filter(offence -> !defendant.getOffences().stream().map(Offence::getId).toList().contains(offence.getId()))
                                .forEach(offence -> defendant.getOffences().add(offence))
                        ));
    }
}