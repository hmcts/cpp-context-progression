
package uk.gov.moj.cpp.progression.query;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MaterialIdMapping;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MaterialBulkRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class MaterialBulkQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialBulkQueryView.class);

    @Inject
    private MaterialBulkRepository materialBulkRepository;

    @Handles("progression.query.material-content-bulk")
    public JsonEnvelope findMaterialIdMappingsInBulk(final JsonEnvelope envelope) {
        LOGGER.info("Processing bulk material mappings query");

        final List<UUID> materialIds = Arrays.stream(envelope.payloadAsJsonObject()
                        .getString("materialIds")
                        .split(","))
                .map(String::trim)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        LOGGER.debug("Querying for {} material IDs", materialIds.size());

        final List<MaterialIdMapping> mappings = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        LOGGER.info("Found {} material mappings", mappings.size());

        final JsonObject responsePayload = createResponsePayload(mappings);

        return envelopeFrom(envelope.metadata(), responsePayload);
    }


    private JsonObject createResponsePayload(final List<MaterialIdMapping> mappings) {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        mappings.forEach(mapping -> {
            final JsonObjectBuilder materialBuilder = Json.createObjectBuilder()
                    .add("materialId", mapping.getMaterialId().toString());

            if (mapping.getCourtDocumentId() != null) {
                materialBuilder.add("courtDocumentId", mapping.getCourtDocumentId().toString());
            } else {
                materialBuilder.addNull("courtDocumentId");
            }

            if (mapping.getCaseId() != null) {
                materialBuilder.add("caseId", mapping.getCaseId().toString());
            } else {
                materialBuilder.addNull("caseId");
            }

            if (mapping.getCaseUrn() != null) {
                materialBuilder.add("caseUrn", mapping.getCaseUrn());
            } else {
                materialBuilder.addNull("caseUrn");
            }

            arrayBuilder.add(materialBuilder);
        });

        return Json.createObjectBuilder().add("materialIds", arrayBuilder).build();
    }
}