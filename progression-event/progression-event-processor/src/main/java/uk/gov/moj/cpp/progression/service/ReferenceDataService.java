package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataService {

    public static final String CJSOFFENCECODE = "cjsoffencecode";
    public static final String REFERENCEDATA_QUERY_OFFENCES = "referencedata.query.offences";
    public static final String OFFENCES = "offences";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
    private static final String REFERENCEDATA_GET_COURT_CENTRE = "referencedata.get.court-centre";
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;


    public Optional<JsonObject> getOffenceByCjsCode(final JsonEnvelope envelope, final String cjsOffenceCode) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CJSOFFENCECODE, cjsOffenceCode).build();

        LOGGER.info("cjsoffencecode {} ref data request {}", cjsOffenceCode, requestParameter);

        final JsonEnvelope offences = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, REFERENCEDATA_QUERY_OFFENCES)
                .apply(requestParameter));

        LOGGER.info("cjsoffencecode {} ref data payload {}", cjsOffenceCode, offences.payloadAsJsonObject());

        if (!offences.payloadAsJsonObject().getJsonArray(OFFENCES).isEmpty()) {
            result = Optional.of((JsonObject) offences.payloadAsJsonObject().getJsonArray(OFFENCES).get(0));
        }
        return result;
    }

    public Optional<JsonObject> getJudgeById(final UUID judgeId, final JsonEnvelope event) {
        return getJudgeByIdAsText(judgeId.toString(), event);
    }

    public Optional<JsonObject> getJudgeByIdAsText(final String judgeId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", judgeId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_JUDGE).apply(payload);
        JsonEnvelope jsonEnvelop = requester.request(request);
        LOGGER.info("'referencedata.get.judge' {} received with payload {}", judgeId, jsonEnvelop.payloadAsJsonObject());
        return Optional.of((JsonObject) jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        return getCourtCentreByIdAsText(courtCentreId.toString(), event);
    }

    public Optional<JsonObject> getCourtCentreByIdAsText(final String courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_COURT_CENTRE).apply(payload);
        JsonEnvelope jsonEnvelop = requester.request(request);
        LOGGER.info("'referencedata.get.court-center' {} received with payload {}", courtCentreId, jsonEnvelop.payloadAsJsonObject());
        return Optional.of((JsonObject) jsonEnvelop.payloadAsJsonObject());
    }
}
