package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentReviewRequiredProcessor {

    @Inject
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.event.document-review-required")
    public void processDocumentReviewRequired(final JsonEnvelope event) {
        JsonObject payLoad = event.payloadAsJsonObject();
        final String caseId = payLoad.getString("caseId");
        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(event, caseId);
        if(prosecutionCaseOptional.isPresent()) {
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseOptional.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
            payLoad = enrich(payLoad, "urn", prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
            payLoad = enrich(payLoad, "prosecutingAuthority", prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode());
        }
        sender.send(Enveloper.envelop(payLoad).withName("public.progression.document-review-required").withMetadataFrom(event));
    }

    private JsonObject enrich(final JsonObject source, final String key, String value) {
        if(null != value ) {
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            source.entrySet().
                    forEach(e -> builder.add(e.getKey(), e.getValue()));
            builder.add(key, value);
            return builder.build();
        }
        return source;
    }

}
