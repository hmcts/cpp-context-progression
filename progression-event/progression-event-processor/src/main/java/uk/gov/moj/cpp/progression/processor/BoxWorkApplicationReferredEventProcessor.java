package uk.gov.moj.cpp.progression.processor;


import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135"})
public class BoxWorkApplicationReferredEventProcessor {

    static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxWorkApplicationReferredEventProcessor.class.getCanonicalName());

    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    ProgressionService progressionService;

    @Handles("progression.event.boxwork-application-referred")
    public void processBoxWorkApplication(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final BoxworkApplicationReferred boxworkApplicationReferred = jsonObjectToObjectConverter.convert(payload, BoxworkApplicationReferred.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received prosecution case offences updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(progressionService.transformBoxWorkApplication(boxworkApplicationReferred)).build();

        final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);

        final List<UUID> applicationIds = hearingInitiate.getHearing().getCourtApplications().stream().map(CourtApplication::getId).collect(Collectors.toList());
        progressionService.linkApplicationsToHearing(jsonEnvelope, hearingInitiate.getHearing(), applicationIds, HearingListingStatus.HEARING_INITIALISED);

        LOGGER.info(" Box work Referred  with hearing initiated with payload {}", hearingInitiateCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(hearingInitiateCommand));

        progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);

        final List<CourtApplication> courtApplications = ofNullable(hearingInitiate.getHearing().getCourtApplications()).orElse(new ArrayList<>());
        courtApplications.forEach(courtApplication -> progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.IN_PROGRESS));

        LOGGER.info(" Box work Referred with payload {}", hearingInitiateCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED).apply(hearingInitiateCommand));

    }

}
