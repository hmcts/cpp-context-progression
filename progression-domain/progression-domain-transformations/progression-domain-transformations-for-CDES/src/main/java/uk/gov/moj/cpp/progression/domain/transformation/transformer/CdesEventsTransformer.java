package uk.gov.moj.cpp.progression.domain.transformation.transformer;


import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Stream.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.COURT_DOCUMENT_TYPE_RBAC;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.COURT_FINAL_ORDERS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.COURT_REFERRAL;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENTS_TYPE_ACCESS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENT_TYPE_DESCRIPTION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENT_TYPE_RBAC;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.MATERIALS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.POSTAL_NOTIFICATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.PROGRESSION_EVENT_COURT_DOCUMENT_ADDED;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.PROGRESSION_EVENT_COURT_DOCUMENT_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.SECTION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.SEQ_NUM;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.SUMMONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.USER_GROUPS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesUtil.getDocumentTypeIdPredicate;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesUtil.getDocumentTypeRefDataForCharges;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesUtil.getDocumentTypeRefDataForNows;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesUtil.getDocumentTypeRefDataForPostalNotification;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesUtil.getTypeRefDataObject;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S1168","pmd:BeanMembersShouldSerialize"})
@Transformation
public class CdesEventsTransformer implements EventTransformation {

    private static final Logger LOGGER = getLogger(CdesEventsTransformer.class);

    private static final Set<String> PROGRESSION_COURT_DOCUMENT_EVENT_NAMES = newHashSet(
            PROGRESSION_EVENT_COURT_DOCUMENT_ADDED,
            PROGRESSION_EVENT_COURT_DOCUMENT_CREATED,
            PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED);

    private static final String UPLOAD_USER_GROUPS = "uploadUserGroups";
    private static final String READ_USER_GROUPS = "readUserGroups";
    private static final String DOWNLOAD_USER_GROUPS = "downloadUserGroups";
    private static final String DELETE_USER_GROUPS = "deleteUserGroups";


    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        return PROGRESSION_COURT_DOCUMENT_EVENT_NAMES.contains(eventEnvelope.metadata().name().toLowerCase()) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        JsonEnvelope transformedEvent = null;

        final String name = event.metadata().name();

        if (newArrayList(PROGRESSION_EVENT_COURT_DOCUMENT_ADDED, PROGRESSION_EVENT_COURT_DOCUMENT_CREATED).contains(name)) {
            transformedEvent = buildCourtDocumentPayload(event);
        } else if (name.equalsIgnoreCase(PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED)) {
            transformedEvent = buildEventCourtProceedingsInitiatedPayload(event);
        }

