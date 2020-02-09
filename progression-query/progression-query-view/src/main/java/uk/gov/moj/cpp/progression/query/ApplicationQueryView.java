package uk.gov.moj.cpp.progression.query;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.AssignedUser;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1612"})
@ServiceComponent(Component.QUERY_VIEW)
public class ApplicationQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationQueryView.class);
    private static final String APPLICATION_ID = "applicationId";

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    public static final String APPLICATION_ID_SEARCH_PARAM = "applicationId";
    private static final String ID = "id";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_STATUS = "notificationStatus";
    private static final String MATERIAL_ID = "materialId";
    private static final String NOTIFICATION_TYPE = "notificationType";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String STATUS_CODE = "statusCode";
    private static final String UPDATED = "updated";

    @Handles("progression.query.application")
    public JsonEnvelope getApplication(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final UUID applicationId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID).get();
        try {
            final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
            final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
            jsonObjectBuilder.add("courtApplication", application);

            if (courtApplicationEntity.getAssignedUserId() != null) {

                final AssignedUser assignedUser = AssignedUser.assignedUser()
                        .withUserId(courtApplicationEntity.getAssignedUserId())
                        .build();
                jsonObjectBuilder.add("assignedUser", buildAssignedUserJson(assignedUser));
            }

            final List<CourtDocumentEntity> courtDocuments = courtDocumentRepository.findByApplicationId(applicationId);
            jsonObjectBuilder.add("courtDocuments", buildCourtDocuments(courtDocuments));

            final List<CourtApplicationEntity> childApplications = courtApplicationRepository.findByParentApplicationId(applicationId);
            jsonObjectBuilder.add("linkedApplicationsSummary", buildApplicationSummaries(childApplications));
            childApplications.add(courtApplicationEntity);
            jsonObjectBuilder.add("hearings", getHearings(childApplications));
        } catch (final NoResultException e) {
            LOGGER.info("### No application found with applicationId='{}'", applicationId, e);
        }
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.application.summary")
    public JsonEnvelope getApplicationSummary(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final Optional<UUID> applicationId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID);
        try {
            final List<CourtApplicationEntity> courtApplications = courtApplicationRepository.findByParentApplicationId(applicationId.get());

            if (!courtApplications.isEmpty()) {
                final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();

                courtApplications.forEach(courtApplicationEntity ->
                        buildApplicationSummary(courtApplicationEntity, jsonApplicationBuilder));

                jsonObjectBuilder.add("courtApplications", jsonApplicationBuilder.build());
            }


        } catch (final NoResultException e) {
            LOGGER.info("### No applications found with applicationId='{}'", applicationId, e);
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.application.notification-status")
    public JsonEnvelope getApplicationNotifications(final JsonEnvelope envelope) {

        final String strApplicationIds = JsonObjects.getString(envelope.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM).orElse("");

        final List<UUID> applicationIds = Arrays.stream(strApplicationIds.trim().split(",")).map(UUID::fromString).collect(Collectors.toList());

        final Map<UUID, List<NotificationStatusEntity>> applicationNotificationMap = applicationIds.stream().map(applicationId -> notificationStatusRepository.findByApplicationId(applicationId))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(NotificationStatusEntity::getApplicationId, HashMap::new, Collectors.toCollection(ArrayList::new)));

        return createJsonEnvelope(envelope, applicationNotificationMap);
    }

    private JsonArray buildApplicationSummaries(final List<CourtApplicationEntity> childApplications) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        childApplications.forEach(cae -> buildApplicationSummary(cae, jsonArrayBuilder));
        return jsonArrayBuilder.build();
    }

    private JsonArray buildCourtDocuments(final List<CourtDocumentEntity> courtDocuments) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        courtDocuments.forEach(courtDocumentEntity -> buildCourtDocument(courtDocumentEntity.getPayload(), jsonArrayBuilder));
        return jsonArrayBuilder.build();
    }


    private JsonObject buildAssignedUserJson(final AssignedUser assignedUser) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("userId", assignedUser.getUserId().toString());
        if (assignedUser.getFirstName() != null) {
            jsonObjectBuilder.add("firstName", assignedUser.getFirstName());
        }
        if (assignedUser.getLastName() != null) {
            jsonObjectBuilder.add("lastName", assignedUser.getLastName());
        }
        return jsonObjectBuilder.build();
    }


    private JsonArray getHearings(final List<CourtApplicationEntity> courtApplicationEntities) {
        final List<HearingApplicationEntity> entities = new ArrayList<>();
        courtApplicationEntities.forEach(courtApplicationEntity ->
                entities.addAll(hearingApplicationRepository.findByApplicationId(courtApplicationEntity.getApplicationId())));

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final List<UUID> hearingIds = new ArrayList<>();
        entities.forEach(hearingApplicationEntity -> {
            final UUID hearingId = hearingApplicationEntity.getId().getHearingId();
            if (!hearingIds.contains(hearingId)) {
                hearingIds.add(hearingId);
                jsonArrayBuilder.add(stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload()));
            }
        });
        return jsonArrayBuilder.build();
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
                .add(APPLICATION_ID, notificationStatusEntity.getApplicationId().toString());

        ofNullable(notificationStatusEntity.getApplicationId()).ifPresent(caseId -> jsonObjectBuilder.add(CASE_ID, caseId.toString()));
        ofNullable(notificationStatusEntity.getMaterialId()).ifPresent(materialId -> jsonObjectBuilder.add(MATERIAL_ID, materialId.toString()));
        ofNullable(notificationStatusEntity.getNotificationStatus()).ifPresent(notificationStatus -> jsonObjectBuilder.add(NOTIFICATION_STATUS, notificationStatus.toString()));
        ofNullable(notificationStatusEntity.getNotificationType()).ifPresent(notificationType -> jsonObjectBuilder.add(NOTIFICATION_TYPE, notificationType.toString()));
        ofNullable(notificationStatusEntity.getErrorMessage()).ifPresent(errorMessage -> jsonObjectBuilder.add(ERROR_MESSAGE, errorMessage));
        ofNullable(notificationStatusEntity.getStatusCode()).ifPresent(statusCode -> jsonObjectBuilder.add(STATUS_CODE, statusCode));
        ofNullable(notificationStatusEntity.getUpdated()).ifPresent(updated -> jsonObjectBuilder.add(UPDATED, updated.toString()));

        jsonArrayBuilder.add(jsonObjectBuilder);
    }

    private void buildApplicationSummary(final CourtApplicationEntity courtApplicationEntity, final JsonArrayBuilder jsonApplicationBuilder) {
        final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                (courtApplicationEntity.getPayload()), CourtApplication.class);

        if (Objects.nonNull(courtApplication)) {
            jsonApplicationBuilder.add(objectToJsonObjectConverter.convert(CourtApplicationSummary.applicationSummary()
                    .withApplicationId(courtApplication.getId().toString())
                    .withApplicationReference(courtApplication.getApplicationReference())
                    .withApplicationStatus(courtApplication.getApplicationStatus())
                    .withApplicationTitle(courtApplication.getType())
                    .withApplicantDisplayName(courtApplication.getApplicant())
                    .withRespondentDisplayNames(courtApplication.getRespondents())
                    .withAssignedUserId(courtApplicationEntity.getAssignedUserId())
                    .build()));
        }
    }

    private void buildCourtDocument(final String courtDocumentPayload, final JsonArrayBuilder jsonArrayBuilder) {
        JsonObject courtDocumentJson = stringToJsonObjectConverter.convert(courtDocumentPayload);
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(courtDocumentJson, CourtDocument.class);
        if (Objects.isNull(courtDocument.getIsRemoved()) || !courtDocument.getIsRemoved()) {
            jsonArrayBuilder.add(objectToJsonObjectConverter.convert(CourtDocument.courtDocument()
                    .withCourtDocumentId(courtDocument.getCourtDocumentId())
                    .withDocumentCategory(courtDocument.getDocumentCategory())
                    .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                    .withDocumentTypeId(courtDocument.getDocumentTypeId())
                    .withName(courtDocument.getName())
                    .withMaterials(courtDocument.getMaterials())
                    .withMimeType(courtDocument.getMimeType())
                    .withIsRemoved(courtDocument.getIsRemoved())
                    .build()));
        }
    }
}
