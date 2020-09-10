package uk.gov.justice.api.resource.utils.payload;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PleaValueDescriptionBuilder {
    private static final String DESCRIPTION = "description";
    private static final String DEFENDANT = "defendant";
    private static final String OFFENCES = "offences";
    private static final String PLEAS = "pleas";
    private static final String PLEA_VALUE = "pleaValue";

    @Inject
    private ReferenceDataService referenceDataService;

    public JsonObject rebuildWithPleaValueDescription(final JsonObject payload) throws IOException {
        final Map<String, String> pleaTypeDescriptions = referenceDataService.retrievePleaTypeDescriptions();
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(payload);
        jsonNode.path(DEFENDANT).path(OFFENCES).forEach(offence ->
            offence.path(PLEAS).forEach(plea ->
                ((ObjectNode)plea).put(DESCRIPTION, pleaTypeDescriptions.get(plea.get(PLEA_VALUE).asText()))
            )
        );
        return objectMapper.treeToValue(jsonNode, JsonObject.class);
    }

}
