package uk.gov.moj.cpp.progression.query.view;

import static uk.gov.moj.cpp.progression.query.CaseLsmInfoQuery.PARAM_CASE_ID;

import uk.gov.justice.progression.query.RelatedReference;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class RelatedReferenceQueryView {
    public static final String RELATED_REFERENCE_LIST = "relatedReferenceList";
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private RelatedReferenceRepository relatedReferenceRepository;

    @Handles("progression.query.related-references")
    public JsonEnvelope getProsecutionCaseWithRelatedUrn(final JsonEnvelope envelope) {
        final UUID caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), PARAM_CASE_ID)
                .orElseThrow(() -> new IllegalArgumentException("caseId parameter cannot be empty!"));
        return JsonEnvelope.envelopeFrom(envelope.metadata(), getRelatedReferences(caseId).build());
    }

    private JsonObjectBuilder getRelatedReferences(final UUID caseId) {
        final JsonObjectBuilder responseBuilder = JsonObjects.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        relatedReferenceRepository
                .findByProsecutionCaseId(caseId)
                .stream()
                .map(e -> RelatedReference.relatedReference()
                        .withRelatedReference(e.getReference())
                        .withRelatedReferenceId(e.getId())
                        .withProsecutionCaseId(e.getProsecutionCaseId())
                        .build())
                .map(e -> objectToJsonObjectConverter.convert(e))
                .forEach(e -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(e)));

        responseBuilder.add(RELATED_REFERENCE_LIST, jsonArrayBuilder);
        return responseBuilder;
    }

}
