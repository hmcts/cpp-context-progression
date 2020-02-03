package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.BoxWorkTaskStatus;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
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
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingResulted;

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
    private static final long serialVersionUID = 202L;
    private final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
    private UUID boxWorkAssignedUserId;
    private String boxWorkTaskId;
    private BoxWorkTaskStatus boxWorkTaskStatus;
    private Hearing hearing;
    private HearingListingStatus hearingListingStatus;

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
                otherwiseDoNothing());
    }

    public Stream<Object> createSummonsData(final CourtCentre courtCentre, final ZonedDateTime hearingDateTime, final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds) {
        if (!listDefendantRequests.isEmpty()) {
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
                    .withDefendantReferralReasons(CollectionUtils.isNotEmpty(referralReasons) ? referralReasons : null)
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

        return apply(Stream.empty());
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
        return apply(Stream.empty());
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

    public Stream<Object> saveHearingResult(Hearing hearing, ZonedDateTime sharedTime) {
        LOGGER.debug("Hearing Resulted.");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(HearingResulted.hearingResulted().withHearing(hearing).withSharedTime(sharedTime).build());
        streamBuilder.add(new ProsecutionCaseDefendantListingStatusChanged(boxWorkAssignedUserId,
                boxWorkTaskId,
                boxWorkTaskStatus,
                hearing,
                HearingListingStatus.HEARING_RESULTED));
        return apply(streamBuilder.build());
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
}