        LOGGER.info("transformedEvent before returning {}", transformedEvent);
        return of(transformedEvent);
    }


    private JsonEnvelope buildCourtDocumentPayload(final JsonEnvelope event){
        final JsonObject payload = event.payloadAsJsonObject();

        final JsonObject courtDocument = payload.getJsonObject(COURT_DOCUMENT);
        LOGGER.info("Court Document: {}", courtDocument);

        JsonObject refData = null;

        if(courtDocument.containsKey(DOCUMENT_TYPE_DESCRIPTION) &&
               SUMMONS.equalsIgnoreCase(courtDocument.getString(DOCUMENT_TYPE_DESCRIPTION).trim())){
            refData = getDocumentTypeRefDataForCharges();
        }
        else if(courtDocument.containsKey(DOCUMENT_TYPE_DESCRIPTION) &&
                COURT_FINAL_ORDERS.equalsIgnoreCase(courtDocument.getString(DOCUMENT_TYPE_DESCRIPTION).trim())){
            refData = getDocumentTypeRefDataForNows();
        }
        else if(courtDocument.containsKey(DOCUMENT_TYPE_DESCRIPTION) &&
                POSTAL_NOTIFICATION.equalsIgnoreCase(courtDocument.getString(DOCUMENT_TYPE_DESCRIPTION).trim())){
            refData = getDocumentTypeRefDataForPostalNotification();
        }

        else {

            refData = (JsonObject) getTypeRefDataObject().getJsonArray(DOCUMENTS_TYPE_ACCESS).stream()
                    .filter(getDocumentTypeIdPredicate(courtDocument))
                    .findFirst().orElse(null);
        }
        if(refData==null || refData == JsonValue.NULL){
            LOGGER.error("Ref data not found : {}", refData);
            return envelopeFrom(metadataFrom(event.metadata()), payload);
        }

        LOGGER.info("ref data payload filtered: {}", refData);

        final JsonObjectBuilder prosecutionObjectBuilder = jsonObjectToBuilder(courtDocument);
        final JsonObjectBuilder payloadObjectBuilder = jsonObjectToBuilder(payload);

        final JsonArray materials = courtDocument.getJsonArray(MATERIALS);

        prosecutionObjectBuilder.add(DOCUMENT_TYPE_ID, refData.getString(ID));
        prosecutionObjectBuilder.add(DOCUMENT_TYPE_DESCRIPTION, refData.getString(SECTION));

        prosecutionObjectBuilder.add(MATERIALS, transformMaterials(materials, refData));
        prosecutionObjectBuilder.add(DOCUMENT_TYPE_RBAC, buildDocumentTypeRBAC(refData));

        prosecutionObjectBuilder.add(SEQ_NUM, refData.getInt(SEQ_NUM));

        payloadObjectBuilder.add(COURT_DOCUMENT, prosecutionObjectBuilder.build());

        final JsonObject transformedEvent = payloadObjectBuilder.build();
        LOGGER.info("transformedEvent = {} ", transformedEvent);
        return envelopeFrom(metadataFrom(event.metadata()), transformedEvent);
    }

    private JsonObject buildDocumentTypeRBAC(final JsonObject refData) {

        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonArray readUserGroups = getRBACUserGroups(refData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC), READ_USER_GROUPS);
        final JsonArray uploadUserGroups = getRBACUserGroups(refData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC), UPLOAD_USER_GROUPS);
        final JsonArray downloadUserGroups = getRBACUserGroups(refData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC), DOWNLOAD_USER_GROUPS);
        final JsonArray deleteUserGroups = getRBACUserGroups(refData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC), DELETE_USER_GROUPS);

        if(isJsonArrayNotEmpty(readUserGroups)) {
            builder.add(READ_USER_GROUPS, readUserGroups);
        }
        if(isJsonArrayNotEmpty(uploadUserGroups)) {
            builder.add(UPLOAD_USER_GROUPS, uploadUserGroups);
        }

        if(isJsonArrayNotEmpty(downloadUserGroups)) {
            builder.add(DOWNLOAD_USER_GROUPS, downloadUserGroups);
        }

        if(isJsonArrayNotEmpty(deleteUserGroups)) {
            builder.add(DELETE_USER_GROUPS, deleteUserGroups);
        }

        return builder.build();

    }


    private boolean isJsonArrayNotEmpty(final JsonArray jsonArray){
        return jsonArray!=null && !jsonArray.isEmpty();
    }

    private void buildMaterialPayload(final JsonObject refData, final JsonObject material, final JsonObjectBuilder materialPayloadObjectBuilder) {
        if(material.containsKey(USER_GROUPS)) {
            final JsonArray readUserGroups = getRBACUserGroups(refData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC), READ_USER_GROUPS);

            materialPayloadObjectBuilder.add(USER_GROUPS, readUserGroups);
        }
    }

    private JsonEnvelope buildEventCourtProceedingsInitiatedPayload(final JsonEnvelope event){
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.info("original payload: {}", payload);
        final JsonObject courtReferral = payload.getJsonObject(COURT_REFERRAL);
        final JsonObjectBuilder courtReferralObjectBuilder = jsonObjectToBuilderForCourtReferral(courtReferral);
        final JsonObject transformedEvent = createObjectBuilder()
                .add(COURT_REFERRAL,courtReferralObjectBuilder.build()).build();
        LOGGER.info("transformedEvent = {} ", transformedEvent);
        return envelopeFrom(metadataFrom(event.metadata()), transformedEvent);

    }


    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //This needs to be empty
    }


    private JsonObjectBuilder jsonObjectToBuilderForCourtReferral(JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if ("courtDocuments".equals(entry.getKey())) {
                continue;
            }
            jsonObjectBuilder.add(entry.getKey(), entry.getValue());
        }

        return jsonObjectBuilder;
    }

    private JsonObjectBuilder jsonObjectToBuilder(JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if (MATERIALS.equals(entry.getKey())|| DOCUMENT_TYPE_ID.equals(entry.getKey()) || DOCUMENT_TYPE_DESCRIPTION.equals(entry.getKey()) ) {
                continue;
            }
            jsonObjectBuilder.add(entry.getKey(), entry.getValue());
        }

        return jsonObjectBuilder;
    }


    private JsonObjectBuilder jsonObjectToBuilderForMaterial(JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if (USER_GROUPS.equals(entry.getKey())) {
                continue;
            }
            jsonObjectBuilder.add(entry.getKey(), entry.getValue());
        }

        return jsonObjectBuilder;
    }

    private JsonArray getRBACUserGroups(final JsonObject documentTypeData, final String accessLevel) {

        final JsonArray documentTypeRBACJsonArray = documentTypeData.getJsonArray(accessLevel);
        if (null == documentTypeRBACJsonArray || documentTypeRBACJsonArray.isEmpty()) {
            return null;
        }

        final JsonArrayBuilder userGroups = Json.createArrayBuilder();

        documentTypeData.getJsonArray(accessLevel).forEach( s -> userGroups.add(((JsonObject)s).getJsonObject("cppGroup").getString("groupName")));

        return userGroups.build();

    }


    public  JsonArray transformMaterials(final JsonArray materials, final JsonObject refData) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        materials.forEach(o -> {

                    final JsonObject materialObject = (JsonObject) o;
                    final JsonObjectBuilder materialPayloadObjectBuilder = jsonObjectToBuilderForMaterial(materialObject);

                    buildMaterialPayload(refData, materialObject, materialPayloadObjectBuilder);

                    transformedPayloadObjectBuilder.add(materialPayloadObjectBuilder);
                });
        return transformedPayloadObjectBuilder.build();
    }

}

