package uk.gov.moj.cpp.progression.service;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S1192", "squid:CallToDeprecatedMethod"})
public class ReferenceDataService {

    public static final String CJSOFFENCECODE = "cjsoffencecode";
    public static final String REFERENCEDATA_QUERY_OFFENCES = "referencedata.query.offences";
    public static final String OFFENCES = "offences";
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String PLEA_TYPE_VALUE = "pleaValue";
    public static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
    public static final String REFERENCEDATA_GET_COURT_CENTRE = "referencedata.get.court-centre";
    public static final String REFERENCEDATA_GET_ORGANISATION = "referencedata.query.organisation-unit.v2";
    public static final String GET_ENFORCEMENT_AREA_BY_COURT_CODE = "referencedata.query.enforcement-area";
    public static final String REFERENCEDATA_GET_DOCUMENT_ACCESS = "referencedata.query.document-type-access";
    public static final String REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE = "referencedata.get-all-document-metadata";
    public static final String REFERENCEDATA_GET_OUCODE = "referencedata.query.local-justice-area-court-prosecutor-mapping-courts";
    public static final String REFERENCEDATA_GET_COURTCENTER = "referencedata.query.organisationunits";
    public static final String REFERENCEDATA_QUERY_ETHNICITIES = "referencedata.query.ethnicities";
    public static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    public static final String REFERENCEDATA_QUERY_NATIONALITIES = "referencedata.query.country-nationality";
    public static final String REFERENCEDATA_GET_REFERRAL_REASONS = "referencedata.query.referral-reasons";
    public static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";
    public static final String ID = "id";
    public static final String REFERENCEDATA_QUERY_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    public static final String REFERENCEDATA_QUERY_COURT_ROOM = "referencedata.query.courtroom";
    public static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    public static final String REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS = "referencedata.query.local-justice-areas";
    public static final String REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS = "referencedata.get-all-result-definitions";
    private static final String REFERENCEDATA_QUERY_PLEA_TYPES = "referencedata.query.plea-types";
    public static final String PROSECUTOR = "shortName";
    public static final String NATIONALITY_CODE = "isoCode";
    public static final String NATIONALITY = "nationality";
    public static final String ETHNICITY_CODE = "code";
    public static final String ETHNICITY = "description";
    public static final String SHORT_NAME = "shortName";
    public static final String COUNTRY_NATIONALITY = "countryNationality";

