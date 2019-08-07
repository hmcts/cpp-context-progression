package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S1192"})
public class ReferenceDataService {

    public static final String CJSOFFENCECODE = "cjsoffencecode";
    public static final String REFERENCEDATA_QUERY_OFFENCES = "referencedata.query.offences";
    public static final String OFFENCES = "offences";
    public static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
    public static final String REFERENCEDATA_GET_COURT_CENTRE = "referencedata.get.court-centre";
    public static final String REFERENCEDATA_GET_ORGANISATION = "referencedata.query.organisation-unit.v2";
    public static final String GET_ENFORCEMENT_AREA_BY_COURT_CODE = "referencedata.query.enforcement-area";
    public static final String REFERENCEDATA_GET_DOCUMENT_TYPE = "referencedata.query.document-metadata";
    public static final String REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE = "referencedata.get-all-document-metadata";
    public static final String REFERENCEDATA_GET_OUCODE = "referencedata.query.local-justice-area-court-prosecutor-mapping-courts";
    public static final String REFERENCEDATA_GET_COURTCENTER = "referencedata.query.organisationunits";
    public static final String REFERENCEDATA_QUERY_ETHNICITIES = "referencedata.query.ethnicities";
    public static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    public static final String REFERENCEDATA_QUERY_NATIONALITIES = "referencedata.query.country-nationality";
    public static final String REFERENCEDATA_GET_REFERRAL_REASONS = "referencedata.query.referral-reasons";
    public static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";
    public static final String REFERENCEDATA_QUERY_COURT_ROOM = "referencedata.query.courtroom";
    public static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    public static final String PROSECUTOR = "shortName";
    public static final String NATIONALITY_CODE = "isoCode";
    public static final String NATIONALITY = "nationality";
    public static final String ETHNICITY_CODE = "code";
    public static final String ETHNICITY = "description";
    public static final String SHORT_NAME = "shortName";
    public static final String COUNTRY_NATIONALITY = "countryNationality";
    public static final String ID = "id";
    public static final String IDS = "ids";
    public static final String HEARING_TYPES = "hearingTypes";
    public static final String ETHNICITIES = "ethnicities";
    public static final String ORGANISATIONUNITS = "organisationunits";
    public static final String COURT_CODE_QUERY_PARAMETER = "localJusticeAreaNationalCourtCode";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
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

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("cjsoffencecode {} ref data payload {}", cjsOffenceCode, offences.toObfuscatedDebugString());
        }

        if (!offences.payloadAsJsonObject().getJsonArray(OFFENCES).isEmpty()) {
            result = Optional.of((JsonObject) offences.payloadAsJsonObject().getJsonArray(OFFENCES).get(0));
        }
        return result;
    }

    public Optional<JsonObject> getJudgeById(final UUID judgeId, final JsonEnvelope event) {
        return getJudgeByIdAsText(judgeId.toString(), event);
    }

    public Optional<JsonObject> getJudgeByIdAsText(final String judgeId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(ID, judgeId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_JUDGE).apply(payload);
        final JsonEnvelope jsonEnvelop = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.get.judge' {} received with payload {}", judgeId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        return getCourtCentreByIdAsText(courtCentreId.toString(), event);
    }

    public Optional<JsonObject> getCourtCentreByIdAsText(final String courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(ID, courtCentreId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_COURT_CENTRE).apply(payload);
        final JsonEnvelope jsonEnvelop = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.get.court-center' {} received with payload {}", courtCentreId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getOrganisationUnitById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add(ID, courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_ORGANISATION).apply(payload);
        final JsonEnvelope jsonEnvelop = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.organisation-unit.v2' {} received with payload {}", courtCentreId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getDocumentTypeData(final UUID documentTypeId, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder().add(ID, documentTypeId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_DOCUMENT_TYPE).apply(payload);
        final JsonEnvelope response = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATA_GET_DOCUMENT_TYPE, documentTypeId, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getAllDocumentsTypes(final JsonEnvelope event, final LocalDate date) {
        final JsonObject payload = Json.createObjectBuilder().add("date", date.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE).apply(payload);
        final JsonEnvelope response = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get all document metadata '{}' received with payload {} ", REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtsByPostCodeAndProsecutingAuthority(final JsonEnvelope jsonEnvelope, final String postcode, final String prosecutingAuthority) {
        final JsonObject payloadForoucode = Json.createObjectBuilder()
                .add("postcode", postcode)
                .add("prosecutingAuthority", prosecutingAuthority)
                .build();
        final JsonEnvelope requestForoucode = enveloper.withMetadataFrom(jsonEnvelope, REFERENCEDATA_GET_OUCODE).apply(payloadForoucode);
        final JsonEnvelope responseForoucode = requester.request(requestForoucode);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get oucode '{}' received with payload {} ", REFERENCEDATA_GET_OUCODE, responseForoucode.toObfuscatedDebugString());
        }
        return Optional.ofNullable(responseForoucode.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtsOrganisationUnitsByOuCode(final JsonEnvelope event, final String oucode) {

        final JsonObject payload = Json.createObjectBuilder()
                .add("oucode", oucode)
                .build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_COURTCENTER).apply(payload);
        final JsonEnvelope response = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get court center '{}' received with payload {} ", REFERENCEDATA_GET_COURTCENTER, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtRoomById(final UUID roomId, final JsonEnvelope event) {

        final JsonObject payload = Json.createObjectBuilder()
                .add(ID, roomId.toString())
                .build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_COURT_ROOM).apply(payload);
        final JsonEnvelope response = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get court room '{}' received with payload {} ", REFERENCEDATA_QUERY_COURT_ROOM, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getReferralReasons(final JsonEnvelope event) {
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_REFERRAL_REASONS).apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.request(request);
        if (response.payload() == null) {
            throw new RuntimeException("Reference Data - Referral reasons query failed.");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASONS, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getReferralReasonById(final JsonEnvelope event, final UUID id) {
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_REFERRAL_REASONS).apply(createObjectBuilder().build());
        final JsonEnvelope response = requester.request(request);
        if (response.payload() == null) {
            throw new RuntimeException("Reference Data - Referral reasons query failed.");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASONS, response.toObfuscatedDebugString());
        }
        return response.payloadAsJsonObject().getJsonArray("referralReasons").stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public CourtCentre getCourtCentre(final JsonEnvelope jsonEnvelope, final String postcode, final String prosecutionAuthorityCode) {
        LOGGER.debug("Calling referenceDataService getCourtsByPostCodeAndProsecutingAuthority with postcode : {} and prosecutionAuthorityCode : {} ", postcode, prosecutionAuthorityCode);
        final JsonObject responseOuCode = getCourtsByPostCodeAndProsecutingAuthority(jsonEnvelope, postcode, prosecutionAuthorityCode)
                .orElseThrow(RuntimeException::new);
        final String oucode = ((JsonObject) responseOuCode.getJsonArray("courts").get(0)).getString("oucode");

        LOGGER.debug("Calling referenceDataService getCourtsOrganisationUnitsByOuCode with oucode : {}  ", oucode);

        final JsonObject courtJson = getCourtsOrganisationUnitsByOuCode(jsonEnvelope, oucode)
                .orElseThrow(RuntimeException::new);
        final JsonObject courtCentreJson = (JsonObject) courtJson.getJsonArray(ORGANISATIONUNITS).get(0);

        LOGGER.debug("Found court center from reference data search : {}  ", courtCentreJson);

        return CourtCentre.courtCentre()
                .withId(fromString(courtCentreJson.getString(ID)))
                .withName(courtCentreJson.getString("oucodeL3Name", null))
                .withWelshName(courtCentreJson.getString("oucodeL3WelshName", null))
                .build();
    }

    public Optional<JsonObject> getEthinicity(final JsonEnvelope event, final UUID id) {

        LOGGER.info(" Calling {} to get ethinicity for {} ", REFERENCEDATA_QUERY_ETHNICITIES, id);
        final JsonObject payload = Json.createObjectBuilder().build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_ETHNICITIES).apply(payload);
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject().getJsonArray(ETHNICITIES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getHearingType(final JsonEnvelope event, final UUID id) {

        LOGGER.info(" Calling {} to get hearing-type for {} ", REFERENCEDATA_QUERY_HEARING_TYPES, id);
        final JsonObject payload = Json.createObjectBuilder().build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_HEARING_TYPES).apply(payload);
        final JsonEnvelope response = requester.request(request);
        return response.payloadAsJsonObject().getJsonArray(HEARING_TYPES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationality(final JsonEnvelope event, final UUID id) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, id);
        final JsonEnvelope response = getNationalityResponse(event);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationalityByNationality(final JsonEnvelope event, final String nationality) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, nationality);
        final JsonEnvelope response = getNationalityResponse(event);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(NATIONALITY).equalsIgnoreCase(nationality))
                .findFirst();
    }

    private JsonEnvelope getNationalityResponse(final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder().build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_NATIONALITIES).apply(payload);
        return requester.request(request);
    }

    public Optional<JsonObject> getProsecutor(final JsonEnvelope event, final UUID id) {

        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_PROSECUTOR, id);

        final JsonObject payload = Json.createObjectBuilder().add(ID, id.toString()).build();

        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_PROSECUTOR).apply(payload);
        final JsonEnvelope response = requester.request(request);

        if (response.payload() == null) {
            Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getJudiciariesByJudiciaryIdList(final List<UUID> judiciaryIds, final JsonEnvelope event) {
        final String judiciaryIdsStr = judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        LOGGER.info(" Calling {} to get prosecutors for judiciary ids: {} ", REFERENCEDATA_QUERY_JUDICIARIES, judiciaryIdsStr);
        final JsonObject payload = createObjectBuilder().add(IDS, judiciaryIdsStr).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_JUDICIARIES).apply(payload);
        final JsonEnvelope response = requester.request(request);
        return Optional.ofNullable(response.payloadAsJsonObject());
    }


    public JsonObject getEnforcementAreaByLjaCode(JsonEnvelope context, final String ljaCode) {
        JsonEnvelope requestEnvelope;
        JsonEnvelope jsonResultEnvelope;
        requestEnvelope = enveloper.withMetadataFrom(context, GET_ENFORCEMENT_AREA_BY_COURT_CODE)
                .apply(createObjectBuilder().add(COURT_CODE_QUERY_PARAMETER, ljaCode)
                        .build());
        jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);

        return jsonResultEnvelope.payloadAsJsonObject();
    }
}
