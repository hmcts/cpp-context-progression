package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.MapUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import java.util.Map;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemDocGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorService.class);
    private static final String GENERATE_DOCUMENT_COMMAND = "systemdocgenerator.generate-document";
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String PROPERTY_VALUE = "propertyValue";

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter jsonObjectConverter;


    public void generateDocument(final DocumentGenerationRequest request, final JsonEnvelope envelope) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("originatingSource", request.getOriginatingSource())
                .add("templateIdentifier", request.getTemplateIdentifier())
                .add("conversionFormat", request.getConversionFormat().getValue())
                .add("sourceCorrelationId", request.getSourceCorrelationId())
                .add("payloadFileServiceId", request.getPayloadFileServiceId().toString());

        if (isNotEmpty(request.getAdditionalInformation())) {
            JsonArrayBuilder infoArrayBuilder = JsonObjects.createArrayBuilder();
            final Map<String, String> additionalInfo = request.getAdditionalInformation();
            additionalInfo.forEach((k, v) ->
                    infoArrayBuilder.add(createObjectBuilder()
                            .add(PROPERTY_NAME, k)
                            .add(PROPERTY_VALUE, v)
                    ));
            payloadBuilder.add(ADDITIONAL_INFORMATION, infoArrayBuilder.build());
        }
        JsonObject payload = payloadBuilder.build();
        LOGGER.info(GENERATE_DOCUMENT_COMMAND + " - {}", payload);
        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata()).withName(GENERATE_DOCUMENT_COMMAND),
                payload
        ));
    }

}
