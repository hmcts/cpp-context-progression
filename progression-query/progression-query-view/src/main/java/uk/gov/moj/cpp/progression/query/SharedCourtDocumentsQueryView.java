package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(Component.QUERY_VIEW)
public class SharedCourtDocumentsQueryView {

    private static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";

    @Inject
    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtDocumentTransform courtDocumentTransform;

    @Handles("progression.query.shared-court-documents")
    public JsonEnvelope getSharedCourtDocuments(final JsonEnvelope envelope) {

        if (!envelope.payloadAsJsonObject().containsKey("caseId") || !envelope.payloadAsJsonObject().containsKey("defendantId")) {
            throw new BadRequestException(String.format("%s no search parameter specified ", COURT_DOCUMENTS_SEARCH_NAME));
        }
        final UUID hearingId = fromString(envelope.payloadAsJsonObject().getString("hearingId"));
        final UUID userGroupId = fromString(envelope.payloadAsJsonObject().getString("userGroupId"));
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString("caseId"));
        final UUID defendantId = fromString(envelope.payloadAsJsonObject().getString("defendantId"));

        final List<SharedCourtDocumentEntity> sharedDocuments = sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId);

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();

        final List<CourtDocument> courtDocuments = sharedDocuments.stream()
                .map(courtDocumentIdentity -> courtDocumentRepository.findBy(courtDocumentIdentity.getCourtDocumentId()))
                .filter(cde -> !cde.isRemoved())
                .map(this::courtDocument)
                .collect(Collectors.toList());

        final List<CourtDocumentIndex> courtDocumentIndices = courtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered).build()).sorted((o1, o2) -> {
                    if (CollectionUtils.isNotEmpty(o1.getDocument().getMaterials()) &&
                            CollectionUtils.isNotEmpty(o2.getDocument().getMaterials()) &&
                            nonNull(o2.getDocument().getMaterials().get(0).getUploadDateTime()) &&
                            nonNull(o1.getDocument().getMaterials().get(0).getUploadDateTime())) {
                        return o2.getDocument().getMaterials().get(0).getUploadDateTime().compareTo(o1.getDocument().getMaterials().get(0).getUploadDateTime());
                    }
                    return -1;
                }).collect(Collectors.toList());

        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);

        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        return jsonObjectToObjectConverter.convert(jsonFromString(courtDocumentEntity.getPayload()), CourtDocument.class);
    }

}
