package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentAddedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentAddedProcessor.class.getCanonicalName());
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataService refDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        final CourtsDocumentAdded courtsDocumentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentAdded.class);
        final CourtDocument courtDocument = courtsDocumentAdded.getCourtDocument();
        final Optional<JsonObject> documentTypeData = refDataService.getDocumentTypeData(courtDocument.getDocumentTypeId(), envelope);
        documentTypeData.ifPresent(data -> {
            final JsonObject jsonObject = Json.createObjectBuilder()
                    .add("courtDocument", objectToJsonObjectConverter
                            .convert(buildCourtDocumentWithMaterialUserGroups(courtDocument, data))).build();
            LOGGER.info("court document is being created '{}' ", jsonObject);
            sender.send(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
        });
    }

    private CourtDocument buildCourtDocumentWithMaterialUserGroups(final CourtDocument courtDocument, final JsonObject documentTypeData) {
        final JsonArray userGroupsArray = documentTypeData.getJsonArray("documentAccess");
        final Material commandMaterial = courtDocument.getMaterials().stream().findFirst().get();
        final Material material = Material.material().withId(commandMaterial
                .getId()).withGenerationStatus(commandMaterial.getGenerationStatus())
                .withName(commandMaterial.getName())
                .withUploadDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withUserGroups(userGroupsArray.stream().map(ug -> ((JsonString)ug).getString()).collect(Collectors.toList()))
                .build();
        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withName(courtDocument.getName())
                .withIsRemoved(false)
                .withMimeType(courtDocument.getMimeType())
                .withMaterials(Collections.singletonList(material))
                .build();
    }


}
