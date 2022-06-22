package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;


import java.util.Collection;
import java.util.stream.Stream;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssignDefendantRequestFromCurrentHearingToExtendHearing;
import uk.gov.justice.core.courts.AssignDefendantRequestToExtendHearing;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdateRequested;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.exceptions.CourtApplicationAndCaseNotFoundException;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:CallToDeprecatedMethod"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventProcessor {

    public static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    private static final String PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING = "progression.command.extend-hearing";
    private static final String PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE = "progression.command-enrich-hearing-initiate";
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
    private PartialHearingConfirmService partialHearingConfirmService;

    @Inject
    private ListingService listingService;

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("public.listing.hearing-confirmed")
    public void processEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.listing.hearing-confirmed event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);
        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();
        final Hearing hearingInProgression = progressionService.retrieveHearing(jsonEnvelope, confirmedHearing.getId());

        triggerRetryOnMissingCaseAndApplication(confirmedHearing.getId(), hearingInProgression);

        if (nonNull(confirmedHearing.getExistingHearingId())) {
            final Optional<JsonObject> hearingIdFromQuery = progressionService.getHearing(jsonEnvelope, confirmedHearing.getExistingHearingId().toString());
            if (isHearingInitialised(hearingIdFromQuery)) {
                processExtendHearing(jsonEnvelope, confirmedHearing, hearingInProgression);
            }
        } else {

            final SeedingHearing seedingHearing = hearingInProgression.getSeedingHearing();

            final Initiate hearingInitiate = Initiate.initiate()
                    .withHearing(progressionService.transformConfirmedHearing(confirmedHearing, jsonEnvelope, seedingHearing))
                    .build();

            final List<ProsecutionCase> deltaProsecutionCases = processDeltaProsecutionCases(jsonEnvelope, confirmedHearing, hearingInProgression, seedingHearing, hearingInitiate);

            final List<UUID> applicationIds = confirmedHearing.getCourtApplicationIds();
            final List<ConfirmedProsecutionCase> confirmedProsecutionCases = confirmedHearing.getProsecutionCases();

            final Hearing hearing = hearingInitiate.getHearing();
            final ZonedDateTime hearingStartDateTime = getEarliestDate(hearing.getHearingDays());
            LOGGER.info("List of application ids {} ", applicationIds);

            final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).orElse(new ArrayList<>());

            courtApplications.forEach(courtApplication -> LOGGER.info("sending notification for Application : {}", objectToJsonObjectConverter.convert(courtApplication)));
            courtApplications.forEach(courtApplication -> notificationService.sendNotification(jsonEnvelope, UUID.randomUUID(), courtApplication, hearing.getCourtCentre(), hearingStartDateTime, hearing.getJurisdictionType()));

            if (isNotEmpty(applicationIds)) {
                LOGGER.info("Update application status to LISTED, associate Hearing with id: {} to Applications with ids {} and generate summons", hearing.getId(), applicationIds);
                progressionService.updateCourtApplicationStatus(jsonEnvelope, applicationIds, ApplicationStatus.LISTED);
                progressionService.linkApplicationsToHearing(jsonEnvelope, hearing, applicationIds, HearingListingStatus.HEARING_INITIALISED);
                progressionService.updateCaseStatus(jsonEnvelope, hearing, applicationIds);
            }

            if (isNotEmpty(confirmedProsecutionCases)) {
                confirmedProsecutionCases.forEach(prosecutionCase ->
                        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING).apply(
                                CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearing.getId()).withCaseId(prosecutionCase.getId()).build()))
                );

            }

            ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                    .forEach(prosecutionCase -> progressionService.increaseListingNumber(jsonEnvelope, prosecutionCase, hearing.getId()));


            progressionService.prepareSummonsData(jsonEnvelope, confirmedHearing);

            final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);
            final JsonEnvelope hearingInitiateTransformedPayload = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE).apply(hearingInitiateCommand);

            LOGGER.info(" hearing initiate transformed payload {}", hearingInitiateTransformedPayload.toObfuscatedDebugString());

            sender.send(hearingInitiateTransformedPayload);

            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, hearingInitiate, deltaProsecutionCases);
        }
    }


    /**
     * If partial allocation is happened in confirm process, a new list hearing request generated
     * for the left over prosecutionCases. Regarding the hearing seeded or not, it calls
     * listNextCourtHearings or listCourtHearing. And also if the hearing has related seededHearing
     * , related seededHearingsProsecutionCases is removed from deltaProsecutionCases and the
     * related seed process raises for each related seedingHearing
     *
     * @param jsonEnvelope
     * @param confirmedHearing
     * @param hearingInProgression
     * @param seedingHearing
     * @param hearingInitiate
     */
    private List<ProsecutionCase> processDeltaProsecutionCases(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final Hearing hearingInProgression, final SeedingHearing seedingHearing, final Initiate hearingInitiate) {
        final List<ProsecutionCase> deltaProsecutionCases = partialHearingConfirmService.getDifferences(confirmedHearing, hearingInProgression);

        if (isNotEmpty(deltaProsecutionCases)) {
            updateHearingForPartialAllocation(jsonEnvelope, confirmedHearing, deltaProsecutionCases);
            HearingListingNeeds newHearingForRemainingUnallocatedOffences;
            if (nonNull(seedingHearing)) {
                final List<ProsecutionCase> deltaSeededProsecutionCases = partialHearingConfirmService.getDeltaSeededProsecutionCases(confirmedHearing, hearingInProgression, seedingHearing);
                final ListNextHearings listNextHearings = partialHearingConfirmService.transformToListNextCourtHearing(deltaSeededProsecutionCases, hearingInitiate.getHearing(), hearingInProgression, seedingHearing);
                listingService.listNextCourtHearings(jsonEnvelope, listNextHearings);
                newHearingForRemainingUnallocatedOffences = listNextHearings.getHearings().get(0);
                final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap = partialHearingConfirmService.getRelatedSeedingHearingsProsecutionCasesMap(confirmedHearing, hearingInProgression, seedingHearing);
                processCommandUpdateRelatedHearing(jsonEnvelope, newHearingForRemainingUnallocatedOffences, relatedSeedingHearingsProsecutionCasesMap);

            } else {
                final ListCourtHearing listCourtHearing = partialHearingConfirmService.transformToListCourtHearing(deltaProsecutionCases, hearingInitiate.getHearing(), hearingInProgression);
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
                newHearingForRemainingUnallocatedOffences = listCourtHearing.getHearings().get(0);
            }

            assignDefendantRequestFromCurrentHearingToExtendHearing(jsonEnvelope, hearingInitiate, newHearingForRemainingUnallocatedOffences);

            progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, Collections.singletonList(newHearingForRemainingUnallocatedOffences), seedingHearing);
        }

        return deltaProsecutionCases;
    }


    /**
     * if partial allocation happened in  seededHearing exists , the method  calls
     * "command.update-related-hearing" for each related seedingHearing.
     *
     * @param jsonEnvelope
     * @param hearingListingNeed
     * @param relatedSeedingHearingsProsecutionCasesMap
     */
    private void processCommandUpdateRelatedHearing(final JsonEnvelope jsonEnvelope, final HearingListingNeeds hearingListingNeed, final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap) {
        for (final Map.Entry<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearing : relatedSeedingHearingsProsecutionCasesMap.entrySet()) {
            final UpdateRelatedHearingCommand updateRelatedHearingCommand = UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                    .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                            .withId(hearingListingNeed.getId())
                            .withProsecutionCases(relatedSeedingHearing.getValue())
                            .build())
                    .withSeedingHearing(relatedSeedingHearing.getKey())
                    .build();
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.update-related-hearing").apply(objectToJsonObjectConverter.convert(updateRelatedHearingCommand)));
        }
    }

    private void assignDefendantRequestFromCurrentHearingToExtendHearing(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate, final HearingListingNeeds hearingListingNeeds) {
        final JsonObject command = objectToJsonObjectConverter.convert(AssignDefendantRequestFromCurrentHearingToExtendHearing.assignDefendantRequestFromCurrentHearingToExtendHearing()
                .withCurrentHearingId(hearingInitiate.getHearing().getId())
                .withExtendHearingId(hearingListingNeeds.getId())
                .build());
        final JsonEnvelope payload = enveloper.withMetadataFrom(jsonEnvelope, "progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing").apply(command);
        sender.send(payload);
    }

    private void updateHearingForPartialAllocation(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final List<ProsecutionCase> deltaProsecutionCases) {
        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(confirmedHearing.getId(), deltaProsecutionCases);
        progressionService.updateHearingForPartialAllocation(jsonEnvelope, updateHearingForPartialAllocation);
    }

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(JsonEnvelope jsonEnvelope) {

        LOGGER.info(" hearing initiate with payload {}", jsonEnvelope.toObfuscatedDebugString());

        final Initiate hearingInitiate = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), Initiate.class);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(objectToJsonObjectConverter.convert(hearingInitiate)));
        if (isNotEmpty(hearingInitiate.getHearing().getProsecutionCases())) {
            final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts = ProsecutionCasesReferredToCourtTransformer
                    .transform(hearingInitiate, null);

            prosecutionCasesReferredToCourts.forEach(prosecutionCasesReferredToCourt -> {
                final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);

                final JsonEnvelope caseReferToCourt = enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT)
                        .apply(prosecutionCasesReferredToCourtJson);

                LOGGER.info(" Prosecution Cases Referred To Courts with payload {}", caseReferToCourt.toObfuscatedDebugString());

                sender.send(caseReferToCourt);
            });
            progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);
        }
        LOGGER.info("hearing-confirmed event populate hearing to probation caseworker for hearingId '{}' ", hearingInitiate.getHearing().getId());
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, hearingInitiate.getHearing().getId());
    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private void processExtendHearing(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final Hearing hearingInProgression) {
        LOGGER.info(" processing extend hearing for hearing id {}", confirmedHearing.getExistingHearingId());

        prepareSummonsDataForExtendHearing(jsonEnvelope, confirmedHearing);

        final Hearing incomingHearing = progressionService.transformToHearingFrom(confirmedHearing, jsonEnvelope);

        final HearingListingNeeds hearingListingNeeds =
                progressionService.transformHearingToHearingListingNeeds(incomingHearing, confirmedHearing.getExistingHearingId());

        final boolean isPartiallyAllocated = !partialHearingConfirmService.getDifferences(confirmedHearing, hearingInProgression).isEmpty();

        if (isPartiallyAllocated) {
            final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = partialHearingConfirmService.transformConfirmProsecutionCasesToUpdateHearingForPartialAllocation(confirmedHearing.getId(), confirmedHearing.getProsecutionCases());
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.update-hearing-for-partial-allocation").apply(objectToJsonObjectConverter.convert(updateHearingForPartialAllocation)));
        }

        ofNullable(incomingHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(prosecutionCase -> progressionService.increaseListingNumber(jsonEnvelope, prosecutionCase, hearingListingNeeds.getId()));

        final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                .withExtendedHearingFrom(confirmedHearing.getId())
                .withHearingRequest(hearingListingNeeds)
                .withIsAdjourned(FALSE)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .build();

        final JsonObject extendHearingCommand = objectToJsonObjectConverter.convert(extendHearing);

        final JsonEnvelope hearingExtendTransformedPayload =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING).apply(extendHearingCommand);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" hearing extend transformed payload {}", hearingExtendTransformedPayload.toObfuscatedDebugString());
        }

        sender.send(hearingExtendTransformedPayload);

        LOGGER.info("hearing-confirmed event populate hearing to probation caseworker for hearingId '{}' ", confirmedHearing.getId());
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, confirmedHearing.getId());
        final Boolean fullExtension = confirmedHearing.getFullExtension();
        if(nonNull(fullExtension) && fullExtension.booleanValue()) {
            progressionService.sendListingCommandToDeleteHearing(jsonEnvelope, confirmedHearing.getId());
        }
    }

    @Handles("progression.event.defendant-request-from-current-hearing-to-extend-hearing-created")
    public void processDefendantRequestFromCurrentHearingToExtendHearingCreated(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("processing 'progression.event.defendant-request-from-current-hearing-to-extend-hearing-created' {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final DefendantRequestFromCurrentHearingToExtendHearingCreated event =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantRequestFromCurrentHearingToExtendHearingCreated.class);

        final AssignDefendantRequestToExtendHearing command = AssignDefendantRequestToExtendHearing.assignDefendantRequestToExtendHearing()
                .withHearingId(event.getExtendHearingId())
                .withDefendantRequests(event.getDefendantRequests())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(command);

        final JsonEnvelope assignDefendantRequestToExtendHearingEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, "progression.command.assign-defendant-request-to-extend-hearing")
                        .apply(payload);

        sender.send(assignDefendantRequestToExtendHearingEnvelope);
    }

    @Handles("progression.event.extend-hearing-defendant-request-created")
    public void processExtendHearingDefendantRequestCreated(final JsonEnvelope jsonEnvelope) {

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
    public void processExtendHearingDefendantRequestUpdated(final JsonEnvelope jsonEnvelope) {

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

    private void prepareSummonsDataForExtendHearing(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing) {
        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing =
                PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing()
                        .withConfirmedHearing(confirmedHearing)
                        .build();

        final JsonObject prepareSummonsDataJsonObject = objectToJsonObjectConverter.convert(prepareSummonsDataForExtendedHearing);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTEND_HEARING)
                        .apply(prepareSummonsDataJsonObject);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    private boolean isHearingInitialised(final Optional<JsonObject> hearingIdFromQuery) {
        if (hearingIdFromQuery.isPresent()) {
            final String listingStatus = hearingIdFromQuery.get().getString("hearingListingStatus", null);
            if (nonNull(listingStatus) && listingStatus.equals(HearingListingStatus.HEARING_INITIALISED.name())) {
                LOGGER.info(" hearing listing status is : {}", listingStatus);
                return true;
            }
        }
        LOGGER.info(" hearing is not found ");
        return false;
    }

    private void triggerRetryOnMissingCaseAndApplication(final UUID hearingId, final Hearing hearingInProgression) {
        if (isEmpty(hearingInProgression.getCourtApplications()) && isEmpty(hearingInProgression.getProsecutionCases())) {

            throw new CourtApplicationAndCaseNotFoundException(format("Prosecution case and court application not found for hearing id : %s", hearingId));
        }
    }
}
