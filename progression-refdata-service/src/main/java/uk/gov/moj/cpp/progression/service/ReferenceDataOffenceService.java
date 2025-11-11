package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2629", "squid:CallToDeprecatedMethod", "squid:java:S2789"})
public class ReferenceDataOffenceService {
    public static final String REFERENCEDATAOFFENCE_GET_OFFENCE = "referencedataoffences.query.offence";
    public static final String REFERENCEDATAOFFENCE_QUERY_OFFENCES = "referencedataoffences.query.offences-list";
    public static final String OFFENCE_TITLE = "title";
    public static final String WELSH_OFFENCE_TITLE = "welshoffencetitle";
    public static final String LEGISLATION = "legislation";
    public static final String LEGISLATION_WELSH = "welshlegislation";
    public static final String MODEOFTRIAL_DERIVED = "modeOfTrialDerived";
    public static final String REPORT_RESTRICT_RESULT_CODE = "reportRestrictResultCode";
    public static final String MODE_OF_TRIAL = "modeOfTrial";
    public static final String MODEOFTRIAL_CODE = "code";
    public static final String DVLA_CODE = "dvlaCode";
    public static final String ENDORSABLE_FLAG = "endorsableFlag";
    public static final String MAX_PENALTY = "maxPenalty";
    public static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    public static final String OFFENCE_ID = "offenceId";
    public static final String OFFENCES = "offences";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataOffenceService.class);
    private static final String DOCUMENT = "document";
    private static final String DETAILS = "details";
    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";
    public static final String EX_PARTE = "exParte";


    public Optional<JsonObject> getOffenceById(final UUID offenceId, final JsonEnvelope envelope, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder().add(OFFENCE_ID, offenceId.toString()).build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATAOFFENCE_GET_OFFENCE)
                .withMetadataFrom(envelope));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATAOFFENCE_GET_OFFENCE, offenceId, response.toObfuscatedDebugString());
        }

        if (response.payload() == null || Objects.isNull(response.payload()) || response.payload() == JsonValue.NULL || JsonValue.NULL.equals(response.payload())) {
            return Optional.empty();
        }
        final JsonObject offencePayload = response.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        return buildOffence(offencePayload, jsonObjectBuilder);
    }

    private Optional<JsonObject> buildOffence(final JsonObject offencePayload, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonObject offenceDocument = offencePayload.getJsonObject(DETAILS).getJsonObject(DOCUMENT);
        jsonObjectBuilder.add(OFFENCE_TITLE, getValue(offenceDocument.getJsonObject(ENGLISH), OFFENCE_TITLE));
        jsonObjectBuilder.add(LEGISLATION, getValue(offenceDocument.getJsonObject(ENGLISH), LEGISLATION));
        jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, getValue(offenceDocument.getJsonObject(WELSH), WELSH_OFFENCE_TITLE));
        jsonObjectBuilder.add(LEGISLATION_WELSH, getValue(offenceDocument.getJsonObject(WELSH), LEGISLATION_WELSH));
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, getString(offencePayload, CJS_OFFENCE_CODE).orElse(EMPTY));
        jsonObjectBuilder.add(MODEOFTRIAL_CODE, getString(offencePayload, MODEOFTRIAL_DERIVED).orElse(EMPTY));
        jsonObjectBuilder.add(DVLA_CODE, getString(offencePayload, DVLA_CODE).orElse(EMPTY));
        jsonObjectBuilder.add(ENDORSABLE_FLAG, getBoolean(offencePayload, ENDORSABLE_FLAG).orElse(Boolean.FALSE));
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
        return Optional.of(generateOffenceJsonObject(offencePayload));
    }

    public Optional<List<JsonObject>> getMultipleOffencesByOffenceCodeList(final List<String> cjsOffenceCodes, final JsonEnvelope envelope, final Requester requester) {
        return getMultipleOffencesByOffenceCodeList(cjsOffenceCodes, envelope, requester, null);
    }

    public Optional<List<JsonObject>> getMultipleOffencesByOffenceCodeList(final List<String> cjsOffenceCodes, final JsonEnvelope envelope, final Requester requester, final Optional<String> sowRef) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("cjsoffencecode", cjsOffenceCodes.stream()
                        .collect(Collectors.joining(",")));

        if (sowRef != null) {
            jsonObjectBuilder.add("sowRef", String.valueOf(sowRef));
        }

        final JsonObject requestParameter = jsonObjectBuilder.build();

        LOGGER.info("cjsoffencecodes {} ref data request {}", cjsOffenceCodes, requestParameter);

        final JsonEnvelope offences = requester.request(envelop(requestParameter)
                .withName(REFERENCEDATAOFFENCE_QUERY_OFFENCES)
                .withMetadataFrom(envelope));

        if (offences.payloadIsNull()) {
            return Optional.empty();
        }

        final List<JsonObject> offencesJsonObject = new ArrayList<>();

        if(!offences.payloadIsNull()) {
            LOGGER.info("cjsoffencecode {}  offence ref data payload {}", cjsOffenceCodes, offences.toObfuscatedDebugString());

            offences.payloadAsJsonObject().getJsonArray(OFFENCES)
                    .forEach(offenceJsonValue -> {
                        final JsonObject offenceJsonObject = (JsonObject) offenceJsonValue;

                        offencesJsonObject.add(generateOffenceJsonObject(offenceJsonObject));
                    });
        }

        return Optional.of(offencesJsonObject);
    }

    private JsonObject generateOffenceJsonObject(final JsonObject offencePayload) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add(CJS_OFFENCE_CODE, getString(offencePayload, CJS_OFFENCE_CODE).orElse(EMPTY));
        jsonObjectBuilder.add(OFFENCE_ID, getString(offencePayload, OFFENCE_ID).orElse(EMPTY));
        jsonObjectBuilder.add(MODE_OF_TRIAL, getString(offencePayload, MODEOFTRIAL_DERIVED).orElse(EMPTY));
        jsonObjectBuilder.add(REPORT_RESTRICT_RESULT_CODE, getString(offencePayload, REPORT_RESTRICT_RESULT_CODE).orElse(EMPTY));
        jsonObjectBuilder.add(DVLA_CODE, getString(offencePayload, DVLA_CODE).orElse(EMPTY));
        jsonObjectBuilder.add(ENDORSABLE_FLAG, getBoolean(offencePayload, ENDORSABLE_FLAG).orElse(Boolean.FALSE));
        jsonObjectBuilder.add(EX_PARTE, getBoolean(offencePayload, EX_PARTE).orElse(Boolean.FALSE));
        jsonObjectBuilder.add(MAX_PENALTY, getString(offencePayload, MAX_PENALTY).orElse(EMPTY));

        if (offencePayload.containsKey(DETAILS) && offencePayload.getJsonObject(DETAILS).containsKey(DOCUMENT)) {
            final JsonObject offenceDocument = offencePayload.getJsonObject(DETAILS).getJsonObject(DOCUMENT);
            jsonObjectBuilder.add(OFFENCE_TITLE, getValue(offenceDocument.getJsonObject(ENGLISH), OFFENCE_TITLE));
            jsonObjectBuilder.add(LEGISLATION, getValue(offenceDocument.getJsonObject(ENGLISH), LEGISLATION));
            jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, getValue(offenceDocument.getJsonObject(WELSH), WELSH_OFFENCE_TITLE));
            jsonObjectBuilder.add(LEGISLATION_WELSH, getValue(offenceDocument.getJsonObject(WELSH), LEGISLATION_WELSH));
        } else {
            jsonObjectBuilder.add(OFFENCE_TITLE, getString(offencePayload, OFFENCE_TITLE).orElse(EMPTY));
            jsonObjectBuilder.add(LEGISLATION, getString(offencePayload, LEGISLATION).orElse(EMPTY));
            jsonObjectBuilder.add(WELSH_OFFENCE_TITLE, getString(offencePayload, "titleWelsh").orElse(EMPTY));
            jsonObjectBuilder.add(LEGISLATION_WELSH, getString(offencePayload, "legislationWelsh").orElse(EMPTY));
        }
        return jsonObjectBuilder.build();
    }

    private String getValue(final JsonObject jsonObject, final String key) {
        if (nonNull(jsonObject)) {
            return getString(jsonObject, key).orElse(EMPTY);
        }
        return EMPTY;
    }


}
