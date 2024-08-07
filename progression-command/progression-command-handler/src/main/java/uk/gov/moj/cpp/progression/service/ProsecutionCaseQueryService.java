package uk.gov.moj.cpp.progression.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.Optional;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

public class ProsecutionCaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQueryService.class);
    public static final String CASE_ID = "caseId";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";

    @Inject
    private Enveloper enveloper;


    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;


    public Optional<JsonObject> getProsecutionCase(final JsonEnvelope envelope, final String caseId) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} , Get prosecution case detail request {}", caseId, requestParameter);

        final JsonEnvelope prosecutioncase = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_PROSECUTION_CASES)
                .apply(requestParameter));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutioncase.toObfuscatedDebugString());
        }

        if (!prosecutioncase.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(prosecutioncase.payloadAsJsonObject());
        }
        return result;
    }

}
