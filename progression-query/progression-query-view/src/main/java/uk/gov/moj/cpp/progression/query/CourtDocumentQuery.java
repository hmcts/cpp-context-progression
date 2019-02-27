package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
@SuppressWarnings({"squid:S1612"})
public class CourtDocumentQuery {

    public static final String ID_PARAMETER = "courtDocumentId";
    public static final String DEFENDANT_ID_PARAMETER = "defendantId";
    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENT_RESULT_FIELD = "courtDocument";
    public static final String CASE_IDS_SEARCH_PARAM = "caseId";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentQuery.class);

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CourtDocumentTransform courtDocumentTransform;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private UserAndGroupProvider userAndGroupProvider;

    private static JsonObject jsonFromString(String jsonObjectStr) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope envelope) {


        final Optional<UUID> id = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID_PARAMETER);
        JsonObject jsonDocument = null;

        if (id.isPresent()) {
            final CourtDocumentEntity courtDocumentEntity = courtDocumentRepository.findBy(id.get());
            jsonDocument = jsonFromString(courtDocumentEntity.getPayload());
        }

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add(COURT_DOCUMENT_RESULT_FIELD, jsonDocument);

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        return jsonObjectToObjectConverter.convert(jsonFromString(courtDocumentEntity.getPayload()), CourtDocument.class);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchcourtdocumentsByCaseId(final JsonEnvelope envelope) {
        final String strCaseIds = JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_IDS_SEARCH_PARAM).orElse("");
        //assume comma seperated
        final List<UUID> caseIds = new ArrayList<>();
        for (final String strCaseId : strCaseIds.trim().split(",")) {
            caseIds.add(UUID.fromString(strCaseId));
        }

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        final Map<UUID, CourtDocument> id2CourtDocument = new HashMap<>();
        caseIds.stream()
                .map(caseId -> courtDocumentRepository.findByProsecutionCaseId(caseId))
                .flatMap(List::stream)
                .map(entity -> courtDocument(entity))
                .forEach(courtDocument -> id2CourtDocument.put(courtDocument.getCourtDocumentId(), courtDocument));

        final Set<String> usergroupsInDocuments = id2CourtDocument.values().stream()
                .flatMap(cd -> cd.getMaterials() == null ? Stream.empty() : cd.getMaterials().stream())
                .flatMap(m -> m.getUserGroups() == null ? Stream.empty() : m.getUserGroups().stream())
                .collect(Collectors.toSet());
        final Set<String> permittedGroups = usergroupsInDocuments.stream().filter(
                userGroup -> userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(envelope),
                        asList(userGroup))
        ).collect(Collectors.toSet());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("search for case %s for documents userGroupsInDocument=%s permittedGroups=%s",
                    strCaseIds, usergroupsInDocuments, permittedGroups));
        }

        final List<CourtDocument> filteredMaterialCourtDocuments = id2CourtDocument.values().stream()
                .map(courtDocument -> filterPermittedMaterial(courtDocument, permittedGroups))
                .filter(courtDocument2 -> courtDocument2.getMaterials()!=null && !courtDocument2.getMaterials().isEmpty())
                .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices = filteredMaterialCourtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered).build())
                .collect(Collectors.toList());

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return JsonEnvelope.envelopeFrom(envelope.metadata(), resultJson);
    }

    private CourtDocument filterPermittedMaterial(final CourtDocument courtDocument, final Set<String> permittedGroups) {
        final List<Material> filteredMaterials =
                courtDocument.getMaterials().stream().filter(
                        m -> m.getUserGroups() != null && m.getUserGroups().stream().anyMatch(ug -> permittedGroups.contains(ug))
                ).collect(Collectors.toList());

        return CourtDocument.courtDocument()
                .withName(courtDocument.getName())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withIsRemoved(courtDocument.getIsRemoved())
                .withMimeType(courtDocument.getMimeType())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withMaterials(filteredMaterials)
                .build();
    }


}
