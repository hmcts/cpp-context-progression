package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdateRequested;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SummonsService;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:CallToDeprecatedMethod"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventProcessor {

    public static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    private static final String PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING = "progression.command.extend-hearing";
    static final String PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE = "progression.command-enrich-hearing-initiate";
    private static final String PRIVATE_PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTEND_HEARING =
            "progression.command.prepare-summons-data-for-extended-hearing";
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING_DEFENDANT_REQUEST_UPDATE_REQUESTED =
            "progression.command.extend-hearing-defendant-request-update-requested";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingConfirmedEventProcessor.class.getName());
    @Inject
    ProgressionService progressionService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationService notificationService;

    @Inject
    private SummonsService summonsService;

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("public.listing.hearing-confirmed")
    public void processEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.listing.hearing-confirmed event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);

        if( Objects.nonNull(hearingConfirmed.getConfirmedHearing().getExistingHearingId())){

            final Optional<JsonObject> hearingIdFromQuery =
                    progressionService.getHearing(jsonEnvelope, hearingConfirmed.getConfirmedHearing().getExistingHearingId().toString());

                    if (isHearingInitialised(hearingIdFromQuery) && canExtendHearing(hearingIdFromQuery, hearingConfirmed) ) {
                        processExtendHearing(jsonEnvelope, hearingConfirmed);
                    }
        } else {

            final Initiate hearingInitiate = Initiate.initiate()
                    .withHearing(progressionService.transformConfirmedHearing(hearingConfirmed.getConfirmedHearing(), jsonEnvelope))
                    .build();

            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, hearingInitiate);

            final List<UUID> applicationIds = hearingConfirmed.getConfirmedHearing().getCourtApplicationIds();
            final List<ConfirmedProsecutionCase> confirmedProsecutionCases = hearingConfirmed.getConfirmedHearing().getProsecutionCases();

            final Hearing hearing = hearingInitiate.getHearing();
            final ZonedDateTime hearingStartDateTime = getEarliestDate(hearing.getHearingDays());
            LOGGER.info("List of application ids {} ", applicationIds);

            final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).orElse(new ArrayList<>());

            courtApplications.forEach(courtApplication -> LOGGER.info("sending notification for Application : {}", objectToJsonObjectConverter.convert(courtApplication)));
            courtApplications.forEach(courtApplication -> notificationService.sendNotification(jsonEnvelope, UUID.randomUUID(), courtApplication, hearing.getCourtCentre(), hearingStartDateTime));

        if (CollectionUtils.isNotEmpty(applicationIds)) {
            LOGGER.info("Update application status to LISTED, associate Hearing with id: {} to Applications with ids {} and generate summons", hearing.getId(), applicationIds);
            progressionService.updateCourtApplicationStatus(jsonEnvelope, applicationIds, ApplicationStatus.LISTED);
            progressionService.linkApplicationsToHearing(jsonEnvelope, hearing, applicationIds, HearingListingStatus.HEARING_INITIALISED);
            progressionService.updateCaseStatus(jsonEnvelope, hearing, applicationIds);
            summonsService.generateSummonsPayload(jsonEnvelope, hearingConfirmed.getConfirmedHearing());
        }

            if (CollectionUtils.isNotEmpty(confirmedProsecutionCases)) {
                confirmedProsecutionCases.forEach(prosecutionCase ->
                        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING).apply(
                                CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearing.getId()).withCaseId(prosecutionCase.getId()).build()))
                );
                progressionService.prepareSummonsData(jsonEnvelope, hearingConfirmed.getConfirmedHearing());
            }

            final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);
            final JsonEnvelope hearingInitiateTransformedPayload = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE).apply(hearingInitiateCommand);

            LOGGER.info(" hearing initiate transformed payload {}", hearingInitiateTransformedPayload.toObfuscatedDebugString());


            sender.send(hearingInitiateTransformedPayload);
        }
    }

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(JsonEnvelope jsonEnvelope) {

        LOGGER.info(" hearing initiate with payload {}", jsonEnvelope.toObfuscatedDebugString());

        final Initiate hearingInitiate = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), Initiate.class);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(objectToJsonObjectConverter.convert(hearingInitiate)));
        if (CollectionUtils.isNotEmpty(hearingInitiate.getHearing().getProsecutionCases())) {
            final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts = ProsecutionCasesReferredToCourtTransformer
                    .transform(hearingInitiate, null);

            prosecutionCasesReferredToCourts.stream().forEach(prosecutionCasesReferredToCourt -> {
                final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);

                final JsonEnvelope caseReferToCourt = enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT)
                        .apply(prosecutionCasesReferredToCourtJson);

                LOGGER.info(" Prosecution Cases Referred To Courts with payload {}", caseReferToCourt.toObfuscatedDebugString());

                sender.send(caseReferToCourt);
            });
            progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);
        }


    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private boolean canExtendHearing(Optional<JsonObject> hearingIdFromQuery, HearingConfirmed hearingConfirmed) {
        final List<UUID> newCaseIds = hearingConfirmed.getConfirmedHearing().getProsecutionCases().stream().map(ConfirmedProsecutionCase::getId).collect(Collectors.toList());
        final Hearing dbHearing = jsonObjectConverter.convert(hearingIdFromQuery.get().getJsonObject("hearing"), Hearing.class);
        final List<UUID> dbCaseIds = dbHearing.getProsecutionCases().stream().map(ProsecutionCase::getId).collect(Collectors.toList());

        if(dbCaseIds.containsAll(newCaseIds)){
            LOGGER.info(" hearing with id {} cannot be extended as it has came cases {}",
                    hearingConfirmed.getConfirmedHearing().getId(), dbCaseIds);
            return false;
        }else{
            LOGGER.info(" hearing with id {} can be extended as it has different cases {}",
                    hearingConfirmed.getConfirmedHearing().getId(), dbCaseIds);
            return true;
        }
    }


    private void processExtendHearing(JsonEnvelope jsonEnvelope, HearingConfirmed hearingConfirmed) {

        LOGGER.info(" processing extend hearing for hearing id {}", hearingConfirmed.getConfirmedHearing().getExistingHearingId());

        prepareSummonsDataForExtendHearing(jsonEnvelope, hearingConfirmed);

        final Hearing incomingHearing = progressionService.transformConfirmedHearing(hearingConfirmed.getConfirmedHearing(), jsonEnvelope);
        progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, incomingHearing.getProsecutionCases());

        final HearingListingNeeds hearingListingNeeds =
                progressionService.transformHearingToHearingListingNeeds(incomingHearing, hearingConfirmed.getConfirmedHearing().getExistingHearingId());

        final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                .withHearingRequest(hearingListingNeeds)
                .withIsAdjourned(FALSE)
                .build();

        final JsonObject extendHearingCommand = objectToJsonObjectConverter.convert(extendHearing);

        final JsonEnvelope hearingExtendTransformedPayload =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING).apply(extendHearingCommand);

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(" hearing extend transformed payload {}", hearingExtendTransformedPayload.toObfuscatedDebugString());
        }

        sender.send(hearingExtendTransformedPayload);

    }

    @Handles("progression.event.extend-hearing-defendant-request-created")
    public void processExtendHearingDefendantRequestCreated(final JsonEnvelope jsonEnvelope){

        LOGGER.info(" processing 'progression.event.extend-hearing-defendant-request-created' {}", jsonEnvelope.toObfuscatedDebugString());

        final ExtendHearingDefendantRequestCreated extendHearingDefendantRequestCreated =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ExtendHearingDefendantRequestCreated.class);

        final ExtendHearingDefendantRequestUpdateRequested extendHearingDefendantRequestUpdateRequested = ExtendHearingDefendantRequestUpdateRequested
                .extendHearingDefendantRequestUpdateRequested()
                .withDefendantRequests(extendHearingDefendantRequestCreated.getDefendantRequests())
                .withConfirmedHearing(extendHearingDefendantRequestCreated.getConfirmedHearing())
                .build();

        final JsonObject extendHearingDefendantRequestUpdateRequestedJson = objectToJsonObjectConverter.convert(extendHearingDefendantRequestUpdateRequested);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING_DEFENDANT_REQUEST_UPDATE_REQUESTED)
                        .apply(extendHearingDefendantRequestUpdateRequestedJson);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    @Handles("progression.event.extend-hearing-defendant-request-updated")
    public void processExtendHearingDefendantRequestUpdated(final JsonEnvelope jsonEnvelope){

        LOGGER.info(" processing 'progression.event.extend-hearing-defendant-request-updated' {}", jsonEnvelope.toObfuscatedDebugString());

        final ExtendHearingDefendantRequestUpdated extendHearingDefendantRequestUpdated =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ExtendHearingDefendantRequestUpdated.class);

        final ConfirmedHearing confirmedHearing = extendHearingDefendantRequestUpdated.getConfirmedHearing();

        final ConfirmedHearing confirmedHearingForSummons = ConfirmedHearing.confirmedHearing()
                .withId(confirmedHearing.getExistingHearingId())
                .withCourtCentre(confirmedHearing.getCourtCentre())
                .withHearingDays(confirmedHearing.getHearingDays())
                .withProsecutionCases(confirmedHearing.getProsecutionCases())
                .build();

        progressionService.prepareSummonsData(jsonEnvelope, confirmedHearingForSummons);

    }

    private void prepareSummonsDataForExtendHearing(JsonEnvelope jsonEnvelope, HearingConfirmed hearingConfirmed) {
        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing =
                PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing()
                        .withConfirmedHearing(hearingConfirmed.getConfirmedHearing())
                        .build();

        final JsonObject prepareSummonsDataJsonObject = objectToJsonObjectConverter.convert(prepareSummonsDataForExtendedHearing);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTEND_HEARING)
                        .apply(prepareSummonsDataJsonObject);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    private boolean isHearingInitialised(Optional<JsonObject> hearingIdFromQuery) {
        if(hearingIdFromQuery.isPresent()){
            final String listingStatus = hearingIdFromQuery.get().getString("hearingListingStatus", null);
            if(Objects.nonNull(listingStatus) && listingStatus.equals(HearingListingStatus.HEARING_INITIALISED.name())){
                LOGGER.info(" hearing listing status is : {}", listingStatus);
                return true;
            }
        }
        LOGGER.info(" hearing is not found ");
        return false;
    }

}
