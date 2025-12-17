package uk.gov.moj.cpp.progression.event;

import static java.util.Objects.isNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.AddRelatedReference;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"WeakerAccess", "squid:S3655", "squid:S3457", "squid:CallToDeprecatedMethod", "squid:S1612"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionEventProcessor {

    public static final String CASE_ID = "caseId";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String GUILTY = "GUILTY";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventProcessor.class.getCanonicalName());
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS = "public.progression.events.case-already-exists-in-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_PROSECUTION_CASE_CREATED = "public.progression.prosecution-case-created";
    private static final String PUBLIC_PROGRESSION_EVENTS_NOW_NOTIFICATION_SUPPRESSED = "public.progression.now-notification-suppressed";
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.events.case-already-exists-in-crown-court")
    public void publishCaseAlreadyExistsInCrownCourtEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS).apply(payload));
    }

    @Handles("progression.event.prosecution-case-created")
    public void publishProsecutionCaseCreatedEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'progression.event.prosecution-case-created:' event with payload: {}", event.toObfuscatedDebugString());
        }

        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();

        if (isNull(prosecutionCase.getGroupId())) {
            LOGGER.info("Raising public message public.progression.prosecution-case-created for Case '{}'  ", prosecutionCase.getId());
            sender.send(Enveloper.envelop(event.payload())
                    .withName(PUBLIC_PROGRESSION_EVENTS_PROSECUTION_CASE_CREATED)
                    .withMetadataFrom(event));
        }

        final String relatedUrn = prosecutionCase.getRelatedUrn();
        if (StringUtils.isNotBlank(relatedUrn)) {
            final String caseId = prosecutionCase.getId().toString();
            LOGGER.info("fire command to add the related reference urn");
            final AddRelatedReference addRelatedReference = AddRelatedReference
                    .addRelatedReference()
                    .withRelatedReference(relatedUrn)
                    .withProsecutionCaseId(UUID.fromString(caseId))
                    .build();
            final JsonObject jsonObject = objectToJsonObjectConverter.convert(addRelatedReference);
            sender.send(Enveloper.envelop(jsonObject)
                    .withName("progression.command.add-related-reference")
                    .withMetadataFrom(event));
        }

        if (isNull(prosecutionCase.getIsGroupMember()) || !prosecutionCase.getIsGroupMember()) {
            final JsonObject jsonObject = createObjectBuilder()
                    .add("prosecutionCaseId", prosecutionCase.getId().toString())
                    .build();

            sender.send(Enveloper.envelop(jsonObject)
                    .withName("progression.command.process-matched-defendants")
                    .withMetadataFrom(event));
        }
    }

    @Handles("progression.event.now-document-notification-suppressed")
    public void publishSuppressNowDocumentNotificationEvent(final JsonEnvelope event) {
        sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName(PUBLIC_PROGRESSION_EVENTS_NOW_NOTIFICATION_SUPPRESSED)
                .withMetadataFrom(event));
    }

}


