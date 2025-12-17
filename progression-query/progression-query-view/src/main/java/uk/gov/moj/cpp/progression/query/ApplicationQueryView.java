package uk.gov.moj.cpp.progression.query;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationHearingQueryHelper.buildApplicationHearingResponse;

import uk.gov.justice.core.courts.AssignedUser;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.courts.progression.query.AagResults;
import uk.gov.justice.courts.progression.query.ApplicationDetails;
import uk.gov.justice.courts.progression.query.LinkedCases;
import uk.gov.justice.courts.progression.query.Offences;
import uk.gov.justice.courts.progression.query.ThirdParties;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.RespondentDetails;
import uk.gov.justice.progression.query.laa.ApplicationLaa;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.query.utils.converters.laa.ApplicationLaaConverter;
import uk.gov.moj.cpp.progression.query.view.ApplicationAtAGlanceHelper;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.service.DefenceQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

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
import javax.json.JsonValue;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1612"})
@ServiceComponent(Component.QUERY_VIEW)
public class ApplicationQueryView {

    public static final String APPLICATION_ID_SEARCH_PARAM = "applicationId";
    public static final String CASEID_SEARCH_PARAM = "caseId";
    private static final String APPLICATION_ID_NOT_FOUND = "### applicationId not found";
    private static final String NO_APPLICATION_FOUND_WITH_APPLICATION_ID = "### No application found with applicationId='{}'";
    private static final String NO_APPLICATIONS_FOUND_WITH_APPLICATION_IDS = "### No applications found with applicationIds='{}'";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationQueryView.class);
    private static final String APPLICATION_ID = "applicationId";
    private static final String APPLICATION_IDS = "applicationIds";
    private static final String APPLICATION_STATUS = "applicationStatus";
    private static final String ID = "id";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_STATUS = "notificationStatus";
    private static final String MATERIAL_ID = "materialId";
    private static final String NOTIFICATION_TYPE = "notificationType";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String STATUS_CODE = "statusCode";
    private static final String UPDATED = "updated";
    private static final String TARGET_TYPE_APPLICATION = "APPLICATION_ID_LAA";

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    @Inject
    private ApplicationAtAGlanceHelper applicationAtAGlanceHelper;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ApplicationLaaConverter applicationLaaConverter;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;
    @Inject
    private Requester requester;
    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private DefenceQueryService defenceQueryService;

    @Handles("progression.query.application.aaag")
    @SuppressWarnings("squid:S3776")
    public JsonEnvelope getCourtApplicationForApplicationAtAGlance(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> applicationId = getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID);
        if (applicationId.isPresent()) {
            try {
                final CourtApplication courtApplication = getCourtApplication(applicationId.get());
                validateUserPermissionForApplicationType(envelope, courtApplication);
                jsonObjectBuilder.add(APPLICATION_ID, applicationId.get().toString());

                final ApplicationDetails applicationDetails = applicationAtAGlanceHelper.getApplicationDetails(courtApplication);
                final JsonObject applicationDetailsJson = objectToJsonObjectConverter.convert(applicationDetails);
                jsonObjectBuilder.add("applicationDetails", applicationDetailsJson);

                final JsonArray linkedCases = getLinkedCases(courtApplication);
                if (!linkedCases.isEmpty()) {
                    jsonObjectBuilder.add("linkedCases", linkedCases);
                }
                final boolean applyAddressConfidentialityCheck = !linkedCases.isEmpty() &&
                                envelope.payloadAsJsonObject().getBoolean("isDefenceQuery", false) &&
                                linkedCases.stream().anyMatch(lc -> defenceQueryService.isUserOnlyDefendingCase(
                                                envelope, ((JsonObject) lc).getString("prosecutionCaseId")));

                final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, envelope, applyAddressConfidentialityCheck);
                final JsonObject applicantDetailsJson = objectToJsonObjectConverter.convert(applicantDetails);
                jsonObjectBuilder.add("applicantDetails", applicantDetailsJson);

                final List<CourtApplicationEntity> childApplications = courtApplicationRepository.findByParentApplicationId(applicationId.get());
                if (!childApplications.isEmpty()) {
                    LOGGER.info("### childApplications found with applicationId='{}'", applicationId);
                    final JsonArray jsonValue = buildApplicationSummaries(childApplications);
                    LOGGER.info("### childApplications/linkedApplications='{}'", jsonValue);
                    jsonObjectBuilder.add("linkedApplications", jsonValue);
                }
                if (courtApplication.getParentApplicationId() != null) {
                    LOGGER.info("### Parent application found with applicationId='{}'", courtApplication.getParentApplicationId());
                    final CourtApplicationEntity parentApplication = courtApplicationRepository.findByApplicationId(courtApplication.getParentApplicationId());
                    if (parentApplication != null) {
                        final JsonObject jsonValue = buildApplicationSummary(parentApplication);
                        LOGGER.info("### Parent application jsonValue='{}'", jsonValue);
                        jsonObjectBuilder.add("parentApplication", jsonValue);
                    }
                }

                final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.getRespondentDetails(courtApplication, applyAddressConfidentialityCheck);
                if (!respondentDetails.isEmpty()) {
                    final JsonValue respondentDetailsJson = objectToJsonValueConverter.convert(respondentDetails);
                    jsonObjectBuilder.add("respondentDetails", respondentDetailsJson);
                }

                final List<ThirdParties> thirdPartyDetails = applicationAtAGlanceHelper.getThirdPartyDetails(courtApplication, applyAddressConfidentialityCheck);
                if (!thirdPartyDetails.isEmpty()) {
                    final JsonValue thirdPartyJson = objectToJsonValueConverter.convert(thirdPartyDetails);
                    jsonObjectBuilder.add("thirdParties", thirdPartyJson);
                }
            } catch (final NoResultException e) {
                LOGGER.warn(NO_APPLICATION_FOUND_WITH_APPLICATION_ID, applicationId, e);
            }
        } else {
            LOGGER.warn(APPLICATION_ID_NOT_FOUND);
        }
        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private void validateUserPermissionForApplicationType(final JsonEnvelope envelope, final CourtApplication courtApplication) {
        if(nonNull(courtApplication.getType()) &&
                nonNull(courtApplication.getType().getCode()) &&
                    !userDetailsLoader.isUserHasPermissionForApplicationTypeCode(envelope.metadata(), courtApplication.getType().getCode())){
            throw new ForbiddenRequestException("User doesn't have permission to view this application");
        }
    }

    @Handles("progression.query.application-laa")
    public Envelope<ApplicationLaa> getApplicationForLaa(final JsonEnvelope envelope) {
        final UUID applicationId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID).get();
        try {
            final CourtApplication courtApplication = getCourtApplication(applicationId);
            final List<Hearing> hearingList = getHearingList(courtApplication.getId());
            final String laaApplicationShortId = retrieveLaaApplicationShortIdFromSystemIdMapper(applicationId);
            return Envelope.envelopeFrom(envelope.metadata(), applicationLaaConverter.convert(courtApplication, hearingList, laaApplicationShortId));

        } catch (final NoResultException e) {
            LOGGER.info(NO_APPLICATION_FOUND_WITH_APPLICATION_ID, applicationId, e);
            return Envelope.envelopeFrom(envelope.metadata(), ApplicationLaa.applicationLaa().build());
        }
    }

    public String retrieveLaaApplicationShortIdFromSystemIdMapper(final UUID applicationId) {
        LOGGER.info("Retrieving laaApplicationShortId from SystemIdMapper for applicationId: {}", applicationId);

        final UUID systemUserId = systemUserProvider.getContextSystemUserId()
                .orElseThrow(ApplicationQueryView::get);

        final Optional<String> laaApplicationShortId = systemIdMapperClient.findBy(applicationId, TARGET_TYPE_APPLICATION, systemUserId)
                .map(SystemIdMapping::getSourceId);

        if (laaApplicationShortId.isPresent()) {
            LOGGER.info("Successfully retrieved laaApplicationShortId: {} for the applicationId:{}", laaApplicationShortId.get(), applicationId);
        } else {
            LOGGER.info("No laaApplicationShortId found for applicationId: {}", applicationId);
        }

        return laaApplicationShortId.orElse(null);
    }

    private static IllegalStateException get() {
        return new IllegalStateException("Invalid context system user id");
    }

    private List<Hearing> getHearingList(final UUID applicationId) {
        return getHearings(singletonList(applicationId)).stream()
                .map(jsonValue -> jsonObjectToObjectConverter.convert((JsonObject) jsonValue, Hearing.class))
                .toList();
    }

    private CourtApplication getCourtApplication(final UUID applicationId) {
        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
        final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        return jsonObjectToObjectConverter.convert(application, CourtApplication.class);
    }

    @Handles("progression.query.application")
    public JsonEnvelope getApplication(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final UUID applicationId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID).get();
        try {
            final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
            final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
            jsonObjectBuilder.add("courtApplication", application);

            if (nonNull(courtApplicationEntity.getAssignedUserId())) {
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
            jsonObjectBuilder.add("hearings", getHearings(getApplicationIds(childApplications)));
        } catch (final NoResultException e) {
            LOGGER.info(NO_APPLICATION_FOUND_WITH_APPLICATION_ID, applicationId, e);
        }
        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private static List<UUID> getApplicationIds(final List<CourtApplicationEntity> childApplications) {
        return childApplications.stream().map(CourtApplicationEntity::getApplicationId).toList();
    }

    /**
     * Returns a simplified court application response without the associated documents, assigned
     * users and hearings etc.
     *
     * @param envelope - request envelope
     * @return court application
     */
    @Handles("progression.query.application-only")
    public JsonEnvelope getApplicationOnly(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        final UUID applicationId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID).get();

        try {
            final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
            final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
            jsonObjectBuilder.add("courtApplication", application);
        } catch (final NoResultException e) {
            LOGGER.info(NO_APPLICATION_FOUND_WITH_APPLICATION_ID, applicationId, e);
        }
        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.application-status")
    public JsonEnvelope getApplicationStatus(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        final List<UUID> applicationIdList = JsonObjects.getString(envelope.payloadAsJsonObject(), APPLICATION_IDS)
                .map(applicationIdStr -> Arrays.stream(applicationIdStr.split(",")).map(UUID::fromString).toList())
                .orElse(emptyList());

        try {
            final List<CourtApplicationEntity> courtApplicationEntityList = courtApplicationRepository.findByApplicationIds(applicationIdList);
            final JsonArrayBuilder applicationStatusJsonArray = createArrayBuilder();
            courtApplicationEntityList.forEach(courtApplicationEntity -> {
                final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
                applicationStatusJsonArray.add(Json.createObjectBuilder()
                        .add(APPLICATION_ID, application.getString(ID))
                        .add(APPLICATION_STATUS, application.getString(APPLICATION_STATUS)));
            });
            jsonObjectBuilder.add("applicationsWithStatus", applicationStatusJsonArray);
        } catch (final NoResultException e) {
            LOGGER.info(NO_APPLICATIONS_FOUND_WITH_APPLICATION_IDS, applicationIdList, e);
        }
        return envelopeFrom(
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

        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.application.notification-status")
    public JsonEnvelope getApplicationNotifications(final JsonEnvelope envelope) {

        final String strApplicationIds = getString(envelope.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM).orElse("");

        final List<UUID> applicationIds = Arrays.stream(strApplicationIds.trim().split(",")).map(UUID::fromString).collect(toList());

        final Map<UUID, List<NotificationStatusEntity>> applicationNotificationMap = applicationIds.stream().map(applicationId -> notificationStatusRepository.findByApplicationId(applicationId))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(NotificationStatusEntity::getApplicationId, HashMap::new, Collectors.toCollection(ArrayList::new)));

        return createJsonEnvelope(envelope, applicationNotificationMap);
    }

    @Handles("progression.query.applicationhearings")
    public JsonEnvelope getApplicationHearings(final JsonEnvelope envelope) {
        final Optional<UUID> applicationId = getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID);
        final List<HearingApplicationEntity> hearingEntities = hearingApplicationRepository.findByApplicationId(applicationId.get());
        final List<JsonObject> hearingPayloads = hearingEntities.stream()
                .map(hearingApplicationEntity -> stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload()))
                .collect(toList());
        final JsonObject responsePayload = buildApplicationHearingResponse(hearingPayloads);
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.court-proceedings-for-application")
    public JsonEnvelope getCourtProceedingsForApplication(final JsonEnvelope query) {
        final String applicationId = getString(query.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM).orElse("");
        final InitiateCourtApplicationEntity applicationEntity = initiateCourtApplicationRepository.findBy(UUID.fromString(applicationId));

        JsonObject applicationObject = stringToJsonObjectConverter.convert(applicationEntity.getPayload());

        final List<CourtApplicationEntity> childApplications = courtApplicationRepository.findByParentApplicationId(UUID.fromString(applicationId));
        if (!childApplications.isEmpty()) {
            final JsonArray childApplicationsArray = buildApplicationSummaries(childApplications);
            applicationObject = createObjectBuilder(applicationObject).add("linkedApplications", createArrayBuilder(childApplicationsArray)).build();
        }

        return envelopeFrom(query.metadata(), applicationObject);
    }

    @Handles("progression.query.case.status-for-application")
    public JsonEnvelope getCaseStatusForApplication(final JsonEnvelope query) {
        final String applicationId = getString(query.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM).orElse("");
        final String caseId = getString(query.payloadAsJsonObject(), CASEID_SEARCH_PARAM).orElse("");
        final String prosecutionCase = courtApplicationCaseRepository.findCaseStatusByApplicationId(UUID.fromString(applicationId), UUID.fromString(caseId));
        final JsonObject payloadEntity = stringToJsonObjectConverter.convert(prosecutionCase);
        return envelopeFrom(query.metadata(), payloadEntity);
    }

    private JsonArray buildApplicationSummaries(final List<CourtApplicationEntity> childApplications) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        childApplications.forEach(cae -> buildApplicationSummary(cae, jsonArrayBuilder));
        return jsonArrayBuilder.build();
    }

    private JsonObject buildApplicationSummary(final CourtApplicationEntity parentApplication) {
        final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                (parentApplication.getPayload()), CourtApplication.class);

        return objectToJsonObjectConverter.convert(CourtApplicationSummary.applicationSummary()
                .withApplicationId(courtApplication.getId().toString())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withApplicationTitle(courtApplication.getType())
                .withApplicantDisplayName(courtApplication.getApplicant())
                .withRespondentDisplayNames(courtApplication.getRespondents())
                .withAssignedUserId(parentApplication.getAssignedUserId())
                .build());
    }


    private JsonArray buildCourtDocuments(final List<CourtDocumentEntity> courtDocuments) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        courtDocuments.forEach(courtDocumentEntity -> buildCourtDocument(courtDocumentEntity, jsonArrayBuilder));
        return jsonArrayBuilder.build();
    }


    private JsonObject buildAssignedUserJson(final AssignedUser assignedUser) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("userId", assignedUser.getUserId().toString());
        if (nonNull(assignedUser.getFirstName())) {
            jsonObjectBuilder.add("firstName", assignedUser.getFirstName());
        }
        if (nonNull(assignedUser.getLastName())) {
            jsonObjectBuilder.add("lastName", assignedUser.getLastName());
        }
        return jsonObjectBuilder.build();
    }


    private JsonArray getHearings(final List<UUID> applicationIds) {
        final List<HearingApplicationEntity> entities = new ArrayList<>();
        applicationIds.forEach(applicationId ->
                entities.addAll(hearingApplicationRepository.findByApplicationId(applicationId)));

        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
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

    private JsonArray getLinkedCases(final CourtApplication courtApplication) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        ofNullable(courtApplication.getCourtApplicationCases()).ifPresent(courtApplicationCases ->
                courtApplicationCases.forEach(courtApplicationCase -> {
                    final LinkedCases.Builder linkedCasesBuilder = new LinkedCases.Builder();
                    linkedCasesBuilder.withCaseStatus(courtApplicationCase.getCaseStatus())
                            .withProsecutionCaseId(courtApplicationCase.getProsecutionCaseId())
                            .withProsecutionCaseIdentifier(courtApplicationCase.getProsecutionCaseIdentifier());
                    final ProsecutionCase prosecutionCase = applicationAtAGlanceHelper.getProsecutionCase(courtApplicationCase.getProsecutionCaseId());
                    if (prosecutionCase != null && prosecutionCase.getInitiationCode() != null) {
                        linkedCasesBuilder.withInitiationCode(prosecutionCase.getInitiationCode().toString());
                    }
                    ofNullable(courtApplicationCase.getOffences()).ifPresent(courtApplicationOffences ->
                            linkedCasesBuilder.withOffences(courtApplicationOffences.stream().map(this::getOffence).collect(toList()))
                    );
                    jsonArrayBuilder.add(objectToJsonObjectConverter.convert(linkedCasesBuilder.build()));
                })
        );
        ofNullable(courtApplication.getCourtOrder()).ifPresent(courtOrder -> courtOrder.getCourtOrderOffences().forEach(courtOrderOffence -> {
            final LinkedCases.Builder linkedCasesBuilder = new LinkedCases.Builder();
            linkedCasesBuilder.withProsecutionCaseId(courtOrderOffence.getProsecutionCaseId());
            linkedCasesBuilder.withProsecutionCaseIdentifier(courtOrderOffence.getProsecutionCaseIdentifier());
            linkedCasesBuilder.withOffences(newArrayList(getOffence(courtOrderOffence.getOffence())));
            jsonArrayBuilder.add(objectToJsonObjectConverter.convert(linkedCasesBuilder.build()));
        }));
        return jsonArrayBuilder.build();
    }

    private Offences getOffence(final Offence offence) {
        final Offences.Builder offenceBuilder = new Offences.Builder();
        offenceBuilder.withId(offence.getId());
        offenceBuilder.withOffenceCode(offence.getOffenceCode());
        offenceBuilder.withOffenceTitle(offence.getOffenceTitle());
        offenceBuilder.withOffenceTitleWelsh(offence.getOffenceTitleWelsh());
        offenceBuilder.withOffenceLegislation(offence.getOffenceLegislation());
        offenceBuilder.withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh());
        offenceBuilder.withWording(offence.getWording());
        offenceBuilder.withWordingWelsh(offence.getWordingWelsh());
        offenceBuilder.withStartDate(offence.getStartDate());
        offenceBuilder.withEndDate(offence.getEndDate());
        offenceBuilder.withCount(offence.getCount());
        offenceBuilder.withAllocationDecision(offence.getAllocationDecision());
        ofNullable(offence.getPlea()).ifPresent(offenceBuilder::withPlea);
        ofNullable(offence.getVerdict()).ifPresent(offenceBuilder::withVerdict);
        offenceBuilder.withCustodyTimeLimit(offence.getCustodyTimeLimit());
        offenceBuilder.withReportingRestrictions(offence.getReportingRestrictions());
        ofNullable(offence.getJudicialResults()).ifPresent(judicialResults -> {
            final List<AagResults> aagResults = judicialResults.stream().map(applicationAtAGlanceHelper::getAagResult).collect(toList());
            offenceBuilder.withAagResults(aagResults);
        });
        return offenceBuilder.build();
    }

    private JsonEnvelope createJsonEnvelope(final JsonEnvelope envelope, final Map<UUID, List<NotificationStatusEntity>> applicationNotificationMap) {

        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        applicationNotificationMap.forEach((k, v) -> applicationNotificationMap.get(k).forEach(notificationStatusEntity -> prepareResponse(notificationStatusEntity, jsonArrayBuilder)));

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        jsonObjectBuilder.add(NOTIFICATION_STATUS, jsonArrayBuilder.build());

        return envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    private void prepareResponse(final NotificationStatusEntity notificationStatusEntity, final JsonArrayBuilder jsonArrayBuilder) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

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

        if (nonNull(courtApplication)) {
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

    private void buildCourtDocument(final CourtDocumentEntity courtDocumentEntity, final JsonArrayBuilder jsonArrayBuilder) {
        final String courtDocumentPayload = courtDocumentEntity.getPayload();
        final JsonObject courtDocumentJson = stringToJsonObjectConverter.convert(courtDocumentPayload);
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(courtDocumentJson, CourtDocument.class);
        if (Objects.isNull(courtDocumentEntity.isRemoved()) || !courtDocumentEntity.isRemoved()) {
            jsonArrayBuilder.add(objectToJsonObjectConverter.convert(CourtDocument.courtDocument()
                    .withCourtDocumentId(courtDocument.getCourtDocumentId())
                    .withDocumentCategory(courtDocument.getDocumentCategory())
                    .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                    .withDocumentTypeId(courtDocument.getDocumentTypeId())
                    .withName(courtDocument.getName())
                    .withMaterials(courtDocument.getMaterials())
                    .withMimeType(courtDocument.getMimeType())
                    .build()));
        }
    }
}