    public static final String IDS = "ids";
    public static final String OUCODE = "oucode";
    public static final String CPS_FLAG = "cpsFlag";
    public static final String HEARING_TYPES = "hearingTypes";
    public static final String ETHNICITIES = "ethnicities";
    public static final String ORGANISATIONUNITS = "organisationunits";
    public static final String COURT_CODE_QUERY_PARAMETER = "localJusticeAreaNationalCourtCode";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);


    public Optional<JsonObject> getOffenceByCjsCode(final JsonEnvelope envelope, final String cjsOffenceCode, final Requester requester) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CJSOFFENCECODE, cjsOffenceCode).build();

        LOGGER.info("cjsoffencecode {} ref data request {}", cjsOffenceCode, requestParameter);

        final JsonEnvelope offences = requester.request(envelop(requestParameter)
                .withName(REFERENCEDATA_QUERY_OFFENCES)
                .withMetadataFrom(envelope));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("cjsoffencecode {} ref data payload {}", cjsOffenceCode, offences.toObfuscatedDebugString());
        }

        if (!offences.payloadAsJsonObject().getJsonArray(OFFENCES).isEmpty()) {
            result = Optional.of((JsonObject) offences.payloadAsJsonObject().getJsonArray(OFFENCES).get(0));
        }
        return result;
    }

    public Optional<JsonObject> getJudgeById(final UUID judgeId, final JsonEnvelope event, final Requester requester) {
        return getJudgeByIdAsText(judgeId.toString(), event, requester);
    }

    public Optional<JsonObject> getJudgeByIdAsText(final String judgeId, final JsonEnvelope event, final Requester requester) {
        final JsonObject payload = createObjectBuilder().add(ID, judgeId).build();

        final JsonEnvelope jsonEnvelop = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_JUDGE)
                .withMetadataFrom(event));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.get.judge' {} received with payload {}", judgeId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getOrganisationUnitById(final UUID courtCentreId, final JsonEnvelope event, final Requester requester) {
        final JsonObject payload = createObjectBuilder().add(ID, courtCentreId.toString()).build();
        final Envelope<JsonObject> envelope = requester.requestAsAdmin(envelop(payload)
                .withName(REFERENCEDATA_GET_ORGANISATION)
                .withMetadataFrom(event), JsonObject.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.organisation-unit.v2' {} received with payload {}", courtCentreId, envelope.payload());
        }
        return Optional.ofNullable(envelope.payload());
    }

    public Optional<JsonObject> getDocumentTypeAccessData(final UUID documentTypeId, final JsonEnvelope event, final Requester requester) {
        final JsonObject payload = Json.createObjectBuilder().add(ID, documentTypeId.toString()).build();
        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_DOCUMENT_ACCESS)
                .withMetadataFrom(event));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" '{}' by id {} received with payload {} ", REFERENCEDATA_GET_DOCUMENT_ACCESS, documentTypeId, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }


    public Optional<JsonObject> getAllDocumentsTypes(final JsonEnvelope event, final LocalDate date, final Requester requester) {
        final JsonObject payload = Json.createObjectBuilder().add("date", date.toString()).build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE)
                .withMetadataFrom(event));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get all document metadata '{}' received with payload {} ", REFERENCEDATA_GET_ALL_DOCUMENTS_TYPE, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtsByPostCodeAndProsecutingAuthority(final JsonEnvelope jsonEnvelope, final String postcode, final String prosecutingAuthority, final Requester requester) {
        final JsonObject payloadForoucode = Json.createObjectBuilder()
                .add("postcode", postcode)
                .add("prosecutingAuthority", prosecutingAuthority)
                .build();
        final JsonEnvelope responseForoucode = requester.request(envelop(payloadForoucode)
                .withName(REFERENCEDATA_GET_OUCODE)
                .withMetadataFrom(jsonEnvelope));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get oucode '{}' received with payload {} ", REFERENCEDATA_GET_OUCODE, responseForoucode.toObfuscatedDebugString());
        }
        return Optional.ofNullable(responseForoucode.payloadAsJsonObject());
    }

    public Optional<JsonObject> getLocalJusticeArea(final JsonEnvelope jsonEnvelope, final String ljaCode, final Requester requester) {
        final JsonObject payloadForLjaCode = Json.createObjectBuilder()
                .add("nationalCourtCode", ljaCode)
                .build();
        final Envelope<JsonObject> requestForLocalJusticeArea = envelop(payloadForLjaCode).withName(REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS).withMetadataFrom(jsonEnvelope);
        final JsonEnvelope responseForLocalJusticeArea = requester.request(requestForLocalJusticeArea);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Get ljaCode '{}' received with payload {} ", REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS, responseForLocalJusticeArea.toObfuscatedDebugString());
        }
        return Optional.of(responseForLocalJusticeArea.payloadAsJsonObject());
    }


    public Optional<JsonObject> getCourtsOrganisationUnitsByOuCode(final JsonEnvelope event, final String oucode, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder()
                .add("oucode", oucode)
                .build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_COURTCENTER)
                .withMetadataFrom(event));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get court center '{}' received with payload {} ", REFERENCEDATA_GET_COURTCENTER, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtRoomById(final UUID roomId, final JsonEnvelope event, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder()
                .add(ID, roomId.toString())
                .build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_COURT_ROOM)
                .withMetadataFrom(event));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get court room '{}' received with payload {} ", REFERENCEDATA_QUERY_COURT_ROOM, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getReferralReasons(final JsonEnvelope event, final Requester requester) {
        final JsonEnvelope response = requester.request(envelop(createObjectBuilder().build())
                .withName(REFERENCEDATA_GET_REFERRAL_REASONS)
                .withMetadataFrom(event));

        if (response.payload() == null) {
            throw new RuntimeException("Reference Data - Referral reasons query failed.");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASONS, response.toObfuscatedDebugString());
        }
        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getReferralReasonById(final JsonEnvelope event, final UUID id, final Requester requester) {
        final JsonEnvelope response = requester.request(envelop(createObjectBuilder().build())
                .withName(REFERENCEDATA_GET_REFERRAL_REASONS)
                .withMetadataFrom(event));

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

    public CourtCentre getCourtCentre(final JsonEnvelope jsonEnvelope, final String postcode, final String prosecutionAuthorityCode, final Requester requester) {
        LOGGER.debug("Calling referenceDataService getCourtsByPostCodeAndProsecutingAuthority with postcode : {} and prosecutionAuthorityCode : {} ", postcode, prosecutionAuthorityCode);
        final JsonObject responseOuCode = getCourtsByPostCodeAndProsecutingAuthority(jsonEnvelope, postcode, prosecutionAuthorityCode, requester)
                .orElseThrow(RuntimeException::new);
        final String oucode = ((JsonObject) responseOuCode.getJsonArray("courts").get(0)).getString("oucode");

        LOGGER.debug("Calling referenceDataService getCourtsOrganisationUnitsByOuCode with oucode : {}  ", oucode);

        final JsonObject courtJson = getCourtsOrganisationUnitsByOuCode(jsonEnvelope, oucode, requester)
                .orElseThrow(RuntimeException::new);
        final JsonObject courtCentreJson = (JsonObject) courtJson.getJsonArray(ORGANISATIONUNITS).get(0);

        LOGGER.debug("Found court center from reference data search : {}  ", courtCentreJson);

        return CourtCentre.courtCentre()
                .withId(fromString(courtCentreJson.getString(ID)))
                .withName(courtCentreJson.getString("oucodeL3Name", null))
                .withWelshName(courtCentreJson.getString("oucodeL3WelshName", null))
                .build();
    }

    public CourtCentre getCourtCentre(final String oucode, final JsonEnvelope jsonEnvelope, final Requester requester) {
        final JsonObject jsonObject = getCourtsOrganisationUnitsByOuCode(jsonEnvelope, oucode, requester).orElseThrow(RuntimeException::new);
        final JsonObject orgUnit = (JsonObject) jsonObject.getJsonArray("organisationunits").get(0);

        return CourtCentre.courtCentre()
                .withId(fromString(orgUnit.getString("id")))
                .withName(orgUnit.getString("oucodeL3Name", null))
                .withWelshName(orgUnit.getString("oucodeL3WelshName", null))
                .build();

    }

    public Optional<JsonObject> getEthinicity(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get ethinicity for {} ", REFERENCEDATA_QUERY_ETHNICITIES, id);
        final JsonObject payload = Json.createObjectBuilder().build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_ETHNICITIES)
                .withMetadataFrom(event));

        return response.payloadAsJsonObject().getJsonArray(ETHNICITIES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getHearingType(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get hearing-type for {} ", REFERENCEDATA_QUERY_HEARING_TYPES, id);
        final JsonObject payload = Json.createObjectBuilder().build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_HEARING_TYPES)
                .withMetadataFrom(event));


        return response.payloadAsJsonObject().getJsonArray(HEARING_TYPES).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationality(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, id);
        final JsonEnvelope response = getNationalityResponse(event, requester);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationalityByNationality(final JsonEnvelope event, final String nationality, final Requester requester) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, nationality);
        final JsonEnvelope response = getNationalityResponse(event, requester);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(NATIONALITY).equalsIgnoreCase(nationality))
                .findFirst();
    }

    private JsonEnvelope getNationalityResponse(final JsonEnvelope event, final Requester requester) {
        final JsonObject payload = Json.createObjectBuilder().build();

        final JsonEnvelope request = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_NATIONALITIES)
                .withMetadataFrom(event));

        return requester.request(request);
    }

    public Optional<JsonObject> getProsecutor(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_PROSECUTOR, id);

        final JsonObject payload = Json.createObjectBuilder().add(ID, id.toString()).build();


        final JsonEnvelope request = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_PROSECUTOR)
                .withMetadataFrom(event));


        final JsonEnvelope response = requester.request(request);

        if (response.payload() == null) {
            return Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCPSProsecutorByOuCode(final JsonEnvelope event, final String id, final Requester requester) {

        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_PROSECUTOR_BY_OUCODE, id);

        final JsonObject payload = Json.createObjectBuilder().add(OUCODE, id)
                .add(CPS_FLAG, TRUE)
                .build();


        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_PROSECUTOR_BY_OUCODE)
                .withMetadataFrom(event));

        if (JsonValue.NULL.equals(response.payload())) {
            return Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getJudiciariesByJudiciaryIdList(final List<UUID> judiciaryIds, final JsonEnvelope event, final Requester requester) {
        final String judiciaryIdsStr = judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        LOGGER.info(" Calling {} to get prosecutors for judiciary ids: {} ", REFERENCEDATA_QUERY_JUDICIARIES, judiciaryIdsStr);
        final JsonObject payload = createObjectBuilder().add(IDS, judiciaryIdsStr).build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES)
                .withMetadataFrom(event));

        return Optional.ofNullable(response.payloadAsJsonObject());
    }


    public JsonObject getEnforcementAreaByLjaCode(final JsonEnvelope context, final String ljaCode, final Requester requester) {
        final Envelope<JsonObject> requestEnvelope = envelop(createObjectBuilder().add(COURT_CODE_QUERY_PARAMETER, ljaCode)
                .build())
                .withName(GET_ENFORCEMENT_AREA_BY_COURT_CODE)
                .withMetadataFrom(context);

        final JsonEnvelope envelope = requester.requestAsAdmin(JsonEnvelope.envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));


        return envelope.payloadAsJsonObject();
    }

    public JsonEnvelope getAllResultDefinitions(final JsonEnvelope envelope, final LocalDate orderedDate, final Requester requester) {

        final JsonObject payload = createObjectBuilder()
                .add("on", to(orderedDate))
                .build();

        return requester.request(Enveloper.envelop(payload)
                .withName(REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS)
                .withMetadataFrom(envelope));

    }

    public Optional<JsonObject> getPleaType(final String pleaTypeValue, final Requester requester) {

        LOGGER.info("Get plea type data for {} ", pleaTypeValue);

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_PLEA_TYPES);

        final Envelope<JsonObject> pleaTypes = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder()), JsonObject.class);

        final JsonArray pleaStatusTypes = pleaTypes.payload().getJsonArray(FIELD_PLEA_STATUS_TYPES);

        return pleaStatusTypes
                .stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.getString(PLEA_TYPE_VALUE).equals(pleaTypeValue))
                .findFirst();
    }
}
