package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.TypeOfList;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class HearingUnscheduledListingHelper {

    private static final UUID RESULT_DEFINITION_NHCCS = UUID.fromString("fbed768b-ee95-4434-87c8-e81cbc8d24c8");
    private static final String DATE_AND_TIME_TO_BE_FIXED = "Date and time to be fixed";

    @Inject
    private ListingService listingService;

    public void processUnscheduledHearings(final JsonEnvelope event, final Hearing hearing) {

        final HearingUnscheduledListingNeeds hearingUnscheduledListingNeeds = createHearingListingNeeds(hearing);

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds = List.of(hearingUnscheduledListingNeeds);

        listingService.listUnscheduledHearings(event, ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                .withHearings(unscheduledListingNeeds)
                .build());
    }

    private HearingUnscheduledListingNeeds createHearingListingNeeds(final Hearing hearing) {
        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearing.getId())
                .withTypeOfList(TypeOfList.typeOfList()
                        .withId(RESULT_DEFINITION_NHCCS)
                        .withDescription(DATE_AND_TIME_TO_BE_FIXED)
                        .build())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withProsecutionCases(hearing.getProsecutionCases())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withJudiciary(hearing.getJudiciary())
                .withType(HearingType.hearingType()
                        .withId(hearing.getType().getId())
                        .withDescription(hearing.getType().getDescription())
                        .build())
                .withEstimatedMinutes(0)
                .withEstimatedDuration(hearing.getEstimatedDuration())
                .withCourtCentre(hearing.getCourtCentre())
                .withCourtApplications(hearing.getCourtApplications())
                .build();
    }
}
