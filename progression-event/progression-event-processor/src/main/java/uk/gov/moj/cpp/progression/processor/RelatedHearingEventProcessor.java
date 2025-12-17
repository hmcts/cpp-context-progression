package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.CasesAddedForUpdatedRelatedHearing;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateRelatedHearingCommandForAdhocHearing;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.justice.progression.courts.CaseAddedToHearingBdf;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class RelatedHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedHearingEventProcessor.class.getCanonicalName());

    private static final String PROGRESSION_COMMAND_ADD_CASES_FOR_UPDATED_RELATED_HEARING = "progression.command.add-cases-for-updated-related-hearing";
    private static final String NEW_HEARING_NOTIFICATION_TEMPLATE_NAME = "NewHearingNotification";

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

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    @Handles("progression.event.related-hearing-requested")
    public void processRelatedHearingRequested(final JsonEnvelope event) {
        final RelatedHearingRequested extendHearingRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), RelatedHearingRequested.class);
        final UpdateRelatedHearingCommand updateRelatedHearingCommand = UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                .withHearingRequest(extendHearingRequested.getHearingRequest())
                .withIsAdjourned(extendHearingRequested.getIsAdjourned())
                .withSeedingHearing(extendHearingRequested.getSeedingHearing())
                .withShadowListedOffences(extendHearingRequested.getShadowListedOffences())
                .build();
        sender.send(
                envelop(objectToJsonObjectConverter.convert(updateRelatedHearingCommand))
                        .withName("progression.command.update-related-hearing")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.related-case-requested-for-adhoc-hearing")
    public void processRelatedCaseRequestedForAdhocHearing(final JsonEnvelope event){
        sender.send(
                envelop(event.payloadAsJsonObject())
                        .withName("progression.command.request-related-hearing-for-adhoc-hearing")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.related-hearing-requested-for-adhoc-hearing")
    public void processRelatedHearingRequestedForAdhocHearing(final JsonEnvelope event) {
        final RelatedHearingRequestedForAdhocHearing extendHearingRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), RelatedHearingRequestedForAdhocHearing.class);
        final UpdateRelatedHearingCommandForAdhocHearing updateRelatedHearingCommand = UpdateRelatedHearingCommandForAdhocHearing.updateRelatedHearingCommandForAdhocHearing()
                .withHearingRequest(extendHearingRequested.getHearingRequest())
                .withSendNotificationToParties(extendHearingRequested.getSendNotificationToParties())
                .build();
        sender.send(
                envelop(objectToJsonObjectConverter.convert(updateRelatedHearingCommand))
                        .withName("progression.command.update-related-hearing-for-adhoc-hearing")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.related-hearing-updated")
    public void processRelatedHearingUpdated(final JsonEnvelope jsonEnvelope) {

        final RelatedHearingUpdated relatedHearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), RelatedHearingUpdated.class);
        final UUID hearingId = relatedHearingUpdated.getHearingRequest().getId();
        final List<CourtApplication> courtApplications = relatedHearingUpdated.getHearingRequest().getCourtApplications();
        final List<ProsecutionCase> prosecutionCases = relatedHearingUpdated.getHearingRequest().getProsecutionCases();

        if (isNotEmpty(prosecutionCases)) {

            final List<UUID> caseIds = prosecutionCases.stream()
                    .map(ProsecutionCase::getId)
                    .collect(toList());

            progressionService.linkProsecutionCasesToHearing(jsonEnvelope, hearingId, caseIds);

            // raise command to update related hearing for prosecution cases to listing context
            final UpdateRelatedHearing updateRelatedHearingForListing = UpdateRelatedHearing.updateRelatedHearing()
                    .withHearingId(hearingId)
                    .withProsecutionCases(prosecutionCases)
                    .withSeedingHearing(relatedHearingUpdated.getSeedingHearing())
                    .withShadowListedOffences(relatedHearingUpdated.getShadowListedOffences())
                    .build();

            sender.send(Enveloper.envelop(objectToJsonObjectConverter.convert(updateRelatedHearingForListing)).withName("listing.update-related-hearing").withMetadataFrom(jsonEnvelope));

            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, prosecutionCases);

        } else if (isNotEmpty(courtApplications)) {
            // DD-8648: Application related hearings
        } else {
            LOGGER.info("Court Application / Prosecution Case not found for hearing: {}", hearingId);
        }
    }

    @Handles("progression.event.related-hearing-updated-for-adhoc-hearing")
    public void processRelatedHearingUpdatedForAdhocHearing(final JsonEnvelope jsonEnvelope) {

        final uk.gov.justice.progression.courts.RelatedHearingUpdatedForAdhocHearing relatedHearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), uk.gov.justice.progression.courts.RelatedHearingUpdatedForAdhocHearing.class);
        final UUID hearingId = relatedHearingUpdated.getHearingRequest().getId();
        final List<ProsecutionCase> prosecutionCases = relatedHearingUpdated.getHearingRequest().getProsecutionCases();

        if (isNotEmpty(prosecutionCases)) {

            final List<UUID> caseIds = prosecutionCases.stream()
                    .map(ProsecutionCase::getId)
                    .collect(toList());

            progressionService.linkProsecutionCasesToHearing(jsonEnvelope, hearingId, caseIds);

            // raise command to update related hearing for prosecution cases to listing context
            final uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing updateRelatedHearingForListing = uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing.relatedHearingUpdatedForAdhocHearing()
                    .withHearingId(hearingId)
                    .withProsecutionCases(prosecutionCases)
                    .build();

            sender.send(Enveloper.envelop(objectToJsonObjectConverter.convert(updateRelatedHearingForListing)).withName("public.progression.related-hearing-updated-for-adhoc-hearing").withMetadataFrom(jsonEnvelope));

            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, prosecutionCases);

            if(ofNullable(relatedHearingUpdated.getSendNotificationToParties()).orElse(false)) {
                sendHearingNotificationsToDefenceAndProsecutor(jsonEnvelope, relatedHearingUpdated);
            }

        }
    }

    @Handles("progression.event.case-added-to-hearing-bdf")
    public void processCaseAddedToHearingBdf(final JsonEnvelope jsonEnvelope) {

        final CaseAddedToHearingBdf caseAddedToHearingBdf = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CaseAddedToHearingBdf.class);
        final UUID hearingId = caseAddedToHearingBdf.getHearingId();
        final List<ProsecutionCase> prosecutionCases = caseAddedToHearingBdf.getProsecutionCases();

        if (isNotEmpty(prosecutionCases)) {

            final List<UUID> caseIds = prosecutionCases.stream()
                    .map(ProsecutionCase::getId)
                    .toList();

            progressionService.linkProsecutionCasesToHearing(jsonEnvelope, hearingId, caseIds);

        }
    }

    @Handles("public.events.listing.cases-added-for-updated-related-hearing")
    public void handlePublicCasesAddedForUpdatedRelatedHearing(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.events.listing.cases-added-for-updated-related-hearing' event with payload: {}", jsonEnvelope.toObfuscatedDebugString());
        }
        sender.send(envelop(jsonEnvelope.payloadAsJsonObject())
                .withName(PROGRESSION_COMMAND_ADD_CASES_FOR_UPDATED_RELATED_HEARING)
                .withMetadataFrom(jsonEnvelope));

        final String hearingId = jsonEnvelope.payloadAsJsonObject().getString("hearingId");
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, fromString(hearingId));
    }

    @Handles("progression.event.cases-added-for-updated-related-hearing")
    public void processCasesAddedForUpdatedRelatedHearing(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'progression.event.cases-added-for-updated-related-hearing' event with payload: {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final CasesAddedForUpdatedRelatedHearing casesAddedForUpdatedRelatedHearing = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CasesAddedForUpdatedRelatedHearing.class);
        final UpdateRelatedHearing updateRelatedHearingForHearing = UpdateRelatedHearing.updateRelatedHearing()
                .withHearingId(casesAddedForUpdatedRelatedHearing.getHearingRequest().getId())
                .withProsecutionCases(casesAddedForUpdatedRelatedHearing.getHearingRequest().getProsecutionCases())
                .withShadowListedOffences(casesAddedForUpdatedRelatedHearing.getShadowListedOffences())
                .build();
        sender.send(Enveloper.envelop(objectToJsonObjectConverter.convert(updateRelatedHearingForHearing)).withName("hearing.update-related-hearing").withMetadataFrom(jsonEnvelope));

    }


    private void sendHearingNotificationsToDefenceAndProsecutor(final JsonEnvelope jsonEnvelope, final uk.gov.justice.progression.courts.RelatedHearingUpdatedForAdhocHearing relatedHearingUpdatedForAdhocHearing) {

        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();
        hearingNotificationInputData.setCaseIds(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getProsecutionCases().stream().map(ProsecutionCase::getId).collect(toList()));
        hearingNotificationInputData.setDefendantIds(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getProsecutionCases().stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getId)
                .collect(toList()));
        hearingNotificationInputData.setDefendantOffenceListMap(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getProsecutionCases().stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Defendant::getId, def -> def.getOffences().stream().map(Offence::getId).collect(toList()))));
        hearingNotificationInputData.setTemplateName(NEW_HEARING_NOTIFICATION_TEMPLATE_NAME);
        hearingNotificationInputData.setHearingId(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getId());
        hearingNotificationInputData.setHearingDateTime(hearingNotificationHelper.getEarliestStartDateTime(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getEarliestStartDateTime()));
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getCourtCentre().getId());
        hearingNotificationInputData.setCourtRoomId(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getCourtCentre().getRoomId());
        hearingNotificationInputData.setHearingType(relatedHearingUpdatedForAdhocHearing.getHearingRequest().getType().getDescription());

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, hearingNotificationInputData);

    }

}
