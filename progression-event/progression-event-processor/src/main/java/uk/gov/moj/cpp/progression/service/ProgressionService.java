package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    public static final String CASE_ID = "caseId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String PROGRESSION_QUERY_DEFENDANT = "progression.query.defendant";
    public static final String OFFENCES = "offences";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;


    public Optional<JsonObject> getDefendantByDefendantId(final String userId, final String caseId, final String defendantId ) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId).build();

        LOGGER.info("caseId {} , defendantId {},  get defendantby defendantID request {}", caseId, defendantId, requestParameter);

        final JsonEnvelope defendant = requester.request(enveloper
                .withMetadataFrom(envelopeFor(userId), PROGRESSION_QUERY_DEFENDANT)
                .apply(requestParameter));

        LOGGER.info("defendantId {} ref data payload {}", defendantId, defendant.payloadAsJsonObject());

        if (!defendant.payloadAsJsonObject().isEmpty()) {
            result = Optional.of((JsonObject) defendant.payloadAsJsonObject());
        }
        return result;
    }

    private JsonEnvelope envelopeFor(final String userId) {
        return envelopeFrom(
                metadataWithRandomUUID("to-be-replaced")
                        .withUserId(userId)
                        .build(),
                null);
    }
}
