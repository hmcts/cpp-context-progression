package uk.gov.moj.cpp.progression.service.hearingeventlog;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingEventLogGenerationService {

    public static final String TEMPLATE_NAME = "HearingEventLog";
    public static final UUID PRIVATE_SECTION_JUDGES_HMCTS = UUID.fromString("460fae22-c002-11e8-a355-529269fb1459");
    public static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    public static final String READ_USER_GROUPS = "readUserGroups";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventLogGenerationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;


    public void generateHearingLogEvent(final JsonEnvelope event, final UUID caseId, final JsonObject hearingEventlogs, final Optional<String> applicationId) throws IOException {

        if (nonNull(hearingEventlogs)) {
            final String filename = format("HearingEventLog_%s.pdf", ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
            final UUID systemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new RuntimeException("systemUserProvider.getContextSystemUserId() not available"));
            final byte[] hearingEventLog = this.documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(hearingEventlogs, TEMPLATE_NAME, systemUserId);
            final UUID materialId = documentGeneratorService.generatePdfDocument(event, filename, hearingEventLog);
            LOGGER.info("materialId generated from document generator service - {}", materialId);
            generateCourtDocument(event, caseId, filename, materialId, applicationId);
        }
    }

    private void generateCourtDocument(final JsonEnvelope event, final UUID caseId, final String filename, final UUID materialId, final Optional<String> applicationId) {

        final CourtDocument courtDocument = courtDocument(event, materialId, caseId, filename, applicationId);
        final JsonObject courtDocumentPayload = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

        LOGGER.info("creating hearing event log court document payload - {}", courtDocumentPayload);
        sender.send(envelop(courtDocumentPayload).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(event));
    }

    private CourtDocument courtDocument(final JsonEnvelope envelope, final UUID materialId, final UUID caseId, final String filename, final Optional<String> applicationId) {

        DocumentCategory.Builder documentCategory = DocumentCategory.documentCategory();

        if(applicationId.isPresent() && !applicationId.get().isEmpty()) {
            documentCategory = documentCategory.withApplicationDocument(
                    ApplicationDocument.applicationDocument().withApplicationId(UUID.fromString(applicationId.get())).build());
        } else {
            documentCategory = DocumentCategory.documentCategory().
                    withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build());
        }
        final Optional<JsonObject> documentTypeData = referenceDataService.getDocumentTypeAccessData(PRIVATE_SECTION_JUDGES_HMCTS, envelope, requester);
        final JsonObject documentTypeDataJsonObject = documentTypeData.orElseThrow(() -> new RuntimeException("failed to look up Private section - Judges & HMCTS type " + PRIVATE_SECTION_JUDGES_HMCTS));
        final JsonObject documentTypeRBACData = documentTypeDataJsonObject.getJsonObject(COURT_DOCUMENT_TYPE_RBAC);
        final Integer seqNum = Integer.parseInt(documentTypeDataJsonObject.getJsonNumber("seqNum") == null ? "0" : documentTypeDataJsonObject.getJsonNumber("seqNum").toString());
        final List<String> rbacUserGroups = getRBACUserGroups(documentTypeRBACData, READ_USER_GROUPS);
        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(PRIVATE_SECTION_JUDGES_HMCTS)
                .withDocumentTypeDescription(documentTypeDataJsonObject.getString("section"))
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withUserGroups(rbacUserGroups)
                                .withName(filename)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory.build())
                .withDocumentTypeRBAC(getRBACDataObject(documentTypeRBACData))
                .withContainsFinancialMeans(false)
                .withSeqNum(seqNum)
                .withName(filename)
                .withMimeType("application/pdf")
                .withSendToCps(false)
                .build();

    }

    private DocumentTypeRBAC getRBACDataObject(final JsonObject documentTypeRBACData) {
        return DocumentTypeRBAC.
                documentTypeRBAC()
                .withUploadUserGroups(getRBACUserGroups(documentTypeRBACData, UPLOAD_ACCESS))
                .withReadUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                .withDownloadUserGroups(getRBACUserGroups(documentTypeRBACData, DOWNLOAD_ACCESS))
                .withDeleteUserGroups(getRBACUserGroups(documentTypeRBACData, DELETE_ACCESS))
                .build();
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeRBACData, final String accessLevel) {

        if (!documentTypeRBACData.containsKey(accessLevel)) {
            return new ArrayList<>();
        }
        final JsonArray documentTypeRBACJsonArray = documentTypeRBACData.getJsonArray(accessLevel);

        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject("cppGroup").getString("groupName")).collect(toList());
    }

}