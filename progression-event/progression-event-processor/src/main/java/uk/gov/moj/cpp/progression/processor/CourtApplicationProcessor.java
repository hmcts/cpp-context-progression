package uk.gov.moj.cpp.progression.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationRejected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:CallToDeprecatedMethod"})
public class CourtApplicationProcessor {

    static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    static final String COURT_APPLICATION_REJECTED = "public.progression.court-application-rejected";
    static final String COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    static final String ADD_COURT_APPLICATION_TO_CASE = "progression.command.add-court-application-to-case";
    private static final String COURT_APPLICATION_UPDATED = "public.progression.court-application-updated";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("progression.event.court-application-created")
    public void processCourtApplicationCreated(final JsonEnvelope event) {
        final CourtApplicationCreated courtApplicationCreated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class);
        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Raising public event for create court application caseId: {}", courtApplication.getLinkedCaseId());
        }
        final JsonObject payload = Json.createObjectBuilder()
                .add("applicationId", courtApplication.getId().toString())
                .add("arn", courtApplicationCreated.getArn())
                .build();
        sender.send(enveloper.withMetadataFrom(event, COURT_APPLICATION_CREATED).apply(payload));
        final JsonObject command = Json.createObjectBuilder()
                .add("courtApplication", objectToJsonObjectConverter.convert(courtApplication))
                .build();
        sender.send(enveloper.withMetadataFrom(event, ADD_COURT_APPLICATION_TO_CASE).apply(command));
    }

    @Handles("progression.event.court-application-rejected")
    public void processCourtApplicationRejected(final JsonEnvelope event) {
        final CourtApplicationRejected courtApplicationRejected = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationRejected.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Raising public event for court application rejected caseId: {} with {}", courtApplicationRejected.getCaseId(),courtApplicationRejected.getDescription());
        }
        final JsonObject payload = Json.createObjectBuilder()
                .add("applicationId", courtApplicationRejected.getApplicationId())
                .build();
        sender.send(enveloper.withMetadataFrom(event, COURT_APPLICATION_REJECTED).apply(payload));
    }

    @Handles("progression.event.listed-court-application-changed")
    public void processCourtApplicationChanged(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.listed-court-application-changed" , event.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(event, COURT_APPLICATION_CHANGED).apply(event.payloadAsJsonObject()));
    }

    @Handles("progression.event.court-application-updated")
    public void processCourtApplicationUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.court-application-updated" , event.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(event, COURT_APPLICATION_UPDATED).apply(event.payloadAsJsonObject()));
    }

}
