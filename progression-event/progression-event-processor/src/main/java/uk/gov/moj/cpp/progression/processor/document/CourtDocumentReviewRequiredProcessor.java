package uk.gov.moj.cpp.progression.processor.document;

import static java.util.AbstractMap.SimpleEntry;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.MapUtils;

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
            payLoad = enrich(payLoad, getCaseValuesMap(prosecutionCase));
        }
        sender.send(Enveloper.envelop(payLoad).withName("public.progression.document-review-required").withMetadataFrom(event));
    }

    private Map<String, String> getCaseValuesMap(final ProsecutionCase prosecutionCase) {
        final Map<String, String> map = new HashMap<>();
        for (final SimpleEntry<String, String> e : Arrays.asList(
                new SimpleEntry<>("urn", ofNullable(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()).orElse(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())),
                new SimpleEntry<>("prosecutingAuthority", prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()))) {
            if (map.put(e.getKey(), e.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }

    private JsonObject enrich(final JsonObject source, final Map<String, String> keyValuePair) {
        if(MapUtils.isNotEmpty(keyValuePair)) {
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            source.entrySet().
                    forEach(e -> builder.add(e.getKey(), e.getValue()));

            keyValuePair.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .forEach(e -> builder.add(e.getKey(), e.getValue()));
            return builder.build();
        }
        return source;
    }

}
