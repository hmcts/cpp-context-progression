package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsDocumentGenerated;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.formatters.AccountingDivisionCodeFormatter;
import uk.gov.moj.cpp.progression.service.ConversionFormat;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S00112")
public class NowsRequestedEventProcessor {

    private static final String FINANCIAL_ORDER_DETAILS = "financialOrderDetails";
    private static final String ACCOUNTING_DIVISION_CODE = "accountingDivisionCode";
    private static final UUID NOW_DOCUMENT_TYPE_ID = fromString("460fbc00-c002-11e8-a355-529269fb1459");
    private static final String READ_USER_GROUPS = "readUserGroups";
    private static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED = "public.progression.now-document-requested";
    private static final Logger LOGGER = LoggerFactory.getLogger(NowsRequestedEventProcessor.class);
    private static final String PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT = "progression.command.record-nows-document-sent";
    public static final String MATERIAL_ID = "materialId";
    public static final String NOW_DOCUMENT_REQUEST = "nowDocumentRequest";
    private final Sender sender;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;
    private final DocumentGeneratorService documentGeneratorService;
    private final RefDataService refDataService;
    private final UsersGroupService usersGroupService;
    private final ObjectMapper mapper;
    private final SystemDocGeneratorService systemDocGeneratorService;
    private final FileService fileService;
    @Inject
    private Requester requester;

    @Inject
    public NowsRequestedEventProcessor(final Sender sender,
                                       final DocumentGeneratorService documentGeneratorService,
                                       final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                       final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                       final RefDataService refDataService,
                                       final UsersGroupService usersGroupService,
                                       final FileService fileService,
                                       final SystemDocGeneratorService systemDocGeneratorService) {
        this.sender = sender;
        this.documentGeneratorService = documentGeneratorService;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.refDataService = refDataService;
        this.usersGroupService = usersGroupService;
        this.fileService = fileService;
        this.systemDocGeneratorService = systemDocGeneratorService;
        this.mapper = new ObjectMapperProducer().objectMapper();
    }

    @Handles("progression.event.now-document-requested")
    public void processNowDocumentRequested(final JsonEnvelope envelope) {

        final JsonObject requestJson = envelope.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nows requested payload - {}", requestJson);
        }

        final NowDocumentRequested nowDocumentRequested = jsonObjectToObjectConverter.convert(requestJson, NowDocumentRequested.class);

        final NowDocumentRequest nowDocumentRequest = nowDocumentRequested.getNowDocumentRequest();

        if (nowDocumentRequest.getStorageRequired()) {
            addAsCourtDocuments(envelope, nowDocumentRequest);
        }

        final UUID hearingId = nowDocumentRequest.getHearingId();

        final UUID materialId = nowDocumentRequest.getMaterialId();

        final String templateName = nowDocumentRequested.getTemplateName();

        final String fileName = nowDocumentRequested.getFileName();

        final JsonObject nowDocumentContentJson = objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent());

        final JsonObject updatedNowContent = updateNowContentWithAccountDivisionCode(nowDocumentContentJson);

        final UUID payloadFileId = fileService.storePayload(updatedNowContent, fileName, templateName);

        sendRequestToGenerateDocumentAsync(envelope, templateName, materialId.toString(), payloadFileId);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add("payloadFileId", payloadFileId.toString())
                .add("hearingId", hearingId.toString())
                .add("fileName", fileName)
                .add("cpsProsecutionCase", nowDocumentRequested.getCpsProsecutionCase());

        if (nonNull(nowDocumentRequest.getNowDistribution())) {
            payloadBuilder.add("nowDistribution", objectToJsonObjectConverter.convert(nowDocumentRequest.getNowDistribution()));
        }
        if (nonNull(nowDocumentRequest.getNowContent().getOrderAddressee())) {
            payloadBuilder.add("orderAddressee", objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent().getOrderAddressee()));
        }

        sender.send(envelop(payloadBuilder.build())
                .withName(PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT)
                .withMetadataFrom(envelope));

        final JsonObjectBuilder publicMessageBuilder = createObjectBuilder()
                .add(MATERIAL_ID, requestJson.getString(MATERIAL_ID))
                .add(NOW_DOCUMENT_REQUEST, requestJson.getJsonObject(NOW_DOCUMENT_REQUEST));

        sender.send(envelop(publicMessageBuilder.build())
                .withName(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED)
                .withMetadataFrom(envelope));
    }

    @Handles("progression.event.nows-document-generated")
    public void processNowsDocumentGenerated(final JsonEnvelope envelope) {

        final JsonObject requestJson = envelope.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("NOWs document generated payload - {}", requestJson);
        }

        final NowsDocumentGenerated nowsDocumentGenerated = jsonObjectToObjectConverter.convert(requestJson, NowsDocumentGenerated.class);

        final JsonEnvelope jsonEnvelope = assembleEnvelopeWithPayloadAndMetaDetails(requestJson,
                envelope.metadata().name(), nowsDocumentGenerated.getUserId().toString());

        documentGeneratorService.addDocumentToMaterial(sender, jsonEnvelope, nowsDocumentGenerated);
    }

    private void sendRequestToGenerateDocumentAsync(
            final JsonEnvelope envelope, final String templateName, final String materialId, final UUID fileId) {

        final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                "NOWs",
                templateName,
                ConversionFormat.PDF,
                materialId,
                fileId);

        systemDocGeneratorService.generateDocument(documentGenerationRequest, envelope);
    }

    private JsonObject updateNowContentWithAccountDivisionCode(final JsonObject jsonObject) {

        final JsonNode jsonNode = mapper.valueToTree(jsonObject);

        if (Objects.nonNull(jsonNode.path(FINANCIAL_ORDER_DETAILS)) && !(jsonNode.path(FINANCIAL_ORDER_DETAILS).isMissingNode())) {
            final int value = jsonObject.getJsonObject(FINANCIAL_ORDER_DETAILS).getInt(ACCOUNTING_DIVISION_CODE);
            final ObjectNode financialOrderDetailsNode = (ObjectNode) jsonNode.path(FINANCIAL_ORDER_DETAILS);
            financialOrderDetailsNode.put(ACCOUNTING_DIVISION_CODE, AccountingDivisionCodeFormatter
                    .formatAccountingDivisionCode(Integer.toString(value)));
            try {
                return jsonFromString(mapper.writeValueAsString(jsonNode));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return jsonObject;
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
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
                .withMimeType("application/pdf")
                .build();
    }
}
