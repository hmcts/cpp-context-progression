package uk.gov.moj.cpp.progression.query.view;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.progression.query.CaseLsmInfoQuery.PARAM_CASE_ID;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.RelatedReference;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

@ServiceComponent(Component.QUERY_VIEW)
public class RelatedReferenceQueryView {
    public static final String RELATED_REFERENCE_LIST = "relatedReferenceList";
    public static final String IS_CIVIL = "isCivil";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private RelatedReferenceRepository relatedReferenceRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private SearchProsecutionCaseRepository searchProsecutionCaseRepository;

    @Handles("progression.query.related-references")
    public JsonEnvelope getProsecutionCaseWithRelatedUrn(final JsonEnvelope envelope) {
        final UUID caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), PARAM_CASE_ID)
                .orElseThrow(() -> new IllegalArgumentException("caseId parameter cannot be empty!"));
        return JsonEnvelope.envelopeFrom(envelope.metadata(), getRelatedReferences(caseId).build());
    }

    private JsonObjectBuilder getRelatedReferences(final UUID caseId) {
        final JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        resolveIsCivil(caseId).ifPresent(isCivil -> responseBuilder.add(IS_CIVIL, isCivil));

        relatedReferenceRepository
                .findByProsecutionCaseId(caseId)
                .stream()
                .map(e -> {
                    final RelatedReference.Builder builder = RelatedReference.relatedReference()
                            .withRelatedReference(e.getReference())
                            .withRelatedReferenceId(e.getId())
                            .withProsecutionCaseId(e.getProsecutionCaseId());
                    resolveRelatedCaseIsCivil(e.getReference()).ifPresent(builder::withIsCivil);
                    return builder.build();
                })
                .map(objectToJsonObjectConverter::convert)
                .forEach(jsonArrayBuilder::add);

        responseBuilder.add(RELATED_REFERENCE_LIST, jsonArrayBuilder);
        return responseBuilder;
    }

    private Optional<Boolean> resolveRelatedCaseIsCivil(final String relatedReferenceUrn) {
        if (relatedReferenceUrn == null) {
            return Optional.empty();
        }

        return searchProsecutionCaseRepository.findByCaseUrn(relatedReferenceUrn.toUpperCase())
                .stream()
                .findFirst()
                .map(SearchProsecutionCaseEntity::getCaseId)
                .flatMap(this::toUUID)
                .flatMap(this::resolveIsCivil);
    }

    private Optional<Boolean> resolveIsCivil(final UUID caseId) {
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(
                    stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
            return Optional.of(nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    private Optional<UUID> toUUID(final String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
