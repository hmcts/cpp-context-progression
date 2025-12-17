package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.processor.HearingConfirmedEventProcessor.PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.service.ProgressionService.getEarliestDate;


import java.time.LocalDate;
import java.util.Optional;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseAddedToHearing;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135"})
public class ExtendedHearingProcessor {

    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PROGRESSION_COMMAND_PROCESS_HEARING_EXTENDED = "progression.command.process-hearing-extended";
    private static final String PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING = "progression.command-link-prosecution-cases-to-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReferredToCourtEventProcessor.class.getCanonicalName());
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING = "progression.command.extend-hearing";
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
    private PartialHearingConfirmService partialHearingConfirmService;

    @Handles("progression.event.hearing-extended")
    public void process(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final HearingExtended hearingExtended = jsonObjectToObjectConverter.convert(payload, HearingExtended.class);

        if(Optional.ofNullable(hearingExtended.getIsUnAllocatedHearing()).orElse(false)){
            LOGGER.info("we do not want to inform the world as this is unallocated hearing");
            return;
        }
        final JsonArrayBuilder shadowListedOffencesBuilder = createArrayBuilder();

        if (nonNull(hearingExtended.getShadowListedOffences())) {
            hearingExtended.getShadowListedOffences().forEach(shadowListedOffence -> shadowListedOffencesBuilder.add(shadowListedOffence.toString()));
        }

        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("hearingRequest", objectToJsonObjectConverter.convert(hearingExtended.getHearingRequest()))
                .add("shadowListedOffences", shadowListedOffencesBuilder.build())
                .build();

        sender.send(envelop(commandPayload)
                .withName(PROGRESSION_COMMAND_PROCESS_HEARING_EXTENDED)
                .withMetadataFrom(jsonEnvelope));
    }

    @Handles("progression.event.hearing-extended-processed")
    public void processed(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final HearingExtendedProcessed hearingExtendedProcessed = jsonObjectToObjectConverter.convert(payload, HearingExtendedProcessed.class);
        final UUID hearingId = hearingExtendedProcessed.getHearingRequest().getId();
        final Hearing hearing = hearingExtendedProcessed.getHearing();

        final List<CourtApplication> courtApplications = hearingExtendedProcessed.getHearingRequest().getCourtApplications();
        final List<ProsecutionCase> prosecutionCases = hearingExtendedProcessed.getHearingRequest().getProsecutionCases();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Raising public event for hearing extended when application: {} with {}", hearingId, courtApplications);
        }
        if (nonNull(courtApplications)) {
            final CourtApplication courtApplication = courtApplications.get(0);
            final JsonObject hearingCourtApplication = Json.createObjectBuilder()
                    .add("hearingId", hearingId.toString())
                    .add("courtApplication", objectToJsonObjectConverter.convert(courtApplication))
                    .build();
          progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.LISTED);

          final Hearing updatedHearing = updateHearingWithApplication(hearing, courtApplication);
          progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, Arrays.asList(courtApplication.getId()), HearingListingStatus.SENT_FOR_LISTING);
          sender.send(envelop(hearingCourtApplication).withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).withMetadataFrom(jsonEnvelope));
          progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, prosecutionCases);
        } else if (nonNull(prosecutionCases)){
            LOGGER.info("extending hearing {} for prosecution cases",hearingId);

            final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts =
                    ProsecutionCasesReferredToCourtTransformer.transform(hearingExtendedProcessed);

            prosecutionCasesReferredToCourts.forEach(prosecutionCasesReferredToCourt -> {
                final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);
                sender.send(envelop(prosecutionCasesReferredToCourtJson).withName(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT).withMetadataFrom(jsonEnvelope));
            });

            final List<UUID> caseIds = prosecutionCases.stream().map(ProsecutionCase::getId).collect(Collectors.toList());
            progressionService.linkProsecutionCasesToHearing(jsonEnvelope, hearingId, caseIds);

            // raising public event for listing and hearing
            final  uk.gov.justice.progression.courts.HearingExtended hearingExtendedEvent = uk.gov.justice.progression.courts.HearingExtended
                    .hearingExtended()
                    .withShadowListedOffences(hearingExtendedProcessed.getShadowListedOffences())
                    .withHearingId(hearingId)
                    .withProsecutionCases(prosecutionCases)
                    .build();

            final JsonObject hearingProsecutionCases =  objectToJsonObjectConverter.convert(hearingExtendedEvent);
            sender.send(envelop(hearingProsecutionCases).withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).withMetadataFrom(jsonEnvelope));
            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, prosecutionCases);
            progressionService.populateHearingToProbationCaseworker(jsonEnvelope, hearingId);
        }
    }

    @Handles("public.listing.cases-added-to-hearing")
    public void addCasesToUnAllocatedHearing(final JsonEnvelope jsonEnvelope){

        final CaseAddedToHearing caseAddedToHearing = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CaseAddedToHearing.class);

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final Hearing storedTargetHearing = progressionService.retrieveHearing(jsonEnvelope, caseAddedToHearing.getHearingId());
        linkNewCasesToHearing(jsonEnvelope, payload, caseAddedToHearing.getHearingId(), storedTargetHearing);

        final LocalDate earliestHearingDate = getEarliestDate(storedTargetHearing.getHearingDays()).toLocalDate();
        final Hearing incomingHearing = Hearing.hearing().withValuesFrom(storedTargetHearing)
                .withProsecutionCases(progressionService.transformProsecutionCase(caseAddedToHearing.getConfirmedProsecutionCase(), earliestHearingDate, jsonEnvelope, null))
                .build();

        final HearingListingNeeds hearingListingNeeds =
                progressionService.transformHearingToHearingListingNeeds(incomingHearing, caseAddedToHearing.getHearingId());
        final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                .withExtendedHearingFrom(caseAddedToHearing.getHearingId())
                .withHearingRequest(hearingListingNeeds)
                .withIsAdjourned(FALSE)
                .withIsPartiallyAllocated(true)
                .withIsUnAllocatedHearing(true)
                .build();

        final JsonObject extendHearingCommand = objectToJsonObjectConverter.convert(extendHearing);

        sender.send(envelop(extendHearingCommand)
                .withName(PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING)
                .withMetadataFrom(jsonEnvelope));
    }

    private void linkNewCasesToHearing(final JsonEnvelope jsonEnvelope, final JsonObject payload, final UUID hearingId, final Hearing storedHearing) {
        final List<UUID> cases = payload.getJsonArray("confirmedProsecutionCase").stream().map(v -> (JsonObject)v).map(caseForAdd -> fromString(caseForAdd.getString("id"))).collect(Collectors.toList());

        final List<UUID> newCases = cases.stream().filter(id -> storedHearing.getProsecutionCases().stream().noneMatch(prosecutionCase -> prosecutionCase.getId().equals(id))).collect(Collectors.toList());

        newCases.forEach(caseForAdd ->
                sender.send(envelop(createObjectBuilder().add("hearingId", hearingId.toString()).add("caseId", caseForAdd.toString()).build())
                        .withName(PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING)
                        .withMetadataFrom(jsonEnvelope))
                );
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
                .withYouthCourt(hearing.getYouthCourt())
                .withYouthCourtDefendantIds(hearing.getYouthCourtDefendantIds())
                .build();
    }

}
