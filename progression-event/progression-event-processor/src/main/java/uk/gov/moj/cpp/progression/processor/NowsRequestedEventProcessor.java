package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S00112")
public class NowsRequestedEventProcessor {

    public static final UUID NOW_DOCUMENT_TYPE_ID = UUID.fromString("460fbc00-c002-11e8-a355-529269fb1459");
    public static final String READ_USER_GROUPS = "readUserGroups";
    public static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED = "public.progression.now-document-requested";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications" ;
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";

    private static final Logger LOGGER = LoggerFactory.getLogger(NowsRequestedEventProcessor.class);
    private final Sender sender;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;
    private final DocumentGeneratorService documentGeneratorService;
    private final RefDataService refDataService;
    private final UsersGroupService usersGroupService;

    @Inject
    private Requester requester;

    @Inject
    public NowsRequestedEventProcessor(final Sender sender,
                                       final DocumentGeneratorService documentGeneratorService,
                                       final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                       final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                       final RefDataService refDataService,
                                       final UsersGroupService usersGroupService) {
        this.sender = sender;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.documentGeneratorService = documentGeneratorService;
        this.refDataService = refDataService;
        this.usersGroupService = usersGroupService;
    }

    @Handles("progression.event.now-document-requested")
    public void processNowDocumentRequested(final JsonEnvelope event) {

        final UUID userId = fromString(event.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final JsonObject requestJson = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nows requested payload - {}", requestJson);
        }

        final NowDocumentRequested nowDocumentRequested = jsonObjectToObjectConverter.convert(requestJson, NowDocumentRequested.class);

        final NowDocumentRequest nowDocumentRequest = nowDocumentRequested.getNowDocumentRequest();

        if (nowDocumentRequest.getStorageRequired()) {
            addAsCourtDocuments(event, nowDocumentRequest);
        }

        final String fileName = documentGeneratorService.generateNow(sender, event, userId, nowDocumentRequest);
        if (nowDocumentRequest.getTemplateName().startsWith("EDT_")) {
            final List<Envelope<JsonObject>> commandAddCourtDocumentEnvolopeList = getAddCourtDocumentEnvelope(event, nowDocumentRequested, fileName);
            commandAddCourtDocumentEnvolopeList.forEach(sender::send);
        }
        sender.send(envelop(event.payloadAsJsonObject())
                .withName(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED)
                .withMetadataFrom(event));
    }

