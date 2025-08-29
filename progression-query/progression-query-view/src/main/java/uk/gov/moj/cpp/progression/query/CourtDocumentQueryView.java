package uk.gov.moj.cpp.progression.query;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getLong;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.CourtDocumentSummary;
import uk.gov.justice.core.courts.CourtdocumentsWithPagination;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.progression.PaginationData;
import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.progression.enums.SortField;
import uk.gov.moj.cpp.progression.enums.SortOrder;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.service.CourtDocumentIndexService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.NoResultException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
@SuppressWarnings({"squid:S1612", "squid:S2259", "squid:S00112", "squid:S3776", "squid:S1155",
        "squid:S1166", "squid:S2221", "squid:MethodCyclomaticComplexity", "pmd:LocalVariableCouldBeFinal"})
public class CourtDocumentQueryView {

    private static final String GROUPS = "groups";
    private static final String GROUP_NAME = "groupName";
    public static final String ID_PARAMETER = "courtDocumentId";
    public static final String DEFENDANT_ID_PARAMETER = "defendantId";
    public static final String HEARING_ID_PARAMETER = "hearingId";
    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENTS_SEARCH_NAME_ALL = "progression.query.courtdocuments-all";
    public static final String COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME = "progression.query.courtdocuments.with.pagination";
    public static final String COURT_DOCUMENTS_NOW_SEARCH_NAME = "progression.query.courtdocuments.now";
    private static final String COURT_DOCUMENTS_SEARCH_PROSECUTION = "progression.query.courtdocuments.for.prosecution";
    public static final String COURT_DOCUMENT_RESULT_FIELD = "courtDocument";
    public static final String CASE_ID_SEARCH_PARAM = "caseId";
    private static final String IS_PROSECUTING_CASE = "prosecutingCase";
    public static final String DEFENDANT_ID_SEARCH_PARAM = "defendantId";
    public static final String APPLICATION_ID_SEARCH_PARAM = "applicationId";
    private static final String ID = "id";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_STATUS = "notificationStatus";
    public static final String APPLICATION_ID = "applicationId";
    public static final String PROSECUTION_NOTIFICATION_STATUS =
            "progression.query.prosecution.notification-status";
    private static final String REFERENCEDATA_GET_ALL_DOCUMENT_TYPE_ACCESS_QUERY =
            "referencedata.get-all-document-type-access";
    private static final String DOCUMENT_TYPE_ACCESS = "documentsTypeAccess";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentQueryView.class);
    public static final String DOCUMENT_USER_GROUP_LOGGER =
            "search for case %s for documents userGroupsInDocument=%s permittedGroups=%s";
    public static final String READ_USER_ACTION = "readUserGroups";
    private static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    private static final String SECTION = "sectionId";
    private static final String SORT_FIELD = "sortField";
    private static final String SORT_ORDER = "sortOrder";
    private static final String DATE = "date";
    private static final String DESC = "desc";
    private static final String ASC = "asc";
    private static final String DOCUMENT_NAME = "documentName";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long DEFAULT_PAGE = 1L;
    public static final String MIME_TYPE_PDF = "application/pdf";
    public static final String MATERIAL_TYPE_PDF = "pdf";
    private static final String PERMISSIONS = "permissions";
    private static final String CDES_EXCLUDE_NON_CPS_ROLE = "CDES_EXCLUDE_NON_CPS_ROLE";
    private static final String USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS = "usersgroups.get-logged-in-user-permissions";
    private static final String OBJECT = "object";


    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private CourtDocumentIndexService courtDocumentIndexService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CourtDocumentTransform courtDocumentTransform;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private Requester requester;

    @Inject
    private CpsSendNotificationRepository cpsSendNotificationRepository;
    
    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope envelope) {
        final Optional<UUID> id = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID_PARAMETER);
        CourtDocumentEntity courtDocumentEntity = null;
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);

        if (id.isPresent()) {
            courtDocumentEntity = courtDocumentRepository.findBy(id.get());
            if (nonNull(courtDocumentEntity) && !courtDocumentEntity.isRemoved()) {
                jsonEnvelope = envelopeFrom(envelope.metadata(),
                        jsonObjectBuilder
                                .add(COURT_DOCUMENT_RESULT_FIELD,
                                        jsonFromString(courtDocumentEntity
                                                .getPayload()))
                                .build());
            }
        }

        return jsonEnvelope;
    }

    private List<UUID> commaSeparatedUuidParam2UUIDs(final String strUuids) {
        return Stream.of(strUuids.split(","))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchCourtDocuments(final JsonEnvelope envelope) {
        final List<String> userGroupsByUserId = getUserGroupsByUserId(envelope);
        if(isUserHasNonCPSProsecutorExcludePermission(envelope)){
            userGroupsByUserId.removeIf(userGroup-> "Non CPS Prosecutors".equalsIgnoreCase(userGroup));
        }


        return searchCourtDocuments(userGroupsByUserId, envelope);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_PROSECUTION)
    public JsonEnvelope searchCourtDocumentsForProsecution(final JsonEnvelope envelope) {
        final List<String> userGroupsByUserId = getUserGroupsByUserId(envelope);
        return searchCourtDocuments(userGroupsByUserId, envelope);
    }

    private JsonEnvelope searchCourtDocuments(final List<String> userGroupsByUserId, final JsonEnvelope envelope) {
        final String strCaseIds =
                JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_ID_SEARCH_PARAM)
                        .orElse(null);
        final String defendantId = JsonObjects
                .getString(envelope.payloadAsJsonObject(), DEFENDANT_ID_SEARCH_PARAM)
                .orElse(null);
        final String strApplicationIds = JsonObjects
                .getString(envelope.payloadAsJsonObject(), APPLICATION_ID_SEARCH_PARAM)
                .orElse(null);

        final Boolean isProsecuting = JsonObjects
                .getBoolean(envelope.payloadAsJsonObject(), IS_PROSECUTING_CASE)
                .orElse(null);

        LOGGER.info("Prosecuting value from the envelope is {}", isProsecuting);

        final List<CourtDocumentEntity> courtDocumentEntities = new ArrayList<>();
        boolean foundSearchParameter = false;

        LOGGER.info("CourtDocumentQueryCourtDocumentQueryCourtDocumentQuery1 {}", strCaseIds);

        final List<UUID> masterDefendantIds = new ArrayList<>();
        if (isFilterByCaseIdAndDefedantId(strCaseIds, defendantId)) {
            final List<UUID> caseIdList = commaSeparatedUuidParam2UUIDs(strCaseIds);
            LOGGER.info("CourtDocumentQueryCourtDocumentQueryCourtDocumentQuery2 {}", caseIdList);

            final List<UUID> allDefendantIDs = commaSeparatedUuidParam2UUIDs(defendantId);
            if (caseIdList.size() == 1) {
                final List<UUID> masterDefendantList = retrieveMasterDefendantIdList(
                        caseIdList.get(0),
                        allDefendantIDs.size() > 0 ? allDefendantIDs.get(0) : null);
                if (masterDefendantList.size() == 1) {
                    LOGGER.info("CourtDocumentQueryCourtDocumentQueryCourtDocumentQuery3 {}",
                            masterDefendantList.get(0));
                    masterDefendantIds.addAll(masterDefendantList);
                }
            }

            allDefendantIDs.addAll(masterDefendantIds);

            foundSearchParameter = true;
            final List<CourtDocumentEntity> byProsecutionCaseIdsAndDefendantIds =
                    courtDocumentRepository.findByProsecutionCaseIdAndDefendantId(
                            commaSeparatedUuidParam2UUIDs(strCaseIds),
                            allDefendantIDs);

            // find by case ids only
            final List<CourtDocumentEntity> byProsecutionCaseIds =
                    courtDocumentRepository.findByProsecutionCaseIdsAndDefendantIsNull(
                            commaSeparatedUuidParam2UUIDs(strCaseIds));

            byProsecutionCaseIdsAndDefendantIds.addAll(byProsecutionCaseIds);

            if (!byProsecutionCaseIdsAndDefendantIds.isEmpty()) {
                courtDocumentEntities.addAll(byProsecutionCaseIdsAndDefendantIds);
            }

        } else if (isNotEmpty(strCaseIds)) {
            foundSearchParameter = true;
            final List<CourtDocumentEntity> byProsecutionCaseIds = courtDocumentRepository
                    .findByProsecutionCaseIds(commaSeparatedUuidParam2UUIDs(strCaseIds));
            if (!byProsecutionCaseIds.isEmpty()) {
                courtDocumentEntities.addAll(byProsecutionCaseIds);
            }
        } else if (isNotEmpty(defendantId)) {
            foundSearchParameter = true;
            courtDocumentEntities.addAll(courtDocumentRepository
                    .findByDefendantId(UUID.fromString(defendantId)));
        }

        if (isNotEmpty(strApplicationIds)) {
            foundSearchParameter = true;

            if(isNotAuthorisedToViewMaterials(envelope.metadata(), commaSeparatedUuidParam2UUIDs(strApplicationIds))){
                throw new ForbiddenRequestException("User does not have permission to view material list!");
            }

            final List<CourtDocumentEntity> byApplicationIds = courtDocumentRepository
                    .findByApplicationIds(commaSeparatedUuidParam2UUIDs(strApplicationIds));

            if (!byApplicationIds.isEmpty()) {
                courtDocumentEntities.addAll(byApplicationIds);
            }
        }
        if (!foundSearchParameter) {
            throw new RuntimeException(String.format("%s no search parameter specified ",
                    COURT_DOCUMENTS_SEARCH_NAME));
        }

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        final Map<UUID, CourtDocument> id2CourtDocument = new HashMap<>();

        final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList = getAllDocumentTypeAccess();

        courtDocumentEntities.stream()
                .filter(entity -> !entity.isRemoved())
                .map(this::courtDocument)
                .filter(courtDocument -> isAllowedToAccessDocumentForGivenAction(documentTypeAccessReferenceDataList, courtDocument, userGroupsByUserId))
                .forEach(courtDocument -> id2CourtDocument.put(courtDocument.getCourtDocumentId(), courtDocument));

        final Set<String> usergroupsInDocuments = id2CourtDocument.values().stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty()
                        : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty()
                        : m.getUserGroups().stream())
                .collect(Collectors.toSet());

        final Set<String> permittedGroups = getPermittedGroups(usergroupsInDocuments, userGroupsByUserId);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(DOCUMENT_USER_GROUP_LOGGER, strCaseIds, usergroupsInDocuments,
                    permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments =
                id2CourtDocument.values().stream()
                        .map(courtDocument -> filterPermittedMaterial(courtDocument,
                                permittedGroups))
                        .filter(courtDocument2 -> CollectionUtils
                                .isNotEmpty(courtDocument2.getMaterials()))
                        .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices = filteredMaterialCourtDocuments
                .stream().sorted(Comparator.comparing(CourtDocument::getSeqNum))
                .map(courtDocumentFiltered -> courtDocumentTransform
                        .transform(courtDocumentFiltered, cpsSendNotificationRepository).build())
                .filter(courtDocumentIndex -> isEmpty(defendantId)
                        || courtDocumentIndex.getDefendantIds().isEmpty()
                        || courtDocumentIndex.getDefendantIds()
                        .contains(UUID.fromString(defendantId))
                        || (masterDefendantIds.size() == 1
                        && courtDocumentIndex.getDefendantIds()
                        .contains(masterDefendantIds
                                .get(0))))
                .collect(Collectors.toList());

        if (TRUE.equals(isProsecuting)) {
            final List<UUID> defenceOnlyTrueList = documentTypeAccessReferenceDataList.stream()
                    .filter(documentTypeAccessReferenceData ->
                            TRUE.equals(documentTypeAccessReferenceData.getDefenceOnly()))
                    .map(DocumentTypeAccessReferenceData::getId)
                    .collect(toList());
            LOGGER.info("defence only true list {}", defenceOnlyTrueList);

            if (!defenceOnlyTrueList.isEmpty()) {
                final List<CourtDocumentIndex> listToRemove = courtDocumentIndices.stream()
                        .filter(cdi -> defenceOnlyTrueList.stream()
                                .anyMatch(d -> d.equals(cdi.getDocument().getDocumentTypeId())))
                        .collect(toList());

                LOGGER.info("The removed list {}", listToRemove);

                courtDocumentIndices.removeAll(listToRemove);
            }
        }

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private boolean isNotAuthorisedToViewMaterials(final Metadata metadata, final List<UUID> applicationIds) {
        if(CollectionUtils.isEmpty(applicationIds)){
            return false;
        }
        return applicationIds.stream()
                .map(this::getCourtApplication)
                .filter(Objects::nonNull)
                .anyMatch(courtApplication -> !userDetailsLoader.isUserHasPermissionForApplicationTypeCode(metadata, courtApplication.getType().getCode()));
    }

    private CourtApplication getCourtApplication(final UUID applicationId) {
        try {
            final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
            final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
            return jsonObjectToObjectConverter.convert(application, CourtApplication.class);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME_ALL)
    public JsonEnvelope searchCourtDocumentsAll(final JsonEnvelope envelope) {
        final String strCaseIds = JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_ID_SEARCH_PARAM).orElse(null);

        final List<CourtDocumentEntity> courtDocumentEntities = new ArrayList<>();

        final List<CourtDocumentEntity> byProsecutionCaseIds = courtDocumentRepository
                .findByProsecutionCaseIds(commaSeparatedUuidParam2UUIDs(strCaseIds));
        if (!byProsecutionCaseIds.isEmpty()) {
            courtDocumentEntities.addAll(byProsecutionCaseIds);
        }

        final List<CourtDocumentIndex> courtDocumentIndices = courtDocumentEntities.stream()
                .filter(entity -> !entity.isRemoved())
                .map(this::courtDocument)
                .map(courtDocumentFiltered -> courtDocumentTransform
                .transform(courtDocumentFiltered, cpsSendNotificationRepository).build())
                .collect(toList());

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private boolean isFilterByCaseIdAndDefedantId(final String strCaseIds,
                                                  final String defendantId) {
        return isNotEmpty(strCaseIds) && isNotEmpty(defendantId);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME)
    public JsonEnvelope searchCourtDocumentsWithPagination(final JsonEnvelope envelope) {
        final String sortField = getString(envelope.payloadAsJsonObject(), SORT_FIELD).orElse(DATE);
        final String sortOrder = getString(envelope.payloadAsJsonObject(), SORT_ORDER).orElse(equalsIgnoreCase(sortField, DATE) ? DESC : ASC);


        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(getUUID(envelope.payloadAsJsonObject(), CASE_ID).orElseThrow(() -> new IllegalArgumentException("caseId cannot be empty!")))
                .withDefendantId(getUUID(envelope.payloadAsJsonObject(), DEFENDANT_ID_SEARCH_PARAM))
                .withSectionId(getString(envelope.payloadAsJsonObject(), SECTION))
                .withDocumentName(getString(envelope.payloadAsJsonObject(), DOCUMENT_NAME))
                .build();


        final List<CourtDocumentIndexEntity> courtDocumentIndexEntities = courtDocumentIndexService.getCourtDocumentIndexByCriteria(searchCriteria);

        final List<DocumentTypeAccessReferenceData> documentTypeAccessData = getAllDocumentTypeAccess();
        final List<String> groupNamesForUser = getUserGroupsByUserId(envelope);

        final List<CourtDocument> courtDocumentList = courtDocumentIndexEntities.stream()
                .map(courtDocumentIndexEntity -> courtDocument(courtDocumentIndexEntity.getCourtDocument()))
                .filter(courtDocument -> isSearchCriteriaMatching(courtDocument, searchCriteria))
                .filter(courtDocument -> MATERIAL_TYPE_PDF.equals(courtDocument.getMimeType()) || MIME_TYPE_PDF.equals(courtDocument.getMimeType()))
                .filter(courtDocument -> isAllowedToAccessDocumentForGivenAction(documentTypeAccessData, courtDocument, groupNamesForUser))
                .filter(courtDocument -> CollectionUtils.isNotEmpty(courtDocument.getMaterials()))
                .collect(Collectors.toList());


        final Set<String> allowedUserGroupsInDocuments = courtDocumentList.stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty() : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty() : m.getUserGroups().stream())
                .collect(Collectors.toSet());

        final Set<String> permittedGroups = getPermittedGroups(allowedUserGroupsInDocuments, groupNamesForUser);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(DOCUMENT_USER_GROUP_LOGGER,
                    searchCriteria.getCaseId(), allowedUserGroupsInDocuments, permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments = courtDocumentList.stream()
                .map(courtDocument -> filterPermittedMaterial(courtDocument, permittedGroups))
                .filter(courtDocument -> CollectionUtils.isNotEmpty(courtDocument.getMaterials()))
                .collect(Collectors.toList());


        final List<CourtDocumentSummary> courtDocumentSummaries = filteredMaterialCourtDocuments.stream().map(courtDocument ->
                courtDocument.getMaterials().stream().map(getCourtDocumentSummary(courtDocument)).findFirst()).filter(Optional::isPresent).map(Optional::get).collect(toList());


        final PaginationData paginationData = PaginationData.paginationData()
                .withPage(getLong(envelope.payloadAsJsonObject(), PAGE).orElse(DEFAULT_PAGE).intValue())
                .withPageSize(getLong(envelope.payloadAsJsonObject(), PAGE_SIZE).orElse(DEFAULT_PAGE_SIZE).intValue())
                .withSortField(SortField.valueFor(sortField).orElse(SortField.DATE))
                .withSortOrder(SortOrder.valueFor(sortOrder).orElse(SortOrder.ASC))
                .withTotalRecordCount(courtDocumentSummaries.size())
                .build();
        final CourtdocumentsWithPagination result = CourtdocumentsWithPagination.courtdocumentsWithPagination()
                .withPaginationData(paginationData)
                .withCourtDocuments(getSortedCourtDocumentSummary(courtDocumentSummaries, paginationData))
                .build();


        return envelopeFrom(envelope.metadata(), objectToJsonObjectConverter.convert(result));
    }

    private boolean isUserHasNonCPSProsecutorExcludePermission(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, createObjectBuilder().build());
        final JsonEnvelope response = requester.request(requestEnvelope);

        if (nonNull(response) && nonNull(response.payload())) {
            final JsonObject responseJson = response.payloadAsJsonObject();
            if (nonNull(responseJson) && CollectionUtils.isNotEmpty(responseJson.getJsonArray(PERMISSIONS))) {
                return responseJson.getJsonArray(PERMISSIONS).getValuesAs(JsonObject.class).stream()
                        .anyMatch(permission -> CDES_EXCLUDE_NON_CPS_ROLE.equalsIgnoreCase(permission.getJsonString(OBJECT).getString()));
            }
        }
        return false;
    }

    @SuppressWarnings("squid:S3655")
    private boolean isSearchCriteriaMatching(final CourtDocument courtDocument, final SearchCriteria searchCriteria) {
        if (searchCriteria.getDocumentName().isPresent()) {
            return courtDocument.getName().toLowerCase().startsWith(searchCriteria.getDocumentName().get().toLowerCase());
        }
        return true;
    }

    private Function<Material, CourtDocumentSummary> getCourtDocumentSummary(final CourtDocument courtDocument) {
        return material ->
                CourtDocumentSummary.courtDocumentSummary()
                        .withCourtDocumentId(courtDocument.getCourtDocumentId())
                        .withAmendmentDate(courtDocument.getAmendmentDate())
                        .withDocumentCategory(courtDocument.getDocumentCategory())
                        .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                        .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                        .withDocumentTypeId(courtDocument.getDocumentTypeId())
                        .withDocumentTypeRBAC(courtDocument.getDocumentTypeRBAC())
                        .withIsRemoved(courtDocument.getIsRemoved())
                        .withMaterial(material)
                        .withMimeType(courtDocument.getMimeType())
                        .withName(courtDocument.getName())
                        .withSendToCps(courtDocument.getSendToCps())
                        .withSeqNum(courtDocument.getSeqNum())
                        .build();
    }

    private List<String> getUserGroupsByUserId(final JsonEnvelope envelope) {
        return getUserGroupsByUserId(new Action(envelope)).getJsonArray(GROUPS).stream()
                .map(groupJson -> (JsonObject) groupJson)
                .map(gr -> gr.getString(GROUP_NAME))
                .collect(Collectors.toList());
    }

    private List<CourtDocumentSummary> getSortedCourtDocumentSummary(final List<CourtDocumentSummary> courtDocumentSummaries, final PaginationData paginationData) {

        final List<CourtDocumentSummary> sortedCourtDocumentSummaries;
        if (SortField.SECTION == paginationData.getSortField()) {
            if (SortOrder.ASC == paginationData.getSortOrder()) {
                sortedCourtDocumentSummaries = courtDocumentSummaries.stream()
                        .sorted(Comparator.comparing(CourtDocumentSummary::getDocumentTypeDescription)).collect(toList());
            } else {
                sortedCourtDocumentSummaries = courtDocumentSummaries.stream()
                        .sorted(Comparator.comparing(CourtDocumentSummary::getDocumentTypeDescription).reversed()).collect(toList());
            }
        } else {
            if (SortOrder.ASC == paginationData.getSortOrder()) {
                sortedCourtDocumentSummaries = courtDocumentSummaries.stream()
                        .sorted(Comparator.comparing(courtDocumentSummary -> courtDocumentSummary.getMaterial().getUploadDateTime())).collect(toList());
            } else {
                sortedCourtDocumentSummaries = courtDocumentSummaries.stream()
                        .sorted(Comparator.comparing(courtDocumentSummary -> ((CourtDocumentSummary) courtDocumentSummary).getMaterial().getUploadDateTime()).reversed()).collect(toList());
            }
        }

        return sortedCourtDocumentSummaries.subList(getFromIndex(paginationData), getToIndex(paginationData));
    }

    private int getToIndex(final PaginationData paginationData) {
        return paginationData.getPage() * paginationData.getPageSize() > paginationData.getTotalRecordCount() ? paginationData.getTotalRecordCount() : paginationData.getPage() * paginationData.getPageSize();
    }

    private int getFromIndex(final PaginationData paginationData) {
        return (paginationData.getPage() - 1) * paginationData.getPageSize();
    }

    private boolean isAllowedToAccessDocumentForGivenAction(
            final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList,
            final CourtDocument courtDocument, final List<String> userGroupsByUserId) {


        final Optional<DocumentTypeAccessReferenceData> documentTypeAccess =
                documentTypeAccessReferenceDataList.stream().filter(
                                documentTypeAccessReferenceData -> documentTypeAccessReferenceData
                                        .getId()
                                        .equals(courtDocument.getDocumentTypeId()))
                        .findFirst();

        if (!documentTypeAccess.isPresent()) {
            return false;
        }

        final JsonObject documentTypeAccessObject =
                objectToJsonObjectConverter.convert(documentTypeAccess);

        if (isNull(courtDocument.getDocumentTypeId())) {
            return false;
        }

        final Optional<List<String>> listOfValidUserGroup =
                getListOfValidUserGroup(documentTypeAccessObject, READ_USER_ACTION);

        return listOfValidUserGroup.filter(documentTypeAccessGroup -> isUserGroupsMatchesWithRBAC(
                documentTypeAccessGroup, userGroupsByUserId)).isPresent();

    }


    private boolean isUserGroupsMatchesWithRBAC(final List<String> groupsWithCreateAccess,
                                                final List<String> userGroupsByUserId) {
        return groupsWithCreateAccess.stream().anyMatch(userGroupsByUserId::contains);
    }

    private Optional<List<String>> getListOfValidUserGroup(final JsonObject documentTypeData,
                                                           final String userAction) {
        if (documentTypeData == null || !documentTypeData.containsKey(COURT_DOCUMENT_TYPE_RBAC)
                || !documentTypeData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC)
                .containsKey(userAction)
                || documentTypeData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC)
                .getJsonArray(userAction).isEmpty()) {
            return Optional.empty();
        }
        final JsonArray courtDocumentTypeRBAC = documentTypeData
                .getJsonObject(COURT_DOCUMENT_TYPE_RBAC).getJsonArray(userAction);


        final List<String> result = IntStream.range(0, courtDocumentTypeRBAC.size())
                .mapToObj(courtDocumentTypeRBAC::getJsonObject)
                .filter(group -> group.getJsonObject("cppGroup") != null)
                .map(group -> group.getJsonObject("cppGroup"))
                .filter(group -> isNoneBlank(group.getString(GROUP_NAME)))
                .map(group -> group.getString(GROUP_NAME)).collect(toList());

        return Optional.of(result);

    }

    @Handles(COURT_DOCUMENTS_NOW_SEARCH_NAME)
    public JsonEnvelope searchCourtDocumentsByHearingId(final JsonEnvelope envelope) {
        final String hearingIds = JsonObjects
                .getString(envelope.payloadAsJsonObject(), HEARING_ID_PARAMETER).orElse("");
        final String defendantId = JsonObjects
                .getString(envelope.payloadAsJsonObject(), DEFENDANT_ID_PARAMETER)
                .orElse("");
        final String documentCategory = JsonObjects
                .getString(envelope.payloadAsJsonObject(), "document_category").orElse("");
        // assume comma separated
        final List<UUID> caseIds = new ArrayList<>();
        for (final String hearingId : hearingIds.trim().split(",")) {
            caseIds.add(UUID.fromString(hearingId));
        }

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        final Map<UUID, CourtDocument> id2CourtDocument = new HashMap<>();
        caseIds.stream()
                .map(hearingId -> courtDocumentRepository.findCourtDocumentForNow(hearingId, documentCategory, UUID.fromString(defendantId)))
                .flatMap(List::stream)
                .filter(cd -> !cd.isRemoved())
                .map(this::courtDocument)
                .forEach(courtDocument -> id2CourtDocument.put(courtDocument.getCourtDocumentId(), courtDocument));

        final Set<String> usergroupsInDocuments = id2CourtDocument.values().stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty()
                        : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty()
                        : m.getUserGroups().stream())
                .collect(Collectors.toSet());
        final Set<String> permittedGroups = getPermittedGroups(usergroupsInDocuments, envelope);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(DOCUMENT_USER_GROUP_LOGGER, hearingIds, usergroupsInDocuments,
                    permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments = id2CourtDocument.values()
                .stream()
                .map(courtDocument -> filterPermittedMaterial(courtDocument,
                        permittedGroups))
                .filter(courtDocument2 -> courtDocument2.getMaterials() != null
                        && !courtDocument2.getMaterials().isEmpty())
                .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices =
                filteredMaterialCourtDocuments.stream()
                        .map(courtDocumentFiltered -> courtDocumentTransform
                                .transform(courtDocumentFiltered, cpsSendNotificationRepository).build())
                        .collect(Collectors.toList());

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return envelopeFrom(envelope.metadata(), resultJson);
    }

    @Handles(PROSECUTION_NOTIFICATION_STATUS)
    public JsonEnvelope getCaseNotifications(final JsonEnvelope envelope) {

        final String strCaseIds = JsonObjects
                .getString(envelope.payloadAsJsonObject(), CASE_ID_SEARCH_PARAM).orElse("");

        final List<UUID> caseIds = Arrays.stream(strCaseIds.trim().split(",")).map(UUID::fromString)
                .collect(Collectors.toList());

        final Map<UUID, List<NotificationStatusEntity>> caseNotificationMap = caseIds.stream()
                .map(caseId -> notificationStatusRepository.findByCaseId(caseId))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(NotificationStatusEntity::getCaseId,
                        HashMap::new, Collectors.toCollection(ArrayList::new)));

        return createJsonEnvelope(envelope, caseNotificationMap);
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(
                jsonFromString(courtDocumentEntity.getPayload()), CourtDocument.class);

        return CourtDocument.courtDocument().withName(courtDocument.getName())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withMimeType(courtDocument.getMimeType())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withMaterials(courtDocument.getMaterials())
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withDocumentTypeRBAC(courtDocument.getDocumentTypeRBAC())
                .withSeqNum(courtDocumentEntity.getSeqNum()).build();
    }

    private CourtDocument filterPermittedMaterial(final CourtDocument courtDocument,
                                                  final Set<String> permittedGroups) {
        final List<Material> filteredMaterials = courtDocument.getMaterials().stream()
                .filter(m -> CollectionUtils.isEmpty(m.getUserGroups()) || m.getUserGroups()
                        .stream().anyMatch(ug -> permittedGroups.contains(ug)))
                .collect(Collectors.toList());

        return CourtDocument.courtDocument().withName(courtDocument.getName())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withMimeType(courtDocument.getMimeType())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withMaterials(filteredMaterials)
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withDocumentTypeRBAC(courtDocument.getDocumentTypeRBAC())
                .withSeqNum(courtDocument.getSeqNum()).build();
    }

    private JsonEnvelope createJsonEnvelope(final JsonEnvelope envelope,
                                            final Map<UUID, List<NotificationStatusEntity>> applicationNotificationMap) {

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        applicationNotificationMap.forEach((k, v) -> applicationNotificationMap.get(k).forEach(
                notificationStatusEntity -> prepareResponse(notificationStatusEntity,
                        jsonArrayBuilder)));

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        jsonObjectBuilder.add(NOTIFICATION_STATUS, jsonArrayBuilder.build());

        return envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    private void prepareResponse(final NotificationStatusEntity notificationStatusEntity,
                                 final JsonArrayBuilder jsonArrayBuilder) {

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        jsonObjectBuilder.add(ID, notificationStatusEntity.getId().toString())
                .add(NOTIFICATION_ID,
                        notificationStatusEntity.getNotificationId().toString())
                .add(CASE_ID, notificationStatusEntity.getCaseId().toString());

        ofNullable(notificationStatusEntity.getApplicationId())
                .ifPresent(applicationId -> jsonObjectBuilder.add(APPLICATION_ID,
                        applicationId.toString()));
        ofNullable(notificationStatusEntity.getMaterialId()).ifPresent(
                materialId -> jsonObjectBuilder.add("materialId", materialId.toString()));
        ofNullable(notificationStatusEntity.getNotificationStatus())
                .ifPresent(notificationStatus -> jsonObjectBuilder.add(NOTIFICATION_STATUS,
                        notificationStatus.toString()));
        ofNullable(notificationStatusEntity.getNotificationType())
                .ifPresent(notificationType -> jsonObjectBuilder.add("notificationType",
                        notificationType.toString()));
        ofNullable(notificationStatusEntity.getErrorMessage()).ifPresent(
                errorMessage -> jsonObjectBuilder.add("errorMessage", errorMessage));
        ofNullable(notificationStatusEntity.getStatusCode())
                .ifPresent(statusCode -> jsonObjectBuilder.add("statusCode", statusCode));
        ofNullable(notificationStatusEntity.getUpdated())
                .ifPresent(updated -> jsonObjectBuilder.add("updated", updated.toString()));

        jsonArrayBuilder.add(jsonObjectBuilder);
    }

    private Set<String> getPermittedGroups(final Set<String> userGroupsInDocuments,
                                           final JsonEnvelope envelope) {
        final List<String> userGroupsByUserId = getUserGroupsByUserId(envelope);
        return userGroupsInDocuments.stream()
                .filter(userGroup -> isMemberInGroups(userGroupsByUserId, userGroup))
                .collect(Collectors.toSet());
    }

    private Set<String> getPermittedGroups(final Set<String> userGroupsInDocuments,
                                           final List<String> userGroupsByUserId) {
        return userGroupsInDocuments.stream()
                .filter(userGroup -> isMemberInGroups(userGroupsByUserId, userGroup))
                .collect(Collectors.toSet());
    }

    private boolean isMemberInGroups(final List<String> userGroupsByUserId, final String userGroup) {
        boolean isMember = false;
        if (CollectionUtils.isNotEmpty(userGroupsByUserId)) {
            isMember = userGroupsByUserId.stream()
                    .anyMatch(u -> userGroup.equalsIgnoreCase(u));
        }
        return isMember;
    }

    private JsonObject getUserGroupsByUserId(final Action action) {
        JsonObject userGroups = null;
        final Optional<String> userId = action.userId();
        if (userId.isPresent()) {
            final Metadata metadata = metadataFrom(action.envelope().metadata())
                    .withName("usersgroups.get-groups-by-user").build();
            final JsonObject payload =
                    Json.createObjectBuilder().add("userId", userId.get()).build();
            final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);

            final Envelope<JsonObject> response =
                    requester.requestAsAdmin(jsonEnvelope, JsonObject.class);
            if (response.payload().getValueType() != JsonValue.ValueType.NULL) {
                userGroups = response.payload();
            }
        }

        return userGroups;
    }


    public List<DocumentTypeAccessReferenceData> getAllDocumentTypeAccess() {

        return getRefDataStream(REFERENCEDATA_GET_ALL_DOCUMENT_TYPE_ACCESS_QUERY,
                DOCUMENT_TYPE_ACCESS, createObjectBuilder().add(DATE, LocalDate.now().toString()))
                .map(asDocumentsMetadataRefData())
                .collect(Collectors.toList());

    }


    private Stream<JsonValue> getRefDataStream(final String queryName, final String fieldName,
                                               final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope =
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName(queryName),
                        jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class).payload()
                .getJsonArray(fieldName).stream();
    }

    @SuppressWarnings({"squid:S2139"})
    public static Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(),
                        DocumentTypeAccessReferenceData.class);
            } catch (final IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceData. Payload :{}",
                        jsonValue, e);
                throw new UncheckedIOException(e);
            }
        };
    }

    private List<UUID> retrieveMasterDefendantIdList(final UUID caseId, final UUID defendantId) {
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity =
                    prosecutionCaseRepository.findByCaseId(caseId);

            final JsonObject prosecutionCasePayload =
                    stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter
                    .convert(prosecutionCasePayload, ProsecutionCase.class);
            return prosecutionCase.getDefendants().stream()
                    .filter(d -> d.getId().equals(defendantId))
                    .map(Defendant::getMasterDefendantId).collect(Collectors.toList());
        } catch (final Exception ex) {
            return new ArrayList<>();
        }
    }
}
