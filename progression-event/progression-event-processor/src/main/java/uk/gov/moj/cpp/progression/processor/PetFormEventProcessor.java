package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class PetFormEventProcessor {

    private static final Logger LOGGER = getLogger(PetFormEventProcessor.class);
    public static final String PET_ID = "petId";
    public static final String CASE_ID = "caseId";
    public static final String MATERIAL_ID = "materialId";
    public static final String USER_ID = "userId";

    public static final String PUBLIC_PROGRESSION_PET_FORM_FINALISED = "public.progression.pet-form-finalised";
    public static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    public static final String PUBLIC_PROGRESSION_PET_FORM_CREATED = "public.progression.pet-form-created";
    public static final String PUBLIC_PROGRESSION_PET_FORM_UPDATED = "public.progression.pet-form-updated";
    public static final String PUBLIC_PROGRESSION_PET_DETAIL_UPDATED = "public.progression.pet-detail-updated";
    public static final String PUBLIC_PROGRESSION_PET_OPERATION_FAILED = "public.progression.pet-operation-failed";
    public static final String PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED = "public.progression.pet-form-defendant-updated";

    public static final UUID CASE_DOCUMENT_TYPE_ID = fromString("6b9df1fb-7bce-4e33-88dd-db91f75adeb8");

    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DATA = "data";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String DOCUMENT_TYPE_DESCRIPTION = "Case Management";
    public static final String PROSECUTION_CASE = "prosecutionCase";
    public static final String PET_FORM_DATA = "petFormData";

    @Inject
    private Sender sender;

    @Inject
    private MaterialService materialService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.pet-form-created")
    public void petFormCreated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-created event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_CREATED),
                event.payload()
        ));
    }


    @Handles("progression.event.pet-form-updated")
    public void petFormUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-updated event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_UPDATED),
                event.payload()
        ));
    }

    @Handles("progression.event.pet-form-finalised")
    public void petFormFinalised(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-finalised event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));
        final JsonObject publicEventPayload = event.payloadAsJsonObject();

        final UUID petId = fromString(publicEventPayload.getString(PET_ID));
        final UUID caseId = fromString(publicEventPayload.getString(CASE_ID));
        final UUID materialId = randomUUID();
        final JsonObject prosecutionCase = publicEventPayload.getJsonObject(PROSECUTION_CASE);

        final Optional<JsonObject> petForm = materialService.getStructuredForm(event, petId);
        if (!petForm.isPresent()) {
            LOGGER.info("progression.event.pet-form-finalised Pet does not exist. PetId: {}", petId);
            return;
        }

        LOGGER.info("Generating Pet Document PetId: {}, MaterialId: {}", petId, materialId);
        final JsonObject petFormData = stringToJsonObjectConverter.convert(petForm.get().getString(DATA));

        final JsonObject documentData = createObjectBuilder()
                .add(PET_FORM_DATA, petFormData)
                .add(PROSECUTION_CASE, prosecutionCase)
                .build();

        final String filename = documentGeneratorService.generatePetDocument(event, documentData, materialId);
        final JsonObject payload = createObjectBuilder()
                .add(PET_ID, petId.toString())
                .add(CASE_ID, caseId.toString())
                .add(USER_ID, publicEventPayload.getString(USER_ID))
                .add(MATERIAL_ID, materialId.toString())
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_FINALISED),
                payload
        ));

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter
                        .convert(buildCourtDocument(caseId, materialId, filename))).build();

        LOGGER.info("court document is being created '{}' ", jsonObject);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                jsonObject
        ));

    }

    @Handles("progression.event.pet-detail-updated")
    public void petDetailUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-detail-updated event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_DETAIL_UPDATED),
                event.payload()
        ));
    }

    @Handles("progression.event.pet-operation-failed")
    public void petOperationFailed(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-operation-failed event received: {}", event);
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_OPERATION_FAILED),
                event.payload()
        ));
    }


    @Handles("progression.event.pet-form-defendant-updated")
    public void petFormDefendantUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-defendant-updated received with petId: {} for case: {} for defendant: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID), event.payloadAsJsonObject().getString(DEFENDANT_ID));
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED),
                event.payload()
        ));
    }

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String filename) {

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseId)
                        .build())
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
                .build();
    }
}
