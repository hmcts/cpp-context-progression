package uk.gov.moj.cpp.progression.query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.query.view.service.GetCaseAtAGlanceService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.SearchCaseBuilder;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

@SuppressWarnings({"squid:S3655", "squid:S1612"})
@ServiceComponent(Component.QUERY_VIEW)
public class ProsecutionCaseQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQuery.class);
    private static final String ID = "caseId";
    private static final String FIELD_QUERY = "q";
    private static final String SEARCH_RESULT = "searchResults";

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private SearchProsecutionCaseRepository searchCaseRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private GetCaseAtAGlanceService getCaseAtAGlanceService;

    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getProsecutionCase(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final Optional<UUID> caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            final JsonObject prosecutionCase = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            jsonObjectBuilder.add("prosecutionCase", prosecutionCase);
            final List<CourtDocumentEntity> courtDocuments = courtDocumentRepository.findByProsecutionCaseId(caseId
                    .get());

            final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            courtDocuments.stream().forEach(courtDocumentEntity ->
                    prepareResponse(courtDocumentEntity.getPayload(), jsonArrayBuilder)
            );
            jsonObjectBuilder.add("courtDocuments", jsonArrayBuilder.build());

        } catch (final NoResultException e) {
            LOGGER.info("### No case found with caseId='{}'", caseId, e);
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());

    }

    @Handles("progression.query.case-at-a-glance")
    public JsonEnvelope getCaseAtAGlance(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID);

        final GetCaseAtAGlance getCaseAtAGlance = getCaseAtAGlanceService.getCaseAtAGlance(caseId.get());
        final JsonObject getCaseAtAGlanceJson = objectToJsonObjectConverter.convert(getCaseAtAGlance);

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                getCaseAtAGlanceJson);
    }

    @Handles("progression.query.usergroups-by-material-id")
    public JsonEnvelope searchByMaterialId(final JsonEnvelope envelope) {

        LOGGER.debug("Searching for allowed user groups with materialId='{}'", FIELD_QUERY);
        final JsonObjectBuilder json = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = courtDocumentMaterialRepository.findBy(UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));
        if (courtDocumentMaterialEntity != null) {
            courtDocumentMaterialEntity.getUserGroups().forEach(s -> jsonArrayBuilder.add(s));
        } else {
            LOGGER.info("No user groups found with materialId='{}'", FIELD_QUERY);
        }
        json.add("allowedUserGroups", jsonArrayBuilder.build());
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                json.build());
    }

    @Handles("progression.query.search-cases")
    public JsonEnvelope searchCase(final JsonEnvelope envelope) {
        final String searchCriteria = envelope.payloadAsJsonObject().getString(FIELD_QUERY);
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        if (StringUtils.isNotBlank(searchCriteria)) {
            final List<SearchProsecutionCaseEntity> cases = searchCaseRepository.findBySearchCriteria(prepareSearch
                    (searchCriteria));
            final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            cases.forEach(caseEntity ->
                    jsonArrayBuilder.add(stringToJsonObjectConverter.convert(SearchCaseBuilder.searchCaseBuilder()
                            .withSearchCaseEntity(caseEntity)
                            .withDefendantFullName()
                            .withResultPayload()
                            .build().getResultPayload())));
            jsonObjectBuilder.add(SEARCH_RESULT, jsonArrayBuilder.build());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private void prepareResponse(final String resultPayload, final JsonArrayBuilder jsonArrayBuilder) {
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                (resultPayload), CourtDocument.class);
        if (Objects.isNull(courtDocument.getIsRemoved()) || !courtDocument.getIsRemoved()) {
            jsonArrayBuilder.add(objectToJsonObjectConverter.convert(CourtDocument.courtDocument()
                    .withCourtDocumentId(courtDocument.getCourtDocumentId())
                    .withDocumentCategory(courtDocument.getDocumentCategory())
                    .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                    .withDocumentTypeId(courtDocument.getDocumentTypeId())
                    .withName(courtDocument.getName())
                    .withMaterials(courtDocument.getMaterials())
                    .withMimeType(courtDocument.getMimeType())
                    .withIsRemoved(courtDocument.getIsRemoved())
                    .build()));
        }
    }

}
