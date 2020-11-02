package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2629", "squid:CallToDeprecatedMethod"})
public class ReferenceDataOffenceService {
    public static final String REFERENCEDATAOFFENCE_GET_OFFENCE = "referencedataoffences.query.offence";
    public static final String REFERENCEDATAOFFENCE_QUERY_OFFENCES = "referencedataoffences.query.offences-list";
    public static final String OFFENCE_TITLE = "title";
    public static final String WELSH_OFFENCE_TITLE = "welshoffencetitle";
    public static final String LEGISLATION = "legislation";
    public static final String LEGISLATION_WELSH = "welshlegislation";
    public static final String MODEOFTRIAL_DERIVED = "modeOfTrialDerived";
    public static final String MODEOFTRIAL_CODE = "code";
    public static final String DVLA_CODE = "dvlaCode";
    public static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    public static final String OFFENCE_ID = "offenceId";
    public static final String OFFENCES = "offences";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataOffenceService.class);
    private static final String DOCUMENT = "document";
    private static final String DETAILS = "details";
    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";


    public Optional<JsonObject> getOffenceById(final UUID offenceId, final JsonEnvelope envelope, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder().add(OFFENCE_ID, offenceId.toString()).build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATAOFFENCE_GET_OFFENCE)
                .withMetadataFrom(envelope));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATAOFFENCE_GET_OFFENCE, offenceId, response.toObfuscatedDebugString());
        }

        if (response.payload() == null) {
            return Optional.empty();
        }
        final JsonObject offencePayload = response.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        return buildOffence(offencePayload, jsonObjectBuilder);
    }

    private Optional<JsonObject> buildOffence(final JsonObject offencePayload, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonObject offenceDocument = offencePayload.getJsonObject(DETAILS).getJsonObject(DOCUMENT);
        jsonObjectBuilder.add(OFFENCE_TITLE, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION, offenceDocument.getJsonObject(ENGLISH) != null ? offenceDocument.getJsonObject(ENGLISH).getString(LEGISLATION) : StringUtils.EMPTY);
        jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(WELSH_OFFENCE_TITLE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(LEGISLATION_WELSH, offenceDocument.getJsonObject(WELSH) != null ? offenceDocument.getJsonObject(WELSH).getString(LEGISLATION_WELSH) : StringUtils.EMPTY);
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, offencePayload.getString(CJS_OFFENCE_CODE) != null ? offencePayload.getString(CJS_OFFENCE_CODE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(MODEOFTRIAL_CODE, offencePayload.getString(MODEOFTRIAL_DERIVED) != null ? offencePayload.getString(MODEOFTRIAL_DERIVED) : StringUtils.EMPTY);
        jsonObjectBuilder.add(DVLA_CODE, offencePayload.getString(DVLA_CODE) != null ? offencePayload.getString(DVLA_CODE) : StringUtils.EMPTY);

        return Optional.of(jsonObjectBuilder.build());
    }

    public Optional<JsonObject> getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope envelope, final Requester requester) {
        final JsonObject requestParameter = createObjectBuilder()
                .add("cjsoffencecode", cjsOffenceCode).build();

        LOGGER.info("cjsoffencecode {} ref data request {}", cjsOffenceCode, requestParameter);

        final JsonEnvelope offences = requester.request(envelop(requestParameter)
                .withName(REFERENCEDATAOFFENCE_QUERY_OFFENCES)
                .withMetadataFrom(envelope));

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
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, offencePayload.getString(CJS_OFFENCE_CODE) != null ? offencePayload.getString(CJS_OFFENCE_CODE) : StringUtils.EMPTY);
        jsonObjectBuilder.add(OFFENCE_ID, offencePayload.getString(OFFENCE_ID) != null ? offencePayload.getString(OFFENCE_ID) : StringUtils.EMPTY);
        jsonObjectBuilder.add("modeOfTrial", offencePayload.getString(MODEOFTRIAL_DERIVED));
        return Optional.of(jsonObjectBuilder.build());
    }
}