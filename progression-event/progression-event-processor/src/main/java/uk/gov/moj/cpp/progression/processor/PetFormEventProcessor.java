package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.helper.DocmosisTextHelper;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class PetFormEventProcessor {

    public static final String LAST_NAME = "lastName";
    public static final String FORM_ID = "formId";
    public static final String PET_DEFENDANTS = "petDefendants";
    public static final String IS_YOUTH = "isYouth";
    public static final String UPDATED_BY = "updatedBy";
    public static final String USER_NAME = "userName";
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
    public static final String FINALISED_FORM_DATA = "finalisedFormData";
    public static final String DOCUMENT_FILE_NAME = "fileName";
    public static final String IS_WELSH = "isWelsh";
    public static final UUID CASE_DOCUMENT_TYPE_ID = fromString("6b9df1fb-7bce-4e33-88dd-db91f75adeb8");
    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DATA = "data";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String DOCUMENT_TYPE_DESCRIPTION = "Case Management";
    public static final String PROSECUTION_CASE = "prosecutionCase";
    public static final String PET_FORM_DATA = "petFormData";
    public static final String ID = "id";
    public static final String FIRST_NAME = "firstName";
    public static final String DEFENDANT_DATA = "defendantData";
    public static final String SUBMISSION_ID = "submissionId";
    public static final String FORM_ID_SNAKE_CASE = "form_id";
    private static final String CPS_DEFENDANT_ID = "cpsDefendantId";

    private static final Logger LOGGER = getLogger(PetFormEventProcessor.class);
    @Inject
    private Sender sender;

    @Inject
    private MaterialService materialService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private DocmosisTextHelper docmosisTextHelper;

    @Handles("progression.event.pet-form-created")
    public void petFormCreated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-created event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject publicEventPayload = buildPetFormCreatedPublicEventPayload(event);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_CREATED),
                publicEventPayload
        ));
    }

    @Handles("progression.event.pet-form-updated")
    public void petFormUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-updated event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject publicEventPayload = buildPetFormUpdatedPublicEventPayload(event);
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_UPDATED),
                publicEventPayload
        ));
    }

    @Handles("progression.event.pet-form-released")
    public void petFormReleased(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-released event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject formReleased = event.payloadAsJsonObject();
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder();
        publicEventBuilder.add(PET_ID, formReleased.getString(PET_ID))
                .add(CASE_ID, formReleased.getString(CASE_ID));
        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, formReleased.getString(USER_ID, null), formReleased.getString(USER_NAME, null)));

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName("public.progression.pet-form-released"),
                publicEventBuilder.build()
        ));
    }

    @Handles("progression.event.pet-form-finalised")
    public void petFormFinalised(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-form-finalised event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));
        final JsonObject publicEventPayload = event.payloadAsJsonObject();
        final UUID petId = fromString(publicEventPayload.getString(PET_ID));
        final UUID caseId = fromString(publicEventPayload.getString(CASE_ID));
        final JsonArray formArray = publicEventPayload.getJsonArray(FINALISED_FORM_DATA);
        final String submissionId = publicEventPayload.getString(SUBMISSION_ID, null);
        final UUID materialId = fromString(publicEventPayload.getString(MATERIAL_ID));

        LOGGER.info("Generating Pet Document PetId: {}, MaterialId: {}", petId, materialId);

        final JsonArrayBuilder documentMetadataArrayBuilder = createArrayBuilder();
        formArray.forEach(formDataPerDefendant ->
                documentMetadataArrayBuilder.add(processFinalisedFormData(event, formDataPerDefendant, petId, caseId, materialId)));

        final JsonObjectBuilder payload = createObjectBuilder()
                .add(PET_ID, petId.toString())
                .add(CASE_ID, caseId.toString())
                .add(UPDATED_BY, getUpdatedBy(event, publicEventPayload.getString(USER_ID, null), publicEventPayload.getString(USER_NAME, null)))
                .add(MATERIAL_ID, materialId.toString());
        if (isNotEmpty(submissionId)) {
            payload.add(SUBMISSION_ID, submissionId);
        }

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_FINALISED),
                payload
        ));

    }

    private JsonObject processFinalisedFormData(final JsonEnvelope event, final JsonValue formDataPerDefendant, final UUID petId, final UUID caseId, final UUID materialId) {
        final JsonObjectBuilder documentMetaDataBuilder = createObjectBuilder();
        final JsonObject documentData = stringToJsonObjectConverter.convert(docmosisTextHelper.replaceEscapeCharForDocmosis(((JsonString)formDataPerDefendant).getString()));
        LOGGER.info("Generating Pet Form Document petId: {}, MaterialId: {}", petId, materialId);
        final String filename = documentGeneratorService.generateFormDocument(event, FormType.PET, documentData, materialId);
        documentMetaDataBuilder.add(DOCUMENT_FILE_NAME, filename);
        documentMetaDataBuilder.add(IS_WELSH, documentData.getBoolean(IS_WELSH, false));

        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter
                        .convert(buildCourtDocument(caseId, materialId, filename))).build();

        LOGGER.info("court document is being created '{}' ", jsonObject);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                jsonObject
        ));
        return documentMetaDataBuilder.add(MATERIAL_ID, materialId.toString()).build();
    }

    @Handles("progression.event.pet-detail-updated")
    public void petDetailUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.pet-detail-updated event received with petId: {} for case: {}", event.payloadAsJsonObject().getString(PET_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject publicEventPayload = buildPetDetailUpdatedPublicEventPayload(event);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_DETAIL_UPDATED),
                publicEventPayload
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

        final JsonObject publicEventPayload = buildPetFormDefendantUpdatedPublicEventPayload(event);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED),
                publicEventPayload
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
                .withSendToCps(true)
                .withNotificationType("pet-form-finalised")
                .build();
    }

    private JsonObject buildPetFormDefendantUpdatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject petFormDefendantUpdated = event.payloadAsJsonObject();
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder();
        publicEventBuilder.add(PET_ID, petFormDefendantUpdated.getString(PET_ID))
                .add(CASE_ID, petFormDefendantUpdated.getString(CASE_ID))
                .add(DEFENDANT_ID, petFormDefendantUpdated.getString(DEFENDANT_ID))
                .add(DEFENDANT_DATA, petFormDefendantUpdated.getString(DEFENDANT_DATA));

        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, petFormDefendantUpdated.getString(USER_ID, null), petFormDefendantUpdated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }

    private JsonObject buildPetDetailUpdatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject petDetailUpdated = event.payloadAsJsonObject();
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder();
        publicEventBuilder.add(PET_ID, petDetailUpdated.getString(PET_ID))
                .add(CASE_ID, petDetailUpdated.getString(CASE_ID))
                .add(PET_DEFENDANTS, petDetailUpdated.getJsonArray(PET_DEFENDANTS));

        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, petDetailUpdated.getString(USER_ID, null), petDetailUpdated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }


    private JsonObject buildPetFormUpdatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject formUpdated = event.payloadAsJsonObject();
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder();
        publicEventBuilder.add(PET_ID, formUpdated.getString(PET_ID))
                .add(CASE_ID, formUpdated.getString(CASE_ID))
                .add(PET_FORM_DATA, formUpdated.getString(PET_FORM_DATA));
        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, formUpdated.getString(USER_ID, null), formUpdated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }

    private JsonObject buildPetFormCreatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject formCreated = event.payloadAsJsonObject();
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder();
        publicEventBuilder.add(PET_ID, formCreated.getString(PET_ID))
                .add(CASE_ID, formCreated.getString(CASE_ID))
                .add(FORM_ID, formCreated.getString(FORM_ID))
                .add(PET_DEFENDANTS, formCreated.getJsonArray(PET_DEFENDANTS));
        if (formCreated.containsKey(PET_FORM_DATA)) {
            publicEventBuilder.add(PET_FORM_DATA, formCreated.getString(PET_FORM_DATA));
        }
        if (formCreated.containsKey(IS_YOUTH)) {
            publicEventBuilder.add(IS_YOUTH, formCreated.getBoolean(IS_YOUTH));
        }
        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, formCreated.getString(USER_ID, null), formCreated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }

    private JsonObject getUpdatedBy(final JsonEnvelope event, final String userId, final String userName) {
        final JsonObjectBuilder builder = createObjectBuilder();

        if (isNotEmpty(userName)) {
            builder.add("name", userName);
            return builder.build();
        }

        final JsonObject userDetails = usersGroupService.getUserById(event, userId);
        return builder
                .add(ID, userId)
                .add(FIRST_NAME, userDetails.getString(FIRST_NAME))
                .add(LAST_NAME, userDetails.getString(LAST_NAME))
                .build();
    }

    @Handles("public.prosecutioncasefile.cps-serve-pet-submitted")
    public void handleServePetSubmittedPublicEvent(final JsonEnvelope envelope) {

        LOGGER.info("prosecutioncasefile.event.cps-serve-pet-submitted");

        final JsonObject payload = envelope.payloadAsJsonObject();

        final Optional<JsonObject> petFormObject = referenceDataService.getPetForm(envelope, requester);
        final String formId = petFormObject.map(jsonObject -> jsonObject.getString(FORM_ID_SNAKE_CASE)).orElse(null);

        final JsonArrayBuilder defendantIdArray = JsonObjects.createArrayBuilder();
        final List<JsonObject> petDefendantList = payload.getJsonArray(PET_DEFENDANTS).getValuesAs(JsonObject.class);
        petDefendantList.forEach(defendant -> defendantIdArray.add(JsonObjects.createObjectBuilder()
                                .add(DEFENDANT_ID, defendant.getString(DEFENDANT_ID))
                                .build()
                        )
                );

        final JsonObject createPetFormPayload = JsonObjects.createObjectBuilder().add(CASE_ID, payload.get(CASE_ID))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(PET_ID, String.valueOf(randomUUID()))
                .add(FORM_ID, formId)
                .add(PET_DEFENDANTS, defendantIdArray.build())
                .add(PET_FORM_DATA, appendDataElement(payload.getString(PET_FORM_DATA)))
                .add(USER_NAME, payload.getString(USER_NAME))
                .add(IS_YOUTH, payload.getBoolean(IS_YOUTH))
                .build();

        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                .withName("progression.command.create-pet-form")
                .build(), createPetFormPayload));

        if(isNotEmpty(petDefendantList)){
            petDefendantList.forEach( defendant -> updateCpsDefendantId(envelope, payload.getString(CASE_ID), defendant));
        }

        LOGGER.info("prosecutioncasefile.event.cps-serve-pet-submitted");

    }

    private void updateCpsDefendantId(final JsonEnvelope envelope, final String caseId, final JsonObject defendant) {
        if (isNotEmpty(defendant.getString(CPS_DEFENDANT_ID, null))) {
            final String defendantId = defendant.getString(DEFENDANT_ID);
            final String cpsDefendantId = defendant.getString(CPS_DEFENDANT_ID);
            LOGGER.info("updating defendant {} with cpsDefendantId {} in case {}", defendantId, cpsDefendantId, caseId);
            final UpdateCpsDefendantId updateCpsDefendantId = UpdateCpsDefendantId.updateCpsDefendantId()
                    .withCpsDefendantId(cpsDefendantId)
                    .withCaseId(fromString(caseId))
                    .withDefendantId(fromString(defendantId))
                    .build();

            sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("progression.command.update-cps-defendant-id").build(), updateCpsDefendantId));
        }
    }

    private String appendDataElement(final String petFormData) {
        final StringBuilder data = new StringBuilder();
        data.append("{\"data\":");
        data.append(petFormData);
        data.append("}");
        return data.toString();
    }
}
