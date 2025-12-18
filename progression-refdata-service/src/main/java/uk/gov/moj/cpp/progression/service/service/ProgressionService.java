package uk.gov.moj.cpp.progression.service.service;

import static java.util.Objects.isNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2221"})
public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    private static final String PROSECUTION_CASE = "progression.query.case";

    private static final String CASE_ID = "caseId";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public Optional<JsonObject> getProsecutionCase(final UUID caseId, final Envelope<?> query) {
        LOGGER.info(" Calling {} to get prosecution case for {} ", PROSECUTION_CASE, caseId);
        final JsonObject requestJson = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();
        final JsonEnvelope responseEnvelope = requester.request(envelop(requestJson)
                .withName(PROSECUTION_CASE).withMetadataFrom(query));
        if (isNull(responseEnvelope.payloadAsJsonObject()) || isNull(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"))) {
            return Optional.empty();
        }
        LOGGER.info("Got prosecution - {}", responseEnvelope.payloadAsJsonObject());
        return Optional.of(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"));
    }
}
