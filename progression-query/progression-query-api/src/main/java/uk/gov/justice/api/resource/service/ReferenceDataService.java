package uk.gov.justice.api.resource.service;

import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.exract.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1067", "squid:S1192"})
public class ReferenceDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    public static final String REFERENCEDATA_GET_ORGANISATION = "referencedata.query.organisation-unit.v2";
    public static final String REFERENCEDATA_GET_PROSECUTOR = "referencedata.query.prosecutor";
    public static final String REFERENCEDATA_GET_HEARINGTYPES = "referencedata.query.hearing-types";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCEDATA_QUERY_CLUSTER = "referencedata.query.cluster-org-units";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS = "referencedata.get-all-document-type-access";

    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";

    private static final String REFERENCEDATA_QUERY_PLEA_TYPES = "referencedata.query.plea-types";
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_PLEA_TYPE_DESCRIPTION = "pleaTypeDescription";
    private static final String FIELD_PLEA_VALUE = "pleaValue";
    private static final String FIELD_JUDICIARIES = "judiciaries";
    private static final String FIELD_DOCUMENTS_TYPE_ACCESS = "documentsTypeAccess";
    private static final String FIELD_ID = "id";
    private static final String DEFAULT_VALUE = null;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    public Map<String, String> retrievePleaTypeDescriptions() {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_PLEA_TYPES);

        final Envelope<JsonObject> pleaTypes = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder()), JsonObject.class);
        final JsonArray pleaStatusTypes = pleaTypes.payload().getJsonArray(FIELD_PLEA_STATUS_TYPES);

        return pleaStatusTypes.stream()
                .collect(Collectors.toMap(
                        jsonValue -> ((JsonObject) jsonValue).getString(FIELD_PLEA_VALUE),
                        jsonValue -> ((JsonObject) jsonValue).getString(FIELD_PLEA_TYPE_DESCRIPTION)
                ));
    }


    public Optional<JsonObject> getOrganisationUnitById(final JsonEnvelope event, final UUID courtCentreId) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();

        final Metadata metadata = metadataFrom(event.metadata())
                .withName(REFERENCEDATA_GET_ORGANISATION)
                .build();

        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, payload));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.organisation-unit.v2' {} received with payload {}", courtCentreId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCourtCenterDataByCourtName(final JsonEnvelope event, final String courtName) {
        final JsonObject payload = createObjectBuilder()
                .add("ouCourtRoomName", courtName)
                .build();

        final Metadata metadata = metadataFrom(event.metadata())
                .withName("referencedata.query.ou.courtrooms.ou-courtroom-name")
                .build();

        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, payload));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.ou.courtrooms.ou-courtroom-name' {} received with payload {}", courtName, jsonEnvelop.toObfuscatedDebugString());
        }

        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Address getCourtCentreAddress(final JsonEnvelope jsonEnvelope, final UUID courtCentreId) {
        LOGGER.debug("Calling referenceDataService getOrganisationUnitById with courtCenterId: {} ", courtCentreId);
        final JsonObject courtCentreJson = this
                .getOrganisationUnitById(jsonEnvelope, courtCentreId)
                .orElseThrow(RuntimeException::new);
        LOGGER.debug("Found court center from reference data search : {}  ", courtCentreJson);

        return Address.address()
                .withAddress1(courtCentreJson.getString(ADDRESS_1))
                .withAddress2(courtCentreJson.getString(ADDRESS_2, null))
                .withAddress3(courtCentreJson.getString(ADDRESS_3, null))
                .withAddress4(courtCentreJson.getString(ADDRESS_4, null))
                .withAddress5(courtCentreJson.getString(ADDRESS_5, null))
                .withPostcode(courtCentreJson.getString(POSTCODE, null))
                .build();

    }

    public ProsecutingAuthority getProsecutor(final JsonEnvelope event, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {

        final ProsecutingAuthority.Builder prosecutingAuthorityBuilder = ProsecutingAuthority.prosecutingAuthority();

        if (isNameInformationEmpty(prosecutionCaseIdentifier)) {
            final JsonObject prosecutorJson = getProsecutorById(event, prosecutionCaseIdentifier.getProsecutionAuthorityId().toString()).orElseThrow(RuntimeException::new);
            final JsonObject addressJson = prosecutorJson.getJsonObject("address");
            prosecutingAuthorityBuilder
                    .withName(prosecutorJson.getString("fullName"))
                    .withAddress(
                            Address.address()
                                    .withAddress1(addressJson.getString(ADDRESS_1, null))
                                    .withAddress2(addressJson.getString(ADDRESS_2, null))
                                    .withAddress3(addressJson.getString(ADDRESS_3, null))
                                    .withAddress4(addressJson.getString(ADDRESS_4, null))
                                    .withAddress5(addressJson.getString(ADDRESS_5, null))
                                    .withPostcode(addressJson.getString(POSTCODE, null))
                                    .build());

        }

        return prosecutingAuthorityBuilder.build();

    }

    private boolean isNameInformationEmpty(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isBlank(prosecutionCaseIdentifier.getProsecutionAuthorityName());
    }

    public Map<UUID, ReferenceHearingDetails> getHearingTypes(final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().build();

        final Metadata metadata = metadataFrom(event.metadata())
                .withName(REFERENCEDATA_GET_HEARINGTYPES)
                .build();

        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, payload));
        LOGGER.info("'referencedata.query.hearing-types' {} received", jsonEnvelop.payloadAsJsonObject());
        final JsonObject hearingTypes = jsonEnvelop.payloadAsJsonObject();
        final JsonArray arryayOfObjects = hearingTypes.getJsonArray("hearingTypes");
        final Map<UUID, ReferenceHearingDetails> referenceHearingTypeDetails = new HashMap<>();
        for (int count = 0; count < arryayOfObjects.size(); count++) {
            final ReferenceHearingDetails referenceHearingDetails = convertToHearingDetais(arryayOfObjects.getJsonObject(count));
            referenceHearingTypeDetails.put(referenceHearingDetails.getHearingTypeId(), referenceHearingDetails);
        }
        return referenceHearingTypeDetails;
    }

    private ReferenceHearingDetails convertToHearingDetais(final JsonObject hearingType) {
        return new ReferenceHearingDetails(UUID.fromString(hearingType.getString("id")), hearingType.getString("hearingCode"), hearingType.getString("hearingDescription"), hearingType.getBoolean("trialTypeFlag"));
    }

    public Optional<JsonObject> getProsecutorById(final JsonEnvelope event, final String prosecutorId) {
        final JsonObject payload = createObjectBuilder().add("id", prosecutorId).build();

        final Metadata metadata = metadataFrom(event.metadata())
                .withName(REFERENCEDATA_GET_PROSECUTOR)
                .build();

        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, payload));
        LOGGER.info("'referencedata.get.prosecutor' {} received with payload {}", prosecutorId, jsonEnvelop.payloadAsJsonObject());
        return Optional.ofNullable(jsonEnvelop.payloadAsJsonObject());
    }

    public Optional<JsonObject> getJudiciary(final UUID judiciaryId) {
        final JsonObject queryParameters = createObjectBuilder().add("ids", judiciaryId.toString()).build();

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(judiciaryId)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES);

        final Envelope<JsonObject> jsonEnvelop = requester.requestAsAdmin(envelopeFrom(metadataBuilder, queryParameters), JsonObject.class);
        final JsonArray judiciaries = jsonEnvelop.payload().getJsonArray(FIELD_JUDICIARIES);
        LOGGER.info("'referencedata.query.judiciaries {} received with payload {}", judiciaryId, jsonEnvelop.payload());
        return isNull(judiciaries) || judiciaries.isEmpty() ? empty() : of(judiciaries.getJsonObject(0));
    }

    public JsonEnvelope getCourtCentreIdsByClusterId( final UUID clusterId) {
        final JsonObject payload = createObjectBuilder().add("clusterId", clusterId.toString()).build();

        final Metadata metadata = metadataBuilder()
                .withId(clusterId)
                .withName(REFERENCEDATA_QUERY_CLUSTER)
                .build();

        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, payload));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.cluster-org-units' {} received with payload {}", clusterId, jsonEnvelop.toObfuscatedDebugString());
        }
        return  jsonEnvelop;
    }

    public List<DocumentTypeAccessReferenceData> getDocumentsTypeAccess() {
        return getRefDataStream(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS, FIELD_DOCUMENTS_TYPE_ACCESS,
                createObjectBuilder().add("date", LocalDate.now().toString()))
                .map(asDocumentsMetadataRefData()).collect(Collectors.toList());
    }

    public static class ReferenceHearingDetails {
        private final UUID hearingTypeId;
        private final String hearingTypeCode;
        private final String hearingTypeDescription;
        private final Boolean trialTypeFlag;

        public ReferenceHearingDetails(final UUID hearingTypeId, final String hearingTypeCode, final String hearingTypeDescription, final Boolean trialTypeFlag) {
            this.hearingTypeId = hearingTypeId;
            this.hearingTypeCode = hearingTypeCode;
            this.hearingTypeDescription = hearingTypeDescription;
            this.trialTypeFlag = trialTypeFlag;
        }

        public UUID getHearingTypeId() {
            return hearingTypeId;
        }

        public String getHearingTypeCode() {
            return hearingTypeCode;
        }

        public String getHearingTypeDescription() {
            return hearingTypeDescription;
        }

        public Boolean getTrialTypeFlag() {
            return trialTypeFlag;
        }
    }

    private Stream<JsonValue> getRefDataStream(final String queryName, final String fieldName, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(queryName), jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class)
                .payload()
                .getJsonArray(fieldName)
                .stream();
    }

    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }

    public static Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), DocumentTypeAccessReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceDataId: {}", jsonValue.asJsonObject().getString(FIELD_ID, DEFAULT_VALUE), e);
                return null;
            }
        };
    }
}
