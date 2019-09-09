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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2629")
public class ReferenceDataOffenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataOffenceService.class);

    private static final String DOCUMENT = "document";
    private static final String DETAILS = "details";
    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";

    public static final String REFERENCEDATAOFFENCE_GET_OFFENCE = "referencedataoffences.query.offence";
    public static final String REFERENCEDATAOFFENCE_QUERY_OFFENCES = "referencedataoffences.query.offences-list";
    public static final String OFFENCE_TITLE = "title";
    public static final String WELSH_OFFENCE_TITLE = "welshoffencetitle";
    public static final String LEGISLATION = "legislation";
    public static final String LEGISLATION_WELSH = "welshlegislation";
    public static final String MODEOFTRIAL_DERIVED = "modeOfTrialDerived";
    public static final String MODEOFTRIAL_CODE = "code";
    public static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    public static final String OFFENCE_ID = "offenceId";
    public static final String OFFENCES = "offences";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;


    public Optional<JsonObject> getOffenceById(final UUID offenceId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(OFFENCE_ID, offenceId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, REFERENCEDATAOFFENCE_GET_OFFENCE).apply(payload);
        final JsonEnvelope response = requester.request(request);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATAOFFENCE_GET_OFFENCE, offenceId, response.toObfuscatedDebugString());
        }

        if (response.payload() == null) {
            return Optional.empty();
        }
        final JsonObject offencePayload = response.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject offenceDocument = offencePayload.getJsonObject(DETAILS).getJsonObject(DOCUMENT);
        jsonObjectBuilder.add(OFFENCE_TITLE, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(LEGISLATION) : StringUtils.EMPTY);
        jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(WELSH_OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION_WELSH, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(LEGISLATION_WELSH) : StringUtils.EMPTY);
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, offencePayload.getString(CJS_OFFENCE_CODE)!= null?offencePayload.getString(CJS_OFFENCE_CODE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(MODEOFTRIAL_CODE, offencePayload.getString(MODEOFTRIAL_DERIVED)!= null?offencePayload.getString(MODEOFTRIAL_DERIVED) : StringUtils.EMPTY);

        return Optional.of(jsonObjectBuilder.build());
    }

    public Optional<JsonObject> getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope envelope) {
        final JsonObject requestParameter = createObjectBuilder()
                .add("cjsoffencecode", cjsOffenceCode).build();

        LOGGER.info("cjsoffencecode {} ref data request {}", cjsOffenceCode, requestParameter);

        final JsonEnvelope offences = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, REFERENCEDATAOFFENCE_QUERY_OFFENCES)
                .apply(requestParameter));

        if (offences.payload() == null) {
            return Optional.empty();
        }

        LOGGER.info("cjsoffencecode {}  offence ref data payload {}", cjsOffenceCode, offences.toObfuscatedDebugString());

        final JsonObject offencePayload = (JsonObject) offences.payloadAsJsonObject().getJsonArray(OFFENCES).get(0);
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject offenceDocument = offencePayload.getJsonObject(DETAILS).getJsonObject(DOCUMENT);
        jsonObjectBuilder.add(OFFENCE_TITLE, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(LEGISLATION) : StringUtils.EMPTY);
        jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(WELSH_OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION_WELSH, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(LEGISLATION_WELSH) : StringUtils.EMPTY);
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, offencePayload.getString(CJS_OFFENCE_CODE)!= null?offencePayload.getString(CJS_OFFENCE_CODE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(OFFENCE_ID, offencePayload.getString(OFFENCE_ID)!= null?offencePayload.getString(OFFENCE_ID) : StringUtils.EMPTY);
        jsonObjectBuilder.add("modeOfTrial", offencePayload.getString(MODEOFTRIAL_DERIVED));
        return Optional.of(jsonObjectBuilder.build());
    }
}