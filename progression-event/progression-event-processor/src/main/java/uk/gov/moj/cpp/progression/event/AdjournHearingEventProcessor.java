package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.hearing.courts.HearingAdjourned;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1135", "squid:S3655", "squid:S1481", "squid:S1612", "squid:S1188", "squid:S00112", "squid:S1168"})
@ServiceComponent(EVENT_PROCESSOR)
public class AdjournHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdjournHearingEventProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Handles("public.hearing.adjourned")
    public void handleHearingAdjournedPublicEvent(final JsonEnvelope event) {
        final HearingAdjourned hearingAdjourned = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingAdjourned.class);
        hearingAdjourned.getNextHearings().forEach(nextHearing -> {
            final List<CourtApplication> courtApplications =
                    Optional.ofNullable(nextHearing.getNextHearingCourtApplicationId()).map(ids ->
                            ids.stream().map(id -> progressionService.getCourtApplicationByIdTyped(event, id.toString()).<RuntimeException>orElseThrow(
                                    () -> new RuntimeException(String.format("unknown court application: %s ", id))
                                    )
                            ).collect(Collectors.toList())).orElse(Collections.emptyList());

            final HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds()
                    .withCourtCentre(nextHearing.getCourtCentre())
                    .withEarliestStartDateTime(nextHearing.getListedStartDateTime())
                    .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                    .withEstimatedDuration(nextHearing.getEstimatedDuration())
                    .withId(UUID.randomUUID())
                    .withJurisdictionType(JurisdictionType.valueOf(nextHearing.getJurisdictionType().name()))
                    .withJudiciary(nextHearing.getJudiciary())
                    .withType(nextHearing.getType())
                    .withReportingRestrictionReason(nextHearing.getReportingRestrictionReason());

            final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = getCourtApplicationPartyListingNeeds(courtApplications, nextHearing.getHearingLanguage());
            if (CollectionUtils.isNotEmpty(courtApplicationPartyListingNeedsList)) {
                builder.withCourtApplicationPartyListingNeeds(courtApplicationPartyListingNeedsList);
            }

            if (CollectionUtils.isNotEmpty(courtApplications)) {
                builder.withCourtApplications(courtApplications);
            }

            final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                    .withHearings(Arrays.asList(builder.build()))
                    .withAdjournedFromDate(LocalDate.now())
                    .build();

            listingService.listCourtHearing(event, listCourtHearing);
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing);
        });
    }

    private List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyListingNeeds(final List<CourtApplication> courtApplications, final HearingLanguage hearingLanguage) {

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = new ArrayList<>();
        for (final CourtApplication courtApplication : courtApplications) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("getCourtApplicationPartyListingNeeds courtApplication %s applicant ", courtApplication.getId()));

            }
            if (courtApplication.getApplicant().getProsecutingAuthority() != null) {
                final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(courtApplication.getId())
                        .withCourtApplicationPartyId(courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityId())
                        .withHearingLanguageNeeds(HearingLanguage.valueOf(hearingLanguage.name()))
                        .build();
                courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
            }

            final List<CourtApplicationParty> respondents = courtApplication.getRespondents();
            if (null != respondents && !respondents.isEmpty()) {
                respondents.forEach(courtApplicationRespondent -> {
                    if (null != courtApplicationRespondent.getProsecutingAuthority()) {
                        final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                                .withCourtApplicationId(courtApplication.getId())
                                .withCourtApplicationPartyId(courtApplicationRespondent.getId())
                                .withHearingLanguageNeeds(HearingLanguage.valueOf(hearingLanguage.name()))
                                .build();
                        courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
                    }
                });
            }
        }
        return courtApplicationPartyListingNeedsList;
    }

}
