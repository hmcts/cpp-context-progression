package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
@SuppressWarnings({"squid:S1612", "squid:S2259", "squid:S00112"})
public class CourtDocumentQuery {

    public static final String ID_PARAMETER = "courtDocumentId";
    public static final String DEFENDANT_ID_PARAMETER = "defendantId";
    public static final String HEARING_ID_PARAMETER = "hearingId";
    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENTS_NOW_SEARCH_NAME = "progression.query.courtdocuments.now";
    public static final String COURT_DOCUMENT_RESULT_FIELD = "courtDocument";
    public static final String CASE_ID_SEARCH_PARAM = "caseId";
    public static final String DEFENDANT_ID_SEARCH_PARAM = "defendantId";
    public static final String APPLICATION_ID_SEARCH_PARAM = "applicationId";
    private static final String ID = "id";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_STATUS = "notificationStatus";
    public static final String APPLICATION_ID = "applicationId";
    public static final String PROSECUTION_NOTIFICATION_STATUS = "progression.query.prosecution.notification-status";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentQuery.class);
    public static final String DOCUMENT_USER_GROUP_LOGGER = "search for case %s for documents userGroupsInDocument=%s permittedGroups=%s";

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CourtDocumentTransform courtDocumentTransform;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private UserAndGroupProvider userAndGroupProvider;

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope envelope) {
        final Optional<UUID> id = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID_PARAMETER);
        CourtDocumentEntity courtDocumentEntity = null;
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        if (id.isPresent()) {
            courtDocumentEntity = courtDocumentRepository.findBy(id.get());
            if (nonNull(courtDocumentEntity)) {
                jsonObjectBuilder.add(COURT_DOCUMENT_RESULT_FIELD, jsonFromString(courtDocumentEntity.getPayload()));
            } else {
                jsonObjectBuilder.add(COURT_DOCUMENT_RESULT_FIELD, Json.createObjectBuilder().build());
            }
        }

        return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    private List<UUID> commaSeparatedUuidParam2UUIDs(final String strUuids) {
        return Stream.of(strUuids.split(","))
                .map(strUuid -> UUID.fromString(strUuid))
                .collect(Collectors.toList());
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchCourtDocuments(final JsonEnvelope envelope) {
        final String strCaseIds = JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_ID_SEARCH_PARAM).orElse(null);
        final String defendantId = JsonObjects.getString(envelope.payloadAsJsonObject(), DEFENDANT_ID_SEARCH_PARAM).orElse(null);
        final String strApplicationIds = JsonObjects.getString(envelope.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM).orElse(null);

        final List<CourtDocumentEntity> courtDocumentEntities = new ArrayList<>();
        boolean foundSearchParameter = false;
        if (StringUtils.isNotEmpty(strCaseIds)) {
            foundSearchParameter = true;
            courtDocumentEntities.addAll(commaSeparatedUuidParam2UUIDs(strCaseIds).stream()
                    .map(caseId -> courtDocumentRepository.findByProsecutionCaseId(caseId))
                    .flatMap(List::stream).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(strApplicationIds)) {
            foundSearchParameter = true;
            courtDocumentEntities.addAll(commaSeparatedUuidParam2UUIDs(strApplicationIds).stream()
                    .map(applicationId -> courtDocumentRepository.findByApplicationId(applicationId))
                    .flatMap(List::stream).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(defendantId)) {
            foundSearchParameter = true;
            courtDocumentEntities.addAll(courtDocumentRepository.findByDefendantId(UUID.fromString(defendantId)));
        }
        if (!foundSearchParameter) {
            throw new RuntimeException(String.format("%s no search parameter specified ", COURT_DOCUMENTS_SEARCH_NAME));
        }

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        final Map<UUID, CourtDocument> id2CourtDocument = new HashMap<>();
        courtDocumentEntities.stream()
                .map(entity -> courtDocument(entity))
                .forEach(courtDocument -> id2CourtDocument.put(courtDocument.getCourtDocumentId(), courtDocument));

        final Set<String> usergroupsInDocuments = id2CourtDocument.values().stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty() : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty() : m.getUserGroups().stream())
                .collect(Collectors.toSet());
        final Set<String> permittedGroups = usergroupsInDocuments.stream().filter(
                userGroup -> {
                    boolean isMember = userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(envelope),
                            asList(userGroup));
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("current user isMemberOf('%s') = %s", userGroup, isMember));
                    }
                    return isMember;
                }

        ).collect(Collectors.toSet());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(DOCUMENT_USER_GROUP_LOGGER,
                    strCaseIds, usergroupsInDocuments, permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments = id2CourtDocument.values().stream()
                .map(courtDocument -> filterPermittedMaterial(courtDocument, permittedGroups))
                .filter(courtDocument2 -> CollectionUtils.isNotEmpty(courtDocument2.getMaterials()))
                .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices = filteredMaterialCourtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered).build()).sorted((o1, o2) -> {
                    if (CollectionUtils.isNotEmpty(o1.getDocument().getMaterials()) &&
                            CollectionUtils.isNotEmpty(o2.getDocument().getMaterials()) &&
                            nonNull(o2.getDocument().getMaterials().get(0).getUploadDateTime()) &&
                            nonNull(o1.getDocument().getMaterials().get(0).getUploadDateTime())) {
                        return o2.getDocument().getMaterials().get(0).getUploadDateTime().compareTo(o1.getDocument().getMaterials().get(0).getUploadDateTime());
                    }
                    return -1;
                }).collect(Collectors.toList());

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), resultJson);
    }

    @Handles(COURT_DOCUMENTS_NOW_SEARCH_NAME)
    public JsonEnvelope searchCourtDocumentsByHearingId(final JsonEnvelope envelope) {
        final String hearingIds = JsonObjects.getString(envelope.payloadAsJsonObject(), HEARING_ID_PARAMETER).orElse("");
        final String defendantId = JsonObjects.getString(envelope.payloadAsJsonObject(), DEFENDANT_ID_PARAMETER).orElse("");
        final String documentCategory = JsonObjects.getString(envelope.payloadAsJsonObject(), "document_category").orElse("");
        //assume comma separated
        final List<UUID> caseIds = new ArrayList<>();
        for (final String hearingId : hearingIds.trim().split(",")) {
            caseIds.add(UUID.fromString(hearingId));
        }

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        final Map<UUID, CourtDocument> id2CourtDocument = new HashMap<>();
        caseIds.stream()
                .map(hearingId -> courtDocumentRepository.findCourtDocumentForNow(hearingId, documentCategory, UUID.fromString(defendantId)))
                .flatMap(List::stream)
                .map(entity -> courtDocument(entity))
                .forEach(courtDocument -> id2CourtDocument.put(courtDocument.getCourtDocumentId(), courtDocument));

        final Set<String> usergroupsInDocuments = id2CourtDocument.values().stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty() : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty() : m.getUserGroups().stream())
                .collect(Collectors.toSet());
        final Set<String> permittedGroups = usergroupsInDocuments.stream().filter(
                userGroup -> userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(envelope),
                        asList(userGroup))
        ).collect(Collectors.toSet());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(DOCUMENT_USER_GROUP_LOGGER,
                    hearingIds, usergroupsInDocuments, permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments = id2CourtDocument.values().stream()
                .map(courtDocument -> filterPermittedMaterial(courtDocument, permittedGroups))
                .filter(courtDocument2 -> courtDocument2.getMaterials() != null && !courtDocument2.getMaterials().isEmpty())
                .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices = filteredMaterialCourtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered).build())
                .collect(Collectors.toList());

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), resultJson);
    }

    @Handles(PROSECUTION_NOTIFICATION_STATUS)
    public JsonEnvelope getCaseNotifications(final JsonEnvelope envelope) {

        final String strCaseIds = JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_ID_SEARCH_PARAM).orElse("");

        final List<UUID> caseIds = Arrays.stream(strCaseIds.trim().split(",")).map(UUID::fromString).collect(Collectors.toList());

        final Map<UUID, List<NotificationStatusEntity>> caseNotificationMap = caseIds.stream().map(caseId -> notificationStatusRepository.findByCaseId(caseId))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(NotificationStatusEntity::getCaseId, HashMap::new, Collectors.toCollection(ArrayList::new)));

        return createJsonEnvelope(envelope, caseNotificationMap);
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        return jsonObjectToObjectConverter.convert(jsonFromString(courtDocumentEntity.getPayload()), CourtDocument.class);
    }

    private CourtDocument filterPermittedMaterial(final CourtDocument courtDocument, final Set<String> permittedGroups) {
        final List<Material> filteredMaterials =
                courtDocument.getMaterials().stream().filter(
                        m -> (m.getUserGroups() == null || m.getUserGroups().isEmpty() ||
                                m.getUserGroups().stream().anyMatch(ug -> permittedGroups.contains(ug)))
                ).collect(Collectors.toList());

        return CourtDocument.courtDocument()
                .withName(courtDocument.getName())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withIsRemoved(courtDocument.getIsRemoved())
                .withMimeType(courtDocument.getMimeType())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withMaterials(filteredMaterials)
                .build();
    }

    private JsonEnvelope createJsonEnvelope(final JsonEnvelope envelope, final Map<UUID, List<NotificationStatusEntity>> applicationNotificationMap) {

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        applicationNotificationMap.forEach((k, v) -> applicationNotificationMap.get(k).forEach(notificationStatusEntity -> prepareResponse(notificationStatusEntity, jsonArrayBuilder)));

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        jsonObjectBuilder.add(NOTIFICATION_STATUS, jsonArrayBuilder.build());

        return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    private void prepareResponse(final NotificationStatusEntity notificationStatusEntity, final JsonArrayBuilder jsonArrayBuilder) {

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        jsonObjectBuilder.add(ID, notificationStatusEntity.getId().toString())
                .add(NOTIFICATION_ID, notificationStatusEntity.getNotificationId().toString())
                .add(CASE_ID, notificationStatusEntity.getCaseId().toString());

        ofNullable(notificationStatusEntity.getApplicationId()).ifPresent(applicationId -> jsonObjectBuilder.add(APPLICATION_ID, applicationId.toString()));
        ofNullable(notificationStatusEntity.getMaterialId()).ifPresent(materialId -> jsonObjectBuilder.add("materialId", materialId.toString()));
        ofNullable(notificationStatusEntity.getNotificationStatus()).ifPresent(notificationStatus -> jsonObjectBuilder.add(NOTIFICATION_STATUS, notificationStatus.toString()));
        ofNullable(notificationStatusEntity.getNotificationType()).ifPresent(notificationType -> jsonObjectBuilder.add("notificationType", notificationType.toString()));
        ofNullable(notificationStatusEntity.getErrorMessage()).ifPresent(errorMessage -> jsonObjectBuilder.add("errorMessage", errorMessage));
        ofNullable(notificationStatusEntity.getStatusCode()).ifPresent(statusCode -> jsonObjectBuilder.add("statusCode", statusCode));
        ofNullable(notificationStatusEntity.getUpdated()).ifPresent(updated -> jsonObjectBuilder.add("updated", updated.toString()));

        jsonArrayBuilder.add(jsonObjectBuilder);
    }
}
