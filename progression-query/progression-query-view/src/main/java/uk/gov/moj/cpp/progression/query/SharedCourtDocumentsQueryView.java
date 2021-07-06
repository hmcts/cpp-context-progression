package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(Component.QUERY_VIEW)
public class SharedCourtDocumentsQueryView {

    private static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    private static final int BATCH_SIZE = 50;

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

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

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
        final List<CourtDocument> courtDocuments = getCourtDocumentsByBatch(sharedDocuments);

        final List<CourtDocumentIndex> courtDocumentIndices = courtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered).build()).sorted((o1, o2) -> {
                    if (CollectionUtils.isNotEmpty(o1.getDocument().getMaterials()) &&
                            CollectionUtils.isNotEmpty(o2.getDocument().getMaterials()) &&
                            nonNull(o2.getDocument().getMaterials().get(0).getUploadDateTime()) &&
                            nonNull(o1.getDocument().getMaterials().get(0).getUploadDateTime())) {
                        return o2.getDocument().getMaterials().get(0).getUploadDateTime().compareTo(o1.getDocument().getMaterials().get(0).getUploadDateTime());
                    }
                    return -1;
                }).filter(courtDocumentIndex -> courtDocumentIndex.getDefendantIds().isEmpty() || courtDocumentIndex.getDefendantIds().contains(defendantId)).collect(toList());

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);
        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private List<CourtDocument> getCourtDocumentsByBatch(final List<SharedCourtDocumentEntity> sharedDocuments) {
        final List<CourtDocument> courtDocumentsResult = new ArrayList<>();
        for (int i = 0; i < sharedDocuments.size(); i += BATCH_SIZE) {
            final List<UUID> courtDocumentIds = sharedDocuments.stream().skip(i).limit(BATCH_SIZE).map(SharedCourtDocumentEntity::getCourtDocumentId).collect(toList());
            final List<CourtDocumentEntity> courtDocumentEntities = courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(courtDocumentIds);
            final List<CourtDocument> courtDocuments = courtDocumentEntities.stream().map(this::courtDocument).collect(toList());
            courtDocumentsResult.addAll(courtDocuments);
        }
        return courtDocumentsResult;
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        return jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);
    }

}
