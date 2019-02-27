package uk.gov.moj.cpp.progression.processor;


import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.Initiate;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S3655", "squid:S2629"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventProcessor {

    static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";

    static final String HEARING_INITIATE_COMMAND = "hearing.initiate";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    ProgressionService progressionService;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingConfirmedEventProcessor.class.getName());

    @Handles("public.listing.hearing-confirmed")
    public void processEvent(JsonEnvelope jsonEnvelope) {

        LOGGER.debug("hearing confirmed Event Received metadata {} payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(progressionService.transformConfirmedHearing(hearingConfirmed.getConfirmedHearing(), jsonEnvelope))
                .build();
        final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);

        LOGGER.info(" hearing intitiate with payload {}", hearingInitiateCommand);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(hearingInitiateCommand));

        // Prepare New command with HearingConfirmed and push it to private handler that will produce event with all required information for summons generation
        progressionService.prepareSummonsData(jsonEnvelope, hearingConfirmed.getConfirmedHearing());

        final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts = ProsecutionCasesReferredToCourtTransformer
                .transform(hearingInitiate, null);

        prosecutionCasesReferredToCourts.stream().forEach(prosecutionCasesReferredToCourt -> {
            final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);
            LOGGER.info(" Prosecution Cases Referred To Courts with payload {}", prosecutionCasesReferredToCourtJson);

            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT)
                    .apply(prosecutionCasesReferredToCourtJson));
        });
        progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);

    }



}
