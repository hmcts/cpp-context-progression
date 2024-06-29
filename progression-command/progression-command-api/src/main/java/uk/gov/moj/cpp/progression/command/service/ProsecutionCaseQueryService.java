package uk.gov.moj.cpp.progression.command.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
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
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public Optional<JsonObject> getProsecutionCase(final JsonEnvelope envelope, final UUID caseId) {
        Optional<JsonObject> result = Optional.empty();
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
}
