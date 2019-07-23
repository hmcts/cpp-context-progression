package uk.gov.moj.cpp.progression.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135"})
public class ExtendedHearingProcessor {

    private static final String APPLICATION_REFERRED_AND_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReferredToCourtEventProcessor.class.getCanonicalName());
    @Inject
    private ProgressionService progressionService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;
    @Inject
    private Enveloper enveloper;


    @Handles("progression.event.hearing-extended")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final HearingExtended hearingExtended = jsonObjectToObjectConverter.convert(payload, HearingExtended.class);
        final UUID hearingId = hearingExtended.getHearingRequest().getId();
        final Optional<JsonObject> hearingIdFromQuery = progressionService.getHearing(jsonEnvelope, hearingId.toString());

        final List<CourtApplication> courtApplications = hearingExtended.getHearingRequest().getCourtApplications();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Raising public event for hearing extended when application: {} with {}", hearingId, courtApplications);
        }
        if (Objects.nonNull(courtApplications) && hearingIdFromQuery.isPresent()) {
            final CourtApplication courtApplication = courtApplications.get(0);
            final JsonObject hearingCourtApplication = Json.createObjectBuilder()
                    .add("hearingId", hearingId.toString())
                    .add("courtApplication", objectToJsonObjectConverter.convert(courtApplication))
                    .build();
          progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.LISTED);

          final Hearing hearing = jsonObjectToObjectConverter.convert(hearingIdFromQuery.get().getJsonObject("hearing"), Hearing.class);
          final Hearing updatedHearing = updateHearingWithApplication(hearing, courtApplication);
          progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, Arrays.asList(courtApplication.getId()), HearingListingStatus.SENT_FOR_LISTING);
          sender.send(enveloper.withMetadataFrom(jsonEnvelope, APPLICATION_REFERRED_AND_HEARING_EXTENDED).apply(hearingCourtApplication));
        } else {
            LOGGER.info("Court application not found for hearing: {}", hearingId);
        }
    }

    private Hearing updateHearingWithApplication(final Hearing hearing, final CourtApplication courtApplication) {
        List<CourtApplication> courtApplications = hearing.getCourtApplications();
        if (courtApplications == null) {
            courtApplications = new ArrayList<>();
        }
        courtApplications.add(courtApplication);
        return Hearing.hearing()
                .withType(hearing.getType())
                .withCourtApplications(courtApplications)
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withJudiciary(hearing.getJudiciary())
                .withProsecutionCases(hearing.getProsecutionCases())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withId(hearing.getId())
                .withApplicantCounsels(hearing.getApplicantCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .build();
    }
}
