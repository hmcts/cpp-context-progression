package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.DefendantBailDocumentCreated;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class DefendantBailDocumentCreatedProcessor {

    protected static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantBailDocumentCreatedProcessor.class.getCanonicalName());
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private MaterialService materialService;

    @Inject
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.defendant-bail-document-created")
    public void handleDefendantBailDocumentCreatedEvent(final JsonEnvelope envelope) {
        final DefendantBailDocumentCreated defendantBailDocumentCreated = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantBailDocumentCreated.class);

        final Optional<JsonObject> documentsMetadata = referenceDataService.getAllDocumentsTypes(envelope, LocalDate.now(), requester);
        if (documentsMetadata.isPresent()) {
            final JsonObject bailDocument = documentsMetadata.get().getJsonArray("documentsMetadata").getValuesAs(JsonObject.class).stream()
                    .filter(jsonObject -> "Bail and Custody".equals(jsonObject.getString("documentType"))).findFirst().get();
            final String bailDocumentTypeId = bailDocument.getString("id");

            final Optional<JsonObject> materialMetadata = materialService.getMaterialMetadata(envelope, defendantBailDocumentCreated.getMaterialId());
            materialMetadata.ifPresent(data -> {
                final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                        .add("materialId", defendantBailDocumentCreated.getMaterialId().toString())
                        .add("courtDocument", objectToJsonObjectConverter
                                .convert(buildCourtDocumentWithMaterialUserGroups(defendantBailDocumentCreated, data, bailDocumentTypeId))).build();
                LOGGER.info("court document is being created '{}' ", jsonObject);
                sender.send(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_ADD_COURT_DOCUMENT).apply(jsonObject));
            });
        }
    }

    private CourtDocument buildCourtDocumentWithMaterialUserGroups(final DefendantBailDocumentCreated defendantBailDocumentCreated, final JsonObject materialMetadata, final String bailDocumentTypeId) {

        final List<UUID> defendants = new ArrayList<>();
        defendants.add(defendantBailDocumentCreated.getDefendantId());
        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument()
                .withDefendants(defendants)
                .withProsecutionCaseId(defendantBailDocumentCreated.getCaseId())
                .build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withDefendantDocument(defendantDocument)
                .build();

        final Material material = Material.material().withId(defendantBailDocumentCreated.getMaterialId())
                .withUploadDateTime(ZonedDateTime.parse(materialMetadata.getString("materialAddedDate")))
                .build();
        return CourtDocument.courtDocument()
                .withCourtDocumentId(defendantBailDocumentCreated.getBailDocumentId())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeId(UUID.fromString(bailDocumentTypeId))
                .withMimeType(materialMetadata.getString("mimeType"))
                .withName(materialMetadata.getString("fileName"))
                .withMaterials(Collections.singletonList(material))
                .build();
    }
}
