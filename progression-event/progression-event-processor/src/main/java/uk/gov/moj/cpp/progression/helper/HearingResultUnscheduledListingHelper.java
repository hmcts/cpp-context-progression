package uk.gov.moj.cpp.progression.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class HearingResultUnscheduledListingHelper {
    private static final UUID WCPU = UUID.fromString("0d1b161b-d6b0-4b1b-ae08-535864e4f631");
    private static final UUID WCPN = UUID.fromString("ed34136f-2a13-45a4-8d4f-27075ae3a8a9");

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultUnscheduledListingHelper.class.getName());

    @Inject
    private UnscheduledCourtHearingListTransformer unscheduledCourtHearingListTransformer;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    public void processUnscheduledCourtHearings(final JsonEnvelope event, final Hearing originalHearing) {
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds = unscheduledCourtHearingListTransformer.transformHearing(originalHearing);
        if (unscheduledListingNeeds.isEmpty()) {
            return;
        }

        LOGGER.info("Unscheduled listing , Posting {} listUnscheduledHearings for new hearings .", unscheduledListingNeeds.size());
        final ListUnscheduledCourtHearing listUnscheduledCourtHearing = ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                                                                                .withHearings(unscheduledListingNeeds)
                                                                                .build();

        listingService.listUnscheduledHearings(event, listUnscheduledCourtHearing);

        final List<Hearing> hearingList = unscheduledListingNeeds.stream()
                                                  .filter(uln -> nonNull(uln.getProsecutionCases()))
                                                  .map(uln -> convertToHearing(uln, originalHearing.getHearingDays()))
                                                  .collect(Collectors.toList());

        final Set<UUID> hearingsToBeSentNotification = getHearingIsToBeSentNotification(unscheduledListingNeeds);

        if (!hearingList.isEmpty()) {
            LOGGER.info("Unscheduled listing ,Posting {} sendUpdateDefendantListingStatusForUnscheduledListing for new hearings.", hearingList.size());
            progressionService.sendUpdateDefendantListingStatusForUnscheduledListing(event, hearingList, hearingsToBeSentNotification);
            progressionService.recordUnlistedHearing(event,originalHearing.getId(),hearingList);
        }

    }

    public boolean checksIfUnscheduledHearingNeedsToBeCreated(final Hearing hearing) {
        final boolean createUnscheduledHearing = nonNull(hearing.getCourtApplications()) && hearing.getCourtApplications().stream()
                                                                                                    .anyMatch(this::checksIfUnscheduledHearingNeedsToBeCreated);

        if (createUnscheduledHearing) {
            return true;
        }

        if (isNull(hearing.getProsecutionCases())) {
            return false;
        }

        return hearing.getProsecutionCases().stream()
                       .anyMatch(pc -> pc.getDefendants().stream()
                                               .anyMatch(d -> d.getOffences().stream()
                                                                      .anyMatch(this::checksIfUnscheduledHearingNeedsToBeCreated)));
    }

    private boolean checksIfUnscheduledHearingNeedsToBeCreated(final CourtApplication courtApplication) {
        if (isNull(courtApplication.getJudicialResults())) {
            return false;
        }

        return courtApplication.getJudicialResults().stream()
                                            .anyMatch(jr -> TRUE.equals(jr.getIsUnscheduled()) || unscheduledCourtHearingListTransformer.hasNextHearingWithDateToBeFixed(jr));
    }

    private boolean checksIfUnscheduledHearingNeedsToBeCreated(final Offence offence) {
        if (isNull(offence.getJudicialResults())) {
            return false;
        }

        return offence.getJudicialResults().stream()
                                            .anyMatch(jr -> TRUE.equals(jr.getIsUnscheduled()) || unscheduledCourtHearingListTransformer.hasNextHearingWithDateToBeFixed(jr));
    }

    private Hearing convertToHearing(final HearingUnscheduledListingNeeds unscheduledListingNeeds, final List<HearingDay> hearingDays) {
        return Hearing.hearing()
                       .withCourtCentre(unscheduledListingNeeds.getCourtCentre())
                       .withJurisdictionType(unscheduledListingNeeds.getJurisdictionType())
                       .withId(unscheduledListingNeeds.getId())
                       .withJudiciary(unscheduledListingNeeds.getJudiciary())
                       .withReportingRestrictionReason(unscheduledListingNeeds.getReportingRestrictionReason())
                       .withType(unscheduledListingNeeds.getType())
                       .withProsecutionCases(unscheduledListingNeeds.getProsecutionCases())
                       .withHearingDays(hearingDays)
                       .build();
    }

    private Set<UUID> getHearingIsToBeSentNotification(final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds){
        final Set<UUID> hearingsToBeSentNotification = new HashSet<>();


        unscheduledListingNeeds.stream().filter(unscheduledHearing -> nonNull(unscheduledHearing.getCourtApplications()))
                .forEach(unscheduledHearing -> unscheduledHearing.getCourtApplications().stream()
                .filter(application -> nonNull(application.getJudicialResults())).forEach(
                        application -> application.getJudicialResults().forEach(
                                judicialResult -> {
                                    final UUID resultTypeId = judicialResult.getJudicialResultTypeId();
                                    if (WCPU.equals(resultTypeId) || WCPN.equals(resultTypeId)) {
                                        hearingsToBeSentNotification.add(unscheduledHearing.getId());
                                    }
                                }
                        )

                ));

        unscheduledListingNeeds.stream().filter(unscheduledHearing -> nonNull(unscheduledHearing.getProsecutionCases()))
                .forEach(unscheduledHearing -> unscheduledHearing.getProsecutionCases().stream().forEach(
                pc -> pc.getDefendants().stream().forEach(
                        defendant -> defendant.getOffences().stream()
                                .filter(offence -> nonNull(offence.getJudicialResults())).forEach(
                                        offence -> offence.getJudicialResults().stream().forEach(
                                                judicialResult -> {
                                                    final UUID resultTypeId = judicialResult.getJudicialResultTypeId();
                                                    if (WCPU.equals(resultTypeId) || WCPN.equals(resultTypeId)) {
                                                        hearingsToBeSentNotification.add(unscheduledHearing.getId());
                                                    }
                                                }
                                        )
                                )
                )
        ));

        return hearingsToBeSentNotification;
    }

}
