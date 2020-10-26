package uk.gov.moj.cpp.progression.aggregate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isAllDefendantProceedingConcluded;

import uk.gov.justice.core.courts.BoxWorkTaskStatus;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.core.courts.UnscheduledHearingRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1948", "squid:S1172"})
public class HearingAggregate implements Aggregate {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAggregate.class);
    private static final long serialVersionUID = 203L;
    private final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
    private UUID boxWorkAssignedUserId;
    private String boxWorkTaskId;
    private BoxWorkTaskStatus boxWorkTaskStatus;
    private Hearing hearing;
    private HearingListingStatus hearingListingStatus;
    private Boolean unscheduledHearingListedFromThisHearing;
    private boolean duplicate;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ProsecutionCaseDefendantListingStatusChanged.class).apply(e -> {
                    this.boxWorkAssignedUserId = e.getBoxWorkAssignedUserId();
                    this.boxWorkTaskId = e.getBoxWorkTaskId();
                    this.boxWorkTaskStatus = e.getBoxWorkTaskStatus();
                    this.hearing = e.getHearing();
                    this.hearingListingStatus = e.getHearingListingStatus();
                }),
                when(HearingResulted.class).apply(e ->
                        this.hearing = e.getHearing()
                ),
                when(HearingDefendantRequestCreated.class).apply(e -> {
                    if (!e.getDefendantRequests().isEmpty()) {
                        listDefendantRequests.addAll(e.getDefendantRequests());
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
                otherwiseDoNothing());
    }

    public Stream<Object> createSummonsData(final CourtCentre courtCentre, final ZonedDateTime hearingDateTime, final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds) {
        if (isNotEmpty(listDefendantRequests)) {
            return apply(Stream.of(SummonsDataPrepared.summonsDataPrepared()
                    .withSummonsData(
                            SummonsData.summonsData()
                                    .withHearingDateTime(hearingDateTime)
                                    .withCourtCentre(courtCentre)
                                    .withConfirmedProsecutionCaseIds(confirmedProsecutionCaseIds)
                                    .withListDefendantRequests(listDefendantRequests)
                                    .build()
                    )
                    .build()));
        }
        return null;
    }

    public Stream<Object> enrichInitiateHearing(final Hearing hearing) {
        if (!listDefendantRequests.isEmpty()) {
            final List<UUID> defendantIds = hearing.getProsecutionCases().stream()
                    .map(ProsecutionCase::getDefendants)
                    .flatMap(Collection::stream)
                    .map(Defendant::getId)
                    .collect(Collectors.toList());
            final List<ReferralReason> referralReasons = listDefendantRequests.stream()
                    .filter(listDefendantRequest -> {
                        if (Objects.nonNull(listDefendantRequest.getReferralReason())) {
                            return defendantIds.contains(listDefendantRequest.getReferralReason().getDefendantId());
                        } else {
                            return false;
                        }
                    })
                    .map(ListDefendantRequest::getReferralReason)
                    .collect(Collectors.toList());

            final Hearing enrichedHearing = Hearing.hearing()
                    .withCourtCentre(hearing.getCourtCentre())
                    .withDefenceCounsels(hearing.getDefenceCounsels())
                    .withDefendantAttendance(hearing.getDefendantAttendance())
                    .withDefendantReferralReasons(isNotEmpty(referralReasons) ? referralReasons : null)
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

    public Stream<Object> updateDefendantListingStatus(final Hearing hearing, final HearingListingStatus hearingListingStatus) {
        LOGGER.debug("Hearing with id {} and the status: {} ", hearing.getId(), hearingListingStatus);
        final ProsecutionCaseDefendantListingStatusChanged.Builder prosecutionCaseDefendantListingStatusChanged = ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged();
        if (HearingListingStatus.HEARING_RESULTED == this.hearingListingStatus) {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(HearingListingStatus.HEARING_RESULTED);
        } else {
            prosecutionCaseDefendantListingStatusChanged.withHearingListingStatus(hearingListingStatus);
        }
        prosecutionCaseDefendantListingStatusChanged.withHearing(hearing);
        if (hearing.getIsBoxHearing() != null && hearing.getIsBoxHearing()) {
            if (HearingListingStatus.HEARING_INITIALISED == hearingListingStatus) {
                prosecutionCaseDefendantListingStatusChanged.withBoxWorkTaskStatus(BoxWorkTaskStatus.IN_PROGRESS);
            }
            if (HearingListingStatus.HEARING_RESULTED == hearingListingStatus) {
                prosecutionCaseDefendantListingStatusChanged.withBoxWorkTaskStatus(BoxWorkTaskStatus.COMPLETE);

            }
        }

        return apply(Stream.of(prosecutionCaseDefendantListingStatusChanged.build()));
    }

    public Stream<Object> assignBoxworkUser(final UUID userId) {
        LOGGER.debug("assign Boxwork User");
        if (this.boxWorkTaskStatus != null) {
            return apply(Stream.of(new ProsecutionCaseDefendantListingStatusChanged(userId,
                    boxWorkTaskId,
                    boxWorkTaskStatus,
                    hearing,
                    hearingListingStatus)));
        }

        return apply(empty());
    }

    public Stream<Object> boxworkComplete() {
        LOGGER.debug("Boxwork Complete when hearing resulted");

        if (this.boxWorkTaskStatus != null) {
            return apply(Stream.of(new ProsecutionCaseDefendantListingStatusChanged(boxWorkAssignedUserId,
                    boxWorkTaskId,
                    BoxWorkTaskStatus.COMPLETE,
                    hearing,
                    hearingListingStatus)));
        }
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
        return apply(Stream.of(HearingDefendantRequestCreated.hearingDefendantRequestCreated().withDefendantRequests(listDefendantRequests).build()));
    }


    public Stream<Object> saveHearingResult(final Hearing hearing, final ZonedDateTime sharedTime) {
        LOGGER.debug("Hearing Resulted.");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final Hearing.Builder updatedHearingBuilder = Hearing.hearing();
        if (hasProsecutionCases(hearing)) {
            final List<ProsecutionCase> updatedProsecutionCases = hearing.getProsecutionCases().stream().map(prosecutionCase -> {
                final List<Defendant> updatedDefendants = new ArrayList<>();

                final boolean allDefendantProceedingConcluded = isAllDefendantProceedingConcluded(prosecutionCase, updatedDefendants);
                return ProsecutionCase.prosecutionCase()
                        .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                        .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                        .withId(prosecutionCase.getId())
                        .withDefendants(updatedDefendants)
                        .withInitiationCode(prosecutionCase.getInitiationCode())
                        .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                        .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                        .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                        .withCaseMarkers(prosecutionCase.getCaseMarkers())
                        .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                        .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                        .withRemovalReason(prosecutionCase.getRemovalReason())
                        .withCaseStatus(allDefendantProceedingConcluded ? CaseStatusEnum.INACTIVE.getDescription() : prosecutionCase.getCaseStatus())
                        .build();
            }).collect(toList());
            updatedHearingBuilder.withProsecutionCases(updatedProsecutionCases);
        }

        updatedHearingBuilder
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
                .withIsEffectiveTrial(hearing.getIsEffectiveTrial());
        streamBuilder.add(HearingResulted.hearingResulted().withHearing(updatedHearingBuilder.build()).withSharedTime(sharedTime).build());
        streamBuilder.add(new ProsecutionCaseDefendantListingStatusChanged(boxWorkAssignedUserId,
                boxWorkTaskId,
                boxWorkTaskStatus,
                hearing,
                HearingListingStatus.HEARING_RESULTED));

        return apply(streamBuilder.build());
    }

    private boolean hasProsecutionCases(final Hearing hearing) {
        return Objects.nonNull(hearing.getProsecutionCases()) && !hearing.getProsecutionCases().isEmpty();
    }


    public ProsecutionCaseDefendantListingStatusChanged getSavedListingStatusChanged() {
        if (this.boxWorkTaskStatus != null) {
            return new ProsecutionCaseDefendantListingStatusChanged(boxWorkAssignedUserId,
                    boxWorkTaskId,
                    boxWorkTaskStatus,
                    hearing,
                    hearingListingStatus);
        }
        return null;
    }

    public Stream<Object> updateListDefendantRequest(final List<ListDefendantRequest> listDefendantRequests, ConfirmedHearing confirmedHearing) {
        return apply(Stream.of(ExtendHearingDefendantRequestUpdated.extendHearingDefendantRequestUpdated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
                .build()));
    }

    public Stream<Object> createListDefendantRequest(final ConfirmedHearing confirmedHearing) {
        return apply(Stream.of(ExtendHearingDefendantRequestCreated.extendHearingDefendantRequestCreated()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(confirmedHearing)
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
}
