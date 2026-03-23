package uk.gov.justice.api.resource.utils.payload;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;

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
    private static final String HEARINGS = "hearings";
    private static final String PLEAS = "pleas";
    private static final String PLEA_VALUE = "pleaValue";
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String COURT_ORDERS = "courtOrders";
    private static final String COURT_ORDER_OFFENCES = "courtOrderOffences";
    private static final String COURT_APPLICATION_CASES = "courtApplicationCases";
    private static final String PLEA = "plea";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public JsonObject rebuildPleaWithDescription(final JsonObject payload) throws IOException {
        final Map<String, String> pleaTypeDescriptions = referenceDataService.retrievePleaTypeDescriptions();
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(payload);
        jsonNode.path(DEFENDANT).path(HEARINGS).forEach(hearing ->
                hearing.path(OFFENCES).forEach(offence ->
                        offence.path(PLEAS).forEach(plea -> {
                                    ((ObjectNode) plea).put(DESCRIPTION, pleaTypeDescriptions.get(plea.get(PLEA_VALUE).asText()));
                                }
                        )
                ));

        jsonNode.path(DEFENDANT).path(HEARINGS).forEach(hearing ->
                hearing.path(COURT_APPLICATIONS).forEach(courtApplication ->
                        courtApplication.path(COURT_ORDERS).path(COURT_ORDER_OFFENCES).forEach(courtOrderOffence -> {
                            if (courtOrderOffence.has(PLEA)){
                                final ObjectNode plea = (ObjectNode) courtOrderOffence.path(PLEA);
                                plea.put(DESCRIPTION, pleaTypeDescriptions.get(plea.get(PLEA_VALUE).asText()));
                            }
                        })
                ));

        jsonNode.path(DEFENDANT).path(HEARINGS).forEach(hearing ->
                hearing.path(COURT_APPLICATIONS).forEach(courtApplication ->
                        courtApplication.path(COURT_APPLICATION_CASES).forEach(courtApplicationCase ->
                                courtApplicationCase.path(OFFENCES).forEach(offence ->
                                        offence.path(PLEAS).forEach(pleaNode -> {
                                            if (pleaNode.has(PLEA_VALUE)) {
                                                final ObjectNode plea = (ObjectNode) pleaNode;
                                                plea.put(DESCRIPTION, pleaTypeDescriptions.get(plea.get(PLEA_VALUE).asText()));
                                            }
                                        })
                                )
                        )
                )
        );

        return objectMapper.treeToValue(jsonNode, JsonObject.class);
    }
}