    private List<Envelope<JsonObject>> getAddCourtDocumentEnvelope(final JsonEnvelope event, final NowDocumentRequested nowDocumentRequested, final String fileName) {
        LOGGER.info("EDT court document requested, NowDocumentRequested = {}", nowDocumentRequested.getNowDocumentRequest());
        final UUID materialId = nowDocumentRequested.getMaterialId();

        final List<Envelope<JsonObject>> commandAddCourtDocumentEnvolopeList = nowDocumentRequested.getNowDocumentRequest().getCases().stream().map(prosecutionCaseId -> {
                    CourtDocument courtDocument = buildCourtDocument(prosecutionCaseId, materialId, fileName, false);
                    final JsonObject jsonObject = createObjectBuilder()
                            .add("materialId", nowDocumentRequested.getMaterialId().toString())
                            .add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();
            return envelop(jsonObject).withName("progression.command.add-court-document").withMetadataFrom(event);
                }).collect(toList());
        LOGGER.info("EDT court document list size {}", commandAddCourtDocumentEnvolopeList.size());
        return commandAddCourtDocumentEnvolopeList;
    }

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String filename, boolean cpsFlag) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withUploadDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(filename)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(cpsFlag)
                .build();
    }

    private boolean isCpsProsecutionCase(final NowDocumentContent nowContent) {
        return nowContent.getCases().stream()
                .filter(pc -> nonNull(pc.getIsCps()))
                .filter(ProsecutionCase::getIsCps)
                .findFirst()
                .map(ProsecutionCase::getIsCps)
                .orElse(false);
    }

    private void addAsCourtDocuments(final JsonEnvelope incomingEvent, final NowDocumentRequest nowDocumentRequest) {

        final List<String> permittedGroups = getPermittedGroups(incomingEvent, nowDocumentRequest);

        final Optional<JsonObject> documentTypeData = refDataService.getDocumentTypeAccessData(NOW_DOCUMENT_TYPE_ID, incomingEvent, requester);

        final JsonObject documentTypeDataJsonObject = documentTypeData.orElseThrow(() -> new RuntimeException("Failed to look up NOWS document type " + NOW_DOCUMENT_TYPE_ID));

        if (!documentTypeDataJsonObject.containsKey(COURT_DOCUMENT_TYPE_RBAC) && !documentTypeDataJsonObject.getJsonObject(COURT_DOCUMENT_TYPE_RBAC).containsKey(READ_USER_GROUPS)) {
            throw new RuntimeException("No courtDocumentTypeRBAC specified for NOWS document type: " + NOW_DOCUMENT_TYPE_ID);
        }

        LOGGER.info("permittedGroups == {} ", permittedGroups);

        final CourtDocument courtDocument = courtDocument(nowDocumentRequest, permittedGroups, documentTypeDataJsonObject);

        final JsonObject jsonObject = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

        sender.send(envelop(jsonObject).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(incomingEvent));
    }

    private List<String> getPermittedGroups(final JsonEnvelope event, final NowDocumentRequest nowDocumentRequest) {

        final JsonObject groupsWithOrganisation = usersGroupService.getGroupsWithOrganisation(event);

        final Map<String, List<JsonObject>> resultsGroupMappings = getResultsGroupMappings(groupsWithOrganisation);

        final List<String> visibleToUserGroups = nowDocumentRequest.getVisibleToUserGroups();

        final List<String> notVisibleToUserGroups = nowDocumentRequest.getNotVisibleToUserGroups();

        //In NOWs document visibleToUserGroups and notVisibleToUserGroups are mutual exclusive
        if (nonNull(visibleToUserGroups)) {
            final List<List<String>> cppGroupUsersList = visibleToUserGroups.stream()
                    .map(userGroupName -> getAssociatedGroups(resultsGroupMappings, userGroupName))
                    .collect(toList());

            cppGroupUsersList.add(getAssociatedGroups(resultsGroupMappings, "HMCTS"));
            return cppGroupUsersList.stream().flatMap(Collection::stream).collect(toList());
        }

        if (nonNull(notVisibleToUserGroups)) {
            notVisibleToUserGroups.forEach(notVisibleToUserGroup -> resultsGroupMappings.remove(notVisibleToUserGroup.toUpperCase()));
            return getAllGroups(resultsGroupMappings);
        }

        return getAllGroups(resultsGroupMappings);
    }

    private Map<String, List<JsonObject>> getResultsGroupMappings(JsonObject groupsWithOrganisation) {

        final JsonArray groupsArray = groupsWithOrganisation.getJsonArray("groupsWithOrganisation");

        return groupsArray.stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(jsonObject -> jsonObject.containsKey("resultsReferenceDataGroup"))
                .filter(jsonObject -> jsonObject.containsKey("documentsReferenceDataGroup"))
                .collect(Collectors.groupingBy(jsonObject -> jsonObject.getString("resultsReferenceDataGroup").toUpperCase()));
    }

    private List<String> getAssociatedGroups(final Map<String, List<JsonObject>> resultsGroupMappings, final String resultGroup) {
        final List<JsonObject> resultsList = resultsGroupMappings.getOrDefault(resultGroup.toUpperCase(), Collections.emptyList());
        return resultsList.stream().map(jsonObject -> jsonObject.getString("groupName")).collect(toList());
    }

    private List<String> getAllGroups(final Map<String, List<JsonObject>> resultsGroupMap) {
        final Set<String> resultsSet = new HashSet<>();
        resultsGroupMap.forEach((k, v) -> resultsSet.addAll(getAssociatedGroups(resultsGroupMap, k)));
        return new ArrayList<>(resultsSet);
    }

    private DocumentTypeRBAC getRBACDataObject(final List<String> permittedRBACUserGroups) {
        return DocumentTypeRBAC.
                documentTypeRBAC()
                .withReadUserGroups(permittedRBACUserGroups)
                .withDownloadUserGroups(permittedRBACUserGroups)
                .build();
    }

    private NowDocument nowDocument(final UUID defendantId, final List<UUID> prosecutionCaseIds, final UUID hearingId) {
        return NowDocument.nowDocument()
                .withDefendantId(defendantId)
                .withOrderHearingId(hearingId)
                .withProsecutionCases(prosecutionCaseIds)
                .build();
    }

    private CourtDocument courtDocument(final NowDocumentRequest nowDocumentRequest, final List<String> permittedGroups, final JsonObject documentTypeDataJsonObject) {

        final Integer seqNum = Integer.valueOf(documentTypeDataJsonObject.getJsonNumber("seqNum") == null ? "0" : documentTypeDataJsonObject.getJsonNumber("seqNum").toString());

        final String sectionName = documentTypeDataJsonObject.getString("section", "");

        final String orderName = String.join(" - ", nowDocumentRequest.getSubscriberName(), nowDocumentRequest.getNowContent().getOrderName());

        final NowDocument nowDocument = nowDocument(nowDocumentRequest.getMasterDefendantId(), nowDocumentRequest.getCases(), nowDocumentRequest.getHearingId());

        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(NOW_DOCUMENT_TYPE_ID)
                .withDocumentTypeDescription(sectionName)
                .withDocumentTypeRBAC(getRBACDataObject(permittedGroups))
                .withSeqNum(seqNum)
                .withMaterials(Collections.singletonList(Material.material()
                        .withId(nowDocumentRequest.getMaterialId())
                        .withGenerationStatus(null)
                        .withUserGroups(permittedGroups)
                        .withUploadDateTime(ZonedDateTime.now())
                        .withName(orderName)
                        .build())
                )
                .withDocumentCategory(
                        DocumentCategory.documentCategory()
                                .withNowDocument(nowDocument)
                                .build()
                )
                .withName(orderName)
                .withMimeType(APPLICATION_PDF)
                .build();
    }
}
