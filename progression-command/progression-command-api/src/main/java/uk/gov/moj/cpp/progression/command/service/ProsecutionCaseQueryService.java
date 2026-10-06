package uk.gov.moj.cpp.progression.command.service;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQueryService.class);
    private static final String CASE_ID = "caseId";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase-v2";
    private static final String PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION = "progression.query.court-proceedings-for-application";
    private static final String APPLICATION_ID = "applicationId";
    private static final String COURT_APPLICATION = "courtApplication";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public Optional<JsonObject> getProsecutionCase(final JsonEnvelope envelope, final UUID caseId) {
        Optional<JsonObject> result = empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        LOGGER.info("caseId {} , Get prosecution case detail request {}", caseId, requestParameter);

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_QUERY_PROSECUTION_CASES)
                .build();

        final JsonEnvelope prosecutionCase = requester.request(envelopeFrom(metadata, requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutionCase.toObfuscatedDebugString());
        }

        if (!prosecutionCase.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(prosecutionCase.payloadAsJsonObject());
        }
        return result;
    }

    public Optional<JsonObject> getCourtApplicationById(final UUID applicationId, final Envelope<?> envelope) {
        final JsonObject payload = createObjectBuilder().add("applicationId", applicationId.toString()).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName("progression.query.application").withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        if (!response.payload().isEmpty()) {
            return Optional.of(response.payload());
        }
        return empty();

    }

    /**
     * Returns the {@code courtApplication} of the stored initiate payload for the given application.
     * The view throws for an unknown id, so any failure is treated as "not found".
     */
    public Optional<CourtApplication> getCourtProceedingsForApplication(final UUID applicationId, final Envelope<?> envelope) {
        final JsonObject payload = createObjectBuilder().add(APPLICATION_ID, applicationId.toString()).build();
        final Envelope<JsonObject> requestEnvelope = envelop(payload)
                .withName(PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION).withMetadataFrom(envelope);
        try {
            final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
            return ofNullable(response.payload())
                    .filter(json -> json.containsKey(COURT_APPLICATION))
                    .map(json -> jsonObjectToObjectConverter.convert(json.getJsonObject(COURT_APPLICATION), CourtApplication.class));
        } catch (final RuntimeException e) {
            LOGGER.warn("applicationId {} court proceedings for application not found", applicationId, e);
            return empty();
        }
    }
}
