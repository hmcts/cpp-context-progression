package uk.gov.moj.cpp.progression.processor;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.CasesAddedForUpdatedRelatedHearing;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class RelatedHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedHearingEventProcessor.class.getCanonicalName());

    private static final String PROGRESSION_COMMAND_ADD_CASES_FOR_UPDATED_RELATED_HEARING = "progression.command.add-cases-for-updated-related-hearing";

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

    @Handles("public.events.listing.cases-added-for-updated-related-hearing")
    public void handlePublicCasesAddedForUpdatedRelatedHearing(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.events.listing.cases-added-for-updated-related-hearing' event with payload: {}", jsonEnvelope.toObfuscatedDebugString());
        }
        sender.send(envelop(jsonEnvelope.payloadAsJsonObject())
                .withName(PROGRESSION_COMMAND_ADD_CASES_FOR_UPDATED_RELATED_HEARING)
                .withMetadataFrom(jsonEnvelope));
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




}
