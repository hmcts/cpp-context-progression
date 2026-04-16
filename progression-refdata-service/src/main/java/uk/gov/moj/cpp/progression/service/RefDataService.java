package uk.gov.moj.cpp.progression.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;
import uk.gov.moj.cpp.progression.domain.pojo.ReferenceDataDirectionManagementType;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.service.exception.ReferenceDataServiceException;

@SuppressWarnings({"squid:S00112", "squid:S1192", "squid:CallToDeprecatedMethod"})
public class RefDataService {

    public static final String CJSOFFENCECODE = "cjsoffencecode";
    public static final String REFERENCEDATA_QUERY_OFFENCES = "referencedata.query.offences";
    public static final String OFFENCES = "offences";
    public static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
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
    public static final String REFERENCEDATA_GET_REFERRAL_REASON_BY_ID = "reference-data.query.get-referral-reason";
    public static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";
    public static final String ID = "id";
    public static final String REFERENCEDATA_QUERY_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    public static final String REFERENCEDATA_QUERY_COURT_ROOM = "referencedata.query.courtroom";
    public static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    public static final String REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS = "referencedata.query.local-justice-areas";
    public static final String REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS = "referencedata.get-all-result-definitions";

    private static final String REFERENCEDATA_QUERY_COUNTRY_BY_POSTCODE = "referencedata.query.country-by-postcode";
    private static final String REFERENCE_DATA_QUERY_CPS_PROSECUTORS = "referencedata.query.get.prosecutor.by.cpsflag";
    private static final String PROSECUTORS = "prosecutors";
    private static final String REFERENCEDATA_QUERY_COURT_APPLICATION_TYPES = "referencedata.query.application-types";
    private static final String REFERENCEDATA_QUERY_PRISONS_CUSTODY_SUITES = "referencedata.query.prisons-custody-suites";
    private static final String FIELD_APPLICATION_TYPES = "courtApplicationTypes";
    public static final String CP_RESULT_ACTION_MAPPING = "referencedata.query.result-action-mapping";
    private static final String REFERENCEDATA_QUERY_PUBLIC_HOLIDAYS_NAME = "referencedata.query.public-holidays";
    private static final String FIELD_PRISONS_CUSTODY_SUITES = "prisons-custody-suites";
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
    public static final String POLICE_FLAG = "policeFlag";
    private static final String PROSECUTOR_CODE = "prosecutorCode";
    public static final String HEARING_TYPES = "hearingTypes";
    public static final String ETHNICITIES = "ethnicities";
    public static final String ORGANISATIONUNITS = "organisationunits";
    public static final String COURT_CODE_QUERY_PARAMETER = "localJusticeAreaNationalCourtCode";
    public static final String LOCAL_JUSTICE_AREA = "localJusticeArea";
    private static final String REFERENCEDATA_GET_COTR_REVIEW_NOTES = "referencedata.query.cotr-review-notes";
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String PLEA_TYPE_VALUE = "pleaValue";
    private static final String REFERENCEDATA_QUERY_PLEA_TYPES = "referencedata.query.plea-types";
    private static final String REFERENCEDATA_QUERY_PET_FORM = "referencedata.query.latest-pet-form";
    private static final String REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String REFERENCE_DATA_QUERY_GET_PROSECUTORS = "referencedata.query.prosecutors";
    public static final String REFERENCE_DATA_QUERY_GET_OU_COURT_CODE = "referencedata.query.get.police-opt-courtroom-ou-courtroom-code";

    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataService.class);
    private static final String REFERENCEDATA_GET_ALL_DOCUMENT_TYPE_ACCESS_QUERY = "referencedata.get-all-document-type-access";
    private static final String DOCUMENT_TYPE_ACCESS = "documentsTypeAccess";
    private static final String DATE = "date";
    private static final String PUBLIC_HOLIDAYS = "publicHolidays";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    public static final String REFERENCEDATA_GET_DIRECTION_MANAGEMENT_TYPES = "referencedata.query.direction-management-types";

    public static final String REFERENCEDATA_GET_ALL_DIRECTIONS = "referencedata.get-all-directions";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public Optional<DocumentTypeAccessReferenceData> getDocumentTypeAccessReferenceData(final Requester requester, final UUID documentTypeId) {
        final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDatas = getAllDocumentTypeAccess(requester);
        return documentTypeAccessReferenceDatas.stream()
                .filter(documentTypeAccessReferenceData -> documentTypeAccessReferenceData.getId().equals(documentTypeId))
                .findAny();
    }

    public List<DocumentTypeAccessReferenceData> getAllDocumentTypeAccess(final Requester requester) {
        return getRefDataStream(REFERENCEDATA_GET_ALL_DOCUMENT_TYPE_ACCESS_QUERY,
                DOCUMENT_TYPE_ACCESS, createObjectBuilder().add(DATE, LocalDate.now().toString()), requester)
                .map(asDocumentsMetadataRefData())
                .collect(Collectors.toList());
    }

    public List<LocalDate> getPublicHolidays(final String division, final LocalDate fromDate, final LocalDate toDate, final Requester requester) {

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_PUBLIC_HOLIDAYS_NAME);

        final JsonObject params = createObjectBuilder()
                .add("division", division)
                .add("dateFrom", fromDate.toString())
                .add("dateTo", toDate.toString())
                .build();

        final JsonObject payload = requester.requestAsAdmin(envelopeFrom(metadataBuilder, params), JsonObject.class).payload();
        if (!payload.containsKey(PUBLIC_HOLIDAYS) || payload.getJsonArray(PUBLIC_HOLIDAYS).isEmpty()) {
            return emptyList();
        }

        final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return payload.getJsonArray(PUBLIC_HOLIDAYS).getValuesAs(JsonObject.class).stream()
                .map(jsonObject -> jsonObject.getString(DATE))
                .map(date -> LocalDate.parse(date, dateFormat))
                .collect(Collectors.toList());

    }

    @SuppressWarnings({"squid:S2139"})
    private Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), DocumentTypeAccessReferenceData.class);
            } catch (final IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceData. Payload :{}", jsonValue, e);
                throw new UncheckedIOException(e);
            }
        };
    }

    private Stream<JsonValue> getRefDataStream(final String queryName, final String fieldName,
                                               final JsonObjectBuilder jsonObjectBuilder, final Requester requester) {
        final JsonEnvelope envelope =
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName(queryName),
                        jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class).payload()
                .getJsonArray(fieldName).stream();
    }


    public static UUID extractUUID(final JsonObject object, final String key) {
        return object.containsKey(key) && !object.getString(key).isEmpty() ? fromString(object.getString(key, null)) : null;
    }

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

    public Optional<JsonObject> getCotrReviewNotes(final Metadata metadata, final Requester requester) {
        final Metadata metadataNew = metadataWithNewActionName(metadata, REFERENCEDATA_GET_COTR_REVIEW_NOTES);
        return Optional.ofNullable(requester.request(envelopeFrom(metadataNew, createObjectBuilder().build()), JsonObject.class).payload());
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

    public Optional<JsonObject> getCourtCentreWithCourtRoomsById(final UUID courtCentreId, final JsonEnvelope event, final Requester requester) {

        final JsonObject payload = Json.createObjectBuilder()
                .add(ID, courtCentreId.toString())
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
                .map(JsonObject.class::cast)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getReferralReasonByReferralReasonId(final JsonEnvelope event, final UUID referralReasonId, final Requester requester) {

        final Envelope<JsonObject> envelope = requester.request(envelop(createObjectBuilder().add(ID, referralReasonId.toString()).build())
                .withName(REFERENCEDATA_GET_REFERRAL_REASON_BY_ID)
                .withMetadataFrom(event), JsonObject.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(" get referral reasons by referral reason Id '{}' received with payload {} ", REFERENCEDATA_GET_REFERRAL_REASON_BY_ID, envelope.payload());
        }
        return Optional.ofNullable(envelope.payload());
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

    public CourtCentre getCourtByCourtHouseOUCode(final String courtHouseOUCode, final JsonEnvelope envelope, final Requester requester) {
        final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();

        final JsonObject payload = Json.createObjectBuilder()
                .add("oucode", courtHouseOUCode)
                .build();
        LOGGER.info(" Calling {} to get court centre for {} ", REFERENCEDATA_GET_COURTCENTER, courtHouseOUCode);
        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_GET_COURTCENTER)
                .withMetadataFrom(envelope));

        final JsonEnvelope organisationUnitsResponse = requester.requestAsAdmin(response);

        final JsonObject jsonObject = organisationUnitsResponse.payloadAsJsonObject();

        final Optional<JsonObject> courtOptional = jsonObject
                .getJsonArray("organisationunits")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(e -> courtHouseOUCode.equals(e.getString("oucode")))
                .findFirst();

        populateCourtCenter(courtCentreBuilder, courtOptional);
        return courtCentreBuilder.build();
    }

    private void populateCourtCenter(final CourtCentre.Builder courtCentreBuilder,
                                     final Optional<JsonObject> courtOptional) {
        final LjaDetails.Builder ljaDetails = LjaDetails.ljaDetails();

        if (courtOptional.isPresent()) {
            final JsonObject court = courtOptional.get();
            courtCentreBuilder
                    .withId(extractUUID(court, ID))
                    .withName(court.getString("oucodeL3Name", null))
                    .withCode(court.getString(OUCODE, null))
                    .withLja(ljaDetails.withLjaCode(court.getString("lja")).build());
            if (court.getBoolean("isWelsh", false)) {
                courtCentreBuilder
                        .withWelshName(court.getString("oucodeL3WelshName", null))
                        .withWelshCourtCentre(court.getBoolean("isWelsh", false));
            }
        }
    }

    public Optional<JsonObject> getEthinicity(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get ethinicity for {} ", REFERENCEDATA_QUERY_ETHNICITIES, id);
        final JsonObject payload = Json.createObjectBuilder().build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_ETHNICITIES)
                .withMetadataFrom(event));

        return response.payloadAsJsonObject().getJsonArray(ETHNICITIES).stream()
                .map(JsonObject.class::cast)
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
                .map(JsonObject.class::cast)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationality(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, id);
        final JsonEnvelope response = getNationalityResponse(event, requester);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(JsonObject.class::cast)
                .filter(jsonObject -> jsonObject.getString(ID).equals(id.toString()))
                .findFirst();
    }

    public Optional<JsonObject> getNationalityByNationality(final JsonEnvelope event, final String nationality, final Requester requester) {

        LOGGER.info(" Calling {} to get nationalities for {} ", REFERENCEDATA_QUERY_NATIONALITIES, nationality);
        final JsonEnvelope response = getNationalityResponse(event, requester);
        return response.payloadAsJsonObject().getJsonArray(COUNTRY_NATIONALITY).stream()
                .map(JsonObject.class::cast)
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

        if (response.payloadIsNull()) {
            return Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getProsecutorV2(final JsonEnvelope event, final UUID id, final Requester requester) {

        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_PROSECUTOR, id);

        final JsonObject payload = Json.createObjectBuilder().add(ID, id.toString()).build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_PROSECUTOR)
                .withMetadataFrom(event));

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

    public Optional<JsonArray> getCPSProsecutors(final JsonEnvelope event, final Requester requester) {

        LOGGER.info(" Calling {} to get prosecutors with cpsFlag true", REFERENCE_DATA_QUERY_CPS_PROSECUTORS);

        final JsonObject payload = Json.createObjectBuilder()
                .add(CPS_FLAG, TRUE)
                .build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCE_DATA_QUERY_CPS_PROSECUTORS)
                .withMetadataFrom(event));

        if (JsonValue.NULL.equals(response.payload())) {
            return Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject().getJsonArray(PROSECUTORS));
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
                .map(JsonObject.class::cast)
                .filter(jsonObject -> jsonObject.getString(PLEA_TYPE_VALUE).equals(pleaTypeValue))
                .findFirst();
    }

    public LjaDetails getLjaDetails(final JsonEnvelope jsonEnvelope, final String ljaCode, final Requester requester) {
        final JsonObject courtEnforcementArea = getEnforcementAreaByLjaCode(jsonEnvelope, ljaCode, requester);
        if (courtEnforcementArea == null || courtEnforcementArea.isNull(LOCAL_JUSTICE_AREA)) {
            return null;
        }
        return LjaDetails.ljaDetails()
                .withLjaCode(courtEnforcementArea.getJsonObject(LOCAL_JUSTICE_AREA).getString("nationalCourtCode", null))
                .withLjaName(courtEnforcementArea.getJsonObject(LOCAL_JUSTICE_AREA).getString("name", null))
                .withWelshLjaName(courtEnforcementArea.getJsonObject(LOCAL_JUSTICE_AREA).getString("welshName", null))
                .build();
    }

    public Optional<JsonObject> getPetForm(final JsonEnvelope event, final Requester requester) {

        LOGGER.info("Get PET FORM details with ID '{}'", event.metadata().streamId());
        final JsonObject payload = Json.createObjectBuilder().build();
        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_PET_FORM)
                .withMetadataFrom(event));

        return Optional.ofNullable(response.payloadAsJsonObject());
    }

    public boolean getPoliceFlag(final String originatingOrganisation, final String prosecutionAuthority, final Requester requester) {
        final Optional<JsonObject> optionalJsonObject;

        if (isNotEmpty(originatingOrganisation)) {
            optionalJsonObject = getProsecutorByOriginatingOrganisation(originatingOrganisation, requester);
        } else if (isNotEmpty(prosecutionAuthority)) {
            optionalJsonObject = getProsecutorByProsecutionAuthority(prosecutionAuthority, requester);
        } else {
            return FALSE;
        }

        if (optionalJsonObject.isPresent()) {
            final JsonObject jsonObject = optionalJsonObject.get();
            return jsonObject.containsKey(POLICE_FLAG) ? jsonObject.getBoolean(POLICE_FLAG) : FALSE;
        }

        return FALSE;
    }

    public Optional<JsonObject> getProsecutorByOriginatingOrganisation(final String originatingOrganisation, final Requester requester) {
        final JsonObject payload = createObjectBuilder().add(OUCODE, originatingOrganisation).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(jsonEnvelope, JsonObject.class);
        return (nonNull(response) && nonNull(response.payload())) ? Optional.of(response.payload()) : Optional.empty();
    }

    public Optional<JsonObject> getProsecutorByProsecutionAuthority(final String prosecutionAuthority, final Requester requester) {
        final JsonObject payload = createObjectBuilder().add(PROSECUTOR_CODE, prosecutionAuthority).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_GET_PROSECUTORS)
                .build();

        final Envelope<JsonObject> response = requester.requestAsAdmin(envelopeFrom(metadata, payload), JsonObject.class);
        if (isNull(response.payload()) || response.payload().getJsonArray("prosecutors").isEmpty()) {
            return Optional.empty();
        }
        return ofNullable(response.payload().getJsonArray("prosecutors").getJsonObject(0));
    }

    public String getCountryByPostcode(final JsonEnvelope envelope, final String postCode, final Requester requester) {
        final JsonObject payload = createObjectBuilder().add("postCode", postCode).build();
        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_COUNTRY_BY_POSTCODE)
                .withMetadataFrom(envelope));

        return response.payloadAsJsonObject().getString("country");
    }

    public Optional<JsonObject> getOuCourtRoomCode(final String courtRoomId, final Requester requester) {
        final JsonObject payload = createObjectBuilder()
                .add("courtRoomUuid", courtRoomId)
                .build();

        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_GET_OU_COURT_CODE);

        return ofNullable(requester.requestAsAdmin(envelopeFrom(metadataBuilder, payload), JsonObject.class).payload());
    }

    public CourtApplicationType retrieveApplicationType(final String applicationCode, final Requester requester) {
        final List<CourtApplicationType> courtApplicationTypes = getRefDataStream(requester, REFERENCEDATA_QUERY_COURT_APPLICATION_TYPES, FIELD_APPLICATION_TYPES, createObjectBuilder()).map(asApplicationTypeRefData()).collect(Collectors.toList());
        final Optional<CourtApplicationType> courtApplicationTypeOptional = courtApplicationTypes.stream().filter(courtApplicationType -> applicationCode.equals(courtApplicationType.getCode())).findFirst();
        return courtApplicationTypeOptional.isPresent() ? courtApplicationTypeOptional.get() : null;
    }

    public List<PrisonCustodySuite> getPrisonsCustodySuites(final Requester requester) {
        return getRefDataStream(requester, REFERENCEDATA_QUERY_PRISONS_CUSTODY_SUITES, FIELD_PRISONS_CUSTODY_SUITES, createObjectBuilder())
                .map(asPrisonCustodySuite())
                .collect(Collectors.toList());
    }

    public List<CpResultActionMapping> getResultIdsByActionCode(final String resultActionCode, final Requester requester) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(CP_RESULT_ACTION_MAPPING).build();

        final JsonObject payload = createObjectBuilder()
                .add("resultActionCode", resultActionCode)
                .build();

        final Envelope<CpResultActionMappingResponse> jsonEnvelope = requester.requestAsAdmin(
                envelopeFrom(metadata, payload), CpResultActionMappingResponse.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'{}' received with payload {}", CP_RESULT_ACTION_MAPPING, jsonEnvelope);
        }

        return isNull(jsonEnvelope.payload()) ? new ArrayList<>() : jsonEnvelope.payload().getResultActionMapping();
    }



    public List<ReferenceDataDirectionManagementType> getDirectionManagementTypes() {
        final JsonObject request = createObjectBuilder().build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                Envelope.metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_DIRECTION_MANAGEMENT_TYPES),
                request);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (isNull(response.payload())) {
            throw new ReferenceDataServiceException("Failed to get direction-management-types from reference Data");
        }
        LOGGER.info("Got direction-management-types from reference data context");
        final List<ReferenceDataDirectionManagementType> referenceDataDirectionManagementTypes = new ArrayList<>();
        JsonArray directionManagementTypesJsonArray = response.payload().getJsonArray("directionManagementTypes");
        IntStream.range(0, directionManagementTypesJsonArray.size()).mapToObj(caseCounter -> directionManagementTypesJsonArray.getJsonObject(caseCounter)).forEach(referenceDataDirectionManagementType ->
                referenceDataDirectionManagementTypes.add(jsonObjectToObjectConverter.convert(referenceDataDirectionManagementType,ReferenceDataDirectionManagementType.class))
        );
        return referenceDataDirectionManagementTypes;
    }

    public List<Direction> getDirections() {
        final JsonObject request = createObjectBuilder().build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                Envelope.metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_ALL_DIRECTIONS),
                request);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (isNull(response.payload())) {
            throw new ReferenceDataServiceException("Failed to get directions from reference Data");
        }
        LOGGER.info("Got directions from reference data context");
        final List<Direction> directions = new ArrayList<>();
        JsonArray directionsJsonArray = response.payload().getJsonArray("directions");
        IntStream.range(0, directionsJsonArray.size()).mapToObj(caseCounter -> directionsJsonArray.getJsonObject(caseCounter)).forEach(referenceDataDirectionManagementType ->
                directions.add(jsonObjectToObjectConverter.convert(referenceDataDirectionManagementType,Direction.class))
        );
        return directions;
    }


    private Stream<JsonValue> getRefDataStream(final Requester requester, final String queryName, final String fieldName, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(queryName), jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class)
                .payload()
                .getJsonArray(fieldName)
                .stream();
    }

    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }

    private static Function<JsonValue, CourtApplicationType> asApplicationTypeRefData() {
        return jsonValue -> {
            try {
                return new ObjectMapperProducer().objectMapper().readValue(jsonValue.toString(), CourtApplicationType.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal CourtApplicationType. Payload :{}", jsonValue, e);
                return null;
            }
        };
    }

    private static Function<JsonValue, PrisonCustodySuite> asPrisonCustodySuite() {
        return jsonValue -> {
            try {
                return new ObjectMapperProducer().objectMapper().readValue(jsonValue.toString(), PrisonCustodySuite.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal PrisonCustodySuites. Payload :{}", jsonValue, e);
                return null;
            }
        };
    }
}